package com.heyanle.priestess.bot.pipeline.stages

import com.heyanle.priestess.bot.agent.AgentCase
import com.heyanle.priestess.bot.agent.AgentResponse
import com.heyanle.priestess.bot.agent.context.ContextManager
import com.heyanle.priestess.bot.agent.context.TokenCounter
import com.heyanle.priestess.bot.config.AgentConfig
import com.heyanle.priestess.bot.config.PipelineConfig
import com.heyanle.priestess.bot.conversation.MessageRole
import com.heyanle.priestess.bot.memory.MemoryScope
import com.heyanle.priestess.bot.memory.MemoryScopeContext
import com.heyanle.priestess.bot.memory.MemoryType
import com.heyanle.priestess.bot.memory.MemoryCase
import com.heyanle.priestess.bot.persona.PersonaCase
import com.heyanle.priestess.bot.persona.PersonaMemoryInjector
import com.heyanle.priestess.bot.persona.PersonaUpsertRequest
import com.heyanle.priestess.bot.skill.DefaultSkill
import com.heyanle.priestess.bot.skill.SkillCase
import com.heyanle.priestess.bot.skill.SkillController
import com.heyanle.priestess.bot.testkit.testConversationCase
import com.heyanle.priestess.bot.testkit.testPersonaMemoryControllers
import com.heyanle.priestess.bot.testkit.testPipelineContext
import com.heyanle.priestess.bot.tool.ToolController
import com.heyanle.priestess.bot.tool.builtin.SystemInfoTool
import com.heyanle.priestess.bot.workspace.WorkspaceConfig
import com.heyanle.priestess.bot.workspace.WorkspaceConfigSet
import com.heyanle.priestess.bot.workspace.WorkspaceConfigSource
import com.heyanle.priestess.bot.workspace.WorkspaceController
import com.heyanle.priestess.bot.workspace.WorkspaceMcpServerConfig
import com.heyanle.priestess.bot.workspace.WorkspaceMemoryPolicyConfig
import com.heyanle.priestess.bot.workspace.WorkspacePersonaConfig
import com.heyanle.priestess.bot.workspace.WorkspaceResolutionConfig
import com.heyanle.priestess.bot.workspace.WorkspaceSkillConfig
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PreProcessStageTest {
    @Test
    fun `creates agent context with platform session history and metadata`() = runBlocking {
        val conversationCase = testConversationCase("preprocess-context")
        val existing = conversationCase.getOrCreate("fake-platform", "session-1")
        conversationCase.storeMessage(existing.id, MessageRole.USER, "previous user")
        conversationCase.storeMessage(existing.id, MessageRole.ASSISTANT, "previous assistant")
        val ctx = testPipelineContext(text = "current user", sessionId = "session-1")

        val flow = stage(conversationCase).process(ctx)

        val agentContext = assertNotNull(ctx.agentContext)
        assertEquals("test-agent", agentContext.agent.name)
        assertEquals("fake-model", agentContext.agent.model)
        assertEquals(ctx.event.platform, agentContext.platform)
        assertEquals(ctx.event.session, agentContext.session)
        assertEquals(existing.id, agentContext.conversationId)
        assertEquals(listOf("previous user", "previous assistant"), agentContext.messages.map { it.content })
        assertEquals("test-agent", ctx.shared["subAgentSelectionAgent"])
        assertEquals("primary_agent", ctx.shared["subAgentSelectionReason"])
        assertNotNull(flow)
    }

    @Test
    fun `post flow persists current user and final assistant response`() = runBlocking {
        val conversationCase = testConversationCase("preprocess-persist")
        val ctx = testPipelineContext(text = "current user", sessionId = "session-1")

        val flow = stage(conversationCase).process(ctx)
        ctx.agentResponse = AgentResponse.Final("assistant final")
        flow.collect()

        val conversationId = assertNotNull(ctx.agentContext).conversationId
        val stored = conversationCase.getMessages(conversationId, 10)
        assertTrue(stored.any { it.role == MessageRole.USER && it.content == "current user" })
        assertTrue(stored.any { it.role == MessageRole.ASSISTANT && it.content == "assistant final" })
    }

    @Test
    fun `pins resolved workspace and injects workspace metadata into agent context`() = runBlocking {
        val conversationCase = testConversationCase("preprocess-workspace")
        val workspaceController = workspaceController(
            listOf(
                WorkspaceConfig(
                    id = "default",
                    name = "Default",
                    isDefault = true,
                ),
                WorkspaceConfig(
                    id = "ops",
                    name = "Ops Workspace",
                    providerName = "ops-provider",
                    agents = listOf(
                        AgentConfig(
                            name = "ops-agent",
                            instructions = "Ops instructions",
                            model = "ops-model",
                            maxSteps = 4,
                        ),
                    ),
                    skills = listOf(
                        WorkspaceSkillConfig(
                            name = "default",
                            settings = mapOf("mode" to "ops"),
                        ),
                    ),
                    mcpServers = listOf(
                        WorkspaceMcpServerConfig(
                            id = "ops-mcp",
                            command = "ops-mcp-server",
                            env = mapOf("TOKEN" to "secret-token"),
                        ),
                    ),
                    resolution = WorkspaceResolutionConfig(sessionIds = listOf("ops-session")),
                    memory = WorkspaceMemoryPolicyConfig(
                        enabled = true,
                        allowedScopes = listOf("SESSION"),
                        knowledgeBaseIds = listOf("kb-1"),
                        maxInjectedMemories = 2,
                    ),
                ),
            ),
        )
        val ctx = testPipelineContext(text = "current user", sessionId = "ops-session")

        stage(conversationCase, workspaceController = workspaceController).process(ctx)

        val agentContext = assertNotNull(ctx.agentContext)
        assertEquals("ops", ctx.workspaceId)
        assertEquals("session rule", ctx.workspaceResolutionReason)
        assertEquals(ctx.workspaceSnapshot?.version, ctx.workspaceSnapshotVersion)
        assertEquals("ops-agent", agentContext.agent.name)
        assertEquals("ops-model", agentContext.agent.model)
        assertEquals("ops", agentContext.metadata["workspace_id"])
        assertEquals("Ops Workspace", agentContext.metadata["workspace_name"])
        assertEquals("ops-provider", agentContext.metadata["provider_name"])
        assertEquals("system_info", agentContext.metadata["workspace_tool_names"])
        assertEquals("default", agentContext.metadata["workspace_skill_names"])
        assertEquals("default=mode:ops", agentContext.metadata["workspace_skill_settings"])
        assertEquals("ops-mcp", agentContext.metadata["workspace_mcp_server_ids"])
        assertEquals("true", agentContext.metadata["workspace_memory_enabled"])
        assertEquals("SESSION", agentContext.metadata["workspace_memory_allowed_scopes"])
        assertEquals("kb-1", agentContext.metadata["workspace_memory_knowledge_base_ids"])
        assertEquals("2", agentContext.metadata["workspace_memory_max_injected"])
        assertEquals(ctx.workspaceSnapshotVersion.toString(), agentContext.metadata["workspace_snapshot_version"])
        assertEquals("ops", ctx.shared["workspaceId"])
        assertEquals(ctx.workspaceSnapshotVersion, ctx.shared["workspaceSnapshotVersion"])
    }

    @Test
    fun `injects persona and memory prompt section before agent execution`() = runBlocking {
        val conversationCase = testConversationCase("preprocess-persona-memory")
        val (personaController, memoryController) = testPersonaMemoryControllers("preprocess-persona-memory-db")
        val injector = PersonaMemoryInjector(PersonaCase(personaController), MemoryCase(memoryController))
        val workspaceController = workspaceController(
            listOf(
                WorkspaceConfig(
                    id = "default",
                    name = "Default",
                    isDefault = true,
                    agents = listOf(
                        AgentConfig(
                            name = "test-agent",
                            instructions = "Test instructions",
                            model = "fake-model",
                        ),
                    ),
                    memory = WorkspaceMemoryPolicyConfig(
                        enabled = true,
                        allowedScopes = listOf("SESSION"),
                        maxInjectedMemories = 2,
                    ),
                ),
            ),
        )
        val persona = personaController.upsert(
            PersonaUpsertRequest(
                workspaceId = "default",
                name = "Careful Assistant",
                tone = "careful",
                boundaries = listOf("Do not leak secrets"),
                systemPromptTemplate = "Ask clarifying questions when context is missing.",
                agentNames = listOf("test-agent"),
            ),
        )
        val memory = memoryController.save(
            content = "User prefers concise Kotlin answers",
            type = MemoryType.PREFERENCE,
            scope = MemoryScope.SESSION,
            scopeContext = MemoryScopeContext(
                workspaceId = "default",
                sessionId = "session-1",
                userId = "user-1",
                agentName = "test-agent",
            ),
            tags = listOf("kotlin"),
        )
        val ctx = testPipelineContext(
            text = "Please give Kotlin answer",
            sessionId = "session-1",
            senderId = "user-1",
        )

        stage(
            conversationCase,
            workspaceController = workspaceController,
            personaMemoryInjector = injector,
        ).process(ctx)

        val agentContext = assertNotNull(ctx.agentContext)
        assertContains(agentContext.agent.instructions, "Test instructions")
        assertContains(agentContext.agent.instructions, "Persona And Memory Context")
        assertContains(agentContext.agent.instructions, "Careful Assistant")
        assertContains(agentContext.agent.instructions, "Ask clarifying questions")
        assertContains(agentContext.agent.instructions, memory.id)
        assertEquals(persona.id, agentContext.metadata["injected_persona_id"])
        assertEquals(memory.id, agentContext.metadata["injected_memory_ids"])
        assertEquals(persona.id, ctx.shared["injectedPersonaId"])
        assertEquals(listOf(memory.id), ctx.shared["injectedMemoryIds"])
    }

    @Test
    fun `later messages use workspace snapshot published by reload`() = runBlocking {
        val conversationCase = testConversationCase("preprocess-workspace-reload")
        val source = MutableWorkspaceSource(
            listOf(
                WorkspaceConfig(
                    id = "default",
                    name = "Default",
                    isDefault = true,
                    agents = listOf(AgentConfig(name = "agent-v1", model = "model-v1")),
                ),
            ),
        )
        val workspaceController = workspaceController(source)
        val first = testPipelineContext(text = "first", sessionId = "session-1")
        stage(conversationCase, workspaceController = workspaceController).process(first)
        val firstVersion = assertNotNull(first.workspaceSnapshotVersion)

        source.workspaces = listOf(
            WorkspaceConfig(
                id = "default",
                name = "Default",
                isDefault = true,
                agents = listOf(AgentConfig(name = "agent-v2", model = "model-v2")),
            ),
        )
        val reload = workspaceController.reload("default")
        val second = testPipelineContext(text = "second", sessionId = "session-2")
        stage(conversationCase, workspaceController = workspaceController).process(second)

        assertTrue(reload.success)
        assertTrue(assertNotNull(second.workspaceSnapshotVersion) > firstVersion)
        assertEquals(reload.snapshotVersion, second.workspaceSnapshotVersion)
        assertEquals("agent-v2", assertNotNull(second.agentContext).agent.name)
        assertEquals("model-v2", second.agentContext?.agent?.model)
    }

    @Test
    fun `later messages use workspace memory policy from reloaded snapshot`() = runBlocking {
        val conversationCase = testConversationCase("preprocess-memory-policy-reload")
        val (personaController, memoryController) = testPersonaMemoryControllers("preprocess-memory-policy-reload-db")
        val injector = PersonaMemoryInjector(PersonaCase(personaController), MemoryCase(memoryController))
        val source = MutableWorkspaceSource(
            listOf(
                WorkspaceConfig(
                    id = "default",
                    name = "Default",
                    isDefault = true,
                    agents = listOf(
                        AgentConfig(
                            name = "test-agent",
                            instructions = "Test instructions",
                            model = "fake-model",
                        ),
                    ),
                    memory = WorkspaceMemoryPolicyConfig(
                        enabled = true,
                        allowedScopes = listOf("SESSION"),
                        maxInjectedMemories = 2,
                    ),
                ),
            ),
        )
        val workspaceController = workspaceController(source)
        val persona = personaController.upsert(
            PersonaUpsertRequest(
                workspaceId = "default",
                name = "Policy Persona",
                systemPromptTemplate = "Use policy context.",
                agentNames = listOf("test-agent"),
            ),
        )
        val memory = memoryController.save(
            content = "User likes memory policy checks",
            type = MemoryType.FACT,
            scope = MemoryScope.SESSION,
            scopeContext = MemoryScopeContext(
                workspaceId = "default",
                sessionId = "memory-policy-session",
                userId = "memory-policy-user",
                agentName = "test-agent",
            ),
        )
        val first = testPipelineContext(
            text = "memory policy",
            sessionId = "memory-policy-session",
            senderId = "memory-policy-user",
        )
        stage(
            conversationCase,
            workspaceController = workspaceController,
            personaMemoryInjector = injector,
        ).process(first)

        val firstAgent = assertNotNull(first.agentContext)
        assertContains(firstAgent.agent.instructions, persona.name)
        assertContains(firstAgent.agent.instructions, memory.id)
        assertEquals(memory.id, firstAgent.metadata["injected_memory_ids"])

        source.workspaces = listOf(
            WorkspaceConfig(
                id = "default",
                name = "Default",
                isDefault = true,
                agents = listOf(
                    AgentConfig(
                        name = "test-agent",
                        instructions = "Test instructions",
                        model = "fake-model",
                    ),
                ),
                memory = WorkspaceMemoryPolicyConfig(
                    enabled = false,
                    allowedScopes = listOf("SESSION"),
                    maxInjectedMemories = 2,
                ),
            ),
        )
        assertTrue(workspaceController.reload("default").success)

        val second = testPipelineContext(
            text = "memory policy",
            sessionId = "memory-policy-session",
            senderId = "memory-policy-user",
        )
        stage(
            conversationCase,
            workspaceController = workspaceController,
            personaMemoryInjector = injector,
        ).process(second)

        val secondAgent = assertNotNull(second.agentContext)
        assertContains(secondAgent.agent.instructions, persona.name)
        assertEquals(persona.id, secondAgent.metadata["injected_persona_id"])
        assertEquals(null, secondAgent.metadata["injected_memory_ids"])
        assertEquals(persona.id, second.shared["injectedPersonaId"])
    }

    @Test
    fun `later messages use workspace persona selection from reloaded snapshot`() = runBlocking {
        val conversationCase = testConversationCase("preprocess-persona-selection-reload")
        val (personaController, memoryController) = testPersonaMemoryControllers("preprocess-persona-selection-reload-db")
        val injector = PersonaMemoryInjector(PersonaCase(personaController), MemoryCase(memoryController))
        val personaA = personaController.upsert(
            PersonaUpsertRequest(
                workspaceId = "default",
                name = "Persona A",
                systemPromptTemplate = "Use persona A.",
                agentNames = listOf("test-agent"),
            ),
        )
        val personaB = personaController.upsert(
            PersonaUpsertRequest(
                workspaceId = "default",
                name = "Persona B",
                systemPromptTemplate = "Use persona B.",
                agentNames = listOf("test-agent"),
            ),
        )
        val source = MutableWorkspaceSource(
            listOf(
                WorkspaceConfig(
                    id = "default",
                    name = "Default",
                    isDefault = true,
                    agents = listOf(AgentConfig(name = "test-agent", instructions = "Test instructions", model = "fake-model")),
                    personas = listOf(WorkspacePersonaConfig(id = personaA.id)),
                    memory = WorkspaceMemoryPolicyConfig(enabled = false),
                ),
            ),
        )
        val workspaceController = workspaceController(source)
        val first = testPipelineContext(text = "persona", sessionId = "persona-session")
        stage(
            conversationCase,
            workspaceController = workspaceController,
            personaMemoryInjector = injector,
        ).process(first)

        val firstAgent = assertNotNull(first.agentContext)
        assertContains(firstAgent.agent.instructions, "Persona A")
        assertEquals(personaA.id, firstAgent.metadata["injected_persona_id"])

        source.workspaces = listOf(
            WorkspaceConfig(
                id = "default",
                name = "Default",
                isDefault = true,
                agents = listOf(AgentConfig(name = "test-agent", instructions = "Test instructions", model = "fake-model")),
                personas = listOf(WorkspacePersonaConfig(id = personaB.id)),
                memory = WorkspaceMemoryPolicyConfig(enabled = false),
            ),
        )
        assertTrue(workspaceController.reload("default").success)

        val second = testPipelineContext(text = "persona", sessionId = "persona-session")
        stage(
            conversationCase,
            workspaceController = workspaceController,
            personaMemoryInjector = injector,
        ).process(second)

        val secondAgent = assertNotNull(second.agentContext)
        assertContains(secondAgent.agent.instructions, "Persona B")
        assertEquals(personaB.id, secondAgent.metadata["injected_persona_id"])
    }

    private fun stage(
        conversationCase: com.heyanle.priestess.bot.conversation.ConversationCase,
        workspaceController: WorkspaceController? = null,
        personaMemoryInjector: PersonaMemoryInjector? = null,
    ): PreProcessStage {
        return PreProcessStage(
            agentConfig = AgentConfig(
                name = "test-agent",
                instructions = "Test instructions",
                model = "fake-model",
                maxSteps = 3,
            ),
            pipelineConfig = PipelineConfig(maxHistoryMessages = 5),
            conversationCase = conversationCase,
            agentCase = AgentCase(),
            contextManager = ContextManager(TokenCounter()),
            workspaceController = workspaceController,
            personaMemoryInjector = personaMemoryInjector,
        )
    }

    private fun workspaceController(workspaces: List<WorkspaceConfig>): WorkspaceController {
        return workspaceController(MutableWorkspaceSource(workspaces))
    }

    private fun workspaceController(source: WorkspaceConfigSource): WorkspaceController {
        val tools = ToolController().apply {
            register(SystemInfoTool())
        }
        val skills = SkillController().apply {
            register(DefaultSkill())
        }
        return WorkspaceController(
            source = source,
            toolController = tools,
            skillCase = SkillCase(skills),
            nowProvider = { 1_000L },
        )
    }

    private class MutableWorkspaceSource(
        var workspaces: List<WorkspaceConfig>,
    ) : WorkspaceConfigSource {
        override fun load(): WorkspaceConfigSet = WorkspaceConfigSet(workspaces)
    }
}
