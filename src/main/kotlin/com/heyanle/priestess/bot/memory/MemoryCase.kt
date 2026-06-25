package com.heyanle.priestess.bot.memory

class MemoryCase(
    private val controller: MemoryController,
) {
    fun save(
        content: String,
        type: MemoryType,
        scope: MemoryScope,
        scopeContext: MemoryScopeContext,
        tags: List<String> = emptyList(),
        confidence: Double = 1.0,
        expiresAt: Long? = null,
    ): MemoryRecord {
        return controller.save(
            content = content,
            type = type,
            scope = scope,
            scopeContext = scopeContext,
            tags = tags,
            confidence = confidence,
            expiresAt = expiresAt,
        )
    }

    fun list(filter: MemoryFilter): List<MemoryRecord> = controller.list(filter)

    fun search(searchQuery: MemorySearchQuery): List<MemorySearchResult> = controller.search(searchQuery)

    fun delete(id: String, scopeContext: MemoryScopeContext): Boolean = controller.delete(id, scopeContext)

    fun expire(nowMillis: Long = System.currentTimeMillis()): Int = controller.expire(nowMillis)
}
