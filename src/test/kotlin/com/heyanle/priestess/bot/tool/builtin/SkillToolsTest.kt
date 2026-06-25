package com.heyanle.priestess.bot.tool.builtin

import com.heyanle.priestess.bot.skill.PipelineSkillState
import com.heyanle.priestess.bot.skill.SkillPromptDocument
import com.heyanle.priestess.bot.tool.AgentToolContext
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SkillToolsTest {
    @Test
    fun `use skill validates name and availability`() = runBlocking {
        val tool = UseSkillTool()
        val context = AgentToolContext(
            skillState = PipelineSkillState(
                listOf(SkillPromptDocument(name = "research", markdown = "# Skill: research")),
            ),
        )

        val missingName = tool.execute(context, emptyMap())
        val unavailable = tool.execute(context, mapOf("name" to "missing"))
        val loaded = tool.execute(context, mapOf("name" to "research"))

        assertFalse(missingName.success)
        assertEquals("VALIDATION_ERROR", missingName.errorCode)
        assertFalse(unavailable.success)
        assertEquals("SKILL_NOT_FOUND", unavailable.errorCode)
        assertTrue(loaded.success)
        assertEquals(listOf("research"), context.skillState.loadedNames)
    }

    @Test
    fun `unload skill removes loaded document`() = runBlocking {
        val state = PipelineSkillState(
            listOf(SkillPromptDocument(name = "research", markdown = "# Skill: research")),
        )
        state.load("research")
        val context = AgentToolContext(skillState = state)
        val tool = UnloadSkillTool()

        val unloaded = tool.execute(context, mapOf("name" to "research"))
        val unloadedAgain = tool.execute(context, mapOf("name" to "research"))

        assertTrue(unloaded.success)
        assertEquals(emptyList(), state.loadedNames)
        assertFalse(unloadedAgain.success)
        assertEquals("SKILL_NOT_LOADED", unloadedAgain.errorCode)
    }
}
