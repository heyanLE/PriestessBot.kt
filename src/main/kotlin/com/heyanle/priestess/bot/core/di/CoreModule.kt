package com.heyanle.priestess.bot.core.di

import com.heyanle.priestess.bot.agent.context.ContextManager
import com.heyanle.priestess.bot.agent.context.TokenCounter
import com.heyanle.priestess.bot.agent.AgentCase
import com.heyanle.priestess.bot.config.ConfigCase
import com.heyanle.priestess.bot.config.ConfigController
import com.heyanle.priestess.bot.conversation.ConversationCase
import com.heyanle.priestess.bot.conversation.ConversationController
import com.heyanle.priestess.bot.conversation.MessageHistory
import com.heyanle.priestess.bot.core.db.DatabaseCase
import com.heyanle.priestess.bot.core.db.DatabaseController
import com.heyanle.priestess.bot.pipeline.PipelineCase
import com.heyanle.priestess.bot.pipeline.PipelineController
import com.heyanle.priestess.bot.platform.PlatformCase
import com.heyanle.priestess.bot.platform.PlatformController
import com.heyanle.priestess.bot.provider.ProviderCase
import com.heyanle.priestess.bot.provider.ProviderController
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

    single {
        val configCase: ConfigCase = get()
        DatabaseController(dbPath = configCase.current().database.path)
    }
    single { DatabaseCase(controller = get()) }

    single { ConversationController(db = get()) }
    single { MessageHistory(db = get()) }
    single { ConversationCase(controller = get(), history = get()) }

    single {
        val controller = ToolController()
        registerBuiltinTools(controller)
        controller
    }
    single { ToolCase(controller = get()) }
    single { ToolExecutor(registry = get()) }

    single { ProviderController(configCase = get()) }
    single { ProviderCase(controller = get()) }

    single { TokenCounter() }
    single { ContextManager(tokenCounter = get()) }
    single { AgentCase() }
    single { SkillController() }
    single { SkillCase(controller = get()) }

    single {
        PipelineController(
            configCase = get(),
            conversationCase = get(),
            agentCase = get(),
            contextManager = get(),
            providerCase = get(),
            toolExecutor = get(),
            toolController = get(),
        )
    }
    single { PipelineCase(controller = get()) }

    single {
        val scope = this
        PlatformCase(pipelineCaseProvider = { scope.get<PipelineCase>() })
    }
    single { PlatformController(configCase = get(), platformCase = get()) }
}
