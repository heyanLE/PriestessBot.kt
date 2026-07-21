## ADDED Requirements

### Requirement: Persona error-message persistence
The system SHALL persist optional structured error messages with a persona, including `permissionDenied`, and expose them through persona create, update, list, get, and resolve operations.

#### Scenario: Permission-denied message round trip
- **GIVEN** an operator saves a persona with a `permissionDenied` message
- **WHEN** that persona is retrieved or resolved
- **THEN** its returned error-message configuration contains the saved `permissionDenied` message
