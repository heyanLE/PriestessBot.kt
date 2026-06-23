package com.heyanle.priestess.bot.agent.orchestration

import com.heyanle.priestess.bot.agent.AgentCase
import com.heyanle.priestess.bot.agent.AgentContext
import com.heyanle.priestess.bot.agent.AgentHooks
import com.heyanle.priestess.bot.agent.AgentResponse
import com.heyanle.priestess.bot.agent.context.ContextManager
import com.heyanle.priestess.bot.agent.runner.ReActRunner
import com.heyanle.priestess.bot.config.AgentConfig
import com.heyanle.priestess.bot.config.SubAgentConfig
import com.heyanle.priestess.bot.config.SubAgentOrchestrationConfig
import com.heyanle.priestess.bot.config.SubAgentRouteConfig
import com.heyanle.priestess.bot.provider.ProviderCase
import com.heyanle.priestess.bot.provider.model.ConversationMessage
import com.heyanle.priestess.bot.server.AgentChatEventDto
import com.heyanle.priestess.bot.tool.ToolController
import com.heyanle.priestess.bot.tool.ToolExecutor
import java.util.UUID

class SubAgentOrchestrator(
    private val agentCase: AgentCase,
    private val contextManager: ContextManager,
    private val providerCase: ProviderCase,
    private val toolExecutor: ToolExecutor,
    private val toolController: ToolController,
) {
    fun select(
        message: String,
        primaryAgent: AgentConfig,
        config: SubAgentOrchestrationConfig,
    ): SubAgentSelection {
        if (!config.enabled) {
            return SubAgentSelection(
                agentName = primaryAgent.name,
                agentConfig = primaryAgent,
                routeName = null,
                reason = "orchestration_disabled",
            )
        }

        val agents = config.agents.filter { it.enabled }.associateBy { it.name }
        val normalizedMessage = message.lowercase()
        val matchedRoute = config.routes
            .filter { it.enabled }
            .sortedWith(compareByDescending<SubAgentRouteConfig> { it.priority }.thenBy { it.name })
            .firstOrNull { route ->
                route.keywords.any { keyword ->
                    keyword.isNotBlank() && normalizedMessage.contains(keyword.lowercase())
                }
            }

        if (matchedRoute != null) {
            val target = agents[matchedRoute.targetAgentName]
            if (target != null) {
                return target.toSelection(matchedRoute.name, "keyword_match")
            }
        }

        val defaultAgent = config.defaultAgentName.takeIf { it.isNotBlank() }?.let { agents[it] }
        if (defaultAgent != null) {
            return defaultAgent.toSelection(null, "default_agent")
        }

        return SubAgentSelection(
            agentName = primaryAgent.name,
            agentConfig = primaryAgent,
            routeName = null,
            reason = "primary_agent",
        )
    }

    suspend fun run(
        message: String,
        primaryAgent: AgentConfig,
        config: SubAgentOrchestrationConfig,
        conversationId: String = "sub-agent-${UUID.randomUUID()}",
    ): SubAgentRunResult {
        val selection = select(message, primaryAgent, config)
        val events = mutableListOf<AgentChatEventDto>()

        if (message.isBlank()) {
            return SubAgentRunResult(selection, "ERROR", "Message must not be blank", events, conversationId)
        }

        val provider = providerCase.getByName(selection.agentConfig.providerName)
            ?: return SubAgentRunResult(
                selection = selection,
                status = "ERROR",
                content = "Provider '${selection.agentConfig.providerName}' is not available",
                events = events,
                conversationId = conversationId,
            )

        val agent = agentCase.createAgent(selection.agentConfig)
        val context = AgentContext(
            agent = agent,
            conversationId = conversationId,
            platform = null,
            session = null,
            messages = mutableListOf(ConversationMessage.user(message)),
            metadata = mapOf("source" to "sub-agent-orchestrator"),
        )
        val hooks = object : AgentHooks {
            override suspend fun onAgentBegin(context: AgentContext) {
                events += AgentChatEventDto(type = "agent_begin", message = "Agent started")
            }

            override suspend fun onToolStart(context: AgentContext, toolName: String, arguments: String) {
                events += AgentChatEventDto(type = "tool_start", message = arguments, toolName = toolName)
            }

            override suspend fun onToolEnd(context: AgentContext, toolName: String, result: AgentResponse.ToolExecuted) {
                events += AgentChatEventDto(
                    type = "tool_end",
                    message = if (result.toolResult.success) result.toolResult.output else result.toolResult.error,
                    toolName = toolName,
                    success = result.toolResult.success,
                )
            }

            override suspend fun onAgentDone(context: AgentContext, response: AgentResponse.Final) {
                events += AgentChatEventDto(type = "agent_done", message = "Agent completed")
            }

            override suspend fun onAgentError(context: AgentContext, error: AgentResponse.Error) {
                events += AgentChatEventDto(type = "agent_error", message = error.message, success = false)
            }
        }

        val runner = ReActRunner(
            context = context,
            provider = provider,
            toolExecutor = toolExecutor,
            toolRegistry = toolController,
            contextManager = contextManager,
            hooks = hooks,
        )

        return when (val response = runner.stepUntilDone()) {
            is AgentResponse.Final -> SubAgentRunResult(selection, "FINAL", response.content, events, conversationId)
            is AgentResponse.Error -> SubAgentRunResult(selection, "ERROR", response.message, events, conversationId)
            else -> SubAgentRunResult(
                selection,
                "ERROR",
                "Agent ended before producing a final response",
                events,
                conversationId,
            )
        }
    }

    private fun SubAgentConfig.toSelection(routeName: String?, reason: String): SubAgentSelection {
        return SubAgentSelection(
            agentName = name,
            agentConfig = agent.copy(name = name),
            routeName = routeName,
            reason = reason,
        )
    }
}

data class SubAgentSelection(
    val agentName: String,
    val agentConfig: AgentConfig,
    val routeName: String?,
    val reason: String,
)

data class SubAgentRunResult(
    val selection: SubAgentSelection,
    val status: String,
    val content: String,
    val events: List<AgentChatEventDto>,
    val conversationId: String,
)
