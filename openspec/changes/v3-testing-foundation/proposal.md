## Why

v3 needs a deliberate testing foundation so future changes to the Agent loop, tool execution, pipeline stages, Dashboard APIs, workspace reload, and persona/memory interfaces can be made safely. The current suite covers several modules, but the highest-risk flows still lack systematic unit, integration, system, and regression coverage.

## What Changes

- Establish a `testing-foundation` capability that defines the repository's layered testing contract, fixture strategy, quality gates, and regression expectations.
- Add required unit coverage for `ReActRunner`, context compression, `ToolExecutor`, pipeline stages, workspace reload, and persona/memory behavior.
- Add required integration coverage for Pipeline -> ReActRunner -> ToolExecutor -> Respond, config reload propagation, plugin extension registration/cleanup, Dashboard route contracts, knowledge search, and future workspace snapshots.
- Add a credential-free system test path using fake platform, fake provider, and fake tool fixtures to validate a complete ReAct tool-call message flow.
- Extend existing capabilities with testability requirements for Agent loop behavior, ToolExecutor behavior, pipeline stage behavior, Dashboard API contracts, config reload rollback, and runtime quality gates.

## Capabilities

### New Capabilities
- `testing-foundation`: Layered testing strategy, shared test fixtures, quality gates, and regression policy for v3.

### Modified Capabilities
- `agent-loop`: Require unit-level coverage for ReActRunner final, tool-call, provider error, tool error, max-step, and terminal-state behavior.
- `tool-mcp`: Require ToolExecutor tests for argument validation, unknown tools, exceptions, batch partial failures, permissions, timeouts, and metrics.
- `pipeline`: Require stage-level tests for allow/block/stop behavior, onion flow post-processing, AgentContext creation, and response sending.
- `dashboard-api`: Require route contract tests for all Dashboard management APIs, including future workspace/persona/memory endpoints as they land.
- `config-hot-reload`: Require reload validation tests proving failed workspace/config reloads do not replace the active snapshot.
- `ops-runtime`: Require CI and PR quality gates for unit, integration, system, and regression suites without real external credentials.

## Impact

- Adds OpenSpec artifacts and delta specs for v3 testing only.
- Future implementation will add or organize test source files under the existing Kotlin test tree and may introduce shared fake fixtures.
- Does not require real Telegram, NapCat, OpenAI, Anthropic, Gemini, MCP server, or Dashboard browser credentials.
- Defines expectations for future workspace/persona/memory modules before their implementation so those APIs ship with tests from the start.
