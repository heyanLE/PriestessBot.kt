## 1. Permission and configuration foundation

- [x] 1.1 Add defaulted command-prefix and flat administrator/super-administrator ID configuration models, parsing, and runtime wiring.
- [x] 1.2 Define ordered `PermissionGroup` and a resolver that maps normalized sender IDs to `OPERATOR`, `ADMIN`, or `SUPER_ADMIN`.
- [x] 1.3 Extend `PipelineContext`, `AgentContext`, and `AgentToolContext` with the resolved sender permission metadata.
- [x] 1.4 Add unit tests for permission precedence, default operator behavior, and configuration compatibility defaults.

## 2. Persona denial wording and conversation reset

- [x] 2.1 Add `PersonaErrorMessages.permissionDenied` to persona domain models, persistence schema, mapping, and existing persona API/DTO flows with backward-compatible defaults.
- [x] 2.2 Implement a shared persona error-message resolver with a stable fallback when no eligible persona configures a denial message.
- [x] 2.3 Add a conversation-history clear operation that deletes messages for the current platform/session while preserving the conversation record.
- [x] 2.4 Add persistence and API tests for persona error-message round trips and session-history clearing.

## 3. Command pipeline

- [x] 3.1 Create the registry-backed Command abstraction, command context/result types, and built-in command registration seam.
- [x] 3.2 Implement `/new` with `ADMIN` requirement, current-session history clearing, and direct success/denial responses.
- [x] 3.3 Add permission-resolution and command-dispatch stages after workspace preparation and before `PreProcessStage`.
- [x] 3.4 Add pipeline handled-response semantics so commands bypass Agent/LLM stages but still reach decoration and response delivery without being persisted to history.
- [x] 3.5 Add stage, command, and end-to-end tests for successful `/new`, denied `/new`, unknown commands, and ordinary non-command messages.

## 4. Permission-aware Tool and Skill assembly

- [x] 4.1 Add defaulted required-permission declarations to `ToolSchema`, `Skill`, workspace skill config, and workspace skill descriptors.
- [x] 4.2 Filter visible Tool schemas and Skill references by sender permission: hide `SUPER_ADMIN` capabilities from lower roles and annotate `ADMIN` capabilities for operators.
- [x] 4.3 Retain each workspace Skill reference's required permission in `PipelineSkillState` and enforce it in `use_skill`; deny unauthorized loads without mutating loaded-skill state.
- [x] 4.4 Enforce Tool required permission in `ToolExecutor`; return a structured `PERMISSION_DENIED` result containing current/required roles and persona denial wording without invoking the Tool.
- [x] 4.5 Ensure OpenAI-compatible Agent tool-message handling preserves the original `tool_call_id` and sends the permission-denied result as `role=tool` content.
- [x] 4.6 Add Tool/Skill visibility, `use_skill` authorization, execution-enforcement, and OpenAI-compatible tool-result serialization regression tests.

## 5. Platform identity and verification

- [x] 5.1 Update Telegram event parsing to put `message.from.id` in `session.metadata["senderId"]` and preserve correct private/group session identity.
- [x] 5.2 Add Telegram sender-ID tests and retain NapCat sender-ID coverage.
- [ ] 5.3 Run targeted module tests, then run `./gradlew test` and `openspec validate add-command-permission-controls --strict`.
