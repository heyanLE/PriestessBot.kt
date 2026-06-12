package com.heyanle.priestess.bot.platform

import com.heyanle.priestess.bot.core.config.PlatformConfig
import com.heyanle.priestess.bot.core.lifecycle.LifecycleAware
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PlatformManager(
    private val platformConfigs: List<PlatformConfig> = emptyList(),
) : LifecycleAware {

    private val mutex = Mutex()
    private val platforms = mutableListOf<Platform>()
    private val jobs = mutableMapOf<String, Job>()

    fun get(name: String): Platform? {
        return runBlocking {
            mutex.withLock {
                platforms.find { it.metadata.name == name }
            }
        }
    }

    fun getAll(): List<Platform> {
        return runBlocking {
            mutex.withLock {
                platforms.toList()
            }
        }
    }

    fun getRunning(): List<Platform> {
        return runBlocking {
            mutex.withLock {
                platforms.filter {
                    jobs.containsKey(it.metadata.name) && jobs[it.metadata.name]?.isActive == true
                }
            }
        }
    }

    override suspend fun start() {
        mutex.withLock {
            for (cfg in platformConfigs.filter { it.enabled }) {
                if (platforms.any { it.metadata.name == cfg.name || it.metadata.name == cfg.type }) continue
                val platform = PlatformRegistry.createFromConfig(cfg) ?: continue
                platforms.add(platform)
            }

            for (platform in platforms) {
                if (platform.metadata.name in jobs && jobs[platform.metadata.name]?.isActive == true) continue
                jobs[platform.metadata.name] = platform.run()
            }
        }
    }

    override suspend fun stop() {
        mutex.withLock {
            for ((name, job) in jobs) {
                job.cancel()
            }
            jobs.clear()
            for (platform in platforms) {
                platform.terminate()
            }
            platforms.clear()
        }
    }
}
