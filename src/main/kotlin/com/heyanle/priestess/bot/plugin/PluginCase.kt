package com.heyanle.priestess.bot.plugin

class PluginCase(
    private val controller: PluginController,
    private val extensionRegistry: PluginExtensionRegistry,
) {
    fun list(): List<PluginDescriptor> = controller.list()

    fun discover(): List<PluginDescriptor> = controller.discover()

    fun load(id: String): PluginDescriptor = controller.load(id)

    fun enable(id: String): PluginDescriptor = controller.enable(id)

    fun disable(id: String): PluginDescriptor = controller.disable(id)

    fun unload(id: String): PluginDescriptor = controller.unload(id)

    fun reload(): List<PluginDescriptor> = controller.reload()

    fun extensions(type: String? = null): List<PluginExtensionMetadata> = extensionRegistry.list(type)

    suspend fun stop() {
        controller.stop()
    }
}
