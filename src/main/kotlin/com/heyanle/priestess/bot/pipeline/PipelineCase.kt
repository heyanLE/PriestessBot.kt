package com.heyanle.priestess.bot.pipeline

import com.heyanle.priestess.bot.platform.MessageEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Job

class PipelineCase(
    private val controller: PipelineController,
) {
    private val logger = KotlinLogging.logger {}

    fun process(event: MessageEvent): Job {
        logger.info {
            "[PIPELINE-030] PipelineCase accepted message platform=${event.platform.metadata.name}, " +
                "session=${event.session.id}, messageId=${event.messageId}"
        }
        return controller.process(event)
    }
}
