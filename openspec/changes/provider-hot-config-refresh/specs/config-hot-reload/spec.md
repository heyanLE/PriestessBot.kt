## MODIFIED Requirements

### Requirement: Runtime config publication
The system SHALL publish updated config slices to runtime components after explicit updates or reloads.

#### Scenario: Provider controller observes provider config publication
- **GIVEN** Dashboard or config reload publishes updated provider config
- **WHEN** a later Agent execution requests a provider
- **THEN** the provider controller resolves providers from the latest published provider config without requiring process restart
