package com.heyanle.priestess.bot.persona

import com.heyanle.priestess.bot.memory.MemoryCase
import com.heyanle.priestess.bot.memory.MemoryScopeContext
import com.heyanle.priestess.bot.memory.MemorySearchQuery
import com.heyanle.priestess.bot.memory.MemorySearchResult

data class PersonaMemoryInjectionContext(
    val workspaceId: String,
    val agentName: String,
    val platformId: String? = null,
    val sessionId: String? = null,
    val userId: String? = null,
    val message: String,
    val maxMemories: Int = 3,
    val allowedPersonaIds: Set<String>? = null,
)

data class PersonaMemoryInjection(
    val instructions: String,
    val metadata: Map<String, String>,
    val persona: Persona?,
    val memories: List<MemorySearchResult>,
) {
    val hasContent: Boolean
        get() = persona != null || memories.isNotEmpty()
}

class PersonaMemoryInjector(
    private val personaCase: PersonaCase,
    private val memoryCase: MemoryCase,
    private val maxMemorySnippetChars: Int = 320,
) {
    fun inject(
        baseInstructions: String,
        context: PersonaMemoryInjectionContext,
    ): PersonaMemoryInjection {
        val workspaceId = context.workspaceId.trim()
        val agentName = context.agentName.trim()
        require(workspaceId.isNotBlank()) { "Workspace id must not be blank" }
        require(agentName.isNotBlank()) { "Agent name must not be blank" }

        val persona = resolvePersona(workspaceId, agentName, context.allowedPersonaIds)
        val scopeContext = MemoryScopeContext(
            workspaceId = workspaceId,
            platformId = context.platformId,
            sessionId = context.sessionId,
            userId = context.userId,
            agentName = agentName,
        )
        val memories = if (context.maxMemories <= 0) {
            emptyList()
        } else {
            memoryCase.search(
                MemorySearchQuery(
                    query = context.message,
                    scopeContext = scopeContext,
                    limit = context.maxMemories.coerceIn(1, 20),
                ),
            )
        }
        val renderedSection = renderSection(persona, memories)
        val instructions = if (renderedSection.isBlank()) {
            baseInstructions
        } else {
            buildString {
                append(baseInstructions.trimEnd())
                if (isNotEmpty()) append("\n\n")
                append(renderedSection)
            }
        }
        val metadata = buildMetadata(persona, memories)
        return PersonaMemoryInjection(
            instructions = instructions,
            metadata = metadata,
            persona = persona,
            memories = memories,
        )
    }

    private fun renderSection(
        persona: Persona?,
        memories: List<MemorySearchResult>,
    ): String {
        if (persona == null && memories.isEmpty()) return ""
        return buildString {
            append("## Persona And Memory Context")
            if (persona != null) {
                append("\n\n### Persona: ")
                append(persona.name)
                if (persona.description.isNotBlank()) {
                    append("\nDescription: ")
                    append(persona.description)
                }
                if (persona.tone.isNotBlank()) {
                    append("\nTone: ")
                    append(persona.tone)
                }
                if (persona.boundaries.isNotEmpty()) {
                    append("\nBoundaries:")
                    persona.boundaries.forEach { boundary ->
                        append("\n- ")
                        append(boundary)
                    }
                }
                if (persona.systemPromptTemplate.isNotBlank()) {
                    append("\nInstructions:\n")
                    append(persona.systemPromptTemplate)
                }
            }
            if (memories.isNotEmpty()) {
                append("\n\n### Relevant Memories")
                memories.forEachIndexed { index, result ->
                    append("\n")
                    append(index + 1)
                    append(". [")
                    append(result.record.id)
                    append("] ")
                    append(result.record.type.name)
                    append(": ")
                    append(result.record.content.trim().take(maxMemorySnippetChars))
                    if (result.record.content.length > maxMemorySnippetChars) append("...")
                    append(" (reason: ")
                    append(result.matchReason)
                    append(")")
                }
            }
        }
    }

    private fun buildMetadata(
        persona: Persona?,
        memories: List<MemorySearchResult>,
    ): Map<String, String> {
        val metadata = mutableMapOf<String, String>()
        if (persona != null) {
            metadata["injected_persona_id"] = persona.id
            metadata["injectedPersonaId"] = persona.id
            metadata["injected_persona_name"] = persona.name
            metadata["injectedPersonaName"] = persona.name
        }
        if (memories.isNotEmpty()) {
            metadata["injected_memory_ids"] = memories.joinToString(",") { it.record.id }
            metadata["injectedMemoryIds"] = memories.joinToString(",") { it.record.id }
            metadata["injected_memory_scores"] = memories.joinToString(",") { "${it.record.id}:${it.score}" }
            metadata["injectedMemoryScores"] = memories.joinToString(",") { "${it.record.id}:${it.score}" }
            metadata["injected_memory_reasons"] = memories.joinToString(" | ") { "${it.record.id}:${it.matchReason}" }
            metadata["injectedMemoryReasons"] = memories.joinToString(" | ") { "${it.record.id}:${it.matchReason}" }
        }
        metadata["injected_memory_count"] = memories.size.toString()
        metadata["injectedMemoryCount"] = memories.size.toString()
        return metadata
    }

    private fun resolvePersona(
        workspaceId: String,
        agentName: String,
        allowedPersonaIds: Set<String>?,
    ): Persona? {
        if (allowedPersonaIds == null) {
            return personaCase.resolve(workspaceId, agentName)
        }
        val normalizedAgentName = agentName.trim()
        return personaCase.list(workspaceId)
            .asSequence()
            .filter { it.id in allowedPersonaIds }
            .filter { it.enabled }
            .filter { it.agentNames.isEmpty() || normalizedAgentName in it.agentNames }
            .sortedWith(
                compareByDescending<Persona> { normalizedAgentName in it.agentNames }
                    .thenByDescending { it.updatedAt },
            )
            .firstOrNull()
    }
}
