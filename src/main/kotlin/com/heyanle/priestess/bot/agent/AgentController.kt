package com.heyanle.priestess.bot.agent

import com.heyanle.priestess.bot.config.AgentConfig
import com.heyanle.priestess.bot.core.controller.BaseController

/**
 * Agent 控制器，负责将配置转换为运行时 Agent 实例。
 */
class AgentController : BaseController("AgentController") {
    fun createAgent(config: AgentConfig): Agent {
        val compressStrategy = when (config.compressStrategy.lowercase()) {
            "token_window", "llm_compress" -> CompressStrategy.TOKEN_WINDOW
            else -> CompressStrategy.ROUND_TRUNCATION
        }

        return Agent(
            name = config.name,
            instructions = config.instructions,
            model = config.model,
            maxSteps = config.maxSteps,
            toolTimeoutMs = config.toolTimeoutSeconds * 1000,
            compressStrategy = compressStrategy,
            maxContextTokens = config.maxTokens,
            maxContextRounds = config.maxRounds,
            toolResultInlineTokens = config.toolResultInlineTokens.coerceAtLeast(1),
            toolResultPreviewTokens = config.toolResultPreviewTokens.coerceIn(1, config.toolResultInlineTokens.coerceAtLeast(1)),
            toolResultTtlSeconds = config.toolResultTtlSeconds.coerceAtLeast(1),
            toolResultMaxBytes = config.toolResultMaxBytes.coerceAtLeast(1),
            toolResultStoreMaxBytes = config.toolResultStoreMaxBytes.coerceAtLeast(config.toolResultMaxBytes.coerceAtLeast(1)),
        )
    }
}
