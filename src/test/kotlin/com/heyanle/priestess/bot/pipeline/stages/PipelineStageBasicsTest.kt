package com.heyanle.priestess.bot.pipeline.stages

import com.heyanle.priestess.bot.config.PipelineConfig
import com.heyanle.priestess.bot.platform.MessageComponent
import com.heyanle.priestess.bot.platform.SessionType
import com.heyanle.priestess.bot.testkit.testPipelineContext
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PipelineStageBasicsTest {
    @Test
    fun `waking check allows private messages`() = runBlocking {
        val ctx = testPipelineContext(sessionType = SessionType.PRIVATE)

        WakingCheckStage(PipelineConfig(wakingPrefix = "/")).process(ctx)

        assertFalse(ctx.isStopped)
    }

    @Test
    fun `waking check stops group message without mention or prefix`() = runBlocking {
        val ctx = testPipelineContext(sessionType = SessionType.GROUP, text = "hello")

        WakingCheckStage(PipelineConfig(wakingPrefix = "/")).process(ctx)

        assertTrue(ctx.isStopped)
    }

    @Test
    fun `waking check allows group message with prefix`() = runBlocking {
        val ctx = testPipelineContext(sessionType = SessionType.GROUP, text = "/hello")

        WakingCheckStage(PipelineConfig(wakingPrefix = "/")).process(ctx)

        assertFalse(ctx.isStopped)
    }

    @Test
    fun `waking check allows group message with bot mention`() = runBlocking {
        val ctx = testPipelineContext(
            sessionType = SessionType.GROUP,
            components = listOf(MessageComponent.At("bot-1"), MessageComponent.Text(" hello")),
        )

        WakingCheckStage(PipelineConfig(wakingPrefix = "/")).process(ctx)

        assertFalse(ctx.isStopped)
    }

    @Test
    fun `whitelist check stops non-whitelisted private sender`() = runBlocking {
        val ctx = testPipelineContext(senderId = "user-1")

        WhitelistCheckStage(PipelineConfig(whitelistEnabled = true, whitelistUsers = listOf("user-2"))).process(ctx)

        assertTrue(ctx.isStopped)
    }

    @Test
    fun `whitelist check allows whitelisted group`() = runBlocking {
        val ctx = testPipelineContext(sessionType = SessionType.GROUP, sessionId = "group-1", senderId = "user-1")

        WhitelistCheckStage(PipelineConfig(whitelistEnabled = true, whitelistGroups = listOf("group-1"))).process(ctx)

        assertFalse(ctx.isStopped)
    }

    @Test
    fun `rate limit stops messages after configured limit`() = runBlocking {
        val stage = RateLimitStage(PipelineConfig(rateLimitEnabled = true, rateLimitPerMinute = 1))
        val first = testPipelineContext(senderId = "user-1")
        val second = testPipelineContext(senderId = "user-1")

        stage.process(first)
        stage.process(second)

        assertFalse(first.isStopped)
        assertTrue(second.isStopped)
    }

    @Test
    fun `session status stops disabled session and allows re-enabled session`() = runBlocking {
        val stage = SessionStatusStage(PipelineConfig())
        val disabled = testPipelineContext(sessionId = "session-disabled")
        val enabledAgain = testPipelineContext(sessionId = "session-disabled")

        stage.disableSession("session-disabled")
        stage.process(disabled)
        stage.enableSession("session-disabled")
        stage.process(enabledAgain)

        assertTrue(disabled.isStopped)
        assertFalse(enabledAgain.isStopped)
        assertTrue(stage.isSessionEnabled("session-disabled"))
    }

    @Test
    fun `content safety placeholder allows blank and text content when enabled`() = runBlocking {
        val stage = ContentSafetyStage(PipelineConfig(contentSafetyEnabled = true))
        val blank = testPipelineContext(text = "")
        val text = testPipelineContext(text = "ordinary message")

        stage.process(blank)
        stage.process(text)

        assertFalse(blank.isStopped)
        assertFalse(text.isStopped)
    }

    @Test
    fun `respond stage sends decorated response through source platform`() = runBlocking {
        val ctx = testPipelineContext()
        ctx.shared["decoratedResponse"] = "hello back"
        val platform = ctx.event.platform as com.heyanle.priestess.bot.testkit.FakePlatform

        RespondStage().process(ctx)

        assertEquals(1, platform.sentMessages.size)
        assertEquals("hello back", platform.sentMessages.single().second.textContent)
    }
}
