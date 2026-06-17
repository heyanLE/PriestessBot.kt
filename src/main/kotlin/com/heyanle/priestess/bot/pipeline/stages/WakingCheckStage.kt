package com.heyanle.priestess.bot.pipeline.stages

import com.heyanle.priestess.bot.pipeline.PipelineConfig
import com.heyanle.priestess.bot.pipeline.PipelineContext
import com.heyanle.priestess.bot.pipeline.Stage
import com.heyanle.priestess.bot.pipeline.StageOrder
import com.heyanle.priestess.bot.platform.MessageComponent
import com.heyanle.priestess.bot.platform.SessionType
import kotlinx.coroutines.flow.Flow

/**
 * 唤醒检测阶段：
 * - 私聊消息 → 直接通过
 * - 群聊消息 → 需要满足以下之一：
 *   1. @提及机器人
 *   2. 消息以配置的前缀开头
 * - 频道消息 → 需要满足前缀或 @提及
 */
class WakingCheckStage(
    private val config: PipelineConfig,
) : Stage {

    override val name = "WakingCheck"
    override val order = StageOrder.WAKING_CHECK

    override suspend fun process(ctx: PipelineContext): Flow<Unit>? {
        // 私聊消息默认通过
        if (ctx.isPrivate) {
            return null
        }

        val text = ctx.textContent.trim()

        // 检查是否 @提及了机器人
        val hasAtMention = ctx.event.chain.components.any { component ->
            component is MessageComponent.At
        }

        // 检查是否有命令前缀
        val hasPrefix = config.wakingPrefix.isNotEmpty() && text.startsWith(config.wakingPrefix)

        if (!hasAtMention && !hasPrefix) {
            ctx.stop()
            log("Group message without @mention or prefix, stopping pipeline")
        }

        return null
    }

    private fun log(message: String) {
        println("[WakingCheck] $message")
    }
}
