package com.heyanle.priestess.bot.knowledge

import kotlinx.serialization.Serializable

@Serializable
data class KnowledgeBase(
    val id: String,
    val name: String,
    val description: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class KnowledgeChunk(
    val id: String,
    val knowledgeBaseId: String,
    val documentName: String,
    val content: String,
    val createdAt: Long,
)

data class KnowledgeSearchResult(
    val chunk: KnowledgeChunk,
    val score: Double,
)
