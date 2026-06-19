# NapCat Testing

This project keeps NapCat coverage in two layers:

1. Default unit tests parse representative OneBot/NapCat message payloads without a live server.
2. An opt-in integration test connects to a local NapCat WebSocket endpoint and calls `get_status`.

The NapCat setup reference used for the integration target is:
https://napneko.github.io/guide/start-install

## Unit Tests

Run the normal test task:

```powershell
.\gradlew.bat test
```

Covered behavior:

- Private WebSocket message payloads become `MessageEvent` with `SessionType.PRIVATE`.
- Group WebSocket message payloads use `group_id` as the session id.
- Non-message WebSocket events are ignored.
- HTTP fallback payloads with `sender.user_id` parse into message events.

## Local WebSocket Integration

The integration test is disabled by default so CI and developer machines do not
need a running NapCat instance.

Start NapCat locally, then run:

```powershell
.\gradlew.bat "-Dnapcat.integration.enabled=true" "-Dnapcat.ws.host=192.168.31.24" "-Dnapcat.ws.port=10001" test --tests com.heyanle.priestess.bot.platform.adapters.napcat4_18_6.NapCatWebSocketIntegrationTest
```

Equivalent environment variables:

```powershell
$env:NAPCAT_INTEGRATION_ENABLED="true"
$env:NAPCAT_WS_HOST="192.168.31.24"
$env:NAPCAT_WS_PORT="10001"
.\gradlew.bat test --tests com.heyanle.priestess.bot.platform.adapters.napcat4_18_6.NapCatWebSocketIntegrationTest
```

If your NapCat WebSocket requires an access token, add one of:

```powershell
$env:NAPCAT_ACCESS_TOKEN="<your-token>"
```

or:

```powershell
.\gradlew.bat "-Dnapcat.integration.enabled=true" "-Dnapcat.access.token=<your-token>" test --tests com.heyanle.priestess.bot.platform.adapters.napcat4_18_6.NapCatWebSocketIntegrationTest
```

The test opens `ws://<host>:<port>`, sends:

```json
{"action":"get_status","params":{},"echo":"priestess-napcat-test"}
```

and verifies that the response contains the matching `echo` plus a OneBot-style
status or retcode field.

When no token is supplied and the server requires one, `retcode=1403` is treated
as a successful reachability check: it proves the WebSocket endpoint is a live
NapCat/OneBot server, but not an authenticated API call.
