package com.heyanle.priestess.bot.pipeline

import com.heyanle.priestess.bot.agent.AgentContext
import com.heyanle.priestess.bot.agent.AgentResponse
import com.heyanle.priestess.bot.platform.MessageEvent
import com.heyanle.priestess.bot.workspace.WorkspaceResolution
import com.heyanle.priestess.bot.workspace.WorkspaceSnapshotLease
import com.heyanle.priestess.bot.workspace.WorkspaceSnapshot

/**
 * Pipeline 上下文，贯穿所有阶段，携带：
 * - 原始事件引用
 * - 阶段间共享数据（通过 [shared] Map）
 * - Agent 上下文（PreProcess 阶段创建）
 * - 最终响应（Process 阶段填充）
 */
class PipelineContext(
    val event: MessageEvent,
) {
    /** 阶段间共享数据（键值对） */
    val shared: MutableMap<String, Any> = mutableMapOf()

    /** Agent 执行上下文，由 PreProcessStage 创建 */
    var agentContext: AgentContext? = null

    /** Agent 最终响应，由 ProcessStage 填充 */
    var agentResponse: AgentResponse? = null

    /** Workspace snapshot pinned for this message, resolved during PreProcess. */
    var workspaceSnapshot: WorkspaceSnapshot? = null
        private set

    var workspaceId: String? = null
        private set

    var workspaceSnapshotVersion: Long? = null
        private set

    var workspaceResolutionReason: String? = null
        private set

    private var workspaceLease: WorkspaceSnapshotLease? = null

    /** 是否应终止管道（由各阶段设置） */
    val isStopped: Boolean
        get() = event.isStopped.get()

    fun pinWorkspace(resolution: WorkspaceResolution) {
        workspaceLease?.close()
        workspaceSnapshot = resolution.snapshot
        workspaceId = resolution.snapshot.id
        workspaceSnapshotVersion = resolution.snapshot.version
        workspaceResolutionReason = resolution.reason
        workspaceLease = resolution.lease
    }

    fun releaseWorkspace() {
        workspaceLease?.close()
        workspaceLease = null
    }

    fun stop() {
        event.stopPropagation()
    }

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
