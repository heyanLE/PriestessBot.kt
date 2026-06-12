## 1. Project scaffolding & core infrastructure

- [x] 1.1 Rename root package from `org.example` to `com.heyanle.priestess.bot`, create base package structure under `src/main/kotlin/com/heyanle/kt/astrbot/`
- [x] 1.2 Define `PriestessConfig`, `PlatformConfig`, `ProviderConfig`, `AgentConfig` as `@Serializable` data classes with defaults
- [x] 1.3 Implement JSON config loading and default generation
- [x] 1.4 Create `CoreLifecycle` with ordered startup/shutdown: DB → EventBus → ToolRegistry → ProviderManager → PipelineScheduler → PlatformManager
- [x] 1.5 Create `CoreModule` Koin DI module registering all components (`single` for singletons, `factory` for AgentRunner)
- [x] 1.6 Implement `EventBus` using `Channel<Event>(Channel.BUFFERED)` with sealed `Event` class (MessageEvent, SystemEvent, ControlEvent)
- [x] 1.7 Create `PriestessBot.kt` entry point wiring Config → DI → CoreLifecycle

## 2. Database & persistence

- [x] 2.1 Add Exposed + SQLite JDBC dependencies to build.gradle.kts
- [x] 2.2 Define `Database` interface with init/connect/disconnect
- [x] 2.3 Implement `PriestessDb` with Exposed table definitions (conversations, messages)
- [x] 2.4 Implement `ConversationManager` with CRUD, findByPlatformSession, expiration cleanup
- [x] 2.5 Implement `MessageHistory` with store, getByConversation, chronological ordering

## 3. Platform abstraction layer

- [x] 3.1 Define `PlatformMetadata` data class and `@RegisterPlatform` annotation
- [x] 3.2 Define `Platform` abstract class with `run()`, `sendMessage()`, `terminate()`, `commitEvent()`
- [x] 3.3 Define `MessageEvent`, `MessageSession`, `MessageChain`, `MessageComponent` (sealed: Text, Image, At, File)
- [x] 3.4 Implement `PlatformManager` with start/stop/status for multiple platforms
- [x] 3.5 Implement `PlatformRegistry` scanning `@RegisterPlatform` annotated classes at startup
- [x] 3.6 Implement `TelegramPlatform` with Bot API long polling, message parsing, and reply sending
- [x] 3.7 Implement `NapCatPlatform` with HTTP API client (IP + port config only), message parsing, and reply sending

## 4. Provider layer

- [x] 4.1 Define `ChatProvider` interface with `textChat()`, `textChatStream()` (reserved), `getModels()`, `test()`
- [x] 4.2 Define `LLMRequest`, `LLMResponse`, `ConversationMessage` as `@Serializable` data classes
- [x] 4.3 Implement `ProviderManager` with register, getByName, testAll
- [x] 4.4 Implement `OpenAIProvider` with Chat Completions API (synchronous only), supports custom base URL for compatible services
- [x] 4.5 Implement `OllamaProvider` with Chat API and model list retrieval (synchronous only)

## 5. Tool / MCP layer

- [x] 5.1 Define `ToolSchema` data class and `FunctionTool` abstract class with `execute(context, args)`
- [x] 5.2 Implement `ToolSet` with add/remove and `toOpenAIFormat()` conversion
- [x] 5.3 Implement `ToolRegistry` collecting annotated tools and MCP tools
- [x] 5.4 Implement `ToolExecutor` resolving tool calls by name, validating args, invoking `execute()`
- [x] 5.5 Define `McpTransport` interface and `McpConfig` data class
- [x] 5.6 Implement `StdioTransport` (spawn child process, stdin/stdout communication, auto-restart)
- [x] 5.7 Implement `SseTransport` (HTTP GET SSE connection, exponential backoff reconnect)
- [x] 5.8 Implement `StreamableHttpTransport` (HTTP POST request-response, retry on timeout)
- [x] 5.9 Implement `McpClient` with connect/disconnect, listTools, callTool (selects transport by config)
- [x] 5.10 Implement `McpTool` wrapping MCP tool definitions as FunctionTool
- [x] 5.11 Implement `WebSearchTool` built-in tool calling a search API
- [x] 5.12 Implement `EarlyReplyTool` sending proactive messages via Platform reference in AgentContext
- [x] 5.13 Implement `SendMessageTool` for proactive messaging (platform support check)
- [x] 5.14 Implement `SystemInfoTool` returning runtime status, agent info, tool list

## 6. Agent loop

- [ ] 6.1 Define `Agent` data class (name, instructions, toolSet)
- [ ] 6.2 Define `AgentRunner` interface with `reset()`, `step()`, `stepUntilDone()`, `isDone()`, `finalResponse()`
- [ ] 6.3 Define `AgentState` state machine (IDLE, RUNNING, DONE, ERROR)
- [ ] 6.4 Define `AgentHooks` interface (onAgentBegin, onToolStart, onToolEnd, onAgentDone)
- [ ] 6.5 Define `AgentContext` carrying messages, agent config, tool timeout, Platform ref, MessageSession ref
- [ ] 6.6 Define `AgentResponse` sealed class (Thinking, ToolResult, Final, Error)
- [ ] 6.7 Define `ContextCompressStrategy` interface
- [ ] 6.8 Implement `RoundTruncationStrategy` (keep last N rounds)
- [ ] 6.9 Implement `TokenWindowStrategy` (trim oldest messages exceeding token limit)
- [ ] 6.10 Implement `TokenCounter` for message token estimation
- [ ] 6.11 Implement `ContextManager` checking if compression is needed, delegating to configured strategy
- [ ] 6.12 Implement `ReActRunner` with Thought → Action → Observation loop: context check, LLM call, tool execution, final answer or max steps error
- [ ] 6.13 Implement `LLMCompressStrategy` stub interface (reserved for v2, throws NotImplemented in v1)

## 7. Pipeline

- [ ] 7.1 Define `Stage` interface with `name`, `order`, `initialize(ctx)`, `process(event): Flow<Unit>?`
- [ ] 7.2 Define `StageOrder` enum for the 9 stages
- [ ] 7.3 Define `PipelineContext` carrying event, response, and cross-stage shared data
- [ ] 7.4 Implement `PipelineScheduler` with recursive Flow-based onion model executor
- [ ] 7.5 Implement `WakingCheckStage` (mention detection, prefix matching, private message bypass)
- [ ] 7.6 Implement `WhitelistCheckStage` (user/group whitelist filtering)
- [ ] 7.7 Implement `SessionStatusStage` (session enabled/disabled check)
- [ ] 7.8 Implement `RateLimitStage` (per-user/per-session frequency limiting)
- [ ] 7.9 Implement `ContentSafetyStage` (placeholder content filtering hook)
- [ ] 7.10 Implement `PreProcessStage` (inject System Prompt, load history from ConversationManager, attach Skill instructions) — onion model: pre-injection → yield → post-cleanup
- [ ] 7.11 Implement `ProcessStage` (create ReActRunner factory instance, call stepUntilDone) — onion model: pre-init → yield → post-capture
- [ ] 7.12 Implement `ResultDecorateStage` (format response, Markdown rendering placeholder)
- [ ] 7.13 Implement `RespondStage` (send final response via Platform.sendMessage, persist conversation)

## 8. Skill management

- [ ] 8.1 Define `Skill` interface with name, description, priority, canHandle(), execute()
- [ ] 8.2 Implement `SkillManager` with register, sortByPriority, dispatch
- [ ] 8.3 Implement `DefaultSkill` fallback handler returning a generic response

## 9. Integration & wiring

- [ ] 9.1 Wire PlatformManager → EventBus → PipelineScheduler → all Stages in CoreLifecycle startup order
- [ ] 9.2 Ensure AgentRunner factory scope creates new instance per ProcessStage invocation
- [ ] 9.3 Test full message flow: NapCat/Telegram receive → Pipeline (all stages) → ReAct → LLM → Tool → Respond
- [ ] 9.4 Add comprehensive logging (SLF4J) across all modules with consistent log levels
- [ ] 9.5 Verify config file persistence with manual edit → reload → apply cycle
