package com.heyanle.priestess.bot.tool

import com.heyanle.priestess.bot.observability.MetricsRegistry
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/**
 * Executes tool calls by resolving tool names, validating arguments,
 * and invoking the corresponding [FunctionTool.execute].
 */
class ToolExecutor(
    private val registry: ToolController,
    private val metricsRegistry: MetricsRegistry = MetricsRegistry(),
    private val policy: ToolPolicy = ToolPolicy.allowAll(),
    private val defaultTimeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
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
        timeoutMillis: Long = defaultTimeoutMillis,
    ): ToolResult {
        val tool = context.scopedTools.firstOrNull { it.schema.name == toolCallName }
            ?: registry.get(toolCallName)
            ?: return ToolResult.error("Unknown tool: $toolCallName").also {
                recordToolCall(toolCallName, "error")
            }
        val workspaceAllowedTools = workspaceToolNames(context)
        if (workspaceAllowedTools != null && tool.schema.name !in workspaceAllowedTools) {
            return ToolResult.permissionDenied(
                ToolPolicyDecision.denied(
                    code = ToolPolicyDenialCode.DISABLED_TOOL,
                    message = "Tool '${tool.schema.name}' is not enabled in workspace",
                    auditLog = tool.schema.auditLog,
                ),
            ).also {
                recordToolCall(tool.schema.name, "permission_denied")
            }
        }

        val args = try {
            parseArguments(argumentsJson)
        } catch (e: Exception) {
            return ToolResult.error("Failed to parse arguments for tool '$toolCallName': ${e.message}").also {
                recordToolCall(tool.schema.name, "error")
            }
        }

        val validationError = validateArguments(tool, args)
        if (validationError != null) {
            return ToolResult.error(validationError).also {
                recordToolCall(tool.schema.name, "error")
            }
        }

        val decision = policy.check(context, tool, args)
        if (!decision.allowed) {
            return ToolResult.permissionDenied(decision).also {
                recordToolCall(tool.schema.name, "permission_denied")
            }
        }

        return try {
            withTimeout(timeoutMillis.coerceAtLeast(1)) {
                tool.execute(context, args)
            }.also {
                recordToolCall(tool.schema.name, if (it.success) "success" else "error")
            }
        } catch (e: TimeoutCancellationException) {
            ToolResult.timeout(toolCallName, timeoutMillis).also {
                recordToolCall(tool.schema.name, "timeout")
            }
        } catch (e: Exception) {
            ToolResult.error("Tool '$toolCallName' execution failed: ${e.message}").also {
                recordToolCall(tool.schema.name, "error")
            }
        }
    }

    private fun validateArguments(tool: FunctionTool, args: Map<String, String>): String? {
        val missing = tool.schema.parameters.required.filter { it !in args || args[it].isNullOrBlank() }
        if (missing.isNotEmpty()) {
            return "Missing required parameter(s) for tool '${tool.schema.name}': ${missing.joinToString()}"
        }
        return null
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
        timeoutMillis: Long = defaultTimeoutMillis,
    ): Map<String, ToolResult> {
        val results = mutableMapOf<String, ToolResult>()
        for ((id, name, arguments) in toolCalls) {
            results[id] = execute(context, name, arguments, timeoutMillis)
        }
        return results
    }

    private fun recordToolCall(toolName: String, status: String) {
        metricsRegistry.incrementCounter(
            "priestess_tool_calls_total",
            mapOf("tool" to toolName, "status" to status),
        )
    }

    private fun workspaceToolNames(context: AgentToolContext): Set<String>? {
        val raw = context.metadata["workspace_tool_names"]
            ?: context.metadata["workspaceToolNames"]
            ?: return null
        return raw.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
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

    companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 30_000L
    }
}
