package com.heyanle.priestess.bot.pipeline

import com.heyanle.priestess.bot.agent.AgentCase
import com.heyanle.priestess.bot.agent.context.ContextManager
import com.heyanle.priestess.bot.agent.orchestration.SubAgentOrchestrator
import com.heyanle.priestess.bot.config.ConfigCase
import com.heyanle.priestess.bot.conversation.ConversationCase
import com.heyanle.priestess.bot.core.controller.BaseController
import com.heyanle.priestess.bot.observability.MetricsRegistry
import com.heyanle.priestess.bot.pipeline.stages.ContentSafetyStage
import com.heyanle.priestess.bot.pipeline.stages.PreProcessStage
import com.heyanle.priestess.bot.pipeline.stages.ProcessStage
import com.heyanle.priestess.bot.pipeline.stages.RateLimitStage
import com.heyanle.priestess.bot.pipeline.stages.RespondStage
import com.heyanle.priestess.bot.pipeline.stages.ResultDecorateStage
import com.heyanle.priestess.bot.pipeline.stages.SessionStatusStage
import com.heyanle.priestess.bot.pipeline.stages.WakingCheckStage
import com.heyanle.priestess.bot.pipeline.stages.WhitelistCheckStage
import com.heyanle.priestess.bot.persona.PersonaMemoryInjector
import com.heyanle.priestess.bot.platform.MessageEvent
import com.heyanle.priestess.bot.provider.ProviderCase
import com.heyanle.priestess.bot.skill.SkillCase
import com.heyanle.priestess.bot.tool.ToolController
import com.heyanle.priestess.bot.tool.ToolExecutor
import com.heyanle.priestess.bot.workspace.WorkspaceController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Owns the ordered message-processing stage pipeline.
 *
 * Stages are constructed inside this controller instead of being registered in DI,
 * keeping pipeline ordering and composition as an internal implementation detail.
 * Incoming platform messages enter through PipelineCase and are processed on this
 * controller's task scope.
 */
class PipelineController private constructor(
    private val stageProvider: () -> List<Stage>,
    private val metricsRegistry: MetricsRegistry = MetricsRegistry(),
    private val drainTimeoutMillis: Long = DEFAULT_DRAIN_TIMEOUT_MILLIS,
) : BaseController("PipelineController") {

    private val activeMessageJobs = mutableSetOf<Job>()
    @Volatile
    private var shuttingDown = false

    constructor(
        configCase: ConfigCase,
        conversationCase: ConversationCase,
        agentCase: AgentCase,
        contextManager: ContextManager,
        providerCase: ProviderCase,
        toolExecutor: ToolExecutor,
        toolController: ToolController,
        skillCase: SkillCase? = null,
        subAgentOrchestrator: SubAgentOrchestrator? = null,
        workspaceController: WorkspaceController? = null,
        personaMemoryInjector: PersonaMemoryInjector? = null,
        metricsRegistry: MetricsRegistry = MetricsRegistry(),
        drainTimeoutMillis: Long = DEFAULT_DRAIN_TIMEOUT_MILLIS,
    ) : this({
        buildStages(
            configCase = configCase,
            conversationCase = conversationCase,
            agentCase = agentCase,
            contextManager = contextManager,
            providerCase = providerCase,
            toolExecutor = toolExecutor,
            toolController = toolController,
            skillCase = skillCase,
            subAgentOrchestrator = subAgentOrchestrator,
            workspaceController = workspaceController,
            personaMemoryInjector = personaMemoryInjector,
            metricsRegistry = metricsRegistry,
        )
    }, metricsRegistry, drainTimeoutMillis)

    internal constructor(
        testStages: List<Stage>,
        @Suppress("UNUSED_PARAMETER") testOnly: Unit,
        metricsRegistry: MetricsRegistry = MetricsRegistry(),
        drainTimeoutMillis: Long = DEFAULT_DRAIN_TIMEOUT_MILLIS,
    ) : this({ testStages }, metricsRegistry, drainTimeoutMillis)

    fun process(event: MessageEvent): Job {
        if (shuttingDown) {
            logger.warn {
                "[PIPELINE-090] Reject message while shutting down platform=${event.platform.metadata.name}, " +
                    "session=${event.session.id}"
            }
            return Job().also { it.cancel(CancellationException("Pipeline is shutting down")) }
        }

        val job = launchTask("process-message") {
            val startedAtNanos = System.nanoTime()
            val platformName = event.platform.metadata.name
            var status = "completed"
            val orderedStages = stageProvider().sortedBy { it.order.level }
            try {
                logger.info {
                    "[PIPELINE-040] PipelineController start platform=${event.platform.metadata.name}, " +
                        "session=${event.session.id}, stages=${orderedStages.size}"
                }
                val ctx = PipelineContext(event)
                try {
                    executePipeline(ctx, orderedStages, 0)
                    logger.info {
                        "[PIPELINE-049] PipelineController done platform=${event.platform.metadata.name}, " +
                            "session=${event.session.id}, stopped=${ctx.isStopped}"
                    }
                } finally {
                    ctx.releaseWorkspace()
                }
            } catch (e: CancellationException) {
                status = "failed"
                throw e
            } catch (e: Exception) {
                status = "failed"
                throw e
            } finally {
                metricsRegistry.incrementCounter(
                    "priestess_pipeline_messages_total",
                    mapOf("platform" to platformName, "status" to status),
                )
                metricsRegistry.recordDuration(
                    "priestess_pipeline_duration_milliseconds",
                    mapOf("platform" to platformName, "status" to status),
                    elapsedMillis(startedAtNanos),
                )
            }
        }
        synchronized(activeMessageJobs) {
            activeMessageJobs.add(job)
        }
        job.invokeOnCompletion {
            synchronized(activeMessageJobs) {
                activeMessageJobs.remove(job)
            }
        }
        return job
    }

    suspend fun drain(timeoutMillis: Long = drainTimeoutMillis): Boolean {
        shuttingDown = true
        val jobs = synchronized(activeMessageJobs) { activeMessageJobs.toList() }
        if (jobs.isEmpty()) return true
        logger.info { "Draining ${jobs.size} in-flight pipeline job(s)" }
        val completed = withTimeoutOrNull(timeoutMillis.coerceAtLeast(0)) {
            jobs.joinAll()
            true
        } ?: false
        if (!completed) {
            logger.warn { "Pipeline drain timed out after ${timeoutMillis}ms; cancelling remaining work" }
        }
        return completed
    }

    override suspend fun stop() {
        drain()
        super.stop()
    }

    private suspend fun executePipeline(ctx: PipelineContext, orderedStages: List<Stage>, stageIndex: Int) {
        if (stageIndex >= orderedStages.size) return
        if (ctx.isStopped) return

        val stage = orderedStages[stageIndex]
        val flow: Flow<Unit>? = try {
            logger.info { "[PIPELINE-04${stage.order.level}] Enter stage ${stage.order.level}:${stage.name}" }
            stage.process(ctx)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error(e) { "[PIPELINE-59${stage.order.level}] Stage '${stage.name}' failed" }
            return
        }

        if (flow == null) {
            logger.info {
                "[PIPELINE-14${stage.order.level}] Exit stage ${stage.order.level}:${stage.name}, stopped=${ctx.isStopped}"
            }
            executePipeline(ctx, orderedStages, stageIndex + 1)
        } else {
            executePipeline(ctx, orderedStages, stageIndex + 1)
            try {
                logger.info { "[PIPELINE-24${stage.order.level}] Collect post stage ${stage.order.level}:${stage.name}" }
                flow.collect {}
                logger.info { "[PIPELINE-34${stage.order.level}] Post stage complete ${stage.order.level}:${stage.name}" }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error(e) { "[PIPELINE-69${stage.order.level}] Stage '${stage.name}' post-processing failed" }
            }
        }
    }

    companion object {
        const val DEFAULT_DRAIN_TIMEOUT_MILLIS = 10_000L

        private fun buildStages(
            configCase: ConfigCase,
            conversationCase: ConversationCase,
            agentCase: AgentCase,
            contextManager: ContextManager,
            providerCase: ProviderCase,
            toolExecutor: ToolExecutor,
            toolController: ToolController,
            skillCase: SkillCase?,
            subAgentOrchestrator: SubAgentOrchestrator?,
            workspaceController: WorkspaceController?,
            personaMemoryInjector: PersonaMemoryInjector?,
            metricsRegistry: MetricsRegistry,
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
                    subAgentConfig = config.subAgents,
                    pipelineConfig = config.pipeline,
                    conversationCase = conversationCase,
                    agentCase = agentCase,
                    contextManager = contextManager,
                    subAgentOrchestrator = subAgentOrchestrator,
                    workspaceController = workspaceController,
                    personaMemoryInjector = personaMemoryInjector,
                    skillCase = skillCase,
                ),
                ProcessStage(
                    providerCase = providerCase,
                    toolExecutor = toolExecutor,
                    toolController = toolController,
                    contextManager = contextManager,
                    metricsRegistry = metricsRegistry,
                ),
                ResultDecorateStage(),
                RespondStage(),
            )
        }

        private fun elapsedMillis(startedAtNanos: Long): Long {
            return ((System.nanoTime() - startedAtNanos).coerceAtLeast(0L)) / 1_000_000L
        }
    }
}
