## ADDED Requirements

### Requirement: Conversation detail view
The Dashboard frontend SHALL provide a detail view for a selected conversation.

#### Scenario: Operator opens a conversation
- **GIVEN** the conversation list is visible
- **WHEN** the operator selects a conversation
- **THEN** the Dashboard navigates to `/conversations/{id}`
- **AND** loads the message history for that conversation

### Requirement: Message transcript rendering
The conversation detail view SHALL render message roles, content, timestamps, and tool metadata.

#### Scenario: Tool messages are visible
- **GIVEN** a conversation contains assistant tool calls and tool responses
- **WHEN** the operator opens the conversation detail view
- **THEN** tool call payloads and tool call ids are displayed alongside the transcript
