## MODIFIED Requirements

### Requirement: Plugin context registration
The system SHALL provide a plugin context for registering extension metadata, executable tools, and chat providers.

#### Scenario: Plugin registers executable provider
- **WHEN** a plugin calls `registerProvider`
- **THEN** the provider is registered with the runtime provider controller
- **AND** ownership is associated with the plugin id for cleanup
