package com.heyanle.priestess.bot.agent

import com.heyanle.priestess.bot.config.AgentConfig
import com.heyanle.priestess.bot.core.controller.BaseController

/**
 * Agent 控制器，负责将配置转换为运行时 Agent 实例。
 */
class AgentController : BaseController("AgentController") {
    fun createAgent(config: AgentConfig): Agent {
        val compressStrategy = when (config.compressStrategy.lowercase()) {
            "token_window" -> CompressStrategy.TOKEN_WINDOW
            "llm_compress" -> CompressStrategy.LLM_COMPRESS
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
        )
    }
}
