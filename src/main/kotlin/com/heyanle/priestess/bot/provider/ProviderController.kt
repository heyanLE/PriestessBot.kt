package com.heyanle.priestess.bot.provider

import com.heyanle.priestess.bot.config.ConfigCase
import com.heyanle.priestess.bot.config.ProviderConfig
import com.heyanle.priestess.bot.core.controller.BaseController
import kotlinx.coroutines.flow.collectLatest

/**
 * 模型提供者控制器，负责根据配置维护可用的运行时模型提供者。
 */
class ProviderController(
    configCase: ConfigCase,
) : BaseController("ProviderController") {

    private val configProviders = mutableMapOf<String, ChatProvider>()
    private val runtimeProviders = mutableMapOf<String, ChatProvider>()

    init {
        registerBuiltinProviders()
        refreshConfigProviders(configCase.current().providers)
        launchTask("provider-config-refresh") {
            configCase.providerConfigsFlow.collectLatest { configs ->
                refreshConfigProviders(configs)
            }
        }
    }

    @Synchronized
    fun register(provider: ChatProvider) {
        runtimeProviders[provider.metadata.name] = provider
    }

    @Synchronized
    fun unregister(name: String) {
        runtimeProviders.remove(name)
    }

    @Synchronized
    fun getByName(name: String): ChatProvider? = runtimeProviders[name] ?: configProviders[name]

    @Synchronized
    fun getAll(): List<ChatProvider> {
        return (configProviders + runtimeProviders).values.toList()
    }

    fun getMetaList(): List<ProviderMetadata> = getAll().map { it.metadata }

    suspend fun testAll(): Map<String, Boolean> {
        return getAll().associate { provider ->
            provider.metadata.name to try {
                provider.test()
            } catch (e: Exception) {
                logger.warn(e) { "Provider '${provider.metadata.name}' test failed" }
                false
            }
        }
    }

    @Synchronized
    private fun refreshConfigProviders(configs: List<ProviderConfig>) {
        configProviders.clear()
        for (config in configs) {
            val provider = ProviderRegistry.createFromConfig(config) ?: continue
            configProviders[provider.metadata.name] = provider
        }
    }
}
