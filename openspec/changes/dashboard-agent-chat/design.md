# Design

## Backend Flow

`DashboardService.chatAgent(request)` resolves the current `AgentConfig`, optionally overlays a request-supplied config, resolves the configured provider by `providerName`, and creates a transient `AgentContext` seeded with the user message. It then runs `ReActRunner.stepUntilDone()`.

The endpoint is synchronous because the first Dashboard test workflow is a single request/response loop. It still captures runner hooks into ordered DTO events so the UI can display tool activity and failures.

## Request Shape

The request includes:

- `message`: required user message text.
- `config`: optional full Agent config for try-before-save edits.
- `conversationId`: optional logical test conversation id.

## Frontend Flow

`AgentView` displays the active Agent config, available providers and tools, a JSON editor for temporary config edits, and a chat panel. Sending a message calls `/api/agent/chat` with the edited config. The result is appended to the local transcript; saving config still uses the existing `/api/config` route.

## Boundaries

This does not persist Dashboard chat turns to conversation history and does not introduce streaming. Those can be layered later with a WebSocket or SSE route after the synchronous contract is stable.
