package com.heyanle.priestess.bot.core.db

class DatabaseCase(
    private val controller: DatabaseController,
) {
    fun <T> execute(block: () -> T): T = controller.execute(block)
}
