package com.petspot.infrastructure.email;

import com.petspot.global.util.EmailMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * SMTP(JavaMailSender) 기반 EmailSender 구현체.
 * <p>요청 스레드와 분리해 비동기로 보낸다 - 가입된 이메일만 SMTP 대기 시간이 생겨 응답 시간으로 계정 존재 여부가
 * 드러나지 않도록(TSK-SEC-003). 발송 실패는 상위로 전파하지 않고 로그로만 남긴다.
 * 재설정 링크(토큰)와 이메일 원문은 로그에 출력하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender {

    static final String SUBJECT = "[PetSpot] 비밀번호 재설정 안내";

    private final JavaMailSender javaMailSender;

    @Async
    @Override
    public void sendPasswordResetLink(String email, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(SUBJECT);
        message.setText(buildBody(resetLink));

        try {
            javaMailSender.send(message);
            log.info("[AUTH PASSWORD RESET] Reset link email sent to: {}", EmailMasker.mask(email));
        } catch (MailException ex) {
            log.error("[AUTH PASSWORD RESET] Failed to send reset link email to: {}", EmailMasker.mask(email), ex);
        }
    }

    private String buildBody(String resetLink) {
        return "PetSpot 비밀번호 재설정 요청을 받았습니다.\n\n"
                + "아래 링크에서 새 비밀번호를 설정해주세요. 링크는 30분 동안 한 번만 사용할 수 있습니다.\n"
                + resetLink + "\n\n"
                + "직접 요청하지 않으셨다면 이 메일을 무시하세요. 기존 비밀번호는 바뀌지 않습니다.";
    }
}
