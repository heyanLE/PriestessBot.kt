package com.heyanle.priestess.bot.agent.context

import com.heyanle.priestess.bot.agent.AgentCase
import com.heyanle.priestess.bot.config.AgentConfig
import com.heyanle.priestess.bot.provider.model.ConversationMessage
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LLMCompressStrategyTest {
    private val tokenCounter = TokenCounter()

    @Test
    fun `llm compress strategy keeps name and falls back safely`() = runBlocking {
        val strategy = LLMCompressStrategy(tokenCounter)
        val system = ConversationMessage.system("system guardrails")
        val messages = longHistory()

        val compressed = strategy.compress(
            messages = messages,
            systemMessage = system,
            maxTokens = 24,
            maxRounds = 2,
        )

        assertEquals("llm_compress", strategy.name)
        assertEquals(system, compressed.first())
        assertTrue(compressed.size < messages.size + 1)
        assertTrue(compressed.any { it.content == "latest answer" })
    }

    @Test
    fun `context manager executes configured llm compress strategy`() = runBlocking {
        val agent = AgentCase().createAgent(
            AgentConfig(
                compressStrategy = "llm_compress",
                maxTokens = 24,
                maxRounds = 2,
            ),
        )
        val system = ConversationMessage.system("system guardrails")
        val messages = longHistory()
        val manager = ContextManager(tokenCounter)

        val compressed = manager.compress(agent, messages, system)

        assertEquals(system, compressed.first())
        assertTrue(compressed.size < messages.size + 1)
        assertTrue(compressed.any { it.content == "latest answer" })
    }

    private fun longHistory(): List<ConversationMessage> {
        return listOf(
            ConversationMessage.user("old request with a lot of words that should be trimmed"),
            ConversationMessage.assistant("old answer with a lot of words that should be trimmed"),
            ConversationMessage.user("middle request with a lot of words that should be trimmed"),
            ConversationMessage.assistant("middle answer with a lot of words that should be trimmed"),
            ConversationMessage.user("latest request"),
            ConversationMessage.assistant("latest answer"),
        )
    }
}
