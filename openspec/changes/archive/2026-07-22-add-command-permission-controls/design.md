## Context

The current pipeline sends every accepted message through workspace preparation and `PreProcessStage` before an Agent is created. Its only identity-dependent controls are the whitelist and rate limiter. `ToolSchema` and workspace skill descriptors have no permission requirement, and `ToolExecutor` has no message-role check. Personas are resolved only during preprocessing, while command handling must run earlier. Conversation storage can delete an entire conversation but cannot clear only its messages. Telegram currently stores chat metadata without the sender's `from.id`; NapCat already stores `senderId` and `userId`.

The change is intentionally limited to OpenAI and OpenAI-compatible Chat Completions providers. The existing OpenAI adapter already emits assistant tool calls and accepts `role=tool`, `tool_call_id`, and string content. Other provider adapters are not changed.

## Goals / Non-Goals

**Goals:**

- Add predictable, prefix-based local commands that never invoke the LLM.
- Resolve a sender into one ordered permission group before command dispatch and Agent preparation.
- Make permission filtering consistent across commands, workspace skills, visible tool schemas, and Tool execution.
- Let a persona supply the user-facing permission-denied wording for both direct command responses and LLM-visible Tool results.
- Add a privileged `/new` command that clears only the current session's persisted history.

**Non-Goals:**

- Do not change Ollama, Anthropic, Gemini, or their tool-message mappings.
- Do not build a Dashboard command or permission-management UI beyond exposing the added persona field through existing persona APIs.
- Do not add group-, workspace-, time-, or ACL-based roles; version one maps configured IDs to three fixed levels.
- Do not implement plugin commands, though the command registry is designed to admit them later.

## Decisions

### Ordered, global sender roles

Define `PermissionGroup` as `OPERATOR < ADMIN < SUPER_ADMIN`. Global `PermissionConfig` has flat `superAdminIds` and `adminIds` lists; matching a super-admin ID wins if an ID appears in both lists, and every other authenticated sender becomes `OPERATOR`. IDs are normalized to nonblank strings.

This matches the requested operational model and avoids embedding platform-specific IDs in individual capabilities. A general RBAC policy graph was considered, but it would add configuration and migration complexity without a first-version use case.

### Resolve permission after workspace preparation and before commands

Extend stage ordering as follows:

```text
Waking → Whitelist → RateLimit → PrepareWorkspace → ResolvePermission
       → Command → PreProcess → Process → ResultDecorate → Respond
```

`ResolvePermissionStage` writes the resolved role and sender ID to `PipelineContext`. `CommandStage` parses only messages whose trimmed text begins with the configured, nonempty command prefix. It invokes a registered handler by command name and stops Agent preprocessing/processing after placing its direct response into the context. The pipeline must still execute the response path, so a command-specific handled flag is used instead of `event.stopPropagation()`.

Reusing `wakingPrefix` was rejected because it governs whether a group message wakes the bot, whereas the command prefix governs local instruction parsing and has different default/empty semantics.

### Commands as a registry-backed abstraction

Create Pipeline-local `Command` and permission contracts containing name, description, required permission group, argument handling, and a suspend execution method receiving a command context. `CommandCase`/registry resolves built-in handlers now and keeps registration public for a future plugin integration. The first-version types stay in the `pipeline` package rather than separate modules because their only current consumer is the message pipeline.

`/new` has `ADMIN` requirement. It resolves the current conversation by platform and session, deletes only that conversation's messages, and returns a direct success response. Unknown commands and malformed command input are direct, non-LLM results. Commands are not persisted as conversation messages, including their success and denial responses.

### Persona error-message resolver shared by early and Agent paths

Add `PersonaErrorMessages(permissionDenied)` to persisted `Persona` and `PersonaUpsertRequest`, backed by a JSON column. Add a small `PersonaErrorMessageResolver` that resolves the eligible persona for the workspace and primary agent name, then returns its configured `permissionDenied` string or a stable default.

Both `CommandStage` and `ToolExecutor` use that resolver. This avoids moving all persona and memory injection ahead of the command stage and prevents duplicate persona selection rules. The existing `PersonaMemoryInjector` remains responsible only for system-prompt injection.

### Two-tier capability visibility plus execution enforcement

Commands, `ToolSchema`, `Skill`, and workspace skill descriptors declare `requiredPermissionGroup`, defaulting to `OPERATOR`.

- If a capability requires `SUPER_ADMIN` and the sender is below it, omit it from the available Tool/Skill view completely.
- If a capability requires `ADMIN` and the sender is `OPERATOR`, retain it but append a standardized permission-insufficient notice to the Tool description or Skill prompt/reference.
- A Tool invocation is always checked again in `ToolExecutor`. A denied result has `success=false`, `errorCode=PERMISSION_DENIED`, the structured current/required role data, and the persona-rendered denial message.
- `use_skill` checks the requested Skill reference's required group before mutating `PipelineSkillState`. A lower-role caller receives the same structured permission-denied Tool result, with the persona-rendered message, and the Skill document remains unloaded.

The asymmetry is deliberate: super-admin functions are undiscoverable by lower roles, while administrator functions are discoverable for guided operations but remain safe when a model calls them anyway. UI/prompt visibility is not relied on for authorization. `PipelineSkillState` must retain each reference's required group so that the loading operation, rather than only the initial skill listing, remains an authorization boundary.

### OpenAI-compatible tool-result scope

The existing `ConversationMessage.tool` has the identifiers needed for Chat Completions. Permission denials are returned as a textual `ToolResult.error` content and therefore become the `content` of a `role=tool` message with the original `tool_call_id`. No provider interface changes are required. Tests target this exact serialized OpenAI-compatible message sequence.

## Risks / Trade-offs

- [A direct command response might be skipped if pipeline stop semantics are reused] → Use a handled-response state that bypasses Agent stages but leaves decoration and response stages active; test the exact stage sequence.
- [Telegram group permissions could be assigned from chat ID rather than sender ID] → Parse `message.from.id`, set `senderId`, and add private/group adapter tests.
- [Persona lookup before preprocessing could select a different sub-agent-specific persona] → Commands use the workspace primary agent; commands do not invoke sub-agent routing. Document this as the first-version command scope.
- [A prompt-only warning could be bypassed by a model tool call] → Make `ToolExecutor` the mandatory enforcement point and return an explicit denial result.
- [Existing persisted personas lack the new JSON field] → Use `createMissingTablesAndColumns`, default empty messages on read, and provide a default denial phrase.

## Migration Plan

1. Add defaulted configuration and model fields so current config files and persona rows continue to load.
2. Deploy the database column addition through the existing schema bootstrap.
3. Register the command and permission services during runtime composition, then insert the two stages into the pipeline.
4. Verify `/new` and a denied tool call against an OpenAI-compatible fake provider before enabling admin IDs in production configuration.
5. Roll back by removing configured admin IDs or reverting the application version; pre-existing conversations and personas remain readable because all added fields are optional/defaulted.

## Open Questions

- None for the initial scope. The default localized permission-denied phrase is used when no eligible persona supplies one.
