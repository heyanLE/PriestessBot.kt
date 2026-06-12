package com.heyanle.priestess.bot.tool

import kotlinx.serialization.Serializable

/**
 * Result returned from a tool execution.
 */
@Serializable
data class ToolResult(
    val success: Boolean,
    val output: String = "",
    val error: String = "",
) {
    companion object {
        fun success(output: String) = ToolResult(success = true, output = output)
        fun error(message: String) = ToolResult(success = false, error = message)
    }
}
