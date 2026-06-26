package com.heyanle.priestess.bot

import com.heyanle.priestess.bot.config.ConfigCase
import com.heyanle.priestess.bot.core.db.DatabaseCase
import com.heyanle.priestess.bot.observability.ObservabilityCase
import com.heyanle.priestess.bot.pipeline.PipelineCase
import com.heyanle.priestess.bot.platform.PlatformCase
import com.heyanle.priestess.bot.plugin.PluginCase
import com.heyanle.priestess.bot.provider.ProviderCase
import com.heyanle.priestess.bot.server.ServerCase
import com.heyanle.priestess.bot.skill.SkillCase
import com.heyanle.priestess.bot.tool.ToolCase
import com.heyanle.priestess.bot.workspace.WorkspaceCase
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * 运行时编排器，负责启动服务并按确定顺序停止各模块生命周期入口。
 */
class PriestessRuntime private constructor(
    private val startAction: () -> Unit,
    private val stopSteps: List<StopStep>,
) {
    private val logger = KotlinLogging.logger("PriestessRuntime")

    constructor(
        platformCase: PlatformCase,
        pipelineCase: PipelineCase,
        serverCase: ServerCase,
        pluginCase: PluginCase,
        providerCase: ProviderCase,
        toolCase: ToolCase,
        skillCase: SkillCase,
        workspaceCase: WorkspaceCase,
        observabilityCase: ObservabilityCase,
        databaseCase: DatabaseCase,
        configCase: ConfigCase,
        pipelineDrainTimeoutMillis: Long = PipelineCase.DEFAULT_DRAIN_TIMEOUT_MILLIS,
    ) : this(
        startAction = {
            platformCase.start()
            serverCase.start()
        },
        stopSteps = listOf(
            StopStep("platforms") { platformCase.stop() },
            StopStep("pipeline") {
                pipelineCase.drain(pipelineDrainTimeoutMillis)
                pipelineCase.stop()
            },
            StopStep("server") { serverCase.stop() },
            StopStep("plugins") { pluginCase.stop() },
            StopStep("providers") { providerCase.stop() },
            StopStep("tools") { toolCase.stop() },
            StopStep("skills") { skillCase.stop() },
            StopStep("workspace") { workspaceCase.stop() },
            StopStep("observability") { observabilityCase.stop() },
            StopStep("database") { databaseCase.stop() },
            StopStep("config") { configCase.stop() },
        ),
    )

    internal constructor(
        stopSteps: List<Pair<String, suspend () -> Unit>>,
    ) : this(
        startAction = {},
        stopSteps = stopSteps.map { (name, block) -> StopStep(name, block) },
    )

    fun start() {
        startAction()
    }

    suspend fun stop() {
        logger.info { "Priestess runtime shutdown starting..." }
        for (step in stopSteps) {
            runStop(step.name, step.block)
        }
        logger.info { "Priestess runtime shutdown complete." }
    }

    private suspend fun runStop(name: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            logger.error(e) { "Failed to stop $name during runtime shutdown" }
        }
    }

    /**
     * 停止步骤，保存步骤名称和实际的挂起停止动作。
     */
    internal data class StopStep(
        val name: String,
        val block: suspend () -> Unit,
    )
}
