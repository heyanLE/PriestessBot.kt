package com.heyanle.priestess.bot.agent.runner

import com.heyanle.priestess.bot.agent.*
import com.heyanle.priestess.bot.agent.context.ContextManager
import com.heyanle.priestess.bot.provider.ChatProvider
import com.heyanle.priestess.bot.provider.model.ConversationMessage
import com.heyanle.priestess.bot.provider.model.LLMRequest
import com.heyanle.priestess.bot.tool.AgentToolContext
import com.heyanle.priestess.bot.tool.FunctionTool
import com.heyanle.priestess.bot.tool.ToolExecutor
import com.heyanle.priestess.bot.tool.ToolController
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * ReAct (Reasoning + Acting) 执行器。
 *
 * 核心循环：
 * 1. 检查上下文 → ContextManager.compress()
 * 2. 调 LLM → provider.textChat(messages, tools)
 * 3. 有 ToolCall → ToolExecutor.execute() → 结果注入消息 → 回到步骤 1
 * 4. 无 ToolCall → Final 回答
 * 5. 超 maxSteps → Error
 */
class ReActRunner(
    private val context: AgentContext,
    private val provider: ChatProvider,
    private val toolExecutor: ToolExecutor,
    private val toolRegistry: ToolController,
    private val contextManager: ContextManager,
    private val hooks: AgentHooks? = null,
) : AgentRunner {

    private val mutex = Mutex()

    override var state: AgentState = AgentState.IDLE
        private set

    private var currentStep: Int = 0
    private var finalResponseCache: AgentResponse.Final? = null
    private var systemMessage: ConversationMessage? = null

    private fun refreshSystemMessage() {
        val next = ConversationMessage.system(buildSystemPrompt())
        val current = systemMessage
        if (current == null) {
            systemMessage = next
            context.messages.add(0, next)
            return
        }
        if (current.content == next.content) return
        val index = context.messages.indexOf(current)
        if (index >= 0) {
            context.messages[index] = next
        } else {
            context.messages.add(0, next)
        }
        systemMessage = next
    }

    override suspend fun reset() = mutex.withLock {
        state = AgentState.IDLE
        currentStep = 0
        finalResponseCache = null
        systemMessage = null
    }

    override suspend fun step(): AgentResponse = mutex.withLock {
        doStep()
    }

    override suspend fun stepUntilDone(): AgentResponse = mutex.withLock {
        refreshSystemMessage()
        hooks?.onAgentBegin(context)
        state = AgentState.RUNNING

        while (currentStep < context.agent.maxSteps && state == AgentState.RUNNING) {
            val response = doStep()
            when (response) {
                is AgentResponse.Final -> {
                    state = AgentState.DONE
                    finalResponseCache = response
                    return@withLock response
                }
                is AgentResponse.Error -> {
                    state = AgentState.ERROR
                    hooks?.onAgentError(context, response)
                    return@withLock response
                }
                is AgentResponse.ToolExecuted -> currentStep++
                is AgentResponse.Thinking -> currentStep++
            }
        }

        if (state == AgentState.RUNNING) {
            val error = AgentResponse.Error("Exceeded maximum steps (${context.agent.maxSteps})")
            state = AgentState.ERROR
            hooks?.onAgentError(context, error)
            return@withLock error
        }

        return@withLock finalResponseCache
            ?: AgentResponse.Error("Agent ended in unexpected state: $state")
    }

    /**
     * 核心单步执行逻辑，不加锁，调用者需确保在 mutex.withLock 内调用。
     */
    private suspend fun doStep(): AgentResponse {
        if (state == AgentState.DONE) {
            return finalResponseCache ?: AgentResponse.Error("Agent is done but no final response cached")
        }
        if (state == AgentState.ERROR) {
            return AgentResponse.Error("Agent is in error state, cannot continue")
        }

        if (state == AgentState.IDLE) {
            refreshSystemMessage()
            state = AgentState.RUNNING
        }

        return try {
            refreshSystemMessage()
            val compressed = contextManager.compress(
                agent = context.agent,
                messages = context.messages,
                systemMessage = systemMessage,
            )

            val tools = workspaceScopedTools().map { it.schema.toOpenAIFormat() }
            val request = LLMRequest(
                model = context.agent.model,
                messages = compressed.toList(),
                tools = tools,
            )

            val response = provider.textChat(request)

            if (response.hasToolCalls()) {
                handleToolCalls(response)
            } else {
                handleFinalResponse(response)
            }
        } catch (e: Exception) {
            val error = AgentResponse.Error(
                message = e.message ?: "Unknown error during agent step",
                cause = e,
            )
            state = AgentState.ERROR
            hooks?.onAgentError(context, error)
            error
        }
    }

    override fun isDone(): Boolean = state == AgentState.DONE || state == AgentState.ERROR

    override fun finalResponse(): AgentResponse.Final? = finalResponseCache

    private suspend fun handleToolCalls(
        response: com.heyanle.priestess.bot.provider.model.LLMResponse
    ): AgentResponse {
        val toolCalls = response.toolCalls

        val assistantMsg = ConversationMessage.assistant(
            content = response.content,
            toolCalls = toolCalls,
        )
        context.messages.add(assistantMsg)

        val toolInputs = toolCalls.map { tc ->
            Triple(tc.id, tc.name, tc.arguments)
        }

        for (tc in toolCalls) {
            hooks?.onToolStart(context, tc.name, tc.arguments)
        }

        val toolContext = buildToolContext()
        val results = try {
            toolExecutor.executeBatch(
                context = toolContext,
                toolCalls = toolInputs,
                timeoutMillis = context.agent.toolTimeoutMs,
            )
        } catch (e: Exception) {
            toolCalls.associate { tc ->
                tc.id to com.heyanle.priestess.bot.tool.ToolResult(
                    success = false,
                    error = "Tool execution failed: ${e.message}",
                    errorCode = "TOOL_EXECUTION_FAILED",
                )
            }
        }

        for (tc in toolCalls) {
            val result = results[tc.id] ?: continue
            val toolMsg = ConversationMessage.tool(
                toolCallId = tc.id,
                name = tc.name,
                content = if (result.success) result.output else result.error,
            )
            context.messages.add(toolMsg)

            val toolResultResponse = AgentResponse.ToolExecuted(
                toolCallId = tc.id,
                toolName = tc.name,
                toolResult = result,
            )
            hooks?.onToolEnd(context, tc.name, toolResultResponse)
        }

        return AgentResponse.Thinking(response.content)
    }

    private suspend fun handleFinalResponse(
        response: com.heyanle.priestess.bot.provider.model.LLMResponse
    ): AgentResponse {
        val assistantMsg = ConversationMessage.assistant(content = response.content)
        context.messages.add(assistantMsg)

        val final = AgentResponse.Final(response.content)
        finalResponseCache = final
        state = AgentState.DONE
        hooks?.onAgentDone(context, final)
        return final
    }

    private fun buildToolContext(): AgentToolContext {
        return AgentToolContext(
            platform = context.platform,
            session = context.session,
            agentName = context.agent.name,
            model = context.agent.model,
            metadata = context.metadata,
            scopedTools = context.scopedTools,
            skillState = context.skillState,
        )
    }

    private fun workspaceScopedTools(): List<com.heyanle.priestess.bot.tool.FunctionTool> {
        val allowed = workspaceToolNames()
        val tools = (toolRegistry.getAll() + context.scopedTools)
            .distinctBy { it.schema.name }
        return if (allowed == null) {
            tools
        } else {
            tools.filter { it.schema.name in allowed }
        }
    }

    private fun workspaceToolNames(): Set<String>? {
        val raw = context.metadata["workspace_tool_names"]
            ?: context.metadata["workspaceToolNames"]
            ?: return null
        return raw.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    private fun buildSystemPrompt(): String {
        val toolBlock = renderToolBlock(workspaceScopedTools())
        val skillBlock = renderSkillBlock()
        return buildString {
            append("## Platform")
            append("\n")
            append("Platform: ")
            append(context.platform?.metadata?.name ?: context.session?.platformName ?: "unknown")
            context.session?.let { session ->
                append("\nSession: ")
                append(session.type.name.lowercase())
                append(" / ")
                append(session.id)
            }
            append("\n\n## Role Document")
            append("\n")
            append(context.agent.instructions.trim())
            append("\n\n## Tools")
            append("\n")
            append(toolBlock)
            append("\n\n## Loaded Skills")
            append("\n")
            append(skillBlock)
        }.trim()
    }

    private fun renderToolBlock(tools: List<FunctionTool>): String {
        if (tools.isEmpty()) return "No tools are currently available."
        return buildString {
            append("Available tools:")
            tools.forEachIndexed { index, tool ->
                append("\n")
                append(index + 1)
                append(". ")
                append(tool.schema.name)
                append(" - ")
                append(tool.schema.description)
            }
        }
    }

    private fun renderSkillBlock(): String {
        return buildString {
            append("Available skills: ")
            append(context.skillState.availableNames.joinToString(", ").ifBlank { "none" })
            append("\n")
            append(context.skillState.renderLoadedSkillBlock())
        }
    }
}
