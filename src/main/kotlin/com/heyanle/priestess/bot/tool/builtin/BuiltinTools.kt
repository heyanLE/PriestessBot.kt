package com.heyanle.priestess.bot.tool.builtin

import com.heyanle.priestess.bot.tool.ToolController

/**
 * Registers all built-in tools into the [ToolController].
 */
fun registerBuiltinTools(registry: ToolController) {
    registry.registerAll(
        listOf(
            WebSearchTool(),
            EarlyReplyTool(),
            SendMessageTool(),
            SystemInfoTool(
                toolListProvider = { registry.getAll().map { it.schema.name } }
            ),
        )
    )
}
