package com.heyanle.priestess.bot.pipeline.stages

import com.heyanle.priestess.bot.agent.AgentCase
import com.heyanle.priestess.bot.agent.AgentContext
import com.heyanle.priestess.bot.agent.orchestration.SubAgentOrchestrator
import com.heyanle.priestess.bot.config.AgentConfig
import com.heyanle.priestess.bot.config.PipelineConfig
import com.heyanle.priestess.bot.conversation.ConversationCase
import com.heyanle.priestess.bot.conversation.MessageRole
import com.heyanle.priestess.bot.pipeline.PipelineContext
import com.heyanle.priestess.bot.pipeline.Stage
import com.heyanle.priestess.bot.pipeline.StageOrder
import com.heyanle.priestess.bot.persona.PersonaMemoryInjectionContext
import com.heyanle.priestess.bot.persona.PersonaMemoryInjector
import com.heyanle.priestess.bot.provider.model.ConversationMessage
import com.heyanle.priestess.bot.skill.PipelineSkillState
import com.heyanle.priestess.bot.skill.SkillCase
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 预处理阶段，负责准备 AgentContext 并在下游阶段完成后持久化对话历史。
 *
 * 该阶段采用洋葱模型：进入阶段时完成上下文准备，返回的 Flow 在执行、装饰和响应阶段之后保存历史。
 */
class PreProcessStage(
    private val agentConfig: AgentConfig,
    private val pipelineConfig: PipelineConfig,
    private val conversationCase: ConversationCase,
    private val agentCase: AgentCase,
    private val subAgentOrchestrator: SubAgentOrchestrator? = null,
    private val personaMemoryInjector: PersonaMemoryInjector? = null,
    private val skillCase: SkillCase? = null,
) : Stage {
    private val logger = KotlinLogging.logger {}
    private val json = Json { encodeDefaults = true }

    override val name = "PreProcess"
    override val order = StageOrder.PRE_PROCESS

    override suspend fun process(ctx: PipelineContext): Flow<Unit> {
        val session = ctx.event.session
        val platform = ctx.event.platform
        val resolution = ctx.workspaceResolution
        if (resolution == null) {
            logger.warn {
                "[PIPELINE-101] PreProcess missing prepared workspace for " +
                    "platform=${platform.metadata.name}, session=${session.id}; stopping pipeline"
            }
            ctx.stop()
            return flow {}
        }
        val snapshot = resolution.snapshot
        logger.info {
            "[PIPELINE-105] PreProcess using pinned workspace=${snapshot.id}, " +
                "version=${snapshot.version}, reason=${resolution.reason}"
        }

        val conversation = conversationCase.getOrCreate(
            platform = platform.metadata.name,
            sessionId = session.id,
        )
        logger.info {
            "[PIPELINE-110] PreProcess conversation ready id=${conversation.id}, " +
                "platform=${platform.metadata.name}, session=${session.id}"
        }

        val primaryAgentConfig = snapshot.agentConfigs
            .firstOrNull()
            ?: agentConfig
        val orchestrationConfig = snapshot.config.subAgents
        val selection = subAgentOrchestrator?.select(
            message = ctx.textContent,
            primaryAgent = primaryAgentConfig,
            config = orchestrationConfig,
        )
        val selectedAgentConfig = selection?.agentConfig ?: primaryAgentConfig
        val selectedProviderName = when {
            selection != null &&
                selection.agentName != primaryAgentConfig.name &&
                selection.agentConfig.providerName.isNotBlank() &&
                selection.agentConfig.providerName != primaryAgentConfig.providerName -> {
                selection.agentConfig.providerName
            }
            else -> snapshot.providerName.ifBlank { selectedAgentConfig.providerName }
        }
        val baseAgent = agentCase.createAgent(selectedAgentConfig)
        val injection = personaMemoryInjector?.let { injector ->
            val workspaceId = snapshot.id
            val maxMemories = snapshot.memoryPolicy
                .let { policy -> if (policy.enabled) policy.maxInjectedMemories else 0 }
            val allowedPersonaIds = snapshot.personaIds
                .takeIf { it.isNotEmpty() }
                ?.toSet()
            injector.inject(
                baseInstructions = baseAgent.instructions,
                context = PersonaMemoryInjectionContext(
                    workspaceId = workspaceId,
                    agentName = baseAgent.name,
                    platformId = platform.metadata.name,
                    sessionId = session.id,
                    userId = ctx.senderId,
                    message = ctx.textContent,
                    maxMemories = maxMemories,
                    allowedPersonaIds = allowedPersonaIds,
                ),
            )
        }
        val agent = if (injection != null && injection.hasContent) {
            baseAgent.copy(instructions = injection.instructions)
        } else {
            baseAgent
        }
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
                    MessageRole.ASSISTANT -> messages.add(
                        ConversationMessage.assistant(
                            content = msg.content ?: "",
                            toolCalls = msg.toolCalls
                                ?.takeIf { it.isNotBlank() }
                                ?.let {
                                    runCatching {
                                        json.decodeFromString<List<com.heyanle.priestess.bot.provider.model.ToolCall>>(it)
                                    }.getOrNull()
                                },
                        ),
                    )
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
            metadata = buildAgentMetadata(ctx, selectedProviderName) + (injection?.metadata ?: emptyMap()),
            scopedTools = snapshot.mcpResources.map { it.tool },
            skillState = skillCase
                ?.getWorkspaceSkillState(snapshot)
                ?: PipelineSkillState(),
        )

        logger.info {
            "[PIPELINE-119] PreProcess injected agent=${agent.name}, model=${agent.model}, " +
                "selection=${selection?.reason ?: "primary_agent"}, route=${selection?.routeName}, " +
                "workspace=${snapshot.id}, history=$historyCount, " +
                "persona=${injection?.persona?.id ?: "none"}, memories=${injection?.memories?.size ?: 0}"
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
                    val agentMessages = ctx.agentContext?.messages.orEmpty()
                    val currentUserIndex = agentMessages.indexOfLast {
                        it.role == "user" && it.content == ctx.textContent
                    }
                    agentMessages
                        .drop((currentUserIndex + 1).coerceAtLeast(0))
                        .filter { message ->
                            message.role == "tool" ||
                                (message.role == "assistant" && !message.toolCalls.isNullOrEmpty())
                        }
                        .forEach { message ->
                            conversationCase.storeMessage(
                                conversationId = conversation.id,
                                role = if (message.role == "tool") MessageRole.TOOL else MessageRole.ASSISTANT,
                                content = message.content,
                                toolCalls = message.toolCalls?.let { json.encodeToString(it) },
                                toolCallId = message.toolCallId,
                            )
                        }
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

    private fun buildAgentMetadata(
        ctx: PipelineContext,
        selectedProviderName: String,
    ): Map<String, String> {
        val snapshot = ctx.workspaceSnapshot ?: return ctx.event.session.metadata
        val resolutionReason = ctx.workspaceResolution?.reason.orEmpty()
        return ctx.event.session.metadata + mapOf(
            "workspaceId" to snapshot.id,
            "workspaceRootDir" to snapshot.rootDir,
            "workspaceSnapshotVersion" to snapshot.version.toString(),
            "workspaceResolutionReason" to resolutionReason,
            "providerName" to selectedProviderName,
            "workspaceToolNames" to snapshot.toolNames.joinToString(","),
            "workspaceSkillNames" to snapshot.skillNames.joinToString(","),
            "workspaceSkillSettings" to snapshot.skillSettings.entries.joinToString(";") { (name, settings) ->
                "$name=" + settings.entries.joinToString(",") { (key, value) -> "$key:$value" }
            },
            "workspaceMcpServerIds" to snapshot.mcpServerIds.joinToString(","),
            "workspaceMemoryEnabled" to snapshot.memoryPolicy.enabled.toString(),
            "workspaceMemoryAllowedScopes" to snapshot.memoryPolicy.allowedScopes.joinToString(","),
            "workspaceMemoryKnowledgeBaseIds" to snapshot.memoryPolicy.knowledgeBaseIds.joinToString(","),
            "workspaceMemoryMaxInjected" to snapshot.memoryPolicy.maxInjectedMemories.toString(),
        )
    }
}
