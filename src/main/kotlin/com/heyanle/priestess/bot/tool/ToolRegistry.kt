package com.heyanle.priestess.bot.tool

import com.heyanle.priestess.bot.core.lifecycle.LifecycleAware

/**
 * Global registry for all tools in the system.
 *
 * Tools can be registered:
 * - By annotation scanning (classes annotated with [RegisterTool])
 * - Manually via [register] (e.g., MCP tools loaded at runtime)
 * - Via bulk registration from built-in tool modules
 *
 * The registry is a [LifecycleAware] singleton, started during CoreLifecycle.
 */
class ToolRegistry : LifecycleAware {

    private val tools = mutableListOf<FunctionTool>()
    private val toolSet = ToolSet()

    /**
     * Register a single tool.
     */
    fun register(tool: FunctionTool) {
        tools.add(tool)
        toolSet.add(tool)
    }

    /**
     * Register multiple tools.
     */
    fun registerAll(newTools: Collection<FunctionTool>) {
        tools.addAll(newTools)
        toolSet.addAll(newTools)
    }

    /**
     * Unregister a tool by name.
     */
    fun unregister(name: String) {
        tools.removeAll { it.schema.name == name }
        toolSet.removeByName(name)
    }

    /**
     * Get a tool by name.
     */
    fun get(name: String): FunctionTool? {
        return toolSet.get(name)
    }

    /**
     * Get all registered tools.
     */
    fun getAll(): List<FunctionTool> = tools.toList()

    /**
     * Get the ToolSet containing all registered tools.
     */
    fun getToolSet(): ToolSet = toolSet

    /**
     * Get tools in OpenAI format for LLM requests.
     */
    fun toOpenAIFormat() = toolSet.toOpenAIFormat()

    /**
     * Get the count of registered tools.
     */
    fun size(): Int = tools.size

    /**
     * Check if any tools are registered.
     */
    fun isEmpty(): Boolean = tools.isEmpty()

    /**
     * Scan and register tools annotated with [RegisterTool].
     *
     * This uses classpath scanning to find all classes annotated with
     * [RegisterTool] and instantiates them via reflection.
     *
     * @param classLoader The class loader to scan. Defaults to the current thread's context class loader.
     */
    fun scanAndRegister(classLoader: ClassLoader = Thread.currentThread().contextClassLoader) {
        // In a real implementation, this would use a classpath scanner
        // like ClassGraph or Reflections. For v1, tools are registered
        // manually via registerBuiltinTools() in ToolModule.
        //
        // This method is a placeholder for future annotation-based discovery.
    }

    override suspend fun start() {
        // Registry is ready after construction.
        // Annotation scanning could happen here.
    }

    override suspend fun stop() {
        tools.clear()
        toolSet.clear()
    }
}
