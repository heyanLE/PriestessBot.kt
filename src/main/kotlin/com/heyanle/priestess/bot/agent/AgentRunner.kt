package com.heyanle.priestess.bot.agent

interface AgentRunner {
    val state: AgentState
    suspend fun reset()
    suspend fun step(): AgentResponse
    suspend fun stepUntilDone(): AgentResponse
    fun isDone(): Boolean
    fun finalResponse(): AgentResponse.Final?
}
