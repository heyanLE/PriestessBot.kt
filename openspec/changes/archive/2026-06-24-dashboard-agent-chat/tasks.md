## 1. OpenSpec

- [x] 1.1 Create dashboard agent chat proposal, design, specs, and tasks.

## 2. Backend API

- [x] 2.1 Add Agent chat request/response/event DTOs.
- [x] 2.2 Inject Agent runtime collaborators into `DashboardService`.
- [x] 2.3 Implement `/api/agent/chat` with current or request-supplied Agent config.
- [x] 2.4 Capture Agent hook events in response DTOs.

## 3. Frontend

- [x] 3.1 Add Agent route and navigation entry.
- [x] 3.2 Extend Dashboard API client with Agent chat DTOs.
- [x] 3.3 Implement Agent view with config editor, provider/tool context, and chat transcript.

## 4. Tests

- [x] 4.1 Add route test for successful Agent chat.
- [x] 4.2 Add route test for missing provider error.
- [x] 4.3 Add route test for tool execution events.

## 5. Verification

- [x] 5.1 Run frontend build.
- [x] 5.2 Run targeted server tests.
- [x] 5.3 Run full Gradle test suite.
- [x] 5.4 Run OpenSpec strict validation/status checks.
