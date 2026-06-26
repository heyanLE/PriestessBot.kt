package com.heyanle.priestess.bot.platform

import com.heyanle.priestess.bot.config.ConfigCase
import com.heyanle.priestess.bot.config.PlatformConfig
import com.heyanle.priestess.bot.core.controller.BaseController
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collectLatest

/**
 * 平台控制器，负责维护平台适配器实例、监听平台配置变化并承接平台模块生命周期。
 */
class PlatformController(
    private val configCase: ConfigCase,
    private val platformCase: PlatformCase,
) : BaseController("PlatformController") {

    private val lock = Any()
    private val platforms = LinkedHashMap<String, Platform>()
    private val jobs = LinkedHashMap<String, Job>()

    init {
        registerBuiltinPlatforms()
        launchTask("platform-config-listener") {
            configCase.platformConfigsFlow.collectLatest { configs ->
                sync(configs)
            }
        }
    }

    fun get(name: String): Platform? = synchronized(lock) { platforms[name] }

    fun getAll(): List<Platform> = synchronized(lock) { platforms.values.toList() }

    fun getRunning(): List<Platform> = synchronized(lock) {
        platforms.values.filter { platform -> jobs[platform.metadata.name]?.isActive == true }
    }

    private suspend fun sync(configs: List<PlatformConfig>) {
        val enabledKeys = configs.filter { it.enabled }
            .map { it.name.ifBlank { it.type } }
            .toSet()

        val existingNames = synchronized(lock) { platforms.keys.toList() }
        for (name in existingNames) {
            if (name !in enabledKeys) stopPlatform(name)
        }

        for (cfg in configs.filter { it.enabled }) {
            val configuredName = cfg.name.ifBlank { cfg.type }
            val alreadyRunning = synchronized(lock) {
                platforms.containsKey(configuredName) || platforms.containsKey(cfg.type)
            }
            if (alreadyRunning) continue

            try {
                val platform = PlatformRegistry.createFromConfig(cfg) ?: continue
                platform.setMessageHandler { event -> platformCase.handleIncomingMessage(event) }
                val job = platform.run()
                synchronized(lock) {
                    platforms[platform.metadata.name] = platform
                    jobs[platform.metadata.name] = job
                }
                logger.info { "Platform '${platform.metadata.name}' started" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to start platform '$configuredName'" }
            }
        }
    }

    private suspend fun stopPlatform(name: String) {
        val platform: Platform?
        val job: Job?
        synchronized(lock) {
            platform = platforms.remove(name)
            job = jobs.remove(name)
        }

        try {
            platform?.terminate()
        } catch (e: Exception) {
            logger.error(e) { "Failed to terminate platform '$name'" }
        }
        try {
            job?.cancelAndJoin()
        } catch (e: Exception) {
            logger.error(e) { "Failed to stop platform job '$name'" }
        }
    }

    override suspend fun stop() {
        val names = synchronized(lock) { platforms.keys.toList() }
        for (name in names) {
            stopPlatform(name)
        }
        super.stop()
    }
}
