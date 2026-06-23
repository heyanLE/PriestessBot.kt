## MODIFIED Requirements

### Requirement: Agent config strategy values SHALL map to executable runtime behavior

Agent configuration values exposed through config hot reload SHALL map to executable runtime behavior when the value is recognized.

#### Scenario: Configured llm_compress strategy is executable

- **GIVEN** an `AgentConfig` sets `compressStrategy` to `llm_compress`
- **WHEN** the agent is created and context compression is required
- **THEN** the runtime SHALL execute the configured compression strategy without crashing
