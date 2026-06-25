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
import com.heyanle.priestess.bot.memory.MemoryCase
import com.heyanle.priestess.bot.memory.MemoryController
import com.heyanle.priestess.bot.observability.MetricsRegistry
import com.heyanle.priestess.bot.pipeline.PipelineCase
import com.heyanle.priestess.bot.pipeline.PipelineController
import com.heyanle.priestess.bot.persona.PersonaCase
import com.heyanle.priestess.bot.persona.PersonaController
import com.heyanle.priestess.bot.persona.PersonaMemoryInjector
import com.heyanle.priestess.bot.plugin.PluginCase
import com.heyanle.priestess.bot.plugin.PluginExtensionRegistry
import com.heyanle.priestess.bot.plugin.PluginController
import com.heyanle.priestess.bot.platform.PlatformCase
import com.heyanle.priestess.bot.platform.PlatformController
import com.heyanle.priestess.bot.provider.ProviderCase
import com.heyanle.priestess.bot.provider.ProviderController
import com.heyanle.priestess.bot.reminder.ReminderCase
import com.heyanle.priestess.bot.reminder.ReminderController
import com.heyanle.priestess.bot.server.DashboardService
import com.heyanle.priestess.bot.server.PriestessBotServer
import com.heyanle.priestess.bot.server.RuntimeHealthProvider
import com.heyanle.priestess.bot.skill.SkillCase
import com.heyanle.priestess.bot.skill.SkillController
import com.heyanle.priestess.bot.skill.DefaultSkill
import com.heyanle.priestess.bot.tool.ToolCase
import com.heyanle.priestess.bot.tool.ToolController
import com.heyanle.priestess.bot.tool.ToolExecutor
import com.heyanle.priestess.bot.tool.builtin.registerBuiltinTools
import com.heyanle.priestess.bot.workspace.ConfigBackedWorkspaceConfigSource
import com.heyanle.priestess.bot.workspace.RealWorkspaceMcpToolResolver
import com.heyanle.priestess.bot.workspace.WorkspaceConfigSource
import com.heyanle.priestess.bot.workspace.WorkspaceController
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
    single { MemoryController(db = get()) }
    single { MemoryCase(controller = get()) }
    single { PersonaController(db = get()) }
    single { PersonaCase(controller = get()) }
    single { PersonaMemoryInjector(personaCase = get(), memoryCase = get()) }
    single { ReminderController(db = get()) }
    single { ReminderCase(controller = get()) }

    single {
        val controller = ToolController()
        registerBuiltinTools(
            registry = controller,
            knowledgeCaseProvider = { get<KnowledgeCase>() },
            healthProvider = { get<RuntimeHealthProvider>() },
            conversationCaseProvider = { get<ConversationCase>() },
            memoryCaseProvider = { get<MemoryCase>() },
            reminderCaseProvider = { get<ReminderCase>() },
        )
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
    single { SkillController().apply { register(DefaultSkill()) } }
    single { SkillCase(controller = get()) }
    single<WorkspaceConfigSource> { ConfigBackedWorkspaceConfigSource(configCase = get()) }
    single {
        WorkspaceController(
            source = get(),
            toolController = get(),
            skillCase = get(),
            mcpToolResolver = RealWorkspaceMcpToolResolver(),
        )
    }

    single { PluginExtensionRegistry() }
    single {
        PluginController(
            configCase = get(),
            extensionRegistry = get(),
            toolController = get(),
            providerController = get(),
        )
    }
    single { PluginCase(controller = get(), extensionRegistry = get()) }

    single {
        PipelineController(
            configCase = get(),
            conversationCase = get(),
            agentCase = get(),
            contextManager = get(),
            providerCase = get(),
            toolExecutor = get(),
            toolController = get(),
            skillCase = get(),
            subAgentOrchestrator = get(),
            workspaceController = get(),
            personaMemoryInjector = get(),
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
        RuntimeHealthProvider(
            configController = get(),
            configCase = get(),
            platformController = get(),
            providerCase = get(),
            toolController = get(),
            pluginCase = get(),
        )
    }

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
            healthProvider = get(),
            workspaceController = get(),
            personaCase = get(),
            memoryCase = get(),
            personaMemoryInjector = get(),
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
            pluginCase = get(),
            providerController = get(),
            toolController = get(),
            workspaceController = get(),
            databaseController = get(),
            configController = get(),
        )
    }
}
