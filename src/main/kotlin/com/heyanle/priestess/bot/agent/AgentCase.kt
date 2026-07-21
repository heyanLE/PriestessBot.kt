package com.heyanle.priestess.bot.agent

import com.heyanle.priestess.bot.agent.context.ContextManager
import com.heyanle.priestess.bot.agent.context.TokenCounter
import com.heyanle.priestess.bot.agent.runner.ReActRunner
import com.heyanle.priestess.bot.config.AgentConfig
import com.heyanle.priestess.bot.provider.ChatProvider
import com.heyanle.priestess.bot.tool.ToolCase
import com.heyanle.priestess.bot.tool.ToolResultOverflowStore

/**
 * Agent 模块门面，向外提供运行时 Agent 创建和执行能力。
 */
class AgentCase(
    private val controller: AgentController = AgentController(),
    private val contextManager: ContextManager = ContextManager(TokenCounter()),
    private val overflowStore: ToolResultOverflowStore = ToolResultOverflowStore(),
) {
    fun createAgent(config: AgentConfig): Agent {
        return controller.createAgent(config)
    }

    suspend fun runWithProvider(
        context: AgentContext,
        provider: ChatProvider,
        toolCase: ToolCase,
        hooks: AgentHooks? = null,
    ): AgentResponse {
        return ReActRunner(
            context = context,
            provider = provider,
            toolCase = toolCase,
            contextManager = contextManager,
            overflowStore = overflowStore,
            hooks = hooks,
        ).stepUntilDone()
    }
}
