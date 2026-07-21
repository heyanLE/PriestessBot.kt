# config-hot-reload Specification

## Purpose
TBD - created while syncing archived changes. Update Purpose after archive.

## Requirements

### Requirement: Explicit config reload
The system SHALL support reloading the active configuration from the configured config file without restarting the process.

#### Scenario: Reload from disk
- **WHEN** `POST /api/config/reload` is called
- **THEN** the system reads the configured config file
- **AND** publishes the reloaded config through all config flows
- **AND** returns the active config

#### Scenario: Config reload makes workspace configs reloadable
- **WHEN** `POST /api/config/reload` reloads the active config
- **THEN** workspace config source settings are updated
- **AND** workspace snapshots remain unchanged until a workspace reload publishes a new snapshot

### Requirement: Config write publication
The system SHALL publish runtime config updates after dashboard API writes.

#### Scenario: API config replacement
- **WHEN** `PUT /api/config` receives a valid config
- **THEN** the config file is updated
- **AND** platform, provider, agent, database, and pipeline config flows emit the new values

### Requirement: Optional file watcher
The system SHALL provide an optional config file watcher that reloads changed config files.

#### Scenario: Watched file changes
- **GIVEN** config file watching is enabled
- **WHEN** the config file modification time changes
- **THEN** the active config is reloaded and published

### Requirement: Runtime config publication
The system SHALL publish updated config slices to runtime components after explicit updates or reloads, including workspace configuration sources.

#### Scenario: Provider controller observes provider config publication
- **GIVEN** Dashboard or config reload publishes updated provider config
- **WHEN** a later Agent execution requests a provider
- **THEN** the provider controller resolves providers from the latest published provider config without requiring process restart

#### Scenario: Pipeline observes published config for subsequent messages
- **GIVEN** Dashboard or config reload publishes updated Agent, pipeline, or sub-agent config
- **WHEN** the next message enters the pipeline
- **THEN** the pipeline uses the latest published config values without requiring process restart

#### Scenario: Workspace directory config publishes to later messages
- **GIVEN** Dashboard or config reload publishes an updated default workspace directory value
- **WHEN** a later message enters workspace preparation
- **THEN** the runtime resolves workspace directories using the latest published default workspace directory value
- **AND** it does not require process restart

#### Scenario: Workspace config source observes config publication
- **GIVEN** Dashboard or config reload publishes updated workspace configuration source values
- **WHEN** a later workspace reload is requested
- **THEN** the workspace controller reads from the latest published workspace config source values
- **AND** does not require process restart

### Requirement: Agent config strategy values SHALL map to executable runtime behavior

Agent configuration values exposed through config hot reload SHALL map to executable runtime behavior when the value is recognized.

#### Scenario: Configured llm_compress strategy is executable

- **GIVEN** an `AgentConfig` sets `compressStrategy` to `llm_compress`
- **WHEN** the agent is created and context compression is required
- **THEN** the runtime SHALL execute the configured compression strategy without crashing

### Requirement: Runtime configuration SHALL support server API token overrides

The config controller SHALL allow deployments to supply a Dashboard API token from the environment without rewriting the config file.

#### Scenario: Server API token is overridden from environment

- **GIVEN** the config file has a blank server API token
- **AND** `PRIESTESS_SERVER_API_TOKEN` is set
- **WHEN** the config is loaded
- **THEN** the effective server config SHALL use the environment token
- **AND** passive reload SHALL NOT write that token into the config file

### Requirement: Configuration files SHALL load robustly from disk

The runtime SHALL load configuration from disk without treating common first-run or editor encoding artifacts as malformed config.

#### Scenario: UTF-8 BOM config is accepted

- **GIVEN** a config file starts with a UTF-8 byte order mark followed by valid JSON
- **WHEN** the config controller loads or reloads the file
- **THEN** it SHALL decode the JSON config successfully
- **AND** it SHALL NOT replace the file with defaults because of the byte order mark

#### Scenario: Empty config file initializes defaults

- **GIVEN** a config path exists but contains only empty or whitespace text
- **WHEN** the config controller loads the file
- **THEN** it SHALL return the default config
- **AND** it SHALL persist the default config to that path
- **AND** it SHALL NOT create a backup for the empty placeholder file

#### Scenario: Malformed config is backed up

- **GIVEN** a config file contains non-empty malformed JSON
- **WHEN** the config controller loads the file
- **THEN** it SHALL back up the malformed file
- **AND** it SHALL replace the config with defaults

### Requirement: Runtime config persistence SHALL support backup and restore

The config controller SHALL maintain timestamped backups for persisted config replacements and allow restoring a previous valid config.

#### Scenario: Persisted replacement creates a timestamped backup

- **GIVEN** an active config file already exists with non-empty content
- **WHEN** the runtime persists a replacement config
- **THEN** a timestamped backup of the previous file content SHALL be created
- **AND** the replacement config SHALL become the active config

#### Scenario: Backup restore publishes config slices

- **GIVEN** a timestamped backup contains a valid config
- **WHEN** the backup is restored
- **THEN** the active config file SHALL contain the restored config
- **AND** config state flows SHALL publish the restored values

#### Scenario: Restore rejects unknown backup IDs

- **GIVEN** a caller provides an ID that is not a known backup file
- **WHEN** restore is requested
- **THEN** restore SHALL fail without modifying the active config

### Requirement: Default workspace directory configuration
The Config module SHALL expose a default workspace directory setting as part of effective runtime configuration.

#### Scenario: Config file defines default workspace directory
- **WHEN** the config file contains a default workspace directory value
- **THEN** the Config module publishes that value to runtime consumers

#### Scenario: Environment override changes effective default workspace directory
- **GIVEN** the config file contains a default workspace directory value
- **AND** an environment override for the default workspace directory is present
- **WHEN** the Config module loads effective runtime configuration
- **THEN** the environment override wins for runtime use
- **AND** passive reload does not persist the override back into the config file

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

### Requirement: Reload failure rollback SHALL have test coverage
Config and workspace reload behavior SHALL have tests proving invalid reload attempts do not replace the active runtime snapshot.

#### Scenario: Invalid config reload preserves active config
- **GIVEN** a valid active config is already published
- **WHEN** a reload reads invalid config data
- **THEN** a test SHALL verify the active config remains unchanged and the reload result reports failure

#### Scenario: Invalid workspace reload preserves active snapshot
- **GIVEN** a valid workspace snapshot is active
- **WHEN** workspace reload validation fails or snapshot construction fails
- **THEN** a test SHALL verify the old snapshot remains active for new message resolution

#### Scenario: In-flight messages keep prior snapshot
- **GIVEN** a message has entered the pipeline with a resolved config or workspace snapshot
- **WHEN** a later reload publishes a new snapshot
- **THEN** a test SHALL verify the in-flight message continues using the snapshot captured at pipeline entry
