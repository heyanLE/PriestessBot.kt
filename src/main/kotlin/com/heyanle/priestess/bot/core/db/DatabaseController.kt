package com.heyanle.priestess.bot.core.db

import com.heyanle.priestess.bot.core.controller.BaseController
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
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
            val db = Database.connect(
                url = "jdbc:sqlite:$dbPath",
                driver = "org.sqlite.JDBC",
            )
            exposedDb = db
            transaction(db) {
                SchemaUtils.createMissingTablesAndColumns(
                    ConversationsTable,
                    MessagesTable,
                )
                try {
                    exec("PRAGMA journal_mode=WAL;")
                } catch (e: Exception) {
                    logger.warn(e) { "Failed to enable SQLite WAL mode; continuing with default journal mode" }
                }
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
}
