package com.petspot.infrastructure.email;

/**
 * 이메일 발송 책임을 추상화한 인터페이스.
 * 향후 AWS SES, SendGrid 등 구현체로 손쉽게 교체할 수 있도록
 * 인증 서비스(AuthService)와 실제 발송 방식(SMTP 등)의 결합을 분리한다.
 */
public interface EmailSender {

    /**
     * 임시 비밀번호 안내 메일을 발송한다.
     *
     * @param email             수신 이메일 주소
     * @param temporaryPassword 평문 임시 비밀번호 (로그에 출력 금지)
     */
    void sendTemporaryPassword(String email, String temporaryPassword);
}
