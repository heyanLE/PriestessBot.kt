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
import com.heyanle.priestess.bot.memory.MemoryCase
import com.heyanle.priestess.bot.memory.MemoryFilter
import com.heyanle.priestess.bot.memory.MemoryScopeContext
import com.heyanle.priestess.bot.memory.MemorySearchQuery
import com.heyanle.priestess.bot.observability.MetricsRegistry
import com.heyanle.priestess.bot.persona.PersonaCase
import com.heyanle.priestess.bot.persona.PersonaMemoryInjection
import com.heyanle.priestess.bot.persona.PersonaMemoryInjectionContext
import com.heyanle.priestess.bot.persona.PersonaMemoryInjector
import com.heyanle.priestess.bot.persona.PersonaUpsertRequest
import com.heyanle.priestess.bot.plugin.PluginCase
import com.heyanle.priestess.bot.platform.PlatformController
import com.heyanle.priestess.bot.provider.model.ConversationMessage
import com.heyanle.priestess.bot.provider.ProviderCase
import com.heyanle.priestess.bot.tool.ToolController
import com.heyanle.priestess.bot.tool.ToolExecutor
import com.heyanle.priestess.bot.tool.ToolListing
import com.heyanle.priestess.bot.workspace.WorkspaceController
import com.heyanle.priestess.bot.workspace.WorkspaceMemoryPolicyConfig
import com.heyanle.priestess.bot.workspace.WorkspaceSnapshot
import com.heyanle.priestess.bot.workspace.WorkspaceStatus
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
    private val healthProvider: RuntimeHealthProvider,
    private val workspaceController: WorkspaceController? = null,
    private val personaCase: PersonaCase? = null,
    private val memoryCase: MemoryCase? = null,
    private val personaMemoryInjector: PersonaMemoryInjector? = null,
) {
    fun health(): HealthResponse = healthProvider.snapshot()

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
        val listingByName = ToolListing.list(
            registeredTools = toolController.getRegisteredTools(),
            filters = com.heyanle.priestess.bot.tool.ToolListingFilters(includeHighRisk = true),
        ).associateBy { it.name }
        return toolController.getRegisteredTools().map { registered ->
            val tool = registered.tool
            val listing = listingByName.getValue(tool.schema.name)
            ToolDto(
                name = tool.schema.name,
                description = tool.schema.description,
                parameters = tool.schema.parameters,
                source = listing.source,
                owner = listing.owner,
                riskLevel = listing.riskLevel,
                requiredCapabilities = listing.requiredCapabilities,
                defaultEnabled = listing.defaultEnabled,
                effectiveEnabled = listing.effectiveEnabled,
                auditLog = listing.auditLog,
                statusReason = listing.statusReason,
            )
        }
    }

    fun workspaces(): WorkspaceListResponse {
        val controller = requireWorkspaceController()
        return WorkspaceListResponse(controller.list().map { it.toDto() })
    }

    fun workspaceDetail(id: String): WorkspaceDetailDto {
        val controller = requireWorkspaceController()
        val snapshot = controller.get(id) ?: throw NoSuchElementException("Workspace '$id' not found")
        val status = controller.list().firstOrNull { it.id == id }
            ?: WorkspaceStatus(
                id = snapshot.id,
                name = snapshot.name,
                enabled = snapshot.enabled,
                activeSnapshotVersion = snapshot.version,
                loadedAt = snapshot.loadedAt,
                diagnostics = snapshot.diagnostics,
            )
        return snapshot.toDetailDto(status)
    }

    fun reloadWorkspace(id: String): com.heyanle.priestess.bot.workspace.WorkspaceReloadResult {
        return requireWorkspaceController().reload(id)
    }

    fun reloadWorkspaces(): List<com.heyanle.priestess.bot.workspace.WorkspaceReloadResult> {
        return requireWorkspaceController().reloadAll()
    }

    fun workspaceTools(id: String): WorkspaceResourceListResponse {
        val snapshot = requireWorkspaceSnapshot(id)
        return WorkspaceResourceListResponse(snapshot.id, snapshot.toolNames)
    }

    fun workspaceMcp(id: String): WorkspaceResourceListResponse {
        val snapshot = requireWorkspaceSnapshot(id)
        return WorkspaceResourceListResponse(snapshot.id, snapshot.mcpServerIds)
    }

    fun workspaceSkills(id: String): WorkspaceResourceListResponse {
        val snapshot = requireWorkspaceSnapshot(id)
        return WorkspaceResourceListResponse(snapshot.id, snapshot.skillNames)
    }

    fun workspacePersonas(id: String): WorkspaceResourceListResponse {
        val snapshot = requireWorkspaceSnapshot(id)
        return WorkspaceResourceListResponse(snapshot.id, snapshot.personaIds)
    }

    fun workspaceMemory(id: String): WorkspaceMemoryPolicyConfig {
        return requireWorkspaceSnapshot(id).memoryPolicy
    }

    fun personas(workspaceId: String): PersonaListResponse {
        return PersonaListResponse(requirePersonaCase().list(workspaceId.ifBlank { "default" }))
    }

    fun upsertPersona(request: PersonaUpsertDto): com.heyanle.priestess.bot.persona.Persona {
        return requirePersonaCase().upsert(
            PersonaUpsertRequest(
                id = request.id,
                workspaceId = request.workspaceId,
                name = request.name,
                description = request.description,
                tone = request.tone,
                boundaries = request.boundaries,
                systemPromptTemplate = request.systemPromptTemplate,
                enabled = request.enabled,
                agentNames = request.agentNames,
            ),
        )
    }

    fun deletePersona(id: String): DeleteResponse {
        return DeleteResponse(requirePersonaCase().delete(id))
    }

    fun resolvePersona(request: PersonaResolveRequest): PersonaResolveResponse {
        return PersonaResolveResponse(
            requirePersonaCase().resolve(
                workspaceId = request.workspaceId.ifBlank { "default" },
                agentName = request.agentName,
            ),
        )
    }

    fun memories(
        workspaceId: String,
        platformId: String?,
        sessionId: String?,
        userId: String?,
        agentName: String?,
        type: String?,
        tag: String?,
        limit: Int,
    ): MemoryListResponse {
        val memoryType = type?.takeIf { it.isNotBlank() }?.let {
            com.heyanle.priestess.bot.memory.MemoryType.valueOf(it.uppercase())
        }
        return MemoryListResponse(
            requireMemoryCase().list(
                MemoryFilter(
                    scopeContext = MemoryScopeContext(
                        workspaceId = workspaceId.ifBlank { "default" },
                        platformId = platformId,
                        sessionId = sessionId,
                        userId = userId,
                        agentName = agentName,
                    ),
                    type = memoryType,
                    tag = tag,
                    limit = limit,
                ),
            ),
        )
    }

    fun saveMemory(request: MemorySaveRequest): com.heyanle.priestess.bot.memory.MemoryRecord {
        return requireMemoryCase().save(
            content = request.content,
            type = request.type,
            scope = request.scope,
            scopeContext = request.toScopeContext(),
            tags = request.tags,
            confidence = request.confidence,
            expiresAt = request.expiresAt,
        )
    }

    fun searchMemory(request: MemorySearchRequest): MemorySearchResponse {
        return MemorySearchResponse(
            requireMemoryCase().search(
                MemorySearchQuery(
                    query = request.query,
                    scopeContext = request.toScopeContext(),
                    scope = request.scope,
                    type = request.type,
                    limit = request.limit,
                ),
            ),
        )
    }

    fun deleteMemory(
        id: String,
        workspaceId: String,
        platformId: String?,
        sessionId: String?,
        userId: String?,
        agentName: String?,
    ): DeleteResponse {
        return DeleteResponse(
            requireMemoryCase().delete(
                id = id,
                scopeContext = MemoryScopeContext(
                    workspaceId = workspaceId.ifBlank { "default" },
                    platformId = platformId,
                    sessionId = sessionId,
                    userId = userId,
                    agentName = agentName,
                ),
            ),
        )
    }

    fun expireMemory(): ExpireMemoryResponse {
        return ExpireMemoryResponse(requireMemoryCase().expire())
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
        val workspaceId = request.workspaceId.ifBlank { "default" }
        val injection = injectPersonaMemory(agentConfig, request, workspaceId)
        val injectionTrace = injection.toAgentChatTrace(workspaceId)

        if (request.message.isBlank()) {
            return AgentChatResponse(
                status = "ERROR",
                content = "Message must not be blank",
                events = events,
                providerName = agentConfig.providerName,
                model = agentConfig.model,
                conversationId = conversationId,
                injectionTrace = injectionTrace,
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
                injectionTrace = injectionTrace,
            )
        }

        val baseAgent = agentCase.createAgent(agentConfig)
        val agent = if (injection != null && injection.hasContent) {
            baseAgent.copy(instructions = injection.instructions)
        } else {
            baseAgent
        }
        val context = AgentContext(
            agent = agent,
            conversationId = conversationId,
            platform = null,
            session = null,
            messages = mutableListOf(ConversationMessage.user(request.message)),
            metadata = mapOf("source" to "dashboard", "workspace_id" to workspaceId) + (injection?.metadata ?: emptyMap()),
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
                    errorCode = result.toolResult.errorCode,
                    policyDenialCode = result.toolResult.policyDenialCode?.name,
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
                injectionTrace = injectionTrace,
            )
            is AgentResponse.Error -> AgentChatResponse(
                status = "ERROR",
                content = response.message,
                events = events,
                providerName = agentConfig.providerName,
                model = agentConfig.model,
                conversationId = conversationId,
                injectionTrace = injectionTrace,
            )
            else -> AgentChatResponse(
                status = "ERROR",
                content = "Agent ended before producing a final response",
                events = events,
                providerName = agentConfig.providerName,
                model = agentConfig.model,
                conversationId = conversationId,
                injectionTrace = injectionTrace,
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

    private fun requireWorkspaceController(): WorkspaceController {
        return workspaceController ?: throw IllegalStateException("Workspace runtime is not available")
    }

    private fun requireWorkspaceSnapshot(id: String): WorkspaceSnapshot {
        return requireWorkspaceController().get(id)
            ?: throw NoSuchElementException("Workspace '$id' not found")
    }

    private fun requirePersonaCase(): PersonaCase {
        return personaCase ?: throw IllegalStateException("Persona runtime is not available")
    }

    private fun requireMemoryCase(): MemoryCase {
        return memoryCase ?: throw IllegalStateException("Memory runtime is not available")
    }

    private fun injectPersonaMemory(
        agentConfig: com.heyanle.priestess.bot.config.AgentConfig,
        request: AgentChatRequest,
        workspaceId: String,
    ): PersonaMemoryInjection? {
        val injector = personaMemoryInjector ?: return null
        val maxMemories = workspaceController
            ?.get(workspaceId)
            ?.memoryPolicy
            ?.maxInjectedMemories
            ?: 3
        return runCatching {
            injector.inject(
                baseInstructions = agentConfig.instructions,
                context = PersonaMemoryInjectionContext(
                    workspaceId = workspaceId,
                    agentName = agentConfig.name,
                    platformId = request.platformId,
                    sessionId = request.sessionId,
                    userId = request.userId,
                    message = request.message,
                    maxMemories = maxMemories,
                ),
            )
        }.getOrNull()
    }

    private fun PersonaMemoryInjection?.toAgentChatTrace(workspaceId: String): AgentChatInjectionTraceDto {
        if (this == null) {
            return AgentChatInjectionTraceDto(workspaceId = workspaceId)
        }
        return AgentChatInjectionTraceDto(
            workspaceId = workspaceId,
            personaId = persona?.id,
            personaName = persona?.name,
            memoryCount = memories.size,
            memories = memories.map { result ->
                AgentChatInjectedMemoryDto(
                    id = result.record.id,
                    type = result.record.type,
                    score = result.score,
                    matchReason = result.matchReason,
                    contentPreview = result.record.content.trim().take(160),
                )
            },
            metadata = metadata,
        )
    }

    private fun MemorySaveRequest.toScopeContext(): MemoryScopeContext {
        return MemoryScopeContext(
            workspaceId = workspaceId.ifBlank { "default" },
            platformId = platformId,
            sessionId = sessionId,
            userId = userId,
            agentName = agentName,
        )
    }

    private fun MemorySearchRequest.toScopeContext(): MemoryScopeContext {
        return MemoryScopeContext(
            workspaceId = workspaceId.ifBlank { "default" },
            platformId = platformId,
            sessionId = sessionId,
            userId = userId,
            agentName = agentName,
        )
    }

    private fun WorkspaceStatus.toDto(): WorkspaceStatusDto {
        return WorkspaceStatusDto(
            id = id,
            name = name,
            enabled = enabled,
            activeSnapshotVersion = activeSnapshotVersion,
            loadedAt = loadedAt,
            lastReload = lastReload,
            diagnostics = diagnostics,
        )
    }

    private fun WorkspaceSnapshot.toDetailDto(status: WorkspaceStatus): WorkspaceDetailDto {
        return WorkspaceDetailDto(
            status = status.toDto(),
            providerName = providerName,
            agents = agentConfigs.map { it.name },
            tools = toolNames,
            skills = skillNames,
            skillSettings = skillSettings,
            mcpServers = mcpServerIds,
            mcpServerDetails = mcpServers.map {
                WorkspaceMcpServerSummaryDto(
                    id = it.id,
                    transport = it.transport,
                    command = it.command,
                    args = it.args,
                    url = it.url,
                )
            },
            personas = personaIds,
            memory = memoryPolicy,
            reloadPlan = status.lastReload?.plan,
        )
    }
}
