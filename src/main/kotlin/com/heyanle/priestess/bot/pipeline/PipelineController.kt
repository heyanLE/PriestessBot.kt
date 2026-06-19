package com.heyanle.priestess.bot.pipeline

import com.heyanle.priestess.bot.agent.AgentCase
import com.heyanle.priestess.bot.agent.context.ContextManager
import com.heyanle.priestess.bot.config.ConfigCase
import com.heyanle.priestess.bot.conversation.ConversationCase
import com.heyanle.priestess.bot.core.controller.BaseController
import com.heyanle.priestess.bot.pipeline.stages.ContentSafetyStage
import com.heyanle.priestess.bot.pipeline.stages.PreProcessStage
import com.heyanle.priestess.bot.pipeline.stages.ProcessStage
import com.heyanle.priestess.bot.pipeline.stages.RateLimitStage
import com.heyanle.priestess.bot.pipeline.stages.RespondStage
import com.heyanle.priestess.bot.pipeline.stages.ResultDecorateStage
import com.heyanle.priestess.bot.pipeline.stages.SessionStatusStage
import com.heyanle.priestess.bot.pipeline.stages.WakingCheckStage
import com.heyanle.priestess.bot.pipeline.stages.WhitelistCheckStage
import com.heyanle.priestess.bot.platform.MessageEvent
import com.heyanle.priestess.bot.provider.ProviderCase
import com.heyanle.priestess.bot.tool.ToolController
import com.heyanle.priestess.bot.tool.ToolExecutor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

/**
 * Owns the ordered message-processing stage pipeline.
 *
 * Stages are constructed inside this controller instead of being registered in DI,
 * keeping pipeline ordering and composition as an internal implementation detail.
 * Incoming platform messages enter through PipelineCase and are processed on this
 * controller's task scope.
 */
class PipelineController private constructor(
    stages: List<Stage>,
) : BaseController("PipelineController") {

    constructor(
        configCase: ConfigCase,
        conversationCase: ConversationCase,
        agentCase: AgentCase,
        contextManager: ContextManager,
        providerCase: ProviderCase,
        toolExecutor: ToolExecutor,
        toolController: ToolController,
    ) : this(
        buildStages(
            configCase = configCase,
            conversationCase = conversationCase,
            agentCase = agentCase,
            contextManager = contextManager,
            providerCase = providerCase,
            toolExecutor = toolExecutor,
            toolController = toolController,
        ),
    )

    internal constructor(testStages: List<Stage>, @Suppress("UNUSED_PARAMETER") testOnly: Unit) : this(testStages)

    private val orderedStages: List<Stage> = stages.sortedBy { it.order.level }

    fun process(event: MessageEvent): Job {
        return launchTask("process-message") {
            logger.info {
                "[PIPELINE-040] PipelineController start platform=${event.platform.metadata.name}, " +
                    "session=${event.session.id}, stages=${orderedStages.size}"
            }
            val ctx = PipelineContext(event)
            executePipeline(ctx, 0)
            logger.info {
                "[PIPELINE-049] PipelineController done platform=${event.platform.metadata.name}, " +
                    "session=${event.session.id}, stopped=${ctx.isStopped}"
            }
        }
    }

    private suspend fun executePipeline(ctx: PipelineContext, stageIndex: Int) {
        if (stageIndex >= orderedStages.size) return
        if (ctx.isStopped) return

        val stage = orderedStages[stageIndex]
        val flow: Flow<Unit>? = try {
            logger.info { "[PIPELINE-04${stage.order.level}] Enter stage ${stage.order.level}:${stage.name}" }
            stage.process(ctx)
        } catch (e: Exception) {
            logger.error(e) { "[PIPELINE-59${stage.order.level}] Stage '${stage.name}' failed" }
            return
        }

        if (flow == null) {
            logger.info {
                "[PIPELINE-14${stage.order.level}] Exit stage ${stage.order.level}:${stage.name}, stopped=${ctx.isStopped}"
            }
            executePipeline(ctx, stageIndex + 1)
        } else {
            executePipeline(ctx, stageIndex + 1)
            try {
                logger.info { "[PIPELINE-24${stage.order.level}] Collect post stage ${stage.order.level}:${stage.name}" }
                flow.collect {}
                logger.info { "[PIPELINE-34${stage.order.level}] Post stage complete ${stage.order.level}:${stage.name}" }
            } catch (e: Exception) {
                logger.error(e) { "[PIPELINE-69${stage.order.level}] Stage '${stage.name}' post-processing failed" }
            }
        }
    }

    companion object {
        private fun buildStages(
            configCase: ConfigCase,
            conversationCase: ConversationCase,
            agentCase: AgentCase,
            contextManager: ContextManager,
            providerCase: ProviderCase,
            toolExecutor: ToolExecutor,
            toolController: ToolController,
        ): List<Stage> {
            val config = configCase.current()
            return listOf(
                WakingCheckStage(config.pipeline),
                WhitelistCheckStage(config.pipeline),
                SessionStatusStage(config.pipeline),
                RateLimitStage(config.pipeline),
                ContentSafetyStage(config.pipeline),
                PreProcessStage(
                    agentConfig = config.agent,
                    pipelineConfig = config.pipeline,
                    conversationCase = conversationCase,
                    agentCase = agentCase,
                    contextManager = contextManager,
                ),
                ProcessStage(
                    providerCase = providerCase,
                    toolExecutor = toolExecutor,
                    toolController = toolController,
                    contextManager = contextManager,
                ),
                ResultDecorateStage(),
                RespondStage(),
            )
        }
    }
}
