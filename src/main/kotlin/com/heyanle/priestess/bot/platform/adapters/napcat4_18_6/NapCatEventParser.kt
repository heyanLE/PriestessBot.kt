package com.heyanle.priestess.bot.platform.adapters.napcat4_18_6

import com.heyanle.priestess.bot.platform.MessageChain
import com.heyanle.priestess.bot.platform.MessageComponent
import com.heyanle.priestess.bot.platform.MessageEvent
import com.heyanle.priestess.bot.platform.MessageSession
import com.heyanle.priestess.bot.platform.Platform
import com.heyanle.priestess.bot.platform.SessionType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

internal object NapCatEventParser {

    fun parseOneBotEvent(platform: Platform, json: JsonObject): MessageEvent? {
        val postType = json["post_type"]?.jsonPrimitive?.content ?: return null
        if (postType != "message") return null

        val messageType = json["message_type"]?.jsonPrimitive?.content ?: "private"
        val userId = json["user_id"]?.jsonPrimitive?.longOrNull?.toString() ?: return null
        val selfId = json["self_id"]?.jsonPrimitive?.longOrNull?.toString()
        val sessionId = if (messageType == "group") {
            json["group_id"]?.jsonPrimitive?.longOrNull?.toString() ?: userId
        } else {
            userId
        }
        val text = json["raw_message"]?.jsonPrimitive?.content
            ?: json["message"]?.jsonPrimitive?.content
            ?: ""

        return buildMessageEvent(platform, messageType, sessionId, userId, selfId, text)
    }

    fun parseHttpMessage(platform: Platform, json: JsonObject): MessageEvent? {
        val messageType = json["message_type"]?.jsonPrimitive?.content ?: "private"
        val sender = json["sender"] as? JsonObject
        val userId = sender?.get("user_id")?.jsonPrimitive?.longOrNull?.toString()
            ?: json["user_id"]?.jsonPrimitive?.content
            ?: return null
        val selfId = json["self_id"]?.jsonPrimitive?.longOrNull?.toString()
        val sessionId = if (messageType == "group") {
            json["group_id"]?.jsonPrimitive?.content ?: userId
        } else {
            userId
        }
        val text = json["raw_message"]?.jsonPrimitive?.content
            ?: json["message"]?.jsonPrimitive?.content
            ?: ""

        return buildMessageEvent(platform, messageType, sessionId, userId, selfId, text)
    }

    private fun buildMessageEvent(
        platform: Platform,
        messageType: String,
        sessionId: String,
        senderId: String,
        selfId: String?,
        text: String,
    ): MessageEvent {
        val sessionType = when (messageType) {
            "private" -> SessionType.PRIVATE
            "group" -> SessionType.GROUP
            else -> SessionType.PRIVATE
        }
        return MessageEvent(
            platform = platform,
            session = MessageSession(
                id = sessionId,
                type = sessionType,
                platformName = platform.metadata.name,
                metadata = buildMap {
                    put("senderId", senderId)
                    put("userId", senderId)
                    if (selfId != null) {
                        put("selfId", selfId)
                    }
                },
            ),
            chain = parseMessageChain(text),
        )
    }

    private fun parseMessageChain(text: String): MessageChain {
        val components = mutableListOf<MessageComponent>()
        var cursor = 0
        for (match in CQ_AT_REGEX.findAll(text)) {
            val plain = text.substring(cursor, match.range.first)
            if (plain.isNotEmpty()) {
                components.add(MessageComponent.Text(plain))
            }
            val userId = match.groupValues[1]
            if (userId.isNotBlank()) {
                components.add(MessageComponent.At(userId))
            }
            cursor = match.range.last + 1
        }
        val tail = text.substring(cursor)
        if (tail.isNotEmpty()) {
            components.add(MessageComponent.Text(tail))
        }
        return MessageChain(components.ifEmpty { listOf(MessageComponent.Text(text)) })
    }

    private val CQ_AT_REGEX = Regex("""\[CQ:at,qq=([^,\]]+).*?]""")
}
