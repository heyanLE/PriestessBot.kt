# command-permission-controls Specification

## Purpose
TBD - created while syncing archived changes. Update Purpose after archive.

## Requirements

### Requirement: Sender permission groups
The system SHALL resolve every accepted message sender into exactly one ordered permission group: `OPERATOR`, `ADMIN`, or `SUPER_ADMIN`, where `SUPER_ADMIN` exceeds `ADMIN` and `ADMIN` exceeds `OPERATOR`.

#### Scenario: Configured super administrator wins
- **GIVEN** a sender ID occurs in both configured administrator and super-administrator ID lists
- **WHEN** the permission group is resolved
- **THEN** the system assigns `SUPER_ADMIN`

#### Scenario: Unconfigured sender is an operator
- **GIVEN** a message sender ID occurs in neither configured administrator list
- **WHEN** the permission group is resolved
- **THEN** the system assigns `OPERATOR`

### Requirement: Prefix command dispatch
The system SHALL parse trimmed messages that begin with the configured nonempty command prefix and dispatch the command name and arguments to a registered command handler before Agent preprocessing.

#### Scenario: Recognized command bypasses the LLM
- **GIVEN** a registered command named `new` and a message `/new`
- **WHEN** the command stage processes the message
- **THEN** the command handler is invoked
- **AND** no Agent context or LLM request is created for that message
- **AND** the command response is sent through the normal response path

#### Scenario: Non-command message proceeds to Agent preprocessing
- **GIVEN** the command prefix is `/`
- **WHEN** an accepted message does not begin with `/`
- **THEN** the command stage does not handle it
- **AND** the pipeline proceeds to Agent preprocessing

### Requirement: Command authorization
The system SHALL require a command's declared permission group before executing it and SHALL return a direct authorization response when the sender lacks that group.

#### Scenario: Insufficient command permission
- **GIVEN** a command requires `ADMIN`
- **AND** the sender is an `OPERATOR`
- **WHEN** the command is dispatched
- **THEN** the handler does not execute
- **AND** the response uses the resolved persona's permission-denied wording or the system default
- **AND** no LLM request is made

### Requirement: New conversation command
The system SHALL provide a built-in `/new` command requiring `ADMIN` that clears persisted message history for the current platform and session without deleting the conversation identity.

#### Scenario: Administrator starts a new conversation
- **GIVEN** the current platform and session have persisted messages
- **AND** the sender is an `ADMIN` or `SUPER_ADMIN`
- **WHEN** the sender sends `/new`
- **THEN** all persisted messages for that conversation are removed
- **AND** the conversation record remains associated with the same platform and session
- **AND** the success response is not persisted as history

#### Scenario: Operator cannot start a new conversation
- **GIVEN** the sender is an `OPERATOR`
- **WHEN** the sender sends `/new`
- **THEN** the persisted message history remains unchanged
- **AND** the response is the configured permission-denied wording

### Requirement: Persona-configured permission-denied wording
The system SHALL persist an optional persona `permissionDenied` error message and SHALL use it for local command and Tool permission denials associated with that persona's workspace and primary agent.

#### Scenario: Persona supplies denied wording
- **GIVEN** the resolved persona configures a nonblank `permissionDenied` message
- **WHEN** a command or Tool permission check denies the sender
- **THEN** the user-visible denial uses that configured message

#### Scenario: No persona wording is configured
- **GIVEN** no eligible persona configures a nonblank `permissionDenied` message
- **WHEN** a command or Tool permission check denies the sender
- **THEN** the system uses its stable default permission-denied message
