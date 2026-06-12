package com.heyanle.priestess.bot.core.di

import com.heyanle.priestess.bot.conversation.ConversationManager
import com.heyanle.priestess.bot.conversation.MessageHistory
import com.heyanle.priestess.bot.core.config.ConfigLoader
import com.heyanle.priestess.bot.core.config.PriestessConfig
import com.heyanle.priestess.bot.core.db.PriestessDb
import com.heyanle.priestess.bot.core.event.EventBus
import com.heyanle.priestess.bot.core.lifecycle.CoreLifecycle
import com.heyanle.priestess.bot.core.lifecycle.LifecycleAware
import com.heyanle.priestess.bot.platform.PlatformManager
import com.heyanle.priestess.bot.platform.registerBuiltinPlatforms
import com.heyanle.priestess.bot.provider.ProviderManager
import com.heyanle.priestess.bot.provider.ProviderRegistry
import com.heyanle.priestess.bot.provider.registerBuiltinProviders
import com.heyanle.priestess.bot.agent.context.ContextManager
import com.heyanle.priestess.bot.agent.context.TokenCounter
import com.heyanle.priestess.bot.tool.ToolExecutor
import com.heyanle.priestess.bot.tool.ToolRegistry
import com.heyanle.priestess.bot.tool.builtin.registerBuiltinTools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

val coreModule = module {

    // Config
    single { ConfigLoader.load() }

    // Database
    single {
        val config: PriestessConfig = get()
        PriestessDb(dbPath = config.database.path)
    }

    single { ConversationManager(db = get()) }
    single { MessageHistory(db = get()) }

    // EventBus — scope is managed by CoreLifecycle via LifecycleAware.stop()
    single {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        EventBus(scope = scope)
    }

    // Platform — register built-in adapters then create manager from persisted config
    single {
        val eventBus: EventBus = get()
        registerBuiltinPlatforms(eventBus)

        val config: PriestessConfig = get()
        PlatformManager(platformConfigs = config.platforms)
    }

    // Provider — register built-in adapters then create from persisted config
    single {
        registerBuiltinProviders()

        val config: PriestessConfig = get()
        val manager = ProviderManager()
        for (pc in config.providers) {
            val provider = ProviderRegistry.createFromConfig(pc) ?: continue
            manager.register(provider)
        }
        manager
    }

    // Tool Registry — register built-in tools, serves as LifecycleAware
    single {
        val registry = ToolRegistry()
        registerBuiltinTools(registry)
        registry
    }

    // Tool Executor
    single {
        val registry: ToolRegistry = get()
        ToolExecutor(registry = registry)
    }

    // Agent — context compression
    single { TokenCounter() }
    single { ContextManager(tokenCounter = get()) }

    // Lifecycle components — lazy evaluation so later registrations are visible
    single<CoreLifecycle> {
        CoreLifecycle(
            componentsProvider = lazy { getAll<LifecycleAware>().toList() }
        )
    }
}
