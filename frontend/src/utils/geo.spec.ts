import { describe, it, expect } from 'vitest';
import { clamp, distanceKm } from './geo';

describe('distanceKm', () => {
  it('같은 좌표는 0km', () => {
    expect(distanceKm({ latitude: 37.5665, longitude: 126.978 }, { latitude: 37.5665, longitude: 126.978 })).toBe(0);
  });

  it('위도 1도 차이는 약 111km', () => {
    const d = distanceKm({ latitude: 37, longitude: 127 }, { latitude: 38, longitude: 127 });
    expect(d).toBeGreaterThan(110.5);
    expect(d).toBeLessThan(111.5);
  });

  it('서울시청 ~ 부산시청은 약 325km', () => {
    const d = distanceKm({ latitude: 37.5665, longitude: 126.978 }, { latitude: 35.1796, longitude: 129.0756 });
    expect(d).toBeGreaterThan(315);
    expect(d).toBeLessThan(335);
  });
});

describe('clamp', () => {
  it('범위 안의 값은 그대로, 밖의 값은 경계로', () => {
    expect(clamp(5, 1, 50)).toBe(5);
    expect(clamp(0.3, 1, 50)).toBe(1);
    expect(clamp(120, 1, 50)).toBe(50);
  });
});
