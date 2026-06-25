# Design

## Storage

Backups are stored under `<config parent>/backups/<config filename>/` with IDs based on UTC timestamps:

`yyyyMMdd-HHmmss-SSS.json`

For a root-level `config.json`, the backup directory is `backups/config.json/`.

## Creation

`ConfigController.replace(config, persist = true)` creates a backup of the current config file before writing the new config, when the current file exists and is not blank. The initial config file creation and passive `reload()` do not create backups. Existing malformed-file recovery keeps its `.bak` behavior.

## Restore

`ConfigController.restoreBackup(id)` resolves the ID within the backup directory, validates that it is a known backup file, decodes it as `PriestessConfig`, writes it to the active config file, and publishes the restored effective config. Environment overrides are then applied like normal load/reload behavior.

## Dashboard API

Add:

- `GET /api/config/backups`
- `POST /api/config/backups/{id}/restore`

The list route returns metadata only:

- `id`
- `createdAt`
- `sizeBytes`
- `path`

The restore route returns the restored effective `PriestessConfig`.

## Safety

Backup IDs must not be treated as arbitrary paths. Restore only accepts IDs returned by the backup listing logic.
