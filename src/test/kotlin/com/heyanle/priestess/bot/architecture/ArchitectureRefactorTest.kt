package com.heyanle.priestess.bot.architecture

import com.heyanle.priestess.bot.config.ConfigCase
import com.heyanle.priestess.bot.config.ConfigController
import com.heyanle.priestess.bot.config.DatabaseConfig
import com.heyanle.priestess.bot.config.PipelineConfig
import com.heyanle.priestess.bot.config.PlatformConfig
import com.heyanle.priestess.bot.config.PluginConfig
import com.heyanle.priestess.bot.config.PriestessConfig
import com.heyanle.priestess.bot.conversation.ConversationCase
import com.heyanle.priestess.bot.conversation.ConversationController
import com.heyanle.priestess.bot.conversation.MessageHistory
import com.heyanle.priestess.bot.conversation.MessageRole
import com.heyanle.priestess.bot.core.db.DatabaseController
import com.heyanle.priestess.bot.core.controller.BaseController
import com.heyanle.priestess.bot.agent.AgentCase
import com.heyanle.priestess.bot.agent.orchestration.SubAgentOrchestrator
import com.heyanle.priestess.bot.config.AgentConfig
import com.heyanle.priestess.bot.config.ProviderConfig
import com.heyanle.priestess.bot.config.SubAgentConfig
import com.heyanle.priestess.bot.config.SubAgentOrchestrationConfig
import com.heyanle.priestess.bot.config.SubAgentRouteConfig
import com.heyanle.priestess.bot.core.di.coreModule
import com.heyanle.priestess.bot.core.db.DatabaseCase
import com.heyanle.priestess.bot.pipeline.PipelineCase
import com.heyanle.priestess.bot.pipeline.PipelineContext
import com.heyanle.priestess.bot.pipeline.PipelineController
import com.heyanle.priestess.bot.pipeline.Stage
import com.heyanle.priestess.bot.pipeline.StageOrder
import com.heyanle.priestess.bot.pipeline.stages.PreProcessStage
import com.heyanle.priestess.bot.pipeline.stages.ProcessStage
import com.heyanle.priestess.bot.pipeline.stages.RespondStage
import com.heyanle.priestess.bot.pipeline.stages.ResultDecorateStage
import com.heyanle.priestess.bot.pipeline.stages.WakingCheckStage
import com.heyanle.priestess.bot.pipeline.stages.WhitelistCheckStage
import com.heyanle.priestess.bot.platform.MessageChain
import com.heyanle.priestess.bot.platform.MessageComponent
import com.heyanle.priestess.bot.platform.MessageEvent
import com.heyanle.priestess.bot.platform.MessageSession
import com.heyanle.priestess.bot.platform.Platform
import com.heyanle.priestess.bot.platform.PlatformCase
import com.heyanle.priestess.bot.platform.PlatformController
import com.heyanle.priestess.bot.platform.PlatformMetadata
import com.heyanle.priestess.bot.platform.PlatformRegistry
import com.heyanle.priestess.bot.platform.SessionType
import com.heyanle.priestess.bot.provider.ChatProvider
import com.heyanle.priestess.bot.provider.LLMKind
import com.heyanle.priestess.bot.provider.ProviderCase
import com.heyanle.priestess.bot.provider.ProviderController
import com.heyanle.priestess.bot.provider.ProviderMetadata
import com.heyanle.priestess.bot.provider.model.LLMRequest
import com.heyanle.priestess.bot.provider.model.LLMResponse
import com.heyanle.priestess.bot.provider.model.ToolCall
import com.heyanle.priestess.bot.skill.SkillCase
import com.heyanle.priestess.bot.skill.SkillController
import com.heyanle.priestess.bot.tool.AgentToolContext
import com.heyanle.priestess.bot.tool.FunctionTool
import com.heyanle.priestess.bot.tool.ToolCase
import com.heyanle.priestess.bot.tool.ToolController
import com.heyanle.priestess.bot.tool.ToolResult
import com.heyanle.priestess.bot.tool.ToolSchema
import com.heyanle.priestess.bot.workspace.WorkspaceCase
import com.heyanle.priestess.bot.workspace.ConfigBackedWorkspaceConfigSource
import com.heyanle.priestess.bot.workspace.WorkspaceConfig
import com.heyanle.priestess.bot.workspace.WorkspaceConfigSet
import com.heyanle.priestess.bot.workspace.WorkspaceConfigSource
import com.heyanle.priestess.bot.workspace.WorkspaceController
import com.heyanle.priestess.bot.workspace.WorkspaceMemoryPolicyConfig
import com.heyanle.priestess.bot.workspace.WorkspaceResolution
import com.heyanle.priestess.bot.workspace.WorkspaceSnapshot
import com.heyanle.priestess.bot.workspace.WorkspaceRuntimeDefaults
import com.heyanle.priestess.bot.server.DashboardService
import com.heyanle.priestess.bot.server.RuntimeHealthProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArchitectureRefactorTest {

    @Test
    fun `BaseController subclasses use Controller suffix`() {
        val sourceRoot = File("src/main/kotlin")
        val violations = sourceRoot
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                val text = file.readText()
                Regex("""class\s+([A-Za-z0-9_]+)[^{\n]*:\s*BaseController\b""")
                    .findAll(text)
                    .map { match -> file.invariantSeparatorsPath to match.groupValues[1] }
            }
            .filterNot { (_, className) -> className.endsWith("Controller") }
            .toList()

        assertTrue(
            violations.isEmpty(),
            "BaseController subclasses must end with Controller: $violations",
        )
    }

    @Test
    fun `conversation module uses DatabaseCase instead of DatabaseController across module boundary`() {
        val sourceRoot = File("src/main/kotlin/com/heyanle/priestess/bot/conversation")
        val violations = sourceRoot
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file -> file.readText().contains("DatabaseController") }
            .map { it.invariantSeparatorsPath }
            .toList()

        assertTrue(
            violations.isEmpty(),
            "Conversation module must depend on DatabaseCase, not DatabaseController: $violations",
        )
    }

    @Test
    fun `knowledge module uses DatabaseCase instead of DatabaseController across module boundary`() {
        val sourceRoot = File("src/main/kotlin/com/heyanle/priestess/bot/knowledge")
        val violations = sourceRoot
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file -> file.readText().contains("DatabaseController") }
            .map { it.invariantSeparatorsPath }
            .toList()

        assertTrue(
            violations.isEmpty(),
            "Knowledge module must depend on DatabaseCase, not DatabaseController: $violations",
        )
    }

    @Test
    fun `memory module uses DatabaseCase instead of DatabaseController across module boundary`() {
        val sourceRoot = File("src/main/kotlin/com/heyanle/priestess/bot/memory")
        val violations = sourceRoot
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file -> file.readText().contains("DatabaseController") }
            .map { it.invariantSeparatorsPath }
            .toList()

        assertTrue(
            violations.isEmpty(),
            "Memory module must depend on DatabaseCase, not DatabaseController: $violations",
        )
    }

    @Test
    fun `persona module uses DatabaseCase instead of DatabaseController across module boundary`() {
        val sourceRoot = File("src/main/kotlin/com/heyanle/priestess/bot/persona")
        val violations = sourceRoot
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file -> file.readText().contains("DatabaseController") }
            .map { it.invariantSeparatorsPath }
            .toList()

        assertTrue(
            violations.isEmpty(),
            "Persona module must depend on DatabaseCase, not DatabaseController: $violations",
        )
    }

    @Test
    fun `reminder module uses DatabaseCase instead of DatabaseController across module boundary`() {
        val sourceRoot = File("src/main/kotlin/com/heyanle/priestess/bot/reminder")
        val violations = sourceRoot
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file -> file.readText().contains("DatabaseController") }
            .map { it.invariantSeparatorsPath }
            .toList()

        assertTrue(
            violations.isEmpty(),
            "Reminder module must depend on DatabaseCase, not DatabaseController: $violations",
        )
    }

    @Test
    fun `database module exposes controller and case boundary`() {
        val sourceRoot = File("src/main/kotlin/com/heyanle/priestess/bot/core/db")
        val controller = sourceRoot.resolve("DatabaseController.kt")
        val caseText = sourceRoot.resolve("DatabaseCase.kt").readText()

        assertTrue(controller.isFile, "Database module must expose DatabaseController")
        assertTrue(
            controller.readText().contains(": BaseController(\"DatabaseController\")"),
            "DatabaseController must extend BaseController",
        )
        assertTrue(
            caseText.contains("private val controller: DatabaseController"),
            "DatabaseCase must delegate through its own DatabaseController",
        )
        assertTrue(
            caseText.contains("suspend fun stop()") && caseText.contains("controller.stop()"),
            "DatabaseCase must own the module lifecycle facade",
        )
    }

    @Test
    fun `production modules use DatabaseCase instead of DatabaseController across module boundary`() {
        val sourceRoot = File("src/main/kotlin")
        val violations = sourceRoot
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.invariantSeparatorsPath.contains("/core/db/") }
            .filterNot { it.invariantSeparatorsPath.endsWith("/core/di/CoreModule.kt") }
            .filter { file -> file.readText().contains("DatabaseController") }
            .map { it.invariantSeparatorsPath }
            .toList()

        assertTrue(
            violations.isEmpty(),
            "Production modules must depend on DatabaseCase, not DatabaseController: $violations",
        )
    }

    @Test
    fun `config module exposes controller and case boundary`() {
        val sourceRoot = File("src/main/kotlin/com/heyanle/priestess/bot/config")
        val controller = sourceRoot.resolve("ConfigController.kt")
        val caseText = sourceRoot.resolve("ConfigCase.kt").readText()

        assertTrue(controller.isFile, "Config module must expose ConfigController")
        assertTrue(
            controller.readText().contains(": BaseController(\"ConfigController\")"),
            "ConfigController must extend BaseController",
        )
        assertTrue(
            caseText.contains("private val controller: ConfigController"),
            "ConfigCase must delegate through its own ConfigController",
        )
        assertTrue(
            caseText.contains("suspend fun stop()") && caseText.contains("controller.stop()"),
            "ConfigCase must own the module lifecycle facade",
        )
    }

    @Test
    fun `production modules use ConfigCase instead of ConfigController across module boundary`() {
        val sourceRoot = File("src/main/kotlin")
        val violations = sourceRoot
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.invariantSeparatorsPath.contains("/config/") }
            .filterNot { it.invariantSeparatorsPath.endsWith("/core/di/CoreModule.kt") }
            .filter { file -> file.readText().contains("ConfigController") }
            .map { it.invariantSeparatorsPath }
            .toList()

        assertTrue(
            violations.isEmpty(),
            "Production modules must depend on ConfigCase, not ConfigController: $violations",
        )
    }

    @Test
    fun `pipeline module exposes controller and case boundary`() {
        val sourceRoot = File("src/main/kotlin/com/heyanle/priestess/bot/pipeline")
        val controller = sourceRoot.resolve("PipelineController.kt")
        val caseText = sourceRoot.resolve("PipelineCase.kt").readText()

        assertTrue(controller.isFile, "Pipeline module must expose PipelineController")
        assertTrue(
            controller.readText().contains(": BaseController(\"PipelineController\")"),
            "PipelineController must extend BaseController",
        )
        assertTrue(
            caseText.contains("private val controller: PipelineController"),
            "PipelineCase must delegate through its own PipelineController",
        )
        assertTrue(
            caseText.contains("suspend fun drain(") &&
                caseText.contains("return controller.drain(timeoutMillis)") &&
                caseText.contains("suspend fun stop()") &&
                caseText.contains("controller.stop()"),
            "PipelineCase must own the module lifecycle facade",
        )
    }

    @Test
    fun `production modules use PipelineCase instead of PipelineController across module boundary`() {
        val sourceRoot = File("src/main/kotlin")
        val violations = sourceRoot
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.invariantSeparatorsPath.contains("/pipeline/") }
            .filterNot { it.invariantSeparatorsPath.endsWith("/core/di/CoreModule.kt") }
            .filter { file -> file.readText().contains("PipelineController") }
            .map { it.invariantSeparatorsPath }
            .toList()

        assertTrue(
            violations.isEmpty(),
            "Production modules must depend on PipelineCase, not PipelineController: $violations",
        )
    }

    @Test
    fun `platform module exposes controller and case boundary`() {
        val sourceRoot = File("src/main/kotlin/com/heyanle/priestess/bot/platform")
        val controller = sourceRoot.resolve("PlatformController.kt")
        val caseText = sourceRoot.resolve("PlatformCase.kt").readText()

        assertTrue(controller.isFile, "Platform module must expose PlatformController")
        assertTrue(
            controller.readText().contains(": BaseController(\"PlatformController\")"),
            "PlatformController must extend BaseController",
        )
        assertTrue(
            caseText.contains("controllerProvider: () -> PlatformController"),
            "PlatformCase must resolve its own PlatformController lazily",
        )
        assertTrue(
            caseText.contains("fun start()") && caseText.contains("controllerProvider()"),
            "PlatformCase must expose an explicit lifecycle start facade",
        )
        assertTrue(
            caseText.contains("suspend fun stop()") && caseText.contains("controllerProvider().stop()"),
            "PlatformCase must own the module lifecycle facade",
        )
    }

    @Test
    fun `production modules use PlatformCase instead of PlatformController across module boundary`() {
        val sourceRoot = File("src/main/kotlin")
        val violations = sourceRoot
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.invariantSeparatorsPath.contains("/platform/") }
            .filterNot { it.invariantSeparatorsPath.endsWith("/core/di/CoreModule.kt") }
            .filter { file -> file.readText().contains("PlatformController") }
            .map { it.invariantSeparatorsPath }
            .toList()

        assertTrue(
            violations.isEmpty(),
            "Production modules must depend on PlatformCase, not PlatformController: $violations",
        )
    }

    @Test
    fun `agent module exposes controller and case boundary`() {
        val sourceRoot = File("src/main/kotlin/com/heyanle/priestess/bot/agent")
        val controller = sourceRoot.resolve("AgentController.kt")
        val caseText = sourceRoot.resolve("AgentCase.kt").readText()

        assertTrue(controller.isFile, "Agent module must expose AgentController")
        assertTrue(
            controller.readText().contains(": BaseController(\"AgentController\")"),
            "AgentController must extend BaseController",
        )
        assertTrue(
            caseText.contains("private val controller: AgentController"),
            "AgentCase must delegate through its own AgentController",
        )
        assertTrue(
            caseText.contains("private val contextManager: ContextManager"),
            "AgentCase must own the Agent context manager dependency",
        )
        val runWithProviderSignature = caseText
            .substringAfter("suspend fun runWithProvider(")
            .substringBefore("): AgentResponse")
        assertFalse(
            runWithProviderSignature.contains("ContextManager"),
            "AgentCase runWithProvider must not expose ContextManager across module boundaries",
        )
        assertTrue(
            caseText.contains("suspend fun runWithProvider(") &&
                caseText.contains("ReActRunner(") &&
                caseText.contains(".stepUntilDone()"),
            "AgentCase must own the ReAct execution facade",
        )
    }

    @Test
    fun `production modules keep ContextManager inside agent boundary`() {
        val sourceRoot = File("src/main/kotlin")
        val violations = sourceRoot
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.invariantSeparatorsPath.contains("/agent/") }
            .filterNot { it.invariantSeparatorsPath.endsWith("/core/di/CoreModule.kt") }
            .filter { file -> file.readText().contains("ContextManager") }
            .map { it.invariantSeparatorsPath }
            .toList()

        assertTrue(
            violations.isEmpty(),
            "Production modules must not depend on Agent ContextManager directly: $violations",
        )
    }

    @Test
    fun `core module wires ContextManager only through AgentCase`() {
        val coreModuleText = File("src/main/kotlin/com/heyanle/priestess/bot/core/di/CoreModule.kt").readText()

        assertTrue(coreModuleText.contains("single { ContextManager(tokenCounter = get()) }"))
        assertTrue(coreModuleText.contains("single { AgentCase(controller = get(), contextManager = get()) }"))
        assertFalse(
            coreModuleText.contains("contextManager = get(),"),
            "Core module must not inject ContextManager into non-AgentCase modules",
        )
    }

    @Test
    fun `production modules use AgentCase instead of ReActRunner across module boundary`() {
        val sourceRoot = File("src/main/kotlin")
        val violations = sourceRoot
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.invariantSeparatorsPath.contains("/agent/") }
            .filter { file -> file.readText().contains("ReActRunner") }
            .map { it.invariantSeparatorsPath }
            .toList()

        assertTrue(
            violations.isEmpty(),
            "Production modules must run agents through AgentCase, not ReActRunner: $violations",
        )
    }

    @Test
    fun `agent module does not depend on server module`() {
        val sourceRoot = File("src/main/kotlin/com/heyanle/priestess/bot/agent")
        val violations = sourceRoot
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file -> file.readText().contains("com.heyanle.priestess.bot.server") }
            .map { it.invariantSeparatorsPath }
            .toList()

        assertTrue(
            violations.isEmpty(),
            "Agent module must not depend on server DTOs or services: $violations",
        )
    }

    @Test
    fun `skill module exposes controller and case boundary`() {
        val sourceRoot = File("src/main/kotlin/com/heyanle/priestess/bot/skill")
        val controller = sourceRoot.resolve("SkillController.kt")
        val caseText = sourceRoot.resolve("SkillCase.kt").readText()

        assertTrue(controller.isFile, "Skill module must expose SkillController")
        assertTrue(
            controller.readText().contains(": BaseController(\"SkillController\")"),
            "SkillController must extend BaseController",
        )
        assertTrue(
            caseText.contains("private val controller: SkillController"),
            "SkillCase must delegate through its own SkillController",
        )
        assertTrue(
            caseText.contains("suspend fun stop()") && caseText.contains("controller.stop()"),
            "SkillCase must own the module lifecycle facade",
        )
    }

    @Test
    fun `observability module exposes controller and case boundary`() {
        val sourceRoot = File("src/main/kotlin/com/heyanle/priestess/bot/observability")
        val controller = sourceRoot.resolve("ObservabilityController.kt")
        val caseText = sourceRoot.resolve("ObservabilityCase.kt").readText()

        assertTrue(controller.isFile, "Observability module must expose ObservabilityController")
        assertTrue(
            controller.readText().contains(": BaseController(\"ObservabilityController\")"),
            "ObservabilityController must extend BaseController",
        )
        assertTrue(
            caseText.contains("private val controller: ObservabilityController"),
            "ObservabilityCase must delegate through its own ObservabilityController",
        )
        assertTrue(
            caseText.contains("suspend fun stop()") && caseText.contains("controller.stop()"),
            "ObservabilityCase must own the module lifecycle facade",
        )
    }

    @Test
    fun `server module exposes controller and case boundary`() {
        val sourceRoot = File("src/main/kotlin/com/heyanle/priestess/bot/server")
        val controller = sourceRoot.resolve("ServerController.kt")
        val caseText = sourceRoot.resolve("ServerCase.kt").readText()

        assertTrue(controller.isFile, "Server module must expose ServerController")
        assertTrue(
            controller.readText().contains(": BaseController(\"ServerController\")"),
            "ServerController must extend BaseController",
        )
        assertTrue(
            caseText.contains("private val controller: ServerController"),
            "ServerCase must delegate through its own ServerController",
        )
        assertTrue(
            caseText.contains("private val healthProvider: RuntimeHealthProvider") &&
                caseText.contains("fun healthSnapshot()") &&
                caseText.contains("healthProvider.snapshot()"),
            "ServerCase must expose runtime health through the server module facade",
        )
    }

    @Test
    fun `tool module uses ServerCase instead of server health internals`() {
        val sourceRoot = File("src/main/kotlin/com/heyanle/priestess/bot/tool")
        val violations = sourceRoot
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file ->
                val text = file.readText()
                text.contains("RuntimeHealthProvider") || text.contains("HealthResponse")
            }
            .map { it.invariantSeparatorsPath }
            .toList()

        assertTrue(
            violations.isEmpty(),
            "Tool module must depend on ServerCase, not server health internals: $violations",
        )
    }

    @Test
    fun `production modules use ObservabilityCase instead of MetricsRegistry across module boundary`() {
        val sourceRoot = File("src/main/kotlin")
        val violations = sourceRoot
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.invariantSeparatorsPath.contains("/observability/") }
            .filter { file -> file.readText().contains("MetricsRegistry") }
            .map { it.invariantSeparatorsPath }
            .toList()

        assertTrue(
            violations.isEmpty(),
            "Production modules must depend on ObservabilityCase, not MetricsRegistry: $violations",
        )
    }

    @Test
    fun `metric names stay inside observability module`() {
        val sourceRoot = File("src/main/kotlin")
        val violations = sourceRoot
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.invariantSeparatorsPath.contains("/observability/") }
            .filter { file -> file.readText().contains("priestess_") }
            .map { it.invariantSeparatorsPath }
            .toList()

        assertTrue(
            violations.isEmpty(),
            "Metric names must be owned by observability module: $violations",
        )
    }

    @Test
    fun `core module wires observability through case boundary`() {
        val coreModuleText = File("src/main/kotlin/com/heyanle/priestess/bot/core/di/CoreModule.kt").readText()

        assertTrue(coreModuleText.contains("single { ObservabilityController() }"))
        assertTrue(coreModuleText.contains("single { ObservabilityCase(controller = get()) }"))
        assertFalse(
            coreModuleText.contains("single { MetricsRegistry() }"),
            "Core module must not publish MetricsRegistry directly",
        )
    }

    @Test
    fun `runtime starts and stops server through ServerCase`() {
        val runtimeText = File("src/main/kotlin/com/heyanle/priestess/bot/PriestessRuntime.kt").readText()

        assertTrue(runtimeText.contains("serverCase: ServerCase"))
        assertTrue(runtimeText.contains("serverCase.start()"))
        assertTrue(runtimeText.contains("StopStep(\"server\") { serverCase.stop() }"))
        assertFalse(
            runtimeText.contains("PriestessBotServer"),
            "Runtime must depend on ServerCase instead of PriestessBotServer",
        )
    }

    @Test
    fun `runtime stops platforms through PlatformCase`() {
        val runtimeText = File("src/main/kotlin/com/heyanle/priestess/bot/PriestessRuntime.kt").readText()

        assertTrue(runtimeText.contains("platformCase: PlatformCase"))
        assertTrue(runtimeText.contains("platformCase.start()"))
        assertTrue(runtimeText.contains("StopStep(\"platforms\") { platformCase.stop() }"))
        assertFalse(
            runtimeText.contains("PlatformController"),
            "Runtime must depend on PlatformCase instead of PlatformController",
        )
    }

    @Test
    fun `runtime stops pipeline through PipelineCase`() {
        val runtimeText = File("src/main/kotlin/com/heyanle/priestess/bot/PriestessRuntime.kt").readText()

        assertTrue(runtimeText.contains("pipelineCase: PipelineCase"))
        assertTrue(runtimeText.contains("pipelineDrainTimeoutMillis: Long = PipelineCase.DEFAULT_DRAIN_TIMEOUT_MILLIS"))
        assertTrue(runtimeText.contains("pipelineCase.drain(pipelineDrainTimeoutMillis)"))
        assertTrue(runtimeText.contains("pipelineCase.stop()"))
        assertFalse(
            runtimeText.contains("PipelineController"),
            "Runtime must depend on PipelineCase instead of PipelineController",
        )
    }

    @Test
    fun `runtime stops providers through ProviderCase`() {
        val runtimeText = File("src/main/kotlin/com/heyanle/priestess/bot/PriestessRuntime.kt").readText()

        assertTrue(runtimeText.contains("providerCase: ProviderCase"))
        assertTrue(runtimeText.contains("StopStep(\"providers\") { providerCase.stop() }"))
        assertFalse(
            runtimeText.contains("ProviderController"),
            "Runtime must depend on ProviderCase instead of ProviderController",
        )
    }

    @Test
    fun `runtime stops tools through ToolCase`() {
        val runtimeText = File("src/main/kotlin/com/heyanle/priestess/bot/PriestessRuntime.kt").readText()

        assertTrue(runtimeText.contains("toolCase: ToolCase"))
        assertTrue(runtimeText.contains("StopStep(\"tools\") { toolCase.stop() }"))
        assertFalse(
            runtimeText.contains("ToolController"),
            "Runtime must depend on ToolCase instead of ToolController",
        )
    }

    @Test
    fun `runtime stops skills through SkillCase`() {
        val runtimeText = File("src/main/kotlin/com/heyanle/priestess/bot/PriestessRuntime.kt").readText()

        assertTrue(runtimeText.contains("skillCase: SkillCase"))
        assertTrue(runtimeText.contains("StopStep(\"skills\") { skillCase.stop() }"))
        assertFalse(
            runtimeText.contains("SkillController"),
            "Runtime must depend on SkillCase instead of SkillController",
        )
    }

    @Test
    fun `production modules use ToolCase instead of ToolExecutor across module boundary`() {
        val sourceRoot = File("src/main/kotlin")
        val violations = sourceRoot
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.invariantSeparatorsPath.contains("/tool/") }
            .filterNot { it.invariantSeparatorsPath.endsWith("/core/di/CoreModule.kt") }
            .filter { file -> file.readText().contains("ToolExecutor") }
            .map { it.invariantSeparatorsPath }
            .toList()

        assertTrue(
            violations.isEmpty(),
            "Production modules must execute tools through ToolCase, not ToolExecutor: $violations",
        )
    }

    @Test
    fun `runtime stops workspace through WorkspaceCase`() {
        val runtimeText = File("src/main/kotlin/com/heyanle/priestess/bot/PriestessRuntime.kt").readText()

        assertTrue(runtimeText.contains("workspaceCase: WorkspaceCase"))
        assertTrue(runtimeText.contains("StopStep(\"workspace\") { workspaceCase.stop() }"))
        assertFalse(
            runtimeText.contains("WorkspaceController"),
            "Runtime must depend on WorkspaceCase instead of WorkspaceController",
        )
    }

    @Test
    fun `runtime stops observability through ObservabilityCase`() {
        val runtimeText = File("src/main/kotlin/com/heyanle/priestess/bot/PriestessRuntime.kt").readText()

        assertTrue(runtimeText.contains("observabilityCase: ObservabilityCase"))
        assertTrue(runtimeText.contains("StopStep(\"observability\") { observabilityCase.stop() }"))
        assertFalse(
            runtimeText.contains("ObservabilityController"),
            "Runtime must depend on ObservabilityCase instead of ObservabilityController",
        )
    }

    @Test
    fun `runtime stops database through DatabaseCase`() {
        val runtimeText = File("src/main/kotlin/com/heyanle/priestess/bot/PriestessRuntime.kt").readText()

        assertTrue(runtimeText.contains("databaseCase: DatabaseCase"))
        assertTrue(runtimeText.contains("StopStep(\"database\") { databaseCase.stop() }"))
        assertFalse(
            runtimeText.contains("DatabaseController"),
            "Runtime must depend on DatabaseCase instead of DatabaseController",
        )
    }

    @Test
    fun `runtime stops config through ConfigCase`() {
        val runtimeText = File("src/main/kotlin/com/heyanle/priestess/bot/PriestessRuntime.kt").readText()

        assertTrue(runtimeText.contains("configCase: ConfigCase"))
        assertTrue(runtimeText.contains("StopStep(\"config\") { configCase.stop() }"))
        assertFalse(
            runtimeText.contains("ConfigController"),
            "Runtime must depend on ConfigCase instead of ConfigController",
        )
    }

    @Test
    fun `PriestessBotServer stays inside server module and DI assembly`() {
        val sourceRoot = File("src/main/kotlin")
        val violations = sourceRoot
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.invariantSeparatorsPath.contains("/server/") }
            .filterNot { it.invariantSeparatorsPath.endsWith("/core/di/CoreModule.kt") }
            .filter { file -> file.readText().contains("PriestessBotServer") }
            .map { it.invariantSeparatorsPath }
            .toList()

        assertTrue(
            violations.isEmpty(),
            "Only server module and DI assembly may depend on PriestessBotServer: $violations",
        )
    }

    @Test
    fun `core module resolves platform status through PlatformCase without dependency cycle`() = runBlocking {
        val previousConfigPath = System.getProperty("priestess.config.path")
        val runDir = Files.createTempDirectory("priestess-core-module")
        val configPath = runDir.resolve("config.json").toAbsolutePath().toString()
        Files.writeString(
            runDir.resolve("config.json"),
            Json.encodeToString(
                PriestessConfig(
                    database = DatabaseConfig(path = runDir.resolve("priestess.sqlite").toString()),
                    plugins = PluginConfig(directory = runDir.resolve("plugins").toString(), autoDiscover = false),
                ),
            ),
        )
        System.setProperty("priestess.config.path", configPath)
        val app = startKoin {
            modules(coreModule)
        }
        try {
            val service = app.koin.get<DashboardService>()
            val healthProvider = app.koin.get<RuntimeHealthProvider>()

            assertEquals(emptyList(), service.platforms())
            assertEquals("0", healthProvider.snapshot().diagnostics["runningPlatforms"])
        } finally {
            runCatching { app.koin.get<PlatformController>().stop() }
            stopKoin()
            if (previousConfigPath == null) {
                System.clearProperty("priestess.config.path")
            } else {
                System.setProperty("priestess.config.path", previousConfigPath)
            }
        }
    }

    @Test
    fun `ConfigCase update pushes segmented state flows`() {
        val configController = ConfigController(path = tempConfigPath())
        val configCase = ConfigCase(configController)

        configCase.update {
            it.copy(
                platforms = listOf(
                    PlatformConfig(name = "flow-platform", type = "flow-platform"),
                ),
            )
        }

        assertEquals("flow-platform", configCase.platformConfigsFlow.value.single().name)
        assertEquals(configCase.current().database, configCase.databaseConfigFlow.value)
        assertEquals(configCase.current().pipeline, configCase.pipelineConfigFlow.value)
    }

    @Test
    fun `ConfigCase save reloads updated segmented config`() {
        val path = tempConfigPath()
        val configCase = ConfigCase(ConfigController(path = path))

        val updated = configCase.update {
            it.copy(
                pipeline = it.pipeline.copy(
                    wakingPrefix = "!",
                    rateLimitEnabled = false,
                ),
                platforms = listOf(
                    PlatformConfig(name = "persisted-platform", type = "napcat"),
                ),
            )
        }
        configCase.save(updated)

        val reloaded = ConfigCase(ConfigController(path = path))

        assertEquals("!", reloaded.pipelineConfigFlow.value.wakingPrefix)
        assertFalse(reloaded.pipelineConfigFlow.value.rateLimitEnabled)
        assertEquals("persisted-platform", reloaded.platformConfigsFlow.value.single().name)
    }

    @Test
    fun `PlatformController starts platform lazily and message loads pipeline case`() = runBlocking {
        val configController = ConfigController(path = tempConfigPath())
        val configCase = ConfigCase(configController)
        val received = CompletableDeferred<MessageEvent>()
        var pipelineLoaded = false

        PlatformRegistry.registerMeta(
            metadata = PlatformMetadata(
                name = "lazy-platform",
                displayName = "Lazy Platform",
                supportStreaming = false,
                supportProactiveMessage = false,
            ),
            factory = { PublishingPlatform("lazy-platform") },
        )

        val platformCase = PlatformCase {
            pipelineLoaded = true
            PipelineCase(PipelineController(listOf(CapturingStage(received)), Unit))
        }
        val platformController = PlatformController(configCase, platformCase)

        configCase.update {
            it.copy(
                platforms = listOf(
                    PlatformConfig(name = "lazy-platform", type = "lazy-platform"),
                ),
            )
        }

        val platform = withTimeout(1_000) {
            var current: Platform? = null
            while (current == null) {
                current = platformController.get("lazy-platform")
                delay(10)
            }
            current as PublishingPlatform
        }

        assertFalse(pipelineLoaded)

        val event = MessageEvent(
            platform = platform,
            session = MessageSession(
                id = "session-1",
                type = SessionType.PRIVATE,
                platformName = platform.metadata.name,
            ),
            chain = MessageChain.text("hello"),
        )
        platform.publish(event)

        assertEquals(event, withTimeout(1_000) { received.await() })
        assertTrue(pipelineLoaded)

        platformController.stop()
    }

    @Test
    fun `WhitelistCheck blocks bot at mention when sender and group are not whitelisted`() = runBlocking {
        val stage = WhitelistCheckStage(
            PipelineConfig(
                whitelistEnabled = true,
                whitelistUsers = listOf("allowed-user"),
                whitelistGroups = listOf("allowed-group"),
            ),
        )
        val ctx = PipelineContext(
            MessageEvent(
                platform = PublishingPlatform("napcat4_18_6"),
                session = MessageSession(
                    id = "unlisted-group",
                    type = SessionType.GROUP,
                    platformName = "napcat4_18_6",
                    metadata = mapOf("senderId" to "unlisted-user", "selfId" to "3334969096"),
                ),
                chain = MessageChain(
                    listOf(
                        MessageComponent.At("3334969096"),
                        MessageComponent.Text(" hello"),
                    ),
                ),
            ),
        )

        stage.process(ctx)

        assertTrue(ctx.isStopped)
    }

    @Test
    fun `WhitelistCheck blocks non-whitelisted message when at mention targets another user`() = runBlocking {
        val stage = WhitelistCheckStage(
            PipelineConfig(
                whitelistEnabled = true,
                whitelistUsers = listOf("allowed-user"),
                whitelistGroups = listOf("allowed-group"),
            ),
        )
        val ctx = PipelineContext(
            MessageEvent(
                platform = PublishingPlatform("napcat4_18_6"),
                session = MessageSession(
                    id = "unlisted-group",
                    type = SessionType.GROUP,
                    platformName = "napcat4_18_6",
                    metadata = mapOf("senderId" to "unlisted-user", "selfId" to "3334969096"),
                ),
                chain = MessageChain(
                    listOf(
                        MessageComponent.At("111111111"),
                        MessageComponent.Text(" hello"),
                    ),
                ),
            ),
        )

        stage.process(ctx)

        assertTrue(ctx.isStopped)
    }

    @Test
    fun `WakingCheck allows group message when at mention targets bot`() = runBlocking {
        val stage = WakingCheckStage(PipelineConfig(wakingPrefix = "/"))
        val ctx = PipelineContext(
            MessageEvent(
                platform = PublishingPlatform("napcat4_18_6"),
                session = MessageSession(
                    id = "group-1",
                    type = SessionType.GROUP,
                    platformName = "napcat4_18_6",
                    metadata = mapOf("selfId" to "3334969096"),
                ),
                chain = MessageChain(
                    listOf(
                        MessageComponent.At("3334969096"),
                        MessageComponent.Text(" hello"),
                    ),
                ),
            ),
        )

        stage.process(ctx)

        assertFalse(ctx.isStopped)
    }

    @Test
    fun `WakingCheck always allows private message`() = runBlocking {
        val stage = WakingCheckStage(PipelineConfig(wakingPrefix = "/"))
        val ctx = PipelineContext(
            MessageEvent(
                platform = PublishingPlatform("napcat4_18_6"),
                session = MessageSession(
                    id = "user-1",
                    type = SessionType.PRIVATE,
                    platformName = "napcat4_18_6",
                    metadata = mapOf("selfId" to "3334969096"),
                ),
                chain = MessageChain.text("hello without prefix"),
            ),
        )

        stage.process(ctx)

        assertFalse(ctx.isStopped)
    }

    @Test
    fun `WakingCheck blocks group message when at mention targets another user`() = runBlocking {
        val stage = WakingCheckStage(PipelineConfig(wakingPrefix = "/"))
        val ctx = PipelineContext(
            MessageEvent(
                platform = PublishingPlatform("napcat4_18_6"),
                session = MessageSession(
                    id = "group-1",
                    type = SessionType.GROUP,
                    platformName = "napcat4_18_6",
                    metadata = mapOf("selfId" to "3334969096"),
                ),
                chain = MessageChain(
                    listOf(
                        MessageComponent.At("111111111"),
                        MessageComponent.Text(" hello"),
                    ),
                ),
            ),
        )

        stage.process(ctx)

        assertTrue(ctx.isStopped)
    }

    @Test
    fun `message flow reaches pipeline react tool and respond`() = runBlocking {
        val configCase = ConfigCase(ConfigController(path = tempConfigPath()))
        configCase.update {
            it.copy(
                agent = it.agent.copy(
                    model = "fake-model",
                    maxSteps = 3,
                ),
                pipeline = PipelineConfig(
                    wakingPrefix = "",
                    rateLimitEnabled = false,
                    maxHistoryMessages = 10,
                ),
            )
        }

        val database = DatabaseController(tempDbPath())
        val databaseCase = DatabaseCase(database)
        val conversationController = ConversationController(databaseCase)
        val messageHistory = MessageHistory(databaseCase)
        val conversationCase = ConversationCase(conversationController, messageHistory)

        val toolController = ToolController()
        val echoTool = EchoTool()
        toolController.register(echoTool)

        val providerController = ProviderController(configCase)
        val provider = ToolCallingProvider()
        providerController.register(provider)

        val pipelineController = PipelineController(
            configCase = configCase,
            conversationCase = conversationCase,
            agentCase = AgentCase(),
            providerCase = ProviderCase(providerController),
            toolCase = ToolCase(toolController),
            workspaceCase = buildWorkspaceCase(configCase, toolController),
        )
        val platformCase = PlatformCase { PipelineCase(pipelineController) }
        val platform = RecordingPlatform("full-flow")
        platform.setMessageHandler { platformCase.handleIncomingMessage(it) }

        val event = MessageEvent(
            platform = platform,
            session = MessageSession(
                id = "session-full-flow",
                type = SessionType.PRIVATE,
                platformName = platform.metadata.name,
            ),
            chain = MessageChain.text("run the tool"),
        )

        platform.publish(event)

        assertEquals("final after tool", withTimeout(1_000) { platform.sent.await().textContent })
        assertEquals(2, provider.callCount)
        assertEquals("tool-ok", echoTool.lastValue)

        val conversation = conversationController.findByPlatformSession("full-flow", "session-full-flow")
        require(conversation != null)

        withTimeout(1_000) {
            while (messageHistory.getByConversation(conversation.id).size < 4) {
                delay(10)
            }
        }
        val stored = messageHistory.getByConversation(conversation.id)
        assertEquals(listOf(MessageRole.USER, MessageRole.ASSISTANT, MessageRole.TOOL, MessageRole.ASSISTANT), stored.map { it.role })
        assertTrue(stored[1].toolCalls?.contains("echo_tool") == true)
        assertEquals("tool-ok", stored[2].content)
        assertEquals("call-1", stored[2].toolCallId)
        assertEquals("final after tool", stored.last().content)

        pipelineController.stop()
        providerController.stop()
        toolController.stop()
        database.stop()
    }

    @Test
    fun `message flow routes matching prompt to configured sub-agent`() = runBlocking {
        val configCase = ConfigCase(ConfigController(path = tempConfigPath()))
        configCase.update {
            it.copy(
                agent = AgentConfig(name = "primary-agent", providerName = "primary-provider", model = "primary-provider", maxSteps = 3),
                providers = listOf(
                    ProviderConfig(name = "primary-provider", type = "fake", model = "primary-provider"),
                    ProviderConfig(name = "code-provider", type = "fake", model = "code-provider"),
                ),
                subAgents = SubAgentOrchestrationConfig(
                    enabled = true,
                    agents = listOf(
                        SubAgentConfig(
                            name = "code-agent",
                            agent = AgentConfig(name = "code-agent", providerName = "code-provider", model = "code-provider", maxSteps = 3),
                        ),
                    ),
                    routes = listOf(
                        SubAgentRouteConfig(
                            name = "code-route",
                            targetAgentName = "code-agent",
                            keywords = listOf("code"),
                            priority = 10,
                        ),
                    ),
                ),
                pipeline = PipelineConfig(wakingPrefix = "", rateLimitEnabled = false, maxHistoryMessages = 10),
            )
        }

        val primaryProvider = StaticProvider("primary-provider", "primary reply")
        val codeProvider = StaticProvider("code-provider", "code reply")
        val result = runPipelineMessage(configCase, listOf(primaryProvider, codeProvider), "please review code")

        assertEquals("code reply", result.response)
        assertEquals(0, primaryProvider.callCount)
        assertEquals(1, codeProvider.callCount)
        assertEquals("code-agent", result.selectionAgent)
        assertEquals("code-route", result.selectionRoute)
        assertEquals("keyword_match", result.selectionReason)
    }

    @Test
    fun `message flow keeps primary agent when sub-agent routing is disabled`() = runBlocking {
        val configCase = ConfigCase(ConfigController(path = tempConfigPath()))
        configCase.update {
            it.copy(
                agent = AgentConfig(name = "primary-agent", providerName = "primary-provider", model = "primary-provider", maxSteps = 3),
                providers = listOf(
                    ProviderConfig(name = "primary-provider", type = "fake", model = "primary-provider"),
                    ProviderConfig(name = "code-provider", type = "fake", model = "code-provider"),
                ),
                subAgents = SubAgentOrchestrationConfig(
                    enabled = false,
                    agents = listOf(
                        SubAgentConfig(
                            name = "code-agent",
                            agent = AgentConfig(name = "code-agent", providerName = "code-provider", model = "code-provider", maxSteps = 3),
                        ),
                    ),
                    routes = listOf(
                        SubAgentRouteConfig(name = "code-route", targetAgentName = "code-agent", keywords = listOf("code"), priority = 10),
                    ),
                ),
                pipeline = PipelineConfig(wakingPrefix = "", rateLimitEnabled = false, maxHistoryMessages = 10),
            )
        }

        val primaryProvider = StaticProvider("primary-provider", "primary reply")
        val codeProvider = StaticProvider("code-provider", "code reply")
        val result = runPipelineMessage(configCase, listOf(primaryProvider, codeProvider), "please review code")

        assertEquals("primary reply", result.response)
        assertEquals(1, primaryProvider.callCount)
        assertEquals(0, codeProvider.callCount)
        assertEquals("primary-agent", result.selectionAgent)
        assertEquals(null, result.selectionRoute)
        assertEquals("orchestration_disabled", result.selectionReason)
    }

    @Test
    fun `pipeline uses updated sub-agent config for later messages without controller rebuild`() = runBlocking {
        val configCase = ConfigCase(ConfigController(path = tempConfigPath()))
        configCase.update {
            it.copy(
                agent = AgentConfig(name = "primary-agent", providerName = "primary-provider", model = "primary-provider", maxSteps = 3),
                providers = listOf(
                    ProviderConfig(name = "primary-provider", type = "fake", model = "primary-provider"),
                    ProviderConfig(name = "code-provider", type = "fake", model = "code-provider"),
                ),
                subAgents = SubAgentOrchestrationConfig(enabled = false),
                pipeline = PipelineConfig(wakingPrefix = "", rateLimitEnabled = false, maxHistoryMessages = 10),
            )
        }

        val primaryProvider = StaticProvider("primary-provider", "primary reply")
        val codeProvider = StaticProvider("code-provider", "code reply")
        val runtime = RuntimePipelineFixture(configCase, listOf(primaryProvider, codeProvider))

        try {
            val first = runtime.process("please review code", "session-hot-1")
            configCase.update {
                it.copy(
                    subAgents = SubAgentOrchestrationConfig(
                        enabled = true,
                        agents = listOf(
                            SubAgentConfig(
                                name = "code-agent",
                                agent = AgentConfig(name = "code-agent", providerName = "code-provider", model = "code-provider", maxSteps = 3),
                            ),
                        ),
                        routes = listOf(
                            SubAgentRouteConfig(name = "code-route", targetAgentName = "code-agent", keywords = listOf("code"), priority = 10),
                        ),
                    ),
                )
            }
            val second = runtime.process("please review code", "session-hot-2")

            assertEquals("primary reply", first.response)
            assertEquals("code reply", second.response)
            assertEquals(1, primaryProvider.callCount)
            assertEquals(1, codeProvider.callCount)
        } finally {
            runtime.stop()
        }
    }

    @Test
    fun `BaseController task exceptions do not cancel sibling tasks`() = runBlocking {
        val controller = TestController()
        val siblingCompleted = CompletableDeferred<Unit>()

        controller.startFailingTask()
        controller.startSiblingTask(siblingCompleted)

        withTimeout(1_000) { siblingCompleted.await() }
        assertTrue(siblingCompleted.isCompleted)

        controller.stop()
    }

    private fun tempConfigPath(): String {
        return Files.createTempFile("priestess-config", ".json").toAbsolutePath().toString()
    }

    private fun tempDbPath(): String {
        return Files.createTempFile("priestess-db", ".sqlite").toAbsolutePath().toString()
    }

    private suspend fun runPipelineMessage(
        configCase: ConfigCase,
        providers: List<StaticProvider>,
        message: String,
    ): PipelineRunResult {
        val database = DatabaseController(tempDbPath())
        val databaseCase = DatabaseCase(database)
        val conversationController = ConversationController(databaseCase)
        val conversationCase = ConversationCase(conversationController, MessageHistory(databaseCase))
        val toolController = ToolController()
        val providerController = ProviderController(configCase)
        providers.forEach(providerController::register)
        val providerCase = ProviderCase(providerController)
        val agentCase = AgentCase()
        val subAgentOrchestrator = SubAgentOrchestrator(
            agentCase = agentCase,
            providerCase = providerCase,
            toolCase = ToolCase(toolController),
        )
        val captureStage = SelectionCaptureStage()
        val pipelineController = PipelineController(
            listOf(
                PinWorkspaceStage(configCase),
                PreProcessStage(
                    agentConfig = configCase.current().agent,
                    subAgentConfig = configCase.current().subAgents,
                    pipelineConfig = configCase.current().pipeline,
                    conversationCase = conversationCase,
                    agentCase = agentCase,
                    subAgentOrchestrator = subAgentOrchestrator,
                ),
                ProcessStage(
                    agentCase = agentCase,
                    providerCase = providerCase,
                    toolCase = ToolCase(toolController),
                ),
                ResultDecorateStage(),
                captureStage,
                RespondStage(),
            ),
            Unit,
        )
        val platform = RecordingPlatform("sub-agent-flow")
        platform.setMessageHandler { pipelineController.process(it).join() }
        val event = MessageEvent(
            platform = platform,
            session = MessageSession(
                id = "session-sub-agent-flow",
                type = SessionType.PRIVATE,
                platformName = platform.metadata.name,
            ),
            chain = MessageChain.text(message),
        )

        try {
            platform.publish(event)
            val response = withTimeout(1_000) { platform.sent.await().textContent }
            val ctx = withTimeout(1_000) { captureStage.captured.await() }
            return PipelineRunResult(
                response = response,
                selectionAgent = ctx.shared["subAgentSelectionAgent"] as? String,
                selectionRoute = ctx.shared["subAgentSelectionRoute"] as? String,
                selectionReason = ctx.shared["subAgentSelectionReason"] as? String,
            )
        } finally {
            pipelineController.stop()
            providerController.stop()
            toolController.stop()
            database.stop()
        }
    }

    private data class RuntimePipelineResult(
        val response: String,
    )

    private inner class RuntimePipelineFixture(
        configCase: ConfigCase,
        providers: List<StaticProvider>,
    ) {
        private val database = DatabaseController(tempDbPath())
        private val databaseCase = DatabaseCase(database)
        private val conversationCase = ConversationCase(ConversationController(databaseCase), MessageHistory(databaseCase))
        private val toolController = ToolController()
        private val providerController = ProviderController(configCase)
        private val providerCase = ProviderCase(providerController)
        private val agentCase = AgentCase()
        private val subAgentOrchestrator = SubAgentOrchestrator(
            agentCase = agentCase,
            providerCase = providerCase,
            toolCase = ToolCase(toolController),
        )
        private val pipelineController = PipelineController(
            configCase = configCase,
            conversationCase = conversationCase,
            agentCase = agentCase,
            providerCase = providerCase,
            toolCase = ToolCase(toolController),
            subAgentOrchestrator = subAgentOrchestrator,
            workspaceCase = buildWorkspaceCase(configCase, toolController),
        )

        init {
            providers.forEach(providerController::register)
        }

        suspend fun process(message: String, sessionId: String): RuntimePipelineResult {
            val platform = RecordingPlatform("sub-agent-flow")
            platform.setMessageHandler { pipelineController.process(it).join() }
            val event = MessageEvent(
                platform = platform,
                session = MessageSession(
                    id = sessionId,
                    type = SessionType.PRIVATE,
                    platformName = platform.metadata.name,
                ),
                chain = MessageChain.text(message),
            )
            platform.publish(event)
            return RuntimePipelineResult(withTimeout(1_000) { platform.sent.await().textContent })
        }

        suspend fun stop() {
            pipelineController.stop()
            providerController.stop()
            toolController.stop()
            database.stop()
        }
    }

    private data class PipelineRunResult(
        val response: String,
        val selectionAgent: String?,
        val selectionRoute: String?,
        val selectionReason: String?,
    )

    private class PublishingPlatform(name: String) : Platform() {
        override val metadata = PlatformMetadata(
            name = name,
            displayName = name,
            supportStreaming = false,
            supportProactiveMessage = false,
        )

        val job = Job()

        override suspend fun run(): Job = job
        override suspend fun terminate() = Unit
        override suspend fun sendMessage(session: MessageSession, chain: MessageChain): String? = null

        suspend fun publish(event: MessageEvent) {
            commitEvent(event)
        }
    }

    private class CapturingStage(
        private val received: CompletableDeferred<MessageEvent>,
    ) : Stage {
        override val name = "Capturing"
        override val order = StageOrder.WAKING_CHECK

        override suspend fun process(ctx: PipelineContext): Flow<Unit>? {
            received.complete(ctx.event)
            return null
        }
    }

    private class SelectionCaptureStage : Stage {
        val captured = CompletableDeferred<PipelineContext>()
        override val name = "SelectionCapture"
        override val order = StageOrder.RESULT_DECORATE

        override suspend fun process(ctx: PipelineContext): Flow<Unit>? {
            captured.complete(ctx)
            return null
        }
    }

    private class PinWorkspaceStage(
        private val configCase: ConfigCase,
    ) : Stage {
        override val name = "PinWorkspace"
        override val order = StageOrder.PREPARE_WORKSPACE

        override suspend fun process(ctx: PipelineContext): Flow<Unit>? {
            ctx.pinWorkspace(defaultWorkspaceResolution(configCase.current()))
            return null
        }
    }

    private class RecordingPlatform(name: String) : Platform() {
        override val metadata = PlatformMetadata(
            name = name,
            displayName = name,
            supportStreaming = false,
            supportProactiveMessage = false,
        )

        val sent = CompletableDeferred<MessageChain>()

        override suspend fun run(): Job = Job()
        override suspend fun terminate() = Unit

        override suspend fun sendMessage(session: MessageSession, chain: MessageChain): String? {
            sent.complete(chain)
            return null
        }

        suspend fun publish(event: MessageEvent) {
            commitEvent(event)
        }
    }

    private companion object {
        fun buildWorkspaceCase(
            configCase: ConfigCase,
            toolController: ToolController,
        ): WorkspaceCase {
            return WorkspaceCase(
                WorkspaceController(
                    source = ConfigBackedWorkspaceConfigSource(configCase),
                    toolCase = ToolCase(toolController),
                    nowProvider = { 1_000L },
                ),
            )
        }

        fun defaultWorkspaceResolution(config: PriestessConfig = PriestessConfig()): WorkspaceResolution {
            return WorkspaceResolution(
                snapshot = WorkspaceSnapshot(
                    id = "default",
                    name = "Default",
                    enabled = true,
                    version = 1,
                    loadedAt = 1_000L,
                    rootDir = "/tmp/workspace",
                    config = WorkspaceConfig(
                        id = "default",
                        name = "Default",
                        isDefault = true,
                        agents = listOf(config.agent),
                        providerName = config.agent.providerName,
                        subAgents = config.subAgents,
                    ),
                    agentConfigs = listOf(config.agent),
                    providerName = config.agent.providerName,
                    toolNames = emptyList(),
                    skillDescriptors = emptyList(),
                    skillSettings = emptyMap(),
                    mcpServers = emptyList(),
                    personaIds = emptyList(),
                    memoryPolicy = WorkspaceMemoryPolicyConfig(),
                ),
                reason = "test workspace",
            )
        }
    }

    private class ToolCallingProvider : ChatProvider {
        override val metadata = ProviderMetadata(
            name = "fake-model",
            displayName = "Fake Model",
            kind = LLMKind.OPENAI,
            supportToolCalling = true,
            supportVision = false,
            supportStreaming = false,
        )
        override val config = com.heyanle.priestess.bot.config.ProviderConfig(
            name = "fake-model",
            type = "fake",
            model = "fake-model",
        )
        var callCount = 0
            private set

        override suspend fun textChat(request: LLMRequest): LLMResponse {
            callCount += 1
            return if (callCount == 1) {
                LLMResponse(
                    content = "need tool",
                    toolCalls = listOf(
                        ToolCall(
                            id = "call-1",
                            name = "echo_tool",
                            arguments = """{"value":"tool-ok"}""",
                        ),
                    ),
                )
            } else {
                LLMResponse(content = "final after tool")
            }
        }

        override suspend fun getModels(): List<String> = listOf("fake-model")
        override suspend fun test(): Boolean = true
    }

    private class StaticProvider(
        name: String,
        private val content: String,
    ) : ChatProvider {
        override val metadata = ProviderMetadata(
            name = name,
            displayName = name,
            kind = LLMKind.OPENAI,
            supportToolCalling = false,
            supportVision = false,
            supportStreaming = false,
        )
        override val config = ProviderConfig(name = name, type = "fake", model = name)
        var callCount = 0
            private set

        override suspend fun textChat(request: LLMRequest): LLMResponse {
            callCount += 1
            return LLMResponse(content = content)
        }

        override suspend fun getModels(): List<String> = listOf(metadata.name)
        override suspend fun test(): Boolean = true
    }

    private class EchoTool : FunctionTool() {
        override val schema = ToolSchema(
            name = "echo_tool",
            description = "Echoes the provided value.",
        )
        var lastValue: String? = null
            private set

        override suspend fun execute(context: AgentToolContext, args: Map<String, String>): ToolResult {
            lastValue = args["value"]
            return ToolResult.success(lastValue ?: "")
        }
    }

    private class TestController : BaseController("TestController") {
        fun startFailingTask() {
            launchTask("failing") {
                error("expected")
            }
        }

        fun startSiblingTask(done: CompletableDeferred<Unit>) {
            launchTask("sibling") {
                delay(50)
                done.complete(Unit)
            }
        }
    }
}
