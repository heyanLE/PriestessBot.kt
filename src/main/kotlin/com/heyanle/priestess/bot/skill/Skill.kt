package com.heyanle.priestess.bot.skill

/**
 * Skill interface for handling specific types of user messages.
 *
 * Skills are evaluated in priority order (higher priority first).
 * When [canHandle] returns true for a message, [execute] is called
 * and its result is returned as the response.
 *
 * If no Skill can handle a message, [DefaultSkill] acts as fallback.
 */
interface Skill {
    /** Unique name of this skill (used for logging and registration). */
    val name: String

    /** Human-readable description of what this skill does. */
    val description: String

    /**
     * Optional SKILL.md content for prompt loading.
     *
     * Implementations that are backed by a real skill directory should expose
     * the parsed SKILL.md text here. The default renderer below is only a
     * compatibility fallback for older in-process skills.
     */
    val skillMarkdown: String?
        get() = null

    /**
     * Priority of this skill. Higher values indicate higher priority.
     * Skills are evaluated in descending priority order.
     */
    val priority: Int

    /**
     * Checks whether this skill can handle the given message.
     *
     * @param message The user message text to evaluate.
     * @return true if this skill should handle the message.
     */
    suspend fun canHandle(message: String): Boolean

    /**
     * Executes this skill on the given message and returns a response.
     * Only called when [canHandle] returns true.
     *
     * @param message The user message text to process.
     * @return The response string produced by this skill.
     */
    suspend fun execute(message: String): String

    /**
     * Render the skill as a markdown prompt document that can be loaded into a pipeline.
     * Existing skills do not need custom documents yet; they can rely on this default
     * metadata-based rendering until a dedicated SKILL.md exists.
     */
    fun promptMarkdown(settings: Map<String, String> = emptyMap()): String {
        skillMarkdown?.takeIf { it.isNotBlank() }?.let { markdown ->
            if (settings.isEmpty()) return markdown.trim()
            return buildString {
                append(markdown.trim())
                append("\n\n## Settings\n")
                settings.entries.sortedBy { it.key }.forEach { (key, value) ->
                    append("- ")
                    append(key)
                    append(": ")
                    append(value)
                    append("\n")
                }
            }.trim()
        }
        return buildString {
            append("# Skill: ")
            append(name)
            append("\n\n## Description\n")
            append(description.ifBlank { "No description provided." })
            append("\n\n## Priority\n")
            append(priority)
            if (settings.isNotEmpty()) {
                append("\n\n## Settings\n")
                settings.entries.sortedBy { it.key }.forEach { (key, value) ->
                    append("- ")
                    append(key)
                    append(": ")
                    append(value)
                    append("\n")
                }
            }
        }.trim()
    }
}
