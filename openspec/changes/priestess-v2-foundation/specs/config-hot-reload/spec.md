## ADDED Requirements

### Requirement: Explicit config reload
The system SHALL support reloading the active configuration from the configured config file without restarting the process.

#### Scenario: Reload from disk
- **WHEN** `POST /api/config/reload` is called
- **THEN** the system reads the configured config file
- **AND** publishes the reloaded config through all config flows
- **AND** returns the active config

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
