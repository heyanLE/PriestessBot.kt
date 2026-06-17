package com.heyanle.priestess.bot.pipeline

import com.heyanle.priestess.bot.core.event.Event
import com.heyanle.priestess.bot.core.event.EventBus
import com.heyanle.priestess.bot.core.lifecycle.LifecycleAware
import com.heyanle.priestess.bot.platform.MessageEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Pipeline 调度器。
 *
 * 使用递归 + Flow 实现洋葱模型：
 * - [Stage.process] 返回 `null` → 线性，直接进入下一阶段
 * - [Stage.process] 返回 `Flow<Unit>` → 洋葱：前置逻辑已执行 →
 *   递归调度剩余阶段 → collect Flow 完成后置逻辑
 *
 * 监听 [EventBus] 中的 [MessageEvent]，对每条消息执行完整的 9 阶段管道。
 */
class PipelineScheduler(
    private val eventBus: EventBus,
    private val scope: CoroutineScope,
    stages: List<Stage>,
) : LifecycleAware {

    private val orderedStages: List<Stage> = stages.sortedBy { it.order.level }

    private var subscriptionJob: Job? = null

    override suspend fun start() {
        // 初始化所有阶段
        val dummyCtx = PipelineContext(
            MessageEvent(
                platform = object : com.heyanle.priestess.bot.platform.Platform(eventBus) {
                    override val metadata = com.heyanle.priestess.bot.platform.PlatformMetadata(
                        name = "dummy", displayName = "dummy",
                        supportStreaming = false, supportProactiveMessage = false,
                    )
                    override suspend fun run(): Job = Job()
                    override suspend fun terminate() {}
                    override suspend fun sendMessage(
                        session: com.heyanle.priestess.bot.platform.MessageSession,
                        chain: com.heyanle.priestess.bot.platform.MessageChain,
                    ) {}
                },
                session = com.heyanle.priestess.bot.platform.MessageSession(
                    id = "dummy",
                    type = com.heyanle.priestess.bot.platform.SessionType.PRIVATE,
                    platformName = "dummy",
                ),
                chain = com.heyanle.priestess.bot.platform.MessageChain.text(""),
            )
        )
        for (stage in orderedStages) {
            stage.initialize(dummyCtx)
        }

        // 订阅事件总线
        subscriptionJob = eventBus.subscribe { event ->
            if (event is MessageEvent) {
                launchPipeline(event)
            }
        }
    }

    override suspend fun stop() {
        subscriptionJob?.cancel()
    }

    /**
     * 为一条消息启动完整的管道处理。
     */
    private fun launchPipeline(event: MessageEvent) {
        scope.launch {
            try {
                val ctx = PipelineContext(event)
                executePipeline(ctx, 0)
            } catch (e: Exception) {
                System.err.println(
                    "[PipelineScheduler] ERROR processing message: ${e.message}"
                )
                e.printStackTrace()
            }
        }
    }

    /**
     * 递归执行管道：从 [stageIndex] 开始执行剩余阶段。
     *
     * @param ctx  管道上下文
     * @param stageIndex 当前阶段在 [orderedStages] 中的索引
     */
    private suspend fun executePipeline(ctx: PipelineContext, stageIndex: Int) {
        if (stageIndex >= orderedStages.size) return
        if (ctx.isStopped) return

        val stage = orderedStages[stageIndex]
        val flow: Flow<Unit>?

        try {
            flow = stage.process(ctx)
        } catch (e: Exception) {
            System.err.println(
                "[PipelineScheduler] Stage '${stage.name}' threw exception: ${e.message}"
            )
            e.printStackTrace()
            return
        }

        if (flow == null) {
            // 线性阶段：直接进入下一阶段
            executePipeline(ctx, stageIndex + 1)
        } else {
            // 洋葱阶段：前置逻辑已在 process() 中执行
            // → 递归执行后续阶段
            executePipeline(ctx, stageIndex + 1)
            // → 收集 Flow 完成后置逻辑
            try {
                flow.collect {}
            } catch (e: Exception) {
                System.err.println(
                    "[PipelineScheduler] Stage '${stage.name}' post-processing error: ${e.message}"
                )
            }
        }
    }

    @Suppress("UnusedPrivateMember")
    private fun log(message: String) {
        println("[PipelineScheduler] $message")
    }
}
