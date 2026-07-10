package com.heyanle.priestess.bot.tool.builtin

import com.heyanle.priestess.bot.tool.AgentToolContext
import com.heyanle.priestess.bot.tool.FunctionTool
import com.heyanle.priestess.bot.tool.ParameterDef
import com.heyanle.priestess.bot.tool.ToolParameters
import com.heyanle.priestess.bot.tool.ToolResult
import com.heyanle.priestess.bot.tool.ToolRiskLevel
import com.heyanle.priestess.bot.tool.ToolSchema
import com.heyanle.priestess.bot.workspace.WorkspaceCase
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

class SkillsListTool : FunctionTool() {
    override val schema = ToolSchema(
        name = "skills_list",
        description = "List skills available in the current workspace.",
        riskLevel = ToolRiskLevel.SAFE_READ,
        defaultEnabled = false,
        auditLog = false,
    )

    private val json = Json { prettyPrint = true; encodeDefaults = true }

    override suspend fun execute(context: AgentToolContext, args: Map<String, String>): ToolResult {
        val workspace = context.workspaceLocationOrNull()
            ?: return ToolResult.error("Current context does not expose a workspace root", "WORKSPACE_REQUIRED")
        val skills = workspace.listSkills()
        return ToolResult.success(
            json.encodeToString(
                SkillsListResponse(
                    success = true,
                    skills = skills.map { SkillSummary(it.name, it.description) },
                    count = skills.size,
                ),
            ),
        )
    }
}

class SkillViewTool : FunctionTool() {
    override val schema = ToolSchema(
        name = "skill_view",
        description = "Read SKILL.md or another file from a workspace skill directory.",
        parameters = ToolParameters(
            properties = listOf(
                ParameterDef("name", description = "Skill name to inspect.", required = true),
                ParameterDef("file_path", description = "Optional relative path inside the skill directory."),
            ),
            required = listOf("name"),
        ),
        riskLevel = ToolRiskLevel.SAFE_READ,
        defaultEnabled = false,
        auditLog = false,
    )

    private val json = Json { prettyPrint = true; encodeDefaults = true }

    override suspend fun execute(context: AgentToolContext, args: Map<String, String>): ToolResult {
        val workspace = context.workspaceLocationOrNull()
            ?: return ToolResult.error("Current context does not expose a workspace root", "WORKSPACE_REQUIRED")
        val name = args["name"]?.trim().orEmpty()
        if (name.isBlank()) return ToolResult.error("name is required", "VALIDATION_ERROR")
        return runCatching {
            val skill = workspace.findSkillByName(name)
                ?: return ToolResult.error("Skill '$name' was not found", "SKILL_NOT_FOUND")
            val target = workspace.resolveSkillPath(name, args["file_path"])
            if (!target.exists()) {
                return ToolResult.error("File '${args["file_path"] ?: "SKILL.md"}' was not found in skill '$name'", "FILE_NOT_FOUND")
            }
            if (Files.isDirectory(target)) {
                return ToolResult.error("'${target.fileName}' is a directory", "IS_DIRECTORY")
            }
            val content = readUtf8(target)
            ToolResult.success(
                json.encodeToString(
                    SkillViewResponse(
                        success = true,
                        name = skill.name,
                        description = skill.description,
                        content = content,
                        filePath = skill.directory.relativize(target).toString(),
                    ),
                ),
            )
        }.getOrElse { error ->
            ToolResult.error(error.message ?: "Failed to read skill", "SKILL_VIEW_FAILED")
        }
    }
}

class SkillManageTool(
    private val workspaceCaseProvider: (() -> WorkspaceCase)? = null,
) : FunctionTool() {
    override val schema = ToolSchema(
        name = "skill_manage",
        description = "Create, edit, patch, delete, or manage files inside workspace skills.",
        parameters = ToolParameters(
            properties = listOf(
                ParameterDef("action", description = "One of create, edit, patch, delete, write_file, remove_file.", required = true),
                ParameterDef("name", description = "Skill name.", required = true),
                ParameterDef("content", description = "Full SKILL.md content for create or edit."),
                ParameterDef("category", description = "Optional category hint. Current workspace runtime keeps a flat skills directory."),
                ParameterDef("file_path", description = "Relative file path inside the skill directory."),
                ParameterDef("file_content", description = "Full file content for write_file."),
                ParameterDef("old_string", description = "Exact text to replace for patch."),
                ParameterDef("new_string", description = "Replacement text for patch."),
                ParameterDef("replace_all", type = "boolean", description = "Replace all matching fragments for patch."),
            ),
            required = listOf("action", "name"),
        ),
        riskLevel = ToolRiskLevel.HIGH_RISK,
        defaultEnabled = false,
        auditLog = true,
    )

    private val json = Json { prettyPrint = true; encodeDefaults = true }

    override suspend fun execute(context: AgentToolContext, args: Map<String, String>): ToolResult {
        val workspace = context.workspaceLocationOrNull()
            ?: return ToolResult.error("Current context does not expose a workspace root", "WORKSPACE_REQUIRED")
        val action = args["action"]?.trim().orEmpty()
        val name = args["name"]?.trim().orEmpty()
        if (action.isBlank() || name.isBlank()) return ToolResult.error("action and name are required", "VALIDATION_ERROR")

        return runCatching {
            val result = when (action) {
                "create" -> createSkill(workspace, name, args["content"], args["category"])
                "edit" -> editSkill(workspace, name, args["content"])
                "patch" -> patchSkill(workspace, name, args["file_path"], args["old_string"], args["new_string"], parseBooleanArg(args["replace_all"]))
                "delete" -> deleteSkill(workspace, name)
                "write_file" -> writeSkillFile(workspace, name, args["file_path"], args["file_content"])
                "remove_file" -> removeSkillFile(workspace, name, args["file_path"])
                else -> return ToolResult.error("Unknown action '$action'", "VALIDATION_ERROR")
            }
            ToolResult.success(json.encodeToString(result.copy(reload = reloadWorkspace(context))))
        }.getOrElse { error ->
            ToolResult.error(error.message ?: "Skill management failed", "SKILL_MANAGE_FAILED")
        }
    }

    private fun createSkill(workspace: WorkspaceToolLocation, name: String, content: String?, category: String?): SkillManageResponse {
        if (content.isNullOrBlank()) throw IllegalArgumentException("content is required for create")
        if (workspace.findSkillByName(name) != null) throw IllegalArgumentException("Skill '$name' already exists")
        val dirName = skillDirectoryName(name)
        val skillDir = workspace.ensureSkillsDir().resolve(dirName)
        Files.createDirectories(skillDir)
        writeUtf8(skillDir.resolve("SKILL.md"), content)
        return SkillManageResponse(
            success = true,
            message = "Created skill '$name'",
            warning = category?.takeIf { it.isNotBlank() }?.let { "category is currently informational only in this workspace runtime" },
        )
    }

    private fun editSkill(workspace: WorkspaceToolLocation, name: String, content: String?): SkillManageResponse {
        if (content.isNullOrBlank()) throw IllegalArgumentException("content is required for edit")
        val target = workspace.resolveSkillPath(name)
        val format = fileFormat(target)
        writeUtf8(target, normalizeContentForWrite(content, format))
        return SkillManageResponse(success = true, message = "Updated SKILL.md for '$name'")
    }

    private fun patchSkill(
        workspace: WorkspaceToolLocation,
        name: String,
        filePath: String?,
        oldString: String?,
        newString: String?,
        replaceAll: Boolean,
    ): SkillManageResponse {
        if (oldString.isNullOrEmpty()) throw IllegalArgumentException("old_string is required for patch")
        if (newString == null) throw IllegalArgumentException("new_string is required for patch")
        val target = workspace.resolveSkillPath(name, filePath)
        if (!target.exists()) throw IllegalArgumentException("Target file '${filePath ?: "SKILL.md"}' was not found")
        val format = fileFormat(target)
        val original = readUtf8(target)
        val occurrences = original.windowed(oldString.length, 1).count { it == oldString }
        if (occurrences == 0) throw IllegalArgumentException("old_string was not found")
        if (!replaceAll && occurrences > 1) throw IllegalArgumentException("old_string matched $occurrences locations; set replace_all=true to patch every match")
        val updated = if (replaceAll) original.replace(oldString, newString) else original.replaceFirst(oldString, newString)
        writeUtf8(target, normalizeContentForWrite(updated.removePrefix("\uFEFF"), format))
        return SkillManageResponse(success = true, message = "Patched ${target.fileName} for '$name'")
    }

    private fun deleteSkill(workspace: WorkspaceToolLocation, name: String): SkillManageResponse {
        val skill = workspace.findSkillByName(name) ?: throw IllegalArgumentException("Skill '$name' was not found")
        deleteRecursively(skill.directory)
        return SkillManageResponse(success = true, message = "Deleted skill '$name'")
    }

    private fun writeSkillFile(workspace: WorkspaceToolLocation, name: String, filePath: String?, fileContent: String?): SkillManageResponse {
        if (filePath.isNullOrBlank()) throw IllegalArgumentException("file_path is required for write_file")
        if (fileContent == null) throw IllegalArgumentException("file_content is required for write_file")
        val skill = workspace.findSkillByName(name) ?: throw IllegalArgumentException("Skill '$name' was not found")
        val target = resolveSkillOwnedPath(skill.directory, filePath)
        val format = fileFormat(target)
        val normalized = if (target.exists()) normalizeContentForWrite(fileContent, format) else fileContent
        writeUtf8(target, normalized)
        return SkillManageResponse(success = true, message = "Wrote '$filePath' for '$name'")
    }

    private fun removeSkillFile(workspace: WorkspaceToolLocation, name: String, filePath: String?): SkillManageResponse {
        if (filePath.isNullOrBlank()) throw IllegalArgumentException("file_path is required for remove_file")
        val skill = workspace.findSkillByName(name) ?: throw IllegalArgumentException("Skill '$name' was not found")
        val target = resolveSkillOwnedPath(skill.directory, filePath)
        if (!target.exists()) throw IllegalArgumentException("File '$filePath' was not found in skill '$name'")
        Files.delete(target)
        return SkillManageResponse(success = true, message = "Removed '$filePath' from '$name'")
    }

    private fun resolveSkillOwnedPath(skillDir: Path, relativePath: String): Path {
        val target = skillDir.resolve(Path.of(relativePath)).normalize()
        if (!target.startsWith(skillDir)) throw IllegalArgumentException("Skill file path escapes skill directory")
        return target
    }

    private fun skillDirectoryName(name: String): String {
        val sanitized = name.trim().replace(Regex("[^a-zA-Z0-9._-]+"), "-").trim('-')
        return sanitized.ifBlank { "skill" }
    }

    private fun reloadWorkspace(context: AgentToolContext): SkillManageReloadResult? {
        val workspaceId = context.metadata["workspaceId"]?.takeIf { it.isNotBlank() } ?: return null
        val provider = workspaceCaseProvider ?: return null
        return runCatching {
            val result = provider().reload(workspaceId)
            SkillManageReloadResult(success = result.success, status = result.status, snapshotVersion = result.snapshotVersion)
        }.getOrNull()
    }
}

@Serializable
data class SkillsListResponse(
    val success: Boolean,
    val skills: List<SkillSummary>,
    val count: Int,
)

@Serializable
data class SkillSummary(
    val name: String,
    val description: String,
)

@Serializable
data class SkillViewResponse(
    val success: Boolean,
    val name: String,
    val description: String,
    val filePath: String,
    val content: String,
)

@Serializable
data class SkillManageResponse(
    val success: Boolean,
    val message: String,
    val warning: String? = null,
    val reload: SkillManageReloadResult? = null,
)

@Serializable
data class SkillManageReloadResult(
    val success: Boolean,
    val status: String,
    val snapshotVersion: Long? = null,
)
