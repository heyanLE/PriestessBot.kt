package com.heyanle.priestess.bot.plugin

class PluginCase(
    private val manager: PluginManager,
    private val extensionRegistry: PluginExtensionRegistry,
) {
    fun list(): List<PluginDescriptor> = manager.list()

    fun discover(): List<PluginDescriptor> = manager.discover()

    fun load(id: String): PluginDescriptor = manager.load(id)

    fun enable(id: String): PluginDescriptor = manager.enable(id)

    fun disable(id: String): PluginDescriptor = manager.disable(id)

    fun unload(id: String): PluginDescriptor = manager.unload(id)

    fun reload(): List<PluginDescriptor> = manager.reload()

    fun extensions(type: String? = null): List<PluginExtensionMetadata> = extensionRegistry.list(type)
}
