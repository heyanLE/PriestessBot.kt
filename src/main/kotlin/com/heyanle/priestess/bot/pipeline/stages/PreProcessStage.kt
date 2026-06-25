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
import com.heyanle.priestess.bot.persona.PersonaMemoryInjectionContext
import com.heyanle.priestess.bot.persona.PersonaMemoryInjector
import com.heyanle.priestess.bot.provider.model.ConversationMessage
import com.heyanle.priestess.bot.skill.PipelineSkillState
import com.heyanle.priestess.bot.skill.SkillCase
import com.heyanle.priestess.bot.workspace.WorkspaceController
import com.heyanle.priestess.bot.workspace.WorkspaceResolutionContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
    private val workspaceController: WorkspaceController? = null,
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
        val workspaceResolution = workspaceController?.resolve(
            WorkspaceResolutionContext(
                platformName = platform.metadata.name,
                sessionId = session.id,
                userId = ctx.senderId,
                metadata = session.metadata,
            ),
        )
        if (workspaceResolution != null) {
            ctx.pinWorkspace(workspaceResolution)
            logger.info {
                "[PIPELINE-105] PreProcess pinned workspace=${workspaceResolution.snapshot.id}, " +
                    "version=${workspaceResolution.snapshot.version}, reason=${workspaceResolution.reason}"
            }
        }

        val conversation = conversationCase.getOrCreate(
            platform = platform.metadata.name,
            sessionId = session.id,
        )
        logger.info {
            "[PIPELINE-110] PreProcess conversation ready id=${conversation.id}, " +
                "platform=${platform.metadata.name}, session=${session.id}"
        }

        val primaryAgentConfig = ctx.workspaceSnapshot
            ?.agentConfigs
            ?.firstOrNull()
            ?: agentConfig
        val orchestrationConfig = ctx.workspaceSnapshot
            ?.config
            ?.subAgents
            ?: subAgentConfig
        val selection = subAgentOrchestrator?.select(
            message = ctx.textContent,
            primaryAgent = primaryAgentConfig,
            config = orchestrationConfig,
        )
        val selectedAgentConfig = selection?.agentConfig ?: primaryAgentConfig
        val baseAgent = agentCase.createAgent(selectedAgentConfig)
        val injection = personaMemoryInjector?.let { injector ->
            val workspaceId = ctx.workspaceId ?: "default"
            val snapshot = ctx.workspaceSnapshot
            val maxMemories = snapshot
                ?.memoryPolicy
                ?.let { policy -> if (policy.enabled) policy.maxInjectedMemories else 0 }
                ?: 3
            val allowedPersonaIds = snapshot
                ?.personaIds
                ?.takeIf { it.isNotEmpty() }
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
            metadata = buildAgentMetadata(ctx) + (injection?.metadata ?: emptyMap()),
            scopedTools = ctx.workspaceSnapshot?.mcpResources.orEmpty().map { it.tool },
            skillState = ctx.workspaceSnapshot
                ?.let { snapshot -> skillCase?.getWorkspaceSkillState(snapshot) }
                ?: PipelineSkillState(),
        )
        ctx.shared["conversation"] = conversation
        ctx.shared["agent"] = agent
        ctx.shared["subAgentSelectionAgent"] = selection?.agentName ?: agent.name
        ctx.shared["subAgentSelectionReason"] = selection?.reason ?: "primary_agent"
        selection?.routeName?.let { ctx.shared["subAgentSelectionRoute"] = it }
        ctx.workspaceId?.let { ctx.shared["workspaceId"] = it }
        ctx.workspaceSnapshotVersion?.let { ctx.shared["workspaceSnapshotVersion"] = it }
        ctx.workspaceResolutionReason?.let { ctx.shared["workspaceResolutionReason"] = it }
        injection?.persona?.let { ctx.shared["injectedPersonaId"] = it.id }
        val injectedMemories = injection?.memories.orEmpty()
        if (injectedMemories.isNotEmpty()) {
            ctx.shared["injectedMemoryIds"] = injectedMemories.map { it.record.id }
        }

        logger.info {
            "[PIPELINE-119] PreProcess injected agent=${agent.name}, model=${agent.model}, " +
                "selection=${selection?.reason ?: "primary_agent"}, route=${selection?.routeName}, " +
                "workspace=${ctx.workspaceId ?: "none"}, history=$historyCount, " +
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

    private fun buildAgentMetadata(ctx: PipelineContext): Map<String, String> {
        val snapshot = ctx.workspaceSnapshot ?: return ctx.event.session.metadata
        return ctx.event.session.metadata + mapOf(
            "workspace_id" to snapshot.id,
            "workspaceId" to snapshot.id,
            "workspace_name" to snapshot.name,
            "workspaceName" to snapshot.name,
            "workspace_snapshot_version" to snapshot.version.toString(),
            "workspaceSnapshotVersion" to snapshot.version.toString(),
            "workspace_resolution_reason" to (ctx.workspaceResolutionReason ?: ""),
            "workspaceResolutionReason" to (ctx.workspaceResolutionReason ?: ""),
            "provider_name" to snapshot.providerName,
            "providerName" to snapshot.providerName,
            "workspace_tool_names" to snapshot.toolNames.joinToString(","),
            "workspaceToolNames" to snapshot.toolNames.joinToString(","),
            "workspace_skill_names" to snapshot.skillNames.joinToString(","),
            "workspaceSkillNames" to snapshot.skillNames.joinToString(","),
            "workspace_skill_settings" to snapshot.skillSettings.entries.joinToString(";") { (name, settings) ->
                "$name=" + settings.entries.joinToString(",") { (key, value) -> "$key:$value" }
            },
            "workspaceSkillSettings" to snapshot.skillSettings.entries.joinToString(";") { (name, settings) ->
                "$name=" + settings.entries.joinToString(",") { (key, value) -> "$key:$value" }
            },
            "workspace_mcp_server_ids" to snapshot.mcpServerIds.joinToString(","),
            "workspaceMcpServerIds" to snapshot.mcpServerIds.joinToString(","),
            "workspace_memory_enabled" to snapshot.memoryPolicy.enabled.toString(),
            "workspaceMemoryEnabled" to snapshot.memoryPolicy.enabled.toString(),
            "workspace_memory_allowed_scopes" to snapshot.memoryPolicy.allowedScopes.joinToString(","),
            "workspaceMemoryAllowedScopes" to snapshot.memoryPolicy.allowedScopes.joinToString(","),
            "workspace_memory_knowledge_base_ids" to snapshot.memoryPolicy.knowledgeBaseIds.joinToString(","),
            "workspaceMemoryKnowledgeBaseIds" to snapshot.memoryPolicy.knowledgeBaseIds.joinToString(","),
            "workspace_memory_max_injected" to snapshot.memoryPolicy.maxInjectedMemories.toString(),
            "workspaceMemoryMaxInjected" to snapshot.memoryPolicy.maxInjectedMemories.toString(),
        )
    }
}
