package com.heyanle.priestess.bot.pipeline

import com.heyanle.priestess.bot.agent.AgentCase
import com.heyanle.priestess.bot.agent.orchestration.SubAgentOrchestrator
import com.heyanle.priestess.bot.config.AgentConfig
import com.heyanle.priestess.bot.config.ConfigCase
import com.heyanle.priestess.bot.config.ConfigController
import com.heyanle.priestess.bot.config.PipelineConfig
import com.heyanle.priestess.bot.config.SubAgentConfig
import com.heyanle.priestess.bot.config.SubAgentOrchestrationConfig
import com.heyanle.priestess.bot.config.SubAgentRouteConfig
import com.heyanle.priestess.bot.pipeline.stages.PreProcessStage
import com.heyanle.priestess.bot.pipeline.stages.ProcessStage
import com.heyanle.priestess.bot.pipeline.stages.RespondStage
import com.heyanle.priestess.bot.pipeline.stages.ResultDecorateStage
import com.heyanle.priestess.bot.provider.ProviderCase
import com.heyanle.priestess.bot.provider.ProviderController
import com.heyanle.priestess.bot.provider.ChatProvider
import com.heyanle.priestess.bot.provider.LLMKind
import com.heyanle.priestess.bot.provider.ProviderMetadata
import com.heyanle.priestess.bot.provider.model.LLMRequest
import com.heyanle.priestess.bot.provider.model.LLMResponse
import com.heyanle.priestess.bot.skill.Skill
import com.heyanle.priestess.bot.skill.SkillCase
import com.heyanle.priestess.bot.skill.SkillController
import com.heyanle.priestess.bot.testkit.FakePlatform
import com.heyanle.priestess.bot.testkit.FakeProvider
import com.heyanle.priestess.bot.testkit.FakeTool
import com.heyanle.priestess.bot.testkit.testInMemoryConversationCase
import com.heyanle.priestess.bot.tool.ToolCase
import com.heyanle.priestess.bot.tool.ToolController
import com.heyanle.priestess.bot.tool.ToolExecutor
import com.heyanle.priestess.bot.tool.ToolResult
import com.heyanle.priestess.bot.tool.builtin.UseSkillTool
import com.heyanle.priestess.bot.workspace.WorkspaceConfig
import com.heyanle.priestess.bot.workspace.WorkspaceConfigSet
import com.heyanle.priestess.bot.workspace.WorkspaceConfigSource
import com.heyanle.priestess.bot.workspace.WorkspaceCase
import com.heyanle.priestess.bot.workspace.WorkspaceController
import com.heyanle.priestess.bot.workspace.WorkspaceMcpClientHandle
import com.heyanle.priestess.bot.workspace.WorkspaceMcpResource
import com.heyanle.priestess.bot.workspace.WorkspaceMcpServerConfig
import com.heyanle.priestess.bot.workspace.WorkspaceMcpToolResolution
import com.heyanle.priestess.bot.workspace.WorkspaceMcpToolResolver
import com.heyanle.priestess.bot.workspace.WorkspaceSkillConfig
import com.heyanle.priestess.bot.workspace.WorkspaceToolConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkspacePipelineReloadTest {
    @Test
    fun `in-flight message keeps pinned snapshot while later message uses reloaded snapshot`() = runBlocking {
        val source = MutableWorkspaceSource(
            listOf(
                WorkspaceConfig(
                    id = "default",
                    name = "Default",
                    isDefault = true,
                    agents = listOf(AgentConfig(name = "agent-v1", model = "model-v1")),
                    tools = WorkspaceToolConfig(enabledTools = listOf("old_tool")),
                ),
            ),
        )
        val oldTool = FakeTool(name = "old_tool", result = ToolResult.success("old observation"))
        val newTool = FakeTool(name = "new_tool", result = ToolResult.success("new observation"))
        val toolController = ToolController().apply {
            register(oldTool)
            register(newTool)
        }
        val workspaceController = WorkspaceController(
            source = source,
            toolCase = ToolCase(toolController),
            skillCase = com.heyanle.priestess.bot.skill.SkillCase(
                com.heyanle.priestess.bot.skill.SkillController(),
            ),
            nowProvider = { 1_000L },
        )
        val firstGate = GateStage()
        val firstProvider = FakeProvider(listOf(LLMResponse(content = "first final", finishReason = "stop")))
        val firstPlatform = FakePlatform()

        val firstController = pipelineController(
            workspaceController = workspaceController,
            toolController = toolController,
            provider = firstProvider,
            gateStage = firstGate,
        )
        val firstJob = firstController.process(
            com.heyanle.priestess.bot.platform.MessageEvent(
                platform = firstPlatform,
                session = FakePlatform.fakeSession(id = "session-1"),
                chain = com.heyanle.priestess.bot.platform.MessageChain.text("first"),
                messageId = "message-1",
            ),
        )
        firstGate.entered.await()

        source.workspaces = listOf(
            WorkspaceConfig(
                id = "default",
                name = "Default",
                isDefault = true,
                agents = listOf(AgentConfig(name = "agent-v2", model = "model-v2")),
                tools = WorkspaceToolConfig(enabledTools = listOf("new_tool")),
            ),
        )
        val reload = workspaceController.reload("default")
        assertTrue(reload.success)

        firstGate.release.complete(Unit)
        firstJob.join()

        assertEquals("model-v1", firstProvider.requests.single().model)
        assertEquals(listOf("old_tool"), firstProvider.requests.single().toolNames())
        assertEquals("first final", firstPlatform.sentMessages.single().second.textContent)

        val secondProvider = FakeProvider(listOf(LLMResponse(content = "second final", finishReason = "stop")))
        val secondPlatform = FakePlatform()
        val secondController = pipelineController(
            workspaceController = workspaceController,
            toolController = toolController,
            provider = secondProvider,
            gateStage = null,
        )
        val secondJob = secondController.process(
            com.heyanle.priestess.bot.platform.MessageEvent(
                platform = secondPlatform,
                session = FakePlatform.fakeSession(id = "session-2"),
                chain = com.heyanle.priestess.bot.platform.MessageChain.text("second"),
                messageId = "message-2",
            ),
        )
        secondJob.join()

        assertEquals("model-v2", secondProvider.requests.single().model)
        assertEquals(listOf("new_tool"), secondProvider.requests.single().toolNames())
        assertEquals("second final", secondPlatform.sentMessages.single().second.textContent)
    }

    @Test
    fun `failed reload keeps old snapshot for later messages and exposes diagnostics`() = runBlocking {
        val source = MutableWorkspaceSource(
            listOf(
                WorkspaceConfig(
                    id = "default",
                    name = "Default",
                    isDefault = true,
                    agents = listOf(AgentConfig(name = "agent-v1", model = "model-v1")),
                    tools = WorkspaceToolConfig(enabledTools = listOf("old_tool")),
                ),
            ),
        )
        val oldTool = FakeTool(name = "old_tool")
        val newTool = FakeTool(name = "new_tool")
        val toolController = ToolController().apply {
            register(oldTool)
            register(newTool)
        }
        val workspaceController = WorkspaceController(
            source = source,
            toolCase = ToolCase(toolController),
            skillCase = com.heyanle.priestess.bot.skill.SkillCase(
                com.heyanle.priestess.bot.skill.SkillController(),
            ),
            nowProvider = { 1_000L },
        )
        val before = workspaceController.get("default") ?: error("missing default")

        source.workspaces = listOf(
            WorkspaceConfig(
                id = "default",
                name = "Default",
                isDefault = true,
                agents = listOf(AgentConfig(name = "agent-v2", model = "model-v2")),
                tools = WorkspaceToolConfig(enabledTools = listOf("new_tool")),
            ),
            WorkspaceConfig(id = "default", name = "Duplicate"),
        )
        val reload = workspaceController.reload("default")

        assertFalse(reload.success)
        assertEquals(before.version, reload.snapshotVersion)
        assertTrue(reload.diagnostics.any { it.contains("Duplicate workspace id") })
        val status = workspaceController.list().single { it.id == "default" }
        assertEquals(before.version, status.activeSnapshotVersion)
        assertTrue(status.lastReload?.diagnostics.orEmpty().any { it.contains("Duplicate workspace id") })

        val provider = FakeProvider(listOf(LLMResponse(content = "still old", finishReason = "stop")))
        val platform = FakePlatform()
        val controller = pipelineController(
            workspaceController = workspaceController,
            toolController = toolController,
            provider = provider,
            gateStage = null,
        )
        controller.process(
            com.heyanle.priestess.bot.platform.MessageEvent(
                platform = platform,
                session = FakePlatform.fakeSession(id = "session-3"),
                chain = com.heyanle.priestess.bot.platform.MessageChain.text("after failed reload"),
                messageId = "message-3",
            ),
        ).join()

        assertEquals("model-v1", provider.requests.single().model)
        assertEquals(listOf("old_tool"), provider.requests.single().toolNames())
        assertEquals("still old", platform.sentMessages.single().second.textContent)
    }

    @Test
    fun `in-flight message keeps pinned runtime tool while later message uses reloaded runtime tool`() = runBlocking {
        val source = MutableWorkspaceSource(
            listOf(
                WorkspaceConfig(
                    id = "default",
                    name = "Default",
                    isDefault = true,
                    mcpServers = listOf(WorkspaceMcpServerConfig(id = "mcp-v1", command = "mcp-server-v1")),
                ),
            ),
        )
        val oldRuntimeTool = FakeTool(name = "runtime_tool_v1", result = ToolResult.success("old runtime observation"))
        val newRuntimeTool = FakeTool(name = "runtime_tool_v2", result = ToolResult.success("new runtime observation"))
        val resolver = MutableMcpToolResolver(oldRuntimeTool)
        val toolController = ToolController()
        val workspaceController = WorkspaceController(
            source = source,
            toolCase = ToolCase(toolController),
            skillCase = SkillCase(SkillController()),
            mcpToolResolver = resolver,
            nowProvider = { 1_000L },
        )
        val firstGate = GateStage()
        val firstProvider = FakeProvider(
            listOf(
                LLMResponse(
                    toolCalls = listOf(
                        com.heyanle.priestess.bot.provider.model.ToolCall(
                            id = "runtime-call-1",
                            name = "runtime_tool_v1",
                            arguments = """{"value":"first"}""",
                        ),
                    ),
                ),
                LLMResponse(content = "first runtime final", finishReason = "stop"),
            ),
        )
        val firstPlatform = FakePlatform()
        val firstJob = pipelineController(
            workspaceController = workspaceController,
            toolController = toolController,
            provider = firstProvider,
            gateStage = firstGate,
        ).process(
            com.heyanle.priestess.bot.platform.MessageEvent(
                platform = firstPlatform,
                session = FakePlatform.fakeSession(id = "runtime-session-1"),
                chain = com.heyanle.priestess.bot.platform.MessageChain.text("first runtime"),
                messageId = "runtime-message-1",
            ),
        )
        firstGate.entered.await()

        resolver.tool = newRuntimeTool
        source.workspaces = listOf(
            WorkspaceConfig(
                id = "default",
                name = "Default",
                isDefault = true,
                mcpServers = listOf(WorkspaceMcpServerConfig(id = "mcp-v2", command = "mcp-server-v2")),
            ),
        )
        assertTrue(workspaceController.reload("default").success)

        firstGate.release.complete(Unit)
        firstJob.join()

        assertEquals(listOf("runtime_tool_v1"), firstProvider.requests.first().toolNames())
        assertEquals(listOf(mapOf("value" to "first")), oldRuntimeTool.calls)
        assertEquals(emptyList(), newRuntimeTool.calls)
        assertEquals("first runtime final", firstPlatform.sentMessages.single().second.textContent)

        val secondProvider = FakeProvider(
            listOf(
                LLMResponse(
                    toolCalls = listOf(
                        com.heyanle.priestess.bot.provider.model.ToolCall(
                            id = "runtime-call-2",
                            name = "runtime_tool_v2",
                            arguments = """{"value":"second"}""",
                        ),
                    ),
                ),
                LLMResponse(content = "second runtime final", finishReason = "stop"),
            ),
        )
        val secondPlatform = FakePlatform()
        pipelineController(
            workspaceController = workspaceController,
            toolController = toolController,
            provider = secondProvider,
            gateStage = null,
        ).process(
            com.heyanle.priestess.bot.platform.MessageEvent(
                platform = secondPlatform,
                session = FakePlatform.fakeSession(id = "runtime-session-2"),
                chain = com.heyanle.priestess.bot.platform.MessageChain.text("second runtime"),
                messageId = "runtime-message-2",
            ),
        ).join()

        assertEquals(listOf("runtime_tool_v2"), secondProvider.requests.first().toolNames())
        assertEquals(listOf(mapOf("value" to "second")), newRuntimeTool.calls)
        assertEquals("second runtime final", secondPlatform.sentMessages.single().second.textContent)
    }

    @Test
    fun `later messages use workspace scoped skill set from reloaded snapshot`() = runBlocking {
        val source = MutableWorkspaceSource(
            listOf(
                WorkspaceConfig(
                    id = "default",
                    name = "Default",
                    isDefault = true,
                    skills = listOf(WorkspaceSkillConfig(name = "old-skill")),
                ),
            ),
        )
        val skillCase = SkillCase(
            SkillController().apply {
                register(TestSkill(name = "old-skill", response = "old skill response"))
                register(TestSkill(name = "new-skill", response = "new skill response"))
            },
        )
        val toolController = ToolController()
        val workspaceController = WorkspaceController(
            source = source,
            toolCase = ToolCase(toolController),
            skillCase = skillCase,
            nowProvider = { 1_000L },
        )
        val firstProvider = FakeProvider(listOf(LLMResponse(content = "provider should not answer", finishReason = "stop")))
        val firstPlatform = FakePlatform()
        pipelineController(
            workspaceController = workspaceController,
            toolController = toolController,
            provider = firstProvider,
            gateStage = null,
            skillCase = skillCase,
        ).process(
            com.heyanle.priestess.bot.platform.MessageEvent(
                platform = firstPlatform,
                session = FakePlatform.fakeSession(id = "skill-session-1"),
                chain = com.heyanle.priestess.bot.platform.MessageChain.text("skill please"),
                messageId = "skill-message-1",
            ),
        ).join()

        assertEquals(1, firstProvider.requests.size)
        assertTrue(firstProvider.requests.single().messages.first { it.role == "system" }.content.orEmpty().contains("Available skills: old-skill"))
        assertEquals("provider should not answer", firstPlatform.sentMessages.single().second.textContent)

        source.workspaces = listOf(
            WorkspaceConfig(
                id = "default",
                name = "Default",
                isDefault = true,
                skills = listOf(WorkspaceSkillConfig(name = "new-skill")),
            ),
        )
        assertTrue(workspaceController.reload("default").success)

        val secondProvider = FakeProvider(listOf(LLMResponse(content = "provider should still not answer", finishReason = "stop")))
        val secondPlatform = FakePlatform()
        pipelineController(
            workspaceController = workspaceController,
            toolController = toolController,
            provider = secondProvider,
            gateStage = null,
            skillCase = skillCase,
        ).process(
            com.heyanle.priestess.bot.platform.MessageEvent(
                platform = secondPlatform,
                session = FakePlatform.fakeSession(id = "skill-session-2"),
                chain = com.heyanle.priestess.bot.platform.MessageChain.text("skill please again"),
                messageId = "skill-message-2",
            ),
        ).join()

        assertEquals(1, secondProvider.requests.size)
        assertTrue(secondProvider.requests.single().messages.first { it.role == "system" }.content.orEmpty().contains("Available skills: new-skill"))
        assertEquals("provider should still not answer", secondPlatform.sentMessages.single().second.textContent)
    }

    @Test
    fun `use skill loads workspace skill markdown into later llm context`() = runBlocking {
        val source = MutableWorkspaceSource(
            listOf(
                WorkspaceConfig(
                    id = "default",
                    name = "Default",
                    isDefault = true,
                    skills = listOf(WorkspaceSkillConfig(name = "research")),
                ),
            ),
        )
        val skillCase = SkillCase(
            SkillController().apply {
                register(
                    TestSkill(
                        name = "research",
                        response = "unused",
                        skillMarkdown = "# Skill: research\n\n## Instructions\nUse workspace research workflow.",
                    ),
                )
            },
        )
        val toolController = ToolController().apply {
            register(UseSkillTool())
        }
        val workspaceController = WorkspaceController(
            source = source,
            toolCase = ToolCase(toolController),
            skillCase = skillCase,
            nowProvider = { 1_000L },
        )
        val provider = FakeProvider(
            listOf(
                LLMResponse(
                    toolCalls = listOf(
                        com.heyanle.priestess.bot.provider.model.ToolCall(
                            id = "call-1",
                            name = "use_skill",
                            arguments = """{"name":"research"}""",
                        ),
                    ),
                ),
                LLMResponse(content = "used workspace skill", finishReason = "stop"),
            ),
        )
        val platform = FakePlatform()

        pipelineController(
            workspaceController = workspaceController,
            toolController = toolController,
            provider = provider,
            gateStage = null,
            skillCase = skillCase,
        ).process(
            com.heyanle.priestess.bot.platform.MessageEvent(
                platform = platform,
                session = FakePlatform.fakeSession(id = "skill-session-use"),
                chain = com.heyanle.priestess.bot.platform.MessageChain.text("please research"),
                messageId = "skill-message-use",
            ),
        ).join()

        assertEquals(2, provider.requests.size)
        val firstSystem = provider.requests[0].messages.first { it.role == "system" }.content.orEmpty()
        val secondSystem = provider.requests[1].messages.first { it.role == "system" }.content.orEmpty()
        assertTrue(firstSystem.contains("Available skills: research"))
        assertFalse(firstSystem.contains("Use workspace research workflow."))
        assertTrue(secondSystem.contains("# Skill: research"))
        assertTrue(secondSystem.contains("Use workspace research workflow."))
        assertEquals("used workspace skill", platform.sentMessages.single().second.textContent)
    }

    @Test
    fun `later messages use workspace provider selection from reloaded snapshot`() = runBlocking {
        val source = MutableWorkspaceSource(
            listOf(
                WorkspaceConfig(
                    id = "default",
                    name = "Default",
                    isDefault = true,
                    providerName = "provider-v1",
                    agents = listOf(AgentConfig(name = "assistant", model = "fallback-model")),
                ),
            ),
        )
        val toolController = ToolController()
        val workspaceController = WorkspaceController(
            source = source,
            toolCase = ToolCase(toolController),
            skillCase = SkillCase(SkillController()),
            nowProvider = { 1_000L },
        )
        val providerV1 = NamedProvider("provider-v1", "provider v1 response")
        val providerV2 = NamedProvider("provider-v2", "provider v2 response")
        val firstPlatform = FakePlatform()
        pipelineController(
            workspaceController = workspaceController,
            toolController = toolController,
            providers = listOf(providerV1, providerV2),
            gateStage = null,
        ).process(
            com.heyanle.priestess.bot.platform.MessageEvent(
                platform = firstPlatform,
                session = FakePlatform.fakeSession(id = "provider-session-1"),
                chain = com.heyanle.priestess.bot.platform.MessageChain.text("first provider"),
                messageId = "provider-message-1",
            ),
        ).join()

        assertEquals(1, providerV1.requests.size)
        assertEquals(0, providerV2.requests.size)
        assertEquals("provider v1 response", firstPlatform.sentMessages.single().second.textContent)

        source.workspaces = listOf(
            WorkspaceConfig(
                id = "default",
                name = "Default",
                isDefault = true,
                providerName = "provider-v2",
                agents = listOf(AgentConfig(name = "assistant", model = "fallback-model")),
            ),
        )
        assertTrue(workspaceController.reload("default").success)

        val secondPlatform = FakePlatform()
        pipelineController(
            workspaceController = workspaceController,
            toolController = toolController,
            providers = listOf(providerV1, providerV2),
            gateStage = null,
        ).process(
            com.heyanle.priestess.bot.platform.MessageEvent(
                platform = secondPlatform,
                session = FakePlatform.fakeSession(id = "provider-session-2"),
                chain = com.heyanle.priestess.bot.platform.MessageChain.text("second provider"),
                messageId = "provider-message-2",
            ),
        ).join()

        assertEquals(1, providerV1.requests.size)
        assertEquals(1, providerV2.requests.size)
        assertEquals("provider v2 response", secondPlatform.sentMessages.single().second.textContent)
    }

    @Test
    fun `later messages use workspace sub-agent route and execution limits from reloaded snapshot`() = runBlocking {
        val source = MutableWorkspaceSource(
            listOf(
                WorkspaceConfig(
                    id = "default",
                    name = "Default",
                    isDefault = true,
                    providerName = "provider-v1",
                    agents = listOf(AgentConfig(name = "primary-v1", model = "primary-model-v1")),
                    subAgents = SubAgentOrchestrationConfig(
                        enabled = true,
                        agents = listOf(
                            SubAgentConfig(
                                name = "expert-v1",
                                agent = AgentConfig(name = "expert-v1", model = "expert-model-v1", maxSteps = 1),
                            ),
                        ),
                        routes = listOf(
                            SubAgentRouteConfig(
                                name = "expert-route-v1",
                                targetAgentName = "expert-v1",
                                keywords = listOf("expert"),
                            ),
                        ),
                    ),
                    tools = WorkspaceToolConfig(enabledTools = listOf("loop_tool")),
                ),
            ),
        )
        val toolController = ToolController().apply {
            register(FakeTool(name = "loop_tool", result = ToolResult.success("loop output")))
        }
        val skillCase = SkillCase(SkillController())
        val workspaceController = WorkspaceController(
            source = source,
            toolCase = ToolCase(toolController),
            skillCase = skillCase,
            nowProvider = { 1_000L },
        )
        val providerV1 = NamedProvider(
            name = "provider-v1",
            response = "unused",
            responses = listOf(
                LLMResponse(
                    toolCalls = listOf(
                        com.heyanle.priestess.bot.provider.model.ToolCall(
                            id = "call-1",
                            name = "loop_tool",
                            arguments = """{"value":"first"}""",
                        ),
                    ),
                ),
            ),
        )
        val providerV2 = NamedProvider(
            name = "provider-v2",
            response = "unused",
            responses = listOf(
                LLMResponse(
                    toolCalls = listOf(
                        com.heyanle.priestess.bot.provider.model.ToolCall(
                            id = "call-2",
                            name = "loop_tool",
                            arguments = """{"value":"second"}""",
                        ),
                    ),
                ),
            ),
        )

        val firstPlatform = FakePlatform()
        pipelineController(
            workspaceController = workspaceController,
            toolController = toolController,
            providers = listOf(providerV1, providerV2),
            gateStage = null,
            subAgentOrchestrator = subAgentOrchestrator(toolController, providerV1, providerV2),
        ).process(
            com.heyanle.priestess.bot.platform.MessageEvent(
                platform = firstPlatform,
                session = FakePlatform.fakeSession(id = "sub-agent-session-1"),
                chain = com.heyanle.priestess.bot.platform.MessageChain.text("ask expert"),
                messageId = "sub-agent-message-1",
            ),
        ).join()

        assertEquals("expert-model-v1", providerV1.requests.single().model)
        assertEquals(listOf("loop_tool"), providerV1.requests.single().toolNames())
        assertEquals("抱歉，处理您的请求超时了，请稍后再试或简化您的问题。", firstPlatform.sentMessages.single().second.textContent)

        source.workspaces = listOf(
            WorkspaceConfig(
                id = "default",
                name = "Default",
                isDefault = true,
                providerName = "provider-v2",
                agents = listOf(AgentConfig(name = "primary-v2", model = "primary-model-v2")),
                subAgents = SubAgentOrchestrationConfig(
                    enabled = true,
                    agents = listOf(
                        SubAgentConfig(
                            name = "expert-v2",
                            agent = AgentConfig(name = "expert-v2", model = "expert-model-v2", maxSteps = 1),
                        ),
                    ),
                    routes = listOf(
                        SubAgentRouteConfig(
                            name = "expert-route-v2",
                            targetAgentName = "expert-v2",
                            keywords = listOf("expert"),
                        ),
                    ),
                ),
                tools = WorkspaceToolConfig(enabledTools = listOf("loop_tool")),
            ),
        )
        assertTrue(workspaceController.reload("default").success)

        val secondPlatform = FakePlatform()
        pipelineController(
            workspaceController = workspaceController,
            toolController = toolController,
            providers = listOf(providerV1, providerV2),
            gateStage = null,
            subAgentOrchestrator = subAgentOrchestrator(toolController, providerV1, providerV2),
        ).process(
            com.heyanle.priestess.bot.platform.MessageEvent(
                platform = secondPlatform,
                session = FakePlatform.fakeSession(id = "sub-agent-session-2"),
                chain = com.heyanle.priestess.bot.platform.MessageChain.text("ask expert again"),
                messageId = "sub-agent-message-2",
            ),
        ).join()

        assertEquals(1, providerV1.requests.size)
        assertEquals("expert-model-v2", providerV2.requests.single().model)
        assertEquals(listOf("loop_tool"), providerV2.requests.single().toolNames())
        assertEquals("抱歉，处理您的请求超时了，请稍后再试或简化您的问题。", secondPlatform.sentMessages.single().second.textContent)
    }

    private fun pipelineController(
        workspaceController: WorkspaceController,
        toolController: ToolController,
        provider: FakeProvider,
        gateStage: GateStage?,
        skillCase: SkillCase? = null,
        subAgentOrchestrator: SubAgentOrchestrator? = null,
    ): PipelineController = pipelineController(
        workspaceController = workspaceController,
        toolController = toolController,
        providers = listOf(provider),
        gateStage = gateStage,
        skillCase = skillCase,
        subAgentOrchestrator = subAgentOrchestrator,
    )

    private fun pipelineController(
        workspaceController: WorkspaceController,
        toolController: ToolController,
        providers: List<ChatProvider>,
        gateStage: GateStage?,
        skillCase: SkillCase? = null,
        subAgentOrchestrator: SubAgentOrchestrator? = null,
    ): PipelineController {
        val providerController = ProviderController(
            ConfigCase(
                ConfigController(java.nio.file.Files.createTempFile("workspace-pipeline-provider", ".json").toString()),
            ),
        )
        providers.forEach { providerController.register(it) }
        val stages = buildList {
            add(
                PreProcessStage(
                    agentConfig = AgentConfig(name = "fallback-agent", model = "fallback-model"),
                    pipelineConfig = PipelineConfig(maxHistoryMessages = 0),
                    conversationCase = testInMemoryConversationCase(),
                    agentCase = AgentCase(),
                    workspaceCase = WorkspaceCase(workspaceController),
                    subAgentOrchestrator = subAgentOrchestrator,
                    skillCase = skillCase,
                ),
            )
            if (gateStage != null) add(gateStage)
            add(
                ProcessStage(
                    agentCase = AgentCase(),
                    providerCase = ProviderCase(providerController),
                    toolCase = ToolCase(toolController),
                ),
            )
            add(ResultDecorateStage())
            add(RespondStage())
        }
        return PipelineController(testStages = stages, testOnly = Unit)
    }

    private fun subAgentOrchestrator(
        toolController: ToolController,
        vararg providers: ChatProvider,
    ): SubAgentOrchestrator {
        val providerController = ProviderController(
            ConfigCase(
                ConfigController(java.nio.file.Files.createTempFile("workspace-sub-agent-provider", ".json").toString()),
            ),
        )
        providers.forEach { providerController.register(it) }
        return SubAgentOrchestrator(
            agentCase = AgentCase(),
            providerCase = ProviderCase(providerController),
            toolCase = ToolCase(toolController),
        )
    }

    private class GateStage : Stage {
        override val name = "Gate"
        override val order = StageOrder.PROCESS
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()

        override suspend fun process(ctx: PipelineContext): kotlinx.coroutines.flow.Flow<Unit>? {
            entered.complete(Unit)
            release.await()
            return null
        }
    }

    private class MutableWorkspaceSource(
        var workspaces: List<WorkspaceConfig>,
    ) : WorkspaceConfigSource {
        override fun load(): WorkspaceConfigSet = WorkspaceConfigSet(workspaces)
    }

    private class TestSkill(
        override val name: String,
        private val response: String,
        override val skillMarkdown: String? = null,
    ) : Skill {
        override val description: String = name
        override val priority: Int = 10

        override suspend fun canHandle(message: String): Boolean = message.contains("skill", ignoreCase = true)

        override suspend fun execute(message: String): String = response
    }

    private class MutableMcpToolResolver(
        var tool: FakeTool,
    ) : WorkspaceMcpToolResolver {
        override fun resolve(
            workspaceId: String,
            servers: List<WorkspaceMcpServerConfig>,
        ): WorkspaceMcpToolResolution {
            return WorkspaceMcpToolResolution(
                resources = listOf(
                    WorkspaceMcpResource(
                        tool = tool,
                        handle = WorkspaceMcpClientHandle { },
                    ),
                ),
            )
        }
    }

    private class NamedProvider(
        name: String,
        private val response: String,
        responses: List<LLMResponse> = emptyList(),
    ) : ChatProvider {
        private val scriptedResponses = ArrayDeque(responses)
        override val metadata = ProviderMetadata(
            name = name,
            displayName = name,
            kind = LLMKind.OPENAI,
            supportToolCalling = true,
            supportVision = false,
            supportStreaming = false,
        )
        override val config = com.heyanle.priestess.bot.config.ProviderConfig(
            name = name,
            type = name,
            model = "named-model",
        )
        val requests = mutableListOf<LLMRequest>()

        override suspend fun test(): Boolean = true

        override suspend fun textChat(request: LLMRequest): LLMResponse {
            requests += request
            return scriptedResponses.removeFirstOrNull()
                ?: LLMResponse(content = response, finishReason = "stop")
        }

        override suspend fun getModels(): List<String> = listOf("named-model")
    }

    private fun com.heyanle.priestess.bot.provider.model.LLMRequest.toolNames(): List<String> {
        return tools.mapNotNull { tool ->
            val function = tool["function"]?.toString() ?: return@mapNotNull null
            Regex(""""name":"([^"]+)"""").find(function)?.groupValues?.get(1)
        }
    }
}
