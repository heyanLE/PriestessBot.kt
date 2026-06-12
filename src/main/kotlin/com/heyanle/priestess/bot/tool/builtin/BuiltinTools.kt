package com.heyanle.priestess.bot.tool.builtin

import com.heyanle.priestess.bot.tool.ToolRegistry

/**
 * Registers all built-in tools into the [ToolRegistry].
 */
fun registerBuiltinTools(registry: ToolRegistry) {
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
