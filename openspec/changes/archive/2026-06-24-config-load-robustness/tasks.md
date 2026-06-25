## 1. OpenSpec

- [x] 1.1 Create config load robustness proposal, design, specs, and tasks.

## 2. Runtime

- [x] 2.1 Strip a leading BOM before JSON decoding.
- [x] 2.2 Treat empty existing config files as uninitialized and persist defaults.
- [x] 2.3 Preserve backup-and-default behavior for malformed non-empty JSON.

## 3. Tests

- [x] 3.1 Add BOM config load coverage.
- [x] 3.2 Add empty placeholder config coverage.
- [x] 3.3 Add malformed config backup regression coverage.

## 4. Verification

- [x] 4.1 Run targeted config tests.
- [x] 4.2 Run full Gradle test suite.
- [x] 4.3 Run OpenSpec strict validation/status checks.
