package com.heyanle.priestess.bot.config

import kotlinx.serialization.Serializable

@Serializable
data class SubAgentOrchestrationConfig(
    val enabled: Boolean = false,
    val defaultAgentName: String = "",
    val agents: List<SubAgentConfig> = emptyList(),
    val routes: List<SubAgentRouteConfig> = emptyList(),
)

@Serializable
data class SubAgentConfig(
    val name: String,
    val description: String = "",
    val agent: AgentConfig = AgentConfig(name = name),
    val enabled: Boolean = true,
)

@Serializable
data class SubAgentRouteConfig(
    val name: String,
    val targetAgentName: String,
    val keywords: List<String> = emptyList(),
    val priority: Int = 0,
    val enabled: Boolean = true,
)
