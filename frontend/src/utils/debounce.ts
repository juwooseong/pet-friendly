/**
 * 지정한 지연 시간(ms) 동안 추가 호출이 없을 때만 원본 함수를 실행하는 디바운스 래퍼.
 * 반환된 함수의 cancel()로 대기 중인 실행을 취소할 수 있다 (컴포넌트 unmount 시 정리용).
 */
export function debounce<T extends (...args: any[]) => void>(fn: T, delayMs: number) {
  let timer: ReturnType<typeof setTimeout> | undefined;

  const debounced = (...args: Parameters<T>) => {
    if (timer) clearTimeout(timer);
    timer = setTimeout(() => {
      timer = undefined;
      fn(...args);
    }, delayMs);
  };

  debounced.cancel = () => {
    if (timer) clearTimeout(timer);
    timer = undefined;
  };

  return debounced;
}
