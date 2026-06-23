## MODIFIED Requirements

### Requirement: Dashboard overview SHALL show runtime diagnostics

The Dashboard overview SHALL show health diagnostics returned by the backend.

#### Scenario: Operator inspects diagnostics

- **GIVEN** the health response contains diagnostics
- **WHEN** the operator opens the Dashboard overview
- **THEN** the overview SHALL render the diagnostic key/value pairs
