package com.heyanle.priestess.bot.platform.adapters.telegram

import com.heyanle.priestess.bot.platform.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
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

    override suspend fun sendMessage(session: MessageSession, chain: MessageChain): String? {
        val rawText = chain.textContent
        require(rawText.isNotBlank()) { "Message text must not be blank" }
        val htmlText = markdownToTelegramHtml(rawText)
        val response = client.post("$baseUrl/sendMessage") {
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("chat_id", session.id)
                    put("text", htmlText)
                    put("parse_mode", "HTML")
                },
            )
        }
        val body = response.body<JsonObject>()
        val ok = body["ok"]?.jsonPrimitive?.booleanOrNull ?: true
        if (!ok) {
            // HTML 解析失败，回退纯文本
            val fallbackResponse = client.post("$baseUrl/sendMessage") {
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("chat_id", session.id)
                        put("text", rawText)
                    },
                )
            }
            val fallbackBody = fallbackResponse.body<JsonObject>()
            return fallbackBody["result"]?.jsonObject?.get("message_id")?.jsonPrimitive?.content
        }
        return body["result"]?.jsonObject?.get("message_id")?.jsonPrimitive?.content
    }

    /**
     * 将 LLM 常见的 Markdown 语法转为 Telegram HTML 格式。
     * 转换顺序：
     *   HTML 转义 → 代码块 → 行内代码 → 链接 → 粗体 → 斜体 → 删除线 → 下划线 → 剧透
     * 必须在粗体/斜体之前处理链接，避免 URL 中的特殊字符被误格式化。
     */
    private fun markdownToTelegramHtml(text: String): String {
        return text
            // 1. 转义 HTML 保留字符（必须在所有格式化之前）
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            // 2. 代码块 ```...```（必须在行内代码之前）
            .replace(Regex("```(\\w*)\\n?([\\s\\S]*?)```")) { "<pre>${it.groupValues[2].trim()}</pre>" }
            // 3. 行内代码 `...`
            .replace(Regex("`([^`]+)`")) { "<code>${it.groupValues[1]}</code>" }
            // 4. 链接 [text](url)（在粗体/斜体之前，避免 URL 特殊字符被格式化）
            .replace(Regex("\\[([^\\]]+)\\]\\(([^)]+)\\)")) { "<a href=\"${it.groupValues[2]}\">${it.groupValues[1]}</a>" }
            // 5. 粗体 **text**
            .replace(Regex("\\*\\*(.+?)\\*\\*")) { "<b>${it.groupValues[1]}</b>" }
            // 6. 斜体 *text*（排除 ** 双星号）
            .replace(Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)")) { "<i>${it.groupValues[1]}</i>" }
            // 7. 删除线 ~~text~~
            .replace(Regex("~~(.+?)~~")) { "<s>${it.groupValues[1]}</s>" }
            // 8. 下划线 __text__（__ 与 ** 不冲突，分属不同字符）
            .replace(Regex("__(.+?)__")) { "<u>${it.groupValues[1]}</u>" }
            // 9. 剧透 ||text||（Telegram 特有）
            .replace(Regex("\\|\\|(.+?)\\|\\|")) { "<tg-spoiler>${it.groupValues[1]}</tg-spoiler>" }
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
