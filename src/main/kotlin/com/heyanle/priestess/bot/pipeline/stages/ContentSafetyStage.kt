package com.heyanle.priestess.bot.pipeline.stages

import com.heyanle.priestess.bot.config.PipelineConfig
import com.heyanle.priestess.bot.pipeline.PipelineContext
import com.heyanle.priestess.bot.pipeline.Stage
import com.heyanle.priestess.bot.pipeline.StageOrder
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow

/**
 * 内容安全检查阶段（v1 占位实现）。
 *
 * 当前直接放行所有消息，预留敏感词过滤等安全检查的扩展点。
 */
class ContentSafetyStage(
    private val config: PipelineConfig,
) : Stage {
    private val logger = KotlinLogging.logger {}

    override val name = "ContentSafety"
    override val order = StageOrder.CONTENT_SAFETY

    override suspend fun process(ctx: PipelineContext): Flow<Unit>? {
        if (!config.contentSafetyEnabled) return null

        // v1 占位：默认放行所有内容
        // 二期可接入敏感词库、内容审核 API 等

        val text = ctx.textContent
        if (text.isBlank()) {
            // 无文本内容的消息默认放行（如图片消息）
            return null
        }

        // 预留：在此处添加安全检查逻辑
        // 如果检测到不安全内容，调用 ctx.stop() 并可选发送警告

        return null
    }

    private fun log(message: String) {
        logger.info { message }
    }
}
