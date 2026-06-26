package com.heyanle.priestess.bot.workspace

/**
 * 工作区模块门面，向其他模块提供工作区查询、解析和重载能力。
 */
class WorkspaceCase(
    private val controller: WorkspaceController,
) {
    fun list(): List<WorkspaceStatus> = controller.list()
    fun get(id: String): WorkspaceSnapshot? = controller.get(id)
    fun resolve(context: WorkspaceResolutionContext = WorkspaceResolutionContext()): WorkspaceResolution {
        return controller.resolve(context)
    }
    fun reload(id: String): WorkspaceReloadResult = controller.reload(id)
    fun reloadAll(): List<WorkspaceReloadResult> = controller.reloadAll()
    suspend fun stop() {
        controller.stop()
    }
}
