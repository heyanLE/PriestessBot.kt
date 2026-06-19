package com.heyanle.priestess.bot.pipeline

import kotlinx.coroutines.flow.Flow

/**
 * A single step in the message pipeline.
 *
 * Returning `null` means the stage is linear and the controller can continue to
 * the next stage immediately. Returning a [Flow] means the stage uses the onion
 * model: pre-work has already run in [process], the controller runs downstream
 * stages, then collects the returned flow for post-work.
 */
interface Stage {
    val name: String
    val order: StageOrder

    suspend fun process(ctx: PipelineContext): Flow<Unit>?
}

/**
 * Execution order for the v1 pipeline stages.
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
        val sorted: List<StageOrder> = entries.sortedBy { it.level }
    }
}
