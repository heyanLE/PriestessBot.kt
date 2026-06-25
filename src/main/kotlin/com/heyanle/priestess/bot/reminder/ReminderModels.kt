package com.heyanle.priestess.bot.reminder

import com.heyanle.priestess.bot.memory.MemoryScopeContext
import com.heyanle.priestess.bot.platform.SessionType
import kotlinx.serialization.Serializable

@Serializable
enum class ReminderStatus {
    PENDING,
    DELIVERED,
    FAILED,
    DELETED,
}

@Serializable
data class ReminderRecord(
    val id: String,
    val workspaceId: String,
    val text: String,
    val dueAt: Long,
    val status: ReminderStatus,
    val platformId: String? = null,
    val sessionId: String? = null,
    val sessionType: SessionType? = null,
    val userId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deliveredAt: Long? = null,
    val deletedAt: Long? = null,
    val failureReason: String? = null,
    val deliveryAttemptCount: Int = 0,
)

data class ReminderScopeContext(
    val workspaceId: String = MemoryScopeContext.DEFAULT_WORKSPACE_ID,
    val platformId: String? = null,
    val sessionId: String? = null,
    val sessionType: SessionType? = null,
    val userId: String? = null,
)

data class ReminderFilter(
    val scopeContext: ReminderScopeContext,
    val status: ReminderStatus? = null,
    val dueAfter: Long? = null,
    val dueBefore: Long? = null,
    val includeDeleted: Boolean = false,
    val limit: Int = 50,
)

data class ReminderDeliveryResult(
    val delivered: Int,
    val failed: Int,
    val skipped: Int,
)
