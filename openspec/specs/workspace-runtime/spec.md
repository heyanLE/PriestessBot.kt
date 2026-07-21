# workspace-runtime Specification

## Purpose
TBD - created while syncing archived changes. Update Purpose after archive.

## Requirements

### Requirement: Workspace directory layout
The system SHALL treat a workspace as a filesystem directory whose runtime source of truth is read from disk and whose control files are centered on `config.yaml`, `skills/`, and `mcpserver.json`.

#### Scenario: Directory-backed workspace is prepared
- **WHEN** the runtime prepares a workspace from a valid directory path
- **THEN** it reads `config.yaml` as workspace configuration
- **AND** it scans `skills/` for workspace skill directories
- **AND** it reads `mcpserver.json` for MCP server declarations

#### Scenario: Optional workspace assets are absent
- **WHEN** a workspace directory has a valid `config.yaml` but missing or empty `skills/` or `mcpserver.json`
- **THEN** the runtime treats the missing input as an empty workspace asset set
- **AND** it may record diagnostics without failing the snapshot by default

### Requirement: Workspace directory resolution
The system SHALL resolve the effective workspace directory from layered runtime inputs with increasing precedence from config defaults to platform config extensions to message metadata.

#### Scenario: Config default is used when no override exists
- **GIVEN** the Config module publishes a default workspace directory
- **AND** neither platform config nor message metadata supplies an override
- **WHEN** a message enters workspace preparation
- **THEN** the runtime uses the Config module default workspace directory

#### Scenario: Higher-precedence sources override lower-precedence sources
- **GIVEN** the Config module default workspace directory is set
- **AND** `PlatformConfig.config` supplies a different workspace directory
- **AND** message metadata supplies another workspace directory
- **WHEN** the effective workspace directory is resolved
- **THEN** the message metadata workspace directory wins
- **AND** the platform config workspace directory overrides the Config module default when message metadata is absent

### Requirement: Directory-backed workspace snapshots
The system SHALL build `WorkspaceSnapshot` instances by reading the resolved workspace directory at preparation time rather than by reusing a static serialized workspace config entry.

#### Scenario: Snapshot reflects current directory contents
- **GIVEN** a workspace directory changes on disk between two accepted messages
- **WHEN** each message prepares its workspace snapshot
- **THEN** each message reads the workspace directory state visible at its own preparation time
- **AND** later messages can observe newer directory-backed workspace configuration

#### Scenario: Snapshot remains pinned for the message lifetime
- **GIVEN** a message has already pinned a workspace snapshot prepared from a directory
- **WHEN** the workspace directory changes while that message continues processing
- **THEN** the in-flight message continues using the pinned snapshot
- **AND** the directory change only affects later messages

### Requirement: Workspace snapshot skill descriptors
The system SHALL store workspace skill metadata in the snapshot as descriptors containing identifiers and `SKILL.md` file paths rather than eagerly loaded prompt bodies.

#### Scenario: Snapshot stores skill metadata only
- **WHEN** a workspace snapshot is built from a directory containing valid skill folders
- **THEN** the snapshot records each enabled skill's name, description, directory path, and `SKILL.md` path
- **AND** it does not require loading the full `SKILL.md` content at snapshot construction time

### Requirement: Workspace snapshot MCP declarations
The system SHALL store parsed MCP server declarations from `mcpserver.json` in the workspace snapshot for future runtime use even when MCP tools are not yet wrapped as executable workspace tools.

#### Scenario: Snapshot predeclares MCP servers
- **WHEN** `mcpserver.json` contains valid MCP server declarations
- **THEN** the workspace snapshot records those declarations, source metadata, and diagnostics
- **AND** it does not require immediate MCP client initialization or tool wrapping

### Requirement: Workspace configuration model
The system SHALL define serializable workspace configuration as a runtime scope for Agent behavior.

#### Scenario: Workspace config contains runtime scopes
- **WHEN** a workspace config is decoded
- **THEN** it includes id, name, enabled state, agents, skills, MCP servers, tool policy, personas, memory policy, and provider selection values

#### Scenario: Workspace config defaults are backward-compatible
- **GIVEN** a deployment has no explicit workspace configuration
- **WHEN** the runtime starts
- **THEN** the system creates an enabled default workspace derived from the existing global runtime configuration

### Requirement: Immutable workspace snapshots
The system SHALL build immutable workspace snapshots from workspace configuration and registered runtime resources.

#### Scenario: Snapshot contains resolved runtime resources
- **GIVEN** a valid workspace config
- **WHEN** the workspace snapshot is built
- **THEN** the snapshot contains the config, scoped tools, scoped skills, MCP-backed tools, personas, memory policy, agent/provider selection metadata, version, and loaded timestamp

#### Scenario: Snapshot cannot be partially observed
- **GIVEN** a workspace reload is building a candidate snapshot
- **WHEN** a message resolves a workspace before the candidate is published
- **THEN** it receives the previously active complete snapshot

### Requirement: Workspace controller
The system SHALL provide a workspace controller for listing, resolving, inspecting, and reloading workspace snapshots.

#### Scenario: List workspace status
- **WHEN** workspace status is listed
- **THEN** the result includes each workspace id, name, enabled state, active snapshot version, last reload result, loaded timestamp, and diagnostic summary

#### Scenario: Get active snapshot
- **GIVEN** a workspace has an active snapshot
- **WHEN** the snapshot is requested by id
- **THEN** the controller returns the active immutable snapshot for that workspace

#### Scenario: Resolve workspace from context
- **GIVEN** a platform message contains platform, session, user, group, and metadata context
- **WHEN** the workspace is resolved
- **THEN** the controller returns the matching enabled workspace snapshot
- **AND** records which resolution rule or fallback selected it

#### Scenario: Resolve falls back to default workspace
- **GIVEN** no explicit workspace rule matches a message context
- **WHEN** the workspace is resolved
- **THEN** the default enabled workspace snapshot is returned

### Requirement: Atomic workspace reload
The system SHALL reload each workspace by building a complete candidate snapshot before replacing the active snapshot reference.

#### Scenario: Successful reload atomically replaces snapshot
- **GIVEN** a workspace has active snapshot version `N`
- **WHEN** reload builds and validates candidate snapshot version `N+1`
- **THEN** the controller atomically publishes version `N+1`
- **AND** later workspace resolutions return version `N+1`

#### Scenario: Failed reload keeps old snapshot
- **GIVEN** a workspace has active snapshot version `N`
- **WHEN** reload fails validation or runtime resource initialization
- **THEN** version `N` remains active
- **AND** the reload result records failure details without publishing a partial snapshot

#### Scenario: Reload all reports per-workspace results
- **GIVEN** multiple workspaces are configured
- **WHEN** all workspaces are reloaded
- **THEN** each workspace returns an independent reload result
- **AND** a failure in one workspace does not replace that workspace snapshot or prevent successful publication for other valid workspaces

### Requirement: Workspace reload plan
The system SHALL compute a reload plan that summarizes changes between old and candidate snapshots.

#### Scenario: Reload plan identifies scoped changes
- **GIVEN** an old snapshot and a candidate snapshot
- **WHEN** a reload plan is computed
- **THEN** it marks added, removed, and modified skills, MCP servers, MCP tools, built-in/plugin tools, personas, agents, provider selections, and memory policy values

#### Scenario: Reload result includes plan summary
- **WHEN** a workspace reload completes
- **THEN** the reload result includes status, snapshot version, timestamp, plan summary, and any validation or initialization errors

### Requirement: Old snapshot resource cleanup
The system SHALL keep old snapshot resources available while in-flight messages that pinned them are still running, then release closeable resources after they are no longer needed.

#### Scenario: In-flight snapshot remains usable after reload
- **GIVEN** a message pinned snapshot version `N`
- **AND** a reload publishes snapshot version `N+1`
- **WHEN** the pinned message continues executing tools or MCP calls
- **THEN** it continues using snapshot version `N` resources until it completes

#### Scenario: Old closeable resources are released
- **GIVEN** no in-flight message is using an old snapshot
- **WHEN** the controller performs snapshot cleanup
- **THEN** closeable resources owned only by the old snapshot are closed
