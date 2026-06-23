package com.heyanle.priestess.bot.server

import com.heyanle.priestess.bot.agent.AgentCase
import com.heyanle.priestess.bot.agent.AgentContext
import com.heyanle.priestess.bot.agent.AgentHooks
import com.heyanle.priestess.bot.agent.AgentResponse
import com.heyanle.priestess.bot.agent.context.ContextManager
import com.heyanle.priestess.bot.agent.orchestration.SubAgentOrchestrator
import com.heyanle.priestess.bot.agent.runner.ReActRunner
import com.heyanle.priestess.bot.config.ConfigCase
import com.heyanle.priestess.bot.config.ConfigBackup
import com.heyanle.priestess.bot.config.ConfigController
import com.heyanle.priestess.bot.config.PriestessConfig
import com.heyanle.priestess.bot.conversation.ConversationCase
import com.heyanle.priestess.bot.knowledge.KnowledgeCase
import com.heyanle.priestess.bot.observability.MetricsRegistry
import com.heyanle.priestess.bot.plugin.PluginCase
import com.heyanle.priestess.bot.platform.PlatformController
import com.heyanle.priestess.bot.provider.model.ConversationMessage
import com.heyanle.priestess.bot.provider.ProviderCase
import com.heyanle.priestess.bot.tool.ToolController
import com.heyanle.priestess.bot.tool.ToolExecutor
import java.util.UUID

class DashboardService(
    private val configController: ConfigController,
    private val configCase: ConfigCase,
    private val platformController: PlatformController,
    private val providerCase: ProviderCase,
    private val toolController: ToolController,
    private val conversationCase: ConversationCase,
    private val pluginCase: PluginCase,
    private val agentCase: AgentCase,
    private val contextManager: ContextManager,
    private val toolExecutor: ToolExecutor,
    private val knowledgeCase: KnowledgeCase,
    private val subAgentOrchestrator: SubAgentOrchestrator,
    private val metricsRegistry: MetricsRegistry,
) {
    private val startedAtMillis = System.currentTimeMillis()

    fun health(): HealthResponse {
        val config = configCase.current()
        val runningPlatforms = platformController.getRunning().size
        val availableProviders = providerCase.getMetaList().size
        val registeredTools = toolController.getAll().size
        val configuredPlugins = pluginCase.list().size
        val loadedPluginExtensions = pluginCase.extensions().size
        return HealthResponse(
            status = "UP",
            components = mapOf(
                "config" to "UP",
                "database" to "UP",
                "server" to "UP",
                "platforms" to runningPlatforms.toString(),
            ),
            uptimeMillis = (System.currentTimeMillis() - startedAtMillis).coerceAtLeast(0),
            diagnostics = mapOf(
                "configPath" to configController.configPath(),
                "databasePath" to config.database.path,
                "configuredPlatforms" to config.platforms.size.toString(),
                "runningPlatforms" to runningPlatforms.toString(),
                "configuredProviders" to config.providers.size.toString(),
                "availableProviders" to availableProviders.toString(),
                "registeredTools" to registeredTools.toString(),
                "configuredPlugins" to configuredPlugins.toString(),
                "loadedPluginExtensions" to loadedPluginExtensions.toString(),
            ),
        )
    }

    fun metrics(): String = metricsRegistry.renderPrometheus()

    fun config(): PriestessConfig = configCase.current()

    fun replaceConfig(config: PriestessConfig): PriestessConfig = configController.replace(config)

    fun reloadConfig(): PriestessConfig = configController.reload()

    fun configBackups(): List<ConfigBackup> = configController.listBackups()

    fun restoreConfigBackup(id: String): PriestessConfig = configController.restoreBackup(id)

    fun platforms(): List<PlatformStatusDto> {
        val running = platformController.getRunning().map { it.metadata.name }.toSet()
        return configCase.current().platforms.map { cfg ->
            PlatformStatusDto(
                name = cfg.name.ifBlank { cfg.type },
                type = cfg.type,
                enabled = cfg.enabled,
                running = (cfg.name.ifBlank { cfg.type }) in running || cfg.type in running,
                host = cfg.host,
                port = cfg.port,
                wsPort = cfg.wsPort,
            )
        }
    }

    fun setPlatformEnabled(name: String, enabled: Boolean): PriestessConfig {
        return configController.update { current ->
            current.copy(
                platforms = current.platforms.map { cfg ->
                    val cfgName = cfg.name.ifBlank { cfg.type }
                    if (cfgName == name || cfg.type == name) cfg.copy(enabled = enabled) else cfg
                },
            )
        }.also { configController.save(it) }
    }

    fun providers(): List<ProviderDto> {
        return providerCase.getMetaList().map { meta ->
            ProviderDto(
                name = meta.name,
                displayName = meta.displayName,
                kind = meta.kind,
                supportToolCalling = meta.supportToolCalling,
                supportVision = meta.supportVision,
                supportStreaming = meta.supportStreaming,
            )
        }
    }

    suspend fun testProviders(): Map<String, Boolean> = providerCase.testAll()

    fun tools(): List<ToolDto> {
        return toolController.getAll().map { tool ->
            ToolDto(
                name = tool.schema.name,
                description = tool.schema.description,
                parameters = tool.schema.parameters,
            )
        }
    }

    fun conversations(): List<ConversationDto> {
        return conversationCase.getAll().map {
            ConversationDto(
                id = it.id,
                platform = it.platform,
                sessionId = it.sessionId,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt,
            )
        }
    }

    fun messages(conversationId: String, count: Int): List<MessageDto> {
        return conversationCase.getMessages(conversationId, count).map {
            MessageDto(
                id = it.id,
                conversationId = it.conversationId,
                role = it.role.name,
                content = it.content,
                toolCalls = it.toolCalls,
                toolCallId = it.toolCallId,
                createdAt = it.createdAt,
            )
        }
    }

    fun plugins(): PluginListResponse {
        return PluginListResponse(pluginCase.list(), pluginCase.extensions())
    }

    fun discoverPlugins(): PluginListResponse {
        pluginCase.discover()
        return plugins()
    }

    fun enablePlugin(id: String): PluginListResponse {
        pluginCase.enable(id)
        return plugins()
    }

    fun disablePlugin(id: String): PluginListResponse {
        pluginCase.disable(id)
        return plugins()
    }

    fun loadPlugin(id: String): PluginListResponse {
        pluginCase.load(id)
        return plugins()
    }

    fun unloadPlugin(id: String): PluginListResponse {
        pluginCase.unload(id)
        return plugins()
    }

    suspend fun chatAgent(request: AgentChatRequest): AgentChatResponse {
        val agentConfig = request.config ?: configCase.current().agent
        val conversationId = request.conversationId?.takeIf { it.isNotBlank() }
            ?: "dashboard-${UUID.randomUUID()}"
        val events = mutableListOf<AgentChatEventDto>()

        if (request.message.isBlank()) {
            return AgentChatResponse(
                status = "ERROR",
                content = "Message must not be blank",
                events = events,
                providerName = agentConfig.providerName,
                model = agentConfig.model,
                conversationId = conversationId,
            )
        }

        val provider = providerCase.getByName(agentConfig.providerName)
        if (provider == null) {
            return AgentChatResponse(
                status = "ERROR",
                content = "Provider '${agentConfig.providerName}' is not available",
                events = events,
                providerName = agentConfig.providerName,
                model = agentConfig.model,
                conversationId = conversationId,
            )
        }

        val agent = agentCase.createAgent(agentConfig)
        val context = AgentContext(
            agent = agent,
            conversationId = conversationId,
            platform = null,
            session = null,
            messages = mutableListOf(ConversationMessage.user(request.message)),
            metadata = mapOf("source" to "dashboard"),
        )
        val hooks = object : AgentHooks {
            override suspend fun onAgentBegin(context: AgentContext) {
                events += AgentChatEventDto(type = "agent_begin", message = "Agent started")
            }

            override suspend fun onToolStart(context: AgentContext, toolName: String, arguments: String) {
                events += AgentChatEventDto(
                    type = "tool_start",
                    message = arguments,
                    toolName = toolName,
                )
            }

            override suspend fun onToolEnd(context: AgentContext, toolName: String, result: AgentResponse.ToolExecuted) {
                events += AgentChatEventDto(
                    type = "tool_end",
                    message = if (result.toolResult.success) result.toolResult.output else result.toolResult.error,
                    toolName = toolName,
                    success = result.toolResult.success,
                )
            }

            override suspend fun onAgentDone(context: AgentContext, response: AgentResponse.Final) {
                events += AgentChatEventDto(type = "agent_done", message = "Agent completed")
            }

            override suspend fun onAgentError(context: AgentContext, error: AgentResponse.Error) {
                events += AgentChatEventDto(type = "agent_error", message = error.message, success = false)
            }
        }

        val runner = ReActRunner(
            context = context,
            provider = provider,
            toolExecutor = toolExecutor,
            toolRegistry = toolController,
            contextManager = contextManager,
            hooks = hooks,
        )

        return when (val response = runner.stepUntilDone()) {
            is AgentResponse.Final -> AgentChatResponse(
                status = "FINAL",
                content = response.content,
                events = events,
                providerName = agentConfig.providerName,
                model = agentConfig.model,
                conversationId = conversationId,
            )
            is AgentResponse.Error -> AgentChatResponse(
                status = "ERROR",
                content = response.message,
                events = events,
                providerName = agentConfig.providerName,
                model = agentConfig.model,
                conversationId = conversationId,
            )
            else -> AgentChatResponse(
                status = "ERROR",
                content = "Agent ended before producing a final response",
                events = events,
                providerName = agentConfig.providerName,
                model = agentConfig.model,
                conversationId = conversationId,
            )
        }
    }

    fun knowledgeBases(): KnowledgeBaseListResponse {
        return KnowledgeBaseListResponse(knowledgeCase.listBases())
    }

    fun createKnowledgeBase(request: CreateKnowledgeBaseRequest): KnowledgeBaseListResponse {
        knowledgeCase.createBase(request.name, request.description)
        return knowledgeBases()
    }

    fun addKnowledgeDocument(baseId: String, request: AddKnowledgeDocumentRequest): List<com.heyanle.priestess.bot.knowledge.KnowledgeChunk> {
        return knowledgeCase.addTextDocument(baseId, request.documentName, request.content)
    }

    fun searchKnowledge(request: KnowledgeSearchRequest): List<KnowledgeSearchResultDto> {
        return knowledgeCase.search(request.query, request.knowledgeBaseId, request.limit)
            .map { KnowledgeSearchResultDto(it.chunk, it.score) }
    }

    fun subAgentConfig(): com.heyanle.priestess.bot.config.SubAgentOrchestrationConfig {
        return configCase.current().subAgents
    }

    fun replaceSubAgentConfig(config: com.heyanle.priestess.bot.config.SubAgentOrchestrationConfig): com.heyanle.priestess.bot.config.SubAgentOrchestrationConfig {
        return configController.update { current ->
            current.copy(subAgents = config)
        }.also { configController.save(it) }.subAgents
    }

    suspend fun testSubAgent(request: SubAgentTestRequest): SubAgentTestResponse {
        val config = request.config ?: configCase.current().subAgents
        val result = subAgentOrchestrator.run(
            message = request.message,
            primaryAgent = configCase.current().agent,
            config = config,
            conversationId = request.conversationId?.takeIf { it.isNotBlank() } ?: "dashboard-${UUID.randomUUID()}",
        )
        return SubAgentTestResponse(
            status = result.status,
            content = result.content,
            selectedAgentName = result.selection.agentName,
            selectedRouteName = result.selection.routeName,
            selectionReason = result.selection.reason,
            events = result.events,
            conversationId = result.conversationId,
        )
    }
}
