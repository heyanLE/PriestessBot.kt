package com.heyanle.priestess.bot.core.event

interface Event {
    val sourceId: String
}

data class TextMessageEvent(
    val text: String,
    override val sourceId: String = "",
) : Event

data class ImageMessageEvent(
    val imageUrl: String,
    val caption: String = "",
    override val sourceId: String = "",
) : Event

data class SystemEvent(
    val message: String,
    override val sourceId: String = "",
) : Event

data class ControlEvent(
    val action: String,
    override val sourceId: String = "",
) : Event
