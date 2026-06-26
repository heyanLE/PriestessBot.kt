package com.heyanle.priestess.bot.persona

import kotlinx.serialization.Serializable

/**
 * 人设档案，描述智能体在指定工作区中的表达风格和行为边界。
 */
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

/**
 * 人设保存请求，用于创建或更新一个工作区内的人设档案。
 */
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
