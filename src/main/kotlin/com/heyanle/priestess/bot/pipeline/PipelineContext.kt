package com.heyanle.priestess.bot.pipeline

import com.heyanle.priestess.bot.agent.AgentContext
import com.heyanle.priestess.bot.agent.AgentResponse
import com.heyanle.priestess.bot.platform.MessageEvent
import com.heyanle.priestess.bot.workspace.WorkspaceResolution
import com.heyanle.priestess.bot.workspace.WorkspaceSnapshot

/**
 * Pipeline 上下文，贯穿所有阶段，携带：
 * - 原始事件引用
 * - Agent 上下文（PreProcess 阶段创建）
 * - 最终响应（Process 阶段填充）
 * - 装饰后响应（ResultDecorate 阶段填充）
 */
class PipelineContext(
    val event: MessageEvent,
) {
    /** Cross-stage metadata retained for pipeline integrations and diagnostics. */
    val shared: MutableMap<String, Any?> = linkedMapOf()

    /** Agent 执行上下文，由 PreProcessStage 创建 */
    var agentContext: AgentContext? = null

    /** Agent 最终响应，由 ProcessStage 填充 */
    var agentResponse: AgentResponse? = null

    /** 装饰后的响应文本，由 ResultDecorateStage 填充 */
    var decoratedResponse: String? = null

    var directResponse: String? = null

    var permissionGroup: PermissionGroup = PermissionGroup.OPERATOR

    private var workspace: WorkspaceResolution? = null

    val workspaceSnapshot: WorkspaceSnapshot?
        get() = workspace?.snapshot

    val workspaceResolution: WorkspaceResolution?
        get() = workspace

    val workspaceId: String?
        get() = workspace?.snapshot?.id

    val workspaceSnapshotVersion: Long?
        get() = workspace?.snapshot?.version

    val workspaceRootDir: String?
        get() = workspace?.snapshot?.rootDir

    val workspaceResolutionReason: String?
        get() = workspace?.reason

    /** 是否应终止管道（由各阶段设置） */
    val isStopped: Boolean
        get() = event.isStopped.get()

    fun pinWorkspace(resolution: WorkspaceResolution) {
        workspace?.lease?.close()
        workspace = resolution
    }

    fun releaseWorkspace() {
        workspace?.lease?.close()
        workspace = null
    }

    fun stop() {
        event.stopPropagation()
    }

    val isCommandHandled: Boolean
        get() = directResponse != null

    /** 便捷：获取事件文本内容 */
    val textContent: String
        get() = event.chain.textContent

    /** 便捷：获取发送者 ID（从 session metadata 中提取，fallback 为 session id） */
    val senderId: String
        get() = event.session.metadata["senderId"]
            ?: event.session.metadata["userId"]
            ?: event.session.id

    /** 便捷：是否为私聊 */
    val isPrivate: Boolean
        get() = event.session.type == com.heyanle.priestess.bot.platform.SessionType.PRIVATE
}
