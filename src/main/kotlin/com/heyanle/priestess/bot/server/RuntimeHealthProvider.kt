package com.heyanle.priestess.bot.server

import com.heyanle.priestess.bot.config.ConfigCase
import com.heyanle.priestess.bot.config.ConfigController
import com.heyanle.priestess.bot.plugin.PluginCase
import com.heyanle.priestess.bot.platform.PlatformController
import com.heyanle.priestess.bot.provider.ProviderCase
import com.heyanle.priestess.bot.tool.ToolController

class RuntimeHealthProvider(
    private val configController: ConfigController,
    private val configCase: ConfigCase,
    private val platformController: PlatformController,
    private val providerCase: ProviderCase,
    private val toolController: ToolController,
    private val pluginCase: PluginCase,
    private val startedAtMillis: Long = System.currentTimeMillis(),
    private val availableProvidersProvider: (() -> Int)? = null,
) {
    fun snapshot(): HealthResponse {
        val components = linkedMapOf(
            "config" to "UP",
            "database" to "UP",
            "server" to "UP",
            "providers" to "UP",
            "platforms" to "UP",
            "plugins" to "UP",
            "tools" to "UP",
            "workspaceReload" to "UP",
        )
        val diagnostics = linkedMapOf<String, String>()

        inspect(components, "config") {
            val config = configCase.current()
            diagnostics["configPath"] = configController.configPath()
            diagnostics["databasePath"] = config.database.path
            diagnostics["configuredPlatforms"] = config.platforms.size.toString()
            diagnostics["configuredProviders"] = config.providers.size.toString()
            diagnostics["workspaceReloadEnabled"] = config.server.configWatchEnabled.toString()
            diagnostics["workspaceReloadIntervalMillis"] = config.server.configWatchIntervalMillis.toString()
        }

        inspect(components, "platforms") {
            val runningPlatforms = platformController.getRunning().size
            diagnostics["runningPlatforms"] = runningPlatforms.toString()
        }

        inspect(components, "providers") {
            val availableProviders = availableProvidersProvider?.invoke() ?: providerCase.getMetaList().size
            diagnostics["availableProviders"] = availableProviders.toString()
        }

        inspect(components, "tools") {
            diagnostics["registeredTools"] = toolController.getAll().size.toString()
        }

        inspect(components, "plugins") {
            diagnostics["configuredPlugins"] = pluginCase.list().size.toString()
            diagnostics["loadedPluginExtensions"] = pluginCase.extensions().size.toString()
        }

        return HealthResponse(
            status = if (components.values.any { it == "DEGRADED" }) "DEGRADED" else "UP",
            components = components,
            uptimeMillis = (System.currentTimeMillis() - startedAtMillis).coerceAtLeast(0),
            diagnostics = diagnostics,
        )
    }

    private fun inspect(
        components: MutableMap<String, String>,
        component: String,
        block: () -> Unit,
    ) {
        try {
            block()
        } catch (e: Exception) {
            components[component] = "DEGRADED"
            components["${component}Error"] = e::class.simpleName ?: "Exception"
        }
    }
}
