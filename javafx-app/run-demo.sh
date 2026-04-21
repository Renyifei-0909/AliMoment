#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

SERVER_HOST="${1:-10.102.64.114}"
SERVER_USER="${2:-xtx}"
REMOTE_PORT="${3:-8000}"
LOCAL_PORT="${4:-18000}"

chmod +x mvnw
chmod +x run-with-api.sh

if ! lsof -iTCP:"${LOCAL_PORT}" -sTCP:LISTEN >/dev/null 2>&1; then
  echo "Opening SSH tunnel ${LOCAL_PORT} -> ${SERVER_HOST}:${REMOTE_PORT}"
  ssh -f -N -L "${LOCAL_PORT}:127.0.0.1:${REMOTE_PORT}" "${SERVER_USER}@${SERVER_HOST}"
else
  echo "Local port ${LOCAL_PORT} already in use, reusing existing tunnel/service."
fi

exec ./run-with-api.sh "http://127.0.0.1:${LOCAL_PORT}"
