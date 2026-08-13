const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,100}$/;

export function isValidPassword(password: string): boolean {
  return PASSWORD_PATTERN.test(password);
}

export function doPasswordsMatch(password: string, passwordConfirm: string): boolean {
  return password === passwordConfirm;
}
