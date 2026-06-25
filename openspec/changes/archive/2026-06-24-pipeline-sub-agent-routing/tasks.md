## 1. OpenSpec

- [x] 1.1 Create pipeline sub-agent routing proposal, design, specs, and tasks.

## 2. Runtime

- [x] 2.1 Inject sub-agent config and selector into pipeline stage construction.
- [x] 2.2 Select primary or sub-agent config in `PreProcessStage`.
- [x] 2.3 Store selection metadata in `PipelineContext.shared`.
- [x] 2.4 Preserve existing `ProcessStage` execution path.

## 3. Tests

- [x] 3.1 Add pipeline integration test proving matched route uses selected sub-agent.
- [x] 3.2 Add fallback coverage for disabled orchestration or missing match.

## 4. Verification

- [x] 4.1 Run targeted pipeline/sub-agent tests.
- [x] 4.2 Run full Gradle test suite.
- [x] 4.3 Run OpenSpec strict validation/status checks.
