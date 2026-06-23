package com.heyanle.priestess.bot.tool.builtin

import com.heyanle.priestess.bot.knowledge.KnowledgeCase
import com.heyanle.priestess.bot.tool.ToolController

/**
 * Registers all built-in tools into the [ToolController].
 */
fun registerBuiltinTools(
    registry: ToolController,
    knowledgeCaseProvider: (() -> KnowledgeCase)? = null,
) {
    registry.registerAll(
        listOf(
            WebSearchTool(),
            EarlyReplyTool(),
            SendMessageTool(),
            KnowledgeSearchTool(knowledgeCaseProvider),
            SystemInfoTool(
                toolListProvider = { registry.getAll().map { it.schema.name } }
            ),
        )
    )
}
