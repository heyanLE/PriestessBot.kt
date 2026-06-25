package com.heyanle.priestess.bot.testkit

import com.heyanle.priestess.bot.config.ConfigCase
import com.heyanle.priestess.bot.config.ConfigController
import com.heyanle.priestess.bot.config.PriestessConfig
import java.nio.file.Files

fun testConfigController(
    config: PriestessConfig = PriestessConfig(),
    prefix: String = "priestess-config",
): ConfigController {
    val path = Files.createTempFile(prefix, ".json").toAbsolutePath().toString()
    return ConfigController(path).also { it.replace(config) }
}

fun testConfigCase(
    config: PriestessConfig = PriestessConfig(),
    prefix: String = "priestess-config",
): ConfigCase = ConfigCase(testConfigController(config, prefix))

data class TestWorkspaceSnapshot(
    val root: String,
    val skills: Map<String, String> = emptyMap(),
    val mcpServers: Map<String, String> = emptyMap(),
    val metadata: Map<String, String> = emptyMap(),
)

fun testWorkspaceSnapshot(
    root: String = Files.createTempDirectory("priestess-workspace").toAbsolutePath().toString(),
    skills: Map<String, String> = emptyMap(),
    mcpServers: Map<String, String> = emptyMap(),
    metadata: Map<String, String> = emptyMap(),
): TestWorkspaceSnapshot = TestWorkspaceSnapshot(
    root = root,
    skills = skills,
    mcpServers = mcpServers,
    metadata = metadata,
)
