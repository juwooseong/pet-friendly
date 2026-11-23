// E2E 전용 DB(petspot_e2e) 접근 도우미 (TSK-QA-002).
// Node용 PostgreSQL 드라이버를 추가하지 않고, 이미 실행 중인 Docker 컨테이너(petspot-db)의 psql을 쓴다.
import { execFileSync } from 'node:child_process';

export const E2E_DB = 'petspot_e2e';
const CONTAINER = process.env.E2E_DB_CONTAINER ?? 'petspot-db';
const DB_USER = process.env.DB_USERNAME ?? 'petspot';

/** psql로 SQL을 실행하고 출력(행 단위, 구분자 |)을 돌려준다. 오류가 나면 예외를 던진다. */
export function psql(database, sql) {
  return execFileSync(
    'docker',
    ['exec', '-i', CONTAINER, 'psql', '-U', DB_USER, '-d', database, '-v', 'ON_ERROR_STOP=1', '-At'],
    { input: sql, encoding: 'utf8' }
  ).trim();
}
