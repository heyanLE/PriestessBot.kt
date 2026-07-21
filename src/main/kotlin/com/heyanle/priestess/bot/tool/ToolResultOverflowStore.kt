package com.heyanle.priestess.bot.tool

import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.util.UUID
import kotlin.io.path.deleteIfExists

data class StoredToolResult(
    val id: String,
    val conversationId: String,
    val content: String,
    val totalCodePoints: Int,
    val expiresAtMillis: Long,
)

class ToolResultOverflowStore(
    private val root: Path = Files.createTempDirectory("priestess-tool-results-"),
    private val clock: Clock = Clock.systemUTC(),
) : AutoCloseable {
    private data class Entry(val conversationId: String, val path: Path, val bytes: Long, val expiresAtMillis: Long)
    private val entries = linkedMapOf<String, Entry>()

    @Synchronized
    fun store(conversationId: String, content: String, ttlSeconds: Long, maxResultBytes: Long, maxTotalBytes: Long): StoredToolResult? {
        evictExpired()
        val bytes = content.toByteArray(Charsets.UTF_8)
        if (conversationId.isBlank() || bytes.size.toLong() > maxResultBytes) return null
        while (entries.values.sumOf { it.bytes } + bytes.size > maxTotalBytes && entries.isNotEmpty()) delete(entries.entries.first().key)
        if (entries.values.sumOf { it.bytes } + bytes.size > maxTotalBytes) return null
        val id = UUID.randomUUID().toString()
        val expiresAt = clock.millis() + ttlSeconds.coerceAtLeast(1) * 1000
        val path = root.resolve("$id.txt")
        Files.writeString(path, content, Charsets.UTF_8)
        entries[id] = Entry(conversationId, path, bytes.size.toLong(), expiresAt)
        return StoredToolResult(id, conversationId, content, content.codePointCount(0, content.length), expiresAt)
    }

    @Synchronized
    fun read(conversationId: String, id: String): StoredToolResult? {
        evictExpired()
        val entry = entries[id] ?: return null
        if (entry.conversationId != conversationId) return null
        val content = runCatching { Files.readString(entry.path, Charsets.UTF_8) }.getOrNull() ?: run { delete(id); return null }
        return StoredToolResult(id, conversationId, content, content.codePointCount(0, content.length), entry.expiresAtMillis)
    }

    @Synchronized
    private fun evictExpired() { entries.filterValues { it.expiresAtMillis <= clock.millis() }.keys.toList().forEach(::delete) }
    private fun delete(id: String) { entries.remove(id)?.path?.deleteIfExists() }
    @Synchronized override fun close() { entries.keys.toList().forEach(::delete); root.toFile().deleteRecursively() }
}
