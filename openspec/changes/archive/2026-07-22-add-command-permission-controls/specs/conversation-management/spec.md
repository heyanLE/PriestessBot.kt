## ADDED Requirements

### Requirement: Clear current conversation history
The system SHALL clear persisted messages for a conversation identified by platform and session without deleting that conversation record.

#### Scenario: History is empty after clear
- **GIVEN** a known platform and session with persisted user, assistant, and Tool messages
- **WHEN** the current conversation history is cleared
- **THEN** subsequent history retrieval for that conversation returns no messages
- **AND** retrieving or creating that platform and session returns the same conversation identity
