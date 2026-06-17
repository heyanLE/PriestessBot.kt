package com.heyanle.priestess.bot.pipeline.stages

import com.heyanle.priestess.bot.agent.AgentContext
import com.heyanle.priestess.bot.agent.AgentHooks
import com.heyanle.priestess.bot.agent.AgentResponse
import com.heyanle.priestess.bot.agent.AgentRunner
import com.heyanle.priestess.bot.agent.context.ContextManager
import com.heyanle.priestess.bot.agent.runner.ReActRunner
import com.heyanle.priestess.bot.pipeline.PipelineContext
import com.heyanle.priestess.bot.pipeline.Stage
import com.heyanle.priestess.bot.pipeline.StageOrder
import com.heyanle.priestess.bot.provider.ChatProvider
import com.heyanle.priestess.bot.provider.ProviderManager
import com.heyanle.priestess.bot.tool.ToolExecutor
import com.heyanle.priestess.bot.tool.ToolRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Agent 执行阶段（洋葱模型内层）。
 *
 * **前置逻辑**：创建 [ReActRunner] 实例并调用 [AgentRunner.stepUntilDone]
 * **后置逻辑**：将 Agent 最终响应存入 [PipelineContext.agentResponse]，清理 Runner
 *
 * 洋葱模型：创建 Runner → 执行 ReAct 循环 → 收集最终响应
 */
class ProcessStage(
    private val providerManager: ProviderManager,
    private val toolExecutor: ToolExecutor,
    private val toolRegistry: ToolRegistry,
    private val contextManager: ContextManager,
    private val hooks: AgentHooks? = null,
) : Stage {

    override val name = "Process"
    override val order = StageOrder.PROCESS

    override suspend fun process(ctx: PipelineContext): Flow<Unit> = flow {
        // === 前置逻辑：创建 ReActRunner 并执行 ===

        val agentContext = ctx.agentContext
        if (agentContext == null) {
            System.err.println("[Process] AgentContext is null, cannot execute agent")
            ctx.agentResponse = AgentResponse.Error("AgentContext not initialized")
            return@flow
        }

        // 从 AgentContext 中获取 agent 配置的 model，从 ProviderManager 获取对应 provider
        val agent = agentContext.agent
        val providerConfig = com.heyanle.priestess.bot.core.config.ProviderConfig(
            name = agent.model,
            type = agent.model,
            model = agent.model,
        )
        val provider = providerManager.getByName(agent.model)
            ?: providerManager.getAll().firstOrNull()

        if (provider == null) {
            System.err.println("[Process] No provider available for model '${agent.model}'")
            ctx.agentResponse = AgentResponse.Error("No LLM provider available")
            return@flow
        }

        // 注入用户消息到 AgentContext
        agentContext.messages.add(
            com.heyanle.priestess.bot.provider.model.ConversationMessage.user(ctx.textContent)
        )

        // 创建 ReActRunner
        val runner = ReActRunner(
            context = agentContext,
            provider = provider,
            toolExecutor = toolExecutor,
            toolRegistry = toolRegistry,
            contextManager = contextManager,
            hooks = hooks,
        )

        // 执行 ReAct 循环
        log("Executing ReAct loop for agent='${agent.name}', model='${agent.model}'")
        val response = runner.stepUntilDone()

        ctx.agentResponse = response

        // === 后置逻辑：清理 ===
        when (response) {
            is AgentResponse.Final -> {
                log("Agent completed successfully, response length=${response.content.length}")
            }
            is AgentResponse.Error -> {
                log("Agent error: ${response.message}")
            }
            else -> {
                log("Agent ended with unexpected response type: ${response::class.simpleName}")
            }
        }

        emit(Unit)
    }

    private fun log(message: String) {
        println("[Process] $message")
    }
}
