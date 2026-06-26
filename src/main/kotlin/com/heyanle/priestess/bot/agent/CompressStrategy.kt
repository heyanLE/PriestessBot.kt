package com.heyanle.priestess.bot.agent

/**
 * Agent 上下文压缩策略。
 */
enum class CompressStrategy {
    ROUND_TRUNCATION,
    TOKEN_WINDOW,
    LLM_COMPRESS,
}
