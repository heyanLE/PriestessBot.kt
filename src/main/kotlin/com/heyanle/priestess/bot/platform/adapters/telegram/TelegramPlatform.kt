package com.heyanle.priestess.bot.platform.adapters.telegram

import com.heyanle.priestess.bot.platform.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.channels.UnresolvedAddressException

class TelegramConfig(
    val token: String = "",
    override val name: String = "telegram",
    override val displayName: String = "Telegram",
) : AdapterConfig()

class TelegramPlatform(
    private val config: TelegramConfig = TelegramConfig(),
    private val clientFactory: () -> HttpClient = { createDefaultClient() },
) : Platform() {

    private val logger = KotlinLogging.logger {}

    override val metadata = PlatformMetadata(
        name = config.name,
        displayName = config.displayName,
        supportStreaming = true,
        supportProactiveMessage = true,
    )

    private var offset: Long = 0
    private val baseUrl: String get() = "https://api.telegram.org/bot${config.token}"
    @Volatile private var _client: HttpClient? = null
    private val client: HttpClient get() = _client ?: clientFactory().also { _client = it }

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

    override suspend fun sendMessage(session: MessageSession, chain: MessageChain): String? {
        val rawText = chain.textContent
        require(rawText.isNotBlank()) { "Message text must not be blank" }
        var firstMessageId: String? = null
        for (chunk in TelegramMessageChunker.split(rawText, RICH_MESSAGE_MAX_CODE_POINTS)) {
            val body = callTelegram("sendRichMessage", buildJsonObject {
                put("chat_id", session.id)
                put("rich_message", buildJsonObject { put("markdown", chunk) })
            })

            if (body.isSuccessful()) {
                firstMessageId = firstMessageId ?: body.messageId()
                continue
            }

            logApiFailure("sendRichMessage", body)
            firstMessageId = firstMessageId ?: sendPlainText(session, chunk)
        }
        return firstMessageId
    }

    /**
     * Rich Messages 仅在较新的 Telegram Bot API 中可用。不可用或内容被拒绝时，
     * 以普通文本作为可靠降级，避免旧的 HTML 正则转换再次损坏消息内容。
     */
    private suspend fun sendPlainText(session: MessageSession, text: String): String? {
        var firstMessageId: String? = null
        for (chunk in TelegramMessageChunker.split(text, BASIC_MESSAGE_MAX_CODE_POINTS)) {
            val body = callTelegram("sendMessage", buildJsonObject {
                put("chat_id", session.id)
                put("text", chunk)
            })
            if (!body.isSuccessful()) {
                logApiFailure("sendMessage", body)
                throw TelegramApiException("sendMessage", body.errorDescription())
            }
            firstMessageId = firstMessageId ?: body.messageId()
        }
        return firstMessageId
    }

    private suspend fun callTelegram(method: String, request: JsonObject): JsonObject =
        postTelegram(method, request).body()

    private fun JsonObject.isSuccessful(): Boolean =
        this["ok"]?.jsonPrimitive?.booleanOrNull == true

    private fun JsonObject.messageId(): String? =
        this["result"]?.jsonObject?.get("message_id")?.jsonPrimitive?.content

    private fun JsonObject.errorDescription(): String =
        this["description"]?.jsonPrimitive?.content ?: "Telegram API returned an unsuccessful response"

    private fun logApiFailure(method: String, body: JsonObject) {
        val errorCode = body["error_code"]?.jsonPrimitive?.content ?: "unknown"
        logger.warn { "Telegram API call failed method=$method errorCode=$errorCode description=${body.errorDescription()}" }
    }

    private suspend fun postTelegram(method: String, body: JsonObject): HttpResponse {
        var lastFailure: UnresolvedAddressException? = null
        repeat(3) { attempt ->
            try {
                return client.post("$baseUrl/$method") {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            } catch (error: UnresolvedAddressException) {
                lastFailure = error
                if (attempt < 2) delay((attempt + 1) * 1_000L)
            }
        }
        throw requireNotNull(lastFailure)
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

    internal fun parseUpdate(update: JsonObject): MessageEvent? {
        val message = update["message"]?.jsonObject ?: update["channel_post"]?.jsonObject ?: return null
        val chat = message["chat"]?.jsonObject ?: return null
        val chatId = chat["id"]?.jsonPrimitive?.content ?: return null
        val senderId = message["from"]?.jsonObject?.get("id")?.jsonPrimitive?.content
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
                metadata = chat.mapValues { it.value.jsonPrimitive.content } + buildMap {
                    senderId?.let {
                        put("senderId", it)
                        put("userId", it)
                    }
                },
            ),
            chain = MessageChain.text(text),
        )
    }

    private class TelegramApiException(method: String, description: String) :
        IllegalStateException("Telegram API call failed method=$method description=$description")

    private companion object {
        const val RICH_MESSAGE_MAX_CODE_POINTS = 32_768
        const val BASIC_MESSAGE_MAX_CODE_POINTS = 4_096

        fun createDefaultClient(): HttpClient = HttpClient(CIO) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    }
}
