package com.heyanle.priestess.bot.tool

import com.heyanle.priestess.bot.observability.MetricsRegistry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/**
 * Executes tool calls by resolving tool names, validating arguments,
 * and invoking the corresponding [FunctionTool.execute].
 */
class ToolExecutor(
    private val registry: ToolController,
    private val metricsRegistry: MetricsRegistry = MetricsRegistry(),
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Execute a single tool call.
     *
     * @param context The agent tool context.
     * @param toolCallName The name of the tool to call.
     * @param argumentsJson The arguments as a JSON string (from LLM tool call).
     * @return ToolResult with success/error status.
     */
    suspend fun execute(
        context: AgentToolContext,
        toolCallName: String,
        argumentsJson: String,
    ): ToolResult {
        val tool = registry.get(toolCallName)
            ?: return ToolResult.error("Unknown tool: $toolCallName").also {
                recordToolCall(toolCallName, "error")
            }

        val args = try {
            parseArguments(argumentsJson)
        } catch (e: Exception) {
            return ToolResult.error("Failed to parse arguments for tool '$toolCallName': ${e.message}").also {
                recordToolCall(tool.schema.name, "error")
            }
        }

        return try {
            tool.execute(context, args).also {
                recordToolCall(tool.schema.name, if (it.success) "success" else "error")
            }
        } catch (e: Exception) {
            ToolResult.error("Tool '$toolCallName' execution failed: ${e.message}").also {
                recordToolCall(tool.schema.name, "error")
            }
        }
    }

    /**
     * Execute multiple tool calls in sequence.
     *
     * @param context The agent tool context.
     * @param toolCalls List of (toolCallId, toolCallName, argumentsJson) tuples.
     * @return Map of toolCallId to ToolResult.
     */
    suspend fun executeBatch(
        context: AgentToolContext,
        toolCalls: List<Triple<String, String, String>>, // (id, name, arguments)
    ): Map<String, ToolResult> {
        val results = mutableMapOf<String, ToolResult>()
        for ((id, name, arguments) in toolCalls) {
            results[id] = execute(context, name, arguments)
        }
        return results
    }

    private fun recordToolCall(toolName: String, status: String) {
        metricsRegistry.incrementCounter(
            "priestess_tool_calls_total",
            mapOf("tool" to toolName, "status" to status),
        )
    }

    /**
     * Parse a JSON string of arguments into a Map<String, String>.
     */
    private fun parseArguments(argumentsJson: String): Map<String, String> {
        if (argumentsJson.isBlank()) return emptyMap()

        val jsonElement = json.parseToJsonElement(argumentsJson)
        val jsonObj = jsonElement.jsonObject

        val result = mutableMapOf<String, String>()
        for ((key, value) in jsonObj) {
            // Convert JSON values to their string representation
            result[key] = when {
                value.toString().startsWith("\"") -> {
                    // String value: strip quotes
                    value.toString().removeSurrounding("\"")
                }
                else -> value.toString()
            }
        }
        return result
    }
}
