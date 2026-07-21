package com.heyanle.priestess.bot.config

import kotlinx.coroutines.flow.map

/**
 * 配置模块门面，向其他模块提供读取、更新、持久化和备份恢复能力。
 *
 * 该类只依赖配置模块内部的 [ConfigController]，跨模块代码应通过本门面访问配置，
 * 不直接操作 Controller 内部状态。
 */
class ConfigCase(
    private val controller: ConfigController,
) {
    val configFlow = controller.configFlow
    val databaseConfigFlow = controller.databaseConfigFlow
    val platformConfigsFlow = controller.platformConfigsFlow
    val providerConfigsFlow = controller.providerConfigsFlow
    val agentConfigFlow = controller.agentConfigFlow
    val pipelineConfigFlow = controller.pipelineConfigFlow
    val commandConfigFlow = configFlow.map { it.command }
    val permissionConfigFlow = configFlow.map { it.permission }
    val serverConfigFlow = controller.serverConfigFlow
    val pluginConfigFlow = controller.pluginConfigFlow
    val subAgentConfigFlow = controller.subAgentConfigFlow
    val workspaceRuntimeConfigFlow = controller.workspaceRuntimeConfigFlow

    /**
     * 获取当前生效配置。
     *
     * 返回值：内存中最新的完整 [PriestessConfig]。
     */
    fun current(): PriestessConfig = controller.current()

    /**
     * 获取当前配置文件路径。
     *
     * 返回值：配置文件路径字符串。
     */
    fun configPath(): String = controller.configPath()

    /**
     * 按给定转换函数更新内存配置。
     *
     * 参数 [transform]：接收当前配置并返回下一份配置。
     * 返回值：更新后生效的完整配置。
     */
    fun update(transform: (PriestessConfig) -> PriestessConfig): PriestessConfig {
        return controller.update(transform)
    }

    /**
     * 持久化配置到磁盘。
     *
     * 参数 [config]：要写入磁盘的配置，默认使用当前生效配置。
     */
    fun save(config: PriestessConfig = current()) {
        controller.save(config)
    }

    /**
     * 用指定配置替换当前配置，并按需持久化。
     *
     * 参数 [config]：新的完整配置。
     * 参数 [persist]：是否同步写入磁盘。
     * 返回值：替换后生效的完整配置。
     */
    fun replace(config: PriestessConfig, persist: Boolean = true): PriestessConfig {
        return controller.replace(config, persist)
    }

    /**
     * 从磁盘重新加载配置。
     *
     * 返回值：重新加载后生效的完整配置。
     */
    fun reload(): PriestessConfig {
        return controller.reload()
    }

    /**
     * 列出当前配置文件的可用备份。
     *
     * 返回值：按创建时间倒序排列的备份信息列表。
     */
    fun listBackups(): List<ConfigBackup> {
        return controller.listBackups()
    }

    /**
     * 从指定备份恢复配置。
     *
     * 参数 [id]：备份文件名。
     * 返回值：恢复后重新加载的完整配置。
     */
    fun restoreBackup(id: String): PriestessConfig {
        return controller.restoreBackup(id)
    }

    /**
     * 停止配置模块生命周期。
     */
    suspend fun stop() {
        controller.stop()
    }
}
