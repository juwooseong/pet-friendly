import { test, expect } from '@playwright/test';
import { E2E_BACKEND_URL, E2E_PASSWORD, SEED_PLACE } from './seed';

/**
 * 회귀 테스트: 로그인 → 장소 검색 → 상세 → 리뷰 작성 → 새로고침 후 유지 → 즐겨찾기 (TSK-QA-002).
 * 2026-08-18 TSK-FE-008 리뷰 기능을 수작업으로 검증했던 시나리오를 고정한다.
 */
test('로그인한 사용자가 장소를 찾아 리뷰를 남기고 즐겨찾기한다', async ({ page, request }) => {
  // given: API로 회원가입 (실행마다 고유 계정)
  const suffix = Date.now().toString(36);
  const email = `e2e-${suffix}@petspot.test`;
  const register = await request.post(`${E2E_BACKEND_URL}/api/v1/auth/register`, {
    data: { email, password: E2E_PASSWORD, nickname: `e2e${suffix}` },
  });
  expect(register.ok(), await register.text()).toBeTruthy();

  // 로그인
  await page.goto('/login');
  await page.getByPlaceholder('example@petspot.com').fill(email);
  await page.locator('input[type="password"]').fill(E2E_PASSWORD);
  await page.getByRole('button', { name: '로그인' }).click();
  await expect(page).not.toHaveURL(/\/login/);

  // 장소 검색 → 상세
  await page.goto('/search');
  await page.getByPlaceholder('장소명 또는 주소를 입력하세요').fill(SEED_PLACE.name);
  await page.getByRole('button', { name: '검색', exact: true }).click();
  await page.getByText(SEED_PLACE.name).first().click();
  await expect(page).toHaveURL(new RegExp(`/place/${SEED_PLACE.id}$`));
  await expect(page.getByText('아직 작성된 리뷰가 없습니다.')).toBeVisible();

  // 리뷰 작성 (별점 4점)
  const content = `E2E 리뷰 ${suffix} - 강아지와 다녀왔어요.`;
  await page.getByRole('button', { name: '★', exact: true }).nth(3).click();
  await page.getByPlaceholder('이 장소에 대한 리뷰를 남겨주세요.').fill(content);
  await page.getByRole('button', { name: '리뷰 등록' }).click();
  await expect(page.getByText(content)).toBeVisible();
  await expect(page.getByText('리뷰 1개')).toBeVisible();

  // 새로고침해도 리뷰와 리뷰 수가 유지된다
  await page.reload();
  await expect(page.getByText(content)).toBeVisible();
  await expect(page.getByText('리뷰 1개')).toBeVisible();

  // 즐겨찾기 → 즐겨찾기 목록에 표시
  // 버튼 이름 앞에 아이콘 글자가 붙어 있어 끝부분으로 찾는다
  await page.getByRole('button', { name: /즐겨찾기$/ }).click();
  await expect(page.getByRole('button', { name: /즐겨찾기됨$/ })).toBeVisible();
  await page.goto('/favorites');
  await expect(page.getByText(SEED_PLACE.name)).toBeVisible();
});
