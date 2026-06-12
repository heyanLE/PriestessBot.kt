package com.heyanle.priestess.bot.platform

import com.heyanle.priestess.bot.core.config.PlatformConfig

data class PlatformRegistration(
    val metadata: PlatformMetadata,
    val factory: (PlatformConfig?) -> Platform,
)

object PlatformRegistry {

    private var _platforms = mutableListOf<PlatformRegistration>()

    fun registerMeta(metadata: PlatformMetadata, factory: (PlatformConfig?) -> Platform) {
        _platforms.add(PlatformRegistration(metadata, factory))
    }

    fun getMetaList(): List<PlatformMetadata> {
        return _platforms.map { it.metadata }
    }

    fun createPlatform(name: String, config: PlatformConfig? = null): Platform? {
        return _platforms.find { it.metadata.name == name }?.factory?.invoke(config)
    }

    fun createFromConfig(config: PlatformConfig): Platform? {
        if (!config.enabled) return null
        return _platforms.find { it.metadata.name == config.name || it.metadata.name == config.type }
            ?.factory?.invoke(config)
    }
}
