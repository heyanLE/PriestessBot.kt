package com.heyanle.priestess.bot.agent.orchestration

import com.heyanle.priestess.bot.agent.AgentCase
import com.heyanle.priestess.bot.config.AgentConfig
import com.heyanle.priestess.bot.config.ConfigCase
import com.heyanle.priestess.bot.config.ConfigController
import com.heyanle.priestess.bot.config.PriestessConfig
import com.heyanle.priestess.bot.config.ProviderConfig
import com.heyanle.priestess.bot.config.SubAgentConfig
import com.heyanle.priestess.bot.config.SubAgentOrchestrationConfig
import com.heyanle.priestess.bot.config.SubAgentRouteConfig
import com.heyanle.priestess.bot.provider.ChatProvider
import com.heyanle.priestess.bot.provider.LLMKind
import com.heyanle.priestess.bot.provider.ProviderCase
import com.heyanle.priestess.bot.provider.ProviderController
import com.heyanle.priestess.bot.provider.ProviderMetadata
import com.heyanle.priestess.bot.provider.ProviderRegistry
import com.heyanle.priestess.bot.provider.model.LLMRequest
import com.heyanle.priestess.bot.provider.model.LLMResponse
import com.heyanle.priestess.bot.tool.ToolCase
import com.heyanle.priestess.bot.tool.ToolController
import com.heyanle.priestess.bot.tool.ToolExecutor
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class SubAgentOrchestratorTest {
    @Test
    fun `selects sub-agent by keyword route priority`() {
        val orchestrator = testOrchestrator()
        val config = testConfig()

        val selection = orchestrator.select("please review this code", primaryAgent(), config)

        assertEquals("code-agent", selection.agentName)
        assertEquals("code-route", selection.routeName)
        assertEquals("keyword_match", selection.reason)
    }

    @Test
    fun `falls back to default sub-agent when no route matches`() {
        val orchestrator = testOrchestrator()
        val config = testConfig()

        val selection = orchestrator.select("hello there", primaryAgent(), config)

        assertEquals("general-agent", selection.agentName)
        assertEquals(null, selection.routeName)
        assertEquals("default_agent", selection.reason)
    }

    @Test
    fun `runs selected sub-agent through react runtime`() = runBlocking {
        val provider = ScriptedProvider("sub-agent reply")
        val orchestrator = testOrchestrator(provider)

        val result = orchestrator.run("please review this code", primaryAgent(), testConfig())

        assertEquals("FINAL", result.status)
        assertEquals("code-agent", result.selection.agentName)
        assertEquals("sub-agent reply", result.content)
    }

    private fun testConfig(): SubAgentOrchestrationConfig {
        return SubAgentOrchestrationConfig(
            enabled = true,
            defaultAgentName = "general-agent",
            agents = listOf(
                SubAgentConfig(
                    name = "general-agent",
                    agent = AgentConfig(name = "general-agent", providerName = "test-provider", model = "test-model"),
                ),
                SubAgentConfig(
                    name = "code-agent",
                    agent = AgentConfig(name = "code-agent", providerName = "test-provider", model = "test-model"),
                ),
            ),
            routes = listOf(
                SubAgentRouteConfig(name = "code-route", targetAgentName = "code-agent", keywords = listOf("code", "review"), priority = 10),
            ),
        )
    }

    private fun primaryAgent() = AgentConfig(name = "primary", providerName = "test-provider", model = "test-model")

    private fun testOrchestrator(provider: ScriptedProvider = ScriptedProvider("ok")): SubAgentOrchestrator {
        ProviderRegistry.unregister("test-provider")
        ProviderRegistry.register(provider.metadata) { provider }
        val configPath = Files.createTempDirectory("sub-agent-config").resolve("config.json")
        val configController = ConfigController(configPath.toString())
        configController.replace(
            PriestessConfig(
                providers = listOf(ProviderConfig(name = "test-provider", type = "test-provider", model = "test-model")),
            ),
        )
        val providerCase = ProviderCase(ProviderController(ConfigCase(configController)))
        val toolController = ToolController()
        return SubAgentOrchestrator(
            agentCase = AgentCase(),
            providerCase = providerCase,
            toolCase = ToolCase(toolController),
        )
    }

    private class ScriptedProvider(
        private val content: String,
    ) : ChatProvider {
        override val metadata = ProviderMetadata(
            name = "test-provider",
            displayName = "Test Provider",
            kind = LLMKind.OPENAI,
            supportToolCalling = false,
            supportVision = false,
            supportStreaming = false,
        )
        override val config = ProviderConfig(name = "test-provider", type = "test-provider", model = "test-model")

        override suspend fun test(): Boolean = true

        override suspend fun textChat(request: LLMRequest): LLMResponse = LLMResponse(content = content, finishReason = "stop")

        override suspend fun getModels(): List<String> = listOf("test-model")
    }
}
