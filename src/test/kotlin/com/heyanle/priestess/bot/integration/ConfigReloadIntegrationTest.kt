package com.heyanle.priestess.bot.integration

import com.heyanle.priestess.bot.agent.AgentCase
import com.heyanle.priestess.bot.config.AgentConfig
import com.heyanle.priestess.bot.config.PipelineConfig
import com.heyanle.priestess.bot.config.PlatformConfig
import com.heyanle.priestess.bot.config.PriestessConfig
import com.heyanle.priestess.bot.config.ProviderConfig
import com.heyanle.priestess.bot.conversation.MessageRole
import com.heyanle.priestess.bot.observability.MetricsRegistry
import com.heyanle.priestess.bot.observability.ObservabilityCase
import com.heyanle.priestess.bot.pipeline.PipelineCase
import com.heyanle.priestess.bot.pipeline.PipelineController
import com.heyanle.priestess.bot.platform.MessageChain
import com.heyanle.priestess.bot.platform.MessageEvent
import com.heyanle.priestess.bot.platform.PlatformCase
import com.heyanle.priestess.bot.platform.PlatformController
import com.heyanle.priestess.bot.platform.PlatformMetadata
import com.heyanle.priestess.bot.platform.PlatformRegistry
import com.heyanle.priestess.bot.provider.ProviderCase
import com.heyanle.priestess.bot.provider.ProviderController
import com.heyanle.priestess.bot.provider.model.LLMResponse
import com.heyanle.priestess.bot.testkit.FakePlatform
import com.heyanle.priestess.bot.testkit.FakeProvider
import com.heyanle.priestess.bot.testkit.testConfigController
import com.heyanle.priestess.bot.testkit.testConversationCase
import com.heyanle.priestess.bot.tool.ToolCase
import com.heyanle.priestess.bot.tool.ToolController
import com.heyanle.priestess.bot.tool.ToolExecutor
import com.heyanle.priestess.bot.workspace.ConfigBackedWorkspaceConfigSource
import com.heyanle.priestess.bot.workspace.WorkspaceCase
import com.heyanle.priestess.bot.workspace.WorkspaceController
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConfigReloadIntegrationTest {
    @Test
    fun `config reload publishes provider platform and pipeline updates for subsequent messages`() = runBlocking {
        val configController = testConfigController(
            PriestessConfig(
                platforms = emptyList(),
                providers = emptyList(),
                agent = AgentConfig(
                    name = "reload-agent",
                    model = "fake-provider",
                    maxSteps = 3,
                ),
                pipeline = PipelineConfig(wakingPrefix = "!", maxHistoryMessages = 1),
            ),
        )
        val configCase = com.heyanle.priestess.bot.config.ConfigCase(configController)
        val fakeProvider = FakeProvider(listOf(LLMResponse(content = "after reload final", finishReason = "stop")))
        val providerController = ProviderController(configCase)
        providerController.register(fakeProvider)
        val conversationCase = testConversationCase("config-reload-integration")
        val toolController = ToolController()
        val metrics = MetricsRegistry()
        val observabilityCase = ObservabilityCase.standalone(metrics)
        val pipelineController = PipelineController(
            configCase = configCase,
            conversationCase = conversationCase,
            agentCase = AgentCase(),
            providerCase = ProviderCase(providerController),
            toolCase = ToolCase(toolController),
            observabilityCase = observabilityCase,
            workspaceCase = WorkspaceCase(
                WorkspaceController(
                    source = ConfigBackedWorkspaceConfigSource(configCase),
                    toolCase = ToolCase(toolController),
                    nowProvider = { 1_000L },
                ),
            ),
        )
        val pipelineCase = PipelineCase(pipelineController)
        val platformController = PlatformController(
            configCase = configCase,
            platformCase = PlatformCase { pipelineCase },
        )

        PlatformRegistry.registerMeta(
            PlatformMetadata(
                name = "reload-fake-platform",
                displayName = "Reload Fake Platform",
                supportStreaming = false,
                supportProactiveMessage = true,
            ),
        ) { FakePlatform() }

        try {
            assertEquals(emptyList(), providerController.getMetaList().map { it.name }.filter { it == "ollama" })
            assertEquals(emptyList(), platformController.getAll())

            configController.replace(
                configController.current().copy(
                    providers = listOf(
                        ProviderConfig(
                            name = "reload-ollama",
                            type = "ollama",
                            model = "llama3",
                            enabled = true,
                        ),
                    ),
                    platforms = listOf(
                        PlatformConfig(
                            name = "reload-fake-platform",
                            type = "reload-fake-platform",
                            enabled = true,
                        ),
                    ),
                    pipeline = configController.current().pipeline.copy(maxHistoryMessages = 3),
                ),
            )
            awaitUntil {
                providerController.getMetaList().any { it.name == "reload-ollama" } &&
                    platformController.getAll().any { it.metadata.name == "fake-platform" }
            }

            assertNotNull(providerController.getByName("reload-ollama"))
            assertTrue(platformController.getAll().any { it.metadata.name == "fake-platform" })

            val platform = FakePlatform()
            val session = FakePlatform.fakeSession()
            val conversation = conversationCase.getOrCreate(platform.metadata.name, session.id)
            conversationCase.storeMessage(conversation.id, MessageRole.USER, "older one")
            conversationCase.storeMessage(conversation.id, MessageRole.ASSISTANT, "older two")
            conversationCase.storeMessage(conversation.id, MessageRole.USER, "recent three")

            val job = pipelineController.process(
                MessageEvent(
                    platform = platform,
                    session = session,
                    chain = MessageChain.text("new question"),
                    messageId = "reload-message-1",
                ),
            )
            job.join()

            assertEquals(1, platform.sentMessages.size)
            assertEquals("after reload final", platform.sentMessages.single().second.textContent)
            val requestedContents = fakeProvider.requests.single().messages.mapNotNull { it.content }
            assertTrue("older one" in requestedContents)
            assertTrue("older two" in requestedContents)
            assertTrue("recent three" in requestedContents)
            assertTrue("new question" in requestedContents)
        } finally {
            platformController.stop()
            pipelineController.stop()
            providerController.stop()
            PlatformRegistry.unregister("reload-fake-platform")
        }
    }

    private suspend fun awaitUntil(
        timeoutMillis: Long = 2_000,
        condition: () -> Boolean,
    ) {
        val started = System.currentTimeMillis()
        while (!condition()) {
            if (System.currentTimeMillis() - started > timeoutMillis) {
                error("Condition was not met within ${timeoutMillis}ms")
            }
            delay(20)
        }
    }
}
