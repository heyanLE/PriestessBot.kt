package com.heyanle.priestess.bot.tool.builtin

import com.heyanle.priestess.bot.tool.AgentToolContext
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TerminalToolsTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `terminal foreground command returns output`() = runBlocking {
        val root = Files.createTempDirectory("terminal-foreground")
        root.resolve("hello.txt").writeText("hello\n")
        val context = AgentToolContext(metadata = mapOf("workspaceRootDir" to root.toString()))

        val result = TerminalTool().execute(context, mapOf("command" to "cat hello.txt"))
        val response = json.decodeFromString<TerminalCommandResponse>(result.output)

        assertTrue(result.success)
        assertEquals("exited", response.status)
        assertTrue(response.output.contains("hello"))
    }

    @Test
    fun `background terminal session can be waited and read`() = runBlocking {
        val root = Files.createTempDirectory("terminal-background")
        val context = AgentToolContext(metadata = mapOf("workspaceRootDir" to root.toString()))
        val terminal = TerminalTool()
        val process = ProcessTool()
        val reader = ReadTerminalTool()

        val started = terminal.execute(
            context,
            mapOf("command" to "printf ready", "background" to "true"),
        )
        val session = json.decodeFromString<TerminalCommandResponse>(started.output)
        assertTrue(started.success)

        val waited = process.execute(context, mapOf("action" to "wait", "session_id" to session.sessionId, "timeout_ms" to "5000"))
        val waitResponse = json.decodeFromString<ProcessSessionSummary>(waited.output)
        assertEquals(session.sessionId, waitResponse.sessionId)

        val log = reader.execute(context, mapOf("session_id" to session.sessionId))
        val logResponse = json.decodeFromString<ProcessLogResponse>(log.output)
        assertTrue(logResponse.text.contains("ready"))
    }
}
