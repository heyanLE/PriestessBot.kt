# v3 Persona Memory Core

## Why

v3 needs to move Agent personality and long-term memory out of implicit prompt text and into explicit, manageable runtime modules. Today `Agent.instructions`, conversation history, knowledge RAG, and tools can approximate this behavior, but operators cannot configure persona independently, scope or expire memory, delete specific memory records, or inspect which persona and memory were injected into a run.

## What Changes

- Add Persona and MemoryRecord domain models with workspace, agent, session, user, platform, TTL, confidence, tags, and enabled/deleted lifecycle fields.
- Add PersonaController and MemoryController contracts for listing, resolving, saving, searching, deleting, and expiring records.
- Add a deterministic keyword MemoryRetriever as the v3 foundation, with a retriever contract that can later be backed by embeddings or vector stores.
- Add PersonaMemoryInjector that renders persona and relevant memory into Agent system prompt additions and records injection trace metadata.
- Add built-in `memory_save`, `memory_recall`, and `memory_delete` tools with write/delete safety rules.
- Add Dashboard REST APIs and frontend views for persona and memory management.
- Extend Agent chat test responses to expose persona/memory injection trace for operator debugging.

## Capabilities

### New Capabilities
- `persona-memory`: Persona models, memory records, memory retrieval, prompt injection, scope isolation, TTL, deletion, and injection trace behavior.

### Modified Capabilities
- `agent-loop`: Inject resolved persona and relevant memory before the runner initializes system messages, and expose injection trace metadata.
- `tool-mcp`: Add `memory_save`, `memory_recall`, and `memory_delete` built-in tools with explicit read/write/delete safety behavior.
- `dashboard-api`: Add persona and memory management endpoints and include injection traces in Agent chat test responses.
- `dashboard-frontend`: Add persona and memory management views and display Agent chat injection traces.

## Impact

- Introduces new persistent persona and memory storage.
- Changes Agent context construction so persona/memory prompt additions are available before the runner builds messages.
- Adds stateful tools; `memory_save` is `STATE_WRITE`, `memory_delete` deletes only by exact id, and `memory_recall` remains read-only.
- Adds focused tests for persona resolution, memory scope filtering, TTL expiry, exact-id deletion, keyword retrieval, prompt injection, injection trace metadata, tools, and Dashboard APIs.
