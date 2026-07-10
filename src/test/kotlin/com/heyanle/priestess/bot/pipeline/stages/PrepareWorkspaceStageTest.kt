package com.heyanle.priestess.bot.pipeline.stages

import com.heyanle.priestess.bot.config.AgentConfig
import com.heyanle.priestess.bot.config.PlatformConfig
import com.heyanle.priestess.bot.config.PriestessConfig
import com.heyanle.priestess.bot.config.WorkspaceRuntimeConfig
import com.heyanle.priestess.bot.pipeline.PipelineContext
import com.heyanle.priestess.bot.platform.MessageChain
import com.heyanle.priestess.bot.platform.MessageEvent
import com.heyanle.priestess.bot.platform.MessageSession
import com.heyanle.priestess.bot.platform.SessionType
import com.heyanle.priestess.bot.testkit.FakePlatform
import com.heyanle.priestess.bot.testkit.testConfigCase
import com.heyanle.priestess.bot.tool.ToolCase
import com.heyanle.priestess.bot.tool.ToolController
import com.heyanle.priestess.bot.tool.builtin.SystemInfoTool
import com.heyanle.priestess.bot.workspace.ConfigBackedWorkspaceConfigSource
import com.heyanle.priestess.bot.workspace.WorkspaceCase
import com.heyanle.priestess.bot.workspace.WorkspaceController
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.io.path.absolutePathString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PrepareWorkspaceStageTest {
    @Test
    fun `message workspace dir override wins over platform and config defaults`() = runBlocking {
        val configDir = createWorkspaceDir("config-workspace", "Config Workspace")
        val platformDir = createWorkspaceDir("platform-workspace", "Platform Workspace")
        val messageDir = createWorkspaceDir("message-workspace", "Message Workspace")
        val configCase = testConfigCase(
            PriestessConfig(
                agent = AgentConfig(name = "assistant", model = "test-model"),
                workspace = WorkspaceRuntimeConfig(defaultDir = configDir),
                platforms = listOf(
                    PlatformConfig(
                        name = "fake-platform",
                        type = "fake-platform",
                        config = mapOf("workspace_dir" to platformDir),
                    ),
                ),
            ),
            prefix = "prepare-workspace-message-override",
        )
        val stage = stage(configCase)
        val ctx = pipelineContext(
            metadata = mapOf(
                "senderId" to "user-1",
                "workspace_dir" to messageDir,
            ),
        )

        stage.process(ctx)

        assertEquals(messageDir, ctx.workspaceRootDir)
        assertEquals("message workspace_dir", ctx.workspaceResolutionReason)
        assertEquals("Message Workspace", assertNotNull(ctx.workspaceSnapshot).name)
    }

    @Test
    fun `platform workspace dir override wins over config default`() = runBlocking {
        val configDir = createWorkspaceDir("config-workspace", "Config Workspace")
        val platformDir = createWorkspaceDir("platform-workspace", "Platform Workspace")
        val configCase = testConfigCase(
            PriestessConfig(
                agent = AgentConfig(name = "assistant", model = "test-model"),
                workspace = WorkspaceRuntimeConfig(defaultDir = configDir),
                platforms = listOf(
                    PlatformConfig(
                        name = "fake-platform",
                        type = "fake-platform",
                        config = mapOf("workspaceDir" to platformDir),
                    ),
                ),
            ),
            prefix = "prepare-workspace-platform-override",
        )
        val stage = stage(configCase)
        val ctx = pipelineContext()

        stage.process(ctx)

        assertEquals(platformDir, ctx.workspaceRootDir)
        assertEquals("platform workspace_dir", ctx.workspaceResolutionReason)
        assertEquals("Platform Workspace", assertNotNull(ctx.workspaceSnapshot).name)
    }

    @Test
    fun `config default workspace dir is used when no higher priority override exists`() = runBlocking {
        val configDir = createWorkspaceDir("config-workspace", "Config Workspace")
        val configCase = testConfigCase(
            PriestessConfig(
                agent = AgentConfig(name = "assistant", model = "test-model"),
                workspace = WorkspaceRuntimeConfig(defaultDir = configDir),
                platforms = listOf(
                    PlatformConfig(name = "other-platform", type = "other-platform"),
                ),
            ),
            prefix = "prepare-workspace-config-default",
        )
        val stage = stage(configCase)
        val ctx = pipelineContext()

        stage.process(ctx)

        assertEquals(configDir, ctx.workspaceRootDir)
        assertEquals("config default workspace dir", ctx.workspaceResolutionReason)
        assertEquals("Config Workspace", assertNotNull(ctx.workspaceSnapshot).name)
    }

    private fun stage(
        configCase: com.heyanle.priestess.bot.config.ConfigCase,
    ): PrepareWorkspaceStage {
        val tools = ToolController().apply {
            register(SystemInfoTool())
        }
        val workspaceController = WorkspaceController(
            source = ConfigBackedWorkspaceConfigSource(configCase),
            toolCase = ToolCase(tools),
            nowProvider = { 1_000L },
        )
        return PrepareWorkspaceStage(configCase, WorkspaceCase(workspaceController))
    }

    private fun pipelineContext(
        metadata: Map<String, String> = mapOf("senderId" to "user-1"),
        platform: FakePlatform = FakePlatform(),
    ): PipelineContext {
        val session = MessageSession(
            id = "session-1",
            type = SessionType.PRIVATE,
            platformName = platform.metadata.name,
            metadata = metadata,
        )
        return PipelineContext(
            MessageEvent(
                platform = platform,
                session = session,
                chain = MessageChain.text("hello"),
                messageId = "message-1",
            ),
        )
    }

    private fun createWorkspaceDir(prefix: String, name: String): String {
        val root = Files.createTempDirectory(prefix)
        Files.createDirectories(root.resolve("skills"))
        Files.writeString(
            root.resolve("config.yaml"),
            """
            name: $name
            """.trimIndent(),
        )
        Files.writeString(root.resolve("mcpserver.json"), """{"mcpServers":{}}""")
        return root.toAbsolutePath().normalize().absolutePathString()
    }
}
