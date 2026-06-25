package com.heyanle.priestess.bot.tool.builtin

import com.heyanle.priestess.bot.config.ConfigCase
import com.heyanle.priestess.bot.config.ConfigController
import com.heyanle.priestess.bot.plugin.PluginCase
import com.heyanle.priestess.bot.plugin.PluginExtensionRegistry
import com.heyanle.priestess.bot.plugin.PluginController
import com.heyanle.priestess.bot.platform.PlatformCase
import com.heyanle.priestess.bot.platform.PlatformController
import com.heyanle.priestess.bot.provider.ProviderCase
import com.heyanle.priestess.bot.provider.ProviderController
import com.heyanle.priestess.bot.server.HealthResponse
import com.heyanle.priestess.bot.server.RuntimeHealthProvider
import com.heyanle.priestess.bot.tool.AgentToolContext
import com.heyanle.priestess.bot.tool.ToolController
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HealthCheckToolTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `returns non sensitive runtime health summary`() = runBlocking {
        val fixture = healthFixture()
        val tool = HealthCheckTool { fixture.healthProvider }

        val result = tool.execute(AgentToolContext(), emptyMap())

        assertTrue(result.success)
        val response = json.decodeFromString<HealthResponse>(result.output)
        assertEquals("UP", response.status)
        assertTrue(response.components.keys.containsAll(listOf("database", "providers", "platforms", "plugins", "tools", "workspaceReload")))
        assertTrue(response.diagnostics.keys.containsAll(listOf("databasePath", "availableProviders", "registeredTools", "workspaceReloadEnabled")))
        assertFalse(result.output.contains("secret-provider-key"))
        assertFalse(result.output.contains("You are a helpful assistant"))
    }

    @Test
    fun `degrades one component without failing whole tool`() = runBlocking {
        val fixture = healthFixture()
        val degradedProvider = RuntimeHealthProvider(
            configController = fixture.configController,
            configCase = fixture.configCase,
            platformController = fixture.platformController,
            providerCase = fixture.providerCase,
            toolController = fixture.toolController,
            pluginCase = fixture.pluginCase,
            availableProvidersProvider = { error("provider registry unavailable") },
        )
        val tool = HealthCheckTool { degradedProvider }

        val result = tool.execute(AgentToolContext(), emptyMap())

        assertTrue(result.success)
        val response = json.decodeFromString<HealthResponse>(result.output)
        assertEquals("DEGRADED", response.status)
        assertEquals("DEGRADED", response.components["providers"])
        assertEquals("IllegalStateException", response.components["providersError"])
    }

    private fun healthFixture(): HealthFixture {
        val configPath = Files.createTempDirectory("priestess-health-config").resolve("config.json")
        val dbPath = Files.createTempFile("priestess-health", ".sqlite")
        val configController = ConfigController(configPath.toString())
        configController.replace(
            com.heyanle.priestess.bot.config.PriestessConfig(
                providers = listOf(
                    com.heyanle.priestess.bot.config.ProviderConfig(
                        name = "test-provider",
                        type = "test-provider",
                        apiKey = "secret-provider-key",
                    ),
                ),
                database = com.heyanle.priestess.bot.config.DatabaseConfig(path = dbPath.toString()),
            ),
        )
        val configCase = ConfigCase(configController)
        val toolController = ToolController().also {
            registerBuiltinTools(it, healthProvider = { error("not used") })
        }
        val providerController = ProviderController(configCase)
        val providerCase = ProviderCase(providerController)
        val pluginRegistry = PluginExtensionRegistry()
        val pluginCase = PluginCase(
            PluginController(
                configCase = configCase,
                extensionRegistry = pluginRegistry,
                toolController = toolController,
                providerController = providerController,
            ),
            pluginRegistry,
        )
        val platformCase = PlatformCase(pipelineCaseProvider = { error("not used") })
        val platformController = PlatformController(configCase, platformCase)
        val healthProvider = RuntimeHealthProvider(
            configController = configController,
            configCase = configCase,
            platformController = platformController,
            providerCase = providerCase,
            toolController = toolController,
            pluginCase = pluginCase,
        )
        return HealthFixture(
            configController = configController,
            configCase = configCase,
            platformController = platformController,
            providerController = providerController,
            providerCase = providerCase,
            toolController = toolController,
            pluginCase = pluginCase,
            healthProvider = healthProvider,
        )
    }

    private data class HealthFixture(
        val configController: ConfigController,
        val configCase: ConfigCase,
        val platformController: PlatformController,
        val providerController: ProviderController,
        val providerCase: ProviderCase,
        val toolController: ToolController,
        val pluginCase: PluginCase,
        val healthProvider: RuntimeHealthProvider,
    )

}
