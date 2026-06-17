package com.heyanle.priestess.bot.pipeline

import kotlinx.serialization.Serializable

/**
 * 管道级别配置，由 PriestessConfig 引用。
 */
@Serializable
data class PipelineConfig(
    /** 唤醒前缀（如 "/"），空字符串表示仅靠 @提及 唤醒 */
    val wakingPrefix: String = "/",

    /** 是否启用白名单过滤 */
    val whitelistEnabled: Boolean = false,

    /** 白名单用户 ID 列表（whitelistEnabled=true 时生效） */
    val whitelistUsers: List<String> = emptyList(),

    /** 白名单群组 ID 列表（whitelistEnabled=true 时生效） */
    val whitelistGroups: List<String> = emptyList(),

    /** 是否启用频率限制 */
    val rateLimitEnabled: Boolean = true,

    /** 每分钟每用户最大消息数 */
    val rateLimitPerMinute: Int = 20,

    /** 新会话默认是否启用 */
    val sessionEnabledByDefault: Boolean = true,

    /** 是否启用内容安全检查（v1 占位） */
    val contentSafetyEnabled: Boolean = false,

    /** 会话历史加载最大条数（用于 PreProcessStage） */
    val maxHistoryMessages: Int = 50,
)
