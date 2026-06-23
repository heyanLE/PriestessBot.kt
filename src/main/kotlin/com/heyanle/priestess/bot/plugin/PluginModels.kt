package com.heyanle.priestess.bot.plugin

import kotlinx.serialization.Serializable

@Serializable
data class PluginManifest(
    val id: String,
    val name: String = id,
    val version: String = "0.0.0",
    val description: String = "",
    val entrypoint: String = "",
    val dependencies: List<String> = emptyList(),
    val capabilities: List<String> = emptyList(),
)

@Serializable
enum class PluginState {
    DISCOVERED,
    LOADED,
    ENABLED,
    DISABLED,
    FAILED,
}

@Serializable
data class PluginDescriptor(
    val manifest: PluginManifest,
    val state: PluginState,
    val path: String,
    val error: String? = null,
)

@Serializable
data class PluginExtensionMetadata(
    val pluginId: String,
    val type: String,
    val name: String,
    val description: String = "",
)
