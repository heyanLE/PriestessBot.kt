## MODIFIED Requirements

### Requirement: Provider runtime management
The system SHALL expose built-in and plugin-provided provider metadata and connectivity tests through management APIs.

#### Scenario: Plugin provider metadata exposed
- **GIVEN** a plugin has registered a provider
- **WHEN** a dashboard client requests providers
- **THEN** the plugin-provided provider metadata is returned

#### Scenario: Plugin provider test exposed
- **GIVEN** a plugin has registered a provider
- **WHEN** a dashboard client requests provider tests
- **THEN** the plugin-provided provider participates in the result map
