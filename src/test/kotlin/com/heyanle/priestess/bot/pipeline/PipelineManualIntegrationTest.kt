package com.heyanle.priestess.bot.pipeline

import com.heyanle.priestess.bot.config.AgentConfig
import com.heyanle.priestess.bot.config.DatabaseConfig
import com.heyanle.priestess.bot.config.PipelineConfig
import com.heyanle.priestess.bot.config.PlatformConfig
import com.heyanle.priestess.bot.config.PriestessConfig
import com.heyanle.priestess.bot.config.ProviderConfig
import com.heyanle.priestess.bot.core.di.coreModule
import com.heyanle.priestess.bot.platform.PlatformController
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import java.nio.file.Path
import java.nio.file.Files
import kotlin.test.Test

class PipelineManualIntegrationTest {

    private val logger = KotlinLogging.logger {}

    @Test
    fun `start real NapCat to OpenAI pipeline for manual verification`(): Unit = runBlocking {
        assumeTrue(enabled(), "Set PRIESTESS_PIPELINE_MANUAL_ENABLED=true to start the manual pipeline test.")

        val napcatToken = requiredEnv("NAPCAT_ACCESS_TOKEN")
        val openAiApiKey = requiredEnv("PRIESTESS_OPENAI_PROVIDER_API_KEY")

        val tempDir = resolveRunDirectory()
        val configPath = tempDir.resolve("config.json").toAbsolutePath().toString()
        val dbPath = tempDir.resolve("pipeline-manual.sqlite").toAbsolutePath().toString()

        val previousConfigPath = System.getProperty("priestess.config.path")
        System.setProperty("priestess.config.path", configPath)

        Files.writeString(
            tempDir.resolve("config.json"),
            Json {
                prettyPrint = true
                encodeDefaults = true
            }.encodeToString(
                PriestessConfig(
                    platforms = listOf(
                        PlatformConfig(
                            name = env("NAPCAT_PLATFORM_NAME", "napcat4_18_6"),
                            type = "napcat4_18_6",
                            host = env("NAPCAT_WS_HOST", "192.168.31.24"),
                            port = env("NAPCAT_HTTP_PORT", "10000").toInt(),
                            wsPort = env("NAPCAT_WS_PORT", "10001").toInt(),
                            token = napcatToken,
                            useWs = true,
                        ),
                    ),
                    providers = listOf(
                        ProviderConfig(
                            name = env("PRIESTESS_OPENAI_PROVIDER_NAME", "deepseek-v4-flash"),
                            type = "openai",
                            model = env("PRIESTESS_OPENAI_PROVIDER_MODEL", "deepseek-v4-flash"),
                            baseUrl = env(
                                "PRIESTESS_OPENAI_PROVIDER_URL",
                                "http://192.168.31.24:8090/v1/chat/completions",
                            ),
                            apiKey = openAiApiKey,
                        ),
                    ),
                    agent = AgentConfig(
                        name = "pipeline-manual-agent",
                        instructions = env(
                            "PRIESTESS_PIPELINE_MANUAL_PROMPT",
                            "You are PriestessBot in a manual integration test. Reply briefly and clearly.",
                        ),
                        model = env("PRIESTESS_OPENAI_PROVIDER_MODEL", "deepseek-v4-flash"),
                        maxSteps = env("PRIESTESS_PIPELINE_MANUAL_MAX_STEPS", "6").toInt(),
                    ),
                    database = DatabaseConfig(path = dbPath),
                    pipeline = PipelineConfig(
                        wakingPrefix = env("PRIESTESS_PIPELINE_MANUAL_PREFIX", "/"),
                        rateLimitEnabled = false,
                        whitelistEnabled = true,
                        whitelistUsers = listOf("1371735400"),
                        whitelistGroups = listOf("757063076", "729848189"),
                        maxHistoryMessages = 10,
                    ),
                ),
            ),
        )

        val app = startKoin {
            modules(coreModule)
        }

        var platformController: PlatformController? = null
        try {
            platformController = app.koin.get<PlatformController>()
            logger.info {
                "[PIPELINE-000] Manual pipeline is running until Ctrl+C. " +
                    "Send a private NapCat message, or a group message with prefix " +
                    "'${env("PRIESTESS_PIPELINE_MANUAL_PREFIX", "/")}'. " +
                    "Whitelist user=1371735400 group=757063076. " +
                    "Run dir: ${tempDir.toAbsolutePath()}. Config path: $configPath"
            }
            awaitCancellation()
        } finally {
            platformController?.stop()
            stopKoin()
            if (previousConfigPath == null) {
                System.clearProperty("priestess.config.path")
            } else {
                System.setProperty("priestess.config.path", previousConfigPath)
            }
        }
    }

    private fun enabled(): Boolean {
        return env("PRIESTESS_PIPELINE_MANUAL_ENABLED", "false") == "true"
    }

    private fun requiredEnv(name: String): String {
        val value = System.getenv(name).orEmpty()
        assumeTrue(value.isNotBlank(), "Set $name before running the manual pipeline test.")
        return value
    }

    private fun env(name: String, default: String): String {
        return System.getenv(name)?.takeIf { it.isNotBlank() } ?: default
    }

    private fun resolveRunDirectory(): Path {
        val configured = System.getenv("PRIESTESS_PIPELINE_MANUAL_LOG_DIR")
            ?.takeIf { it.isNotBlank() }
        val dir = if (configured == null) {
            Files.createTempDirectory("priestess-pipeline-manual")
        } else {
            Path.of(configured)
        }
        Files.createDirectories(dir)
        return dir
    }
}
