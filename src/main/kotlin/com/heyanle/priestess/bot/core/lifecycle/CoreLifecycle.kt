package com.heyanle.priestess.bot.core.lifecycle

import java.util.concurrent.CopyOnWriteArrayList

interface LifecycleAware {
    suspend fun start()
    suspend fun stop()
}

class CoreLifecycle(
    private val componentsProvider: Lazy<List<LifecycleAware>>,
) {
    private val startedComponents = CopyOnWriteArrayList<LifecycleAware>()

    suspend fun start() {
        val components = componentsProvider.value
        for (component in components) {
            try {
                component.start()
                startedComponents.add(component)
            } catch (e: Exception) {
                System.err.println(
                    "[CoreLifecycle] ERROR: Failed to start ${component::class.simpleName}: ${e.message}"
                )
                for (started in startedComponents.reversed()) {
                    try {
                        started.stop()
                    } catch (rollbackEx: Exception) {
                        System.err.println(
                            "[CoreLifecycle] ERROR: Failed to rollback stop ${started::class.simpleName}: ${rollbackEx.message}"
                        )
                    }
                }
                startedComponents.clear()
                throw RuntimeException(
                    "Failed to start component ${component::class.simpleName}, rolled back ${startedComponents.size} component(s)",
                    e
                )
            }
        }
    }

    suspend fun stop() {
        for (component in startedComponents.reversed()) {
            try {
                component.stop()
            } catch (e: Exception) {
                System.err.println(
                    "[CoreLifecycle] ERROR: Failed to stop ${component::class.simpleName}: ${e.message}"
                )
            }
        }
        startedComponents.clear()
    }
}
