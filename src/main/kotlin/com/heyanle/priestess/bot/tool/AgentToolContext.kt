package com.heyanle.priestess.bot.tool

import com.heyanle.priestess.bot.platform.MessageSession
import com.heyanle.priestess.bot.platform.Platform

/**
 * Context passed to tool execution.
 * Provides access to platform, session, and agent state.
 *
 * This is a lightweight context designed for tool use. The full AgentContext
 * in the agent loop is richer; tools only need this subset.
 */
data class AgentToolContext(
    /**
     * The current platform instance for sending messages.
     */
    val platform: Platform? = null,

    /**
     * The current message session.
     */
    val session: MessageSession? = null,

    /**
     * The name of the current agent.
     */
    val agentName: String = "",

    /**
     * The model being used.
     */
    val model: String = "",

    /**
     * Additional metadata that tools might need.
     */
    val metadata: Map<String, String> = emptyMap(),
)
