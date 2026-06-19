package com.heyanle.priestess.bot.platform

import com.heyanle.priestess.bot.pipeline.PipelineCase
import io.github.oshai.kotlinlogging.KotlinLogging

class PlatformCase(
    private val pipelineCaseProvider: () -> PipelineCase,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun handleIncomingMessage(event: MessageEvent) {
        logger.info {
            "[PIPELINE-020] PlatformCase received message platform=${event.platform.metadata.name}, " +
                "session=${event.session.id}, type=${event.session.type}, text='${event.chain.textContent.take(120)}'"
        }
        pipelineCaseProvider().process(event)
        logger.info {
            "[PIPELINE-029] PlatformCase dispatched message platform=${event.platform.metadata.name}, " +
                "session=${event.session.id}"
        }
    }
}
