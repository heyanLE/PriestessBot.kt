package com.heyanle.priestess.bot.platform.adapters.napcat4_18_6

import com.heyanle.priestess.bot.core.event.EventBus
import com.heyanle.priestess.bot.platform.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*

class NapCatConfig(
    val host: String = "127.0.0.1",
    val port: Int = 3000,
    val wsPort: Int = 3001,
    val useWs: Boolean = true,
    override val name: String = "napcat4_18_6",
    override val displayName: String = "NapCat v4.18.6 (QQ)",
) : AdapterConfig()

class NapCatPlatform(
    eventBus: EventBus,
    private val config: NapCatConfig = NapCatConfig(),
) : Platform(eventBus) {

    override val metadata = PlatformMetadata(
        name = config.name,
        displayName = config.displayName,
        supportStreaming = false,
        supportProactiveMessage = false,
    )

    val baseUrl: String get() = "http://${config.host}:${config.port}"
    val wsUrl: String get() = "ws://${config.host}:${config.wsPort}"

    private var _client: HttpClient? = null
    private val client: HttpClient get() = _client ?: HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(WebSockets)
    }.also { _client = it }

    override suspend fun run(): Job {
        return if (config.useWs) {
            CoroutineScope(Dispatchers.IO).launch { wsListen(wsUrl) }
        } else {
            CoroutineScope(Dispatchers.IO).launch { httpPoll() }
        }
    }

    override suspend fun terminate() {
        _client?.close()
        _client = null
    }

    override suspend fun sendMessage(session: MessageSession, chain: MessageChain) {
        val text = chain.textContent
        require(text.isNotBlank()) { "Message text must not be blank" }
        when (session.type) {
            SessionType.PRIVATE -> {
                client.post("$baseUrl/send_private_msg") {
                    setBody(buildMap {
                        put("user_id", session.id.toLongOrNull() ?: 0L)
                        put("message", listOf(mapOf("type" to "text", "data" to mapOf("text" to text))))
                    })
                }
            }
            SessionType.GROUP -> {
                client.post("$baseUrl/send_group_msg") {
                    setBody(buildMap {
                        put("group_id", session.id.toLongOrNull() ?: 0L)
                        put("message", listOf(mapOf("type" to "text", "data" to mapOf("text" to text))))
                    })
                }
            }
            SessionType.CHANNEL -> { /* QQ has no channel concept */ }
        }
    }

    // ── WebSocket (OneBot 11 forward WS) ──

    private suspend fun wsListen(url: String) {
        while (currentCoroutineContext().isActive) {
            try {
                client.webSocket(url) {
                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                val text = frame.readText()
                                val json = Json.parseToJsonElement(text).jsonObject
                                val event = parseOneBotEvent(json) ?: continue
                                commitEvent(event)
                            }
                            else -> {}
                        }
                    }
                }
            } catch (e: CancellationException) {
                break
            } catch (e: Exception) {
                delay(3000)
            }
        }
    }

    private fun parseOneBotEvent(json: JsonObject): MessageEvent? {
        val postType = json["post_type"]?.jsonPrimitive?.content ?: return null
        if (postType != "message") return null

        val messageType = json["message_type"]?.jsonPrimitive?.content ?: "private"
        val userId = json["user_id"]?.jsonPrimitive?.longOrNull?.toString() ?: return null
        val sessionId = if (messageType == "group") {
            json["group_id"]?.jsonPrimitive?.longOrNull?.toString() ?: userId
        } else {
            userId
        }
        val text = json["raw_message"]?.jsonPrimitive?.content
            ?: json["message"]?.jsonPrimitive?.content
            ?: ""

        return buildMessageEvent(messageType, userId, sessionId, text)
    }

    // ── HTTP polling (fallback) ──

    private suspend fun httpPoll() {
        while (currentCoroutineContext().isActive) {
            try {
                val messages = fetchMessages()
                for (msg in messages) {
                    val event = parseMessage(msg) ?: continue
                    commitEvent(event)
                }
            } catch (e: CancellationException) {
                break
            } catch (e: Exception) {
                delay(3000)
            }
        }
    }

    private suspend fun fetchMessages(): List<JsonObject> {
        try {
            val response = client.get("$baseUrl/get_msg")
            val body = response.body<JsonObject>()
            return body["data"]?.jsonArray?.mapNotNull { it.jsonObject }
                ?: emptyList()
        } catch (e: Exception) {
            return emptyList()
        }
    }

    private fun parseMessage(msg: JsonObject): MessageEvent? {
        val messageType = msg["message_type"]?.jsonPrimitive?.content ?: "private"
        val sender = msg["sender"]?.jsonObject
        val userId = sender?.get("user_id")?.jsonPrimitive?.longOrNull?.toString()
            ?: msg["user_id"]?.jsonPrimitive?.content
            ?: return null
        val sessionId = if (messageType == "group") {
            msg["group_id"]?.jsonPrimitive?.content ?: userId
        } else {
            userId
        }
        val text = msg["raw_message"]?.jsonPrimitive?.content
            ?: msg["message"]?.jsonPrimitive?.content
            ?: ""

        return buildMessageEvent(messageType, userId, sessionId, text)
    }

    // ── Common message event builder ──

    private fun buildMessageEvent(
        messageType: String,
        userId: String,
        sessionId: String,
        text: String,
    ): MessageEvent {
        val sessionType = when (messageType) {
            "private" -> SessionType.PRIVATE
            "group" -> SessionType.GROUP
            else -> SessionType.PRIVATE
        }
        return MessageEvent(
            platform = this,
            session = MessageSession(
                id = sessionId,
                type = sessionType,
                platformName = "napcat4_18_6",
            ),
            chain = MessageChain.text(text),
        )
    }
}
