package com.heyanle.priestess.bot.workspace

import com.heyanle.priestess.bot.config.ConfigCase
import com.heyanle.priestess.bot.config.PriestessConfig

fun interface WorkspaceConfigSource {
    fun load(): WorkspaceConfigSet
}

class ConfigBackedWorkspaceConfigSource(
    private val configProvider: () -> PriestessConfig,
) : WorkspaceConfigSource {
    constructor(configCase: ConfigCase) : this({ configCase.current() })

    override fun load(): WorkspaceConfigSet {
        val config = configProvider()
        return WorkspaceConfigSet(
            defaultWorkspaceDir = config.workspace.defaultDir,
            defaults = WorkspaceRuntimeDefaults(defaultWorkspaceFrom(config)),
        )
    }

    private fun defaultWorkspaceFrom(config: PriestessConfig): WorkspaceConfig {
        return WorkspaceConfig(
            id = WorkspaceConfig.DEFAULT_WORKSPACE_ID,
            name = "Default Workspace",
            enabled = true,
            isDefault = true,
            agents = listOf(config.agent),
            providerName = config.agent.providerName,
            tools = WorkspaceToolConfig(
                enabledTools = config.agent.enabledTools,
                disabledTools = config.agent.disabledTools,
                allowedRiskLevels = config.agent.allowedRiskLevels,
            ),
            subAgents = config.subAgents,
        )
    }
}
