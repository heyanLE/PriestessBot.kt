package com.heyanle.priestess.bot.pipeline.stages

import com.heyanle.priestess.bot.agent.AgentResponse
import com.heyanle.priestess.bot.agent.context.ContextManager
import com.heyanle.priestess.bot.agent.context.TokenCounter
import com.heyanle.priestess.bot.observability.MetricsRegistry
import com.heyanle.priestess.bot.provider.ChatProvider
import com.heyanle.priestess.bot.provider.ProviderCase
import com.heyanle.priestess.bot.provider.ProviderController
import com.heyanle.priestess.bot.provider.ProviderMetadata
import com.heyanle.priestess.bot.provider.model.LLMRequest
import com.heyanle.priestess.bot.provider.model.LLMResponse
import com.heyanle.priestess.bot.testkit.FakeProvider
import com.heyanle.priestess.bot.testkit.testAgentContext
import com.heyanle.priestess.bot.testkit.testConfigCase
import com.heyanle.priestess.bot.testkit.testPipelineContext
import com.heyanle.priestess.bot.tool.ToolController
import com.heyanle.priestess.bot.tool.ToolExecutor
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProcessStageTest {
    @Test
    fun `missing agent context returns error without stopping pipeline`() = runBlocking {
        val ctx = testPipelineContext()

        val flow = stage().process(ctx)

        val response = assertIs<AgentResponse.Error>(ctx.agentResponse)
        assertEquals("AgentContext not initialized", response.message)
        assertFalse(ctx.isStopped)
        assertEquals(null, flow)
    }

    @Test
    fun `missing provider returns user visible agent error`() = runBlocking {
        val ctx = testPipelineContext(text = "hello")
        ctx.agentContext = testAgentContext(messages = mutableListOf())

        stage().process(ctx)

        val response = assertIs<AgentResponse.Error>(ctx.agentResponse)
        assertEquals("No LLM provider available", response.message)
    }

    @Test
    fun `successful process appends user message produces final response and records llm metrics`() = runBlocking {
        val provider = FakeProvider(listOf(LLMResponse(content = "final from provider", finishReason = "stop")))
        val providerController = ProviderController(testConfigCase())
        providerController.register(provider)
        val metrics = MetricsRegistry()
        val ctx = testPipelineContext(text = "current question")
        ctx.agentContext = testAgentContext(messages = mutableListOf())

        val flow = stage(providerController, metrics).process(ctx)
        assertNotNull(flow).collect()

        val response = assertIs<AgentResponse.Final>(ctx.agentResponse)
        assertEquals("final from provider", response.content)
        assertTrue(provider.requests.single().messages.any { it.role == "user" && it.content == "current question" })

        val rendered = metrics.renderPrometheus()
        assertTrue(rendered.contains("""priestess_llm_requests_total{provider="fake-provider",status="success"} 1"""))
        assertFalse(rendered.contains("current question"))
    }

    @Test
    fun `uses workspace provider metadata before legacy model lookup`() = runBlocking {
        val defaultProvider = NamedProvider("default-provider", "default response")
        val workspaceProvider = NamedProvider("workspace-provider", "workspace response")
        val providerController = ProviderController(testConfigCase())
        providerController.register(defaultProvider)
        providerController.register(workspaceProvider)
        val ctx = testPipelineContext(text = "current question")
        ctx.agentContext = testAgentContext(
            messages = mutableListOf(),
            metadata = mapOf("provider_name" to "workspace-provider"),
        )

        stage(providerController).process(ctx)

        val response = assertIs<AgentResponse.Final>(ctx.agentResponse)
        assertEquals("workspace response", response.content)
        assertEquals(1, workspaceProvider.requests.size)
        assertEquals(0, defaultProvider.requests.size)
    }

    private fun stage(
        providerController: ProviderController = ProviderController(testConfigCase()),
        metrics: MetricsRegistry = MetricsRegistry(),
    ): ProcessStage {
        val toolController = ToolController()
        return ProcessStage(
            providerCase = ProviderCase(providerController),
            toolExecutor = ToolExecutor(toolController),
            toolController = toolController,
            contextManager = ContextManager(TokenCounter()),
            metricsRegistry = metrics,
        )
    }

    private class NamedProvider(
        name: String,
        private val content: String,
    ) : ChatProvider by FakeProvider(listOf(LLMResponse(content = content, finishReason = "stop"))) {
        private val delegate = FakeProvider(listOf(LLMResponse(content = content, finishReason = "stop")))
        val requests: List<LLMRequest>
            get() = delegate.requests

        override val metadata: ProviderMetadata = delegate.metadata.copy(name = name, displayName = name)
        override val config = delegate.config.copy(name = name)

        override suspend fun textChat(request: LLMRequest): LLMResponse = delegate.textChat(request)
        override suspend fun test(): Boolean = delegate.test()
        override suspend fun getModels(): List<String> = delegate.getModels()
    }
}
