package com.heyanle.priestess.bot.pipeline.stages

import com.heyanle.priestess.bot.pipeline.PipelineConfig
import com.heyanle.priestess.bot.pipeline.PipelineContext
import com.heyanle.priestess.bot.pipeline.Stage
import com.heyanle.priestess.bot.pipeline.StageOrder
import com.heyanle.priestess.bot.platform.SessionType
import kotlinx.coroutines.flow.Flow

/**
 * 白名单过滤阶段：
 * - 如果未启用白名单 → 直接通过
 * - 启用时检查发送者或群组是否在白名单内
 */
class WhitelistCheckStage(
    private val config: PipelineConfig,
) : Stage {

    override val name = "WhitelistCheck"
    override val order = StageOrder.WHITELIST_CHECK

    override suspend fun process(ctx: PipelineContext): Flow<Unit>? {
        if (!config.whitelistEnabled) return null

        val senderId = ctx.senderId
        val sessionId = ctx.event.session.id

        // 检查用户是否在白名单
        val userWhitelisted = senderId in config.whitelistUsers

        // 检查群组是否在白名单（仅群聊场景）
        val groupWhitelisted = if (ctx.event.session.type == SessionType.GROUP) {
            sessionId in config.whitelistGroups
        } else {
            false
        }

        if (!userWhitelisted && !groupWhitelisted) {
            ctx.stop()
            log("User '$senderId' not in whitelist, stopping pipeline")
        }

        return null
    }

    private fun log(message: String) {
        println("[WhitelistCheck] $message")
    }
}
