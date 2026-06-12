package com.heyanle.priestess.bot.provider

import com.heyanle.priestess.bot.core.config.ProviderConfig
import com.heyanle.priestess.bot.provider.model.LLMRequest
import com.heyanle.priestess.bot.provider.model.LLMResponse

// ── Metadata ──

data class ProviderMetadata(
    val name: String,
    val displayName: String,
    val kind: LLMKind,
    val supportToolCalling: Boolean,
    val supportVision: Boolean,
    val supportStreaming: Boolean,
)

data class ProviderRegistration(
    val metadata: ProviderMetadata,
    val factory: (ProviderConfig) -> ChatProvider,
)

enum class LLMKind {
    OPENAI,       // OpenAI / OpenAI-compatible
    OLLAMA,       // Ollama local models
    ANTHROPIC,    // Anthropic Claude (v2)
    GEMINI,       // Google Gemini (v2)
}

// ── Interface ──

interface ChatProvider : Provider {
    suspend fun textChat(request: LLMRequest): LLMResponse
    suspend fun getModels(): List<String>
}

interface Provider {
    val metadata: ProviderMetadata
    val config: ProviderConfig
    suspend fun test(): Boolean
}

// ── Registry ──

object ProviderRegistry {

    private val registrations = mutableListOf<ProviderRegistration>()

    fun register(metadata: ProviderMetadata, factory: (ProviderConfig) -> ChatProvider) {
        registrations.add(ProviderRegistration(metadata, factory))
    }

    fun getMetaList(): List<ProviderMetadata> {
        return registrations.map { it.metadata }
    }

    fun createFromConfig(config: ProviderConfig): ChatProvider? {
        if (!config.enabled) return null
        return registrations.find { it.metadata.name == config.type || it.metadata.name == config.name }
            ?.factory?.invoke(config)
    }

    fun createByType(type: String, config: ProviderConfig): ChatProvider? {
        return registrations.find { it.metadata.name == type }
            ?.factory?.invoke(config)
    }
}
