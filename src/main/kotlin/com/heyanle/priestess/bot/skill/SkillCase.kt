package com.heyanle.priestess.bot.skill

import com.heyanle.priestess.bot.workspace.WorkspaceSnapshot

class SkillCase(
    private val controller: SkillController,
) {
    suspend fun dispatch(message: String): String? = controller.dispatch(message)
    fun register(skill: Skill) = controller.register(skill)
    fun getAll(): List<Skill> = controller.getAll()

    fun getWorkspaceSkillState(snapshot: WorkspaceSnapshot): PipelineSkillState {
        return PipelineSkillState(getWorkspaceSkillDocuments(snapshot))
    }

    fun getWorkspaceSkillDocuments(snapshot: WorkspaceSnapshot): List<SkillPromptDocument> {
        return getWorkspaceSkillSet(snapshot).documents()
    }

    fun getWorkspaceSkillSet(snapshot: WorkspaceSnapshot): WorkspaceSkillSet {
        val allowedNames = snapshot.skillNames.toSet()
        val settings = snapshot.skillSettings
        val scopedSkills = controller.getAll()
            .filter { it.name in allowedNames }
            .sortedByDescending { it.priority }
        return WorkspaceSkillSet(
            workspaceId = snapshot.id,
            skills = scopedSkills,
            settings = settings,
        )
    }
}

class WorkspaceSkillSet(
    val workspaceId: String,
    private val skills: List<Skill>,
    val settings: Map<String, Map<String, String>>,
) {
    val skillNames: List<String> = skills.map { it.name }

    suspend fun dispatch(message: String): String? {
        for (skill in skills) {
            if (skill.canHandle(message)) {
                return skill.execute(message)
            }
        }
        return null
    }

    fun setting(skillName: String, key: String): String? = settings[skillName]?.get(key)

    fun documents(): List<SkillPromptDocument> {
        return skills.map { skill ->
            SkillPromptDocument(
                name = skill.name,
                markdown = skill.promptMarkdown(settings[skill.name].orEmpty()),
            )
        }
    }

    fun isEmpty(): Boolean = skills.isEmpty()
}
