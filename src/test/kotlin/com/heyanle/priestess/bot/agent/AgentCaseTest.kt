package com.heyanle.priestess.bot.agent

import com.heyanle.priestess.bot.config.AgentConfig
import kotlin.test.Test
import kotlin.test.assertEquals

class AgentCaseTest {
    @Test
    fun `creates runtime agent from config without changing mapping semantics`() {
        val case = AgentCase(AgentController())

        val agent = case.createAgent(
            AgentConfig(
                name = "code-agent",
                instructions = "Answer carefully",
                providerName = "provider-a",
                model = "model-a",
                maxSteps = 7,
                toolTimeoutSeconds = 12,
                compressStrategy = "token_window",
                maxTokens = 2048,
                maxRounds = 9,
            ),
        )

        assertEquals("code-agent", agent.name)
        assertEquals("Answer carefully", agent.instructions)
        assertEquals("model-a", agent.model)
        assertEquals(7, agent.maxSteps)
        assertEquals(12_000L, agent.toolTimeoutMs)
        assertEquals(CompressStrategy.TOKEN_WINDOW, agent.compressStrategy)
        assertEquals(2048, agent.maxContextTokens)
        assertEquals(9, agent.maxContextRounds)
    }

    @Test
    fun `uses llm compression and falls back unknown strategy to round truncation`() {
        val case = AgentCase(AgentController())

        val llmAgent = case.createAgent(agentConfig("llm_compress"))
        val fallbackAgent = case.createAgent(agentConfig("unknown"))

        assertEquals(CompressStrategy.LLM_COMPRESS, llmAgent.compressStrategy)
        assertEquals(CompressStrategy.ROUND_TRUNCATION, fallbackAgent.compressStrategy)
    }

    private fun agentConfig(strategy: String): AgentConfig {
        return AgentConfig(
            name = "agent-$strategy",
            providerName = "provider-a",
            model = "model-a",
            compressStrategy = strategy,
        )
    }
}
