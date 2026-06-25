# Dashboard Knowledge View

## Why

The RAG foundation exposes knowledge-base APIs and a `knowledge_search` tool, but operators still need an in-app surface to create bases, add documents, and test retrieval. v2 Dashboard should make this workflow available without raw HTTP calls.

## What Changes

- Add typed frontend API client support for knowledge bases, documents, and search results.
- Add Dashboard route `/knowledge` and sidebar navigation entry.
- Implement `KnowledgeView` for creating bases, adding text documents, and testing retrieval.
- Keep conversation detail routes out of sidebar navigation.

## Impact

- Frontend-only feature on top of existing Knowledge Dashboard API.
- No backend route changes.
- Adds build verification for the new view.
