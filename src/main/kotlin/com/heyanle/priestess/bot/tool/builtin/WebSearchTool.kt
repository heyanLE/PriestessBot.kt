package com.heyanle.priestess.bot.tool.builtin

import com.heyanle.priestess.bot.tool.AgentToolContext
import com.heyanle.priestess.bot.tool.FunctionTool
import com.heyanle.priestess.bot.tool.ParameterDef
import com.heyanle.priestess.bot.tool.ToolParameters
import com.heyanle.priestess.bot.tool.ToolResult
import com.heyanle.priestess.bot.tool.ToolCapabilities
import com.heyanle.priestess.bot.tool.ToolRiskLevel
import com.heyanle.priestess.bot.tool.ToolSchema
import com.heyanle.priestess.bot.tool.annotation.Tool
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Built-in web search tool.
 *
 * Performs internet searches via the SearXNG API (configurable).
 * Returns formatted search results as a string.
 */
@Tool(name = "web_search", description = "Search the internet for up-to-date information")
class WebSearchTool : FunctionTool() {

    private val json = Json { ignoreUnknownKeys = true }

    override val schema = ToolSchema(
        name = "web_search",
        description = "Search the internet for up-to-date information. Use this when you need current information, facts, or data that may not be in your training data.",
        parameters = ToolParameters(
            properties = listOf(
                ParameterDef(name = "query", type = "string", description = "The search query string", required = true),
                ParameterDef(name = "num_results", type = "integer", description = "Number of results (default: 5, max: 10)", required = false),
            ),
            required = listOf("query"),
        ),
        riskLevel = ToolRiskLevel.EXTERNAL_READ,
        requiredCapabilities = listOf(ToolCapabilities.NETWORK, ToolCapabilities.PROVIDER_SEARCH),
        defaultEnabled = false,
        auditLog = false,
    )

    private val client = HttpClient(CIO) {
        engine { requestTimeout = 15_000 }
    }

    override suspend fun execute(context: AgentToolContext, args: Map<String, String>): ToolResult {
        val query = args["query"] ?: return ToolResult.error("Missing required parameter: query")
        val numResults = args["num_results"]?.toIntOrNull()?.coerceIn(1, 10) ?: 5

        return try {
            val results = searchSearXNG(query, numResults)
            if (results.isEmpty()) {
                ToolResult.success("No results found for query: $query")
            } else {
                ToolResult.success(formatResults(query, results))
            }
        } catch (e: Exception) {
            ToolResult.error("Web search failed: ${e.message}")
        }
    }

    private suspend fun searchSearXNG(query: String, numResults: Int): List<SearchResult> {
        val url = "https://searx.be/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}&format=json&categories=general"
        val response: HttpResponse = client.get(url)
        val body = response.bodyAsText()
        val root = json.parseToJsonElement(body).jsonObject
        val results = root["results"]?.jsonArray ?: return emptyList()
        return results.take(numResults).map { element ->
            val obj = element.jsonObject
            SearchResult(
                title = obj["title"]?.jsonPrimitive?.content ?: "",
                url = obj["url"]?.jsonPrimitive?.content ?: "",
                snippet = obj["content"]?.jsonPrimitive?.content ?: obj["snippet"]?.jsonPrimitive?.content ?: "",
            )
        }
    }

    private fun formatResults(query: String, results: List<SearchResult>): String {
        val sb = StringBuilder()
        sb.appendLine("Search results for: $query")
        sb.appendLine("---")
        results.forEachIndexed { index, result ->
            sb.appendLine("${index + 1}. ${result.title}")
            sb.appendLine("   URL: ${result.url}")
            if (result.snippet.isNotBlank()) sb.appendLine("   ${result.snippet.take(300)}")
            sb.appendLine()
        }
        return sb.toString().trimEnd()
    }

    private data class SearchResult(val title: String, val url: String, val snippet: String)
}
