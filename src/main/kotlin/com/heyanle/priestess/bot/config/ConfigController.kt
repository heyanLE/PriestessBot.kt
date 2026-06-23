package com.heyanle.priestess.bot.config

import com.heyanle.priestess.bot.core.controller.BaseController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Owns application configuration loaded from disk and publishes typed config slices.
 *
 * The external config file remains a single [PriestessConfig], while module code
 * observes focused [StateFlow] values such as platform, provider, database, agent,
 * and pipeline config. Runtime changes are pushed explicitly through [update] and
 * persisted through [save]; this controller does not watch the file system.
 */
class ConfigController(
    private val path: String = resolveDefaultPath(),
    private val envProvider: (String) -> String? = System::getenv,
) : BaseController("ConfigController") {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _configFlow = MutableStateFlow(load())
    private val _databaseConfigFlow = MutableStateFlow(_configFlow.value.database)
    private val _platformConfigsFlow = MutableStateFlow(_configFlow.value.platforms)
    private val _providerConfigsFlow = MutableStateFlow(_configFlow.value.providers)
    private val _agentConfigFlow = MutableStateFlow(_configFlow.value.agent)
    private val _pipelineConfigFlow = MutableStateFlow(_configFlow.value.pipeline)
    private val _serverConfigFlow = MutableStateFlow(_configFlow.value.server)
    private val _pluginConfigFlow = MutableStateFlow(_configFlow.value.plugins)
    private val _subAgentConfigFlow = MutableStateFlow(_configFlow.value.subAgents)

    val configFlow: StateFlow<PriestessConfig> = _configFlow.asStateFlow()

    val databaseConfigFlow: StateFlow<DatabaseConfig> = _databaseConfigFlow.asStateFlow()
    val platformConfigsFlow: StateFlow<List<PlatformConfig>> = _platformConfigsFlow.asStateFlow()
    val providerConfigsFlow: StateFlow<List<ProviderConfig>> = _providerConfigsFlow.asStateFlow()
    val agentConfigFlow: StateFlow<AgentConfig> = _agentConfigFlow.asStateFlow()
    val pipelineConfigFlow: StateFlow<PipelineConfig> = _pipelineConfigFlow.asStateFlow()
    val serverConfigFlow: StateFlow<ServerConfig> = _serverConfigFlow.asStateFlow()
    val pluginConfigFlow: StateFlow<PluginConfig> = _pluginConfigFlow.asStateFlow()
    val subAgentConfigFlow: StateFlow<SubAgentOrchestrationConfig> = _subAgentConfigFlow.asStateFlow()

    init {
        if (_configFlow.value.server.configWatchEnabled) {
            startFileWatcher(_configFlow.value.server.configWatchIntervalMillis)
        }
    }

    fun current(): PriestessConfig = _configFlow.value

    fun configPath(): String = path

    fun update(transform: (PriestessConfig) -> PriestessConfig): PriestessConfig {
        val next = transform(_configFlow.value)
        publish(next)
        return next
    }

    fun save(config: PriestessConfig = current()) {
        createBackupIfPresent()
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(config))
    }

    fun replace(config: PriestessConfig, persist: Boolean = true): PriestessConfig {
        publish(config)
        if (persist) save(config)
        return config
    }

    fun listBackups(): List<ConfigBackup> {
        val dir = backupDirectory()
        if (!Files.isDirectory(dir)) return emptyList()
        return Files.list(dir).use { stream ->
            stream
                .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".json") }
                .map { backup ->
                    ConfigBackup(
                        id = backup.fileName.toString(),
                        createdAt = backupCreatedAt(backup.fileName.toString()),
                        sizeBytes = Files.size(backup),
                        path = backup.toAbsolutePath().normalize().toString(),
                    )
                }
                .sorted(Comparator.comparing(ConfigBackup::createdAt).reversed())
                .toList()
        }
    }

    fun restoreBackup(id: String): PriestessConfig {
        val backup = resolveBackup(id)
        val restored = json.decodeFromString<PriestessConfig>(Files.readString(backup))
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(restored))
        return reload()
    }

    fun reload(): PriestessConfig {
        val next = load()
        publish(next)
        return next
    }

    fun startFileWatcher(intervalMillis: Long = current().server.configWatchIntervalMillis) {
        val file = File(path)
        var lastModified = file.takeIf { it.exists() }?.lastModified() ?: 0L
        launchTask("config-file-watcher") {
            while (true) {
                delay(intervalMillis.coerceAtLeast(250))
                val modified = file.takeIf { it.exists() }?.lastModified() ?: 0L
                if (modified != 0L && modified != lastModified) {
                    lastModified = modified
                    logger.info { "Config file changed, reloading: $path" }
                    reload()
                }
            }
        }
    }

    private fun publish(config: PriestessConfig) {
        _configFlow.value = config
        _databaseConfigFlow.value = config.database
        _platformConfigsFlow.value = config.platforms
        _providerConfigsFlow.value = config.providers
        _agentConfigFlow.value = config.agent
        _pipelineConfigFlow.value = config.pipeline
        _serverConfigFlow.value = config.server
        _pluginConfigFlow.value = config.plugins
        _subAgentConfigFlow.value = config.subAgents
    }

    private fun load(): PriestessConfig {
        val file = File(path)
        val fileConfig = if (!file.exists()) {
            val default = PriestessConfig()
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(default))
            default
        } else try {
            val text = file.readText().trimLeadingBom()
            if (text.isBlank()) {
                PriestessConfig().also { file.writeText(json.encodeToString(it)) }
            } else {
                json.decodeFromString<PriestessConfig>(text)
            }
        } catch (e: SerializationException) {
            logger.warn(e) { "Failed to parse config, backing up and using defaults: $path" }
            file.copyTo(File("$path.bak"), overwrite = true)
            PriestessConfig().also { file.writeText(json.encodeToString(it)) }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to read config, using defaults: $path" }
            PriestessConfig()
        }

        return applyEnvironmentOverrides(fileConfig)
    }

    private fun applyEnvironmentOverrides(config: PriestessConfig): PriestessConfig {
        val server = config.server.copy(
            enabled = envBoolean("PRIESTESS_SERVER_ENABLED", config.server.enabled),
            host = envString("PRIESTESS_SERVER_HOST", config.server.host),
            port = envInt("PRIESTESS_SERVER_PORT", config.server.port) { it in 1..65_535 },
            corsEnabled = envBoolean("PRIESTESS_SERVER_CORS_ENABLED", config.server.corsEnabled),
            configWatchEnabled = envBoolean("PRIESTESS_CONFIG_WATCH_ENABLED", config.server.configWatchEnabled),
            configWatchIntervalMillis = envLong(
                "PRIESTESS_CONFIG_WATCH_INTERVAL_MILLIS",
                config.server.configWatchIntervalMillis,
            ) { it >= 250L },
            apiToken = envString("PRIESTESS_SERVER_API_TOKEN", config.server.apiToken),
        )
        val database = config.database.copy(
            path = envString("PRIESTESS_DATABASE_PATH", config.database.path),
        )
        val plugins = config.plugins.copy(
            enabled = envBoolean("PRIESTESS_PLUGINS_ENABLED", config.plugins.enabled),
            directory = envString("PRIESTESS_PLUGINS_DIRECTORY", config.plugins.directory),
            autoDiscover = envBoolean("PRIESTESS_PLUGINS_AUTO_DISCOVER", config.plugins.autoDiscover),
        )
        return config.copy(server = server, database = database, plugins = plugins)
    }

    private fun envString(name: String, fallback: String): String {
        return envProvider(name)?.takeIf { it.isNotBlank() } ?: fallback
    }

    private fun envBoolean(name: String, fallback: Boolean): Boolean {
        val value = envProvider(name)?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return fallback
        return when (value) {
            "true", "1", "yes", "y" -> true
            "false", "0", "no", "n" -> false
            else -> fallback
        }
    }

    private fun envInt(name: String, fallback: Int, isValid: (Int) -> Boolean): Int {
        val value = envProvider(name)?.trim()?.toIntOrNull() ?: return fallback
        return value.takeIf(isValid) ?: fallback
    }

    private fun envLong(name: String, fallback: Long, isValid: (Long) -> Boolean): Long {
        val value = envProvider(name)?.trim()?.toLongOrNull() ?: return fallback
        return value.takeIf(isValid) ?: fallback
    }

    private fun createBackupIfPresent() {
        val file = Path.of(path)
        if (!Files.isRegularFile(file)) return
        val content = Files.readString(file)
        if (content.isBlank()) return
        val dir = backupDirectory()
        Files.createDirectories(dir)
        Files.writeString(nextBackupPath(dir), content)
    }

    private fun backupDirectory(): Path {
        val configPath = Path.of(path).toAbsolutePath().normalize()
        val parent = configPath.parent ?: Path.of("").toAbsolutePath().normalize()
        return parent.resolve("backups").resolve(configPath.fileName.toString()).normalize()
    }

    private fun resolveBackup(id: String): Path {
        require(id.isNotBlank()) { "Backup id must not be blank" }
        require(id == Path.of(id).fileName.toString()) { "Backup id must be a file name" }
        val dir = backupDirectory()
        val backup = dir.resolve(id).normalize()
        require(backup.startsWith(dir)) { "Backup id is outside backup directory" }
        require(Files.isRegularFile(backup)) { "Backup '$id' not found" }
        return backup
    }

    private fun backupTimestamp(): String {
        return BACKUP_FORMATTER.format(Instant.now())
    }

    private fun nextBackupPath(dir: Path): Path {
        val timestamp = backupTimestamp()
        var candidate = dir.resolve("$timestamp.json")
        var suffix = 1
        while (Files.exists(candidate)) {
            candidate = dir.resolve("$timestamp-$suffix.json")
            suffix += 1
        }
        return candidate
    }

    private fun backupCreatedAt(id: String): String {
        return id.removeSuffix(".json")
    }

    companion object {
        const val DEFAULT_CONFIG_PATH = "config.json"
        private val BACKUP_FORMATTER = DateTimeFormatter
            .ofPattern("yyyyMMdd-HHmmss-SSS")
            .withZone(ZoneOffset.UTC)

        fun resolveDefaultPath(): String {
            return System.getProperty("priestess.config.path")
                ?: System.getenv("PRIESTESS_CONFIG_PATH")
                ?: DEFAULT_CONFIG_PATH
        }

        private fun String.trimLeadingBom(): String {
            return if (startsWith('\uFEFF')) drop(1) else this
        }
    }
}

@Serializable
data class ConfigBackup(
    val id: String,
    val createdAt: String,
    val sizeBytes: Long,
    val path: String,
)
