package com.heyanle.priestess.bot.core.config

import kotlinx.serialization.Serializable

@Serializable
data class PriestessConfig(
    val platforms: List<PlatformConfig> = emptyList(),
    val providers: List<ProviderConfig> = emptyList(),
    val agent: AgentConfig = AgentConfig(),
    val database: DatabaseConfig = DatabaseConfig(),
)

@Serializable
data class DatabaseConfig(
    val path: String = "data/priestess.db",
)

@Serializable
data class PlatformConfig(
    val name: String = "",
    val type: String = "",
    val enabled: Boolean = true,
    val host: String = "127.0.0.1",
    val port: Int = 3000,
    val wsPort: Int = 3001,
    val token: String = "",
    val baseUrl: String = "",
    val useWs: Boolean = true,
    val config: Map<String, String> = emptyMap(),
)

@Serializable
data class ProviderConfig(
    val name: String = "",
    val type: String = "",
    val model: String = "",
    val baseUrl: String = "",
    val apiKey: String = "",
    val enabled: Boolean = true,
    val config: Map<String, String> = emptyMap(),
) {
    /**
     * Resolves the API key with priority:
     * 1. Environment variable PRIESTESS_API_KEY_<UPPERCASE_NAME> (e.g. PRIESTESS_API_KEY_OPENAI)
     * 2. Environment variable PRIESTESS_API_KEY (global fallback)
     * 3. Configured [apiKey] value
     *
     * @param envProvider a function to look up environment variables, defaulting to [System.getenv].
     *                    Inject a custom implementation for testing.
     */
    fun resolveApiKey(envProvider: (String) -> String? = System::getenv): String {
        if (name.isNotBlank()) {
            val providerEnvKey = "PRIESTESS_API_KEY_${name.uppercase()}"
            envProvider(providerEnvKey)?.let { return it }
        }
        envProvider("PRIESTESS_API_KEY")?.let { return it }
        return apiKey
    }
}

@Serializable
data class AgentConfig(
    val name: String = "assistant",
    val instructions: String = "You are a helpful assistant.",
    val model: String = "gpt-4o",
    val providerName: String = "openai",
    val maxSteps: Int = 10,
    val temperature: Double = 0.7,
    val compressStrategy: String = "token_window",
    val maxRounds: Int = 20,
    val maxTokens: Int = 4096,
    val toolTimeoutSeconds: Long = 30,
    val enabledTools: List<String> = emptyList(),
)
