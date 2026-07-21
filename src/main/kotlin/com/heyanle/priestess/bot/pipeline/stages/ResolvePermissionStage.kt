package com.heyanle.priestess.bot.pipeline.stages

import com.heyanle.priestess.bot.pipeline.PipelineContext
import com.heyanle.priestess.bot.pipeline.PermissionResolver
import com.heyanle.priestess.bot.pipeline.Stage
import com.heyanle.priestess.bot.pipeline.StageOrder

class ResolvePermissionStage(
    private val permissionResolver: PermissionResolver,
) : Stage {
    override val name: String = "ResolvePermission"
    override val order: StageOrder = StageOrder.RESOLVE_PERMISSION

    override suspend fun process(ctx: PipelineContext) = run {
        ctx.permissionGroup = permissionResolver.resolve(ctx.senderId)
        null
    }
}
