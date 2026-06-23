## MODIFIED Requirements

### Requirement: Runtime configuration SHALL support deployment environment overrides

The runtime SHALL allow selected operational config values to be overridden by process environment variables after the config file is loaded.

#### Scenario: Server, storage, and plugin overrides apply at startup

- **GIVEN** a config file defines server, database, and plugin settings
- **AND** process environment variables define supported overrides for those settings
- **WHEN** the runtime loads configuration
- **THEN** the effective config SHALL use the environment values
- **AND** config slice flows SHALL publish the overridden values

#### Scenario: Overrides are reapplied after config reload

- **GIVEN** the runtime has loaded configuration with supported environment overrides
- **WHEN** the config file changes and configuration is reloaded
- **THEN** the reloaded effective config SHALL include the latest file values
- **AND** supported environment values SHALL still take precedence

#### Scenario: Loading config does not persist environment overrides

- **GIVEN** a config file defines a value
- **AND** an environment variable overrides that value
- **WHEN** the runtime loads or reloads configuration
- **THEN** the file content SHALL NOT be rewritten only because of the override

#### Scenario: Invalid override values are ignored

- **GIVEN** a config file defines valid operational settings
- **AND** process environment variables contain invalid values for supported numeric or boolean overrides
- **WHEN** the runtime loads configuration
- **THEN** invalid override values SHALL be ignored
- **AND** the effective config SHALL keep the file-backed values for those fields
