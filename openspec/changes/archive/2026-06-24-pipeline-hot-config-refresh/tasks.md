## 1. OpenSpec

- [x] 1.1 Create pipeline hot config refresh proposal, design, specs, and tasks.

## 2. Runtime

- [x] 2.1 Refactor `PipelineController` to build runtime stages per message from current config.
- [x] 2.2 Preserve test-only static stage construction.
- [x] 2.3 Keep in-flight execution stable by passing the per-message stage list through execution.

## 3. Tests

- [x] 3.1 Add coverage proving sub-agent config updates affect later messages without rebuilding `PipelineController`.
- [x] 3.2 Keep existing pipeline behavior tests passing.

## 4. Verification

- [x] 4.1 Run targeted pipeline tests.
- [x] 4.2 Run full Gradle test suite.
- [x] 4.3 Run OpenSpec strict validation/status checks.
