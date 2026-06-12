package com.heyanle.priestess.bot.core.config

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File

object ConfigLoader {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private const val DEFAULT_CONFIG_PATH = "config.json"

    fun load(path: String = DEFAULT_CONFIG_PATH): PriestessConfig {
        val file = File(path)
        if (!file.exists()) {
            val default = PriestessConfig()
            save(default, path)
            return default
        }

        return try {
            val content = file.readText()
            json.decodeFromString<PriestessConfig>(content)
        } catch (e: SerializationException) {
            System.err.println(
                "[ConfigLoader] WARNING: Failed to parse $path: ${e.message}. " +
                "Backing up corrupted file and falling back to defaults."
            )
            try {
                val bakFile = File("$path.bak")
                file.copyTo(bakFile, overwrite = true)
            } catch (backupEx: Exception) {
                System.err.println(
                    "[ConfigLoader] WARNING: Failed to backup corrupted config: ${backupEx.message}"
                )
            }
            val default = PriestessConfig()
            save(default, path)
            default
        } catch (e: Exception) {
            System.err.println(
                "[ConfigLoader] WARNING: Failed to read $path: ${e.message}. " +
                "Falling back to defaults."
            )
            PriestessConfig()
        }
    }

    fun save(config: PriestessConfig, path: String = DEFAULT_CONFIG_PATH) {
        try {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(PriestessConfig.serializer(), config))
        } catch (e: Exception) {
            System.err.println(
                "[ConfigLoader] ERROR: Failed to save config to $path: ${e.message}"
            )
        }
    }
}
