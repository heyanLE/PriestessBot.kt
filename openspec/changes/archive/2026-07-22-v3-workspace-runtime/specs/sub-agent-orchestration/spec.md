## MODIFIED Requirements

### Requirement: Sub-agent routing config
The system SHALL support serializable sub-agent orchestration configuration scoped by workspace.

#### Scenario: Workspace provides agent and persona scope
- **GIVEN** a workspace config defines agents and personas
- **WHEN** the workspace snapshot is built
- **THEN** the snapshot exposes only the agents and personas configured for that workspace

### Requirement: Routed Agent execution
The system SHALL execute the selected sub-agent using the existing Agent runtime within the resolved workspace snapshot.

#### Scenario: Selected agent uses workspace provider and persona
- **GIVEN** a message resolved workspace snapshot version `N`
- **WHEN** the orchestrator selects an agent for that message
- **THEN** the selected agent, provider selection, persona, and execution limits are read from snapshot version `N`

### Requirement: Sub-agent route selection
The system SHALL select a configured sub-agent deterministically from an incoming message within the resolved workspace.

#### Scenario: Route selection is workspace-scoped
- **GIVEN** two workspaces define different route targets for the same keyword
- **WHEN** messages resolve different workspace snapshots
- **THEN** each message uses the route target from its resolved workspace snapshot

