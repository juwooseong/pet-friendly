-- TSK-SEC-001: 비밀번호 변경·임시 비밀번호 발급 시 1씩 올려 이전에 발급한 JWT를 무효화한다.
ALTER TABLE users
ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;
