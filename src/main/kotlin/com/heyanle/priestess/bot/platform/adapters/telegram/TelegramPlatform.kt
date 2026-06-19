package com.heyanle.priestess.bot.platform.adapters.telegram

import com.heyanle.priestess.bot.platform.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*

class TelegramConfig(
    val token: String = "",
    override val name: String = "telegram",
    override val displayName: String = "Telegram",
) : AdapterConfig()

class TelegramPlatform(
    private val config: TelegramConfig = TelegramConfig(),
) : Platform() {

    override val metadata = PlatformMetadata(
        name = config.name,
        displayName = config.displayName,
        supportStreaming = true,
        supportProactiveMessage = true,
    )

    private var offset: Long = 0
    private val baseUrl: String get() = "https://api.telegram.org/bot${config.token}"
    @Volatile private var _client: HttpClient? = null
    private val client: HttpClient get() = _client ?: HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }.also { _client = it }

    override suspend fun run(): Job {
        require(config.token.isNotBlank()) { "Telegram bot token must be configured" }
        return CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    val updates = fetchUpdates()
                    for (update in updates) {
                        val event = parseUpdate(update) ?: continue
                        commitEvent(event)
                    }
                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    delay(5000)
                }
            }
        }
    }

    override suspend fun terminate() {
        _client?.close()
        _client = null
    }

    override suspend fun sendMessage(session: MessageSession, chain: MessageChain) {
        val text = chain.textContent
        require(text.isNotBlank()) { "Message text must not be blank" }
        client.post("$baseUrl/sendMessage") {
            setBody(mapOf("chat_id" to session.id, "text" to text))
        }
    }

    private suspend fun fetchUpdates(): List<JsonObject> {
        val response = client.get("$baseUrl/getUpdates") {
            parameter("offset", offset + 1)
            parameter("timeout", 30)
        }
        val body = response.body<JsonObject>()
        val updates = body["result"]?.jsonArray ?: return emptyList()
        for (update in updates) {
            val id = update.jsonObject["update_id"]?.jsonPrimitive?.long ?: continue
            if (id > offset) offset = id
        }
        return updates.mapNotNull { it.jsonObject }
    }

    private fun parseUpdate(update: JsonObject): MessageEvent? {
        val message = update["message"]?.jsonObject ?: update["channel_post"]?.jsonObject ?: return null
        val chat = message["chat"]?.jsonObject ?: return null
        val chatId = chat["id"]?.jsonPrimitive?.content ?: return null
        val chatType = when (chat["type"]?.jsonPrimitive?.content) {
            "private" -> SessionType.PRIVATE
            "group", "supergroup" -> SessionType.GROUP
            "channel" -> SessionType.CHANNEL
            else -> SessionType.PRIVATE
        }
        val text = message["text"]?.jsonPrimitive?.content ?: ""

        return MessageEvent(
            platform = this,
            session = MessageSession(
                id = chatId,
                type = chatType,
                platformName = metadata.name,
                metadata = chat.mapValues { it.value.jsonPrimitive.content },
            ),
            chain = MessageChain.text(text),
        )
    }
}
