package com.heyanle.priestess.bot.platform

import com.heyanle.priestess.bot.config.PlatformConfig

data class PlatformRegistration(
    val metadata: PlatformMetadata,
    val factory: (PlatformConfig?) -> Platform,
)

object PlatformRegistry {

    private val lock = Any()
    private val platforms = LinkedHashMap<String, PlatformRegistration>()

    fun registerMeta(metadata: PlatformMetadata, factory: (PlatformConfig?) -> Platform) {
        synchronized(lock) {
            platforms[metadata.name] = PlatformRegistration(metadata, factory)
        }
    }

    fun unregister(name: String) {
        synchronized(lock) {
            platforms.remove(name)
        }
    }

    fun getMetaList(): List<PlatformMetadata> {
        return synchronized(lock) {
            platforms.values.map { it.metadata }
        }
    }

    fun createPlatform(name: String, config: PlatformConfig? = null): Platform? {
        val registration = synchronized(lock) {
            platforms[name]
        }
        return registration?.factory?.invoke(config)
    }

    fun createFromConfig(config: PlatformConfig): Platform? {
        if (!config.enabled) return null
        val registration = synchronized(lock) {
            platforms[config.name] ?: platforms[config.type]
        }
        return registration?.factory?.invoke(config)
    }
}
