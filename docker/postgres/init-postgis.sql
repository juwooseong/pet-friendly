-- Enable PostGIS Spatial Extension
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 백엔드 테스트 전용 DB (TSK-QA-003). 테스트가 실제 데이터(petspot_db)와 섞이지 않도록 분리한다.
-- 스키마·확장은 테스트 실행 시 Flyway(V1)가 만든다.
-- 이 스크립트는 볼륨이 비어 있을 때(최초 생성)만 실행된다. 기존 볼륨에는 직접 만든다:
--   docker exec petspot-db psql -U petspot -d postgres -c "CREATE DATABASE petspot_test"
CREATE DATABASE petspot_test;

-- Log confirmation
SELECT PostGIS_Full_Version();
