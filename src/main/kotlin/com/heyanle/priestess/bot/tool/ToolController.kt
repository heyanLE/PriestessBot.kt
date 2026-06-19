package com.heyanle.priestess.bot.tool

import com.heyanle.priestess.bot.core.controller.BaseController

/**
 * Owns registered function tools and the aggregate ToolSet view.
 *
 * Built-in and dynamically added tools are kept in sync with [ToolSet], which is
 * consumed by agent runners and executors. The controller clears registered tools
 * during shutdown; use ToolCase or ToolExecutor for cross-module operations.
 */
class ToolController : BaseController("ToolController") {

    private val tools = mutableListOf<FunctionTool>()
    private val toolSet = ToolSet()

    fun register(tool: FunctionTool) {
        tools.add(tool)
        toolSet.add(tool)
    }

    fun registerAll(newTools: Collection<FunctionTool>) {
        tools.addAll(newTools)
        toolSet.addAll(newTools)
    }

    fun unregister(name: String) {
        tools.removeAll { it.schema.name == name }
        toolSet.removeByName(name)
    }

    fun get(name: String): FunctionTool? = toolSet.get(name)

    fun getAll(): List<FunctionTool> = tools.toList()

    fun getToolSet(): ToolSet = toolSet

    fun toOpenAIFormat() = toolSet.toOpenAIFormat()

    fun size(): Int = tools.size

    fun isEmpty(): Boolean = tools.isEmpty()

    fun scanAndRegister(classLoader: ClassLoader = Thread.currentThread().contextClassLoader) = Unit

    override suspend fun stop() {
        tools.clear()
        toolSet.clear()
        super.stop()
    }
}
