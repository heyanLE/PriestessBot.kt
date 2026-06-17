package com.heyanle.priestess.bot.pipeline.stages

import com.heyanle.priestess.bot.pipeline.PipelineConfig
import com.heyanle.priestess.bot.pipeline.PipelineContext
import com.heyanle.priestess.bot.pipeline.Stage
import com.heyanle.priestess.bot.pipeline.StageOrder
import kotlinx.coroutines.flow.Flow

/**
 * 会话状态检查阶段：
 * - 检查会话是否被禁用
 * - 使用 [PipelineContext.shared] 中存储的每个会话的启用/禁用状态
 * - 默认根据配置决定是否启用
 */
class SessionStatusStage(
    private val config: PipelineConfig,
) : Stage {

    override val name = "SessionStatus"
    override val order = StageOrder.SESSION_STATUS

    /** 禁用的会话 ID 集合（阶段初始化时从配置或其他来源加载） */
    private val disabledSessions = mutableSetOf<String>()

    override suspend fun process(ctx: PipelineContext): Flow<Unit>? {
        val sessionId = ctx.event.session.id

        // 检查会话是否在禁用列表中
        val sessionEnabled = !disabledSessions.contains(sessionId)

        if (!sessionEnabled) {
            ctx.stop()
            log("Session '$sessionId' is disabled, stopping pipeline")
        }

        return null
    }

    /** 禁用指定会话 */
    fun disableSession(sessionId: String) {
        disabledSessions.add(sessionId)
    }

    /** 启用指定会话 */
    fun enableSession(sessionId: String) {
        disabledSessions.remove(sessionId)
    }

    /** 检查会话是否启用 */
    fun isSessionEnabled(sessionId: String): Boolean {
        return !disabledSessions.contains(sessionId)
    }

    private fun log(message: String) {
        println("[SessionStatus] $message")
    }
}
