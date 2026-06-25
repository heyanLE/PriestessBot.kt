package com.heyanle.priestess.bot.tool.builtin

import com.heyanle.priestess.bot.testkit.testConversationCase
import com.heyanle.priestess.bot.testkit.testKnowledgeCase
import com.heyanle.priestess.bot.testkit.testMemoryCase
import com.heyanle.priestess.bot.testkit.testReminderCase
import com.heyanle.priestess.bot.tool.ToolController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BuiltinToolsRegistrationTest {
    @Test
    fun `registers all v3 core tools when dependencies are available`() {
        val registry = ToolController()
        registerBuiltinTools(
            registry = registry,
            knowledgeCaseProvider = { testKnowledgeCase("builtin-registration-knowledge") },
            healthProvider = { error("not used") },
            conversationCaseProvider = { testConversationCase("builtin-registration-conversation") },
            memoryCaseProvider = { testMemoryCase("builtin-registration-memory") },
            reminderCaseProvider = { testReminderCase("builtin-registration-reminder") },
        )

        val tools = registry.getRegisteredTools().associateBy { it.schema.name }
        val expected = setOf(
            "list_tools",
            "use_skill",
            "unload_skill",
            "health_check",
            "fetch_url",
            "conversation_search",
            "memory_save",
            "memory_recall",
            "memory_delete",
            "create_reminder",
            "list_reminders",
            "delete_reminder",
        )

        assertTrue(tools.keys.containsAll(expected))
        expected.forEach { toolName ->
            assertNull(tools.getValue(toolName).metadata.statusReason, "$toolName should be available")
        }
        assertEquals(true, tools.getValue("memory_recall").schema.defaultEnabled)
        assertEquals(true, tools.getValue("list_reminders").schema.defaultEnabled)
    }
}
