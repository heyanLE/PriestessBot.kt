# Design

## Goals

- Make sub-agent routing usable from the Dashboard without hand-crafting API calls.
- Keep the interface consistent with the existing dense operational Dashboard style.
- Preserve advanced configuration flexibility by editing the serialized config directly.

## UX

The view has two primary work areas:

- Config editor: JSON editor for `SubAgentOrchestrationConfig`, with reset/save actions and compact metrics.
- Test runner: prompt input that calls `/api/sub-agents/test`, then displays selected agent, route, reason, status, response content, and execution events.

A route summary panel lists the saved draft's agents and routes so operators can quickly scan enabled state and keyword coverage without reading the whole JSON payload.

## Data Flow

- On mount, call `GET /api/sub-agents/config`.
- Reset restores the last loaded/saved config into the editor.
- Save validates JSON client-side, then calls `PUT /api/sub-agents/config`.
- Test validates JSON client-side and sends the draft config with the message to `POST /api/sub-agents/test`, allowing experimentation before saving.

## Non-Goals

- No visual rule builder yet.
- No backend changes.
- No authentication or authorization.
