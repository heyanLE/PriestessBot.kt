package com.heyanle.priestess.bot.platform.adapters.napcat4_18_6

import com.heyanle.priestess.bot.platform.AdapterConfig
import com.heyanle.priestess.bot.platform.MessageChain
import com.heyanle.priestess.bot.platform.MessageEvent
import com.heyanle.priestess.bot.platform.MessageSession
import com.heyanle.priestess.bot.platform.Platform
import com.heyanle.priestess.bot.platform.PlatformMetadata
import com.heyanle.priestess.bot.platform.SessionType
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText
import io.ktor.websocket.send
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class NapCatConfig(
    val host: String = "127.0.0.1",
    val port: Int = 3000,
    val wsPort: Int = 3001,
    val token: String = "",
    val useWs: Boolean = true,
    override val name: String = "napcat4_18_6",
    override val displayName: String = "NapCat v4.18.6 (QQ)",
) : AdapterConfig()

class NapCatPlatform(
    private val config: NapCatConfig = NapCatConfig(),
) : Platform() {

    private val logger = KotlinLogging.logger {}

    override val metadata = PlatformMetadata(
        name = config.name,
        displayName = config.displayName,
        supportStreaming = false,
        supportProactiveMessage = false,
    )

    val baseUrl: String get() = "http://${config.host}:${config.port}"
    val wsUrl: String get() = "ws://${config.host}:${config.wsPort}"

    private var _client: HttpClient? = null
    @Volatile
    private var activeWebSocketSession: WebSocketSession? = null

    private val client: HttpClient get() = _client ?: HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(WebSockets)
    }.also { _client = it }

    override suspend fun run(): Job {
        logger.info {
            "[PIPELINE-001] Starting NapCat platform name=${metadata.name}, ws=${config.useWs}, " +
                "host=${config.host}, port=${config.port}, wsPort=${config.wsPort}, tokenSet=${config.token.isNotBlank()}"
        }
        return if (config.useWs) {
            CoroutineScope(Dispatchers.IO).launch { wsListen(wsUrl) }
        } else {
            CoroutineScope(Dispatchers.IO).launch { httpPoll() }
        }
    }

    override suspend fun terminate() {
        logger.info { "[PIPELINE-009] Terminating NapCat platform name=${metadata.name}" }
        _client?.close()
        _client = null
    }

    override suspend fun sendMessage(session: MessageSession, chain: MessageChain) {
        val text = chain.textContent
        require(text.isNotBlank()) { "Message text must not be blank" }
        when (session.type) {
            SessionType.PRIVATE -> {
                logger.info { "[PIPELINE-301] NapCat sending private message session=${session.id}, length=${text.length}" }
                if (sendViaWebSocket("send_private_msg", "user_id", session.id.toLongOrNull() ?: 0L, text)) {
                    return
                }
                client.post("$baseUrl/send_private_msg") {
                    addAuthHeader()
                    header(HttpHeaders.ContentType, "application/json")
                    setBody(buildJsonObject {
                        put("user_id", session.id.toLongOrNull() ?: 0L)
                        put("message", textMessageArray(text))
                    })
                }
            }
            SessionType.GROUP -> {
                logger.info { "[PIPELINE-302] NapCat sending group message session=${session.id}, length=${text.length}" }
                if (sendViaWebSocket("send_group_msg", "group_id", session.id.toLongOrNull() ?: 0L, text)) {
                    return
                }
                client.post("$baseUrl/send_group_msg") {
                    addAuthHeader()
                    header(HttpHeaders.ContentType, "application/json")
                    setBody(buildJsonObject {
                        put("group_id", session.id.toLongOrNull() ?: 0L)
                        put("message", textMessageArray(text))
                    })
                }
            }
            SessionType.CHANNEL -> Unit
        }
    }

    // WebSocket (OneBot 11 forward WS)
    private suspend fun wsListen(url: String) {
        while (currentCoroutineContext().isActive) {
            try {
                logger.info { "[PIPELINE-002] NapCat connecting websocket url=$url" }
                client.webSocket(
                    request = {
                        url(url)
                        addAuthHeader()
                    },
                ) {
                    logger.info { "[PIPELINE-003] NapCat websocket connected url=$url" }
                    activeWebSocketSession = this
                    try {
                        for (frame in incoming) {
                            when (frame) {
                                is Frame.Text -> {
                                    val text = frame.readText()
                                    logger.info { "[PIPELINE-010] NapCat received frame length=${text.length}" }
                                    val json = Json.parseToJsonElement(text).jsonObject
                                    val event = NapCatEventParser.parseOneBotEvent(this@NapCatPlatform, json) ?: continue
                                    logger.info {
                                        "[PIPELINE-011] NapCat parsed message session=${event.session.id}, " +
                                            "type=${event.session.type}, source=${event.sourceId}, text='${event.chain.textContent.take(120)}'"
                                    }
                                    commitEvent(event)
                                    logger.info {
                                        "[PIPELINE-019] NapCat committed message session=${event.session.id}, source=${event.sourceId}"
                                    }
                                }
                                else -> Unit
                            }
                        }
                    } finally {
                        if (activeWebSocketSession === this) {
                            activeWebSocketSession = null
                        }
                    }
                }
            } catch (e: CancellationException) {
                break
            } catch (e: Exception) {
                logger.warn(e) { "[PIPELINE-902] NapCat websocket failed, retrying in 3000ms" }
                delay(3000)
            }
        }
    }

    // HTTP polling (fallback)
    private suspend fun httpPoll() {
        while (currentCoroutineContext().isActive) {
            try {
                val messages = fetchMessages()
                for (msg in messages) {
                    val event = NapCatEventParser.parseHttpMessage(this@NapCatPlatform, msg) ?: continue
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
            val response = client.get("$baseUrl/get_msg") {
                addAuthHeader()
            }
            val body = response.body<JsonObject>()
            return body["data"]?.jsonArray?.mapNotNull { it.jsonObject }
                ?: emptyList()
        } catch (e: Exception) {
            return emptyList()
        }
    }

    private fun HttpRequestBuilder.addAuthHeader() {
        if (config.token.isNotBlank()) {
            header(HttpHeaders.Authorization, "Bearer ${config.token}")
        }
    }

    private fun textMessageArray(text: String) = buildJsonArray {
        add(
            buildJsonObject {
                put("type", "text")
                putJsonObject("data") {
                    put("text", text)
                }
            },
        )
    }

    private suspend fun sendViaWebSocket(action: String, targetKey: String, targetId: Long, text: String): Boolean {
        val ws = activeWebSocketSession ?: return false
        val echo = "priestess-send-${System.currentTimeMillis()}"
        val payload = buildJsonObject {
            put("action", action)
            putJsonObject("params") {
                put(targetKey, targetId)
                put("message", textMessageArray(text))
            }
            put("echo", echo)
        }
        return try {
            logger.info { "[PIPELINE-303] NapCat sending websocket action=$action, $targetKey=$targetId, echo=$echo" }
            ws.send(payload.toString())
            true
        } catch (e: Exception) {
            logger.warn(e) { "[PIPELINE-903] NapCat websocket send failed, falling back to HTTP action=$action" }
            false
        }
    }

}
