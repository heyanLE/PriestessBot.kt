## 1. OpenSpec

- [x] 1.1 Create persona/memory proposal, design, tasks, and delta specs.
- [x] 1.2 Run strict OpenSpec validation for `v3-persona-memory-core`.

## 2. Domain And Persistence

- [x] 2.1 Add Persona, MemoryRecord, MemoryScope, MemoryType, MemoryFilter, MemorySearchResult, and injection trace models.
- [x] 2.2 Add database tables and migrations for personas and memory records.
- [x] 2.3 Add serialization for boundaries, tags, scope keys, confidence, timestamps, expiry, and soft deletion fields.

## 3. Controllers

- [x] 3.1 Implement PersonaController CRUD and `resolve(workspaceId, agentName)` behavior.
- [x] 3.2 Implement MemoryController save/list/search/delete/expire behavior.
- [x] 3.3 Enforce required scope keys and exclude deleted or expired memory by default.

## 4. Retrieval

- [x] 4.1 Define MemoryRetriever and scope context contracts.
- [x] 4.2 Implement deterministic keyword retrieval with token, phrase, tag, recency, and confidence scoring.
- [x] 4.3 Return match reasons and bounded ordered results.

## 5. Prompt Injection

- [x] 5.1 Implement PersonaMemoryInjector prompt rendering with bounded memory snippets.
- [x] 5.2 Attach injection trace metadata to AgentContext.
- [x] 5.3 Wire injector into PreProcessStage or AgentContext creation before ReActRunner message initialization.

## 6. Tools

- [x] 6.1 Add `memory_save` built-in tool with `STATE_WRITE` classification.
- [x] 6.2 Add read-only `memory_recall` built-in tool.
- [x] 6.3 Add `memory_delete` built-in tool that deletes only by exact memory id.
- [x] 6.4 Register memory tools with built-in tool wiring and config gating.

## 7. Dashboard API

- [x] 7.1 Add persona request/response DTOs and REST routes.
- [x] 7.2 Add memory request/response DTOs and REST routes.
- [x] 7.3 Include persona/memory injection trace in Agent chat test responses.

## 8. Dashboard Frontend

- [x] 8.1 Add typed API client functions and types for persona and memory operations.
- [x] 8.2 Add PersonaView with list, edit, enable, assignment, save, and delete flows.
- [x] 8.3 Add MemoryView with list, filters, create, search, expire, and exact-id delete flows.
- [x] 8.4 Show Agent chat injection trace in the Agent test view.

## 9. Verification

- [x] 9.1 Add tests for persona resolution and disabled/deleted behavior.
- [x] 9.2 Add tests for memory scope filtering, TTL expiry, and exact-id deletion.
- [x] 9.3 Add tests for keyword retrieval ranking, limits, and match reasons.
- [x] 9.4 Add tests for prompt injection content and trace metadata.
- [x] 9.5 Add tests for memory tool validation, permissions, and execution.
- [x] 9.6 Add Dashboard API and frontend client/view tests.
- [x] 9.7 Run targeted tests and the full Gradle test suite.
