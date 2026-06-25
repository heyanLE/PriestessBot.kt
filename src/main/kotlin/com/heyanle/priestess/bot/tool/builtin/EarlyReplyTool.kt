package com.heyanle.priestess.bot.tool.builtin

import com.heyanle.priestess.bot.platform.MessageChain
import com.heyanle.priestess.bot.tool.AgentToolContext
import com.heyanle.priestess.bot.tool.FunctionTool
import com.heyanle.priestess.bot.tool.ParameterDef
import com.heyanle.priestess.bot.tool.ToolParameters
import com.heyanle.priestess.bot.tool.ToolResult
import com.heyanle.priestess.bot.tool.ToolCapabilities
import com.heyanle.priestess.bot.tool.ToolRiskLevel
import com.heyanle.priestess.bot.tool.ToolSchema
import com.heyanle.priestess.bot.tool.annotation.Tool

/**
 * Early reply tool that sends a proactive message to the user during the Agent loop.
 *
 * Uses the Platform reference in AgentToolContext to send the message.
 */
@Tool(name = "early_reply", description = "Send a proactive message to the user during long processing")
class EarlyReplyTool : FunctionTool() {

    override val schema = ToolSchema(
        name = "early_reply",
        description = "Send a proactive message to the user immediately while continuing to process in the background.",
        parameters = ToolParameters(
            properties = listOf(
                ParameterDef(name = "message", type = "string", description = "The message to send to the user", required = true),
            ),
            required = listOf("message"),
        ),
        riskLevel = ToolRiskLevel.SESSION_ACTION,
        requiredCapabilities = listOf(ToolCapabilities.PLATFORM, ToolCapabilities.SESSION),
        defaultEnabled = true,
        auditLog = true,
    )

    override suspend fun execute(context: AgentToolContext, args: Map<String, String>): ToolResult {
        val message = args["message"] ?: return ToolResult.error("Missing required parameter: message")
        val platform = context.platform ?: return ToolResult.error("No platform in context")
        val session = context.session ?: return ToolResult.error("No session in context")

        return try {
            platform.sendMessage(session, MessageChain.text(message))
            ToolResult.success("Early reply sent: $message")
        } catch (e: Exception) {
            ToolResult.error("Failed to send early reply: ${e.message}")
        }
    }
}
