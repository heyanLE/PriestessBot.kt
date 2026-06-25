package com.heyanle.priestess.bot.integration

import com.heyanle.priestess.bot.config.ConfigCase
import com.heyanle.priestess.bot.config.ConfigController
import com.heyanle.priestess.bot.config.PluginConfig
import com.heyanle.priestess.bot.config.PriestessConfig
import com.heyanle.priestess.bot.platform.PlatformRegistry
import com.heyanle.priestess.bot.plugin.PluginCase
import com.heyanle.priestess.bot.plugin.PluginExtensionRegistry
import com.heyanle.priestess.bot.plugin.PluginController
import com.heyanle.priestess.bot.plugin.PluginManifest
import com.heyanle.priestess.bot.plugin.PluginState
import com.heyanle.priestess.bot.provider.ProviderController
import com.heyanle.priestess.bot.testkit.buildDemoPluginJar
import com.heyanle.priestess.bot.tool.ToolController
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PluginContributionIntegrationTest {
    @Test
    fun `plugin enable registers tool provider platform and disable cleans contributions`() = runBlocking {
        val root = Files.createTempDirectory("priestess-plugin-integration")
        val pluginDir = root.resolve("demo")
        Files.createDirectories(pluginDir)
        buildDemoPluginJar(pluginDir.toFile())
        Files.writeString(
            pluginDir.resolve("plugin.json"),
            Json.encodeToString(
                PluginManifest(
                    id = "demo",
                    name = "Demo",
                    version = "1.2.3",
                    entrypoint = "demo.plugin.DemoPlugin",
                    capabilities = listOf("tool", "provider", "platform"),
                ),
            ),
        )
        val configPath = Files.createTempDirectory("priestess-plugin-integration-config").resolve("config.json")
        val configController = ConfigController(configPath.toString())
        configController.replace(
            PriestessConfig(plugins = PluginConfig(directory = root.toString(), autoDiscover = false)),
        )
        val configCase = ConfigCase(configController)
        val extensionRegistry = PluginExtensionRegistry()
        val toolController = ToolController()
        val providerController = ProviderController(configCase)
        val manager = PluginController(
            configCase = configCase,
            extensionRegistry = extensionRegistry,
            toolController = toolController,
            providerController = providerController,
        )
        val pluginCase = PluginCase(manager, extensionRegistry)

        try {
            assertEquals(PluginState.DISCOVERED, pluginCase.discover().single().state)
            assertEquals(PluginState.ENABLED, pluginCase.enable("demo").state)

            assertEquals("demo-tool", toolController.get("demo-tool")?.schema?.name)
            assertEquals("demo-provider", providerController.getByName("demo-provider")?.metadata?.name)
            assertEquals("demo-platform", PlatformRegistry.createPlatform("demo-platform")?.metadata?.name)
            assertEquals(listOf("platform", "provider", "tool"), pluginCase.extensions().map { it.type }.sorted())

            assertEquals(PluginState.DISABLED, pluginCase.disable("demo").state)
            assertNull(toolController.get("demo-tool"))
            assertNull(providerController.getByName("demo-provider"))
            assertNull(PlatformRegistry.createPlatform("demo-platform"))
            assertTrue(pluginCase.extensions().isEmpty())
        } finally {
            runCatching { pluginCase.unload("demo") }
            runCatching { providerController.stop() }
            PlatformRegistry.unregister("demo-platform")
        }
    }
}
