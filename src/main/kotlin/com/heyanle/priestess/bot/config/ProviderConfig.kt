package com.heyanle.priestess.bot.config

import kotlinx.serialization.Serializable

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
    fun resolveApiKey(envProvider: (String) -> String? = System::getenv): String {
        if (name.isNotBlank()) {
            envProvider("PRIESTESS_API_KEY_${name.uppercase()}")?.let { return it }
        }
        envProvider("PRIESTESS_API_KEY")?.let { return it }
        return apiKey
    }
}
