#!/usr/bin/env bash
# 더미 데이터 시딩 -> k6 실행 -> 더미 데이터 정리를 한 번에 묶는다.
# k6가 실패하거나 스크립트가 중간에 죽어도 trap으로 cleanup.sql이 항상 실행된다.
#
# 사용법: ./load-test/run.sh <도메인/시나리오> [profile]
#   ./load-test/run.sh concert/list smoke
#   ./load-test/run.sh artist/detail stress
set -euo pipefail

SCENARIO="${1:?사용법: ./load-test/run.sh <도메인/시나리오> [smoke|load|stress]}"
PROFILE="${2:-load}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DB_HOST="${DB_HOST:-localhost}"
DB_NAME="${DB_NAME:-coming}"
BASE_URL="${BASE_URL:-http://localhost:8080}"

cleanup() {
  echo "[cleanup] 더미 데이터 정리 중"
  psql -h "$DB_HOST" -d "$DB_NAME" -v ON_ERROR_STOP=1 -f "$SCRIPT_DIR/seed/cleanup.sql"
}
trap cleanup EXIT

echo "[setup] 테스트 유저 준비 (local 프로파일 전용 /api/dev/login)"
curl -sf -X POST "$BASE_URL/api/dev/login" \
  -H "Content-Type: application/json" \
  -d '{"nickname":"k6-load-test","role":"USER"}' > /dev/null

TEST_USER_ID=$(psql -h "$DB_HOST" -d "$DB_NAME" -t -A \
  -c "SELECT id FROM \"user\" WHERE provider = 'local' AND provider_id = 'k6-load-test'")

echo "[seed] 더미 데이터 시딩 중 (test_user_id=$TEST_USER_ID)"
psql -h "$DB_HOST" -d "$DB_NAME" -v ON_ERROR_STOP=1 -v test_user_id="$TEST_USER_ID" \
  -f "$SCRIPT_DIR/seed/seed.sql"

HOT_ARTIST_ID=$(psql -h "$DB_HOST" -d "$DB_NAME" -t -A \
  -c "SELECT id FROM artist WHERE name = '[LOADTEST] Artist 1'")

echo "[k6] $SCENARIO 실행 (profile=$PROFILE)"
k6 run -e PROFILE="$PROFILE" -e BASE_URL="$BASE_URL" -e HOT_ARTIST_ID="$HOT_ARTIST_ID" \
  "$SCRIPT_DIR/k6/$SCENARIO.js"
