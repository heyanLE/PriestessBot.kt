package com.heyanle.priestess.bot.memory

import kotlinx.serialization.Serializable

@Serializable
enum class MemoryScope {
    GLOBAL,
    PLATFORM,
    SESSION,
    USER,
    AGENT,
}

@Serializable
enum class MemoryType {
    FACT,
    PREFERENCE,
    EVENT,
    SUMMARY,
}

@Serializable
data class MemoryRecord(
    val id: String,
    val workspaceId: String,
    val scope: MemoryScope,
    val platformId: String? = null,
    val sessionId: String? = null,
    val userId: String? = null,
    val agentName: String? = null,
    val type: MemoryType,
    val content: String,
    val tags: List<String> = emptyList(),
    val confidence: Double = 1.0,
    val createdAt: Long,
    val updatedAt: Long,
    val expiresAt: Long? = null,
    val deletedAt: Long? = null,
)

data class MemoryScopeContext(
    val workspaceId: String = DEFAULT_WORKSPACE_ID,
    val platformId: String? = null,
    val sessionId: String? = null,
    val userId: String? = null,
    val agentName: String? = null,
) {
    companion object {
        const val DEFAULT_WORKSPACE_ID = "default"
    }
}

data class MemoryFilter(
    val scopeContext: MemoryScopeContext,
    val type: MemoryType? = null,
    val tag: String? = null,
    val includeExpired: Boolean = false,
    val includeDeleted: Boolean = false,
    val nowMillis: Long = System.currentTimeMillis(),
    val limit: Int = 50,
)

data class MemorySearchQuery(
    val query: String,
    val scopeContext: MemoryScopeContext,
    val scope: MemoryScope? = null,
    val type: MemoryType? = null,
    val limit: Int = 10,
    val nowMillis: Long = System.currentTimeMillis(),
)

@Serializable
data class MemorySearchResult(
    val record: MemoryRecord,
    val score: Double,
    val matchReason: String,
)
