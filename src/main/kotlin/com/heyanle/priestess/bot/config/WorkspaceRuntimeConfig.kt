package com.heyanle.priestess.bot.config

import kotlinx.serialization.Serializable

/**
 * Workspace 运行时配置，提供目录化 workspace 的默认路径入口。
 */
@Serializable
data class WorkspaceRuntimeConfig(
    val defaultDir: String = "",
)
