package com.heyanle.priestess.bot

import com.heyanle.priestess.bot.config.ConfigController
import com.heyanle.priestess.bot.core.db.DatabaseController
import com.heyanle.priestess.bot.pipeline.PipelineController
import com.heyanle.priestess.bot.platform.PlatformController
import com.heyanle.priestess.bot.plugin.PluginManager
import com.heyanle.priestess.bot.provider.ProviderController
import com.heyanle.priestess.bot.server.PriestessBotServer
import com.heyanle.priestess.bot.tool.ToolController
import io.github.oshai.kotlinlogging.KotlinLogging

class PriestessRuntime private constructor(
    private val startAction: () -> Unit,
    private val stopSteps: List<StopStep>,
) {
    private val logger = KotlinLogging.logger("PriestessRuntime")

    constructor(
        platformController: PlatformController,
        pipelineController: PipelineController,
        server: PriestessBotServer,
        pluginManager: PluginManager,
        providerController: ProviderController,
        toolController: ToolController,
        databaseController: DatabaseController,
        configController: ConfigController,
        pipelineDrainTimeoutMillis: Long = PipelineController.DEFAULT_DRAIN_TIMEOUT_MILLIS,
    ) : this(
        startAction = { server.start() },
        stopSteps = listOf(
            StopStep("platforms") { platformController.stop() },
            StopStep("pipeline") {
                pipelineController.drain(pipelineDrainTimeoutMillis)
                pipelineController.stop()
            },
            StopStep("server") { server.stop() },
            StopStep("plugins") { pluginManager.stop() },
            StopStep("providers") { providerController.stop() },
            StopStep("tools") { toolController.stop() },
            StopStep("database") { databaseController.stop() },
            StopStep("config") { configController.stop() },
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

    internal data class StopStep(
        val name: String,
        val block: suspend () -> Unit,
    )
}
