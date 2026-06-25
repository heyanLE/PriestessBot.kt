# plugin-runtime-isolation Specification

## Purpose
TBD - created while syncing archived changes. Update Purpose after archive.

## Requirements

### Requirement: Plugin runtime interface
The system SHALL define a plugin runtime interface with load, enable, disable, and unload lifecycle hooks.

#### Scenario: Plugin entrypoint implements lifecycle
- **WHEN** a plugin manifest names an entrypoint class implementing `Plugin`
- **THEN** the manager can instantiate it
- **AND** call lifecycle hooks in order

### Requirement: Isolated plugin ClassLoader
The system SHALL load each executable plugin with a closeable per-plugin ClassLoader.

#### Scenario: Directory plugin loads jars
- **GIVEN** a plugin directory contains `plugin.json` and one or more jar files
- **WHEN** the plugin is loaded
- **THEN** the manager creates a ClassLoader using the plugin jar URLs
- **AND** stores the runtime record for that plugin

#### Scenario: Plugin unload closes ClassLoader
- **GIVEN** a plugin has been loaded
- **WHEN** the plugin is unloaded
- **THEN** the manager invokes unload hooks
- **AND** closes the plugin ClassLoader
- **AND** removes the runtime record

### Requirement: Plugin context registration
The system SHALL provide a plugin context for registering extension metadata.

#### Scenario: Plugin registers extension metadata
- **WHEN** a plugin calls `registerExtension`
- **THEN** the extension appears in `PluginExtensionRegistry`
- **AND** the extension is associated with the plugin id

#### Scenario: Plugin disabled unregisters extensions
- **WHEN** a plugin is disabled
- **THEN** extension metadata registered by that plugin is removed

#### Scenario: Plugin registers executable tool
- **WHEN** a plugin calls `registerTool`
- **THEN** the tool is registered with the runtime tool registry
- **AND** ownership is associated with the plugin id for cleanup

#### Scenario: Plugin registers executable provider
- **WHEN** a plugin calls `registerProvider`
- **THEN** the provider is registered with the runtime provider controller
- **AND** ownership is associated with the plugin id for cleanup

#### Scenario: Plugin registers platform adapter
- **WHEN** a plugin calls `registerPlatform`
- **THEN** the platform adapter factory is registered with the runtime platform registry
- **AND** ownership is associated with the plugin id for cleanup
### Requirement: Lifecycle failure isolation
The system SHALL isolate plugin lifecycle failures from the bot runtime.

#### Scenario: Plugin load fails
- **WHEN** a plugin entrypoint cannot be loaded or instantiated
- **THEN** the plugin state becomes `FAILED`
- **AND** the error is visible in its descriptor
- **AND** other plugins remain manageable
