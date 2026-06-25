package com.heyanle.priestess.bot.skill

data class SkillPromptDocument(
    val name: String,
    val markdown: String,
)

class PipelineSkillState(
    availableSkills: List<SkillPromptDocument> = emptyList(),
) {
    private val availableByName = availableSkills.associateBy { it.name }
    private val loadedByName = linkedMapOf<String, SkillPromptDocument>()

    val availableNames: List<String>
        get() = availableByName.keys.sorted()

    val loadedNames: List<String>
        get() = loadedByName.keys.toList()

    fun load(name: String): SkillPromptDocument? {
        val normalized = name.trim()
        val document = availableByName[normalized] ?: return null
        loadedByName[normalized] = document
        return document
    }

    fun unload(name: String): Boolean {
        return loadedByName.remove(name.trim()) != null
    }

    fun renderLoadedSkillBlock(): String {
        if (loadedByName.isEmpty()) return "No skills loaded."
        return buildString {
            append("Loaded SKILL.md documents:")
            loadedByName.values.forEach { document ->
                append("\n\n---\n")
                append(document.markdown.trim())
            }
        }
    }
}
