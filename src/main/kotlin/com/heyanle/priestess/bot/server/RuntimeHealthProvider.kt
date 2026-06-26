package com.heyanle.priestess.bot.server

import com.heyanle.priestess.bot.config.ConfigCase
import com.heyanle.priestess.bot.plugin.PluginCase
import com.heyanle.priestess.bot.platform.PlatformCase
import com.heyanle.priestess.bot.provider.ProviderCase
import com.heyanle.priestess.bot.tool.ToolCase

/**
 * 运行时健康快照提供者，汇总配置、平台、模型、工具和插件的当前状态。
 */
class RuntimeHealthProvider(
    private val configCase: ConfigCase,
    private val platformCase: PlatformCase,
    private val providerCase: ProviderCase,
    private val toolCase: ToolCase,
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
            diagnostics["configPath"] = configCase.configPath()
            diagnostics["databasePath"] = config.database.path
            diagnostics["configuredPlatforms"] = config.platforms.size.toString()
            diagnostics["configuredProviders"] = config.providers.size.toString()
            diagnostics["workspaceReloadEnabled"] = config.server.configWatchEnabled.toString()
            diagnostics["workspaceReloadIntervalMillis"] = config.server.configWatchIntervalMillis.toString()
        }

        inspect(components, "platforms") {
            diagnostics["runningPlatforms"] = platformCase.runningPlatformCount().toString()
        }

        inspect(components, "providers") {
            val availableProviders = availableProvidersProvider?.invoke() ?: providerCase.getMetaList().size
            diagnostics["availableProviders"] = availableProviders.toString()
        }

        inspect(components, "tools") {
            diagnostics["registeredTools"] = toolCase.getAll().size.toString()
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
