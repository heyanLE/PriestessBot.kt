package com.heyanle.priestess.bot.pipeline.stages

import com.heyanle.priestess.bot.config.ConfigCase
import com.heyanle.priestess.bot.pipeline.PipelineContext
import com.heyanle.priestess.bot.pipeline.Stage
import com.heyanle.priestess.bot.pipeline.StageOrder
import com.heyanle.priestess.bot.workspace.WorkspaceCase

class PrepareWorkspaceStage(
    private val configCase: ConfigCase,
    private val workspaceCase: WorkspaceCase? = null,
) : Stage {
    override val name = "PrepareWorkspace"
    override val order = StageOrder.PREPARE_WORKSPACE

    override suspend fun process(ctx: PipelineContext) = workspaceCase?.let { case ->
        val config = configCase.current()
        val defaultDir = config.workspace.defaultDir
        val messageDir = ctx.event.session.metadata["workspaceDir"]
        when {
            !messageDir.isNullOrBlank() -> {
                ctx.pinWorkspace(case.prepare(messageDir, "message workspace_dir"))
            }
            !defaultDir.isNullOrBlank() -> {
                ctx.pinWorkspace(case.prepare(defaultDir, "config default workspace dir"))
            }
            else -> {
                System.err.println(
                    "[PIPELINE-107] PrepareWorkspace: no workspace dir configured for " +
                        "platform=${ctx.event.platform.metadata.name}, " +
                        "session=${ctx.event.session.id}; stopping pipeline"
                )
                ctx.event.stopPropagation()
            }
        }
        null
    }
}
