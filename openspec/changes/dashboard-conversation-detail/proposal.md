# Dashboard Conversation Detail

## Why

The Dashboard can list conversations, but operators cannot inspect the message history behind a session. v2 needs a practical conversation detail workflow for debugging pipeline behavior, model replies, and tool calls.

## What Changes

- Add frontend route for conversation detail under `/conversations/:id`.
- Let the conversation list navigate into a selected session.
- Fetch and render message history from the existing `/api/conversations/{id}/messages` endpoint.
- Display roles, content, timestamps, tool call ids, and raw tool call payloads.
- Add backend route coverage for message history ordering and limit behavior.

## Impact

- Reuses existing conversation/message APIs and data model.
- Adds frontend-only detail UX plus server tests.
- No runtime pipeline behavior changes.
