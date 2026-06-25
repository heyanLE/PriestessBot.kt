# Dashboard Agent Chat

## Why

The Dashboard frontend can inspect runtime configuration, providers, tools, and plugins, but operators cannot yet test the configured Agent from the Dashboard. v2 needs a direct management loop where an operator edits Agent settings and immediately verifies the response path.

## What Changes

- Add Dashboard API DTOs for Agent config and synchronous chat testing.
- Add `POST /api/agent/chat` that runs the configured ReAct runner against a user message.
- Return final/error response plus step events such as tool starts and tool completions.
- Add an Agent Dashboard view with configuration summary, editable config JSON, provider/tool context, and chat transcript.

## Impact

- Reuses existing `AgentCase`, `ReActRunner`, `ProviderCase`, `ToolExecutor`, and `ToolController`.
- Extends `DashboardService` dependencies and tests.
- Adds a new frontend route without changing the bot pipeline behavior.
