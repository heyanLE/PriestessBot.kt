package com.heyanle.priestess.bot.workspace

import com.heyanle.priestess.bot.config.AgentConfig
import com.heyanle.priestess.bot.skill.SkillCase
import com.heyanle.priestess.bot.tool.ToolController
import com.heyanle.priestess.bot.tool.ToolListing
import com.heyanle.priestess.bot.tool.ToolListingFilters
import java.util.concurrent.atomic.AtomicLong

class WorkspaceController(
    private val source: WorkspaceConfigSource,
    private val toolController: ToolController,
    private val skillCase: SkillCase,
    private val mcpToolResolver: WorkspaceMcpToolResolver = WorkspaceMcpToolResolver.Noop,
    private val nowProvider: () -> Long = System::currentTimeMillis,
) {
    private val versionCounter = AtomicLong(0)
    private val lock = Any()
    private val snapshots = linkedMapOf<String, WorkspaceSnapshot>()
    private val statuses = linkedMapOf<String, WorkspaceStatus>()
    private val lastReloads = linkedMapOf<String, WorkspaceReloadResult>()
    private val snapshotRefs = linkedMapOf<WorkspaceSnapshotKey, Int>()
    private val retiredSnapshots = linkedSetOf<WorkspaceSnapshotKey>()

    init {
        reloadAll()
    }

    fun list(): List<WorkspaceStatus> = synchronized(lock) {
        statuses.values.sortedBy { it.id }.toList()
    }

    fun get(id: String): WorkspaceSnapshot? = synchronized(lock) {
        snapshots[id]
    }

    fun resolve(context: WorkspaceResolutionContext = WorkspaceResolutionContext()): WorkspaceResolution {
        synchronized(lock) {
            context.metadata["workspace_id"]?.let { requested ->
                snapshots[requested]?.takeIf { it.enabled }?.let {
                    return WorkspaceResolution(it, "metadata workspace_id", WorkspaceSnapshotLease(this, it))
                }
            }
            context.metadata["workspaceId"]?.let { requested ->
                snapshots[requested]?.takeIf { it.enabled }?.let {
                    return WorkspaceResolution(it, "metadata workspaceId", WorkspaceSnapshotLease(this, it))
                }
            }
            snapshots.values.firstOrNull { snapshot ->
                snapshot.enabled && snapshot.config.resolution.platformNames.any { it == context.platformName }
            }?.let { return WorkspaceResolution(it, "platform rule", WorkspaceSnapshotLease(this, it)) }
            snapshots.values.firstOrNull { snapshot ->
                snapshot.enabled && snapshot.config.resolution.sessionIds.any { it == context.sessionId }
            }?.let { return WorkspaceResolution(it, "session rule", WorkspaceSnapshotLease(this, it)) }
            snapshots.values.firstOrNull { snapshot ->
                snapshot.enabled && snapshot.config.resolution.userIds.any { it == context.userId }
            }?.let { return WorkspaceResolution(it, "user rule", WorkspaceSnapshotLease(this, it)) }
            snapshots[WorkspaceConfig.DEFAULT_WORKSPACE_ID]?.takeIf { it.enabled }?.let {
                return WorkspaceResolution(it, "default workspace", WorkspaceSnapshotLease(this, it))
            }
            snapshots.values.firstOrNull { it.enabled }?.let {
                return WorkspaceResolution(it, "first enabled workspace", WorkspaceSnapshotLease(this, it))
            }
        }
        error("No enabled workspace snapshot available")
    }

    fun reload(id: String): WorkspaceReloadResult {
        val configSet = source.load()
        val validation = validate(configSet.workspaces)
        val target = configSet.workspaces.find { it.id == id }
        if (target == null) {
            return failedReload(id, listOf("Workspace '$id' not found in source") + validation.diagnostics)
        }
        if (!validation.valid) {
            return failedReload(id, validation.diagnostics)
        }
        return publishCandidate(target, configSet.diagnostics)
    }

    fun reloadAll(): List<WorkspaceReloadResult> {
        val configSet = source.load()
        val validation = validate(configSet.workspaces)
        if (!validation.valid) {
            return configSet.workspaces.map { config ->
                failedReload(config.id, validation.diagnostics + configSet.diagnostics)
            }
        }
        return configSet.workspaces.map { publishCandidate(it, configSet.diagnostics + validation.diagnostics) }
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
            val unknownSkills = config.skills.filter { it.enabled }.map { it.name } - skillCase.getAll().map { it.name }.toSet()
            unknownSkills.forEach { diagnostics += "Workspace '${config.id}' references unknown skill '$it'" }
            val knownTools = toolController.getAll().map { it.schema.name }.toSet()
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
        }
        return if (diagnostics.isEmpty()) WorkspaceValidationResult.ok() else WorkspaceValidationResult.failed(diagnostics)
    }

    private fun publishCandidate(config: WorkspaceConfig, sourceDiagnostics: List<String>): WorkspaceReloadResult {
        val oldSnapshot = synchronized(lock) { snapshots[config.id] }
        val candidate = try {
            buildSnapshot(config, sourceDiagnostics)
        } catch (cause: WorkspaceSnapshotBuildException) {
            return failedReload(
                config.id,
                sourceDiagnostics + listOf(cause.message ?: "Workspace snapshot build failed"),
            )
        }
        val plan = plan(oldSnapshot, candidate)
        val result = WorkspaceReloadResult(
            workspaceId = config.id,
            success = true,
            status = "SUCCESS",
            snapshotVersion = candidate.version,
            timestamp = nowProvider(),
            plan = plan,
            diagnostics = candidate.diagnostics,
        )
        synchronized(lock) {
            val oldSnapshot = snapshots.put(config.id, candidate)
            lastReloads[config.id] = result
            statuses[config.id] = candidate.toStatus(result)
            oldSnapshot?.let { retireSnapshotLocked(it) }
        }
        return result
    }

    private fun buildSnapshot(config: WorkspaceConfig, sourceDiagnostics: List<String>): WorkspaceSnapshot {
        val enabledSkills = scopedSkillNames(config)
        val enabledToolNames = scopedToolNames(config, hasVisibleSkills = enabledSkills.isNotEmpty())
        val skillSettings = scopedSkillSettings(config, enabledSkills)
        val enabledMcpServers = config.mcpServers
            .filter { it.enabled }
            .map { it.copy(env = emptyMap()) }
        val mcpResolution = resolveMcpTools(config.id, enabledMcpServers)
        val mcpHandles = (mcpResolution.handles + mcpResolution.resources.map { it.handle }).distinct()
        val version = versionCounter.incrementAndGet()
        val loadedAt = nowProvider()
        val agents = config.agents.ifEmpty { listOf(AgentConfig()) }
        val providerName = config.providerName.ifBlank { agents.first().providerName }
        val diagnostics = sourceDiagnostics + mcpResolution.diagnostics + buildList {
            if (!config.enabled) add("Workspace '${config.id}' is disabled")
        }
        val mcpToolNames = mcpResolution.toolNames.sorted()
        return WorkspaceSnapshot(
            id = config.id,
            name = config.name,
            enabled = config.enabled,
            version = version,
            loadedAt = loadedAt,
            config = config,
            agentConfigs = agents,
            providerName = providerName,
            toolNames = (enabledToolNames + mcpToolNames).distinct().sorted(),
            skillNames = enabledSkills,
            skillSettings = skillSettings,
            mcpServerIds = enabledMcpServers.map { it.id },
            mcpServers = enabledMcpServers,
            mcpToolNames = mcpToolNames,
            mcpResources = mcpResolution.resources,
            mcpHandles = mcpHandles,
            personaIds = config.personas.filter { it.enabled }.map { it.id },
            memoryPolicy = config.memory,
            diagnostics = diagnostics,
        )
    }

    private fun resolveMcpTools(
        workspaceId: String,
        servers: List<WorkspaceMcpServerConfig>,
    ): WorkspaceMcpToolResolution {
        if (servers.isEmpty()) return WorkspaceMcpToolResolution()
        return try {
            mcpToolResolver.resolve(workspaceId, servers)
        } catch (cause: WorkspaceMcpResolutionException) {
            cause.handles.forEach { handle ->
                runCatching { handle.close() }
            }
            throw WorkspaceSnapshotBuildException(
                "Workspace '$workspaceId' MCP initialization failed: ${cause.message ?: cause::class.simpleName}",
                cause,
            )
        } catch (cause: Exception) {
            throw WorkspaceSnapshotBuildException(
                "Workspace '$workspaceId' MCP initialization failed: ${cause.message ?: cause::class.simpleName}",
                cause,
            )
        }
    }

    private fun scopedToolNames(config: WorkspaceConfig, hasVisibleSkills: Boolean): List<String> {
        val allTools = ToolListing.list(
            registeredTools = toolController.getRegisteredTools(),
            filters = ToolListingFilters(includeHighRisk = true),
        ).filter { it.statusReason == null }
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

    private fun scopedSkillNames(config: WorkspaceConfig): List<String> {
        val enabled = config.skills.filter { it.enabled }.map { it.name }.toSet()
        return skillCase.getAll()
            .asSequence()
            .filter { enabled.isEmpty() || it.name in enabled }
            .map { it.name }
            .sorted()
            .toList()
    }

    private fun scopedSkillSettings(config: WorkspaceConfig, enabledSkillNames: List<String>): Map<String, Map<String, String>> {
        val enabled = enabledSkillNames.toSet()
        return config.skills
            .asSequence()
            .filter { it.enabled && it.name in enabled }
            .associate { it.name to it.settings }
    }

    private fun plan(old: WorkspaceSnapshot?, candidate: WorkspaceSnapshot): WorkspaceReloadPlan {
        val oldResources = old?.resourceKeys().orEmpty()
        val nextResources = candidate.resourceKeys()
        val modified = buildList {
            if (old != null && old.providerName != candidate.providerName) add("provider")
            if (old != null && old.memoryPolicy != candidate.memoryPolicy) add("memory_policy")
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
        if (current <= 0 && retiredSnapshots.remove(key)) {
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

    private class WorkspaceSnapshotBuildException(
        message: String,
        cause: Throwable,
    ) : RuntimeException(message, cause)
}
