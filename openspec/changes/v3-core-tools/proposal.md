## Why

v3 needs a dependable default tool surface so an Agent can inspect runtime state, search recent context, fetch public information, remember durable facts, and schedule reminders without requiring plugins. The current tool framework exists, but core tool coverage, permission metadata, execution policy, timeout behavior, and Dashboard visibility are not yet specified as one coherent contract.

## What Changes

- Add a core tool permission model with risk levels, required capabilities, default enablement, audit metadata, and pre-execution policy checks.
- Add default built-in tools for `list_tools`, `health_check`, `fetch_url`, `conversation_search`, `memory_save`, `memory_recall`, `memory_delete`, `create_reminder`, `list_reminders`, and `delete_reminder`.
- Thread `Agent.toolTimeoutMs` through `ToolExecutor` so all tool calls have bounded execution.
- Expose tool permission, risk, enabled, source, and health/status data through Dashboard APIs and the ToolView.
- Define safety requirements for URL fetching, conversation search workspace isolation, memory scope/TTL handling, reminder binding/delivery, and non-sensitive health output.

## Capabilities

### New Capabilities
- `core-tools`: Core built-in Agent tools, permission policy, timeout execution, memory/reminder/search/fetch behavior, and tool self-inspection.

### Modified Capabilities
- `tool-mcp`: Extend tool schema/execution metadata and executor behavior for risk, capabilities, default enablement, policy checks, audit decisions, and timeouts.
- `dashboard-api`: Extend tool listing/health surfaces so operators can inspect tool permission, source, enabled state, and runtime status.
- `dashboard-frontend`: Update ToolView to render tool permission, risk, enabled/disabled, source, and health/status details.
- `agent-loop`: Ensure Agent configuration timeout is honored for tool calls during the loop.
- `conversation-management`: Add scoped historical message search behavior used by `conversation_search`.

## Impact

- Affects core tool model classes, built-in tool implementations, tool registration/execution, Agent configuration, and runtime DI wiring.
- Affects Dashboard API response models and frontend ToolView state rendering.
- Requires persistence/service integration for conversation history, persona memory, and reminders.
- Adds tests for tool policy decisions, built-in tool behavior, timeout handling, Dashboard API metadata, and ToolView rendering.
