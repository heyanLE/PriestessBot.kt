package com.heyanle.priestess.bot.pipeline

import com.heyanle.priestess.bot.agent.AgentCase
import com.heyanle.priestess.bot.agent.context.ContextManager
import com.heyanle.priestess.bot.agent.context.TokenCounter
import com.heyanle.priestess.bot.config.AgentConfig
import com.heyanle.priestess.bot.config.PipelineConfig
import com.heyanle.priestess.bot.conversation.MessageRole
import com.heyanle.priestess.bot.observability.MetricsRegistry
import com.heyanle.priestess.bot.pipeline.stages.PreProcessStage
import com.heyanle.priestess.bot.pipeline.stages.ProcessStage
import com.heyanle.priestess.bot.pipeline.stages.RespondStage
import com.heyanle.priestess.bot.pipeline.stages.ResultDecorateStage
import com.heyanle.priestess.bot.config.ConfigCase
import com.heyanle.priestess.bot.config.ConfigController
import com.heyanle.priestess.bot.provider.ProviderCase
import com.heyanle.priestess.bot.provider.ProviderController
import com.heyanle.priestess.bot.provider.model.LLMResponse
import com.heyanle.priestess.bot.provider.model.ToolCall
import com.heyanle.priestess.bot.testkit.FakePlatform
import com.heyanle.priestess.bot.testkit.FakeProvider
import com.heyanle.priestess.bot.testkit.FakeTool
import com.heyanle.priestess.bot.testkit.testInMemoryConversationCase
import com.heyanle.priestess.bot.tool.ToolController
import com.heyanle.priestess.bot.tool.ToolExecutor
import com.heyanle.priestess.bot.tool.ToolResult
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MessageFlowSystemTest {
    @Test
    fun `message flows from platform through react tool call to response and persistence`() = runBlocking {
        val platform = FakePlatform()
        val session = FakePlatform.fakeSession()
        val conversationCase = testInMemoryConversationCase()
        val toolController = ToolController()
        val tool = FakeTool(name = "lookup", result = ToolResult.success("tool observation"))
        toolController.register(tool)
        val provider = FakeProvider(
            listOf(
                LLMResponse(
                    content = "need lookup",
                    toolCalls = listOf(ToolCall(id = "call-1", name = "lookup", arguments = """{"value":"question"}""")),
                ),
                LLMResponse(content = "final answer", finishReason = "stop"),
            ),
        )
        val providerController = ProviderController(
            ConfigCase(
                ConfigController(java.nio.file.Files.createTempFile("message-flow-provider", ".json").toString()),
            ),
        )
        providerController.register(provider)
        val metrics = MetricsRegistry()
        val controller = PipelineController(
            testStages = listOf(
                PreProcessStage(
                    agentConfig = AgentConfig(name = "system-agent", model = "fake-model", maxSteps = 4),
                    pipelineConfig = PipelineConfig(maxHistoryMessages = 5),
                    conversationCase = conversationCase,
                    agentCase = AgentCase(),
                    contextManager = ContextManager(TokenCounter()),
                ),
                ProcessStage(
                    providerCase = ProviderCase(providerController),
                    toolExecutor = ToolExecutor(toolController, metrics),
                    toolController = toolController,
                    contextManager = ContextManager(TokenCounter()),
                    metricsRegistry = metrics,
                ),
                ResultDecorateStage(),
                RespondStage(),
            ),
            testOnly = Unit,
            metricsRegistry = metrics,
        )
        val event = com.heyanle.priestess.bot.platform.MessageEvent(
            platform = platform,
            session = session,
            chain = com.heyanle.priestess.bot.platform.MessageChain.text("question"),
            messageId = "message-1",
        )

        val job = controller.process(event)
        job.join()

        assertEquals(1, platform.sentMessages.size)
        assertEquals("final answer", platform.sentMessages.single().second.textContent)
        assertEquals(listOf(mapOf("value" to "question")), tool.calls)
        assertEquals(2, provider.requests.size)
        assertTrue(provider.requests[1].messages.any { it.role == "tool" && it.content == "tool observation" })

        val conversation = conversationCase.getAll().single()
        val messages = conversationCase.getMessages(conversation.id, 10)
        assertTrue(messages.any { it.role == MessageRole.USER && it.content == "question" })
        assertTrue(messages.any { it.role == MessageRole.ASSISTANT && it.content == "need lookup" && it.toolCalls?.contains("lookup") == true })
        assertTrue(messages.any { it.role == MessageRole.TOOL && it.content == "tool observation" && it.toolCallId == "call-1" })
        assertTrue(messages.any { it.role == MessageRole.ASSISTANT && it.content == "final answer" })

        val renderedMetrics = metrics.renderPrometheus()
        assertTrue(renderedMetrics.contains("""priestess_pipeline_messages_total{platform="fake-platform",status="completed"} 1"""))
        assertTrue(renderedMetrics.contains("""priestess_llm_requests_total{provider="fake-provider",status="success"} 1"""))
        assertTrue(renderedMetrics.contains("""priestess_tool_calls_total{status="success",tool="lookup"} 1"""))
        assertFalse(renderedMetrics.contains("question"))
        assertFalse(renderedMetrics.contains("tool observation"))
        assertFalse(renderedMetrics.contains("session-1"))
    }
}
