package com.heyanle.priestess.bot.workspace

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name

internal data class WorkspaceDirectoryLoadResult(
    val config: WorkspaceConfig,
    val explicitId: Boolean,
    val explicitName: Boolean,
    val skillDescriptors: List<WorkspaceSkillDescriptor>,
    val mcpServers: List<WorkspaceMcpServerDeclaration>,
    val diagnostics: List<String>,
)

internal class WorkspaceDirectoryLoader(
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun load(
        rootDir: Path,
        defaults: WorkspaceConfig,
    ): WorkspaceDirectoryLoadResult {
        val diagnostics = mutableListOf<String>()
        val configPath = rootDir.resolve("config.yaml")
        val skillsDir = rootDir.resolve("skills")
        val mcpPath = rootDir.resolve("mcpserver.json")

        val rawConfigMap = readYamlMap(configPath, diagnostics)
        val config = mergeConfig(defaults, rawConfigMap, diagnostics)
        val skillDescriptors = scanSkillDescriptors(skillsDir, config.skills, diagnostics)
        val mcpServers = parseMcpDeclarations(mcpPath, diagnostics)

        return WorkspaceDirectoryLoadResult(
            config = config,
            explicitId = rawConfigMap.containsKey("id"),
            explicitName = rawConfigMap.containsKey("name"),
            skillDescriptors = skillDescriptors,
            mcpServers = mcpServers,
            diagnostics = diagnostics,
        )
    }

    private fun readYamlMap(path: Path, diagnostics: MutableList<String>): Map<String, Any?> {
        if (!path.exists()) {
            diagnostics += "Workspace config file '${path.fileName}' was not found"
            return emptyMap()
        }
        return try {
            val loaded = Load(LoadSettings.builder().build()).loadFromString(Files.readString(path))
            @Suppress("UNCHECKED_CAST")
            (loaded as? Map<*, *>)?.entries
                ?.associate { (key, value) -> key.toString() to value }
                .orEmpty()
        } catch (cause: Exception) {
            diagnostics += "Failed to parse workspace config '${path.fileName}': ${cause.message ?: cause::class.simpleName}"
            emptyMap()
        }
    }

    private fun mergeConfig(
        defaults: WorkspaceConfig,
        rawConfigMap: Map<String, Any?>,
        diagnostics: MutableList<String>,
    ): WorkspaceConfig {
        if (rawConfigMap.isEmpty()) return defaults
        val normalized = normalizeConfigMap(rawConfigMap)
        val override = runCatching {
            json.decodeFromJsonElement(WorkspaceConfig.serializer(), toJsonElement(normalized))
        }.getOrElse { cause ->
            diagnostics += "Failed to decode workspace config '${rawConfigMap["name"] ?: defaults.name}': ${cause.message ?: cause::class.simpleName}"
            defaults
        }
        return defaults.copy(
            id = rawConfigMap["id"]?.toString()?.takeIf { it.isNotBlank() } ?: defaults.id,
            name = rawConfigMap["name"]?.toString()?.takeIf { it.isNotBlank() } ?: defaults.name,
            enabled = rawConfigMap["enabled"]?.asBoolean() ?: defaults.enabled,
            isDefault = rawConfigMap["isDefault"]?.asBoolean() ?: defaults.isDefault,
            rules = if (rawConfigMap.containsKey("rules")) override.rules else defaults.rules,
            agents = if (rawConfigMap.containsKey("agents")) override.agents else defaults.agents,
            providerName = if (rawConfigMap.containsKey("providerName")) override.providerName else defaults.providerName,
            skills = if (rawConfigMap.containsKey("skills")) override.skills else defaults.skills,
            mcpServers = if (rawConfigMap.containsKey("mcpServers")) override.mcpServers else defaults.mcpServers,
            tools = if (rawConfigMap.containsKey("tools")) override.tools else defaults.tools,
            personas = if (rawConfigMap.containsKey("personas")) override.personas else defaults.personas,
            memory = if (rawConfigMap.containsKey("memory")) override.memory else defaults.memory,
            subAgents = if (rawConfigMap.containsKey("subAgents")) override.subAgents else defaults.subAgents,
            resolution = if (rawConfigMap.containsKey("resolution")) override.resolution else defaults.resolution,
        )
    }

    private fun normalizeConfigMap(rawConfigMap: Map<String, Any?>): Map<String, Any?> {
        return rawConfigMap.toMutableMap().apply {
            val rawSkills = this["skills"]
            if (rawSkills is List<*>) {
                this["skills"] = rawSkills.map { item ->
                    when (item) {
                        is String -> mapOf("name" to item)
                        is Map<*, *> -> item.entries.associate { (key, value) -> key.toString() to value }
                        else -> item
                    }
                }
            }
        }
    }

    private fun scanSkillDescriptors(
        skillsDir: Path,
        configuredSkills: List<WorkspaceSkillConfig>,
        diagnostics: MutableList<String>,
    ): List<WorkspaceSkillDescriptor> {
        if (!skillsDir.exists()) {
            diagnostics += "Workspace skills directory '${skillsDir.fileName}' was not found"
            return emptyList()
        }
        if (!skillsDir.isDirectory()) {
            diagnostics += "Workspace skills path '${skillsDir.fileName}' is not a directory"
            return emptyList()
        }
        val configuredByName = configuredSkills.associateBy { it.name }
        val restrictedNames = configuredByName.keys.takeIf { it.isNotEmpty() }
        val discovered = Files.list(skillsDir).use { stream ->
            stream.toList()
                .filter { Files.isDirectory(it) }
                .mapNotNull { skillDir ->
                    val markdownPath = skillDir.resolve("SKILL.md")
                    if (!markdownPath.exists()) {
                        diagnostics += "Workspace skill '${skillDir.name}' is missing SKILL.md"
                        return@mapNotNull null
                    }
                    val metadata = parseSkillMetadata(markdownPath)
                    val inferredName = metadata["name"]?.takeIf { it.isNotBlank() } ?: skillDir.name
                    val config = configuredByName[inferredName]
                    if (restrictedNames != null && inferredName !in restrictedNames) {
                        return@mapNotNull null
                    }
                    if (config != null && !config.enabled) {
                        return@mapNotNull null
                    }
                    WorkspaceSkillDescriptor(
                        name = inferredName,
                        description = metadata["description"].orEmpty(),
                        enabled = config?.enabled ?: true,
                        directoryPath = skillDir.toAbsolutePath().normalize().toString(),
                        skillMarkdownPath = markdownPath.toAbsolutePath().normalize().toString(),
                        settings = config?.settings.orEmpty(),
                        requiredPermissionGroup = config?.requiredPermissionGroup ?: com.heyanle.priestess.bot.pipeline.PermissionGroup.OPERATOR,
                    )
                }
                .toList()
        }
        configuredByName.keys
            .filter { configured -> discovered.none { it.name == configured } }
            .forEach { missing ->
                diagnostics += "Workspace config references missing skill '$missing'"
            }
        return discovered.sortedBy { it.name }
    }

    private fun parseSkillMetadata(markdownPath: Path): Map<String, String> {
        val text = Files.readString(markdownPath)
        if (!text.startsWith("---")) return emptyMap()
        val end = text.indexOf("\n---", startIndex = 3)
        if (end <= 0) return emptyMap()
        val frontMatter = text.substring(3, end).trim()
        return runCatching {
            val loaded = Load(LoadSettings.builder().build()).loadFromString(frontMatter)
            @Suppress("UNCHECKED_CAST")
            (loaded as? Map<*, *>)?.entries
                ?.associate { (key, value) -> key.toString() to value.toString() }
                .orEmpty()
        }.getOrElse { emptyMap() }
    }

    private fun parseMcpDeclarations(
        mcpPath: Path,
        diagnostics: MutableList<String>,
    ): List<WorkspaceMcpServerDeclaration> {
        if (!mcpPath.exists()) {
            diagnostics += "Workspace MCP declaration file '${mcpPath.fileName}' was not found"
            return emptyList()
        }
        return try {
            val root = json.parseToJsonElement(Files.readString(mcpPath))
            when (root) {
                is JsonObject -> {
                    val mcpServers = root["mcpServers"]
                    when (mcpServers) {
                        is JsonObject -> parseMcpServerMap(mcpServers, mcpPath)
                        is JsonArray -> mcpServers.mapNotNull { parseMcpDeclaration(null, it, mcpPath) }.sortedBy { it.id }
                        null -> {
                            diagnostics += "Workspace MCP declaration file '${mcpPath.fileName}' does not contain 'mcpServers'"
                            emptyList()
                        }
                        else -> {
                            diagnostics += "Workspace MCP declarations '${mcpPath.fileName}' must use an object or array under 'mcpServers'"
                            emptyList()
                        }
                    }
                }
                is JsonArray -> root.mapNotNull { parseMcpDeclaration(null, it, mcpPath) }.sortedBy { it.id }
                else -> {
                    diagnostics += "Workspace MCP declarations '${mcpPath.fileName}' must be a JSON object or array"
                    emptyList()
                }
            }
        } catch (cause: Exception) {
            diagnostics += "Failed to parse MCP declarations '${mcpPath.fileName}': ${cause.message ?: cause::class.simpleName}"
            emptyList()
        }
    }

    private fun parseMcpServerMap(
        servers: JsonObject,
        mcpPath: Path,
    ): List<WorkspaceMcpServerDeclaration> {
        return servers.entries
            .mapNotNull { (id, element) -> parseMcpDeclaration(id, element, mcpPath) }
            .sortedBy { it.id }
    }

    private fun parseMcpDeclaration(
        fallbackId: String?,
        element: JsonElement,
        mcpPath: Path,
    ): WorkspaceMcpServerDeclaration? {
        val obj = element as? JsonObject ?: return null
        val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: fallbackId.orEmpty()
        if (id.isBlank()) return null
        return WorkspaceMcpServerDeclaration(
            id = id,
            enabled = obj["enabled"]?.jsonPrimitive?.booleanOrNull ?: true,
            transport = obj["transport"]?.jsonPrimitive?.contentOrNull ?: "stdio",
            command = obj["command"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            args = obj["args"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty(),
            url = obj["url"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            env = obj["env"]?.jsonObject?.mapValues { it.value.jsonPrimitive.contentOrNull.orEmpty() }.orEmpty(),
            sourcePath = mcpPath.toAbsolutePath().normalize().toString(),
        )
    }

    private fun toJsonElement(value: Any?): JsonElement {
        return when (value) {
            null -> JsonNull
            is JsonElement -> value
            is String -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is Map<*, *> -> JsonObject(value.entries.associate { (key, nested) -> key.toString() to toJsonElement(nested) })
            is Iterable<*> -> JsonArray(value.map { toJsonElement(it) })
            is Array<*> -> JsonArray(value.map { toJsonElement(it) })
            else -> JsonPrimitive(value.toString())
        }
    }

    private fun Any?.asBoolean(): Boolean? {
        return when (this) {
            is Boolean -> this
            is String -> this.equals("true", ignoreCase = true)
            else -> null
        }
    }
}
