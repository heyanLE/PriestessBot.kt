# Design

## Server Config

Add `apiToken: String = ""` to `ServerConfig`. A blank value disables authentication.

Environment override:

- `PRIESTESS_SERVER_API_TOKEN`

The override is in-memory like other deployment overrides and is not persisted by passive load/reload.

## Route Protection

Install a small Ktor application interceptor before Dashboard routes:

- If `apiToken` is blank, allow all requests.
- If request path starts with `/api/` or `/ws/`, require `Authorization: Bearer <apiToken>`.
- Otherwise allow the request.

Return `401 Unauthorized` with a small JSON error when the token is missing or wrong.

Protected:

- Dashboard REST API under `/api/*`
- Log WebSocket under `/ws/logs`

Public:

- `/health`
- `/metrics`
- `/`
- Dashboard static assets and SPA fallback

## Frontend Token Handling

The frontend API helper reads a token from:

1. `?token=...` URL query, then stores it in `localStorage`.
2. `localStorage["priestess.dashboardToken"]`.

When present, REST requests include `Authorization: Bearer <token>`. WebSocket logs append `?token=...` because browser WebSocket APIs cannot set arbitrary headers.

The server accepts the token from Authorization header or `token` query parameter for `/ws/*` only.
