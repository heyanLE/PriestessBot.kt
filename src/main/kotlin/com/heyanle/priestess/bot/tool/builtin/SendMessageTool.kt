package com.heyanle.priestess.bot.tool.builtin

import com.heyanle.priestess.bot.platform.MessageChain
import com.heyanle.priestess.bot.platform.MessageSession
import com.heyanle.priestess.bot.platform.SessionType
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
 * Proactive message sending tool.
 * Requires the Platform to support proactive messaging.
 */
@Tool(name = "send_message", description = "Send a proactive message to a user or group")
class SendMessageTool : FunctionTool() {

    override val schema = ToolSchema(
        name = "send_message",
        description = "Send a proactive message to a user or group. The platform must support proactive messaging.",
        parameters = ToolParameters(
            properties = listOf(
                ParameterDef(name = "content", type = "string", description = "The message content", required = true),
                ParameterDef(name = "target_id", type = "string", description = "Target user/group ID. Empty = current session.", required = false),
                ParameterDef(name = "target_type", type = "string", description = "Target type: private/group/channel", required = false, enumValues = listOf("private", "group", "channel")),
            ),
            required = listOf("content"),
        ),
        riskLevel = ToolRiskLevel.SESSION_ACTION,
        requiredCapabilities = listOf(ToolCapabilities.PLATFORM),
        defaultEnabled = false,
        auditLog = true,
    )

    override suspend fun execute(context: AgentToolContext, args: Map<String, String>): ToolResult {
        val content = args["content"] ?: return ToolResult.error("Missing required parameter: content")
        val platform = context.platform ?: return ToolResult.error("No platform available")

        if (!platform.metadata.supportProactiveMessage) {
            return ToolResult.error("Platform '${platform.metadata.name}' does not support proactive messaging")
        }

        val session = if (args["target_id"] != null) {
            MessageSession(
                id = args["target_id"]!!,
                type = when (args["target_type"]?.lowercase()) {
                    "private" -> SessionType.PRIVATE
                    "group" -> SessionType.GROUP
                    "channel" -> SessionType.CHANNEL
                    else -> context.session?.type ?: SessionType.PRIVATE
                },
                platformName = platform.metadata.name,
            )
        } else {
            context.session ?: return ToolResult.error("No session and no target_id")
        }

        return try {
            platform.sendMessage(session, MessageChain.text(content))
            ToolResult.success("Message sent to ${session.id}")
        } catch (e: Exception) {
            ToolResult.error("Failed to send message: ${e.message}")
        }
    }
}
