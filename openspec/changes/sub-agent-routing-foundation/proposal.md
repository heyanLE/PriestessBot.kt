# Sub-Agent Routing Foundation

## Why

v2 needs multi-Agent orchestration. The current runtime can execute one configured Agent and the Dashboard can test that Agent, but there is no way to define named sub-agents or route a request to a specialized Agent.

## What Changes

- Add serializable sub-agent orchestration config with enabled flag, sub-agent list, and keyword routing rules.
- Add a lightweight `SubAgentOrchestrator` that selects a route by keyword match and runs the selected Agent through the existing ReAct runner.
- Add Dashboard API routes for reading/replacing orchestration config and testing route selection/execution.
- Reuse existing Provider, Tool, ContextManager, and Agent runtime.

## Impact

- Extends `PriestessConfig` with backward-compatible orchestration defaults.
- Adds first orchestration mode: deterministic keyword routing.
- Leaves handoff/chain/parallel modes for later changes.
