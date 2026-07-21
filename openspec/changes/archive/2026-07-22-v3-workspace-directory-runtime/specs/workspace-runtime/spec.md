## ADDED Requirements

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
