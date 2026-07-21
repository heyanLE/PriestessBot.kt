## MODIFIED Requirements

### Requirement: Runtime config publication
The system SHALL publish updated config slices to runtime components after explicit updates or reloads, including workspace directory source values used by workspace preparation.

#### Scenario: Workspace directory config publishes to later messages
- **GIVEN** Dashboard or config reload publishes an updated default workspace directory value
- **WHEN** a later message enters workspace preparation
- **THEN** the runtime resolves workspace directories using the latest published default workspace directory value
- **AND** it does not require process restart

## ADDED Requirements

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
