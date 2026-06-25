package com.heyanle.priestess.bot.testkit

import com.heyanle.priestess.bot.conversation.ConversationCase
import com.heyanle.priestess.bot.conversation.ConversationController
import com.heyanle.priestess.bot.conversation.MessageHistory
import com.heyanle.priestess.bot.core.db.DatabaseController
import com.heyanle.priestess.bot.knowledge.KnowledgeCase
import com.heyanle.priestess.bot.knowledge.KnowledgeController
import com.heyanle.priestess.bot.memory.MemoryCase
import com.heyanle.priestess.bot.memory.MemoryController
import com.heyanle.priestess.bot.persona.PersonaCase
import com.heyanle.priestess.bot.persona.PersonaController
import com.heyanle.priestess.bot.reminder.ReminderCase
import com.heyanle.priestess.bot.reminder.ReminderController
import java.nio.file.Files
import java.util.UUID

fun testConversationCase(prefix: String = "priestess-test"): ConversationCase {
    val dbPath = Files.createTempFile(prefix, ".sqlite").toString()
    return conversationCaseForPath(dbPath)
}

fun testInMemoryConversationCase(): ConversationCase {
    return conversationCaseForPath("file:${UUID.randomUUID()}?mode=memory&cache=shared")
}

private fun conversationCaseForPath(dbPath: String): ConversationCase {
    val database = DatabaseController(dbPath)
    return ConversationCase(
        controller = ConversationController(database),
        history = MessageHistory(database),
    )
}

fun testKnowledgeCase(prefix: String = "priestess-knowledge"): KnowledgeCase {
    val dbPath = Files.createTempFile(prefix, ".sqlite").toString()
    val database = DatabaseController(dbPath)
    return KnowledgeCase(KnowledgeController(database))
}

fun testMemoryCase(prefix: String = "priestess-memory"): MemoryCase {
    val dbPath = Files.createTempFile(prefix, ".sqlite").toString()
    val database = DatabaseController(dbPath)
    return MemoryCase(MemoryController(database))
}

fun testPersonaCase(prefix: String = "priestess-persona"): PersonaCase {
    val dbPath = Files.createTempFile(prefix, ".sqlite").toString()
    val database = DatabaseController(dbPath)
    return PersonaCase(PersonaController(database))
}

fun testPersonaMemoryControllers(
    prefix: String = "priestess-persona-memory",
): Pair<PersonaController, MemoryController> {
    val dbPath = Files.createTempFile(prefix, ".sqlite").toString()
    val database = DatabaseController(dbPath)
    return PersonaController(database) to MemoryController(database)
}

fun testReminderCase(prefix: String = "priestess-reminder"): ReminderCase {
    val dbPath = Files.createTempFile(prefix, ".sqlite").toString()
    val database = DatabaseController(dbPath)
    return ReminderCase(ReminderController(database))
}
