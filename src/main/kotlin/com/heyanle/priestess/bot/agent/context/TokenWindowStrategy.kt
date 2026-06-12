package com.heyanle.priestess.bot.agent.context

import com.heyanle.priestess.bot.provider.model.ConversationMessage

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
                // 如果超出 token 限制的消息是 tool 消息，且其 toolCallId 在待处理集合中，
                // 强制包含它以保证消息完整性
                if (msg.role == "tool" && msg.toolCallId != null && msg.toolCallId in pendingToolCallIds) {
                    result.add(0, msg)
                    currentTokens += msgTokens
                } else {
                    // 超出限制前，检查 result 中是否有未配对的 assistant 消息
                    // （有 toolCalls 但 tool 结果未全部包含），如有则回退移除
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

        // 补充缺失的 tool 结果消息（对应已包含的 assistant toolCalls）
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

    /**
     * 回退 result 中不完整的 round：
     * 从尾部开始，如果发现 assistant 消息有 toolCalls 但缺少对应的 tool 结果消息，则移除该 assistant。
     */
    private fun rollbackIncompleteRounds(
        result: MutableList<ConversationMessage>,
        pendingToolCallIds: MutableSet<String>,
    ) {
        val resultToolCallIds = result
            .filter { it.role == "tool" && it.toolCallId != null }
            .map { it.toolCallId!! }
            .toSet()
        // 从尾部检查是否有未配对的 assistant 消息
        val indicesToRemove = mutableListOf<Int>()
        for (i in result.indices.reversed()) {
            val msg = result[i]
            if (msg.role == "assistant" && msg.toolCalls != null) {
                val hasMissing = msg.toolCalls.any { tc -> tc.id !in resultToolCallIds }
                if (hasMissing) {
                    indicesToRemove.add(i)
                    // 从 pendingToolCallIds 中移除，避免后续补充
                    msg.toolCalls.forEach { tc -> pendingToolCallIds.remove(tc.id) }
                }
            }
        }
        for (idx in indicesToRemove) {
            result.removeAt(idx)
        }
    }
}
