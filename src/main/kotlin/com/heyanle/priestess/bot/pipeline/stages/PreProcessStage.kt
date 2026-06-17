package com.heyanle.priestess.bot.pipeline.stages

import com.heyanle.priestess.bot.agent.AgentContext
import com.heyanle.priestess.bot.agent.AgentHooks
import com.heyanle.priestess.bot.agent.CompressStrategy
import com.heyanle.priestess.bot.agent.context.ContextManager
import com.heyanle.priestess.bot.conversation.ConversationManager
import com.heyanle.priestess.bot.conversation.MessageHistory
import com.heyanle.priestess.bot.conversation.MessageRole
import com.heyanle.priestess.bot.core.config.AgentConfig
import com.heyanle.priestess.bot.pipeline.PipelineConfig
import com.heyanle.priestess.bot.pipeline.PipelineContext
import com.heyanle.priestess.bot.pipeline.Stage
import com.heyanle.priestess.bot.pipeline.StageOrder
import com.heyanle.priestess.bot.provider.model.ConversationMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 预处理阶段（洋葱模型外层）。
 *
 * **前置逻辑**：创建 [AgentContext]，注入 System Prompt，加载会话历史，附加 Skill instructions
 * **后置逻辑**：持久化对话到 ConversationManager + MessageHistory
 *
 * 洋葱模型：前置注入 → 让后续阶段（Process / ResultDecorate / Respond）执行 → 后置持久化
 */
class PreProcessStage(
    private val agentConfig: AgentConfig,
    private val pipelineConfig: PipelineConfig,
    private val conversationManager: ConversationManager,
    private val messageHistory: MessageHistory,
    private val contextManager: ContextManager,
) : Stage {

    override val name = "PreProcess"
    override val order = StageOrder.PRE_PROCESS

    override suspend fun process(ctx: PipelineContext): Flow<Unit> = flow {
        // === 前置逻辑：注入 System Prompt、加载历史 ===

        val session = ctx.event.session
        val platform = ctx.event.platform

        // 获取或创建会话
        val conversation = conversationManager.getOrCreate(
            platform = platform.metadata.name,
            sessionId = session.id,
        )

        // 解析压缩策略
        val compressStrategy = when (agentConfig.compressStrategy.lowercase()) {
            "token_window" -> CompressStrategy.TOKEN_WINDOW
            "llm_compress" -> CompressStrategy.LLM_COMPRESS
            else -> CompressStrategy.ROUND_TRUNCATION
        }

        // 创建 Agent 配置
        val agent = com.heyanle.priestess.bot.agent.Agent(
            name = agentConfig.name,
            instructions = agentConfig.instructions,
            model = agentConfig.model,
            maxSteps = agentConfig.maxSteps,
            toolTimeoutMs = agentConfig.toolTimeoutSeconds * 1000,
            compressStrategy = compressStrategy,
            maxContextTokens = agentConfig.maxTokens,
            maxContextRounds = agentConfig.maxRounds,
        )

        // 加载会话历史
        val messages = mutableListOf<ConversationMessage>()
        var historyCount = 0
        if (pipelineConfig.maxHistoryMessages > 0) {
            val storedMessages = messageHistory.getRecentMessages(
                conversationId = conversation.id,
                count = pipelineConfig.maxHistoryMessages,
            )
            historyCount = storedMessages.size
            for (msg in storedMessages) {
                when (msg.role) {
                    MessageRole.USER -> {
                        messages.add(ConversationMessage.user(msg.content ?: ""))
                    }
                    MessageRole.ASSISTANT -> {
                        messages.add(ConversationMessage.assistant(msg.content ?: ""))
                    }
                    MessageRole.SYSTEM -> {
                        // 系统消息不重复加载
                    }
                    MessageRole.TOOL -> {
                        if (msg.toolCallId != null) {
                            messages.add(
                                ConversationMessage.tool(
                                    toolCallId = msg.toolCallId,
                                    name = msg.content ?: "unknown",
                                    content = msg.content ?: "",
                                )
                            )
                        }
                    }
                }
            }
        }

        // 创建 AgentContext（System Prompt 将由 ReActRunner 在 stepUntilDone 中注入）
        val agentContext = AgentContext(
            agent = agent,
            conversationId = conversation.id,
            platform = platform,
            session = session,
            messages = messages,
        )

        ctx.agentContext = agentContext
        ctx.shared["conversation"] = conversation
        ctx.shared["agent"] = agent

        log("PreProcess injected: agent=${agent.name}, history=$historyCount messages")

        // === 让后续阶段执行 ===
        // Flow 在此处挂起，等待后续阶段完成后再执行后置逻辑
        emit(Unit)

        // === 后置逻辑：持久化对话 ===
        try {
            // 持久化用户消息
            messageHistory.store(
                conversationId = conversation.id,
                role = MessageRole.USER,
                content = ctx.textContent,
            )

            // 持久化助手回复
            val response = ctx.agentResponse
            if (response is com.heyanle.priestess.bot.agent.AgentResponse.Final) {
                messageHistory.store(
                    conversationId = conversation.id,
                    role = MessageRole.ASSISTANT,
                    content = response.content,
                )
            }

            // 更新会话活跃时间
            conversationManager.updateActivity(conversation.id)

            log("Persisted conversation: user_msg + assistant_response")
        } catch (e: Exception) {
            System.err.println("[PreProcess] Failed to persist conversation: ${e.message}")
        }
    }

    private fun log(message: String) {
        println("[PreProcess] $message")
    }
}
