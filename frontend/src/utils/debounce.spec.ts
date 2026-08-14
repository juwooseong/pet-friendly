import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { debounce } from './debounce';

describe('debounce', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('지정한 지연 시간 이후에 함수를 1회 실행한다', () => {
    const fn = vi.fn();
    const debounced = debounce(fn, 400);

    debounced('a');
    expect(fn).not.toHaveBeenCalled();

    vi.advanceTimersByTime(400);
    expect(fn).toHaveBeenCalledTimes(1);
    expect(fn).toHaveBeenCalledWith('a');
  });

  it('지연 시간 내에 연속 호출되면 마지막 호출의 인자로만 1회 실행한다', () => {
    const fn = vi.fn();
    const debounced = debounce(fn, 400);

    debounced('a');
    vi.advanceTimersByTime(200);
    debounced('b');
    vi.advanceTimersByTime(200);
    debounced('c');
    vi.advanceTimersByTime(400);

    expect(fn).toHaveBeenCalledTimes(1);
    expect(fn).toHaveBeenCalledWith('c');
  });

  it('cancel() 호출 시 대기 중인 실행이 취소된다', () => {
    const fn = vi.fn();
    const debounced = debounce(fn, 400);

    debounced('a');
    debounced.cancel();
    vi.advanceTimersByTime(400);

    expect(fn).not.toHaveBeenCalled();
  });
});
