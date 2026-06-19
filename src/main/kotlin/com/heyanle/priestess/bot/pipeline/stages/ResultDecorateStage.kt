package com.heyanle.priestess.bot.pipeline.stages

import com.heyanle.priestess.bot.agent.AgentResponse
import com.heyanle.priestess.bot.pipeline.PipelineContext
import com.heyanle.priestess.bot.pipeline.Stage
import com.heyanle.priestess.bot.pipeline.StageOrder
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow

/**
 * 结果装饰阶段。
 *
 * 对 Agent 的响应进行格式化处理：
 * - 错误响应 → 友好的错误提示
 * - 正常响应 → 可选 Markdown 渲染（v1 占位）、添加前缀/后缀
 *
 * v1 实现：透传 Agent 响应文本，错误时转换为用户友好的提示。
 */
class ResultDecorateStage : Stage {
    private val logger = KotlinLogging.logger {}

    override val name = "ResultDecorate"
    override val order = StageOrder.RESULT_DECORATE

    override suspend fun process(ctx: PipelineContext): Flow<Unit>? {
        val response = ctx.agentResponse

        when (response) {
            is AgentResponse.Final -> {
                // 正常响应：v1 透传，保留 Markdown 渲染扩展点
                // 二期可在此处进行 Markdown → 平台格式转换
                val decorated = decorateText(response.content)
                ctx.shared["decoratedResponse"] = decorated
                ctx.shared["responseType"] = "text"
            }
            is AgentResponse.Error -> {
                // 错误响应：转换为友好提示
                val friendlyMessage = when {
                    response.message.contains("Exceeded maximum steps", ignoreCase = true) ->
                        "抱歉，处理您的请求超时了，请稍后再试或简化您的问题。"
                    response.message.contains("AgentContext not initialized", ignoreCase = true) ->
                        "系统初始化异常，请稍后再试。"
                    else ->
                        "抱歉，处理您的请求时出现了错误：${response.message}"
                }
                ctx.shared["decoratedResponse"] = friendlyMessage
                ctx.shared["responseType"] = "error"
            }
            is AgentResponse.Thinking -> {
                // 不应该在 Final 阶段看到 Thinking
                ctx.shared["decoratedResponse"] = "处理中，但未获得最终结果。"
                ctx.shared["responseType"] = "error"
            }
            is AgentResponse.ToolExecuted -> {
                // 不应该在 Final 阶段看到 ToolExecuted
                ctx.shared["decoratedResponse"] = "工具执行完成，但未获得最终回答。"
                ctx.shared["responseType"] = "error"
            }
            null -> {
                ctx.shared["decoratedResponse"] = "未收到任何响应。"
                ctx.shared["responseType"] = "error"
            }
        }

        return null
    }

    /**
     * 装饰文本响应。
     * v1 直接返回原文，v2 可扩展 Markdown 渲染。
     */
    private fun decorateText(content: String): String {
        // v1: 直接透传
        // v2: Markdown 渲染、平台格式适配等
        return content
    }

    private fun log(message: String) {
        logger.info { message }
    }
}
