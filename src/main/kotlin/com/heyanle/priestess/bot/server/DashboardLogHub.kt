package com.heyanle.priestess.bot.server

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object DashboardLogHub {
    private const val DEFAULT_CAPACITY = 200
    private val lock = Mutex()
    private val buffer = ArrayDeque<LogEventDto>()
    private val events = MutableSharedFlow<LogEventDto>(
        replay = 0,
        extraBufferCapacity = DEFAULT_CAPACITY,
    )

    suspend fun publish(event: LogEventDto) {
        lock.withLock {
            buffer.addLast(event)
            while (buffer.size > DEFAULT_CAPACITY) {
                buffer.removeFirst()
            }
        }
        events.tryEmit(event)
    }

    suspend fun recent(): List<LogEventDto> {
        return lock.withLock { buffer.toList() }
    }

    fun events(): Flow<LogEventDto> = events

    suspend fun clearForTest() {
        lock.withLock { buffer.clear() }
    }
}
