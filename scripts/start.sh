#!/usr/bin/env bash
set -euo pipefail
project_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$project_root"
if docker compose version >/dev/null 2>&1; then compose=(docker compose); else compose=(docker-compose); fi
[[ -f deploy/.env ]] || { echo 'Create deploy/.env from deploy/.env.example first.'; exit 1; }
[[ -f target/sales-copilot-1.0.0.jar && -f frontEnd/pc/dist/index.html ]] || { echo 'Run scripts/build.sh first.'; exit 1; }
chmod 600 deploy/.env
"${compose[@]}" --env-file deploy/.env -f deploy/compose.yaml up -d mysql redis
for attempt in {1..60}; do
  if "${compose[@]}" --env-file deploy/.env -f deploy/compose.yaml exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u sales_copilot sales_copilot -Nse "SELECT 1"' >/dev/null 2>&1; then break; fi
  [[ "$attempt" -lt 60 ]] || { echo 'Database not ready.'; exit 1; }
  sleep 2
done
# Idempotent additive schema; no DROP, no bundled customer data.
for migration in deploy/sql/*.sql; do
  "${compose[@]}" --env-file deploy/.env -f deploy/compose.yaml exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u sales_copilot sales_copilot' < "$migration"
done
"${compose[@]}" --env-file deploy/.env -f deploy/compose.yaml up -d --build api web
http_port=$(sed -n 's/^HTTP_PORT=//p' deploy/.env)
for attempt in {1..45}; do
  if curl --noproxy '*' --fail --silent --max-time 3 "http://127.0.0.1:${http_port:-8088}/api/health" >/dev/null; then
    echo "Services ready: http://127.0.0.1:${http_port:-8088}/tools/login"
    exit 0
  fi
  sleep 2
done
echo 'Startup health check failed; inspect API logs.' >&2
exit 1
