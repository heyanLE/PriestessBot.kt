# Dashboard Config Backup Restore

## Why

The backend now supports config backup listing and restore, but Dashboard operators still need to call the API manually. Config recovery is an operational workflow and should be available from the Runtime Config screen where bad edits are most likely to happen.

## What Changes

- Add Dashboard API client types and methods for config backup list/restore.
- Keep backup metadata in the shared Dashboard store.
- Extend the Runtime Config view with backup list, refresh, and restore actions.
- Refresh the active config and draft after a restore.

## Impact

- Operators can recover config edits from the Dashboard without SSH or raw HTTP calls.
- Backup metadata remains non-sensitive in the UI.
- Existing JSON editing remains available.
