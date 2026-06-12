package com.heyanle.priestess.bot.agent.context

import com.heyanle.priestess.bot.provider.model.ConversationMessage

class RoundTruncationStrategy : ContextCompressStrategy {
    override val name: String = "round_truncation"

    /**
     * 按对话轮次截断消息历史。
     *
     * 以 user 消息为轮次边界，从最新消息向前保留最多 [maxRounds] 个轮次。
     * 截断边界外的完整 round（assistant + toolCalls + tool results）会被整体丢弃，
     * 确保消息历史的语义完整性。
     *
     * 边界情况：
     * - 如果 assistant 消息在截断边界外，其 toolCallIds 不会被加入 includedToolCallIds，
     *   因此对应的 tool 结果消息也不会被 collectRelatedTools 收集。这是正确的行为——
     *   整个旧 round 被丢弃。
     * - system 消息始终被跳过（保留到末尾统一添加）。
     */
    override suspend fun compress(
        messages: List<ConversationMessage>,
        systemMessage: ConversationMessage?,
        maxTokens: Int,
        maxRounds: Int,
    ): List<ConversationMessage> {
        if (messages.isEmpty()) return messages
        if (maxRounds <= 0) {
            return if (systemMessage != null) listOf(systemMessage) else emptyList()
        }

        val result = mutableListOf<ConversationMessage>()
        var roundCount = 0
        var i = messages.size - 1
        val includedToolCallIds = mutableSetOf<String>()

        while (i >= 0 && roundCount < maxRounds) {
            val msg = messages[i]
            when (msg.role) {
                "user" -> {
                    collectRelatedTools(messages, i, includedToolCallIds, result)
                    result.add(0, msg)
                    roundCount++
                    i--
                }
                "assistant" -> {
                    if (msg.toolCalls != null) {
                        msg.toolCalls.forEach { tc -> includedToolCallIds.add(tc.id) }
                    }
                    result.add(0, msg)
                    i--
                }
                "tool" -> {
                    if (msg.toolCallId != null && includedToolCallIds.contains(msg.toolCallId)) {
                        result.add(0, msg)
                    }
                    i--
                }
                "system" -> i--
                else -> i--
            }
        }

        if (systemMessage != null && result.none { it.role == "system" }) {
            result.add(0, systemMessage)
        }

        return result
    }

    private fun collectRelatedTools(
        messages: List<ConversationMessage>,
        currentIndex: Int,
        includedToolCallIds: MutableSet<String>,
        result: MutableList<ConversationMessage>,
    ) {
        for (j in (currentIndex - 1) downTo 0) {
            val msg = messages[j]
            if (msg.role == "tool" && msg.toolCallId != null && includedToolCallIds.contains(msg.toolCallId)) {
                if (result.none { it.role == "tool" && it.toolCallId == msg.toolCallId }) {
                    result.add(0, msg)
                }
            }
        }
    }
}
