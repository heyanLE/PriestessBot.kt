package com.heyanle.priestess.bot.provider

import com.heyanle.priestess.bot.provider.adapters.ollama.OllamaProvider
import com.heyanle.priestess.bot.provider.adapters.openai.OpenAIProvider

fun registerBuiltinProviders() {
    ProviderRegistry.register(
        metadata = ProviderMetadata(
            name = "openai",
            displayName = "OpenAI",
            kind = LLMKind.OPENAI,
            supportToolCalling = true,
            supportVision = true,
            supportStreaming = true,
        ),
        factory = { cfg -> OpenAIProvider(cfg) }
    )

    ProviderRegistry.register(
        metadata = ProviderMetadata(
            name = "ollama",
            displayName = "Ollama",
            kind = LLMKind.OLLAMA,
            supportToolCalling = true,
            supportVision = false,
            supportStreaming = true,
        ),
        factory = { cfg -> OllamaProvider(cfg) }
    )
}
