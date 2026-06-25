package com.heyanle.priestess.bot.plugin

import com.heyanle.priestess.bot.provider.ChatProvider
import com.heyanle.priestess.bot.provider.ProviderController
import com.heyanle.priestess.bot.config.PlatformConfig
import com.heyanle.priestess.bot.platform.Platform
import com.heyanle.priestess.bot.platform.PlatformMetadata
import com.heyanle.priestess.bot.platform.PlatformRegistry
import com.heyanle.priestess.bot.tool.FunctionTool
import com.heyanle.priestess.bot.tool.ToolController
import com.heyanle.priestess.bot.tool.ToolMetadata
import com.heyanle.priestess.bot.tool.ToolSource

interface Plugin {
    fun onLoad(context: PluginContext) = Unit
    fun onEnable(context: PluginContext) = Unit
    fun onDisable(context: PluginContext) = Unit
    fun onUnload(context: PluginContext) = Unit
}

interface PluginContext {
    val manifest: PluginManifest
    val pluginPath: String

    fun registerExtension(type: String, name: String, description: String = "")
    fun extensions(type: String? = null): List<PluginExtensionMetadata>
    fun registerTool(tool: FunctionTool)
    fun registeredTools(): List<String>
    fun registerProvider(provider: ChatProvider)
    fun registeredProviders(): List<String>
    fun registerPlatform(metadata: PlatformMetadata, factory: (PlatformConfig?) -> Platform)
    fun registeredPlatforms(): List<String>
}

class DefaultPluginContext(
    override val manifest: PluginManifest,
    override val pluginPath: String,
    private val extensionRegistry: PluginExtensionRegistry,
    private val toolController: ToolController,
    private val providerController: ProviderController,
) : PluginContext {
    private val toolNames = linkedSetOf<String>()
    private val providerNames = linkedSetOf<String>()
    private val platformNames = linkedSetOf<String>()

    override fun registerExtension(type: String, name: String, description: String) {
        extensionRegistry.register(
            PluginExtensionMetadata(
                pluginId = manifest.id,
                type = type,
                name = name,
                description = description,
            ),
        )
    }

    override fun extensions(type: String?): List<PluginExtensionMetadata> {
        return extensionRegistry.list(type).filter { it.pluginId == manifest.id }
    }

    override fun registerTool(tool: FunctionTool) {
        val name = tool.schema.name
        toolController.unregister(name)
        toolController.register(
            tool = tool,
            metadata = ToolMetadata(source = ToolSource.PLUGIN, owner = manifest.id),
        )
        toolNames.add(name)
        registerExtension("tool", name, tool.schema.description)
    }

    override fun registeredTools(): List<String> = toolNames.toList()

    override fun registerProvider(provider: ChatProvider) {
        val name = provider.metadata.name
        providerController.unregister(name)
        providerController.register(provider)
        providerNames.add(name)
        registerExtension("provider", name, provider.metadata.displayName)
    }

    override fun registeredProviders(): List<String> = providerNames.toList()

    override fun registerPlatform(metadata: PlatformMetadata, factory: (PlatformConfig?) -> Platform) {
        PlatformRegistry.unregister(metadata.name)
        PlatformRegistry.registerMeta(metadata, factory)
        platformNames.add(metadata.name)
        registerExtension("platform", metadata.name, metadata.displayName)
    }

    override fun registeredPlatforms(): List<String> = platformNames.toList()

    fun clearRuntimeContributions() {
        toolNames.forEach { toolController.unregister(it) }
        toolNames.clear()
        providerNames.forEach { providerController.unregister(it) }
        providerNames.clear()
        platformNames.forEach { PlatformRegistry.unregister(it) }
        platformNames.clear()
        extensionRegistry.unregisterPlugin(manifest.id)
    }
}
