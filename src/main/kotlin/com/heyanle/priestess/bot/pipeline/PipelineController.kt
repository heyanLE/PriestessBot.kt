package com.heyanle.priestess.bot.pipeline

import com.heyanle.priestess.bot.agent.AgentCase
import com.heyanle.priestess.bot.agent.orchestration.SubAgentOrchestrator
import com.heyanle.priestess.bot.config.ConfigCase
import com.heyanle.priestess.bot.conversation.ConversationCase
import com.heyanle.priestess.bot.core.controller.BaseController
import com.heyanle.priestess.bot.observability.ObservabilityCase
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
import com.heyanle.priestess.bot.tool.ToolCase
import com.heyanle.priestess.bot.workspace.WorkspaceCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 消息流水线控制器，负责按固定顺序组织阶段并承接系统生命周期。
 *
 * 阶段组合保留在控制器内部，外部只通过 PipelineCase 投递平台消息。
 */
class PipelineController private constructor(
    private val stageProvider: () -> List<Stage>,
    private val observabilityCase: ObservabilityCase = ObservabilityCase.standalone(),
    private val drainTimeoutMillis: Long = DEFAULT_DRAIN_TIMEOUT_MILLIS,
) : BaseController("PipelineController") {

    private val activeMessageJobs = mutableSetOf<Job>()
    @Volatile
    private var shuttingDown = false

    constructor(
        configCase: ConfigCase,
        conversationCase: ConversationCase,
        agentCase: AgentCase,
        providerCase: ProviderCase,
        toolCase: ToolCase,
        skillCase: SkillCase? = null,
        subAgentOrchestrator: SubAgentOrchestrator? = null,
        workspaceCase: WorkspaceCase? = null,
        personaMemoryInjector: PersonaMemoryInjector? = null,
        observabilityCase: ObservabilityCase = ObservabilityCase.standalone(),
        drainTimeoutMillis: Long = DEFAULT_DRAIN_TIMEOUT_MILLIS,
    ) : this({
        buildStages(
            configCase = configCase,
            conversationCase = conversationCase,
            agentCase = agentCase,
            providerCase = providerCase,
            toolCase = toolCase,
            skillCase = skillCase,
            subAgentOrchestrator = subAgentOrchestrator,
            workspaceCase = workspaceCase,
            personaMemoryInjector = personaMemoryInjector,
            observabilityCase = observabilityCase,
        )
    }, observabilityCase, drainTimeoutMillis)

    internal constructor(
        testStages: List<Stage>,
        @Suppress("UNUSED_PARAMETER") testOnly: Unit,
        observabilityCase: ObservabilityCase = ObservabilityCase.standalone(),
        drainTimeoutMillis: Long = DEFAULT_DRAIN_TIMEOUT_MILLIS,
    ) : this({ testStages }, observabilityCase, drainTimeoutMillis)

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
                observabilityCase.recordPipelineMessage(platformName, status, elapsedMillis(startedAtNanos))
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
            providerCase: ProviderCase,
            toolCase: ToolCase,
            skillCase: SkillCase?,
            subAgentOrchestrator: SubAgentOrchestrator?,
            workspaceCase: WorkspaceCase?,
            personaMemoryInjector: PersonaMemoryInjector?,
            observabilityCase: ObservabilityCase,
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
                    subAgentOrchestrator = subAgentOrchestrator,
                    workspaceCase = workspaceCase,
                    personaMemoryInjector = personaMemoryInjector,
                    skillCase = skillCase,
                ),
                ProcessStage(
                    agentCase = agentCase,
                    providerCase = providerCase,
                    toolCase = toolCase,
                    observabilityCase = observabilityCase,
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
