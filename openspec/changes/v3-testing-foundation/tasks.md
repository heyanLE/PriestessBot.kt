## 1. Test Fixture Foundation

- [x] 1.1 Create shared fake provider fixtures that can script final responses, tool calls, repeated tool calls, and provider exceptions.
- [x] 1.2 Create shared fake platform fixtures that can emit inbound message events and capture outbound `sendMessage` calls.
- [x] 1.3 Create shared fake tool fixtures for success, schema validation error, execution exception, timeout, and permission-denied outcomes.
- [x] 1.4 Create shared in-memory helpers for conversation history, config/workspace snapshots, clocks, and metrics assertions.
- [x] 1.5 Document test fixture package ownership so new module tests reuse the shared fakes instead of duplicating local stubs.

## 2. Agent Loop Unit Tests

- [x] 2.1 Add `ReActRunnerTest` covering a provider final response with no tool call.
- [x] 2.2 Add `ReActRunnerTest` coverage for a tool-call loop that appends assistant/tool messages and continues to a final response.
- [x] 2.3 Add `ReActRunnerTest` coverage for tool executor failure being converted into a failed observation.
- [x] 2.4 Add `ReActRunnerTest` coverage for provider exceptions returning `AgentResponse.Error`.
- [x] 2.5 Add `ReActRunnerTest` coverage for `maxSteps` exhaustion returning `AgentResponse.Error`.
- [x] 2.6 Add `ReActRunnerTest` coverage for calling `step()` after `DONE` or `ERROR` without restarting or duplicating side effects.
- [x] 2.7 Add context compression tests for round truncation, token window trimming, and LLM compression fallback preserving required context.

## 3. ToolExecutor Unit Tests

- [x] 3.1 Add `ToolExecutorTest` coverage for unknown tool names returning an error result.
- [x] 3.2 Add `ToolExecutorTest` coverage for malformed JSON and schema-invalid arguments skipping tool invocation.
- [x] 3.3 Add `ToolExecutorTest` coverage for tool execution exceptions returning error `ToolResult`.
- [x] 3.4 Add `ToolExecutorTest` coverage for permission-denied tool policy behavior.
- [x] 3.5 Add `ToolExecutorTest` coverage for tool timeout behavior.
- [x] 3.6 Add `ToolExecutorTest` coverage for batch execution order and partial failure preservation.
- [x] 3.7 Add `ToolExecutorTest` coverage for success, failure, timeout, and unknown-tool metrics without sensitive arguments.

## 4. Pipeline Stage Unit Tests

- [x] 4.1 Add stage-level tests for waking checks across direct, group, mention, prefix, and non-triggering messages.
- [x] 4.2 Add stage-level tests for whitelist allow/block behavior.
- [x] 4.3 Add stage-level tests for rate-limit pass, stop, and optional notification behavior.
- [x] 4.4 Add `PreProcessStage` tests verifying AgentContext creation with system prompt, history, platform, session, and metadata.
- [x] 4.5 Add tests for each remaining pipeline stage's allow/block/stop behavior and context mutations.
- [x] 4.6 Add scheduler or stage tests proving onion flow post-processing resumes after later stages run.
- [x] 4.7 Add `RespondStage` tests for final response and error response delivery through the originating platform.

## 5. Integration Tests

- [x] 5.1 Add integration coverage for Pipeline -> ReActRunner -> ToolExecutor -> Respond using fake platform, fake provider, and fake tool fixtures.
- [x] 5.2 Add integration coverage for config reload publishing updates to ProviderController, PlatformController, and PipelineController for subsequent messages.
- [x] 5.3 Add integration coverage for plugin load/enable registering tools/providers/platforms and disable cleanup removing them.
- [x] 5.4 Add Dashboard API route contract tests for existing health, metrics, config, platform, provider, tool, conversation, plugin, log WebSocket, sub-agent, knowledge, and Agent chat routes.
- [x] 5.5 Add integration coverage for KnowledgeSearchTool -> KnowledgeCase -> KnowledgeController success and empty-result paths.
- [x] 5.6 Add workspace reload integration tests for successful snapshot replacement, validation failure rollback, and in-flight message snapshot isolation when workspace APIs are implemented.
- [x] 5.7 Add persona/memory integration tests for persona resolution, prompt injection metadata, memory scope filtering, delete, and expiration when persona/memory APIs are implemented.

## 6. System and Regression Tests

- [x] 6.1 Add `MessageFlowSystemTest` that starts a credential-free runtime wiring with fake platform, fake provider, fake tool, in-memory conversation store, and metrics registry.
- [x] 6.2 Verify the system test covers user message receipt, conversation user message persistence, provider tool call, tool execution, observation handoff, provider final response, platform send, and final history persistence.
- [x] 6.3 Verify the system test asserts pipeline, LLM, and tool metrics are incremented without prompts, message text, user IDs, session IDs, API keys, tool arguments, or exception messages.
- [x] 6.4 Add a regression test template or convention so future bug fixes in Agent, tool, pipeline, Dashboard API, config reload, workspace, persona, or memory modules include a failing-before-fix test.

## 7. Quality Gate Verification

- [x] 7.1 Ensure `./gradlew test` runs the required credential-free test gate locally.
- [x] 7.2 Confirm required tests do not depend on Telegram, NapCat, OpenAI, Anthropic, Gemini, live MCP servers, internet access, or local secrets.
- [x] 7.3 Run `openspec validate v3-testing-foundation --strict` and fix any proposal/spec/task validation issues.
- [x] 7.4 Record any intentionally deferred workspace/persona/memory tests in the implementing change that introduces those modules.
