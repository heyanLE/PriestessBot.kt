package com.heyanle.priestess.bot.workspace

import com.heyanle.priestess.bot.config.AgentConfig
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorkspaceDirectoryLoaderTest {
    private val loader = WorkspaceDirectoryLoader()

    @Test
    fun `loads workspace config skill descriptors and mcp declarations from directory`() {
        val root = Files.createTempDirectory("workspace-loader")
        Files.createDirectories(root.resolve("skills").resolve("research"))
        Files.writeString(
            root.resolve("config.yaml"),
            """
            name: Incident Workspace
            providerName: ops-provider
            rules:
              - no-production-writes
            skills:
              - name: research
                settings:
                  mode: incident
            """.trimIndent(),
        )
        Files.writeString(
            root.resolve("skills").resolve("research").resolve("SKILL.md"),
            """
            ---
            name: research
            description: Incident research workflow
            ---
            # Skill: research
            """.trimIndent(),
        )
        Files.writeString(
            root.resolve("mcpserver.json"),
            """
            {
              "mcpServers": {
                "ops-mcp": {
                  "command": "ops-mcp-server",
                  "args": ["--stdio"],
                  "env": {
                    "TOKEN": "secret-token"
                  }
                }
              }
            }
            """.trimIndent(),
        )

        val result = loader.load(
            rootDir = root,
            defaults = WorkspaceConfig(
                id = WorkspaceConfig.DEFAULT_WORKSPACE_ID,
                name = "Default Workspace",
                agents = listOf(AgentConfig(name = "assistant", model = "gpt-test")),
            ),
        )

        assertEquals("Incident Workspace", result.config.name)
        assertEquals("ops-provider", result.config.providerName)
        assertEquals(listOf("no-production-writes"), result.config.rules)
        assertEquals(1, result.skillDescriptors.size)
        assertEquals("research", result.skillDescriptors.single().name)
        assertEquals("Incident research workflow", result.skillDescriptors.single().description)
        assertEquals("incident", result.skillDescriptors.single().settings["mode"])
        assertEquals(root.resolve("skills").resolve("research").toAbsolutePath().normalize().absolutePathString(), result.skillDescriptors.single().directoryPath)
        assertEquals(root.resolve("skills").resolve("research").resolve("SKILL.md").toAbsolutePath().normalize().absolutePathString(), result.skillDescriptors.single().skillMarkdownPath)
        assertEquals(1, result.mcpServers.size)
        assertEquals("ops-mcp", result.mcpServers.single().id)
        assertEquals("ops-mcp-server", result.mcpServers.single().command)
        assertEquals(listOf("--stdio"), result.mcpServers.single().args)
        assertEquals("secret-token", result.mcpServers.single().env["TOKEN"])
        assertTrue(result.diagnostics.isEmpty())
    }

    @Test
    fun `reports diagnostics for invalid workspace files`() {
        val root = Files.createTempDirectory("workspace-loader-invalid")
        Files.createDirectories(root.resolve("skills").resolve("broken-skill"))
        Files.writeString(
            root.resolve("config.yaml"),
            """
            tools: bad-value
            """.trimIndent(),
        )
        Files.writeString(root.resolve("mcpserver.json"), """{"mcpServers":"broken"}""")

        val result = loader.load(
            rootDir = root,
            defaults = WorkspaceConfig(
                agents = listOf(AgentConfig(name = "assistant", model = "gpt-test")),
            ),
        )

        assertTrue(result.diagnostics.any { it.contains("Failed to decode workspace config") })
        assertTrue(result.diagnostics.any { it.contains("missing SKILL.md") })
        assertTrue(result.diagnostics.any { it.contains("must use an object or array under 'mcpServers'") })
    }
}
