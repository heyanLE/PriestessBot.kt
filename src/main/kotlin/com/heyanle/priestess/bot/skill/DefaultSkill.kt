package com.heyanle.priestess.bot.skill

/**
 * Fallback [Skill] that handles any message when no other skill can.
 *
 * Registers with the lowest possible priority ([Int.MIN_VALUE]) so that
 * all custom skills are evaluated first.
 */
class DefaultSkill : Skill {

    override val name = "default"
    override val description = "Default fallback skill that handles any message no other skill can process."
    override val priority = Int.MIN_VALUE

    override suspend fun canHandle(message: String): Boolean = true

    override suspend fun execute(message: String): String {
        return "I'm sorry, but I don't have a specific skill to handle this request. " +
                "Could you please rephrase or provide more details?"
    }
}
