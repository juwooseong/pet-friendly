package com.petspot.infrastructure.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * SMTP(JavaMailSender) 기반 EmailSender 구현체.
 * 발송 실패는 사용자에게 계정 존재 여부를 노출하지 않기 위해 상위로 전파하지 않고 로그로만 남긴다.
 * 임시 비밀번호 평문은 어떤 경우에도 로그에 출력하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender {

    private static final String SUBJECT = "[PetSpot] 임시 비밀번호 안내";

    private final JavaMailSender javaMailSender;

    @Override
    public void sendTemporaryPassword(String email, String temporaryPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(SUBJECT);
        message.setText(buildBody(temporaryPassword));

        try {
            javaMailSender.send(message);
            log.info("[AUTH FIND PASSWORD] Temporary password email sent to: {}", email);
        } catch (MailException ex) {
            log.error("[AUTH FIND PASSWORD] Failed to send temporary password email to: {}", email, ex);
        }
    }

    private String buildBody(String temporaryPassword) {
        return "PetSpot 임시 비밀번호가 발급되었습니다.\n\n"
                + "임시 비밀번호: " + temporaryPassword + "\n\n"
                + "임시 비밀번호로 로그인하신 후 반드시 새로운 비밀번호로 변경해주세요.";
    }
}
