## MODIFIED Requirements

### Requirement: Runtime config publication
The system SHALL publish updated config slices to runtime components after explicit updates or reloads, including workspace configuration sources.

#### Scenario: Workspace config source observes config publication
- **GIVEN** Dashboard or config reload publishes updated workspace configuration source values
- **WHEN** a later workspace reload is requested
- **THEN** the workspace controller reads from the latest published workspace config source values
- **AND** does not require process restart

### Requirement: Explicit config reload
The system SHALL support reloading the active configuration from the configured config file without restarting the process.

#### Scenario: Config reload makes workspace configs reloadable
- **WHEN** `POST /api/config/reload` reloads the active config
- **THEN** workspace config source settings are updated
- **AND** workspace snapshots remain unchanged until a workspace reload publishes a new snapshot

## ADDED Requirements

### Requirement: Workspace config source reload
The system SHALL provide workspace config sources that can read complete workspace config sets from supported sources.

#### Scenario: Explicit workspace source reload
- **WHEN** the workspace controller reloads a workspace
- **THEN** it reads the current workspace config from the configured workspace config source
- **AND** validates the complete workspace config before building a candidate snapshot

#### Scenario: Optional workspace file watcher
- **GIVEN** workspace file watching is enabled
- **WHEN** a workspace config file changes
- **THEN** the runtime schedules or performs a workspace reload for affected workspaces
- **AND** failed reloads keep the previous active snapshots

