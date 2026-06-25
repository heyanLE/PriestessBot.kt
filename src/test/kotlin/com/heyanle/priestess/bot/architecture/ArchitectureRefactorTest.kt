package com.heyanle.priestess.bot.architecture

import com.heyanle.priestess.bot.config.ConfigCase
import com.heyanle.priestess.bot.config.ConfigController
import com.heyanle.priestess.bot.config.PipelineConfig
import com.heyanle.priestess.bot.config.PlatformConfig
import com.heyanle.priestess.bot.conversation.ConversationCase
import com.heyanle.priestess.bot.conversation.ConversationController
import com.heyanle.priestess.bot.conversation.MessageHistory
import com.heyanle.priestess.bot.conversation.MessageRole
import com.heyanle.priestess.bot.core.db.DatabaseController
import com.heyanle.priestess.bot.core.controller.BaseController
import com.heyanle.priestess.bot.agent.AgentCase
import com.heyanle.priestess.bot.agent.context.ContextManager
import com.heyanle.priestess.bot.agent.context.TokenCounter
import com.heyanle.priestess.bot.agent.orchestration.SubAgentOrchestrator
import com.heyanle.priestess.bot.config.AgentConfig
import com.heyanle.priestess.bot.config.ProviderConfig
import com.heyanle.priestess.bot.config.SubAgentConfig
import com.heyanle.priestess.bot.config.SubAgentOrchestrationConfig
import com.heyanle.priestess.bot.config.SubAgentRouteConfig
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
import com.heyanle.priestess.bot.tool.ToolController
import com.heyanle.priestess.bot.tool.ToolExecutor
import com.heyanle.priestess.bot.tool.ToolResult
import com.heyanle.priestess.bot.tool.ToolSchema
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.nio.file.Files
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
        val conversationController = ConversationController(database)
        val messageHistory = MessageHistory(database)
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
            contextManager = ContextManager(TokenCounter()),
            providerCase = ProviderCase(providerController),
            toolExecutor = ToolExecutor(toolController),
            toolController = toolController,
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
        val conversationController = ConversationController(database)
        val conversationCase = ConversationCase(conversationController, MessageHistory(database))
        val toolController = ToolController()
        val providerController = ProviderController(configCase)
        providers.forEach(providerController::register)
        val providerCase = ProviderCase(providerController)
        val agentCase = AgentCase()
        val contextManager = ContextManager(TokenCounter())
        val toolExecutor = ToolExecutor(toolController)
        val subAgentOrchestrator = SubAgentOrchestrator(
            agentCase = agentCase,
            contextManager = contextManager,
            providerCase = providerCase,
            toolExecutor = toolExecutor,
            toolController = toolController,
        )
        val captureStage = SelectionCaptureStage()
        val pipelineController = PipelineController(
            listOf(
                PreProcessStage(
                    agentConfig = configCase.current().agent,
                    subAgentConfig = configCase.current().subAgents,
                    pipelineConfig = configCase.current().pipeline,
                    conversationCase = conversationCase,
                    agentCase = agentCase,
                    contextManager = contextManager,
                    subAgentOrchestrator = subAgentOrchestrator,
                ),
                ProcessStage(
                    providerCase = providerCase,
                    toolExecutor = toolExecutor,
                    toolController = toolController,
                    contextManager = contextManager,
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
        private val conversationCase = ConversationCase(ConversationController(database), MessageHistory(database))
        private val toolController = ToolController()
        private val providerController = ProviderController(configCase)
        private val providerCase = ProviderCase(providerController)
        private val agentCase = AgentCase()
        private val contextManager = ContextManager(TokenCounter())
        private val toolExecutor = ToolExecutor(toolController)
        private val subAgentOrchestrator = SubAgentOrchestrator(
            agentCase = agentCase,
            contextManager = contextManager,
            providerCase = providerCase,
            toolExecutor = toolExecutor,
            toolController = toolController,
        )
        private val pipelineController = PipelineController(
            configCase = configCase,
            conversationCase = conversationCase,
            agentCase = agentCase,
            contextManager = contextManager,
            providerCase = providerCase,
            toolExecutor = toolExecutor,
            toolController = toolController,
            subAgentOrchestrator = subAgentOrchestrator,
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
        override suspend fun sendMessage(session: MessageSession, chain: MessageChain) = Unit

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

        override suspend fun sendMessage(session: MessageSession, chain: MessageChain) {
            sent.complete(chain)
        }

        suspend fun publish(event: MessageEvent) {
            commitEvent(event)
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
