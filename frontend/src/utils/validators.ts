// 백엔드 com.petspot.global.util.PasswordPolicy.REGEX와 같은 규칙(TSK-BE-034) - 바꿀 때는 함께 바꾼다.
const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,100}$/;

export function isValidPassword(password: string): boolean {
  return PASSWORD_PATTERN.test(password);
}

export function doPasswordsMatch(password: string, passwordConfirm: string): boolean {
  return password === passwordConfirm;
}
