package com.petspot.infrastructure.email;

/**
 * 이메일 발송 책임을 추상화한 인터페이스.
 * 향후 AWS SES, SendGrid 등 구현체로 손쉽게 교체할 수 있도록
 * 인증 서비스와 실제 발송 방식(SMTP 등)의 결합을 분리한다.
 */
public interface EmailSender {

    /**
     * 비밀번호 재설정 링크 메일을 발송한다 (TSK-SEC-003).
     * 구현체는 요청 스레드를 붙잡지 않도록 비동기로 보내고, 실패해도 예외를 던지지 않는다
     * (응답 시간·오류로 계정 존재 여부가 드러나지 않게).
     *
     * @param email     수신 이메일 주소
     * @param resetLink 1회용 재설정 링크 (토큰 포함 - 로그에 출력 금지)
     */
    void sendPasswordResetLink(String email, String resetLink);
}
