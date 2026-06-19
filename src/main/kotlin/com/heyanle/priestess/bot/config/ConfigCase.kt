package com.heyanle.priestess.bot.config

class ConfigCase(
    private val controller: ConfigController,
) {
    val configFlow = controller.configFlow
    val databaseConfigFlow = controller.databaseConfigFlow
    val platformConfigsFlow = controller.platformConfigsFlow
    val providerConfigsFlow = controller.providerConfigsFlow
    val agentConfigFlow = controller.agentConfigFlow
    val pipelineConfigFlow = controller.pipelineConfigFlow

    fun current(): PriestessConfig = controller.current()

    fun update(transform: (PriestessConfig) -> PriestessConfig): PriestessConfig {
        return controller.update(transform)
    }

    fun save(config: PriestessConfig = current()) {
        controller.save(config)
    }
}
