package com.heyanle.priestess.bot.server

import com.heyanle.priestess.bot.config.AgentConfig
import com.heyanle.priestess.bot.config.SubAgentOrchestrationConfig
import com.heyanle.priestess.bot.knowledge.KnowledgeBase
import com.heyanle.priestess.bot.knowledge.KnowledgeChunk
import com.heyanle.priestess.bot.memory.MemoryRecord
import com.heyanle.priestess.bot.memory.MemoryScope
import com.heyanle.priestess.bot.memory.MemorySearchResult
import com.heyanle.priestess.bot.memory.MemoryType
import com.heyanle.priestess.bot.persona.Persona
import com.heyanle.priestess.bot.plugin.PluginDescriptor
import com.heyanle.priestess.bot.plugin.PluginExtensionMetadata
import com.heyanle.priestess.bot.provider.LLMKind
import com.heyanle.priestess.bot.tool.ToolParameters
import com.heyanle.priestess.bot.tool.ToolRiskLevel
import com.heyanle.priestess.bot.tool.ToolSource
import com.heyanle.priestess.bot.workspace.WorkspaceMemoryPolicyConfig
import com.heyanle.priestess.bot.workspace.WorkspaceReloadPlan
import com.heyanle.priestess.bot.workspace.WorkspaceReloadResult
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
    val components: Map<String, String>,
    val timestamp: Long = System.currentTimeMillis(),
    val uptimeMillis: Long = 0,
    val diagnostics: Map<String, String> = emptyMap(),
)

@Serializable
data class PlatformStatusDto(
    val name: String,
    val type: String,
    val enabled: Boolean,
    val running: Boolean,
    val host: String,
    val port: Int,
    val wsPort: Int,
)

@Serializable
data class ProviderDto(
    val name: String,
    val displayName: String,
    val kind: LLMKind,
    val supportToolCalling: Boolean,
    val supportVision: Boolean,
    val supportStreaming: Boolean,
)

@Serializable
data class ToolDto(
    val name: String,
    val description: String,
    val parameters: ToolParameters,
    val source: ToolSource = ToolSource.BUILTIN,
    val owner: String? = null,
    val riskLevel: ToolRiskLevel = ToolRiskLevel.SAFE_READ,
    val requiredCapabilities: List<String> = emptyList(),
    val defaultEnabled: Boolean = true,
    val effectiveEnabled: Boolean = true,
    val auditLog: Boolean = false,
    val statusReason: String? = null,
)

@Serializable
data class ConversationDto(
    val id: String,
    val platform: String,
    val sessionId: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class MessageDto(
    val id: String,
    val conversationId: String,
    val role: String,
    val content: String?,
    val toolCalls: String?,
    val toolCallId: String?,
    val createdAt: Long,
)

@Serializable
data class PluginListResponse(
    val plugins: List<PluginDescriptor>,
    val extensions: List<PluginExtensionMetadata>,
)

@Serializable
data class LogEventDto(
    val level: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
)

@Serializable
data class AgentChatRequest(
    val message: String,
    val config: AgentConfig? = null,
    val conversationId: String? = null,
    val workspaceId: String = "default",
    val platformId: String? = null,
    val sessionId: String? = null,
    val userId: String? = null,
)

@Serializable
data class AgentChatEventDto(
    val type: String,
    val message: String,
    val toolName: String? = null,
    val success: Boolean? = null,
    val errorCode: String? = null,
    val policyDenialCode: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
)

@Serializable
data class AgentChatResponse(
    val status: String,
    val content: String,
    val events: List<AgentChatEventDto>,
    val providerName: String,
    val model: String,
    val conversationId: String,
    val injectionTrace: AgentChatInjectionTraceDto = AgentChatInjectionTraceDto(),
)

@Serializable
data class AgentChatInjectionTraceDto(
    val workspaceId: String = "default",
    val personaId: String? = null,
    val personaName: String? = null,
    val memoryCount: Int = 0,
    val memories: List<AgentChatInjectedMemoryDto> = emptyList(),
    val metadata: Map<String, String> = emptyMap(),
)

@Serializable
data class AgentChatInjectedMemoryDto(
    val id: String,
    val type: MemoryType,
    val score: Double,
    val matchReason: String,
    val contentPreview: String,
)

@Serializable
data class CreateKnowledgeBaseRequest(
    val name: String,
    val description: String = "",
)

@Serializable
data class AddKnowledgeDocumentRequest(
    val documentName: String,
    val content: String,
)

@Serializable
data class KnowledgeSearchRequest(
    val query: String,
    val knowledgeBaseId: String? = null,
    val limit: Int = 5,
)

@Serializable
data class KnowledgeSearchResultDto(
    val chunk: KnowledgeChunk,
    val score: Double,
)

@Serializable
data class KnowledgeBaseListResponse(
    val bases: List<KnowledgeBase>,
)

@Serializable
data class SubAgentTestRequest(
    val message: String,
    val config: SubAgentOrchestrationConfig? = null,
    val conversationId: String? = null,
)

@Serializable
data class SubAgentTestResponse(
    val status: String,
    val content: String,
    val selectedAgentName: String,
    val selectedRouteName: String? = null,
    val selectionReason: String,
    val events: List<AgentChatEventDto>,
    val conversationId: String,
)

@Serializable
data class WorkspaceListResponse(
    val workspaces: List<WorkspaceStatusDto>,
)

@Serializable
data class WorkspaceStatusDto(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val activeSnapshotVersion: Long? = null,
    val loadedAt: Long? = null,
    val lastReload: WorkspaceReloadResult? = null,
    val diagnostics: List<String> = emptyList(),
)

@Serializable
data class WorkspaceDetailDto(
    val status: WorkspaceStatusDto,
    val providerName: String,
    val agents: List<String>,
    val tools: List<String>,
    val skills: List<String>,
    val skillSettings: Map<String, Map<String, String>> = emptyMap(),
    val mcpServers: List<String>,
    val mcpServerDetails: List<WorkspaceMcpServerSummaryDto> = emptyList(),
    val personas: List<String>,
    val memory: WorkspaceMemoryPolicyConfig,
    val reloadPlan: WorkspaceReloadPlan? = null,
)

@Serializable
data class WorkspaceMcpServerSummaryDto(
    val id: String,
    val transport: String,
    val command: String = "",
    val args: List<String> = emptyList(),
    val url: String = "",
)

@Serializable
data class WorkspaceResourceListResponse(
    val workspaceId: String,
    val resources: List<String>,
)

@Serializable
data class PersonaListResponse(
    val personas: List<Persona>,
)

@Serializable
data class PersonaUpsertDto(
    val id: String? = null,
    val workspaceId: String = "default",
    val name: String,
    val description: String = "",
    val tone: String = "",
    val boundaries: List<String> = emptyList(),
    val systemPromptTemplate: String = "",
    val enabled: Boolean = true,
    val agentNames: List<String> = emptyList(),
)

@Serializable
data class PersonaResolveRequest(
    val workspaceId: String = "default",
    val agentName: String,
)

@Serializable
data class PersonaResolveResponse(
    val persona: Persona?,
)

@Serializable
data class MemoryListResponse(
    val memories: List<MemoryRecord>,
)

@Serializable
data class MemorySaveRequest(
    val content: String,
    val type: MemoryType = MemoryType.FACT,
    val scope: MemoryScope = MemoryScope.GLOBAL,
    val workspaceId: String = "default",
    val platformId: String? = null,
    val sessionId: String? = null,
    val userId: String? = null,
    val agentName: String? = null,
    val tags: List<String> = emptyList(),
    val confidence: Double = 1.0,
    val expiresAt: Long? = null,
)

@Serializable
data class MemorySearchRequest(
    val query: String,
    val workspaceId: String = "default",
    val platformId: String? = null,
    val sessionId: String? = null,
    val userId: String? = null,
    val agentName: String? = null,
    val scope: MemoryScope? = null,
    val type: MemoryType? = null,
    val limit: Int = 10,
)

@Serializable
data class MemorySearchResponse(
    val results: List<MemorySearchResult>,
)

@Serializable
data class DeleteResponse(
    val deleted: Boolean,
)

@Serializable
data class ExpireMemoryResponse(
    val expired: Int,
)
