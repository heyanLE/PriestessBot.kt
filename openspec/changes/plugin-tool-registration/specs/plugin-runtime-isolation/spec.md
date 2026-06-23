## MODIFIED Requirements

### Requirement: Plugin context registration
The system SHALL provide a plugin context for registering extension metadata and executable tools.

#### Scenario: Plugin registers executable tool
- **WHEN** a plugin calls `registerTool`
- **THEN** the tool is registered with the runtime tool registry
- **AND** ownership is associated with the plugin id for cleanup
