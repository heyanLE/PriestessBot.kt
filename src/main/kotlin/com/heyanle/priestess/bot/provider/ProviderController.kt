package com.heyanle.priestess.bot.provider

import com.heyanle.priestess.bot.config.ConfigCase
import com.heyanle.priestess.bot.core.controller.BaseController

/**
 * Owns runtime LLM provider instances created from provider configuration.
 *
 * Built-in provider factories are registered when this controller is lazily
 * constructed, and enabled providers are materialized from the current config.
 * ProviderCase exposes lookup and health-check operations to other modules.
 */
class ProviderController(
    configCase: ConfigCase,
) : BaseController("ProviderController") {

    private val providers = mutableMapOf<String, ChatProvider>()

    init {
        registerBuiltinProviders()
        for (pc in configCase.current().providers) {
            val provider = ProviderRegistry.createFromConfig(pc) ?: continue
            register(provider)
        }
    }

    fun register(provider: ChatProvider) {
        providers[provider.metadata.name] = provider
    }

    fun getByName(name: String): ChatProvider? = providers[name]

    fun getAll(): List<ChatProvider> = providers.values.toList()

    fun getMetaList(): List<ProviderMetadata> = providers.values.map { it.metadata }

    suspend fun testAll(): Map<String, Boolean> {
        return providers.mapValues { (_, provider) ->
            try {
                provider.test()
            } catch (e: Exception) {
                logger.warn(e) { "Provider '${provider.metadata.name}' test failed" }
                false
            }
        }
    }
}
