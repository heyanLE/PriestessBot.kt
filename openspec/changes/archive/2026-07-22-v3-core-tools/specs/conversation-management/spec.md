## ADDED Requirements

### Requirement: Conversation history SHALL support scoped search
The conversation management layer SHALL support bounded, workspace-scoped message search for Agent tools and management surfaces.

#### Scenario: Search messages by keyword
- **WHEN** conversation history is searched with a keyword query
- **THEN** matching stored messages SHALL be returned with conversation id, message id, role, timestamp, and content snippet

#### Scenario: Search messages by time and conversation
- **WHEN** conversation history is searched with a time range or conversation id
- **THEN** only messages matching those filters SHALL be returned

#### Scenario: Search is workspace scoped
- **GIVEN** stored messages exist for multiple workspaces
- **WHEN** a workspace-scoped search is executed
- **THEN** messages from other workspaces SHALL NOT be returned

#### Scenario: Search limit is enforced
- **WHEN** a search request specifies a limit
- **THEN** no more than that limit or the configured maximum SHALL be returned
