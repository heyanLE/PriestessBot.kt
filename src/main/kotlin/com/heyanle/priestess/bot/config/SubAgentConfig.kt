package com.heyanle.priestess.bot.config

import kotlinx.serialization.Serializable

/**
 * 子 Agent 编排配置，描述是否启用多 Agent、默认 Agent、可用 Agent 和路由规则。
 */
@Serializable
data class SubAgentOrchestrationConfig(
    val enabled: Boolean = false,
    val defaultAgentName: String = "",
    val agents: List<SubAgentConfig> = emptyList(),
    val routes: List<SubAgentRouteConfig> = emptyList(),
)

/**
 * 子 Agent 配置，定义一个可被编排调用的独立 Agent 画像和启停状态。
 */
@Serializable
data class SubAgentConfig(
    val name: String,
    val description: String = "",
    val agent: AgentConfig = AgentConfig(name = name),
    val enabled: Boolean = true,
)

/**
 * 子 Agent 路由配置，定义关键字命中后应转发到的目标 Agent 及优先级。
 */
@Serializable
data class SubAgentRouteConfig(
    val name: String,
    val targetAgentName: String,
    val keywords: List<String> = emptyList(),
    val priority: Int = 0,
    val enabled: Boolean = true,
)
