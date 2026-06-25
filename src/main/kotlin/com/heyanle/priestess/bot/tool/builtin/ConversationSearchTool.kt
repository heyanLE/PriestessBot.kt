package com.heyanle.priestess.bot.tool.builtin

import com.heyanle.priestess.bot.conversation.ConversationCase
import com.heyanle.priestess.bot.conversation.ConversationMessageSearchQuery
import com.heyanle.priestess.bot.conversation.ConversationSearchResult
import com.heyanle.priestess.bot.conversation.MessageRole
import com.heyanle.priestess.bot.tool.AgentToolContext
import com.heyanle.priestess.bot.tool.FunctionTool
import com.heyanle.priestess.bot.tool.ParameterDef
import com.heyanle.priestess.bot.tool.ToolCapabilities
import com.heyanle.priestess.bot.tool.ToolParameters
import com.heyanle.priestess.bot.tool.ToolResult
import com.heyanle.priestess.bot.tool.ToolRiskLevel
import com.heyanle.priestess.bot.tool.ToolSchema
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ConversationSearchTool(
    private val conversationCaseProvider: () -> ConversationCase,
) : FunctionTool() {
    override val schema: ToolSchema = ToolSchema(
        name = "conversation_search",
        description = "Search stored conversation messages with current-session defaults and bounded results.",
        parameters = ToolParameters(
            properties = listOf(
                ParameterDef("query", description = "Text query matched against message content."),
                ParameterDef("conversation_id", description = "Explicit conversation id. Defaults to current platform/session."),
                ParameterDef(
                    name = "role",
                    description = "Optional message role filter.",
                    enumValues = MessageRole.entries.map { it.label },
                ),
                ParameterDef("since_ms", type = "integer", description = "Only include messages at or after this epoch millis."),
                ParameterDef("until_ms", type = "integer", description = "Only include messages at or before this epoch millis."),
                ParameterDef("limit", type = "integer", description = "Maximum results, default 10, max 50."),
            ),
        ),
        riskLevel = ToolRiskLevel.SAFE_READ,
        requiredCapabilities = listOf(ToolCapabilities.CONVERSATION_HISTORY),
        defaultEnabled = true,
        auditLog = false,
    )

    private val json = Json {
        encodeDefaults = true
        prettyPrint = true
    }

    override suspend fun execute(context: AgentToolContext, args: Map<String, String>): ToolResult {
        val explicitConversationId = args["conversation_id"]?.trim()?.takeIf { it.isNotBlank() }
        val platformName = context.platform?.metadata?.name ?: context.session?.platformName
        val sessionId = context.session?.id
        if (explicitConversationId == null && (platformName.isNullOrBlank() || sessionId.isNullOrBlank())) {
            return ToolResult.error(
                message = "conversation_search requires current platform/session or conversation_id",
                errorCode = "MISSING_SCOPE",
            )
        }

        val query = ConversationMessageSearchQuery(
            conversationId = explicitConversationId,
            platform = if (explicitConversationId == null) platformName else null,
            sessionId = if (explicitConversationId == null) sessionId else null,
            query = args["query"].orEmpty(),
            role = args["role"]?.let(::parseRole),
            sinceMillis = args["since_ms"]?.toLongOrNull(),
            untilMillis = args["until_ms"]?.toLongOrNull(),
            limit = args["limit"]?.toIntOrNull()?.coerceIn(1, 50) ?: 10,
        )
        val results = conversationCaseProvider().searchMessages(query)
        return ToolResult.success(json.encodeToString(ConversationSearchResponse(results.map { it.toDto() })))
    }

    private fun parseRole(value: String): MessageRole? {
        return MessageRole.entries.firstOrNull { it.label.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true) }
    }

    private fun ConversationSearchResult.toDto(): ConversationSearchItem {
        return ConversationSearchItem(
            conversationId = conversation.id,
            platform = conversation.platform,
            sessionId = conversation.sessionId,
            messageId = message.id,
            role = message.role.label,
            content = message.content,
            snippet = snippet,
            createdAt = message.createdAt,
        )
    }
}

@Serializable
data class ConversationSearchResponse(
    val results: List<ConversationSearchItem>,
)

@Serializable
data class ConversationSearchItem(
    val conversationId: String,
    val platform: String,
    val sessionId: String,
    val messageId: String,
    val role: String,
    val content: String?,
    val snippet: String,
    val createdAt: Long,
)
