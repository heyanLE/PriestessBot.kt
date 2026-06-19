package com.heyanle.priestess.bot.config

import kotlinx.serialization.Serializable

@Serializable
data class PriestessConfig(
    val platforms: List<PlatformConfig> = emptyList(),
    val providers: List<ProviderConfig> = emptyList(),
    val agent: AgentConfig = AgentConfig(),
    val database: DatabaseConfig = DatabaseConfig(),
    val pipeline: PipelineConfig = PipelineConfig(),
)
