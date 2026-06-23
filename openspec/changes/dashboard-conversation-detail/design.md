# Design

## Frontend

The list view stays at `/conversations`. A new detail view at `/conversations/:id` loads:

- current conversation metadata from the store list
- message history from `dashboardApi.messages(id, count)`

The detail view renders messages in chronological order as compact transcript rows. Tool-related fields are shown inline and raw JSON is kept in a preformatted block for debugging.

If the store does not yet contain the selected conversation, the view refreshes global Dashboard state before rendering metadata.

## Backend Tests

The existing API route already returns message DTOs. This change adds route tests that seed a conversation and messages through `ConversationCase`, then assert:

- `/api/conversations` includes the session
- `/api/conversations/{id}/messages` returns chronological messages
- the `count` query limits to recent messages
- tool call payload and tool call id survive DTO mapping
