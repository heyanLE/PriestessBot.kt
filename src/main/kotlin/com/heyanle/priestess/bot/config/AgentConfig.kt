package com.heyanle.priestess.bot.config

import kotlinx.serialization.Serializable

/**
 * 主 Agent 运行配置，描述默认身份、模型参数、上下文压缩和工具访问策略。
 */
@Serializable
data class AgentConfig(
    val name: String = "assistant",
    val instructions: String = "You are a helpful assistant.",
    val model: String = "gpt-4o",
    val providerName: String = "openai",
    val maxSteps: Int = 10,
    val temperature: Double = 0.7,
    val compressStrategy: String = "token_window",
    val maxRounds: Int = 20,
    val maxTokens: Int = 4096,
    val toolTimeoutSeconds: Long = 30,
    val enabledTools: List<String> = emptyList(),
    val disabledTools: List<String> = emptyList(),
    val allowedRiskLevels: List<com.heyanle.priestess.bot.tool.ToolRiskLevel> = com.heyanle.priestess.bot.tool.ToolRiskLevel.entries,
)
