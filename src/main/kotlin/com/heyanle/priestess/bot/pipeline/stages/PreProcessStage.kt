package com.heyanle.priestess.bot.pipeline.stages

import com.heyanle.priestess.bot.agent.AgentCase
import com.heyanle.priestess.bot.agent.AgentContext
import com.heyanle.priestess.bot.agent.orchestration.SubAgentOrchestrator
import com.heyanle.priestess.bot.config.AgentConfig
import com.heyanle.priestess.bot.config.PipelineConfig
import com.heyanle.priestess.bot.config.SubAgentOrchestrationConfig
import com.heyanle.priestess.bot.conversation.ConversationCase
import com.heyanle.priestess.bot.conversation.MessageRole
import com.heyanle.priestess.bot.pipeline.PipelineContext
import com.heyanle.priestess.bot.pipeline.Stage
import com.heyanle.priestess.bot.pipeline.StageOrder
import com.heyanle.priestess.bot.provider.model.ConversationMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Prepares the agent context before downstream stages and persists history after them.
 *
 * This stage participates in the onion model: setup happens immediately in
 * [process], while persistence is deferred to the returned [Flow] and runs after
 * Process, decoration, and response stages complete.
 */
class PreProcessStage(
    private val agentConfig: AgentConfig,
    private val subAgentConfig: SubAgentOrchestrationConfig = SubAgentOrchestrationConfig(),
    private val pipelineConfig: PipelineConfig,
    private val conversationCase: ConversationCase,
    private val agentCase: AgentCase,
    private val contextManager: com.heyanle.priestess.bot.agent.context.ContextManager,
    private val subAgentOrchestrator: SubAgentOrchestrator? = null,
) : Stage {
    private val logger = KotlinLogging.logger {}

    override val name = "PreProcess"
    override val order = StageOrder.PRE_PROCESS

    override suspend fun process(ctx: PipelineContext): Flow<Unit> {
        val session = ctx.event.session
        val platform = ctx.event.platform

        val conversation = conversationCase.getOrCreate(
            platform = platform.metadata.name,
            sessionId = session.id,
        )
        logger.info {
            "[PIPELINE-110] PreProcess conversation ready id=${conversation.id}, " +
                "platform=${platform.metadata.name}, session=${session.id}"
        }

        val selection = subAgentOrchestrator?.select(
            message = ctx.textContent,
            primaryAgent = agentConfig,
            config = subAgentConfig,
        )
        val selectedAgentConfig = selection?.agentConfig ?: agentConfig
        val agent = agentCase.createAgent(selectedAgentConfig)
        val messages = mutableListOf<ConversationMessage>()
        var historyCount = 0

        if (pipelineConfig.maxHistoryMessages > 0) {
            val storedMessages = conversationCase.getRecentMessages(
                conversationId = conversation.id,
                count = pipelineConfig.maxHistoryMessages,
            )
            historyCount = storedMessages.size
            for (msg in storedMessages) {
                when (msg.role) {
                    MessageRole.USER -> messages.add(ConversationMessage.user(msg.content ?: ""))
                    MessageRole.ASSISTANT -> messages.add(ConversationMessage.assistant(msg.content ?: ""))
                    MessageRole.SYSTEM -> Unit
                    MessageRole.TOOL -> {
                        if (msg.toolCallId != null) {
                            messages.add(
                                ConversationMessage.tool(
                                    toolCallId = msg.toolCallId,
                                    name = msg.content ?: "unknown",
                                    content = msg.content ?: "",
                                ),
                            )
                        }
                    }
                }
            }
        }

        ctx.agentContext = AgentContext(
            agent = agent,
            conversationId = conversation.id,
            platform = platform,
            session = session,
            messages = messages,
        )
        ctx.shared["conversation"] = conversation
        ctx.shared["agent"] = agent
        ctx.shared["subAgentSelectionAgent"] = selection?.agentName ?: agent.name
        ctx.shared["subAgentSelectionReason"] = selection?.reason ?: "primary_agent"
        selection?.routeName?.let { ctx.shared["subAgentSelectionRoute"] = it }

        logger.info {
            "[PIPELINE-119] PreProcess injected agent=${agent.name}, model=${agent.model}, " +
                "selection=${selection?.reason ?: "primary_agent"}, route=${selection?.routeName}, history=$historyCount"
        }

        return flow {
            emit(Unit)

            try {
                conversationCase.storeMessage(
                    conversationId = conversation.id,
                    role = MessageRole.USER,
                    content = ctx.textContent,
                )

                val response = ctx.agentResponse
                if (response is com.heyanle.priestess.bot.agent.AgentResponse.Final) {
                    conversationCase.storeMessage(
                        conversationId = conversation.id,
                        role = MessageRole.ASSISTANT,
                        content = response.content,
                    )
                }

                conversationCase.updateActivity(conversation.id)
                logger.info {
                    "[PIPELINE-190] PreProcess persisted conversation id=${conversation.id}, " +
                        "response=${ctx.agentResponse?.let { it::class.simpleName }}"
                }
            } catch (e: Exception) {
                logger.error(e) { "[PIPELINE-990] Failed to persist conversation" }
            }
        }
    }
}
