package com.heyanle.priestess.bot.provider.adapters.openai

import com.heyanle.priestess.bot.config.ProviderConfig
import com.heyanle.priestess.bot.provider.*
import com.heyanle.priestess.bot.provider.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.*

/**
 * OpenAI 兼容模型提供者，负责调用 chat completions 和 models 接口。
 */
class OpenAIProvider(
    override val config: ProviderConfig,
    override val metadata: ProviderMetadata = ProviderMetadata(
        name = config.name.ifBlank { "openai" },
        displayName = "OpenAI",
        kind = LLMKind.OPENAI,
        supportToolCalling = true,
        supportVision = true,
        supportStreaming = true,
    ),
    private val client: HttpClient = defaultClient(),
) : ChatProvider {

    private val logger = KotlinLogging.logger {}

    private val configuredUrl: String
        get() = config.baseUrl.ifBlank { "https://api.openai.com/v1" }.trimEnd('/')

    private val chatCompletionsUrl: String
        get() = if (configuredUrl.endsWith("/chat/completions")) {
            configuredUrl
        } else {
            "$configuredUrl/chat/completions"
        }

    private val modelsUrl: String
        get() = if (configuredUrl.endsWith("/chat/completions")) {
            configuredUrl.removeSuffix("/chat/completions") + "/models"
        } else {
            "$configuredUrl/models"
        }

    override suspend fun textChat(request: LLMRequest): LLMResponse {
        val model = request.model.ifBlank { config.model }
        logger.info {
            "[PIPELINE-230] OpenAIProvider request provider=${metadata.name}, model=$model, " +
                "url=$chatCompletionsUrl, messages=${request.messages.size}, tools=${request.tools.size}"
        }
        val response = client.post(chatCompletionsUrl) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${config.resolveApiKey()}")
            setBody(buildJsonObject {
                put("model", model)
                put("messages", buildJsonArray {
                    for (msg in request.messages) {
                        add(buildJsonObject {
                            put("role", msg.role)
                            msg.content?.let { put("content", it) }
                            msg.toolCallId?.let { put("tool_call_id", it) }
                            msg.name?.let { put("name", it) }
                            msg.toolCalls?.let { calls ->
                                put("tool_calls", buildJsonArray {
                                    for (call in calls) {
                                        add(buildJsonObject {
                                            put("id", call.id)
                                            put("type", "function")
                                            putJsonObject("function") {
                                                put("name", call.name)
                                                put("arguments", call.arguments)
                                            }
                                        })
                                    }
                                })
                            }
                        })
                    }
                })
                put("temperature", request.temperature)
                request.maxTokens?.let { put("max_tokens", it) }
                if (request.tools.isNotEmpty()) {
                    put("tools", JsonArray(request.tools))
                }
            })
        }
        val body = readJsonBody(response, "chat completions")
        val parsed = parseResponse(body)
        logger.info {
            "[PIPELINE-239] OpenAIProvider response provider=${metadata.name}, " +
                "finish=${parsed.finishReason}, toolCalls=${parsed.toolCalls.size}, " +
                "contentLength=${parsed.content.length}, totalTokens=${parsed.tokenUsage.totalTokens}"
        }
        return parsed
    }

    override suspend fun getModels(): List<String> {
        val response = client.get(modelsUrl) {
            header("Authorization", "Bearer ${config.resolveApiKey()}")
        }
        val body = readJsonBody(response, "models")
        return body["data"]?.jsonArray
            ?.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.content }
            ?: emptyList()
    }

    override suspend fun test(): Boolean {
        return try { getModels(); true } catch (e: Exception) { false }
    }

    private fun parseResponse(body: JsonObject): LLMResponse {
        val choices = body["choices"]?.jsonArray ?: return LLMResponse()
        val choice = choices.firstOrNull()?.jsonObject ?: return LLMResponse()
        val message = choice["message"]?.jsonObject
        val content = message?.get("content")?.jsonPrimitive?.content ?: ""
        val finishReason = choice["finish_reason"]?.jsonPrimitive?.content ?: ""

        val toolCalls = message?.get("tool_calls")?.jsonArray?.mapNotNull { tc ->
            val obj = tc.jsonObject
            val function = obj["function"]?.jsonObject
            ToolCall(
                id = obj["id"]?.jsonPrimitive?.content ?: "",
                name = function?.get("name")?.jsonPrimitive?.content ?: "",
                arguments = function?.get("arguments")?.jsonPrimitive?.content ?: "",
            )
        } ?: emptyList()

        val usage = body["usage"]?.jsonObject
        val tokenUsage = TokenUsage(
            promptTokens = usage?.get("prompt_tokens")?.jsonPrimitive?.int ?: 0,
            completionTokens = usage?.get("completion_tokens")?.jsonPrimitive?.int ?: 0,
            totalTokens = usage?.get("total_tokens")?.jsonPrimitive?.int ?: 0,
        )
        return LLMResponse(content, toolCalls, finishReason, tokenUsage)
    }

    private suspend fun readJsonBody(response: io.ktor.client.statement.HttpResponse, endpointName: String): JsonObject {
        val status = response.status
        val rawBody = response.bodyAsText()
        val body = runCatching { Json.parseToJsonElement(rawBody).jsonObject }.getOrNull()

        if (!status.isSuccess()) {
            val providerMessage = body?.let(::extractProviderErrorMessage)
            val fallback = rawBody.trim().take(500).ifBlank { "<empty body>" }
            throw IllegalStateException(
                "OpenAI-compatible $endpointName request failed: ${status.value} ${status.description}. " +
                    (providerMessage ?: fallback),
            )
        }

        return body ?: throw IllegalStateException(
            "OpenAI-compatible $endpointName response was not valid JSON " +
                "(status=${status.value}, contentType=${response.contentType() ?: "unknown"}). " +
                rawBody.trim().take(500),
        )
    }

    private fun extractProviderErrorMessage(body: JsonObject): String? {
        val error = body["error"]?.jsonObject ?: return null
        return error["message"]?.jsonPrimitive?.contentOrNull
            ?: error["type"]?.jsonPrimitive?.contentOrNull
            ?: error["code"]?.jsonPrimitive?.contentOrNull
    }

    companion object {
        private fun defaultClient(): HttpClient {
            return HttpClient(CIO) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                install(HttpTimeout) {
                    requestTimeoutMillis = 180_000  // 3 分钟，LLM 复杂请求可能较慢
                    connectTimeoutMillis = 15_000   // 15 秒连接超时
                }
            }
        }
    }
}
