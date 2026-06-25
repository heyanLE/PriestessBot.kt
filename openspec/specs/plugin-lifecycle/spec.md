# plugin-lifecycle Specification

## Purpose
TBD - created while syncing archived changes. Update Purpose after archive.

## Requirements

### Requirement: Plugin manifest model
The system SHALL define a plugin manifest model for local plugin discovery.

#### Scenario: Manifest parsed
- **WHEN** a plugin directory contains `plugin.json`
- **THEN** the system parses id, name, version, description, entrypoint, dependencies, and capabilities

### Requirement: Plugin lifecycle management
The system SHALL track plugin lifecycle state and allow enable/disable operations.

#### Scenario: Discover plugins
- **WHEN** plugin discovery scans the configured plugin directory
- **THEN** discovered plugin manifests appear in plugin listing results

#### Scenario: Enable plugin
- **WHEN** a discovered plugin is enabled
- **THEN** its lifecycle state becomes `ENABLED`

#### Scenario: Disable plugin
- **WHEN** an enabled plugin is disabled
- **THEN** its lifecycle state becomes `DISABLED`

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
### Requirement: Extension registry foundation
The system SHALL provide a registry surface for plugin-provided extension metadata.

#### Scenario: Extension metadata listed
- **WHEN** a plugin registers extension metadata
- **THEN** the registry can list extensions by capability type
