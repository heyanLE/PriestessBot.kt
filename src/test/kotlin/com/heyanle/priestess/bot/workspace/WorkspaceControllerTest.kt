package com.heyanle.priestess.bot.workspace

import com.heyanle.priestess.bot.config.AgentConfig
import com.heyanle.priestess.bot.config.PriestessConfig
import com.heyanle.priestess.bot.skill.DefaultSkill
import com.heyanle.priestess.bot.skill.Skill
import com.heyanle.priestess.bot.skill.SkillCase
import com.heyanle.priestess.bot.skill.SkillController
import com.heyanle.priestess.bot.testkit.FakeTool
import com.heyanle.priestess.bot.tool.ToolCase
import com.heyanle.priestess.bot.tool.ToolController
import com.heyanle.priestess.bot.tool.ToolSchema
import com.heyanle.priestess.bot.tool.builtin.ListToolsTool
import com.heyanle.priestess.bot.tool.builtin.SystemInfoTool
import com.heyanle.priestess.bot.tool.builtin.UnloadSkillTool
import com.heyanle.priestess.bot.tool.builtin.UseSkillTool
import com.heyanle.priestess.bot.tool.builtin.WebSearchTool
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class WorkspaceControllerTest {
    @Test
    fun `derives default workspace from existing global config`() {
        val config = PriestessConfig(
            agent = AgentConfig(name = "assistant", providerName = "openai", enabledTools = listOf("system_info")),
        )
        val source = ConfigBackedWorkspaceConfigSource { config }

        val loaded = source.load().defaults.baseConfig

        assertEquals(WorkspaceConfig.DEFAULT_WORKSPACE_ID, loaded.id)
        assertTrue(loaded.isDefault)
        assertEquals("assistant", loaded.agents.single().name)
        assertEquals("openai", loaded.providerName)
        assertEquals(listOf("system_info"), loaded.tools.enabledTools)
    }

    @Test
    fun `resolves explicit prepared workspace and falls back to default`() {
        val source = MutableWorkspaceSource(
            listOf(
                WorkspaceConfig(id = "default", name = "Default", enabled = true, isDefault = true),
                WorkspaceConfig(id = "ops", name = "Ops", enabled = true),
            ),
        )
        val controller = workspaceController(source)

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
            toolCase = ToolCase(tools),
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
        assertTrue(snapshot.mcpToolNames.isEmpty())
    }

    @Test
    fun `directory backed snapshot stores mcp declarations without eager tool initialization`() {
        val workspaceDir = Files.createTempDirectory("workspace-directory-mcp")
        Files.createDirectories(workspaceDir.resolve("skills"))
        Files.writeString(
            workspaceDir.resolve("config.yaml"),
            """
            name: Directory Workspace
            """.trimIndent(),
        )
        Files.writeString(
            workspaceDir.resolve("mcpserver.json"),
            """
            {
              "mcpServers": {
                "dir-mcp": {
                  "command": "dir-mcp-server"
                }
              }
            }
            """.trimIndent(),
        )
        val resolver = FakeMcpToolResolver()
        val controller = workspaceController(
            source = WorkspaceConfigSource {
                WorkspaceConfigSet(
                    defaultWorkspaceDir = workspaceDir.toAbsolutePath().normalize().absolutePathString(),
                    defaults = WorkspaceRuntimeDefaults(
                        baseConfig = WorkspaceConfig(
                            id = WorkspaceConfig.DEFAULT_WORKSPACE_ID,
                            name = "Default",
                            isDefault = true,
                        ),
                    ),
                )
            },
            mcpToolResolver = resolver,
        )

        val snapshot = controller.resolve().snapshot

        assertEquals(emptyList(), resolver.calls)
        assertEquals(listOf("dir-mcp"), snapshot.mcpServerIds)
        assertEquals(emptyList(), snapshot.mcpToolNames)
        assertTrue(snapshot.toolNames.none { it.startsWith("dir-mcp.") })
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
        val controller = WorkspaceController(
            source = source,
            toolCase = ToolCase(tools),
            skillCase = skillCase,
            mcpToolResolver = mcpToolResolver,
            nowProvider = { 1_000L },
        )
        if (source is MutableWorkspaceSource) {
            source.preparedWorkspaceIds()
                .filter { it != WorkspaceConfig.DEFAULT_WORKSPACE_ID }
                .forEach { workspaceId ->
                    controller.prepare(source.directoryFor(workspaceId), "seed workspace")
                }
        }
        return controller
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
        initialWorkspaces: List<WorkspaceConfig>,
    ) : WorkspaceConfigSource {
        private val json = Json { prettyPrint = true; encodeDefaults = true }
        private val workspaceDirs = linkedMapOf<String, Path>()

        var workspaces: List<WorkspaceConfig> = initialWorkspaces
            set(value) {
                field = value
                sync(value)
            }

        init {
            sync(workspaces)
        }

        override fun load(): WorkspaceConfigSet = WorkspaceConfigSet(
            workspaces = workspaces,
            defaultWorkspaceDir = defaultWorkspaceConfig()
                ?.let { workspaceDirs[it.id] }
                ?.toAbsolutePath()
                ?.normalize()
                ?.absolutePathString()
                .orEmpty(),
            defaults = WorkspaceRuntimeDefaults(defaultWorkspaceConfig() ?: WorkspaceConfig(isDefault = true)),
        )

        fun preparedWorkspaceIds(): List<String> = workspaces.map { it.id }.distinct()

        fun directoryFor(id: String): String {
            return workspaceDirs.getValue(id).toAbsolutePath().normalize().absolutePathString()
        }

        private fun defaultWorkspaceConfig(): WorkspaceConfig? {
            return workspaces.firstOrNull { it.isDefault } ?: workspaces.firstOrNull()
        }

        private fun sync(configs: List<WorkspaceConfig>) {
            configs.distinctBy { it.id }.forEach { config ->
                val root = workspaceDirs.getOrPut(config.id) {
                    Files.createTempDirectory("workspace-${config.id}")
                }
                writeWorkspace(root, config)
            }
        }

        private fun writeWorkspace(root: Path, config: WorkspaceConfig) {
            deleteChildren(root)
            Files.createDirectories(root.resolve("skills"))
            Files.writeString(root.resolve("config.yaml"), json.encodeToString(WorkspaceConfig.serializer(), config))
            Files.writeString(
                root.resolve("mcpserver.json"),
                buildMcpJson(config.mcpServers),
            )
            config.skills.forEach { skill ->
                val skillDir = root.resolve("skills").resolve(skill.name)
                Files.createDirectories(skillDir)
                Files.writeString(
                    skillDir.resolve("SKILL.md"),
                    """
                    ---
                    name: ${skill.name}
                    description: ${skill.name} description
                    ---
                    # Skill: ${skill.name}
                    """.trimIndent(),
                )
            }
        }

        private fun buildMcpJson(servers: List<WorkspaceMcpServerConfig>): String {
            return buildJsonObject {
                putJsonObject("mcpServers") {
                    servers.forEach { server ->
                        putJsonObject(server.id) {
                            put("enabled", server.enabled)
                            put("transport", server.transport)
                            put("command", server.command)
                            put("url", server.url)
                            putJsonArray("args") {
                                server.args.forEach { add(JsonPrimitive(it)) }
                            }
                            putJsonObject("env") {
                                server.env.forEach { (key, value) ->
                                    put(key, value)
                                }
                            }
                        }
                    }
                }
            }
                .toString()
        }

        private fun deleteChildren(root: Path) {
            if (!Files.exists(root)) return
            Files.list(root).use { children ->
                children.forEach { child ->
                    Files.walk(child)
                        .sorted(Comparator.reverseOrder())
                        .forEach(Files::deleteIfExists)
                }
            }
        }
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

    private class FakeMcpToolResolver : WorkspaceMcpToolResolver {
        val calls = mutableListOf<List<WorkspaceMcpServerConfig>>()

        override fun resolve(
            workspaceId: String,
            servers: List<WorkspaceMcpServerConfig>,
        ): WorkspaceMcpToolResolution {
            calls += servers
            return WorkspaceMcpToolResolution()
        }
    }
}
