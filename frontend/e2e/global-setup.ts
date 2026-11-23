// 두 서버(백엔드·프런트)가 뜬 뒤 실행된다. 백엔드가 Flyway로 스키마를 만든 상태에서
// E2E DB의 데이터를 비우고 테스트 장소를 넣는다 (TSK-QA-002). 실행할 때마다 같은 상태에서 시작한다.
import { E2E_DB, psql } from './db.mjs';
import { SEED_PLACE } from './seed';

const DATA_TABLES = ['reviews', 'place_reviews', 'favorites', 'pets', 'places', 'users'];

export default function globalSetup() {
  const p = SEED_PLACE;
  psql(
    E2E_DB,
    `
    TRUNCATE ${DATA_TABLES.join(', ')} CASCADE;
    INSERT INTO places (id, name, category, category_name, address, location, latitude, longitude,
                        rating, review_count, allowed_sizes, is_indoor_allowed, is_outdoor_allowed, has_off_leash_zone)
    VALUES ('${p.id}', '${p.name}', '${p.category}', '${p.categoryName}', '${p.address}',
            ST_SetSRID(ST_MakePoint(${p.longitude}, ${p.latitude}), 4326), ${p.latitude}, ${p.longitude},
            0.0, 0, '["SMALL", "MEDIUM"]', NULL, NULL, NULL);
    `
  );
  console.log(`[e2e] ${E2E_DB} reset and seeded (place: ${p.name})`);
}
