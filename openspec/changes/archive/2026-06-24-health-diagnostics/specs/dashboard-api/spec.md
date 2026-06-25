## MODIFIED Requirements

### Requirement: Dashboard health endpoint SHALL expose operational diagnostics

The Dashboard health endpoint SHALL include lightweight diagnostics that help operators verify runtime wiring without exposing secrets.

#### Scenario: Health response includes diagnostics

- **WHEN** a client requests `GET /health`
- **THEN** the response SHALL include `status`, `components`, `timestamp`, and `uptimeMillis`
- **AND** the response SHALL include a `diagnostics` object
- **AND** diagnostics SHALL include config path, database path, configured platform count, running platform count, configured provider count, available provider count, registered tool count, configured plugin count, and loaded plugin extension count

#### Scenario: Health diagnostics exclude secrets

- **GIVEN** config contains provider API keys or platform tokens
- **WHEN** a client requests `GET /health`
- **THEN** the diagnostics SHALL NOT include provider API keys or platform tokens
