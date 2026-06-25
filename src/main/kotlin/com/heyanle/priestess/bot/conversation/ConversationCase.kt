package com.heyanle.priestess.bot.conversation

class ConversationCase(
    private val controller: ConversationController,
    private val history: MessageHistory,
) {
    fun getOrCreate(platform: String, sessionId: String): Conversation {
        return controller.getOrCreate(platform, sessionId)
    }

    fun updateActivity(id: String) {
        controller.updateActivity(id)
    }

    fun getRecentMessages(conversationId: String, count: Int): List<StoredMessage> {
        return history.getRecentMessages(conversationId, count)
    }

    fun getAll(): List<Conversation> {
        return controller.getAll()
    }

    fun getMessages(conversationId: String, count: Int = 100): List<StoredMessage> {
        return history.getRecentMessages(conversationId, count)
    }

    fun searchMessages(query: ConversationMessageSearchQuery): List<ConversationSearchResult> {
        return history.search(query)
    }

    fun storeMessage(
        conversationId: String,
        role: MessageRole,
        content: String? = null,
        toolCalls: String? = null,
        toolCallId: String? = null,
    ): StoredMessage {
        return history.store(conversationId, role, content, toolCalls, toolCallId)
    }
}
