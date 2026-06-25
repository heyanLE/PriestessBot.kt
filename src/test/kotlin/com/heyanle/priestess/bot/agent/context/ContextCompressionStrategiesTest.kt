package com.heyanle.priestess.bot.agent.context

import com.heyanle.priestess.bot.agent.CompressStrategy
import com.heyanle.priestess.bot.provider.model.ConversationMessage
import com.heyanle.priestess.bot.provider.model.ToolCall
import com.heyanle.priestess.bot.testkit.testAgent
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContextCompressionStrategiesTest {
    private val tokenCounter = TokenCounter()

    @Test
    fun `round truncation keeps system message and latest rounds`() = runBlocking {
        val system = ConversationMessage.system("system")
        val messages = mutableListOf(system)
        repeat(4) { index ->
            messages += ConversationMessage.user("user-$index")
            messages += ConversationMessage.assistant("assistant-$index")
        }

        val result = ContextManager(tokenCounter).compress(
            agent = testAgent().copy(compressStrategy = CompressStrategy.ROUND_TRUNCATION, maxContextRounds = 2),
            messages = messages,
            systemMessage = system,
        )

        assertEquals("system", result.first().role)
        assertEquals(listOf("user-2", "assistant-2", "user-3", "assistant-3"), result.drop(1).map { it.content })
    }

    @Test
    fun `token window trims older messages and preserves system message`() = runBlocking {
        val system = ConversationMessage.system("system")
        val messages = listOf(
            system,
            ConversationMessage.user("older message with many many many tokens"),
            ConversationMessage.assistant("older response with many many many tokens"),
            ConversationMessage.user("new"),
            ConversationMessage.assistant("ok"),
        )

        val result = ContextManager(tokenCounter).compress(
            agent = testAgent().copy(compressStrategy = CompressStrategy.TOKEN_WINDOW, maxContextTokens = 15),
            messages = messages,
            systemMessage = system,
        )

        assertEquals("system", result.first().role)
        assertTrue(result.any { it.content == "new" })
        assertTrue(result.any { it.content == "ok" })
        assertTrue(result.none { it.content?.startsWith("older") == true })
    }

    @Test
    fun `llm compression fallback preserves recent tool observation with system message`() = runBlocking {
        val system = ConversationMessage.system("system")
        val toolCall = ToolCall(id = "call-1", name = "fake_tool", arguments = """{"value":"abc"}""")
        val messages = listOf(
            system,
            ConversationMessage.user("old old old old old old"),
            ConversationMessage.assistant("old response"),
            ConversationMessage.user("new"),
            ConversationMessage.assistant(content = "", toolCalls = listOf(toolCall)),
            ConversationMessage.tool(toolCallId = "call-1", name = "fake_tool", content = "observation"),
        )

        val result = ContextManager(tokenCounter).compress(
            agent = testAgent().copy(compressStrategy = CompressStrategy.LLM_COMPRESS, maxContextTokens = 25),
            messages = messages,
            systemMessage = system,
        )

        assertEquals("system", result.first().role)
        assertTrue(result.any { it.role == "assistant" && it.toolCalls?.firstOrNull()?.id == "call-1" })
        assertTrue(result.any { it.role == "tool" && it.toolCallId == "call-1" && it.content == "observation" })
    }
}
