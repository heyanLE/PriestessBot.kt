package com.heyanle.priestess.bot.platform.adapters.napcat4_18_6

import com.heyanle.priestess.bot.platform.MessageChain
import com.heyanle.priestess.bot.platform.MessageComponent
import com.heyanle.priestess.bot.platform.MessageSession
import com.heyanle.priestess.bot.platform.Platform
import com.heyanle.priestess.bot.platform.PlatformMetadata
import com.heyanle.priestess.bot.platform.SessionType
import kotlinx.coroutines.Job
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NapCatEventParserTest {

    private val platform = TestPlatform("napcat-test")

    @Test
    fun `parse private websocket message event`() {
        val json = Json.parseToJsonElement(
            """
            {
              "post_type": "message",
              "message_type": "private",
              "user_id": 123456,
              "raw_message": "hello priestess"
            }
            """.trimIndent(),
        ).jsonObject

        val event = NapCatEventParser.parseOneBotEvent(platform, json)

        requireNotNull(event)
        assertEquals(SessionType.PRIVATE, event.session.type)
        assertEquals("123456", event.session.id)
        assertEquals("napcat-test", event.session.platformName)
        assertEquals("hello priestess", event.chain.textContent)
    }

    @Test
    fun `parse group websocket message event`() {
        val json = Json.parseToJsonElement(
            """
            {
              "post_type": "message",
              "message_type": "group",
              "group_id": 654321,
              "user_id": 123456,
              "raw_message": "group hello"
            }
            """.trimIndent(),
        ).jsonObject

        val event = NapCatEventParser.parseOneBotEvent(platform, json)

        requireNotNull(event)
        assertEquals(SessionType.GROUP, event.session.type)
        assertEquals("654321", event.session.id)
        assertEquals("group hello", event.chain.textContent)
    }

    @Test
    fun `parse group websocket at mention as message component`() {
        val json = Json.parseToJsonElement(
            """
            {
              "post_type": "message",
              "message_type": "group",
              "self_id": 3334969096,
              "group_id": 654321,
              "user_id": 123456,
              "raw_message": "[CQ:at,qq=3334969096] hello"
            }
            """.trimIndent(),
        ).jsonObject

        val event = NapCatEventParser.parseOneBotEvent(platform, json)

        requireNotNull(event)
        assertEquals("123456", event.session.metadata["senderId"])
        assertEquals("3334969096", event.session.metadata["selfId"])
        assertEquals(
            listOf(MessageComponent.At("3334969096"), MessageComponent.Text(" hello")),
            event.chain.components,
        )
    }

    @Test
    fun `ignore non-message websocket event`() {
        val json = Json.parseToJsonElement(
            """
            {
              "post_type": "notice",
              "notice_type": "group_increase",
              "user_id": 123456
            }
            """.trimIndent(),
        ).jsonObject

        assertNull(NapCatEventParser.parseOneBotEvent(platform, json))
    }

    @Test
    fun `parse http fallback message with sender user id`() {
        val json = Json.parseToJsonElement(
            """
            {
              "message_type": "group",
              "group_id": "654321",
              "sender": { "user_id": 123456 },
              "message": "fallback hello"
            }
            """.trimIndent(),
        ).jsonObject

        val event = NapCatEventParser.parseHttpMessage(platform, json)

        requireNotNull(event)
        assertEquals(SessionType.GROUP, event.session.type)
        assertEquals("654321", event.session.id)
        assertEquals("fallback hello", event.chain.textContent)
    }

    private class TestPlatform(name: String) : Platform() {
        override val metadata = PlatformMetadata(
            name = name,
            displayName = name,
            supportStreaming = false,
            supportProactiveMessage = false,
        )

        override suspend fun run(): Job = Job()
        override suspend fun terminate() = Unit
        override suspend fun sendMessage(session: MessageSession, chain: MessageChain) = Unit
    }
}
