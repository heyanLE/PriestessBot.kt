package com.heyanle.priestess.bot.agent.runner

import com.heyanle.priestess.bot.agent.AgentContext
import com.heyanle.priestess.bot.agent.AgentHooks
import com.heyanle.priestess.bot.agent.AgentResponse
import com.heyanle.priestess.bot.agent.AgentState
import com.heyanle.priestess.bot.agent.context.ContextManager
import com.heyanle.priestess.bot.agent.context.TokenCounter
import com.heyanle.priestess.bot.provider.model.LLMResponse
import com.heyanle.priestess.bot.provider.model.ToolCall
import com.heyanle.priestess.bot.skill.PipelineSkillState
import com.heyanle.priestess.bot.skill.SkillPromptDocument
import com.heyanle.priestess.bot.testkit.FakeProvider
import com.heyanle.priestess.bot.testkit.FakeTool
import com.heyanle.priestess.bot.testkit.testAgent
import com.heyanle.priestess.bot.testkit.testAgentContext
import com.heyanle.priestess.bot.tool.FunctionTool
import com.heyanle.priestess.bot.tool.ToolCase
import com.heyanle.priestess.bot.tool.ToolController
import com.heyanle.priestess.bot.tool.ToolExecutor
import com.heyanle.priestess.bot.tool.ToolPolicy
import com.heyanle.priestess.bot.tool.ToolPolicyDecision
import com.heyanle.priestess.bot.tool.ToolPolicyDenialCode
import com.heyanle.priestess.bot.tool.ToolResult
import com.heyanle.priestess.bot.tool.builtin.UnloadSkillTool
import com.heyanle.priestess.bot.tool.builtin.UseSkillTool
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ReActRunnerTest {
    @Test
    fun `returns final response when provider does not request tools`() = runBlocking {
        val context = testAgentContext()
        val runner = runner(
            context = context,
            provider = FakeProvider(listOf(LLMResponse(content = "done", finishReason = "stop"))),
        )

        val response = runner.stepUntilDone()

        assertEquals(AgentResponse.Final("done"), response)
        assertEquals(AgentState.DONE, runner.state)
        assertEquals("system", context.messages.first().role)
        assertEquals("assistant", context.messages.last().role)
        assertEquals("done", context.messages.last().content)
    }

    @Test
    fun `executes tool call then continues to final response`() = runBlocking {
        val tool = FakeTool(result = ToolResult.success("observed"))
        val provider = FakeProvider(
            listOf(
                LLMResponse(
                    content = "need tool",
                    toolCalls = listOf(ToolCall(id = "call-1", name = "fake_tool", arguments = """{"value":"abc"}""")),
                ),
                LLMResponse(content = "final after tool", finishReason = "stop"),
            ),
        )
        val context = testAgentContext()

        val response = runner(context, provider, listOf(tool)).stepUntilDone()

        assertEquals(AgentResponse.Final("final after tool"), response)
        assertEquals(listOf(mapOf("value" to "abc")), tool.calls)
        assertTrue(context.messages.any { it.role == "assistant" && it.toolCalls?.firstOrNull()?.name == "fake_tool" })
        assertTrue(context.messages.any { it.role == "tool" && it.toolCallId == "call-1" && it.content == "observed" })
        assertEquals(2, provider.requests.size)
    }

    @Test
    fun `only exposes workspace scoped tools to provider`() = runBlocking {
        val allowedTool = FakeTool(name = "allowed_tool")
        val hiddenTool = FakeTool(name = "hidden_tool")
        val provider = FakeProvider(listOf(LLMResponse(content = "done", finishReason = "stop")))
        val context = testAgentContext(
            metadata = mapOf("workspace_tool_names" to "allowed_tool"),
        )

        runner(context, provider, listOf(allowedTool, hiddenTool)).stepUntilDone()

        val exposedToolNames = provider.requests.single().tools.mapNotNull {
            it["function"]?.toString()?.let { function -> Regex(""""name":"([^"]+)"""").find(function)?.groupValues?.get(1) }
        }
        assertEquals(listOf("allowed_tool"), exposedToolNames)
    }

    @Test
    fun `exposes and executes workspace scoped tool without global registration`() = runBlocking {
        val scopedTool = FakeTool(name = "workspace_mcp_tool", result = ToolResult.success("workspace observed"))
        val provider = FakeProvider(
            listOf(
                LLMResponse(
                    toolCalls = listOf(
                        ToolCall(id = "call-1", name = "workspace_mcp_tool", arguments = """{"value":"abc"}"""),
                    ),
                ),
                LLMResponse(content = "final after workspace tool", finishReason = "stop"),
            ),
        )
        val context = testAgentContext(
            metadata = mapOf("workspace_tool_names" to "workspace_mcp_tool"),
            scopedTools = listOf(scopedTool),
        )

        val response = runner(context, provider).stepUntilDone()

        assertEquals(AgentResponse.Final("final after workspace tool"), response)
        val exposedToolNames = provider.requests.first().tools.mapNotNull {
            it["function"]?.toString()?.let { function -> Regex(""""name":"([^"]+)"""").find(function)?.groupValues?.get(1) }
        }
        assertEquals(listOf("workspace_mcp_tool"), exposedToolNames)
        assertEquals(listOf(mapOf("value" to "abc")), scopedTool.calls)
        assertTrue(context.messages.any { it.role == "tool" && it.content == "workspace observed" })
    }

    @Test
    fun `use skill tool loads skill markdown into later system prompt`() = runBlocking {
        val provider = FakeProvider(
            listOf(
                LLMResponse(
                    toolCalls = listOf(
                        ToolCall(id = "call-1", name = "use_skill", arguments = """{"name":"research"}"""),
                    ),
                ),
                LLMResponse(content = "used skill", finishReason = "stop"),
            ),
        )
        val context = testAgentContext(
            skillState = PipelineSkillState(
                listOf(
                    SkillPromptDocument(
                        name = "research",
                        markdown = "# Skill: research\n\nFollow the research workflow.",
                    ),
                ),
            ),
        )

        val response = runner(context, provider, listOf(UseSkillTool())).stepUntilDone()

        assertEquals(AgentResponse.Final("used skill"), response)
        assertEquals(2, provider.requests.size)
        val secondSystem = provider.requests[1].messages.first { it.role == "system" }.content.orEmpty()
        assertTrue(secondSystem.contains("# Skill: research"))
        assertTrue(secondSystem.contains("Follow the research workflow."))
    }

    @Test
    fun `unload skill tool removes skill markdown from later system prompt`() = runBlocking {
        val provider = FakeProvider(
            listOf(
                LLMResponse(
                    toolCalls = listOf(
                        ToolCall(id = "call-1", name = "use_skill", arguments = """{"name":"research"}"""),
                    ),
                ),
                LLMResponse(
                    toolCalls = listOf(
                        ToolCall(id = "call-2", name = "unload_skill", arguments = """{"name":"research"}"""),
                    ),
                ),
                LLMResponse(content = "skill unloaded", finishReason = "stop"),
            ),
        )
        val context = testAgentContext(
            skillState = PipelineSkillState(
                listOf(
                    SkillPromptDocument(
                        name = "research",
                        markdown = "# Skill: research\n\nFollow the research workflow.",
                    ),
                ),
            ),
        )

        val response = runner(context, provider, listOf(UseSkillTool(), UnloadSkillTool())).stepUntilDone()

        assertEquals(AgentResponse.Final("skill unloaded"), response)
        assertEquals(3, provider.requests.size)
        val afterLoad = provider.requests[1].messages.first { it.role == "system" }.content.orEmpty()
        val afterUnload = provider.requests[2].messages.first { it.role == "system" }.content.orEmpty()
        assertTrue(afterLoad.contains("# Skill: research"))
        assertFalse(afterUnload.contains("# Skill: research"))
        assertTrue(afterUnload.contains("No skills loaded."))
    }

    @Test
    fun `converts tool execution failure into failed observation and continues`() = runBlocking {
        val tool = FakeTool(result = ToolResult.error("tool denied"))
        val context = testAgentContext()
        val provider = FakeProvider(
            listOf(
                LLMResponse(toolCalls = listOf(ToolCall(id = "call-1", name = "fake_tool", arguments = """{"value":"abc"}"""))),
                LLMResponse(content = "handled failure", finishReason = "stop"),
            ),
        )

        val response = runner(context, provider, listOf(tool)).stepUntilDone()

        assertEquals(AgentResponse.Final("handled failure"), response)
        assertTrue(context.messages.any { it.role == "tool" && it.content == "tool denied" })
    }

    @Test
    fun `converts tool executor exception into failed observation and continues`() = runBlocking {
        val tool = FakeTool()
        val context = testAgentContext()
        val provider = FakeProvider(
            listOf(
                LLMResponse(toolCalls = listOf(ToolCall(id = "call-1", name = "fake_tool", arguments = """{"value":"abc"}"""))),
                LLMResponse(content = "handled executor failure", finishReason = "stop"),
            ),
        )
        val controller = ToolController().also { it.register(tool) }
        val executor = ToolExecutor(
            registry = controller,
            policy = ToolPolicy { _, _, _ -> error("policy exploded") },
        )

        val response = runner(
            context = context,
            provider = provider,
            tools = listOf(tool),
            toolExecutor = executor,
        ).stepUntilDone()

        assertEquals(AgentResponse.Final("handled executor failure"), response)
        assertEquals(emptyList(), tool.calls)
        assertTrue(context.messages.any { it.role == "tool" && it.toolCallId == "call-1" && it.content == "Tool execution failed: policy exploded" })
    }

    @Test
    fun `uses agent tool timeout for tool calls`() = runBlocking {
        val tool = FakeTool(delayMs = 100)
        val context = testAgentContext(agent = testAgent(toolTimeoutMs = 10))
        val observedToolResults = mutableListOf<AgentResponse.ToolExecuted>()
        val provider = FakeProvider(
            listOf(
                LLMResponse(toolCalls = listOf(ToolCall(id = "call-1", name = "fake_tool", arguments = """{"value":"abc"}"""))),
                LLMResponse(content = "handled timeout", finishReason = "stop"),
            ),
        )

        val response = runner(
            context = context,
            provider = provider,
            tools = listOf(tool),
            toolExecutor = ToolExecutor(ToolController().also { it.register(tool) }, defaultTimeoutMillis = 30_000),
            hooks = recordingHooks(observedToolResults),
        ).stepUntilDone()

        assertEquals(AgentResponse.Final("handled timeout"), response)
        val toolResult = observedToolResults.single().toolResult
        assertEquals("TIMEOUT", toolResult.errorCode)
        assertEquals("Tool 'fake_tool' timed out after 10ms", toolResult.error)
        assertTrue(context.messages.any { it.role == "tool" && it.content == toolResult.error })
    }

    @Test
    fun `tool hooks include structured policy denial result`() = runBlocking {
        val tool = FakeTool()
        val context = testAgentContext()
        val observedToolResults = mutableListOf<AgentResponse.ToolExecuted>()
        val provider = FakeProvider(
            listOf(
                LLMResponse(toolCalls = listOf(ToolCall(id = "call-1", name = "fake_tool", arguments = """{"value":"abc"}"""))),
                LLMResponse(content = "handled denial", finishReason = "stop"),
            ),
        )
        val controller = ToolController().also { it.register(tool) }
        val executor = ToolExecutor(
            registry = controller,
            policy = ToolPolicy { _, _, _ ->
                ToolPolicyDecision.denied(
                    code = ToolPolicyDenialCode.DISABLED_TOOL,
                    message = "disabled for this agent",
                )
            },
        )

        val response = runner(
            context = context,
            provider = provider,
            tools = listOf(tool),
            toolExecutor = executor,
            hooks = recordingHooks(observedToolResults),
        ).stepUntilDone()

        assertEquals(AgentResponse.Final("handled denial"), response)
        assertEquals(emptyList(), tool.calls)
        val toolResult = observedToolResults.single().toolResult
        assertEquals("PERMISSION_DENIED", toolResult.errorCode)
        assertEquals(ToolPolicyDenialCode.DISABLED_TOOL, toolResult.policyDenialCode)
        assertEquals("PERMISSION_DENIED[DISABLED_TOOL]: disabled for this agent", toolResult.error)
    }

    @Test
    fun `returns error when provider throws`() = runBlocking {
        val runner = runner(
            context = testAgentContext(),
            provider = FakeProvider(failure = IllegalStateException("provider down")),
        )

        val response = runner.stepUntilDone()

        val error = assertIs<AgentResponse.Error>(response)
        assertEquals("provider down", error.message)
        assertEquals(AgentState.ERROR, runner.state)
    }

    @Test
    fun `step after error does not call provider again`() = runBlocking {
        val provider = FakeProvider(failure = IllegalStateException("provider down"))
        val runner = runner(
            context = testAgentContext(),
            provider = provider,
        )

        val first = runner.stepUntilDone()
        val second = runner.step()

        assertIs<AgentResponse.Error>(first)
        val secondError = assertIs<AgentResponse.Error>(second)
        assertEquals("Agent is in error state, cannot continue", secondError.message)
        assertEquals(1, provider.requests.size)
    }

    @Test
    fun `returns error when max steps are exceeded`() = runBlocking {
        val tool = FakeTool()
        val runner = runner(
            context = testAgentContext(agent = testAgent(maxSteps = 1)),
            provider = FakeProvider(
                listOf(
                    LLMResponse(toolCalls = listOf(ToolCall(id = "call-1", name = "fake_tool", arguments = """{"value":"abc"}"""))),
                ),
            ),
            tools = listOf(tool),
        )

        val response = runner.stepUntilDone()

        val error = assertIs<AgentResponse.Error>(response)
        assertEquals("Exceeded maximum steps (1)", error.message)
        assertEquals(AgentState.ERROR, runner.state)
    }

    @Test
    fun `step after done returns cached final response without another provider call`() = runBlocking {
        val provider = FakeProvider(listOf(LLMResponse(content = "done", finishReason = "stop")))
        val runner = runner(context = testAgentContext(), provider = provider)

        val first = runner.stepUntilDone()
        val second = runner.step()

        assertEquals(first, second)
        assertEquals(1, provider.requests.size)
        assertNotNull(runner.finalResponse())
    }

    private fun runner(
        context: com.heyanle.priestess.bot.agent.AgentContext,
        provider: FakeProvider,
        tools: List<FunctionTool> = emptyList(),
        toolExecutor: ToolExecutor? = null,
        hooks: AgentHooks? = null,
    ): ReActRunner {
        val toolController = ToolController()
        tools.forEach(toolController::register)
        return ReActRunner(
            context = context,
            provider = provider,
            toolCase = ToolCase(
                controller = toolController,
                executorProvider = { toolExecutor ?: ToolExecutor(toolController) },
            ),
            contextManager = ContextManager(TokenCounter()),
            hooks = hooks,
        )
    }

    private fun recordingHooks(results: MutableList<AgentResponse.ToolExecuted>): AgentHooks {
        return object : AgentHooks {
            override suspend fun onToolEnd(context: AgentContext, toolName: String, result: AgentResponse.ToolExecuted) {
                results += result
            }
        }
    }
}
