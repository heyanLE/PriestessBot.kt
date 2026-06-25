package com.heyanle.priestess.bot.persona

import kotlinx.serialization.Serializable

@Serializable
data class Persona(
    val id: String,
    val workspaceId: String,
    val name: String,
    val description: String = "",
    val tone: String = "",
    val boundaries: List<String> = emptyList(),
    val systemPromptTemplate: String = "",
    val enabled: Boolean = true,
    val agentNames: List<String> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

data class PersonaUpsertRequest(
    val id: String? = null,
    val workspaceId: String,
    val name: String,
    val description: String = "",
    val tone: String = "",
    val boundaries: List<String> = emptyList(),
    val systemPromptTemplate: String = "",
    val enabled: Boolean = true,
    val agentNames: List<String> = emptyList(),
)
