package com.heyanle.priestess.bot.agent.context

import com.heyanle.priestess.bot.provider.model.ConversationMessage

interface ContextCompressStrategy {
    val name: String

    suspend fun compress(
        messages: List<ConversationMessage>,
        systemMessage: ConversationMessage?,
        maxTokens: Int = -1,
        maxRounds: Int = -1,
    ): List<ConversationMessage>
}
