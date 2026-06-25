package com.heyanle.priestess.bot.tool.builtin

import com.heyanle.priestess.bot.conversation.ConversationCase
import com.heyanle.priestess.bot.knowledge.KnowledgeCase
import com.heyanle.priestess.bot.memory.MemoryCase
import com.heyanle.priestess.bot.reminder.ReminderCase
import com.heyanle.priestess.bot.server.RuntimeHealthProvider
import com.heyanle.priestess.bot.tool.FunctionTool
import com.heyanle.priestess.bot.tool.ToolMetadata
import com.heyanle.priestess.bot.tool.ToolController

/**
 * Registers all built-in tools into the [ToolController].
 */
fun registerBuiltinTools(
    registry: ToolController,
    knowledgeCaseProvider: (() -> KnowledgeCase)? = null,
    healthProvider: (() -> RuntimeHealthProvider)? = null,
    conversationCaseProvider: (() -> ConversationCase)? = null,
    memoryCaseProvider: (() -> MemoryCase)? = null,
    reminderCaseProvider: (() -> ReminderCase)? = null,
) {
    fun register(tool: FunctionTool, statusReason: String? = null) {
        registry.register(tool, ToolMetadata(statusReason = statusReason))
    }

    register(ListToolsTool(registeredToolsProvider = { registry.getRegisteredTools() }))
    register(UseSkillTool())
    register(UnloadSkillTool())
    register(
        HealthCheckTool(healthProvider ?: { error("Health dependency is unavailable") }),
        statusReason = if (healthProvider == null) "Requires health dependency" else null,
    )
    register(FetchUrlTool())
    register(
        ConversationSearchTool(conversationCaseProvider ?: { error("Conversation history dependency is unavailable") }),
        statusReason = if (conversationCaseProvider == null) "Requires conversation history dependency" else null,
    )
    register(MemorySaveTool(memoryCaseProvider ?: { error("Memory dependency is unavailable") }), memoryStatusReason(memoryCaseProvider))
    register(MemoryRecallTool(memoryCaseProvider ?: { error("Memory dependency is unavailable") }), memoryStatusReason(memoryCaseProvider))
    register(MemoryDeleteTool(memoryCaseProvider ?: { error("Memory dependency is unavailable") }), memoryStatusReason(memoryCaseProvider))
    register(CreateReminderTool(reminderCaseProvider ?: { error("Reminder dependency is unavailable") }), reminderStatusReason(reminderCaseProvider))
    register(ListRemindersTool(reminderCaseProvider ?: { error("Reminder dependency is unavailable") }), reminderStatusReason(reminderCaseProvider))
    register(DeleteReminderTool(reminderCaseProvider ?: { error("Reminder dependency is unavailable") }), reminderStatusReason(reminderCaseProvider))
    register(WebSearchTool(), statusReason = "Requires search provider dependency")
    register(EarlyReplyTool())
    register(SendMessageTool())
    register(
        KnowledgeSearchTool(knowledgeCaseProvider),
        statusReason = if (knowledgeCaseProvider == null) "Requires knowledge dependency" else null,
    )
    register(
            SystemInfoTool(
                toolListProvider = { registry.getAll().map { it.schema.name } },
            ),
    )
}

private fun memoryStatusReason(provider: (() -> MemoryCase)?): String? {
    return if (provider == null) "Requires memory dependency" else null
}

private fun reminderStatusReason(provider: (() -> ReminderCase)?): String? {
    return if (provider == null) "Requires reminder dependency" else null
}
