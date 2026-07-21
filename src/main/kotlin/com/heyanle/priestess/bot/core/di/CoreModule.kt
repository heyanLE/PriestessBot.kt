package com.heyanle.priestess.bot.core.di

import com.heyanle.priestess.bot.agent.context.ContextManager
import com.heyanle.priestess.bot.agent.context.TokenCounter
import com.heyanle.priestess.bot.agent.AgentCase
import com.heyanle.priestess.bot.agent.AgentController
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
import com.heyanle.priestess.bot.observability.ObservabilityCase
import com.heyanle.priestess.bot.observability.ObservabilityController
import com.heyanle.priestess.bot.pipeline.PipelineCase
import com.heyanle.priestess.bot.pipeline.PipelineController
import com.heyanle.priestess.bot.persona.PersonaCase
import com.heyanle.priestess.bot.persona.PersonaController
import com.heyanle.priestess.bot.persona.PersonaMemoryInjector
import com.heyanle.priestess.bot.persona.PersonaPermissionDeniedMessageResolver
import com.heyanle.priestess.bot.pipeline.PermissionDeniedMessageResolver
import com.heyanle.priestess.bot.pipeline.PermissionResolver
import com.heyanle.priestess.bot.pipeline.CommandCase
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
import com.heyanle.priestess.bot.server.ServerCase
import com.heyanle.priestess.bot.server.ServerController
import com.heyanle.priestess.bot.skill.SkillCase
import com.heyanle.priestess.bot.skill.SkillController
import com.heyanle.priestess.bot.skill.DefaultSkill
import com.heyanle.priestess.bot.tool.ToolCase
import com.heyanle.priestess.bot.tool.ToolController
import com.heyanle.priestess.bot.tool.ToolExecutor
import com.heyanle.priestess.bot.tool.ToolResultOverflowStore
import com.heyanle.priestess.bot.tool.builtin.registerBuiltinTools
import com.heyanle.priestess.bot.workspace.ConfigBackedWorkspaceConfigSource
import com.heyanle.priestess.bot.workspace.RealWorkspaceMcpToolResolver
import com.heyanle.priestess.bot.workspace.WorkspaceCase
import com.heyanle.priestess.bot.workspace.WorkspaceConfigSource
import com.heyanle.priestess.bot.workspace.WorkspaceController
import org.koin.dsl.module

val coreModule = module {

    single { ConfigController() }
    single { ConfigCase(controller = get()) }
    single { ObservabilityController() }
    single { ObservabilityCase(controller = get()) }

    single {
        val configCase: ConfigCase = get()
        DatabaseController(dbPath = configCase.current().database.path)
    }
    single { DatabaseCase(controller = get()) }

    single { ConversationController(db = get<DatabaseCase>()) }
    single { MessageHistory(db = get<DatabaseCase>()) }
    single { ConversationCase(controller = get(), history = get()) }
    single { KnowledgeController(db = get<DatabaseCase>()) }
    single { KnowledgeCase(controller = get()) }
    single { MemoryController(db = get<DatabaseCase>()) }
    single { MemoryCase(controller = get()) }
    single { PersonaController(db = get<DatabaseCase>()) }
    single { PersonaCase(controller = get()) }
    single { PersonaMemoryInjector(personaCase = get(), memoryCase = get()) }
    single<PermissionDeniedMessageResolver> { PersonaPermissionDeniedMessageResolver(personaCase = get()) }
    single { PermissionResolver { get<ConfigCase>().current().permission } }
    single { CommandCase() }
    single { ReminderController(db = get<DatabaseCase>()) }
    single { ReminderCase(controller = get()) }

    single {
        val controller = ToolController()
        val scope = this
        val toolCase = ToolCase(
            controller = controller,
            executorProvider = {
                ToolExecutor(
                    registry = controller,
                    observabilityCase = scope.get(),
                    permissionDeniedMessageResolver = scope.get(),
                )
            },
        )
        registerBuiltinTools(
            registry = toolCase,
            knowledgeCaseProvider = { get<KnowledgeCase>() },
            serverCaseProvider = { get<ServerCase>() },
            conversationCaseProvider = { get<ConversationCase>() },
            memoryCaseProvider = { get<MemoryCase>() },
            reminderCaseProvider = { get<ReminderCase>() },
            workspaceCaseProvider = { get<WorkspaceCase>() },
            overflowStore = get(),
        )
        toolCase
    }

    single { ProviderController(configCase = get()) }
    single { ProviderCase(controller = get()) }

    single { TokenCounter() }
    single { ToolResultOverflowStore() }
    single { ContextManager(tokenCounter = get()) }
    single { AgentController() }
    single { AgentCase(controller = get(), contextManager = get(), overflowStore = get()) }
    single {
        SubAgentOrchestrator(
            agentCase = get(),
            providerCase = get(),
            toolCase = get(),
        )
    }
    single { SkillController().apply { register(DefaultSkill()) } }
    single { SkillCase(controller = get()) }
    single<WorkspaceConfigSource> { ConfigBackedWorkspaceConfigSource(configCase = get()) }
    single {
        WorkspaceController(
            source = get(),
            toolCase = get(),
            skillCase = get(),
            mcpToolResolver = RealWorkspaceMcpToolResolver(),
        )
    }
    single { WorkspaceCase(controller = get()) }

    single { PluginExtensionRegistry() }
    single {
        PluginController(
            configCase = get(),
            extensionRegistry = get(),
            toolCase = get(),
            providerCase = get(),
        )
    }
    single { PluginCase(controller = get(), extensionRegistry = get()) }

    single {
        PipelineController(
            configCase = get(),
            conversationCase = get(),
            agentCase = get(),
            providerCase = get(),
            toolCase = get(),
            skillCase = get(),
            subAgentOrchestrator = get(),
            workspaceCase = get(),
            personaMemoryInjector = get(),
            commandCase = get(),
            permissionResolver = get(),
            permissionDeniedMessageResolver = get(),
            observabilityCase = get(),
        )
    }
    single { PipelineCase(controller = get()) }

    single {
        val scope = this
        PlatformCase(
            pipelineCaseProvider = { scope.get<PipelineCase>() },
            controllerProvider = { scope.get<PlatformController>() },
        )
    }
    single { PlatformController(configCase = get(), platformCase = get()) }

    single {
        RuntimeHealthProvider(
            configCase = get(),
            platformCase = get(),
            providerCase = get(),
            toolCase = get(),
            pluginCase = get(),
        )
    }

    single {
        DashboardService(
            configCase = get(),
            platformCase = get(),
            providerCase = get(),
            toolCase = get(),
            conversationCase = get(),
            pluginCase = get(),
            agentCase = get(),
            knowledgeCase = get(),
            subAgentOrchestrator = get(),
            observabilityCase = get(),
            healthProvider = get(),
            workspaceCase = get(),
            personaCase = get(),
            memoryCase = get(),
            personaMemoryInjector = get(),
        )
    }
    single {
        val configCase: ConfigCase = get()
        PriestessBotServer(config = configCase.current().server, service = get())
    }
    single { ServerController(server = get()) }
    single { ServerCase(controller = get(), healthProvider = get()) }
    single {
        PriestessRuntime(
            platformCase = get(),
            pipelineCase = get(),
            serverCase = get(),
            pluginCase = get(),
            providerCase = get(),
            toolCase = get(),
            skillCase = get(),
            workspaceCase = get(),
            observabilityCase = get(),
            databaseCase = get(),
            configCase = get(),
            overflowStore = get(),
        )
    }
}
