package com.heyanle.priestess.bot.tool.builtin

import com.heyanle.priestess.bot.core.db.DatabaseController
import com.heyanle.priestess.bot.knowledge.KnowledgeCase
import com.heyanle.priestess.bot.knowledge.KnowledgeController
import com.heyanle.priestess.bot.tool.AgentToolContext
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class KnowledgeSearchToolTest {
    @Test
    fun `knowledge search tool returns snippets`() = runBlocking {
        val knowledge = testKnowledgeCase()
        val base = knowledge.createBase("Docs")
        knowledge.addTextDocument(base.id, "dashboard.md", "Dashboard can test Agent responses.")
        val tool = KnowledgeSearchTool { knowledge }

        val result = tool.execute(AgentToolContext(), mapOf("query" to "Agent Dashboard"))

        assertTrue(result.success)
        assertTrue(result.output.contains("dashboard.md"))
        assertTrue(result.output.contains("Agent"))
    }

    @Test
    fun `knowledge search tool reports empty results successfully`() = runBlocking {
        val knowledge = testKnowledgeCase()
        knowledge.createBase("Docs")
        val tool = KnowledgeSearchTool { knowledge }

        val result = tool.execute(AgentToolContext(), mapOf("query" to "missing"))

        assertTrue(result.success)
        assertTrue(result.output.contains("No knowledge results"))
    }

    private fun testKnowledgeCase(): KnowledgeCase {
        val dbPath = Files.createTempFile("priestess-knowledge-tool", ".sqlite")
        val db = DatabaseController(dbPath.toString())
        return KnowledgeCase(KnowledgeController(db))
    }
}
