# Design

## Domain Model

`Persona` defines how an Agent speaks and what boundaries it follows:

- `id`, `workspaceId`
- `name`, `description`, `tone`
- `boundaries`
- `systemPromptTemplate`
- `enabled`
- `agentNames`
- `createdAt`, `updatedAt`

`MemoryRecord` stores long-term memory:

- `id`, `workspaceId`
- `scope`: `GLOBAL`, `PLATFORM`, `SESSION`, `USER`, or `AGENT`
- scope keys: `platformId`, `sessionId`, `userId`, `agentName`
- `type`: `FACT`, `PREFERENCE`, `EVENT`, or `SUMMARY`
- `content`, `tags`, `confidence`
- `createdAt`, `updatedAt`, `expiresAt`
- `deletedAt`

Scope keys are optional except where required by the selected scope. For example, `SESSION` memory requires a session id, and `AGENT` memory requires an agent name. Deleted or expired records are excluded from retrieval by default.

## Controllers

`PersonaController` owns CRUD and resolution:

- `list(workspaceId): List<Persona>`
- `get(id): Persona?`
- `upsert(persona): Persona`
- `delete(id)`
- `resolve(workspaceId, agentName): Persona?`

Resolution selects an enabled persona in the workspace. Agent-specific personas win over workspace-wide personas. If multiple personas match the same specificity, the newest updated record wins.

`MemoryController` owns persistence and lifecycle:

- `save(record): MemoryRecord`
- `list(filter): List<MemoryRecord>`
- `search(query, scope, limit): List<MemorySearchResult>`
- `delete(id)`
- `expire(now): Int`

`delete(id)` is an exact-id operation. It soft-deletes the record so injection trace and audit-friendly records can remain understandable without making the memory retrievable.

## Retrieval

`MemoryRetriever` accepts query text and a scope context containing workspace id, platform id, session id, user id, and agent name. The initial implementation is `KeywordMemoryRetriever`:

- tokenize query, tags, and memory content into lowercase alphanumeric tokens;
- require workspace match;
- include only records whose scope applies to the current context;
- exclude expired or deleted records;
- score token matches, exact phrase containment, tag matches, recency, and confidence;
- sort by score descending, then updated time descending;
- return bounded `MemorySearchResult` entries with score and match reason.

The retriever interface intentionally hides the storage details so a vector retriever can replace or augment keyword retrieval later.

## Prompt Injection

`PersonaMemoryInjector` runs before the Agent runner builds system messages. It receives workspace, agent, platform, session, user, and current user message context.

The injector:

1. resolves a persona for the workspace and agent;
2. searches memory with the current user message;
3. renders a bounded prompt section containing persona instructions and memory snippets;
4. stores trace metadata in `AgentContext.metadata`.

Trace metadata includes persona id/name, injected memory ids, memory scores, match reasons, and whether any records were skipped because of TTL, deletion, scope mismatch, or result limit.

## Tools

Three built-in tools are added:

- `memory_save`: creates or updates a memory record; classified as `STATE_WRITE` and enabled only when stateful tools are allowed.
- `memory_recall`: searches memory for the current context; read-only and safe to enable by default.
- `memory_delete`: deletes one exact memory id; it never accepts free-form fuzzy deletion criteria.

Tool execution uses the same scope rules as prompt injection. If a tool attempts to save a scoped memory without the required scope key, it returns a validation error.

## Dashboard API

Routes:

- `GET /api/personas`
- `POST /api/personas`
- `PUT /api/personas/{id}`
- `DELETE /api/personas/{id}`
- `GET /api/memory`
- `POST /api/memory`
- `POST /api/memory/search`
- `DELETE /api/memory/{id}`
- `POST /api/memory/expire`

Agent chat test responses include an `injectionTrace` object so operators can inspect the persona and memory used for that run.

## Dashboard Frontend

The Dashboard adds:

- `PersonaView` for listing, editing, enabling, assigning, and deleting personas.
- `MemoryView` for listing, filtering, adding, searching, expiring, and exact-id deleting memory records.
- Agent chat trace rendering for injected persona and memory ids, scores, and reasons.

The views are operational tools, so they should prioritize compact tables, filters, editors, and explicit destructive actions over marketing-style presentation.

## Verification

Tests cover:

- persona CRUD and agent-specific resolution;
- memory save/list/search across workspace, platform, session, user, and agent scopes;
- TTL expiry exclusion and explicit expiry cleanup;
- exact-id delete behavior;
- keyword retriever ranking and limits;
- injector prompt content and trace metadata;
- memory tool validation and permissions;
- Dashboard API success and error paths;
- Dashboard frontend API client and trace display behavior.
