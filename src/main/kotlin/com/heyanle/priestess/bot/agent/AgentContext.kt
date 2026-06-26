package com.heyanle.priestess.bot.agent

import com.heyanle.priestess.bot.platform.MessageSession
import com.heyanle.priestess.bot.platform.Platform
import com.heyanle.priestess.bot.provider.model.ConversationMessage
import com.heyanle.priestess.bot.skill.PipelineSkillState
import com.heyanle.priestess.bot.tool.FunctionTool

/**
 * Agent 执行上下文，承载当前会话消息、平台会话和工作区限定工具。
 */
data class AgentContext(
    val agent: Agent,
    val conversationId: String,
    val platform: Platform?,
    val session: MessageSession?,
    val messages: MutableList<ConversationMessage>,
    val metadata: Map<String, String> = emptyMap(),
    val scopedTools: List<FunctionTool> = emptyList(),
    val skillState: PipelineSkillState = PipelineSkillState(),
)
