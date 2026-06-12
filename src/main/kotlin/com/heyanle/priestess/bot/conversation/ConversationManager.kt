package com.heyanle.priestess.bot.conversation

import com.heyanle.priestess.bot.core.db.ConversationsTable
import com.heyanle.priestess.bot.core.db.MessagesTable
import com.heyanle.priestess.bot.core.db.PriestessDb
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class ConversationManager(
    private val db: PriestessDb,
    private val expirationDuration: Duration = 24.hours,
) {

    fun create(platform: String, sessionId: String): Conversation {
        return db.execute {
            val id = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            ConversationsTable.insert {
                it[ConversationsTable.id] = id
                it[ConversationsTable.platform] = platform
                it[ConversationsTable.sessionId] = sessionId
                it[createdAt] = now
                it[updatedAt] = now
            }
            Conversation(
                id = id,
                platform = platform,
                sessionId = sessionId,
                createdAt = now,
                updatedAt = now,
            )
        }
    }

    fun findByPlatformSession(platform: String, sessionId: String): Conversation? {
        return db.execute {
            ConversationsTable.selectAll()
                .where { (ConversationsTable.platform eq platform) and (ConversationsTable.sessionId eq sessionId) }
                .singleOrNull()
                ?.toConversation()
        }
    }

    fun getOrCreate(platform: String, sessionId: String): Conversation {
        return db.execute {
            val existing = ConversationsTable.selectAll()
                .where { (ConversationsTable.platform eq platform) and (ConversationsTable.sessionId eq sessionId) }
                .singleOrNull()
            if (existing != null) {
                return@execute existing.toConversation()
            }
            val id = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            ConversationsTable.insert {
                it[ConversationsTable.id] = id
                it[ConversationsTable.platform] = platform
                it[ConversationsTable.sessionId] = sessionId
                it[createdAt] = now
                it[updatedAt] = now
            }
            Conversation(
                id = id,
                platform = platform,
                sessionId = sessionId,
                createdAt = now,
                updatedAt = now,
            )
        }
    }

    fun updateActivity(id: String) {
        db.execute {
            ConversationsTable.update({ ConversationsTable.id eq id }) {
                it[updatedAt] = System.currentTimeMillis()
            }
        }
    }

    fun delete(id: String) {
        db.execute {
            MessagesTable.deleteWhere { MessagesTable.conversationId eq id }
            ConversationsTable.deleteWhere { ConversationsTable.id eq id }
        }
    }

    fun getAll(): List<Conversation> {
        return db.execute {
            ConversationsTable.selectAll().map { it.toConversation() }
        }
    }

    fun cleanupExpired() {
        db.execute {
            val cutoff = System.currentTimeMillis() - expirationDuration.inWholeMilliseconds
            val expired = ConversationsTable.selectAll()
                .where { ConversationsTable.updatedAt less cutoff }
                .map { it[ConversationsTable.id] }
            for (convId in expired) {
                MessagesTable.deleteWhere { MessagesTable.conversationId eq convId }
                ConversationsTable.deleteWhere { ConversationsTable.id eq convId }
            }
        }
    }

    private fun ResultRow.toConversation(): Conversation {
        return Conversation(
            id = this[ConversationsTable.id],
            platform = this[ConversationsTable.platform],
            sessionId = this[ConversationsTable.sessionId],
            createdAt = this[ConversationsTable.createdAt],
            updatedAt = this[ConversationsTable.updatedAt],
        )
    }
}
