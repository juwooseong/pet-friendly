package com.petspot.global.util;

/**
 * 아이디(이메일) 찾기 응답에서 이메일을 마스킹 처리하기 위한 유틸리티.
 * 로컬 파트의 첫 글자만 노출하고 나머지는 '*'로 대체한다. (예: jjoodaeng2@gmail.com -> j*********@gmail.com)
 */
public final class EmailMasker {

    private EmailMasker() {
    }

    public static String mask(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }

        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);

        String masked = localPart.charAt(0) + "*".repeat(localPart.length() - 1);
        return masked + domainPart;
    }
}
