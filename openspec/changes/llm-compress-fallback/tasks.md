## 1. OpenSpec

- [x] 1.1 Create LLM compression fallback proposal, design, specs, and tasks.

## 2. Runtime

- [x] 2.1 Give `LLMCompressStrategy` a safe fallback implementation.
- [x] 2.2 Wire the fallback with the existing `ContextManager` token counter.
- [x] 2.3 Preserve the `llm_compress` strategy name and config mapping.

## 3. Tests

- [x] 3.1 Add direct `LLMCompressStrategy` fallback coverage.
- [x] 3.2 Add `ContextManager` coverage for configured `llm_compress`.
- [x] 3.3 Add strategy name regression coverage.

## 4. Verification

- [x] 4.1 Run targeted context tests.
- [x] 4.2 Run full Gradle test suite.
- [x] 4.3 Run OpenSpec strict validation/status checks.
