package com.heyanle.priestess.bot.provider

class ProviderManager {

    private val providers = mutableMapOf<String, ChatProvider>()

    fun register(provider: ChatProvider) {
        providers[provider.metadata.name] = provider
    }

    fun getByName(name: String): ChatProvider? {
        return providers[name]
    }

    fun getAll(): List<ChatProvider> {
        return providers.values.toList()
    }

    fun getMetaList(): List<ProviderMetadata> {
        return providers.values.map { it.metadata }
    }

    suspend fun testAll(): Map<String, Boolean> {
        return providers.mapValues { (_, p) ->
            try { p.test() } catch (e: Exception) { false }
        }
    }
}
