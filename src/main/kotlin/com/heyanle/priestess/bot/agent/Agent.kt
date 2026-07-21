package com.heyanle.priestess.bot.agent

/**
 * 运行时 Agent 定义，描述模型、提示词、工具步数和上下文压缩策略。
 */
data class Agent(
    val name: String,
    val instructions: String,
    val model: String,
    val maxSteps: Int = 10,
    val toolTimeoutMs: Long = 30_000L,
    val compressStrategy: CompressStrategy = CompressStrategy.ROUND_TRUNCATION,
    val maxContextTokens: Int = 8000,
    val maxContextRounds: Int = 20,
    val toolResultInlineTokens: Int = 2048,
    val toolResultPreviewTokens: Int = 512,
    val toolResultTtlSeconds: Long = 1800,
    val toolResultMaxBytes: Long = 4 * 1024 * 1024,
    val toolResultStoreMaxBytes: Long = 64 * 1024 * 1024,
)
