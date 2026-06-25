## MODIFIED Requirements

### Requirement: Runtime health SHALL support deployment troubleshooting

The runtime SHALL expose enough non-sensitive health detail to verify the active config, storage location, and extension counts in deployed environments.

#### Scenario: NAS or Docker health identifies active paths

- **GIVEN** the runtime starts with an explicit config file and database path
- **WHEN** health is queried
- **THEN** diagnostics SHALL identify the active config path
- **AND** diagnostics SHALL identify the active database path
