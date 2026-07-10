package com.heyanle.priestess.bot.config

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfigReloadTest {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    @Test
    fun `reload publishes config slices from disk`() {
        val path = Files.createTempDirectory("priestess-config-reload").resolve("config.json")
        val controller = ConfigController(path.toString())
        val initial = PriestessConfig(
            server = ServerConfig(enabled = false),
            plugins = PluginConfig(directory = "plugins-a"),
        )
        Files.writeString(path, json.encodeToString(initial))
        controller.reload()

        val next = initial.copy(
            server = ServerConfig(enabled = true, port = 18080),
            plugins = PluginConfig(directory = "plugins-b"),
        )
        Files.writeString(path, json.encodeToString(next))

        val reloaded = controller.reload()

        assertTrue(reloaded.server.enabled)
        assertEquals(18080, controller.serverConfigFlow.value.port)
        assertEquals("plugins-b", controller.pluginConfigFlow.value.directory)
    }

    @Test
    fun `config with leading bom loads intended values`() {
        val path = Files.createTempDirectory("priestess-config-bom").resolve("config.json")
        val config = PriestessConfig(
            database = DatabaseConfig(path = "/nas/priestess.sqlite"),
            server = ServerConfig(enabled = true, port = 19090),
            plugins = PluginConfig(directory = "/nas/plugins"),
        )
        Files.writeString(path, "\uFEFF${json.encodeToString(config)}")

        val controller = ConfigController(path.toString())

        assertEquals("/nas/priestess.sqlite", controller.current().database.path)
        assertEquals(19090, controller.serverConfigFlow.value.port)
        assertEquals("/nas/plugins", controller.pluginConfigFlow.value.directory)
        assertFalse(Files.exists(path.resolveSibling("config.json.bak")))
    }

    @Test
    fun `empty config file initializes defaults without backup`() {
        val path = Files.createTempDirectory("priestess-config-empty").resolve("config.json")
        Files.writeString(path, "   \n\t")

        val controller = ConfigController(path.toString())

        assertEquals(PriestessConfig(), controller.current())
        assertTrue(Files.readString(path).isNotBlank())
        assertFalse(Files.exists(path.resolveSibling("config.json.bak")))
    }

    @Test
    fun `malformed config file is backed up and replaced with defaults`() {
        val path = Files.createTempDirectory("priestess-config-malformed").resolve("config.json")
        Files.writeString(path, "{ malformed")

        val controller = ConfigController(path.toString())

        assertEquals(PriestessConfig(), controller.current())
        assertTrue(Files.exists(path.resolveSibling("config.json.bak")))
        assertEquals("{ malformed", Files.readString(path.resolveSibling("config.json.bak")))
        assertEquals(PriestessConfig(), json.decodeFromString<PriestessConfig>(Files.readString(path)))
    }

    @Test
    fun `environment overrides apply on initial load`() {
        val path = Files.createTempDirectory("priestess-config-env-load").resolve("config.json")
        val fileConfig = PriestessConfig(
            database = DatabaseConfig(path = "file.sqlite"),
            server = ServerConfig(
                enabled = false,
                host = "127.0.0.1",
                port = 18080,
                corsEnabled = false,
                configWatchEnabled = false,
                configWatchIntervalMillis = 1_000,
                apiToken = "",
            ),
            plugins = PluginConfig(enabled = false, directory = "file-plugins", autoDiscover = false),
        )
        Files.writeString(path, json.encodeToString(fileConfig))

        val controller = ConfigController(
            path = path.toString(),
            envProvider = mapEnv(
                "PRIESTESS_SERVER_ENABLED" to "true",
                "PRIESTESS_SERVER_HOST" to "0.0.0.0",
                "PRIESTESS_SERVER_PORT" to "28080",
                "PRIESTESS_SERVER_CORS_ENABLED" to "yes",
                "PRIESTESS_CONFIG_WATCH_ENABLED" to "1",
                "PRIESTESS_CONFIG_WATCH_INTERVAL_MILLIS" to "3000",
                "PRIESTESS_SERVER_API_TOKEN" to "env-dashboard-token",
                "PRIESTESS_DATABASE_PATH" to "/nas/priestess.sqlite",
                "PRIESTESS_PLUGINS_ENABLED" to "y",
                "PRIESTESS_PLUGINS_DIRECTORY" to "/nas/plugins",
                "PRIESTESS_PLUGINS_AUTO_DISCOVER" to "true",
            ),
        )

        assertEquals("/nas/priestess.sqlite", controller.current().database.path)
        assertTrue(controller.serverConfigFlow.value.enabled)
        assertEquals("0.0.0.0", controller.serverConfigFlow.value.host)
        assertEquals(28080, controller.serverConfigFlow.value.port)
        assertTrue(controller.serverConfigFlow.value.corsEnabled)
        assertTrue(controller.serverConfigFlow.value.configWatchEnabled)
        assertEquals(3_000, controller.serverConfigFlow.value.configWatchIntervalMillis)
        assertEquals("env-dashboard-token", controller.serverConfigFlow.value.apiToken)
        assertTrue(controller.pluginConfigFlow.value.enabled)
        assertEquals("/nas/plugins", controller.pluginConfigFlow.value.directory)
        assertTrue(controller.pluginConfigFlow.value.autoDiscover)
    }

    @Test
    fun `environment overrides reapply after reload without rewriting file`() {
        val path = Files.createTempDirectory("priestess-config-env-reload").resolve("config.json")
        val first = PriestessConfig(
            database = DatabaseConfig(path = "first.sqlite"),
            server = ServerConfig(enabled = false, port = 18080, apiToken = ""),
            plugins = PluginConfig(directory = "first-plugins"),
        )
        Files.writeString(path, json.encodeToString(first))
        val controller = ConfigController(
            path = path.toString(),
            envProvider = mapEnv(
                "PRIESTESS_SERVER_PORT" to "38080",
                "PRIESTESS_SERVER_API_TOKEN" to "reload-token",
                "PRIESTESS_DATABASE_PATH" to "/env/priestess.sqlite",
                "PRIESTESS_PLUGINS_DIRECTORY" to "/env/plugins",
            ),
        )
        val second = first.copy(
            server = first.server.copy(enabled = true, host = "127.0.0.1", port = 19090, apiToken = "file-token"),
            plugins = first.plugins.copy(enabled = false, directory = "second-plugins"),
        )
        Files.writeString(path, json.encodeToString(second))

        val reloaded = controller.reload()

        assertTrue(reloaded.server.enabled)
        assertEquals("127.0.0.1", reloaded.server.host)
        assertEquals(38_080, reloaded.server.port)
        assertEquals("reload-token", reloaded.server.apiToken)
        assertEquals("/env/priestess.sqlite", reloaded.database.path)
        assertFalse(reloaded.plugins.enabled)
        assertEquals("/env/plugins", reloaded.plugins.directory)
        assertEquals(second, json.decodeFromString<PriestessConfig>(Files.readString(path)))
    }

    @Test
    fun `invalid environment overrides are ignored`() {
        val path = Files.createTempDirectory("priestess-config-env-invalid").resolve("config.json")
        val fileConfig = PriestessConfig(
            server = ServerConfig(
                enabled = true,
                port = 18080,
                configWatchIntervalMillis = 2_000,
            ),
            plugins = PluginConfig(enabled = true, autoDiscover = true),
        )
        Files.writeString(path, json.encodeToString(fileConfig))

        val controller = ConfigController(
            path = path.toString(),
            envProvider = mapEnv(
                "PRIESTESS_SERVER_ENABLED" to "maybe",
                "PRIESTESS_SERVER_PORT" to "70000",
                "PRIESTESS_CONFIG_WATCH_INTERVAL_MILLIS" to "100",
                "PRIESTESS_PLUGINS_ENABLED" to "sometimes",
                "PRIESTESS_PLUGINS_AUTO_DISCOVER" to "",
            ),
        )

        assertTrue(controller.current().server.enabled)
        assertEquals(18_080, controller.current().server.port)
        assertEquals(2_000, controller.current().server.configWatchIntervalMillis)
        assertTrue(controller.current().plugins.enabled)
        assertTrue(controller.current().plugins.autoDiscover)
    }

    @Test
    fun `workspace default dir publishes and honors environment override`() {
        val path = Files.createTempDirectory("priestess-config-workspace-dir").resolve("config.json")
        val fileConfig = PriestessConfig(
            workspace = WorkspaceRuntimeConfig(defaultDir = "/file/workspace"),
        )
        Files.writeString(path, json.encodeToString(fileConfig))

        val controller = ConfigController(
            path = path.toString(),
            envProvider = mapEnv(
                "PRIESTESS_WORKSPACE_DEFAULT_DIR" to "/env/workspace",
            ),
        )

        assertEquals("/env/workspace", controller.current().workspace.defaultDir)
        assertEquals("/env/workspace", controller.workspaceRuntimeConfigFlow.value.defaultDir)
    }

    @Test
    fun `persisted replacement creates timestamped backup and restore publishes it`() {
        val path = Files.createTempDirectory("priestess-config-backup").resolve("config.json")
        val original = PriestessConfig(
            database = DatabaseConfig(path = "original.sqlite"),
            server = ServerConfig(enabled = false, port = 18080),
            plugins = PluginConfig(directory = "original-plugins"),
        )
        Files.writeString(path, json.encodeToString(original))
        val controller = ConfigController(path.toString())
        val replacement = original.copy(
            database = DatabaseConfig(path = "replacement.sqlite"),
            server = ServerConfig(enabled = true, port = 28080),
            plugins = PluginConfig(directory = "replacement-plugins"),
        )

        controller.replace(replacement)

        val backups = controller.listBackups()
        assertEquals(1, backups.size)
        val backup = backups.single()
        assertTrue(backup.id.endsWith(".json"))
        assertTrue(backup.sizeBytes > 0)
        assertEquals(original, json.decodeFromString<PriestessConfig>(Files.readString(java.nio.file.Path.of(backup.path))))
        assertEquals(replacement, json.decodeFromString<PriestessConfig>(Files.readString(path)))

        val restored = controller.restoreBackup(backup.id)

        assertEquals(original, restored)
        assertEquals("original.sqlite", controller.databaseConfigFlow.value.path)
        assertEquals(18_080, controller.serverConfigFlow.value.port)
        assertEquals("original-plugins", controller.pluginConfigFlow.value.directory)
        assertEquals(original, json.decodeFromString<PriestessConfig>(Files.readString(path)))
    }

    @Test
    fun `restore rejects unknown backup id without changing active config`() {
        val path = Files.createTempDirectory("priestess-config-backup-reject").resolve("config.json")
        val active = PriestessConfig(server = ServerConfig(port = 18080))
        Files.writeString(path, json.encodeToString(active))
        val controller = ConfigController(path.toString())

        val result = runCatching { controller.restoreBackup("../config.json") }

        assertTrue(result.isFailure)
        assertEquals(active, json.decodeFromString<PriestessConfig>(Files.readString(path)))
    }

    private fun mapEnv(vararg values: Pair<String, String>): (String) -> String? {
        val env = values.toMap()
        return { name -> env[name] }
    }
}
