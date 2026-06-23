# Config Backup And Restore

## Why

v2 already supports config editing, reload, hot publication, and NAS deployment, but operators still have no safe way to recover from a bad Dashboard config save. The ops roadmap calls for configuration backup and restore. The current malformed-config fallback writes a single `.bak` file only after parse failure; it is not a usable version history for normal config edits.

## What Changes

- Create timestamped config backups before persisted config replacements.
- Expose backup listing and restore operations through the Dashboard API.
- Restoring a backup validates the config, writes it to the active config path, and publishes the restored config slices.
- Keep backup metadata non-sensitive; API responses include IDs, timestamps, size, and source path, not config contents.

## Impact

- Operators can recover from accidental bad edits without SSH-ing into the NAS and hand-copying files.
- Existing config reload and environment overlay behavior remains intact.
- Backup files live next to the active config under a dedicated backup directory.
