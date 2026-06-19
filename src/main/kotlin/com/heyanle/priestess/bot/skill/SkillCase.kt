package com.heyanle.priestess.bot.skill

class SkillCase(
    private val controller: SkillController,
) {
    suspend fun dispatch(message: String): String? = controller.dispatch(message)
    fun register(skill: Skill) = controller.register(skill)
    fun getAll(): List<Skill> = controller.getAll()
}
