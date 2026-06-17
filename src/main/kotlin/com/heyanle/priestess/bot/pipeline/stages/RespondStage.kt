package com.heyanle.priestess.bot.pipeline.stages

import com.heyanle.priestess.bot.pipeline.PipelineContext
import com.heyanle.priestess.bot.pipeline.Stage
import com.heyanle.priestess.bot.pipeline.StageOrder
import com.heyanle.priestess.bot.platform.MessageChain
import kotlinx.coroutines.flow.Flow

/**
 * 回复发送阶段（管道最后一环）。
 *
 * 将装饰后的响应通过原始 Platform 发送给用户。
 * 负责会话持久化已在 PreProcessStage 的后置逻辑中处理。
 */
class RespondStage : Stage {

    override val name = "Respond"
    override val order = StageOrder.RESPOND

    override suspend fun process(ctx: PipelineContext): Flow<Unit>? {
        val responseText = ctx.shared["decoratedResponse"] as? String
        val responseType = ctx.shared["responseType"] as? String ?: "text"

        if (responseText.isNullOrBlank()) {
            log("No response to send, skipping")
            return null
        }

        val platform = ctx.event.platform
        val session = ctx.event.session

        try {
            val chain = MessageChain.text(responseText)
            platform.sendMessage(session, chain)
            log("Response sent (type=$responseType): ${responseText.take(100)}...")
        } catch (e: Exception) {
            System.err.println("[Respond] Failed to send message: ${e.message}")
            e.printStackTrace()
        }

        return null
    }

    private fun log(message: String) {
        println("[Respond] $message")
    }
}
