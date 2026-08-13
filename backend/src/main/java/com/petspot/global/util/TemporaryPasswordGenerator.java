package com.petspot.global.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 비밀번호 찾기 시 발급되는 임시 비밀번호를 생성한다.
 * 영문 대/소문자, 숫자, 특수문자를 각 1자 이상 포함하는 12자리를 SecureRandom 기반으로 생성한다.
 */
@Component
public class TemporaryPasswordGenerator {

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SPECIAL = "!@#$%^&*";
    private static final String ALL = UPPER + LOWER + DIGITS + SPECIAL;
    private static final int LENGTH = 12;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        List<Character> chars = new ArrayList<>(LENGTH);
        chars.add(pick(UPPER));
        chars.add(pick(LOWER));
        chars.add(pick(DIGITS));
        chars.add(pick(SPECIAL));
        for (int i = chars.size(); i < LENGTH; i++) {
            chars.add(pick(ALL));
        }
        Collections.shuffle(chars, secureRandom);

        StringBuilder sb = new StringBuilder(LENGTH);
        chars.forEach(sb::append);
        return sb.toString();
    }

    private char pick(String source) {
        return source.charAt(secureRandom.nextInt(source.length()));
    }
}
