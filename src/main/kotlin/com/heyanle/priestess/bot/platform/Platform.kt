package com.heyanle.priestess.bot.platform

import kotlinx.coroutines.Job
import com.heyanle.priestess.bot.core.event.Event
import com.heyanle.priestess.bot.core.event.EventBus
import java.util.concurrent.atomic.AtomicBoolean

sealed class MessageComponent {
    data class Text(val text: String) : MessageComponent()
    data class Image(val url: String) : MessageComponent()
    data class At(val userId: String) : MessageComponent()
    data class File(val url: String, val name: String = "") : MessageComponent()
}

data class MessageChain(
    val components: List<MessageComponent> = emptyList(),
) {
    val textContent: String
        get() = components.filterIsInstance<MessageComponent.Text>().joinToString("") { it.text }

    companion object {
        fun text(text: String) = MessageChain(listOf(MessageComponent.Text(text)))
        fun image(url: String) = MessageChain(listOf(MessageComponent.Image(url)))
    }
}

data class MessageSession(
    val id: String,
    val type: SessionType,
    val platformName: String,
    val metadata: Map<String, String> = emptyMap(),
)

enum class SessionType {
    PRIVATE, GROUP, CHANNEL
}

class MessageEvent(
    val platform: Platform,
    val session: MessageSession,
    val chain: MessageChain,
    val messageId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    override val sourceId: String = "",
) : Event {
    val isStopped: AtomicBoolean = AtomicBoolean(false)

    fun stopPropagation() {
        isStopped.set(true)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MessageEvent) return false
        return platform == other.platform &&
            session == other.session &&
            chain == other.chain &&
            messageId == other.messageId &&
            timestamp == other.timestamp &&
            sourceId == other.sourceId
    }

    override fun hashCode(): Int {
        var result = platform.hashCode()
        result = 31 * result + session.hashCode()
        result = 31 * result + chain.hashCode()
        result = 31 * result + messageId.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + sourceId.hashCode()
        return result
    }
}

abstract class AdapterConfig {
    abstract val name: String
    abstract val displayName: String
}

abstract class Platform(val eventBus: EventBus) {
    abstract val metadata: PlatformMetadata

    abstract suspend fun run(): Job
    abstract suspend fun terminate()
    abstract suspend fun sendMessage(session: MessageSession, chain: MessageChain)

    protected suspend fun commitEvent(event: MessageEvent) {
        eventBus.send(event)
    }

    open suspend fun webhookCallback(request: Any): Any {
        error("${metadata.name} does not support webhook")
    }
}
