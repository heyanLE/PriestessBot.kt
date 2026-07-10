package com.heyanle.priestess.bot.config

import kotlinx.serialization.Serializable

/**
 * 系统完整配置根对象，聚合平台、模型、Agent、数据库、流水线、服务端、插件和工作区配置。
 */
@Serializable
data class PriestessConfig(
    val platforms: List<PlatformConfig> = emptyList(),
    val providers: List<ProviderConfig> = emptyList(),
    val agent: AgentConfig = AgentConfig(),
    val database: DatabaseConfig = DatabaseConfig(),
    val pipeline: PipelineConfig = PipelineConfig(),
    val server: ServerConfig = ServerConfig(),
    val plugins: PluginConfig = PluginConfig(),
    val subAgents: SubAgentOrchestrationConfig = SubAgentOrchestrationConfig(),
    val workspace: WorkspaceRuntimeConfig = WorkspaceRuntimeConfig(),
)
