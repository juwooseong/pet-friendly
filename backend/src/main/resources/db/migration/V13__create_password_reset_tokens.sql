-- TSK-SEC-003: 비밀번호 재설정 링크 토큰. 원문 토큰은 메일에만 담고, DB에는 SHA-256 해시만 저장한다.
CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 계정별 요청 횟수 제한(최근 N분 발급 수)과 미사용 토큰 조회용
CREATE INDEX idx_password_reset_tokens_user_created ON password_reset_tokens (user_id, created_at);
