package com.heyanle.priestess.bot.core.event

import com.heyanle.priestess.bot.core.lifecycle.LifecycleAware
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

private const val DEFAULT_BUFFER_CAPACITY = 64

class EventBus(
    private val scope: CoroutineScope,
) : LifecycleAware {

    private val _events = MutableSharedFlow<Event>(
        replay = 0,
        extraBufferCapacity = DEFAULT_BUFFER_CAPACITY,
    )

    val events: SharedFlow<Event> = _events.asSharedFlow()

    suspend fun send(event: Event) {
        _events.emit(event)
    }

    fun subscribe(handler: suspend (Event) -> Unit): Job {
        return scope.launch {
            _events.collect { event ->
                handler(event)
            }
        }
    }

    /**
     * Convenience method to unsubscribe a subscriber.
     * Equivalent to calling [Job.cancel] directly on the Job returned by [subscribe].
     */
    fun unsubscribe(job: Job) {
        job.cancel()
    }

    override suspend fun start() {
        // SharedFlow is ready after construction
    }

    override suspend fun stop() {
        scope.cancel()
    }
}
