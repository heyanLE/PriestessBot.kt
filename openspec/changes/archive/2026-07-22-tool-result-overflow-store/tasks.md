## 1. Overflow storage foundation

- [x] 1.1 Add runtime configuration for inline token budget, preview token budget, result TTL, maximum single-result bytes, and total overflow-store bytes, with validated defaults.
- [x] 1.2 Implement a disk-backed runtime overflow store with opaque IDs, conversation ownership metadata, size accounting, TTL eviction, capacity eviction, and safe close/delete behavior.
- [x] 1.3 Register the store in dependency injection and close it from the runtime shutdown lifecycle.
- [ ] 1.4 Add unit tests for store/write/read metadata, TTL eviction, capacity handling, and shutdown cleanup.

## 2. Agent result materialization

- [x] 2.1 Add explicit conversation ownership to `AgentToolContext` and propagate it from `AgentContext` through the ReAct tool-execution path.
- [x] 2.2 Add a shared result materializer in `ReActRunner` that preserves in-budget successful results and replaces oversized successful results with a bounded preview and opaque reference.
- [x] 2.3 Ensure materialization preserves source-truncation metadata where the source result exposes it, and returns a bounded unavailable observation when storage cannot accept output.
- [x] 2.4 Keep failed, denied, timeout, and tool-execution-error results out of overflow storage.
- [ ] 2.5 Add runner tests covering builtin, scoped/plugin-style, and MCP-style oversized result paths plus provider-bound token limits.

## 3. Scoped retrieval tool

- [x] 3.1 Implement `read_tool_result` as a default-enabled `SAFE_READ` built-in with result ID, zero-based Unicode-safe offset, and bounded limit parameters.
- [x] 3.2 Return structured content windows with total size metadata, next offset, and truncation state; return a non-sensitive not-found result for missing, expired, and foreign IDs.
- [x] 3.3 Register the reader in built-in tool wiring and ensure existing workspace policy can explicitly deny it.
- [ ] 3.4 Add tests for pagination, Unicode boundaries, maximum window enforcement, owner access, cross-conversation denial, expiry, and policy denial.

## 4. URL tool integration and observability

- [x] 4.1 Update `fetch_url` and `web_extract` result models or materialization metadata so source byte/character truncation remains distinguishable from runtime overflow materialization.
- [ ] 4.2 Add focused tests for large single-page and multi-page extraction results, including source-truncated overflow results.
- [ ] 4.3 Add non-sensitive logs or metrics for overflow creation, retrieval, expiry, and unavailable-storage outcomes.

## 5. Verification

- [ ] 5.1 Run the focused overflow-store, reader-tool, ReAct runner, URL-tool, and context-compression test suites.
- [ ] 5.2 Run the full backend test suite and build the Dashboard-packaged JAR.
- [ ] 5.3 Perform a black-box Telegram/Dashboard smoke test that fetches an oversized page, retrieves a later window by result ID, and confirms no oversized raw result is sent to the provider.
