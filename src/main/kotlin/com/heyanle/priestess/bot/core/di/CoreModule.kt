package com.heyanle.priestess.bot.core.di

import com.heyanle.priestess.bot.agent.context.ContextManager
import com.heyanle.priestess.bot.agent.context.TokenCounter
import com.heyanle.priestess.bot.agent.AgentCase
import com.heyanle.priestess.bot.agent.orchestration.SubAgentOrchestrator
import com.heyanle.priestess.bot.PriestessRuntime
import com.heyanle.priestess.bot.config.ConfigCase
import com.heyanle.priestess.bot.config.ConfigController
import com.heyanle.priestess.bot.conversation.ConversationCase
import com.heyanle.priestess.bot.conversation.ConversationController
import com.heyanle.priestess.bot.conversation.MessageHistory
import com.heyanle.priestess.bot.core.db.DatabaseCase
import com.heyanle.priestess.bot.core.db.DatabaseController
import com.heyanle.priestess.bot.knowledge.KnowledgeCase
import com.heyanle.priestess.bot.knowledge.KnowledgeController
import com.heyanle.priestess.bot.observability.MetricsRegistry
import com.heyanle.priestess.bot.pipeline.PipelineCase
import com.heyanle.priestess.bot.pipeline.PipelineController
import com.heyanle.priestess.bot.plugin.PluginCase
import com.heyanle.priestess.bot.plugin.PluginExtensionRegistry
import com.heyanle.priestess.bot.plugin.PluginManager
import com.heyanle.priestess.bot.platform.PlatformCase
import com.heyanle.priestess.bot.platform.PlatformController
import com.heyanle.priestess.bot.provider.ProviderCase
import com.heyanle.priestess.bot.provider.ProviderController
import com.heyanle.priestess.bot.server.DashboardService
import com.heyanle.priestess.bot.server.PriestessBotServer
import com.heyanle.priestess.bot.skill.SkillCase
import com.heyanle.priestess.bot.skill.SkillController
import com.heyanle.priestess.bot.tool.ToolCase
import com.heyanle.priestess.bot.tool.ToolController
import com.heyanle.priestess.bot.tool.ToolExecutor
import com.heyanle.priestess.bot.tool.builtin.registerBuiltinTools
import org.koin.dsl.module

val coreModule = module {

    single { ConfigController() }
    single { ConfigCase(controller = get()) }
    single { MetricsRegistry() }

    single {
        val configCase: ConfigCase = get()
        DatabaseController(dbPath = configCase.current().database.path)
    }
    single { DatabaseCase(controller = get()) }

    single { ConversationController(db = get()) }
    single { MessageHistory(db = get()) }
    single { ConversationCase(controller = get(), history = get()) }
    single { KnowledgeController(db = get()) }
    single { KnowledgeCase(controller = get()) }

    single {
        val controller = ToolController()
        registerBuiltinTools(controller, knowledgeCaseProvider = { get<KnowledgeCase>() })
        controller
    }
    single { ToolCase(controller = get()) }
    single { ToolExecutor(registry = get(), metricsRegistry = get()) }

    single { ProviderController(configCase = get()) }
    single { ProviderCase(controller = get()) }

    single { TokenCounter() }
    single { ContextManager(tokenCounter = get()) }
    single { AgentCase() }
    single {
        SubAgentOrchestrator(
            agentCase = get(),
            contextManager = get(),
            providerCase = get(),
            toolExecutor = get(),
            toolController = get(),
        )
    }
    single { SkillController() }
    single { SkillCase(controller = get()) }

    single { PluginExtensionRegistry() }
    single {
        PluginManager(
            configCase = get(),
            extensionRegistry = get(),
            toolController = get(),
            providerController = get(),
        )
    }
    single { PluginCase(manager = get(), extensionRegistry = get()) }

    single {
        PipelineController(
            configCase = get(),
            conversationCase = get(),
            agentCase = get(),
            contextManager = get(),
            providerCase = get(),
            toolExecutor = get(),
            toolController = get(),
            subAgentOrchestrator = get(),
            metricsRegistry = get(),
        )
    }
    single { PipelineCase(controller = get()) }

    single {
        val scope = this
        PlatformCase(pipelineCaseProvider = { scope.get<PipelineCase>() })
    }
    single { PlatformController(configCase = get(), platformCase = get()) }

    single {
        DashboardService(
            configController = get(),
            configCase = get(),
            platformController = get(),
            providerCase = get(),
            toolController = get(),
            conversationCase = get(),
            pluginCase = get(),
            agentCase = get(),
            contextManager = get(),
            toolExecutor = get(),
            knowledgeCase = get(),
            subAgentOrchestrator = get(),
            metricsRegistry = get(),
        )
    }
    single {
        val configCase: ConfigCase = get()
        PriestessBotServer(config = configCase.current().server, service = get())
    }
    single {
        PriestessRuntime(
            platformController = get(),
            pipelineController = get(),
            server = get(),
            pluginManager = get(),
            providerController = get(),
            toolController = get(),
            databaseController = get(),
            configController = get(),
        )
    }
}
