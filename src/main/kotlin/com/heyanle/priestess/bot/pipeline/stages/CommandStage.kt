package com.heyanle.priestess.bot.pipeline.stages

import com.heyanle.priestess.bot.config.CommandConfig
import com.heyanle.priestess.bot.conversation.ConversationCase
import com.heyanle.priestess.bot.pipeline.CommandCase
import com.heyanle.priestess.bot.pipeline.CommandContext
import com.heyanle.priestess.bot.pipeline.PipelineContext
import com.heyanle.priestess.bot.pipeline.PermissionDeniedMessageResolver
import com.heyanle.priestess.bot.pipeline.PermissionMessageContext
import com.heyanle.priestess.bot.pipeline.Stage
import com.heyanle.priestess.bot.pipeline.StageOrder

class CommandStage(
    private val configProvider: () -> CommandConfig,
    private val commandCase: CommandCase,
    private val conversationCase: ConversationCase,
    private val permissionDeniedMessageResolver: PermissionDeniedMessageResolver,
) : Stage {
    override val name: String = "Command"
    override val order: StageOrder = StageOrder.COMMAND

    override suspend fun process(ctx: PipelineContext) = run {
        val prefix = configProvider().prefix.trim()
        if (prefix.isEmpty()) return@run null
        val text = ctx.textContent.trim()
        if (!text.startsWith(prefix)) return@run null
        val tokens = text.removePrefix(prefix).trim().split(Regex("\\s+")).filter(String::isNotBlank)
        if (tokens.isEmpty()) return@run null
        val command = commandCase.find(tokens.first())
        if (command == null) {
            ctx.directResponse = "未知指令：${tokens.first()}"
            return@run null
        }
        if (!ctx.permissionGroup.satisfies(command.requiredPermissionGroup)) {
            ctx.directResponse = permissionDeniedMessageResolver.resolve(messageContext(ctx))
            return@run null
        }
        ctx.directResponse = command.execute(CommandContext(ctx, tokens.drop(1), conversationCase))
        null
    }

    private fun messageContext(ctx: PipelineContext): PermissionMessageContext {
        val snapshot = ctx.workspaceSnapshot
        return PermissionMessageContext(
            workspaceId = snapshot?.id.orEmpty(),
            agentName = snapshot?.agentConfigs?.firstOrNull()?.name.orEmpty(),
        )
    }
}
