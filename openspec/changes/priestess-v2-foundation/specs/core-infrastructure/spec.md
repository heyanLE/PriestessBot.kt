## MODIFIED Requirements

### Requirement: Runtime startup
The system SHALL initialize the bot runtime and optionally start the Dashboard API server from the same application entrypoint.

#### Scenario: Bot starts without server
- **GIVEN** the Dashboard API server is disabled
- **WHEN** the application starts
- **THEN** the existing bot runtime starts normally

#### Scenario: Bot starts with server
- **GIVEN** the Dashboard API server is enabled
- **WHEN** the application starts
- **THEN** the bot runtime starts
- **AND** the Dashboard API server starts on the configured host and port

#### Scenario: Coordinated shutdown
- **WHEN** the process shuts down
- **THEN** platform jobs and server resources are stopped gracefully
