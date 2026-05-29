#!/usr/bin/env bash
set -euo pipefail

# Connects a WS subscriber, publishes via HTTP, asserts the subscriber receives the event.
BASE="${BASE:-http://localhost:8080}"
WS="${WS:-ws://localhost:8080/ws}"
API_KEY="${API_KEY:-dev-key}"
CHANNEL="smoke"
PAYLOAD='{"smoke":true,"n":1}'

OUT="$(mktemp)"

( printf '{"action":"subscribe","channel":"%s"}\n' "$CHANNEL"; sleep 6 ) \
  | websocat -n1 "$WS" > "$OUT" &
WS_PID=$!

sleep 2

curl -sf -X POST "$BASE/publish/$CHANNEL" \
  -H "X-API-Key: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "$PAYLOAD"

wait "$WS_PID" || true

if grep -q '"smoke":true' "$OUT"; then
  echo "SMOKE PASS: subscriber received the published event"
  exit 0
else
  echo "SMOKE FAIL: event not received. Captured:"
  cat "$OUT"
  exit 1
fi
