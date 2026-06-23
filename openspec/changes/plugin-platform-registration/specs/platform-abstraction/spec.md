## MODIFIED Requirements

### Requirement: Platform runtime management
The system SHALL expose built-in and plugin-provided platform adapter metadata and allow configured plugin platforms to start through existing config flow.

#### Scenario: Plugin platform metadata exposed
- **GIVEN** a plugin has registered a platform adapter
- **WHEN** platform metadata is requested from the registry
- **THEN** the plugin-provided platform metadata is returned

#### Scenario: Plugin platform created from config
- **GIVEN** a plugin has registered a platform adapter
- **WHEN** a matching enabled `PlatformConfig` is used
- **THEN** the platform adapter can be created through `PlatformRegistry.createFromConfig`
