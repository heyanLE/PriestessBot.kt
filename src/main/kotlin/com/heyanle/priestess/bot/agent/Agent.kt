package com.heyanle.priestess.bot.agent

data class Agent(
    val name: String,
    val instructions: String,
    val model: String,
    val maxSteps: Int = 10,
    val toolTimeoutMs: Long = 30_000L,
    val compressStrategy: CompressStrategy = CompressStrategy.ROUND_TRUNCATION,
    val maxContextTokens: Int = 8000,
    val maxContextRounds: Int = 20,
)
