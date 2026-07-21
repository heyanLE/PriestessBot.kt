## ADDED Requirements

### Requirement: Tool permission declaration and visibility
The system SHALL allow each Tool schema to declare a required permission group, defaulting to `OPERATOR`, and SHALL derive an OpenAI tool view from the current sender's group.

#### Scenario: Super-administrator Tool is hidden
- **GIVEN** a Tool requires `SUPER_ADMIN`
- **AND** the sender is not a `SUPER_ADMIN`
- **WHEN** the OpenAI tool view is built
- **THEN** the Tool is absent from that view

#### Scenario: Administrator Tool is described as unavailable to an operator
- **GIVEN** a Tool requires `ADMIN`
- **AND** the sender is an `OPERATOR`
- **WHEN** the OpenAI tool view is built
- **THEN** the Tool remains in the view
- **AND** its description states that the current sender lacks the required permission

### Requirement: Tool permission execution enforcement
The system SHALL enforce a Tool's required permission group in `ToolExecutor` even when that Tool is visible to the model.

#### Scenario: Unauthorized Tool call returns an OpenAI-compatible denial
- **GIVEN** an `OPERATOR` invokes a Tool requiring `ADMIN`
- **WHEN** `ToolExecutor` executes the Tool call
- **THEN** the Tool implementation is not invoked
- **AND** the result has `success = false` and `errorCode = PERMISSION_DENIED`
- **AND** the result identifies the current and required permission groups and contains the configured persona denial wording
- **AND** the Agent loop appends it as the `content` of a `role=tool` message using the original `tool_call_id`
