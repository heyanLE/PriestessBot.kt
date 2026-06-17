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
}
