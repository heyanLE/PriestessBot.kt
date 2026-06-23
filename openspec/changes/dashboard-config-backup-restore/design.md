# Design

## Frontend Data

Add a `ConfigBackup` frontend type matching backend metadata:

- `id`
- `createdAt`
- `sizeBytes`
- `path`

The Pinia dashboard store owns `configBackups` and exposes:

- `loadConfigBackups()`
- `restoreConfigBackup(id)`

`refreshAll()` includes backups so Overview-to-Config navigation starts with current metadata.

## Runtime Config View

The view keeps the JSON editor as the primary workspace and adds a recovery panel below it. The panel shows backup ID, creation time, size, and path, with a per-row Restore button.

Restore behavior:

1. Disable restore controls while a restore is in progress.
2. Call `POST /api/config/backups/{id}/restore`.
3. Update store config and refresh runtime lists.
4. Reset the JSON draft to the restored config.
5. Show success or error notice.

## Safety

The UI never renders config backup file contents. Restore sends only the selected backup ID returned by the backend list endpoint.
