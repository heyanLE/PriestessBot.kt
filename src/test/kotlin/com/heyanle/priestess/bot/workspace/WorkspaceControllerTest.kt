package com.heyanle.priestess.bot.workspace

import com.heyanle.priestess.bot.config.AgentConfig
import com.heyanle.priestess.bot.config.PriestessConfig
import com.heyanle.priestess.bot.skill.DefaultSkill
import com.heyanle.priestess.bot.skill.Skill
import com.heyanle.priestess.bot.skill.SkillCase
import com.heyanle.priestess.bot.skill.SkillController
import com.heyanle.priestess.bot.testkit.FakeTool
import com.heyanle.priestess.bot.tool.ToolController
import com.heyanle.priestess.bot.tool.ToolSchema
import com.heyanle.priestess.bot.tool.builtin.ListToolsTool
import com.heyanle.priestess.bot.tool.builtin.SystemInfoTool
import com.heyanle.priestess.bot.tool.builtin.UnloadSkillTool
import com.heyanle.priestess.bot.tool.builtin.UseSkillTool
import com.heyanle.priestess.bot.tool.builtin.WebSearchTool
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WorkspaceControllerTest {
    @Test
    fun `derives default workspace from existing global config`() {
        val config = PriestessConfig(
            agent = AgentConfig(name = "assistant", providerName = "openai", enabledTools = listOf("system_info")),
        )
        val source = ConfigBackedWorkspaceConfigSource { config }

        val loaded = source.load().workspaces.single()

        assertEquals(WorkspaceConfig.DEFAULT_WORKSPACE_ID, loaded.id)
        assertTrue(loaded.isDefault)
        assertEquals("assistant", loaded.agents.single().name)
        assertEquals("openai", loaded.providerName)
        assertEquals(listOf("system_info"), loaded.tools.enabledTools)
    }

    @Test
    fun `resolves explicit workspace and falls back to default`() {
        val controller = workspaceController(
            listOf(
                WorkspaceConfig(id = "default", name = "Default", enabled = true, isDefault = true),
                WorkspaceConfig(id = "ops", name = "Ops", enabled = true),
            ),
        )

        assertEquals("ops", controller.resolve(WorkspaceResolutionContext(metadata = mapOf("workspace_id" to "ops"))).snapshot.id)
        val fallback = controller.resolve(WorkspaceResolutionContext(platformName = "unknown"))
        assertEquals("default", fallback.snapshot.id)
        assertEquals("default workspace", fallback.reason)
    }

    @Test
    fun `validation reports duplicate ids and unknown resources without secrets`() {
        val controller = workspaceController(emptyList())

        val result = controller.validate(
            listOf(
                WorkspaceConfig(
                    id = "default",
                    name = "Default",
                    skills = listOf(WorkspaceSkillConfig(name = "missing-skill")),
                    tools = WorkspaceToolConfig(enabledTools = listOf("missing-tool")),
                    mcpServers = listOf(
                        WorkspaceMcpServerConfig(
                            id = "mcp-a",
                            command = "",
                            env = mapOf("TOKEN" to "secret-token"),
                        ),
                    ),
                ),
                WorkspaceConfig(id = "default", name = "Duplicate"),
            ),
        )

        assertFalse(result.valid)
        assertTrue(result.diagnostics.any { it.contains("Duplicate workspace id") })
        assertTrue(result.diagnostics.any { it.contains("unknown skill") })
        assertTrue(result.diagnostics.any { it.contains("unknown enabled tool") })
        assertTrue(result.diagnostics.any { it.contains("requires command") })
        assertFalse(result.diagnostics.joinToString().contains("secret-token"))
    }

    @Test
    fun `successful reload publishes new snapshot and computes plan`() {
        val source = MutableWorkspaceSource(
            listOf(
                WorkspaceConfig(
                    id = "default",
                    name = "Default",
                    tools = WorkspaceToolConfig(enabledTools = listOf("system_info")),
                ),
            ),
        )
        val controller = workspaceController(source)
        val before = controller.get("default")
        assertNotNull(before)
        assertEquals(listOf("system_info"), before.toolNames)

        source.workspaces = listOf(
            WorkspaceConfig(
                id = "default",
                name = "Default",
                tools = WorkspaceToolConfig(enabledTools = listOf("system_info", "list_tools")),
                memory = WorkspaceMemoryPolicyConfig(maxInjectedMemories = 9),
            ),
        )
        val result = controller.reload("default")

        assertTrue(result.success)
        val after = controller.get("default")
        assertNotNull(after)
        assertTrue(after.version > before.version)
        assertEquals(listOf("list_tools", "system_info"), after.toolNames)
        assertTrue(result.plan?.added.orEmpty().contains("tool:list_tools"))
        assertTrue(result.plan?.modified.orEmpty().contains("memory_policy"))
    }

    @Test
    fun `workspace default tool set honors default enabled and keeps skill control tools`() {
        val source = MutableWorkspaceSource(
            listOf(
                WorkspaceConfig(
                    id = "default",
                    name = "Default",
                    skills = listOf(WorkspaceSkillConfig(name = "ops")),
                ),
            ),
        )
        val tools = ToolController().apply {
            register(SystemInfoTool())
            register(
                FakeTool(
                    schema = ToolSchema(
                        name = "disabled_by_default",
                        description = "Disabled by default",
                        defaultEnabled = false,
                    ),
                ),
            )
            register(UseSkillTool())
            register(UnloadSkillTool())
        }
        val skills = SkillCase(
            SkillController().apply {
                register(TestSkill(name = "ops", response = "ops handled", priority = 10))
            },
        )

        val controller = WorkspaceController(
            source = source,
            toolController = tools,
            skillCase = skills,
            nowProvider = { 1_000L },
        )
        val snapshot = controller.get("default") ?: error("missing default")

        assertTrue("system_info" in snapshot.toolNames)
        assertTrue("use_skill" in snapshot.toolNames)
        assertTrue("unload_skill" in snapshot.toolNames)
        assertFalse("disabled_by_default" in snapshot.toolNames)
    }

    @Test
    fun `snapshot carries scoped skill settings and redacted mcp server config`() {
        val controller = workspaceController(
            listOf(
                WorkspaceConfig(
                    id = "default",
                    name = "Default",
                    skills = listOf(
                        WorkspaceSkillConfig(
                            name = "default",
                            settings = mapOf("mode" to "concise"),
                        ),
                    ),
                    mcpServers = listOf(
                        WorkspaceMcpServerConfig(
                            id = "local-mcp",
                            command = "mcp-server",
                            args = listOf("--stdio"),
                            env = mapOf("TOKEN" to "secret-token"),
                        ),
                    ),
                ),
            ),
        )

        val snapshot = controller.get("default") ?: error("missing default")

        assertEquals(listOf("default"), snapshot.skillNames)
        assertEquals("concise", snapshot.skillSettings["default"]?.get("mode"))
        assertEquals(listOf("local-mcp"), snapshot.mcpServerIds)
        assertEquals("mcp-server", snapshot.mcpServers.single().command)
        assertTrue(snapshot.mcpServers.single().env.isEmpty())
    }

    @Test
    fun `snapshot includes resolved mcp tools after candidate initialization succeeds`() {
        val resourceHandle = FakeMcpHandle()
        val resolverOnlyHandle = FakeMcpHandle()
        val resolver = FakeMcpToolResolver(
            result = WorkspaceMcpToolResolution(
                resources = listOf(
                    WorkspaceMcpResource(
                        tool = com.heyanle.priestess.bot.testkit.FakeTool(name = "local-mcp.search"),
                        handle = resourceHandle,
                    ),
                ),
                handles = listOf(resolverOnlyHandle),
                diagnostics = listOf("mcp ok"),
            ),
        )
        val controller = workspaceController(
            source = MutableWorkspaceSource(
                listOf(
                    WorkspaceConfig(
                        id = "default",
                        name = "Default",
                        mcpServers = listOf(
                            WorkspaceMcpServerConfig(id = "local-mcp", command = "mcp-server"),
                        ),
                    ),
                ),
            ),
            mcpToolResolver = resolver,
        )

        val snapshot = controller.get("default") ?: error("missing default")

        assertEquals(listOf("local-mcp"), resolver.calls.single().map { it.id })
        assertEquals(listOf("local-mcp.search"), snapshot.mcpToolNames)
        assertTrue(snapshot.toolNames.contains("local-mcp.search"))
        assertEquals(2, snapshot.mcpHandles.size)
        assertTrue(snapshot.mcpHandles.contains(resourceHandle))
        assertTrue(snapshot.mcpHandles.contains(resolverOnlyHandle))
        assertTrue(snapshot.diagnostics.contains("mcp ok"))
    }

    @Test
    fun `reload closes retired mcp handles only after pinned lease is released`() {
        val source = MutableWorkspaceSource(
            listOf(
                WorkspaceConfig(
                    id = "default",
                    name = "Default",
                    mcpServers = listOf(WorkspaceMcpServerConfig(id = "local-mcp-v1", command = "mcp-server-v1")),
                ),
            ),
        )
        val oldHandle = FakeMcpHandle()
        val newHandle = FakeMcpHandle()
        val resolver = FakeMcpToolResolver(
            result = mcpResolution("local-mcp.search-v1", oldHandle),
        )
        val controller = workspaceController(
            source = source,
            mcpToolResolver = resolver,
        )
        val lease = controller.resolve().lease ?: error("missing lease")

        resolver.result = mcpResolution("local-mcp.search-v2", newHandle)
        source.workspaces = listOf(
            WorkspaceConfig(
                id = "default",
                name = "Default",
                mcpServers = listOf(WorkspaceMcpServerConfig(id = "local-mcp-v2", command = "mcp-server-v2")),
            ),
        )

        val reload = controller.reload("default")

        assertTrue(reload.success)
        assertEquals(0, oldHandle.closeCount)
        assertEquals(0, newHandle.closeCount)

        lease.close()

        assertEquals(1, oldHandle.closeCount)
        assertEquals(0, newHandle.closeCount)

        controller.close()

        assertEquals(1, oldHandle.closeCount)
        assertEquals(1, newHandle.closeCount)
    }

    @Test
    fun `close defers current mcp handles until pinned lease is released`() {
        val handle = FakeMcpHandle()
        val controller = workspaceController(
            source = MutableWorkspaceSource(
                listOf(
                    WorkspaceConfig(
                        id = "default",
                        name = "Default",
                        mcpServers = listOf(WorkspaceMcpServerConfig(id = "local-mcp", command = "mcp-server")),
                    ),
                ),
            ),
            mcpToolResolver = FakeMcpToolResolver(result = mcpResolution("local-mcp.search", handle)),
        )
        val lease = controller.resolve().lease ?: error("missing lease")

        controller.close()

        assertEquals(0, handle.closeCount)

        lease.close()

        assertEquals(1, handle.closeCount)
    }

    @Test
    fun `multiple leases keep retired mcp handles open until last lease is released`() {
        val source = MutableWorkspaceSource(
            listOf(
                WorkspaceConfig(
                    id = "default",
                    name = "Default",
                    mcpServers = listOf(WorkspaceMcpServerConfig(id = "local-mcp-v1", command = "mcp-server-v1")),
                ),
            ),
        )
        val oldHandle = FakeMcpHandle()
        val newHandle = FakeMcpHandle()
        val resolver = FakeMcpToolResolver(result = mcpResolution("local-mcp.search-v1", oldHandle))
        val controller = workspaceController(source = source, mcpToolResolver = resolver)
        val firstLease = controller.resolve().lease ?: error("missing first lease")
        val secondLease = controller.resolve().lease ?: error("missing second lease")

        resolver.result = mcpResolution("local-mcp.search-v2", newHandle)
        source.workspaces = listOf(
            WorkspaceConfig(
                id = "default",
                name = "Default",
                mcpServers = listOf(WorkspaceMcpServerConfig(id = "local-mcp-v2", command = "mcp-server-v2")),
            ),
        )
        assertTrue(controller.reload("default").success)

        firstLease.close()
        assertEquals(0, oldHandle.closeCount)

        secondLease.close()
        assertEquals(1, oldHandle.closeCount)
        assertEquals(0, newHandle.closeCount)
    }

    @Test
    fun `failed mcp candidate initialization keeps old snapshot and closes candidates`() {
        val source = MutableWorkspaceSource(
            listOf(WorkspaceConfig(id = "default", name = "Default")),
        )
        val candidateHandle = FakeMcpHandle()
        val resolver = FakeMcpToolResolver(
            failure = WorkspaceMcpResolutionException(
                message = "boom",
                handles = listOf(candidateHandle),
            ),
        )
        val controller = workspaceController(
            source = source,
            mcpToolResolver = resolver,
        )
        val before = controller.get("default") ?: error("missing default")

        source.workspaces = listOf(
            WorkspaceConfig(
                id = "default",
                name = "Default",
                mcpServers = listOf(
                    WorkspaceMcpServerConfig(id = "local-mcp", command = "mcp-server"),
                ),
            ),
        )
        val result = controller.reload("default")

        assertFalse(result.success)
        assertEquals(before.version, controller.get("default")?.version)
        assertEquals(before.version, result.snapshotVersion)
        assertTrue(result.errorSummary?.contains("MCP initialization failed") == true)
        assertEquals(1, candidateHandle.closeCount)
    }

    @Test
    fun `skill case exposes workspace scoped skill set`() = kotlinx.coroutines.runBlocking {
        val skills = SkillController().apply {
            register(TestSkill(name = "ops", response = "ops handled", priority = 10))
            register(TestSkill(name = "general", response = "general handled", priority = 1))
        }
        val skillCase = SkillCase(skills)
        val controller = workspaceController(
            source = MutableWorkspaceSource(
                listOf(
                    WorkspaceConfig(
                        id = "default",
                        name = "Default",
                        skills = listOf(
                            WorkspaceSkillConfig(
                                name = "ops",
                                settings = mapOf("mode" to "incident"),
                            ),
                        ),
                    ),
                ),
            ),
            skillCase = skillCase,
        )
        val snapshot = controller.get("default") ?: error("missing default")

        val skillSet = skillCase.getWorkspaceSkillSet(snapshot)

        assertEquals(listOf("ops"), skillSet.skillNames)
        assertEquals("incident", skillSet.setting("ops", "mode"))
        assertEquals("ops handled", skillSet.dispatch("please handle this"))
    }

    @Test
    fun `failed reload keeps old snapshot active`() {
        val source = MutableWorkspaceSource(
            listOf(WorkspaceConfig(id = "default", name = "Default")),
        )
        val controller = workspaceController(source)
        val before = controller.get("default") ?: error("missing default")

        source.workspaces = listOf(
            WorkspaceConfig(id = "default", name = "Default"),
            WorkspaceConfig(id = "default", name = "Duplicate"),
        )
        val result = controller.reload("default")

        assertFalse(result.success)
        assertEquals(before.version, controller.get("default")?.version)
        assertEquals(before.version, result.snapshotVersion)
        assertTrue(result.errorSummary?.contains("Duplicate workspace id") == true)
    }

    private fun workspaceController(workspaces: List<WorkspaceConfig>): WorkspaceController {
        return workspaceController(MutableWorkspaceSource(workspaces))
    }

    private fun workspaceController(source: WorkspaceConfigSource): WorkspaceController {
        val skills = SkillController().apply {
            register(DefaultSkill())
        }
        return workspaceController(source, SkillCase(skills))
    }

    private fun workspaceController(
        source: WorkspaceConfigSource,
        skillCase: SkillCase = SkillCase(SkillController().apply { register(DefaultSkill()) }),
        mcpToolResolver: WorkspaceMcpToolResolver = WorkspaceMcpToolResolver.Noop,
    ): WorkspaceController {
        val tools = ToolController().apply {
            register(ListToolsTool { getRegisteredTools() })
            register(SystemInfoTool())
            register(WebSearchTool())
        }
        return WorkspaceController(
            source = source,
            toolController = tools,
            skillCase = skillCase,
            mcpToolResolver = mcpToolResolver,
            nowProvider = { 1_000L },
        )
    }

    private fun workspaceController(
        source: WorkspaceConfigSource,
        mcpToolResolver: WorkspaceMcpToolResolver,
    ): WorkspaceController = workspaceController(
        source = source,
        skillCase = SkillCase(SkillController().apply { register(DefaultSkill()) }),
        mcpToolResolver = mcpToolResolver,
    )

    private class MutableWorkspaceSource(
        var workspaces: List<WorkspaceConfig>,
    ) : WorkspaceConfigSource {
        override fun load(): WorkspaceConfigSet = WorkspaceConfigSet(workspaces)
    }

    private class TestSkill(
        override val name: String,
        private val response: String,
        override val priority: Int,
    ) : Skill {
        override val description: String = name

        override suspend fun canHandle(message: String): Boolean = true

        override suspend fun execute(message: String): String = response
    }

    private fun mcpResolution(toolName: String, handle: WorkspaceMcpClientHandle): WorkspaceMcpToolResolution {
        return WorkspaceMcpToolResolution(
            resources = listOf(
                WorkspaceMcpResource(
                    tool = com.heyanle.priestess.bot.testkit.FakeTool(name = toolName),
                    handle = handle,
                ),
            ),
            handles = listOf(handle),
        )
    }

    private class FakeMcpToolResolver(
        var result: WorkspaceMcpToolResolution = WorkspaceMcpToolResolution(),
        private val failure: RuntimeException? = null,
    ) : WorkspaceMcpToolResolver {
        val calls = mutableListOf<List<WorkspaceMcpServerConfig>>()

        override fun resolve(
            workspaceId: String,
            servers: List<WorkspaceMcpServerConfig>,
        ): WorkspaceMcpToolResolution {
            calls += servers
            failure?.let { throw it }
            return result
        }
    }

    private class FakeMcpHandle : WorkspaceMcpClientHandle {
        var closeCount = 0
            private set

        override fun close() {
            closeCount += 1
        }
    }
}
