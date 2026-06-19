package com.heyanle.priestess.bot.conversation

import com.heyanle.priestess.bot.core.db.MessagesTable
import com.heyanle.priestess.bot.core.db.DatabaseController
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.UUID

enum class MessageRole(val label: String) {
    USER("user"),
    ASSISTANT("assistant"),
    SYSTEM("system"),
    TOOL("tool"),
}

data class StoredMessage(
    val id: String,
    val conversationId: String,
    val role: MessageRole,
    val content: String?,
    val toolCalls: String?,
    val toolCallId: String?,
    val createdAt: Long,
)

class MessageHistory(
    private val db: DatabaseController,
) {

    fun store(
        conversationId: String,
        role: MessageRole,
        content: String? = null,
        toolCalls: String? = null,
        toolCallId: String? = null,
    ): StoredMessage {
        return db.execute {
            val id = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            MessagesTable.insert {
                it[MessagesTable.id] = id
                it[MessagesTable.conversationId] = conversationId
                it[MessagesTable.role] = role.label
                it[MessagesTable.content] = content
                it[MessagesTable.toolCalls] = toolCalls
                it[MessagesTable.toolCallId] = toolCallId
                it[createdAt] = now
            }
            StoredMessage(id, conversationId, role, content, toolCalls, toolCallId, now)
        }
    }

    fun getByConversation(conversationId: String, limit: Int? = null): List<StoredMessage> {
        return db.execute {
            MessagesTable.selectAll()
                .where { MessagesTable.conversationId eq conversationId }
                .orderBy(MessagesTable.createdAt, SortOrder.ASC)
                .let { query ->
                    if (limit != null) query.limit(limit) else query
                }
                .map { it.toStoredMessage() }
        }
    }

    fun getRecentMessages(conversationId: String, count: Int): List<StoredMessage> {
        return db.execute {
            MessagesTable.selectAll()
                .where { MessagesTable.conversationId eq conversationId }
                .orderBy(MessagesTable.createdAt, SortOrder.DESC)
                .limit(count)
                .map { it.toStoredMessage() }
                .reversed()
        }
    }

    fun deleteByConversation(conversationId: String) {
        db.execute {
            MessagesTable.deleteWhere { MessagesTable.conversationId eq conversationId }
        }
    }

    private fun ResultRow.toStoredMessage(): StoredMessage {
        val roleStr = this[MessagesTable.role]
        val role = MessageRole.entries.find { it.label == roleStr }
            ?: throw IllegalStateException("Unknown message role: $roleStr")
        return StoredMessage(
            id = this[MessagesTable.id],
            conversationId = this[MessagesTable.conversationId],
            role = role,
            content = this[MessagesTable.content],
            toolCalls = this[MessagesTable.toolCalls],
            toolCallId = this[MessagesTable.toolCallId],
            createdAt = this[MessagesTable.createdAt],
        )
    }
}
