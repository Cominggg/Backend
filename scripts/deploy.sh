#!/bin/bash
# 블루-그린 배포 스크립트
# 전제조건:
#   - ~/compose/docker-compose.yml: be-blue(:8080), be-green(:8081), redis 서비스 정의
#   - ~/compose/.env: 앱 환경변수
#   - /etc/nginx/conf.d/upstream.conf: coming_backend upstream 정의
#   - deploy 유저가 nginx 명령에 대해 passwordless sudo 보유
set -euo pipefail

COMPOSE_FILE="$HOME/compose/docker-compose.yml"
SLOT_FILE="$HOME/compose/active_slot"
NGINX_UPSTREAM="/etc/nginx/conf.d/upstream.conf"
HEALTH_TIMEOUT=30

current=$(cat "$SLOT_FILE" 2>/dev/null || echo "blue")
if [ "$current" = "blue" ]; then
    next="green"
    next_port=8081
else
    next="blue"
    next_port=8080
fi

echo "[deploy] $current → $next (:$next_port)"

timeout 300 docker compose -f "$COMPOSE_FILE" pull "be-$next"
docker compose -f "$COMPOSE_FILE" up -d "be-$next"

echo "[deploy] health check (최대 ${HEALTH_TIMEOUT}s) ..."
for i in $(seq 1 $HEALTH_TIMEOUT); do
    if curl -sf "http://localhost:$next_port/actuator/health" > /dev/null 2>&1; then
        echo "[deploy] healthy (${i}s)"
        break
    fi
    if [ "$i" -eq "$HEALTH_TIMEOUT" ]; then
        echo "[deploy] health check 실패 — 롤백"
        docker compose -f "$COMPOSE_FILE" stop "be-$next"
        exit 1
    fi
    sleep 1
done

# Nginx upstream 전환
cat > /tmp/upstream.conf <<EOF
upstream coming_backend {
    server 127.0.0.1:${next_port};
}
EOF
sudo mv /tmp/upstream.conf "$NGINX_UPSTREAM"
sudo nginx -s reload

echo "$next" > "$SLOT_FILE"
docker compose -f "$COMPOSE_FILE" stop "be-$current"

echo "[deploy] 완료 — active: $next (:$next_port)"
