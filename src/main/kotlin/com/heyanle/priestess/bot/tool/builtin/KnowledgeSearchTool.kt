package com.heyanle.priestess.bot.tool.builtin

import com.heyanle.priestess.bot.knowledge.KnowledgeCase
import com.heyanle.priestess.bot.tool.AgentToolContext
import com.heyanle.priestess.bot.tool.FunctionTool
import com.heyanle.priestess.bot.tool.ParameterDef
import com.heyanle.priestess.bot.tool.ToolParameters
import com.heyanle.priestess.bot.tool.ToolResult
import com.heyanle.priestess.bot.tool.ToolSchema
import com.heyanle.priestess.bot.tool.annotation.Tool

@Tool(name = "knowledge_search", description = "Search local knowledge bases for relevant context")
class KnowledgeSearchTool(
    private val knowledgeCaseProvider: (() -> KnowledgeCase)? = null,
) : FunctionTool() {
    override val schema = ToolSchema(
        name = "knowledge_search",
        description = "Search local knowledge bases for relevant context.",
        parameters = ToolParameters(
            properties = listOf(
                ParameterDef(name = "query", type = "string", description = "Search query", required = true),
                ParameterDef(name = "knowledgeBaseId", type = "string", description = "Optional knowledge base id"),
                ParameterDef(name = "limit", type = "string", description = "Maximum number of snippets"),
            ),
            required = listOf("query"),
        ),
    )

    override suspend fun execute(context: AgentToolContext, args: Map<String, String>): ToolResult {
        val knowledgeCase = knowledgeCaseProvider?.invoke()
            ?: return ToolResult.error("Knowledge search is not configured")
        val query = args["query"]?.trim().orEmpty()
        if (query.isBlank()) return ToolResult.error("query is required")
        val limit = args["limit"]?.toIntOrNull() ?: 5
        val baseId = args["knowledgeBaseId"]?.takeIf { it.isNotBlank() }
        val results = knowledgeCase.search(query, baseId, limit)
        if (results.isEmpty()) {
            return ToolResult.success("No knowledge results found for: $query")
        }
        val output = buildString {
            appendLine("Knowledge results for: $query")
            results.forEachIndexed { index, result ->
                appendLine()
                appendLine("${index + 1}. ${result.chunk.documentName} (score=${"%.2f".format(result.score)})")
                appendLine(result.chunk.content)
            }
        }
        return ToolResult.success(output.trim())
    }
}
