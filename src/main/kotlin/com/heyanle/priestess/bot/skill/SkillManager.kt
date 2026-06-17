package com.heyanle.priestess.bot.skill

/**
 * Manages the lifecycle and dispatch of registered [Skill] instances.
 *
 * Skills are evaluated in descending priority order.
 * [dispatch] iterates through registered skills and returns the result
 * of the first skill whose [Skill.canHandle] returns true.
 *
 * If no skill matches, [dispatch] returns null.
 */
class SkillManager {

    private val skills = mutableListOf<Skill>()

    /**
     * Register a single skill and re-sort by priority.
     */
    fun register(skill: Skill) {
        skills.add(skill)
        skills.sortByDescending { it.priority }
    }

    /**
     * Register multiple skills and re-sort by priority.
     */
    fun registerAll(newSkills: Collection<Skill>) {
        skills.addAll(newSkills)
        skills.sortByDescending { it.priority }
    }

    /**
     * Sort all registered skills by priority in descending order (highest first).
     */
    fun sortByPriority() {
        skills.sortByDescending { it.priority }
    }

    /**
     * Dispatch a message to the first skill that can handle it.
     *
     * @param message The user message text to dispatch.
     * @return The response from the matching skill, or null if no skill matches.
     */
    suspend fun dispatch(message: String): String? {
        for (skill in skills) {
            if (skill.canHandle(message)) {
                return skill.execute(message)
            }
        }
        return null
    }

    /**
     * Get the list of all registered skills (sorted by priority).
     */
    fun getAll(): List<Skill> = skills.toList()

    /**
     * Remove a skill by name.
     */
    fun unregister(name: String) {
        skills.removeAll { it.name == name }
    }

    /**
     * Remove all registered skills.
     */
    fun clear() {
        skills.clear()
    }

    /**
     * Get the count of registered skills.
     */
    fun size(): Int = skills.size

    /**
     * Check if any skills are registered.
     */
    fun isEmpty(): Boolean = skills.isEmpty()
}
