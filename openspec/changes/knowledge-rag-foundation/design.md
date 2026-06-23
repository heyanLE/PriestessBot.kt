# Design

## Storage

Two SQLite tables are added:

- `knowledge_bases`: id, name, description, createdAt, updatedAt
- `knowledge_chunks`: id, knowledgeBaseId, documentName, content, createdAt

The first implementation stores text chunks only. Chunking splits input by blank lines and then by fixed character size if a paragraph is too large.

## Retrieval

`KeywordKnowledgeRetriever` tokenizes query and chunk content into lowercase alphanumeric tokens. Score is based on token occurrence count with a small bonus for exact phrase containment. Results are sorted by score descending, then newer chunks.

This is intentionally deterministic and dependency-light. Vector embeddings can be added behind the same `KnowledgeCase.search` contract later.

## Dashboard API

Routes:

- `GET /api/knowledge/bases`
- `POST /api/knowledge/bases`
- `POST /api/knowledge/bases/{id}/documents`
- `POST /api/knowledge/search`

## Agent Tool

`KnowledgeSearchTool` is registered with built-in tools. It accepts `query`, optional `knowledgeBaseId`, and optional `limit`, then returns formatted search snippets.
