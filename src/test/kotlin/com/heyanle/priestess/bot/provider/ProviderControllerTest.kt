package com.heyanle.priestess.bot.provider

import com.heyanle.priestess.bot.config.ConfigCase
import com.heyanle.priestess.bot.config.ConfigController
import com.heyanle.priestess.bot.config.PriestessConfig
import com.heyanle.priestess.bot.config.ProviderConfig
import com.heyanle.priestess.bot.provider.model.LLMRequest
import com.heyanle.priestess.bot.provider.model.LLMResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ProviderControllerTest {
    @Test
    fun `provider config updates add and replace config-backed providers`() = runBlocking {
        ProviderRegistry.register(testMetadata("config-provider")) { config -> StaticProvider(config) }
        var controller: ProviderController? = null
        try {
            val configCase = testConfigCase(
                listOf(
                    ProviderConfig(name = "config-provider", type = "config-provider", model = "model-a"),
                ),
            )
            controller = ProviderController(configCase)

            assertEquals("model-a", controller.getByName("config-provider")?.config?.model)

            configCase.update {
                it.copy(
                    providers = listOf(
                        ProviderConfig(name = "config-provider", type = "config-provider", model = "model-b"),
                        ProviderConfig(name = "extra-provider", type = "config-provider", model = "model-c"),
                    ),
                )
            }

            eventually {
                assertEquals("model-b", controller.getByName("config-provider")?.config?.model)
                assertEquals("model-c", controller.getByName("extra-provider")?.config?.model)
            }
        } finally {
            controller?.stop()
            ProviderRegistry.unregister("config-provider")
        }
    }

    @Test
    fun `disabled or removed config providers disappear after config update`() = runBlocking {
        ProviderRegistry.register(testMetadata("disposable-provider")) { config -> StaticProvider(config) }
        var controller: ProviderController? = null
        try {
            val configCase = testConfigCase(
                listOf(
                    ProviderConfig(name = "disposable-provider", type = "disposable-provider", model = "enabled"),
                ),
            )
            controller = ProviderController(configCase)

            assertNotNull(controller.getByName("disposable-provider"))

            configCase.update {
                it.copy(
                    providers = listOf(
                        ProviderConfig(name = "disposable-provider", type = "disposable-provider", model = "disabled", enabled = false),
                    ),
                )
            }

            eventually {
                assertNull(controller.getByName("disposable-provider"))
            }

            configCase.update { it.copy(providers = emptyList()) }
            eventually {
                assertNull(controller.getByName("disposable-provider"))
            }
        } finally {
            controller?.stop()
            ProviderRegistry.unregister("disposable-provider")
        }
    }

    @Test
    fun `runtime registered providers survive config refresh until explicitly unregistered`() = runBlocking {
        ProviderRegistry.register(testMetadata("config-only-provider")) { config -> StaticProvider(config) }
        var controller: ProviderController? = null
        try {
            val configCase = testConfigCase(
                listOf(
                    ProviderConfig(name = "config-only-provider", type = "config-only-provider", model = "config"),
                ),
            )
            controller = ProviderController(configCase)
            val runtimeProvider = StaticProvider(ProviderConfig(name = "runtime-provider", type = "runtime", model = "runtime"))
            controller.register(runtimeProvider)

            configCase.update { it.copy(providers = emptyList()) }

            eventually {
                assertNull(controller.getByName("config-only-provider"))
                assertEquals(runtimeProvider, controller.getByName("runtime-provider"))
            }

            controller.unregister("runtime-provider")
            assertNull(controller.getByName("runtime-provider"))
        } finally {
            controller?.stop()
            ProviderRegistry.unregister("config-only-provider")
        }
    }

    @Test
    fun `builtin anthropic and gemini providers materialize from config type`() {
        registerBuiltinProviders()

        val anthropic = ProviderRegistry.createFromConfig(
            ProviderConfig(name = "claude", type = "anthropic", model = "claude-sonnet-4-5", apiKey = "test"),
        )
        val gemini = ProviderRegistry.createFromConfig(
            ProviderConfig(name = "gemini-main", type = "gemini", model = "gemini-2.5-flash", apiKey = "test"),
        )

        assertNotNull(anthropic)
        assertNotNull(gemini)
        assertEquals(LLMKind.ANTHROPIC, anthropic.metadata.kind)
        assertEquals(LLMKind.GEMINI, gemini.metadata.kind)
        assertEquals("claude", anthropic.metadata.name)
        assertEquals("gemini-main", gemini.metadata.name)
    }

    private suspend fun eventually(assertion: () -> Unit) {
        withTimeout(1_000) {
            while (true) {
                try {
                    assertion()
                    return@withTimeout
                } catch (e: AssertionError) {
                    delay(10)
                }
            }
        }
    }

    private fun testConfigCase(providers: List<ProviderConfig>): ConfigCase {
        val path = Files.createTempDirectory("provider-controller-test").resolve("config.json")
        val controller = ConfigController(path.toString())
        controller.replace(PriestessConfig(providers = providers))
        return ConfigCase(controller)
    }

    private class StaticProvider(
        override val config: ProviderConfig,
    ) : ChatProvider {
        override val metadata = testMetadata(config.name)

        override suspend fun textChat(request: LLMRequest): LLMResponse {
            return LLMResponse(content = config.model)
        }

        override suspend fun getModels(): List<String> = listOf(config.model)

        override suspend fun test(): Boolean = true
    }

    companion object {
        private fun testMetadata(name: String): ProviderMetadata {
            return ProviderMetadata(
                name = name,
                displayName = name,
                kind = LLMKind.OPENAI,
                supportToolCalling = false,
                supportVision = false,
                supportStreaming = false,
            )
        }
    }
}
