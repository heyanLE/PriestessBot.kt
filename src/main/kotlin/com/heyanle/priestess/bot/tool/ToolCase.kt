package com.heyanle.priestess.bot.tool

class ToolCase(
    private val controller: ToolController,
) {
    fun get(name: String): FunctionTool? = controller.get(name)
    fun getAll(): List<FunctionTool> = controller.getAll()
    fun toOpenAIFormat() = controller.toOpenAIFormat()
}
