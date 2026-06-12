package com.heyanle.priestess.bot.platform

import com.heyanle.priestess.bot.core.event.EventBus
import com.heyanle.priestess.bot.platform.adapters.napcat4_18_6.NapCatConfig
import com.heyanle.priestess.bot.platform.adapters.napcat4_18_6.NapCatPlatform
import com.heyanle.priestess.bot.platform.adapters.telegram.TelegramConfig
import com.heyanle.priestess.bot.platform.adapters.telegram.TelegramPlatform

fun registerBuiltinPlatforms(eventBus: EventBus) {
    PlatformRegistry.registerMeta(
        metadata = PlatformMetadata(
            name = "telegram",
            displayName = "Telegram",
            supportStreaming = true,
            supportProactiveMessage = true,
        ),
        factory = { cfg ->
            TelegramPlatform(
                eventBus = eventBus,
                config = TelegramConfig(
                    token = cfg?.token ?: "",
                    name = cfg?.name ?: "telegram",
                    displayName = "Telegram",
                )
            )
        }
    )

    PlatformRegistry.registerMeta(
        metadata = PlatformMetadata(
            name = "napcat4_18_6",
            displayName = "NapCat v4.18.6 (QQ)",
            supportStreaming = false,
            supportProactiveMessage = false,
        ),
        factory = { cfg ->
            NapCatPlatform(
                eventBus = eventBus,
                config = NapCatConfig(
                    host = cfg?.host ?: "127.0.0.1",
                    port = cfg?.port ?: 3000,
                    wsPort = cfg?.wsPort ?: 3001,
                    useWs = cfg?.useWs ?: true,
                    name = cfg?.name ?: "napcat4_18_6",
                    displayName = "NapCat v4.18.6 (QQ)",
                )
            )
        }
    )
}
