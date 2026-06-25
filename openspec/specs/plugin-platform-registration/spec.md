# plugin-platform-registration Specification

## Purpose
TBD - created while syncing archived changes. Update Purpose after archive.

## Requirements

### Requirement: Plugin platform registration
The system SHALL allow executable plugins to register `Platform` adapter factories through `PluginContext`.

#### Scenario: Plugin registers platform factory
- **GIVEN** a loaded plugin has access to `PluginContext`
- **WHEN** the plugin calls `registerPlatform`
- **THEN** the platform metadata and factory are registered in `PlatformRegistry`
- **AND** the platform can be created by name or config

### Requirement: Plugin platform cleanup
The system SHALL remove platform registrations owned by a plugin when that plugin is disabled, unloaded, reloaded, or fails during lifecycle handling.

#### Scenario: Disable removes plugin platform registration
- **GIVEN** an enabled plugin registered a platform
- **WHEN** the plugin is disabled
- **THEN** the platform registration is removed from `PlatformRegistry`

#### Scenario: Failure removes plugin platform registration
- **GIVEN** a plugin registered a platform before a lifecycle failure
- **WHEN** the lifecycle operation fails
- **THEN** platform registrations owned by that plugin are removed

### Requirement: Idempotent plugin platform registration
The system SHALL avoid duplicate platform entries when a plugin is enabled repeatedly.

#### Scenario: Re-enable does not duplicate platform
- **GIVEN** a plugin has registered a platform
- **WHEN** the plugin is enabled again
- **THEN** only one platform metadata entry with that name exists in `PlatformRegistry`
