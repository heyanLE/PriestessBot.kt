## MODIFIED Requirements

### Requirement: Plugin lifecycle management
The system SHALL track plugin lifecycle state and allow load, enable, disable, unload, and reload operations.

#### Scenario: Load plugin
- **WHEN** a discovered plugin is loaded
- **THEN** its lifecycle state becomes `LOADED`
- **AND** its `onLoad` hook has been called

#### Scenario: Enable loaded plugin
- **WHEN** a loaded plugin is enabled
- **THEN** its lifecycle state becomes `ENABLED`
- **AND** its `onEnable` hook has been called

#### Scenario: Disable enabled plugin
- **WHEN** an enabled plugin is disabled
- **THEN** its lifecycle state becomes `DISABLED`
- **AND** its `onDisable` hook has been called

#### Scenario: Unload plugin
- **WHEN** a loaded plugin is unloaded
- **THEN** its `onUnload` hook has been called
- **AND** extension metadata registered by the plugin is removed
- **AND** the plugin runtime is released
