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

probe_health() {
  local port="$1"
  python3 - "$port" <<'PY'
import json
import sys
import urllib.request

port = sys.argv[1]
url = f"http://127.0.0.1:{port}/health"
try:
    with urllib.request.urlopen(url, timeout=2) as resp:
        text = resp.read().decode("utf-8")
        data = json.loads(text)
        if data.get("status") == "ok":
            print(text)
            sys.exit(0)
        sys.exit(1)
except Exception:
    sys.exit(1)
PY
}

pick_port_and_tunnel() {
  local start_port="$1"
  local end_port=$((start_port + 9))
  local port
  for ((port=start_port; port<=end_port; port++)); do
    if probe_health "$port" >/dev/null 2>&1; then
      echo "$port"
      return 0
    fi
    if ! lsof -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
      echo "Opening SSH tunnel ${port} -> ${SERVER_HOST}:${REMOTE_PORT}"
      ssh -f -N -L "${port}:127.0.0.1:${REMOTE_PORT}" "${SERVER_USER}@${SERVER_HOST}"
      sleep 1
      if probe_health "$port" >/dev/null 2>&1; then
        echo "$port"
        return 0
      fi
    fi
  done
  return 1
}

TARGET_PORT="$(pick_port_and_tunnel "${LOCAL_PORT}")" || {
  echo "Unable to establish a healthy tunnel to ${SERVER_HOST}:${REMOTE_PORT}."
  echo "Please make sure the server backend is running, then retry."
  exit 1
}

echo "Using local tunnel port: ${TARGET_PORT}"

exec ./run-with-api.sh "http://127.0.0.1:${TARGET_PORT}"
