package com.heyanle.priestess.bot.agent.context

import com.heyanle.priestess.bot.provider.model.ConversationMessage

/**
 * Token 窗口压缩策略，在预算内尽量保留最新且完整的消息片段。
 */
class TokenWindowStrategy(
    private val tokenCounter: TokenCounter,
) : ContextCompressStrategy {
    override suspend fun compress(
        messages: List<ConversationMessage>,
        systemMessage: ConversationMessage?,
        maxTokens: Int,
        maxRounds: Int,
    ): List<ConversationMessage> {
        if (messages.isEmpty()) return messages
        if (maxTokens <= 0) {
            return if (systemMessage != null) listOf(systemMessage) else emptyList()
        }

        val systemTokens = systemMessage?.let { tokenCounter.count(it) } ?: 0
        val availableTokens = maxTokens - systemTokens
        if (availableTokens <= 0) {
            return if (systemMessage != null) listOf(systemMessage) else emptyList()
        }

        val result = mutableListOf<ConversationMessage>()
        var currentTokens = 0
        val pendingToolCallIds = mutableSetOf<String>()

        for (i in messages.indices.reversed()) {
            val msg = messages[i]
            if (msg.role == "system") continue
            val msgTokens = tokenCounter.count(msg)
            if (currentTokens + msgTokens > availableTokens) {
                if (msg.role == "tool" && msg.toolCallId != null && msg.toolCallId in pendingToolCallIds) {
                    result.add(0, msg)
                    currentTokens += msgTokens
                } else {
                    rollbackIncompleteRounds(result, pendingToolCallIds)
                }
                break
            }
            if (msg.role == "assistant" && msg.toolCalls != null) {
                msg.toolCalls.forEach { tc -> pendingToolCallIds.add(tc.id) }
            }
            result.add(0, msg)
            currentTokens += msgTokens
        }

        val resultToolCallIds = result
            .filter { it.role == "tool" && it.toolCallId != null }
            .map { it.toolCallId!! }
            .toSet()
        val missingToolCallIds = pendingToolCallIds - resultToolCallIds
        if (missingToolCallIds.isNotEmpty()) {
            for (msg in messages) {
                if (msg.role == "tool" && msg.toolCallId in missingToolCallIds) {
                    val msgTokens = tokenCounter.count(msg)
                    if (currentTokens + msgTokens <= availableTokens) {
                        result.add(msg)
                        currentTokens += msgTokens
                    }
                }
            }
        }

        // Keep tool-call rounds structurally valid for OpenAI-compatible providers:
        // every retained tool message must still have its parent assistant tool_calls message.
        currentTokens = repairOrphanToolMessages(
            originalMessages = messages,
            result = result,
            currentTokens = currentTokens,
            availableTokens = availableTokens,
        )

        if (systemMessage != null && result.none { it.role == "system" }) {
            result.add(0, systemMessage)
        }

        return result
    }

    private fun rollbackIncompleteRounds(
        result: MutableList<ConversationMessage>,
        pendingToolCallIds: MutableSet<String>,
    ) {
        val resultToolCallIds = result
            .filter { it.role == "tool" && it.toolCallId != null }
            .map { it.toolCallId!! }
            .toSet()
        val indicesToRemove = mutableListOf<Int>()
        for (i in result.indices.reversed()) {
            val msg = result[i]
            if (msg.role == "assistant" && msg.toolCalls != null) {
                val hasMissing = msg.toolCalls.any { tc -> tc.id !in resultToolCallIds }
                if (hasMissing) {
                    indicesToRemove.add(i)
                    msg.toolCalls.forEach { tc -> pendingToolCallIds.remove(tc.id) }
                }
            }
        }
        for (idx in indicesToRemove) {
            result.removeAt(idx)
        }
    }

    private fun repairOrphanToolMessages(
        originalMessages: List<ConversationMessage>,
        result: MutableList<ConversationMessage>,
        currentTokens: Int,
        availableTokens: Int,
    ): Int {
        var tokens = currentTokens
        val assistantByToolCallId = result
            .filter { it.role == "assistant" && !it.toolCalls.isNullOrEmpty() }
            .flatMap { assistant ->
                assistant.toolCalls!!.map { it.id to assistant }
            }
            .toMap()

        val orphanToolIds = result
            .filter { it.role == "tool" && it.toolCallId != null && it.toolCallId !in assistantByToolCallId }
            .mapNotNull { it.toolCallId }
            .distinct()

        for (toolCallId in orphanToolIds) {
            val assistant = originalMessages.find { msg ->
                msg.role == "assistant" && msg.toolCalls?.any { it.id == toolCallId } == true
            }
            val assistantTokens = assistant?.let(tokenCounter::count)
            if (assistant != null && assistantTokens != null && tokens + assistantTokens <= availableTokens) {
                val insertAt = result.indexOfFirst { it.role == "tool" && it.toolCallId == toolCallId }
                    .takeIf { it >= 0 }
                    ?: result.size
                result.add(insertAt, assistant)
                tokens += assistantTokens
            } else {
                val iterator = result.listIterator()
                while (iterator.hasNext()) {
                    val msg = iterator.next()
                    if (msg.role == "tool" && msg.toolCallId == toolCallId) {
                        tokens -= tokenCounter.count(msg)
                        iterator.remove()
                    }
                }
            }
        }

        return tokens
    }
}
