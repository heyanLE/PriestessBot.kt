## MODIFIED Requirements

### Requirement: Runtime configuration SHALL support server API token overrides

The config controller SHALL allow deployments to supply a Dashboard API token from the environment without rewriting the config file.

#### Scenario: Server API token is overridden from environment

- **GIVEN** the config file has a blank server API token
- **AND** `PRIESTESS_SERVER_API_TOKEN` is set
- **WHEN** the config is loaded
- **THEN** the effective server config SHALL use the environment token
- **AND** passive reload SHALL NOT write that token into the config file
