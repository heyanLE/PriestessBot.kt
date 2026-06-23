package com.heyanle.priestess.bot.server

import com.heyanle.priestess.bot.config.AgentConfig
import com.heyanle.priestess.bot.config.SubAgentOrchestrationConfig
import com.heyanle.priestess.bot.knowledge.KnowledgeBase
import com.heyanle.priestess.bot.knowledge.KnowledgeChunk
import com.heyanle.priestess.bot.plugin.PluginDescriptor
import com.heyanle.priestess.bot.plugin.PluginExtensionMetadata
import com.heyanle.priestess.bot.provider.LLMKind
import com.heyanle.priestess.bot.tool.ToolParameters
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
)

@Serializable
data class AgentChatEventDto(
    val type: String,
    val message: String,
    val toolName: String? = null,
    val success: Boolean? = null,
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
