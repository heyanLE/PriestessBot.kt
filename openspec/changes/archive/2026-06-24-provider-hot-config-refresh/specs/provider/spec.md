## MODIFIED Requirements

### Requirement: Provider lifecycle
The system SHALL expose configured and plugin-registered LLM providers to Agent execution.

#### Scenario: Config provider update affects later lookups
- **GIVEN** a `ProviderController` is already running
- **WHEN** provider config is updated or reloaded
- **THEN** later provider lookups use provider instances built from the latest enabled provider config

#### Scenario: Disabled config provider is removed
- **GIVEN** a provider was created from enabled provider config
- **WHEN** the provider config is updated with that provider disabled or removed
- **THEN** later provider listings and lookups no longer include the config-backed provider

#### Scenario: Plugin providers survive config refresh
- **GIVEN** a plugin or runtime component registered a provider directly
- **WHEN** provider config is updated or reloaded
- **THEN** the runtime-registered provider remains available until explicitly unregistered
