package com.heyanle.priestess.bot.agent.context

import com.heyanle.priestess.bot.provider.model.ConversationMessage

/**
 * Token 窗口压缩策略，在预算内尽量保留最新且完整的消息片段。
 */
class TokenWindowStrategy(
    private val tokenCounter: TokenCounter,
) : ContextCompressStrategy {
    override val name: String = "token_window"

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
}
