#!/usr/bin/env bash

# 按黑盒测试环境的完整形态构建并启动应用：打包后的 Dashboard、
# Telegram 适配器和 HTTP 服务。
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEFAULT_CONFIG="$PROJECT_DIR/config.json"
if [[ ! -f "$DEFAULT_CONFIG" ]]; then
  DEFAULT_CONFIG="$PROJECT_DIR/manual-test-logs/telegram-tools-smoke/config.json"
fi

CONFIG_PATH="${PRIESTESS_CONFIG_PATH:-$DEFAULT_CONFIG}"
HEALTH_URL="${PRIESTESS_HEALTH_URL:-http://127.0.0.1:8080/health}"

if [[ ! -f "$CONFIG_PATH" ]]; then
  echo "Configuration file not found: $CONFIG_PATH" >&2
  exit 1
fi

echo "Building the packaged Dashboard and application JAR..."
(cd "$PROJECT_DIR" && ./gradlew --no-daemon -PbuildDashboard=true jar)

echo "Starting black-box runtime with config: $CONFIG_PATH"
env \
  PRIESTESS_CONFIG_PATH="$CONFIG_PATH" \
  PRIESTESS_SERVER_ENABLED=true \
  "$PROJECT_DIR/run-server.sh" &
SERVER_PID=$!

cleanup() {
  kill "$SERVER_PID" 2>/dev/null || true
}
trap cleanup EXIT INT TERM

for _ in {1..30}; do
  if curl -fsS "$HEALTH_URL" >/dev/null; then
    echo "Black-box runtime is ready. Dashboard: ${HEALTH_URL%/health}"
    wait "$SERVER_PID"
    exit $?
  fi
  if ! kill -0 "$SERVER_PID" 2>/dev/null; then
    wait "$SERVER_PID"
    exit $?
  fi
  sleep 1
done

echo "Timed out waiting for health check: $HEALTH_URL" >&2
exit 1
