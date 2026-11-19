package com.petspot.global.util;

/**
 * 비밀번호 규칙 (TSK-BE-034)
 *
 * <p>회원가입({@code UserRegisterRequestDto})과 비밀번호 변경({@code ChangePasswordRequestDto})이 같은 규칙을 쓰도록
 * 한곳에 모았다. 과거에는 비밀번호 변경만 영문/숫자/특수문자 조합을 강제하고 회원가입은 길이만 검사해서,
 * API를 직접 호출하면 약한 비밀번호로 가입할 수 있었다. 프런트엔드 {@code utils/validators.ts}의
 * {@code PASSWORD_PATTERN}과 같은 정규식이므로 바꿀 때는 함께 바꾼다.
 */
public final class PasswordPolicy {

    /** 8~100자, 영문·숫자·특수문자 각각 1자 이상 포함 */
    public static final String REGEX = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,100}$";

    public static final String MESSAGE = "비밀번호는 8자 이상 100자 이하이며 영문, 숫자, 특수문자를 모두 포함해야 합니다.";

    private PasswordPolicy() {
    }
}
