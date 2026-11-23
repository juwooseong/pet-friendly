#!/usr/bin/env bash
# CI용 DB 실행 (TSK-INF-006). 로컬 docker-compose의 petspot-db와 같은 이미지·이름·포트(5433)로 띄운다.
# - 백엔드 테스트는 petspot_test(init-postgis.sql이 생성), E2E는 petspot_e2e(prepare-db.mjs가 생성)를 쓴다.
# - E2E 스크립트(frontend/e2e/db.mjs)가 `docker exec petspot-db psql`로 접근하므로 컨테이너 이름을 맞춘다.
set -euo pipefail

docker run -d --name petspot-db -p 5433:5432 \
  -e POSTGRES_DB=petspot_db \
  -e POSTGRES_USER="${DB_USERNAME}" \
  -e POSTGRES_PASSWORD="${DB_PASSWORD}" \
  -v "${GITHUB_WORKSPACE}/docker/postgres/init-postgis.sql:/docker-entrypoint-initdb.d/init-postgis.sql:ro" \
  postgis/postgis:17-3.5

# 초기화 중의 임시 서버는 TCP를 열지 않으므로, TCP 접속이 되면 초기화(petspot_test 생성 포함)가 끝난 것이다.
for _ in $(seq 1 60); do
  if docker exec petspot-db pg_isready -h 127.0.0.1 -U "${DB_USERNAME}" -d petspot_test >/dev/null 2>&1; then
    echo "petspot-db ready"
    exit 0
  fi
  sleep 2
done

echo "petspot-db did not become ready" >&2
docker logs petspot-db >&2
exit 1
