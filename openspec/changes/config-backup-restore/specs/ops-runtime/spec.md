## MODIFIED Requirements

### Requirement: Runtime deployment SHALL support config recovery

The runtime SHALL provide an operational recovery path for accidental config edits in deployed environments.

#### Scenario: NAS operator can recover a previous config

- **GIVEN** the runtime is using a persistent config file
- **AND** a previous config backup exists
- **WHEN** the operator restores that backup through the Dashboard API
- **THEN** the runtime SHALL use the restored config without requiring manual file copy operations
