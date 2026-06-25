## MODIFIED Requirements

### Requirement: Tool runtime listing
The system SHALL expose registered built-in and plugin-provided tool metadata through management APIs.

#### Scenario: Plugin tool metadata exposed
- **GIVEN** a plugin has registered a tool
- **WHEN** a dashboard client requests tools
- **THEN** the plugin-provided tool appears in the returned tool metadata
