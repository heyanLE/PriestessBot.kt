## ADDED Requirements

### Requirement: Runtime quality gates SHALL include required test execution
The runtime development workflow SHALL require the project's credential-free test gate before changes are considered complete.

#### Scenario: Full test suite is required
- **WHEN** a v3 implementation task is completed
- **THEN** `./gradlew test` SHALL be run successfully or the blocker SHALL be documented

#### Scenario: New public APIs require tests
- **WHEN** a new public API is added in Agent, tool, pipeline, Dashboard, workspace, persona, memory, provider, platform, plugin, or config modules
- **THEN** the change SHALL include unit or route/integration tests for the API's success and failure behavior

#### Scenario: New tools require standard tool tests
- **WHEN** a new tool is added
- **THEN** the change SHALL include tests for schema exposure, successful execution, argument errors, execution failure, permission denial, and metrics

#### Scenario: System test covers ReAct tool chain
- **WHEN** the required test gate runs
- **THEN** at least one system test SHALL cover user message receipt, fake provider tool call, fake tool execution, provider final response, platform send, conversation history update, and metrics increments
