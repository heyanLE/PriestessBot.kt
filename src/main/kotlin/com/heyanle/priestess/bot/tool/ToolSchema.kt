package com.heyanle.priestess.bot.tool

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * JSON Schema definition for a tool's parameters.
 * Describes the name, description, and parameter structure that LLMs use for tool calling.
 */
@Serializable
data class ToolSchema(
    val name: String,
    val description: String = "",
    val parameters: ToolParameters = ToolParameters(),
) {
    /**
     * Converts this schema to OpenAI function-calling format.
     * Returns a JsonObject matching the OpenAI tool definition structure:
     * { "type": "function", "function": { "name": "...", "description": "...", "parameters": {...} } }
     */
    fun toOpenAIFormat(): JsonObject {
        return buildJsonObject {
            put("type", "function")
            putJsonObject("function") {
                put("name", name)
                put("description", description)
                put("parameters", buildJsonObject {
                    put("type", "object")
                    put("properties", parameters.toJsonProperties())
                    if (parameters.required.isNotEmpty()) {
                        putJsonArray("required") {
                            for (req in parameters.required) {
                                add(JsonPrimitive(req))
                            }
                        }
                    }
                })
            }
        }
    }

    /**
     * Converts to Anthropic tool format.
     */
    fun toAnthropicFormat(): JsonObject {
        return buildJsonObject {
            put("name", name)
            put("description", description)
            put("input_schema", buildJsonObject {
                put("type", "object")
                put("properties", parameters.toJsonProperties())
                if (parameters.required.isNotEmpty()) {
                    putJsonArray("required") {
                        for (req in parameters.required) {
                            add(JsonPrimitive(req))
                        }
                    }
                }
            })
        }
    }
}

/**
 * Defines the parameters a tool accepts.
 */
@Serializable
data class ToolParameters(
    val properties: List<ParameterDef> = emptyList(),
    val required: List<String> = emptyList(),
) {
    fun toJsonProperties(): JsonObject {
        return buildJsonObject {
            for (prop in properties) {
                putJsonObject(prop.name) {
                    put("type", prop.type)
                    put("description", prop.description)
                    if (prop.enumValues.isNotEmpty()) {
                        putJsonArray("enum") {
                            for (v in prop.enumValues) {
                                add(JsonPrimitive(v))
                            }
                        }
                    }
                    if (prop.items != null) {
                        putJsonObject("items") {
                            put("type", prop.items)
                        }
                    }
                }
            }
        }
    }
}

/**
 * A single parameter definition within a tool's schema.
 */
@Serializable
data class ParameterDef(
    val name: String,
    val type: String = "string",
    val description: String = "",
    val required: Boolean = false,
    val enumValues: List<String> = emptyList(),
    val items: String? = null,
)
