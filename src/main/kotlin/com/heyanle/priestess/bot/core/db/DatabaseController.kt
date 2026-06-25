package com.heyanle.priestess.bot.core.db

import com.heyanle.priestess.bot.core.controller.BaseController
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import java.sql.DriverManager
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object ConversationsTable : Table("conversations") {
    val id = varchar("id", 64)
    val platform = varchar("platform", 32)
    val sessionId = varchar("session_id", 128)
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object MessagesTable : Table("messages") {
    val id = varchar("id", 64)
    val conversationId = varchar("conversation_id", 64) references ConversationsTable.id
    val role = varchar("role", 16)
    val content = text("content").nullable()
    val toolCalls = text("tool_calls").nullable()
    val toolCallId = varchar("tool_call_id", 64).nullable()
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object KnowledgeBasesTable : Table("knowledge_bases") {
    val id = varchar("id", 64)
    val name = varchar("name", 128)
    val description = text("description")
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object KnowledgeChunksTable : Table("knowledge_chunks") {
    val id = varchar("id", 64)
    val knowledgeBaseId = varchar("knowledge_base_id", 64) references KnowledgeBasesTable.id
    val documentName = varchar("document_name", 256)
    val content = text("content")
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object MemoryRecordsTable : Table("memory_records") {
    val id = varchar("id", 64)
    val workspaceId = varchar("workspace_id", 128)
    val scope = varchar("scope", 32)
    val platformId = varchar("platform_id", 128).nullable()
    val sessionId = varchar("session_id", 128).nullable()
    val userId = varchar("user_id", 128).nullable()
    val agentName = varchar("agent_name", 128).nullable()
    val type = varchar("type", 32)
    val content = text("content")
    val tags = text("tags")
    val confidence = double("confidence")
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")
    val expiresAt = long("expires_at").nullable()
    val deletedAt = long("deleted_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

object PersonasTable : Table("personas") {
    val id = varchar("id", 64)
    val workspaceId = varchar("workspace_id", 128)
    val name = varchar("name", 128)
    val description = text("description")
    val tone = text("tone")
    val boundaries = text("boundaries")
    val systemPromptTemplate = text("system_prompt_template")
    val enabled = bool("enabled")
    val agentNames = text("agent_names")
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")
    val deletedAt = long("deleted_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

object ReminderRecordsTable : Table("reminder_records") {
    val id = varchar("id", 64)
    val workspaceId = varchar("workspace_id", 128)
    val text = text("text")
    val dueAt = long("due_at")
    val status = varchar("status", 32)
    val platformId = varchar("platform_id", 128).nullable()
    val sessionId = varchar("session_id", 128).nullable()
    val sessionType = varchar("session_type", 32).nullable()
    val userId = varchar("user_id", 128).nullable()
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")
    val deliveredAt = long("delivered_at").nullable()
    val deletedAt = long("deleted_at").nullable()
    val failureReason = text("failure_reason").nullable()
    val deliveryAttemptCount = integer("delivery_attempt_count")

    override val primaryKey = PrimaryKey(id)
}

/**
 * Owns the Exposed database connection and schema initialization.
 *
 * The connection is opened during construction so downstream controllers can use
 * transactions immediately when they are lazily resolved. [stop] closes and
 * unregisters the database connection before cancelling controller tasks.
 */
class DatabaseController(private val dbPath: String) : BaseController("DatabaseController"), AppDatabase {

    @Volatile
    private var exposedDb: Database? = null
    private var keepAliveConnection: Connection? = null
    private val lock = ReentrantLock()

    init {
        openBlocking()
    }

    override suspend fun open() {
        openBlocking()
    }

    private fun openBlocking() {
        lock.withLock {
            if (exposedDb != null) return
            keepAliveConnection = openKeepAliveConnectionIfNeeded()
            val db = Database.connect(
                url = "jdbc:sqlite:$dbPath",
                driver = "org.sqlite.JDBC",
            )
            exposedDb = db
            transaction(db) {
                SchemaUtils.createMissingTablesAndColumns(
                    ConversationsTable,
                    MessagesTable,
                    KnowledgeBasesTable,
                    KnowledgeChunksTable,
                    MemoryRecordsTable,
                    PersonasTable,
                    ReminderRecordsTable,
                )
            }
        }
    }

    override suspend fun close() {
        lock.withLock {
            val db = exposedDb ?: return
            try {
                TransactionManager.closeAndUnregister(db)
            } finally {
                exposedDb = null
                keepAliveConnection?.close()
                keepAliveConnection = null
            }
        }
    }

    override suspend fun stop() {
        close()
        super.stop()
    }

    fun <T> execute(block: () -> T): T {
        lock.withLock {
            val db = exposedDb
                ?: throw IllegalStateException("Database not open. Call open() first.")
            return transaction(db) { block() }
        }
    }

    private fun openKeepAliveConnectionIfNeeded(): Connection? {
        if (!dbPath.contains("mode=memory", ignoreCase = true)) return null
        if (!dbPath.contains("cache=shared", ignoreCase = true)) return null
        return DriverManager.getConnection("jdbc:sqlite:$dbPath")
    }
}
