#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

API_BASE_URL="${1:-${ALIMOMENT_API_BASE_URL:-http://127.0.0.1:8000}}"

chmod +x mvnw
echo "Using API base URL: ${API_BASE_URL}"

ALIMOMENT_API_BASE_URL="${API_BASE_URL}" ./mvnw javafx:run
