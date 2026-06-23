## MODIFIED Requirements

### Requirement: Configuration files SHALL load robustly from disk

The runtime SHALL load configuration from disk without treating common first-run or editor encoding artifacts as malformed config.

#### Scenario: UTF-8 BOM config is accepted

- **GIVEN** a config file starts with a UTF-8 byte order mark followed by valid JSON
- **WHEN** the config controller loads or reloads the file
- **THEN** it SHALL decode the JSON config successfully
- **AND** it SHALL NOT replace the file with defaults because of the byte order mark

#### Scenario: Empty config file initializes defaults

- **GIVEN** a config path exists but contains only empty or whitespace text
- **WHEN** the config controller loads the file
- **THEN** it SHALL return the default config
- **AND** it SHALL persist the default config to that path
- **AND** it SHALL NOT create a backup for the empty placeholder file

#### Scenario: Malformed config is backed up

- **GIVEN** a config file contains non-empty malformed JSON
- **WHEN** the config controller loads the file
- **THEN** it SHALL back up the malformed file
- **AND** it SHALL replace the config with defaults
