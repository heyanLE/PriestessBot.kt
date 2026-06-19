package com.heyanle.priestess.bot.pipeline.stages

import com.heyanle.priestess.bot.agent.AgentHooks
import com.heyanle.priestess.bot.agent.AgentResponse
import com.heyanle.priestess.bot.agent.context.ContextManager
import com.heyanle.priestess.bot.agent.runner.ReActRunner
import com.heyanle.priestess.bot.pipeline.PipelineContext
import com.heyanle.priestess.bot.pipeline.Stage
import com.heyanle.priestess.bot.pipeline.StageOrder
import com.heyanle.priestess.bot.provider.ProviderCase
import com.heyanle.priestess.bot.provider.model.ConversationMessage
import com.heyanle.priestess.bot.tool.ToolController
import com.heyanle.priestess.bot.tool.ToolExecutor
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ProcessStage(
    private val providerCase: ProviderCase,
    private val toolExecutor: ToolExecutor,
    private val toolController: ToolController,
    private val contextManager: ContextManager,
    private val hooks: AgentHooks? = null,
) : Stage {

    private val logger = KotlinLogging.logger {}

    override val name = "Process"
    override val order = StageOrder.PROCESS

    override suspend fun process(ctx: PipelineContext): Flow<Unit>? {
        val agentContext = ctx.agentContext
        if (agentContext == null) {
            logger.error { "[PIPELINE-991] AgentContext is null, cannot execute agent" }
            ctx.agentResponse = AgentResponse.Error("AgentContext not initialized")
            return null
        }

        val agent = agentContext.agent
        val provider = providerCase.getByName(agent.model)
            ?: providerCase.getAll().firstOrNull()

        if (provider == null) {
            logger.error { "[PIPELINE-992] No provider available for model '${agent.model}'" }
            ctx.agentResponse = AgentResponse.Error("No LLM provider available")
            return null
        }

        agentContext.messages.add(ConversationMessage.user(ctx.textContent))
        logger.info {
            "[PIPELINE-210] Process selected provider=${provider.metadata.name}, agent=${agent.name}, " +
                "model=${agent.model}, messages=${agentContext.messages.size}, tools=${toolController.size()}"
        }

        val runner = ReActRunner(
            context = agentContext,
            provider = provider,
            toolExecutor = toolExecutor,
            toolRegistry = toolController,
            contextManager = contextManager,
            hooks = hooks,
        )

        logger.info { "[PIPELINE-220] Process executing ReAct loop agent=${agent.name}, model=${agent.model}" }
        val response = runner.stepUntilDone()
        ctx.agentResponse = response

        when (response) {
            is AgentResponse.Final -> {
                logger.info { "[PIPELINE-229] Process final response length=${response.content.length}" }
            }
            is AgentResponse.Error -> {
                logger.info { "[PIPELINE-298] Process agent error: ${response.message}" }
            }
        else -> {
            logger.info { "[PIPELINE-299] Process ended with response type: ${response::class.simpleName}" }
        }
    }

        return flow {
            emit(Unit)
        }
    }
}
