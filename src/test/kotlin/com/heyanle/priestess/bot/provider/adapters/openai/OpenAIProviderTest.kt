package com.heyanle.priestess.bot.provider.adapters.openai

import com.heyanle.priestess.bot.config.ProviderConfig
import com.heyanle.priestess.bot.provider.model.ConversationMessage
import com.heyanle.priestess.bot.provider.model.LLMRequest
import com.heyanle.priestess.bot.provider.model.ToolCall
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.content.TextContent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OpenAIProviderTest {

    @Test
    fun `textChat accepts full chat completions url and serializes assistant tool calls`() = runBlocking {
        var capturedUrl = ""
        var capturedBody = ""
        val client = HttpClient(
            MockEngine { request ->
                capturedUrl = request.url.toString()
                capturedBody = (request.body as TextContent).text
                respond(
                    content = """
                        {
                          "choices": [
                            {
                              "message": {
                                "content": "ok"
                              },
                              "finish_reason": "stop"
                            }
                          ],
                          "usage": {
                            "prompt_tokens": 1,
                            "completion_tokens": 1,
                            "total_tokens": 2
                          }
                        }
                    """.trimIndent(),
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        ) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val provider = OpenAIProvider(
            config = ProviderConfig(
                name = "deepseek-v4-flash",
                type = "openai",
                model = "deepseek-v4-flash",
                baseUrl = "http://192.168.31.24:8090/v1/chat/completions",
                apiKey = "test-key",
            ),
            client = client,
        )

        val response = provider.textChat(
            LLMRequest(
                messages = listOf(
                    ConversationMessage.user("call a tool"),
                    ConversationMessage.assistant(
                        content = "tool please",
                        toolCalls = listOf(
                            ToolCall(
                                id = "call-1",
                                name = "lookup",
                                arguments = """{"query":"ping"}""",
                            ),
                        ),
                    ),
                    ConversationMessage.tool(
                        toolCallId = "call-1",
                        name = "lookup",
                        content = "pong",
                    ),
                ),
            ),
        )

        val body = Json.parseToJsonElement(capturedBody).jsonObject
        val assistant = body["messages"]!!.jsonArray[1].jsonObject
        val toolCall = assistant["tool_calls"]!!.jsonArray.single().jsonObject
        val toolResult = body["messages"]!!.jsonArray[2].jsonObject

        assertEquals("http://192.168.31.24:8090/v1/chat/completions", capturedUrl)
        assertEquals("deepseek-v4-flash", body["model"]!!.jsonPrimitive.content)
        assertEquals("call-1", toolCall["id"]!!.jsonPrimitive.content)
        assertEquals("function", toolCall["type"]!!.jsonPrimitive.content)
        assertEquals("lookup", toolCall["function"]!!.jsonObject["name"]!!.jsonPrimitive.content)
        assertEquals("""{"query":"ping"}""", toolCall["function"]!!.jsonObject["arguments"]!!.jsonPrimitive.content)
        assertEquals("tool", toolResult["role"]!!.jsonPrimitive.content)
        assertEquals("call-1", toolResult["tool_call_id"]!!.jsonPrimitive.content)
        assertEquals("pong", toolResult["content"]!!.jsonPrimitive.content)
        assertEquals("ok", response.content)
        assertEquals("stop", response.finishReason)
        assertEquals(2, response.tokenUsage.totalTokens)
    }

    @Test
    fun `textChat surfaces non json error bodies as readable exceptions`() = runBlocking {
        val client = HttpClient(
            MockEngine {
                respond(
                    content = "gateway rejected request",
                    status = HttpStatusCode.BadRequest,
                    headers = headersOf(HttpHeaders.ContentType, "application/octet-stream"),
                )
            },
        ) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val provider = OpenAIProvider(
            config = ProviderConfig(
                name = "deepseek-v4-flash",
                type = "openai",
                model = "deepseek-v4-flash",
                baseUrl = "https://api.deepseek.com",
                apiKey = "test-key",
            ),
            client = client,
        )

        val error = assertFailsWith<IllegalStateException> {
            provider.textChat(LLMRequest(messages = listOf(ConversationMessage.user("hello"))))
        }

        assertTrue(error.message!!.contains("400"))
        assertTrue(error.message!!.contains("gateway rejected request"))
    }
}
