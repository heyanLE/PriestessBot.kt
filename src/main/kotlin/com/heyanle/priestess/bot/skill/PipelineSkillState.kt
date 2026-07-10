package com.heyanle.priestess.bot.skill

import java.nio.file.Files
import java.nio.file.Path

/**
 * 技能提示文档，保存可注入模型上下文的技能名称和 Markdown 内容。
 */
data class SkillPromptDocument(
    val name: String,
    val markdown: String,
)

/**
 * 工作区可见技能引用，按需加载 SKILL.md。
 */
data class SkillPromptReference(
    val name: String,
    val description: String = "",
    val markdownPath: String? = null,
    val inlineMarkdown: String? = null,
    val settings: Map<String, String> = emptyMap(),
) {
    fun loadDocument(): SkillPromptDocument {
        val markdown = inlineMarkdown ?: markdownPath
            ?.takeIf { it.isNotBlank() }
            ?.let { path -> Files.readString(Path.of(path)) }
            ?: ""
        val rendered = if (settings.isEmpty()) {
            markdown.trim()
        } else {
            buildString {
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
        return SkillPromptDocument(name = name, markdown = rendered)
    }
}

/**
 * 流水线技能状态，记录当前工作区可用和已加载的技能提示文档。
 */
class PipelineSkillState(
    availableSkills: List<SkillPromptReference> = emptyList(),
) {
    private val availableByName = availableSkills.associateBy { it.name }
    private val loadedByName = linkedMapOf<String, SkillPromptDocument>()

    val availableNames: List<String>
        get() = availableByName.keys.sorted()

    val loadedNames: List<String>
        get() = loadedByName.keys.toList()

    fun load(name: String): SkillPromptDocument? {
        val normalized = name.trim()
        val document = availableByName[normalized]?.loadDocument() ?: return null
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
