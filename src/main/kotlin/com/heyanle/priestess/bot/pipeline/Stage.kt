package com.heyanle.priestess.bot.pipeline

import kotlinx.coroutines.flow.Flow

/**
 * Pipeline 阶段接口。
 *
 * 洋葱模型：
 * - 返回 `null` → 线性阶段，调度器直接进入下一阶段
 * - 返回 `Flow<Unit>` → 洋葱阶段：前置逻辑已在 [process] 中执行，
 *   调度器会先递归执行后续所有阶段，再 collect 该 Flow 完成后置逻辑
 */
interface Stage {
    val name: String
    val order: StageOrder

    /** 阶段初始化，在管道启动时调用一次 */
    suspend fun initialize(ctx: PipelineContext) {}

    /**
     * 处理事件。
     * @return `null` 表示线性阶段；非 null Flow 表示洋葱阶段
     */
    suspend fun process(ctx: PipelineContext): Flow<Unit>?
}

/**
 * 9 阶段执行顺序。
 *
 * 1. WAKING_CHECK    — 唤醒检测（@提及 / 前缀 / 私聊旁路）
 * 2. WHITELIST_CHECK — 白名单过滤
 * 3. SESSION_STATUS  — 会话开关检查
 * 4. RATE_LIMIT      — 频率限制
 * 5. CONTENT_SAFETY  — 内容安全（v1 占位）
 * 6. PRE_PROCESS     — 预处理（注入 System Prompt、加载历史）[洋葱]
 * 7. PROCESS         — Agent 执行（创建 ReActRunner）[洋葱]
 * 8. RESULT_DECORATE — 结果装饰（格式化、Markdown 渲染）
 * 9. RESPOND         — 回复发送 + 会话持久化
 */
enum class StageOrder(val level: Int) {
    WAKING_CHECK(1),
    WHITELIST_CHECK(2),
    SESSION_STATUS(3),
    RATE_LIMIT(4),
    CONTENT_SAFETY(5),
    PRE_PROCESS(6),
    PROCESS(7),
    RESULT_DECORATE(8),
    RESPOND(9);

    companion object {
        /** 按 [level] 排序的阶段列表 */
        val sorted: List<StageOrder> = entries.sortedBy { it.level }
    }
}
