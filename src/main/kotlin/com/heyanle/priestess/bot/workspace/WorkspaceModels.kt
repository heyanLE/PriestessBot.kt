package com.heyanle.priestess.bot.workspace

import com.heyanle.priestess.bot.config.AgentConfig
import com.heyanle.priestess.bot.config.SubAgentOrchestrationConfig
import com.heyanle.priestess.bot.tool.FunctionTool
import com.heyanle.priestess.bot.tool.ToolRiskLevel
import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicBoolean

@Serializable
data class WorkspaceConfig(
    val id: String = DEFAULT_WORKSPACE_ID,
    val name: String = "Default Workspace",
    val enabled: Boolean = true,
    val isDefault: Boolean = false,
    val rules: List<String> = emptyList(),
    val agents: List<AgentConfig> = emptyList(),
    val providerName: String = "",
    val skills: List<WorkspaceSkillConfig> = emptyList(),
    val mcpServers: List<WorkspaceMcpServerConfig> = emptyList(),
    val tools: WorkspaceToolConfig = WorkspaceToolConfig(),
    val personas: List<WorkspacePersonaConfig> = emptyList(),
    val memory: WorkspaceMemoryPolicyConfig = WorkspaceMemoryPolicyConfig(),
    val subAgents: SubAgentOrchestrationConfig = SubAgentOrchestrationConfig(),
    val resolution: WorkspaceResolutionConfig = WorkspaceResolutionConfig(),
) {
    companion object {
        const val DEFAULT_WORKSPACE_ID = "default"
    }
}

@Serializable
data class WorkspaceSkillConfig(
    val name: String,
    val enabled: Boolean = true,
    val settings: Map<String, String> = emptyMap(),
)

@Serializable
data class WorkspaceMcpServerConfig(
    val id: String,
    val enabled: Boolean = true,
    val transport: String = "stdio",
    val command: String = "",
    val args: List<String> = emptyList(),
    val url: String = "",
    val env: Map<String, String> = emptyMap(),
)

@Serializable
data class WorkspaceToolConfig(
    val enabledTools: List<String> = emptyList(),
    val disabledTools: List<String> = emptyList(),
    val allowedRiskLevels: List<ToolRiskLevel> = ToolRiskLevel.entries,
)

@Serializable
data class WorkspacePersonaConfig(
    val id: String,
    val enabled: Boolean = true,
    val agentNames: List<String> = emptyList(),
)

@Serializable
data class WorkspaceMemoryPolicyConfig(
    val enabled: Boolean = true,
    val allowedScopes: List<String> = listOf("GLOBAL", "PLATFORM", "SESSION", "USER", "AGENT"),
    val knowledgeBaseIds: List<String> = emptyList(),
    val maxInjectedMemories: Int = 5,
)

@Serializable
data class WorkspaceResolutionConfig(
    val platformNames: List<String> = emptyList(),
    val sessionIds: List<String> = emptyList(),
    val userIds: List<String> = emptyList(),
)

data class WorkspaceResolution(
    val snapshot: WorkspaceSnapshot,
    val reason: String,
    val lease: WorkspaceSnapshotLease? = null,
)

data class WorkspaceSnapshotKey(
    val workspaceId: String,
    val version: Long,
)

data class WorkspaceSnapshot(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val version: Long,
    val loadedAt: Long,
    val rootDir: String,
    val rules: List<String> = emptyList(),
    val config: WorkspaceConfig,
    val agentConfigs: List<AgentConfig>,
    val providerName: String,
    val toolNames: List<String>,
    val skillDescriptors: List<WorkspaceSkillDescriptor>,
    val skillSettings: Map<String, Map<String, String>>,
    val mcpServers: List<WorkspaceMcpServerDeclaration>,
    val mcpToolNames: List<String> = emptyList(),
    val mcpResources: List<WorkspaceMcpResource> = emptyList(),
    val mcpHandles: List<WorkspaceMcpClientHandle> = emptyList(),
    val personaIds: List<String>,
    val memoryPolicy: WorkspaceMemoryPolicyConfig,
    val diagnostics: List<String> = emptyList(),
) {
    val skillNames: List<String>
        get() = skillDescriptors.map { it.name }

    val mcpServerIds: List<String>
        get() = mcpServers.map { it.id }

    fun closeMcpHandles() {
        mcpHandles.forEach { handle ->
            runCatching { handle.close() }
        }
    }

    fun key(): WorkspaceSnapshotKey = WorkspaceSnapshotKey(
        workspaceId = id,
        version = version,
    )
}

data class WorkspaceSkillDescriptor(
    val name: String,
    val description: String = "",
    val enabled: Boolean = true,
    val directoryPath: String,
    val skillMarkdownPath: String,
    val inlineMarkdown: String? = null,
    val settings: Map<String, String> = emptyMap(),
)

@Serializable
data class WorkspaceMcpServerDeclaration(
    val id: String,
    val enabled: Boolean = true,
    val transport: String = "stdio",
    val command: String = "",
    val args: List<String> = emptyList(),
    val url: String = "",
    val env: Map<String, String> = emptyMap(),
    val sourcePath: String = "",
)

data class WorkspaceMcpToolResolution(
    val resources: List<WorkspaceMcpResource> = emptyList(),
    val handles: List<WorkspaceMcpClientHandle> = emptyList(),
    val diagnostics: List<String> = emptyList(),
) {
    val toolNames: List<String> = resources.map { it.tool.schema.name }
}

data class WorkspaceMcpResource(
    val tool: FunctionTool,
    val handle: WorkspaceMcpClientHandle,
)

fun interface WorkspaceMcpClientHandle {
    fun close()
}

interface WorkspaceMcpToolResolver {
    fun resolve(workspaceId: String, servers: List<WorkspaceMcpServerConfig>): WorkspaceMcpToolResolution

    companion object {
        val Noop = object : WorkspaceMcpToolResolver {
            override fun resolve(
                workspaceId: String,
                servers: List<WorkspaceMcpServerConfig>,
            ): WorkspaceMcpToolResolution = WorkspaceMcpToolResolution()
        }
    }
}

class WorkspaceMcpResolutionException(
    message: String,
    val handles: List<WorkspaceMcpClientHandle> = emptyList(),
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class WorkspaceSnapshotLease internal constructor(
    private val controller: WorkspaceController,
    val snapshot: WorkspaceSnapshot,
) : AutoCloseable {
    private val closed = AtomicBoolean(false)

    init {
        controller.retainSnapshot(snapshot)
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            controller.releaseSnapshot(snapshot)
        }
    }
}

@Serializable
data class WorkspaceStatus(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val activeSnapshotVersion: Long? = null,
    val loadedAt: Long? = null,
    val lastReload: WorkspaceReloadResult? = null,
    val diagnostics: List<String> = emptyList(),
)

@Serializable
data class WorkspaceReloadPlan(
    val workspaceId: String,
    val oldVersion: Long? = null,
    val newVersion: Long,
    val added: List<String> = emptyList(),
    val removed: List<String> = emptyList(),
    val modified: List<String> = emptyList(),
)

@Serializable
data class WorkspaceReloadResult(
    val workspaceId: String,
    val success: Boolean,
    val status: String,
    val snapshotVersion: Long? = null,
    val timestamp: Long,
    val plan: WorkspaceReloadPlan? = null,
    val diagnostics: List<String> = emptyList(),
    val errorSummary: String? = null,
)

data class WorkspaceConfigSet(
    val workspaces: List<WorkspaceConfig> = emptyList(),
    val defaultWorkspaceDir: String = "",
    val defaults: WorkspaceRuntimeDefaults = WorkspaceRuntimeDefaults(),
    val diagnostics: List<String> = emptyList(),
)

data class WorkspaceRuntimeDefaults(
    val baseConfig: WorkspaceConfig = WorkspaceConfig(
        id = WorkspaceConfig.DEFAULT_WORKSPACE_ID,
        name = "Default Workspace",
        enabled = true,
        isDefault = true,
    ),
)

data class WorkspaceValidationResult(
    val valid: Boolean,
    val diagnostics: List<String> = emptyList(),
) {
    companion object {
        fun ok(diagnostics: List<String> = emptyList()) = WorkspaceValidationResult(true, diagnostics)
        fun failed(diagnostics: List<String>) = WorkspaceValidationResult(false, diagnostics)
    }
}
