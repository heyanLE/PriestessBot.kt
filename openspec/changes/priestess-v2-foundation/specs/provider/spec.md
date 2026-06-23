## MODIFIED Requirements

### Requirement: Provider runtime management
The system SHALL expose provider metadata and connectivity tests through management APIs.

#### Scenario: Provider metadata exposed
- **WHEN** a dashboard client requests providers
- **THEN** registered provider metadata is returned

#### Scenario: Provider test exposed
- **WHEN** a dashboard client requests provider tests
- **THEN** each registered provider is tested
- **AND** a provider-name to boolean result map is returned
