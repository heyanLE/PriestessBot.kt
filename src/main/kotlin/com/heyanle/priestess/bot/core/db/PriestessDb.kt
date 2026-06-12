package com.heyanle.priestess.bot.core.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

class PriestessDb(private val dbPath: String) : AppDatabase {

    @Volatile
    private var exposedDb: Database? = null
    private val lock = ReentrantLock()

    override suspend fun start() {
        open()
    }

    override suspend fun stop() {
        close()
    }

    override suspend fun open() {
        withContext(Dispatchers.IO) {
            lock.withLock {
                if (exposedDb != null) return@withContext
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
                    exec("PRAGMA journal_mode=WAL;")
                }
            }
        }
    }

    override suspend fun close() {
        withContext(Dispatchers.IO) {
            lock.withLock {
                val db = exposedDb ?: return@withContext
                try {
                    TransactionManager.closeAndUnregister(db)
                } finally {
                    exposedDb = null
                }
            }
        }
    }

    fun <T> execute(block: () -> T): T {
        lock.withLock {
            val db = exposedDb
                ?: throw IllegalStateException("Database not open. Call open() first.")
            return transaction(db) { block() }
        }
    }
}
