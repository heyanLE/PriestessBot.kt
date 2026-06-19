package com.heyanle.priestess.bot.config

import com.heyanle.priestess.bot.core.controller.BaseController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

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

    val configFlow: StateFlow<PriestessConfig> = _configFlow.asStateFlow()

    val databaseConfigFlow: StateFlow<DatabaseConfig> = _databaseConfigFlow.asStateFlow()
    val platformConfigsFlow: StateFlow<List<PlatformConfig>> = _platformConfigsFlow.asStateFlow()
    val providerConfigsFlow: StateFlow<List<ProviderConfig>> = _providerConfigsFlow.asStateFlow()
    val agentConfigFlow: StateFlow<AgentConfig> = _agentConfigFlow.asStateFlow()
    val pipelineConfigFlow: StateFlow<PipelineConfig> = _pipelineConfigFlow.asStateFlow()

    fun current(): PriestessConfig = _configFlow.value

    fun update(transform: (PriestessConfig) -> PriestessConfig): PriestessConfig {
        val next = transform(_configFlow.value)
        publish(next)
        return next
    }

    fun save(config: PriestessConfig = current()) {
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(config))
    }

    private fun publish(config: PriestessConfig) {
        _configFlow.value = config
        _databaseConfigFlow.value = config.database
        _platformConfigsFlow.value = config.platforms
        _providerConfigsFlow.value = config.providers
        _agentConfigFlow.value = config.agent
        _pipelineConfigFlow.value = config.pipeline
    }

    private fun load(): PriestessConfig {
        val file = File(path)
        if (!file.exists()) {
            val default = PriestessConfig()
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(default))
            return default
        }

        return try {
            json.decodeFromString<PriestessConfig>(file.readText())
        } catch (e: SerializationException) {
            logger.warn(e) { "Failed to parse config, backing up and using defaults: $path" }
            file.copyTo(File("$path.bak"), overwrite = true)
            PriestessConfig().also { file.writeText(json.encodeToString(it)) }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to read config, using defaults: $path" }
            PriestessConfig()
        }
    }

    companion object {
        const val DEFAULT_CONFIG_PATH = "config.json"

        fun resolveDefaultPath(): String {
            return System.getProperty("priestess.config.path")
                ?: System.getenv("PRIESTESS_CONFIG_PATH")
                ?: DEFAULT_CONFIG_PATH
        }
    }
}
