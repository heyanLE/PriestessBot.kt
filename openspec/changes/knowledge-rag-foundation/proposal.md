# Knowledge RAG Foundation

## Why

v2 calls for knowledge-base/RAG support so Agents can answer from project or operator-managed content. The current runtime has tools, Dashboard APIs, and SQLite persistence, but no knowledge storage or retrieval path.

## What Changes

- Add SQLite-backed knowledge base and document chunk storage.
- Add a keyword retriever foundation with deterministic scoring.
- Add `KnowledgeCase` for creating/listing knowledge bases, adding text documents, and searching chunks.
- Add Dashboard REST routes for knowledge base management and retrieval testing.
- Add built-in `knowledge_search` tool so Agents can retrieve stored knowledge.

## Impact

- Extends database schema with knowledge base and chunk tables.
- Adds a lightweight first retriever that can later be replaced or augmented with vector stores.
- Adds tests for persistence, retrieval ranking, Dashboard APIs, and tool execution.
