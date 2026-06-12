package com.heyanle.priestess.bot.agent

import com.heyanle.priestess.bot.platform.MessageSession
import com.heyanle.priestess.bot.platform.Platform
import com.heyanle.priestess.bot.provider.model.ConversationMessage

data class AgentContext(
    val agent: Agent,
    val conversationId: String,
    val platform: Platform?,
    val session: MessageSession?,
    val messages: MutableList<ConversationMessage>,
    val metadata: Map<String, String> = emptyMap(),
)
