package com.heyanle.priestess.bot.skill

import com.heyanle.priestess.bot.core.controller.BaseController

/**
 * Owns registered skill handlers and dispatch order.
 *
 * Skills are stored in priority order so dispatch can return the first handler
 * that accepts a message. Cross-module callers should use SkillCase instead of
 * manipulating the controller directly.
 */
class SkillController : BaseController("SkillController") {

    private val skills = mutableListOf<Skill>()

    fun register(skill: Skill) {
        skills.add(skill)
        skills.sortByDescending { it.priority }
    }

    fun registerAll(newSkills: Collection<Skill>) {
        skills.addAll(newSkills)
        skills.sortByDescending { it.priority }
    }

    fun sortByPriority() {
        skills.sortByDescending { it.priority }
    }

    suspend fun dispatch(message: String): String? {
        for (skill in skills) {
            if (skill.canHandle(message)) {
                return skill.execute(message)
            }
        }
        return null
    }

    fun getAll(): List<Skill> = skills.toList()

    fun unregister(name: String) {
        skills.removeAll { it.name == name }
    }

    fun clear() {
        skills.clear()
    }

    fun size(): Int = skills.size

    fun isEmpty(): Boolean = skills.isEmpty()
}
