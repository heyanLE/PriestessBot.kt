## ADDED Requirements

### Requirement: Layered test suites
The project SHALL maintain unit, integration, system, and regression tests with clear ownership boundaries.

#### Scenario: Unit tests cover deterministic module behavior
- **WHEN** a module has pure logic, boundary handling, state transitions, or failure conversion
- **THEN** its behavior SHALL be covered by unit tests that do not require external network services or credentials

#### Scenario: Integration tests cover module contracts
- **WHEN** behavior depends on handoffs between controllers, pipeline stages, Agent runner, tools, storage, or API services
- **THEN** integration tests SHALL verify the contract using in-process fakes or test application support

#### Scenario: System tests cover full message flow
- **WHEN** the system test suite runs
- **THEN** it SHALL include at least one credential-free message flow from platform message receipt through Agent final response delivery

#### Scenario: Regression tests cover fixed defects
- **WHEN** a bug is fixed in core Agent, tool, pipeline, Dashboard API, config reload, workspace, persona, or memory behavior
- **THEN** the fix SHALL include a regression test that fails without the fix

### Requirement: Shared fake fixtures
The project SHALL provide reusable fake fixtures for testing Agent and runtime flows without real external services.

#### Scenario: Fake provider scripts tool-call flow
- **WHEN** a test needs an LLM response sequence
- **THEN** it SHALL be able to use a fake provider that returns scripted final responses, tool calls, or provider errors

#### Scenario: Fake platform captures sent messages
- **WHEN** a pipeline or system test sends a response
- **THEN** it SHALL be able to use a fake platform that records outbound messages for assertions

#### Scenario: Fake tool returns success or failure
- **WHEN** an Agent or ToolExecutor test needs tool behavior
- **THEN** it SHALL be able to register fake tools with deterministic success, validation error, execution error, timeout, or permission-denied outcomes

#### Scenario: Metrics can be asserted in memory
- **WHEN** tests exercise pipeline, LLM, or tool behavior
- **THEN** they SHALL be able to assert metric increments without scraping a live HTTP endpoint

### Requirement: Credential-free quality gate
The required test gate SHALL run without real platform accounts, provider API keys, external MCP servers, or internet access.

#### Scenario: PR test command runs locally
- **WHEN** a contributor runs `./gradlew test`
- **THEN** the required unit, integration, system, and regression tests SHALL run using local in-process fixtures

#### Scenario: External credentials are absent
- **GIVEN** provider API keys and platform tokens are not configured
- **WHEN** the required test gate runs
- **THEN** tests SHALL NOT fail because those credentials are missing

### Requirement: Test directory ownership
The test tree SHALL organize tests by production module ownership and keep shared fixtures reusable.

#### Scenario: Module tests live near module boundaries
- **WHEN** tests are added for Agent, tools, pipeline stages, workspace, persona, memory, Dashboard API, or system flows
- **THEN** their package paths SHALL reflect the production module under test

#### Scenario: Shared fixtures avoid duplication
- **WHEN** multiple suites need the same fake provider, fake platform, fake tool, fake clock, config source, conversation store, or metrics helper
- **THEN** the fixture SHALL live in a shared test fixture package instead of being copied into each test
