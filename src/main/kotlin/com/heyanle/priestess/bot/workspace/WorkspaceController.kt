package com.heyanle.priestess.bot.workspace

import com.heyanle.priestess.bot.core.controller.BaseController
import com.heyanle.priestess.bot.skill.SkillCase
import com.heyanle.priestess.bot.tool.ToolCase
import com.heyanle.priestess.bot.tool.ToolListingFilters
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicLong
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name

/**
 * 工作区控制器，负责根据目录实时构建、发布和回收工作区运行快照。
 */
class WorkspaceController(
    private val source: WorkspaceConfigSource,
    private val toolCase: ToolCase,
    private val skillCase: SkillCase? = null,
    private val mcpToolResolver: WorkspaceMcpToolResolver = WorkspaceMcpToolResolver.Noop,
    private val nowProvider: () -> Long = System::currentTimeMillis,
) : BaseController("WorkspaceController") {
    private val versionCounter = AtomicLong(0)
    private val lock = Any()
    private val loader = WorkspaceDirectoryLoader()
    private val snapshots = linkedMapOf<String, WorkspaceSnapshot>()
    private val statuses = linkedMapOf<String, WorkspaceStatus>()
    private val lastReloads = linkedMapOf<String, WorkspaceReloadResult>()
    private val snapshotRefs = linkedMapOf<WorkspaceSnapshotKey, Int>()
    private val retiredSnapshots = linkedSetOf<WorkspaceSnapshotKey>()
    private val closedSnapshotKeys = linkedSetOf<WorkspaceSnapshotKey>()

    init {
        bootstrapDefaultSnapshot()
    }

    fun list(): List<WorkspaceStatus> = synchronized(lock) {
        statuses.values.sortedBy { it.id }.toList()
    }

    fun get(id: String): WorkspaceSnapshot? = synchronized(lock) {
        snapshots[id]
    }

    /**
     * Resolves a prepared workspace for a message. An explicit workspace id wins;
     * otherwise the most specific configured resolution rule is used, followed by
     * the configured default workspace.
     */
    fun resolve(context: WorkspaceResolutionContext = WorkspaceResolutionContext()): WorkspaceResolution {
        val settings = source.load()
        val requestedId = context.metadata["workspace_id"]
            ?: context.metadata["workspaceId"]
        val selected = synchronized(lock) {
            requestedId
                ?.takeIf { it.isNotBlank() }
                ?.let(snapshots::get)
                ?.let { it to "metadata workspace_id" }
                ?: settings.workspaces
                    .asSequence()
                    .filter { it.enabled }
                    .sortedByDescending(::resolutionSpecificity)
                    .firstOrNull { it.matches(context) }
                    ?.let { config -> snapshots[config.id]?.let { it to "workspace resolution rule" } }
                ?: settings.workspaces
                    .firstOrNull { it.isDefault && it.enabled }
                    ?.let { config -> snapshots[config.id]?.let { it to "default workspace" } }
                ?: snapshots[WorkspaceConfig.DEFAULT_WORKSPACE_ID]?.let { it to "default workspace" }
                ?: snapshots.values.firstOrNull()?.let { it to "first prepared workspace" }
        } ?: error("No prepared workspace is available")
        return WorkspaceResolution(
            snapshot = selected.first,
            reason = selected.second,
            lease = WorkspaceSnapshotLease(this, selected.first),
        )
    }

    fun prepare(workspaceDir: String, reason: String): WorkspaceResolution {
        val settings = source.load()
        val candidate = buildSnapshotForDirectory(workspaceDir, settings, settings.diagnostics)
        publishPreparedSnapshot(candidate)
        return WorkspaceResolution(candidate, reason, WorkspaceSnapshotLease(this, candidate))
    }

    fun reload(id: String): WorkspaceReloadResult {
        val current = synchronized(lock) { snapshots[id] }
            ?: return failedReload(id, listOf("Workspace '$id' was not prepared yet"))
        return try {
            val settings = source.load()
            if (current.rootDir.isBlank()) {
                return failedReload(id, listOf("Workspace '$id' has no directory root"))
            }
            val candidate = buildSnapshotForDirectory(current.rootDir, settings, settings.diagnostics)
            publishReloadCandidate(current, candidate)
        } catch (cause: WorkspaceSnapshotBuildException) {
            failedReload(id, listOf(cause.message ?: "Workspace reload failed"))
        }
    }

    fun reloadAll(): List<WorkspaceReloadResult> {
        val currentSnapshots = synchronized(lock) { snapshots.values.toList() }
        if (currentSnapshots.isEmpty()) {
            val snapshot = bootstrapDefaultSnapshot()
            return listOf(
                WorkspaceReloadResult(
                    workspaceId = snapshot.id,
                    success = true,
                    status = "SUCCESS",
                    snapshotVersion = snapshot.version,
                    timestamp = nowProvider(),
                    diagnostics = snapshot.diagnostics,
                ),
            )
        }
        return currentSnapshots.map { reload(it.id) }
    }

    fun validate(workspaces: List<WorkspaceConfig>): WorkspaceValidationResult {
        val diagnostics = mutableListOf<String>()
        if (workspaces.isEmpty()) {
            diagnostics += "At least one workspace is required"
        }
        val duplicateIds = workspaces.groupingBy { it.id }.eachCount().filterValues { it > 1 }.keys
        duplicateIds.forEach { diagnostics += "Duplicate workspace id '$it'" }
        workspaces.forEach { config ->
            if (config.id.isBlank()) diagnostics += "Workspace id must not be blank"
            if (config.name.isBlank()) diagnostics += "Workspace '${config.id}' name must not be blank"
            val duplicateSkillNames = config.skills.groupingBy { it.name }.eachCount().filterValues { it > 1 }.keys
            duplicateSkillNames.forEach { diagnostics += "Workspace '${config.id}' has duplicate skill '$it'" }
            val knownTools = toolCase.getAll().map { it.schema.name }.toSet()
            val unknownEnabledTools = config.tools.enabledTools.filter { it !in knownTools }
            unknownEnabledTools.forEach { diagnostics += "Workspace '${config.id}' references unknown enabled tool '$it'" }
            val unknownDisabledTools = config.tools.disabledTools.filter { it !in knownTools }
            unknownDisabledTools.forEach { diagnostics += "Workspace '${config.id}' references unknown disabled tool '$it'" }
            val duplicateMcpIds = config.mcpServers.groupingBy { it.id }.eachCount().filterValues { it > 1 }.keys
            duplicateMcpIds.forEach { diagnostics += "Workspace '${config.id}' has duplicate MCP server '$it'" }
            config.mcpServers.filter { it.enabled }.forEach { server ->
                if (server.id.isBlank()) diagnostics += "Workspace '${config.id}' has blank MCP server id"
                if (server.transport == "stdio" && server.command.isBlank()) {
                    diagnostics += "Workspace '${config.id}' MCP server '${server.id}' requires command"
                }
            }
            skillCase?.let { skills ->
                val knownSkills = skills.getAll().map { it.name }.toSet()
                val unknownSkills = config.skills.filter { it.enabled }.map { it.name } - knownSkills
                unknownSkills.forEach { diagnostics += "Workspace '${config.id}' references unknown skill '$it'" }
            }
        }
        return if (diagnostics.isEmpty()) WorkspaceValidationResult.ok() else WorkspaceValidationResult.failed(diagnostics)
    }

    fun close() {
        val snapshotsToClose = synchronized(lock) {
            snapshots.values.toList().also { currentSnapshots ->
                currentSnapshots.forEach { retiredSnapshots.add(it.key()) }
            }
        }
        synchronized(lock) {
            snapshotsToClose.forEach { snapshot ->
                tryCloseSnapshotLocked(snapshot)
            }
        }
    }

    override suspend fun stop() {
        close()
        super.stop()
    }

    internal fun retainSnapshot(snapshot: WorkspaceSnapshot) {
        synchronized(lock) {
            val key = snapshot.key()
            snapshotRefs[key] = (snapshotRefs[key] ?: 0) + 1
        }
    }

    internal fun releaseSnapshot(snapshot: WorkspaceSnapshot) {
        synchronized(lock) {
            releaseSnapshotLocked(snapshot)
        }
    }

    private fun bootstrapDefaultSnapshot(): WorkspaceSnapshot {
        val settings = source.load()
        val snapshot = if (settings.defaultWorkspaceDir.isNotBlank()) {
            buildSnapshotForDirectory(settings.defaultWorkspaceDir, settings, settings.diagnostics)
        } else {
            buildSyntheticDefaultSnapshot(settings, settings.diagnostics + "No default workspace directory configured")
        }
        publishPreparedSnapshot(snapshot)
        return snapshot
    }

    private fun buildSnapshotForDirectory(
        workspaceDir: String,
        settings: WorkspaceConfigSet,
        sourceDiagnostics: List<String>,
    ): WorkspaceSnapshot {
        val normalizedRoot = normalizeDirectory(workspaceDir)
        if (normalizedRoot.isBlank()) {
            return buildSyntheticDefaultSnapshot(
                settings,
                sourceDiagnostics + "Workspace directory was blank; using synthetic default snapshot",
            )
        }
        val rootPath = Path.of(normalizedRoot)
        if (!rootPath.exists() || !rootPath.isDirectory()) {
            return buildSyntheticDefaultSnapshot(
                settings,
                sourceDiagnostics + "Workspace directory '$normalizedRoot' was not found or is not a directory",
                normalizedRoot,
            )
        }

        val loadResult = loader.load(rootPath, settings.defaults.baseConfig)
        val resolvedId = resolveWorkspaceId(
            normalizedRoot = normalizedRoot,
            explicitId = loadResult.explicitId,
            configuredId = loadResult.config.id,
            defaultWorkspaceDir = settings.defaultWorkspaceDir,
        )
        val resolvedName = resolveWorkspaceName(
            normalizedRoot = normalizedRoot,
            explicitName = loadResult.explicitName,
            configuredName = loadResult.config.name,
        )
        val resolvedConfig = loadResult.config.copy(
            id = resolvedId,
            name = resolvedName,
            isDefault = resolvedId == WorkspaceConfig.DEFAULT_WORKSPACE_ID,
        )
        val visibleSkills = loadResult.skillDescriptors
        val enabledToolNames = scopedToolNames(resolvedConfig, hasVisibleSkills = visibleSkills.isNotEmpty())
        val mcpDeclarations = loadResult.mcpServers
            .filter { it.enabled }
            .map { it.copy(env = emptyMap()) }
        val version = versionCounter.incrementAndGet()
        val loadedAt = nowProvider()

        return WorkspaceSnapshot(
            id = resolvedId,
            name = resolvedName,
            enabled = resolvedConfig.enabled,
            version = version,
            loadedAt = loadedAt,
            rootDir = normalizedRoot,
            rules = resolvedConfig.rules,
            config = resolvedConfig,
            agentConfigs = resolvedConfig.agents.ifEmpty { settings.defaults.baseConfig.agents },
            providerName = resolvedConfig.providerName.ifBlank {
                resolvedConfig.agents.firstOrNull()?.providerName ?: settings.defaults.baseConfig.providerName
            },
            toolNames = enabledToolNames,
            skillDescriptors = visibleSkills,
            skillSettings = visibleSkills.associate { it.name to it.settings },
            mcpServers = mcpDeclarations,
            personaIds = resolvedConfig.personas.filter { it.enabled }.map { it.id },
            memoryPolicy = resolvedConfig.memory,
            diagnostics = (sourceDiagnostics + loadResult.diagnostics).distinct(),
        )
    }

    private fun buildSyntheticDefaultSnapshot(
        settings: WorkspaceConfigSet,
        diagnostics: List<String>,
        rootDir: String = settings.defaultWorkspaceDir,
    ): WorkspaceSnapshot {
        val base = settings.defaults.baseConfig
        val version = versionCounter.incrementAndGet()
        return WorkspaceSnapshot(
            id = WorkspaceConfig.DEFAULT_WORKSPACE_ID,
            name = base.name,
            enabled = base.enabled,
            version = version,
            loadedAt = nowProvider(),
            rootDir = normalizeDirectory(rootDir),
            rules = base.rules,
            config = base,
            agentConfigs = base.agents,
            providerName = base.providerName.ifBlank { base.agents.firstOrNull()?.providerName.orEmpty() },
            toolNames = scopedToolNames(base, hasVisibleSkills = false),
            skillDescriptors = emptyList(),
            skillSettings = emptyMap(),
            mcpServers = emptyList(),
            personaIds = base.personas.filter { it.enabled }.map { it.id },
            memoryPolicy = base.memory,
            diagnostics = diagnostics.distinct(),
        )
    }

    private fun publishPreparedSnapshot(candidate: WorkspaceSnapshot) {
        synchronized(lock) {
            val previous = snapshots.put(candidate.id, candidate)
            statuses[candidate.id] = candidate.toStatus(lastReloads[candidate.id])
            previous?.let { retireSnapshotLocked(it) }
        }
    }

    private fun publishReloadCandidate(
        current: WorkspaceSnapshot,
        candidate: WorkspaceSnapshot,
    ): WorkspaceReloadResult {
        val plan = plan(current, candidate)
        val result = WorkspaceReloadResult(
            workspaceId = candidate.id,
            success = true,
            status = "SUCCESS",
            snapshotVersion = candidate.version,
            timestamp = nowProvider(),
            plan = plan,
            diagnostics = candidate.diagnostics,
        )
        synchronized(lock) {
            val previous = snapshots.put(candidate.id, candidate)
            lastReloads[candidate.id] = result
            statuses[candidate.id] = candidate.toStatus(result)
            previous?.let { retireSnapshotLocked(it) }
        }
        return result
    }

    private fun scopedToolNames(config: WorkspaceConfig, hasVisibleSkills: Boolean): List<String> {
        val allTools = toolCase.list(ToolListingFilters(includeHighRisk = true))
            .filter { it.statusReason == null }
        val controlTools = if (hasVisibleSkills) setOf("use_skill", "unload_skill") else emptySet()
        val registeredControlTools = controlTools.intersect(allTools.map { it.name }.toSet())
        val enabled = config.tools.enabledTools.toSet() + registeredControlTools
        val disabled = config.tools.disabledTools.toSet()
        return allTools
            .asSequence()
            .filter { if (config.tools.enabledTools.isEmpty()) it.effectiveEnabled else it.name in enabled }
            .filter { it.name !in disabled }
            .filter { it.riskLevel in config.tools.allowedRiskLevels }
            .map { it.name }
            .sorted()
            .toList()
    }

    private fun plan(old: WorkspaceSnapshot?, candidate: WorkspaceSnapshot): WorkspaceReloadPlan {
        val oldResources = old?.resourceKeys().orEmpty()
        val nextResources = candidate.resourceKeys()
        val modified = buildList {
            if (old != null && old.providerName != candidate.providerName) add("provider")
            if (old != null && old.memoryPolicy != candidate.memoryPolicy) add("memory_policy")
            if (old != null && old.rootDir != candidate.rootDir) add("root_dir")
            if (old != null && old.rules != candidate.rules) add("rules")
        }
        return WorkspaceReloadPlan(
            workspaceId = candidate.id,
            oldVersion = old?.version,
            newVersion = candidate.version,
            added = (nextResources - oldResources).sorted(),
            removed = (oldResources - nextResources).sorted(),
            modified = modified,
        )
    }

    private fun failedReload(id: String, diagnostics: List<String>): WorkspaceReloadResult {
        val result = WorkspaceReloadResult(
            workspaceId = id,
            success = false,
            status = "FAILED",
            snapshotVersion = synchronized(lock) { snapshots[id]?.version },
            timestamp = nowProvider(),
            diagnostics = diagnostics,
            errorSummary = diagnostics.firstOrNull() ?: "Workspace reload failed",
        )
        synchronized(lock) {
            lastReloads[id] = result
            snapshots[id]?.let { statuses[id] = it.toStatus(result) }
        }
        return result
    }

    private fun retireSnapshotLocked(snapshot: WorkspaceSnapshot) {
        retiredSnapshots.add(snapshot.key())
        tryCloseSnapshotLocked(snapshot)
    }

    private fun releaseSnapshotLocked(snapshot: WorkspaceSnapshot) {
        val key = snapshot.key()
        val current = snapshotRefs[key] ?: 0
        val next = (current - 1).coerceAtLeast(0)
        if (next <= 0) {
            snapshotRefs.remove(key)
        } else {
            snapshotRefs[key] = next
        }
        tryCloseSnapshotLocked(snapshot)
    }

    private fun tryCloseSnapshotLocked(snapshot: WorkspaceSnapshot) {
        val key = snapshot.key()
        val current = snapshotRefs[key] ?: 0
        if (current <= 0 && retiredSnapshots.remove(key) && closedSnapshotKeys.add(key)) {
            snapshot.closeMcpHandles()
        }
    }

    private fun WorkspaceSnapshot.resourceKeys(): Set<String> {
        return buildSet {
            toolNames.forEach { add("tool:$it") }
            skillNames.forEach { add("skill:$it") }
            mcpServerIds.forEach { add("mcp:$it") }
            mcpToolNames.forEach { add("mcp_tool:$it") }
            personaIds.forEach { add("persona:$it") }
            agentConfigs.forEach { add("agent:${it.name}") }
            rules.forEach { add("rule:$it") }
        }
    }

    private fun WorkspaceSnapshot.toStatus(lastReload: WorkspaceReloadResult?): WorkspaceStatus {
        return WorkspaceStatus(
            id = id,
            name = name,
            enabled = enabled,
            activeSnapshotVersion = version,
            loadedAt = loadedAt,
            lastReload = lastReload,
            diagnostics = diagnostics,
        )
    }

    private fun normalizeDirectory(path: String): String {
        return path.trim().takeIf { it.isNotBlank() }
            ?.let { Path.of(it).toAbsolutePath().normalize().toString() }
            .orEmpty()
    }

    private fun WorkspaceConfig.matches(context: WorkspaceResolutionContext): Boolean {
        val rule = resolution
        return rule.platformNames.matchesOrEmpty(context.platformName) &&
            rule.sessionIds.matchesOrEmpty(context.sessionId) &&
            rule.userIds.matchesOrEmpty(context.userId) &&
            (rule.platformNames.isNotEmpty() || rule.sessionIds.isNotEmpty() || rule.userIds.isNotEmpty())
    }

    private fun resolutionSpecificity(config: WorkspaceConfig): Int {
        val rule = config.resolution
        return listOf(rule.platformNames, rule.sessionIds, rule.userIds).count { it.isNotEmpty() }
    }

    private fun List<String>.matchesOrEmpty(value: String): Boolean = isEmpty() || value in this

    private fun resolveWorkspaceId(
        normalizedRoot: String,
        explicitId: Boolean,
        configuredId: String,
        defaultWorkspaceDir: String,
    ): String {
        val normalizedDefault = normalizeDirectory(defaultWorkspaceDir)
        if (normalizedDefault.isNotBlank() && normalizedRoot == normalizedDefault) {
            return if (explicitId && configuredId.isNotBlank()) configuredId else WorkspaceConfig.DEFAULT_WORKSPACE_ID
        }
        if (explicitId && configuredId.isNotBlank()) {
            return configuredId
        }
        val rootPath = Path.of(normalizedRoot)
        val base = rootPath.name.ifBlank { "workspace" }
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "workspace" }
        val suffix = normalizedRoot.hashCode().toUInt().toString(16).take(8)
        return "$base-$suffix"
    }

    private fun resolveWorkspaceName(
        normalizedRoot: String,
        explicitName: Boolean,
        configuredName: String,
    ): String {
        if (explicitName && configuredName.isNotBlank()) return configuredName
        return runCatching { Path.of(normalizedRoot).name }
            .getOrNull()
            ?.ifBlank { configuredName }
            ?: configuredName
    }

    private class WorkspaceSnapshotBuildException(
        message: String,
        cause: Throwable,
    ) : RuntimeException(message, cause)
}
