// 백엔드를 띄우기 전에 E2E 전용 DB가 있는지 확인하고, 없으면 만든다 (TSK-QA-002).
// 스키마는 백엔드 시작 시 Flyway가 만들고, 데이터 초기화·시드는 global-setup.ts가 한다.
import { E2E_DB, psql } from './db.mjs';

const exists = psql('postgres', `SELECT 1 FROM pg_database WHERE datname = '${E2E_DB}';`);
if (exists !== '1') {
  psql('postgres', `CREATE DATABASE ${E2E_DB};`);
  console.log(`[e2e] created database ${E2E_DB}`);
}
