## ADDED Requirements

### Requirement: Dashboard Tool API SHALL expose tool policy state
The Dashboard API SHALL expose tool permission, enablement, source, and runtime status metadata.

#### Scenario: Tool list includes policy state
- **WHEN** a dashboard client calls `GET /api/tools`
- **THEN** each tool item SHALL include name, description, source, owner when available, risk level, required capabilities, default enabled state, effective enabled state, audit flag, and status reason when unavailable

#### Scenario: Tool list includes built-in and plugin tools
- **GIVEN** built-in and plugin tools are registered
- **WHEN** a dashboard client calls `GET /api/tools`
- **THEN** the response SHALL include both built-in and plugin tools with their source classification

#### Scenario: Tool health does not expose secrets
- **GIVEN** a tool is backed by provider, platform, plugin, memory, reminder, or network configuration
- **WHEN** a dashboard client requests tool metadata
- **THEN** the response SHALL NOT include API keys, tokens, prompts, message bodies, or memory contents

### Requirement: Dashboard health data SHALL align with health_check tool
The Dashboard API health data SHALL provide the same non-sensitive operational components used by the `health_check` built-in tool.

#### Scenario: Health components align
- **WHEN** a client requests `GET /health`
- **THEN** the response SHALL include component and diagnostic categories for database, providers, platforms, plugins, tools, and workspace reload state

#### Scenario: Health alignment preserves public endpoint safety
- **WHEN** a client requests `GET /health`
- **THEN** the response SHALL remain non-sensitive and suitable for unauthenticated health checks when Dashboard token auth leaves health public
