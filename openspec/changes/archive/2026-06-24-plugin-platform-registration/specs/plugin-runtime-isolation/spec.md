## MODIFIED Requirements

### Requirement: Plugin context registration
The system SHALL provide a plugin context for registering extension metadata, executable tools, chat providers, and platform adapter factories.

#### Scenario: Plugin registers platform adapter
- **WHEN** a plugin calls `registerPlatform`
- **THEN** the platform adapter factory is registered with the runtime platform registry
- **AND** ownership is associated with the plugin id for cleanup
