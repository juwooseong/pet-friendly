import { defineConfig, devices } from '@playwright/test';

/**
 * E2E 테스트 설정 (TSK-QA-002). 실행: `npm run e2e`
 *
 * - 백엔드: E2E 전용 DB(petspot_e2e)에 붙은 Spring Boot를 8090 포트로 띄운다(개발 DB·백엔드 테스트 DB와 분리).
 *   Docker의 petspot-db 컨테이너가 실행 중이어야 한다. DB가 없으면 `e2e/prepare-db.mjs`가 만든다.
 * - 프런트: Vite 개발 서버를 3100 포트로 띄우고 `/api`를 위 백엔드로 프록시한다.
 * - 시드: 두 서버가 뜬 뒤(Flyway 완료 후) `e2e/global-setup.ts`가 데이터를 비우고 테스트 장소를 넣는다.
 *
 * Vitest와 섞이지 않도록 E2E 파일은 `*.e2e.ts`로 만든다.
 */
const BACKEND_PORT = 8090; // e2e/seed.ts의 E2E_BACKEND_URL과 같아야 한다
const FRONTEND_PORT = 3100;
const DB_PORT = process.env.DB_PORT ?? '5433';
const isWindows = process.platform === 'win32';

const backendArgs = [
  `--server.port=${BACKEND_PORT}`,
  `--spring.datasource.url=jdbc:postgresql://localhost:${DB_PORT}/petspot_e2e`,
  '--spring.devtools.restart.enabled=false',
  '--spring.jpa.show-sql=false',
  // 메일 계정을 비워 실제 메일이 나가지 않게 한다
  '--spring.mail.username=',
  '--spring.mail.password=',
].join(' ');

export default defineConfig({
  testDir: './e2e',
  testMatch: '**/*.e2e.ts',
  globalSetup: './e2e/global-setup.ts',
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never', outputFolder: 'playwright-report' }]],
  outputDir: 'test-results',
  use: {
    baseURL: `http://127.0.0.1:${FRONTEND_PORT}`,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: [
    {
      command: `node ../frontend/e2e/prepare-db.mjs && ${isWindows ? '.\\gradlew.bat' : './gradlew'} bootRun --args="${backendArgs}"`,
      cwd: '../backend',
      url: `http://127.0.0.1:${BACKEND_PORT}/api/v1/places/search?size=1`,
      timeout: 240_000,
      reuseExistingServer: !process.env.CI,
    },
    {
      command: `npx vite --port ${FRONTEND_PORT} --strictPort`,
      url: `http://127.0.0.1:${FRONTEND_PORT}`,
      timeout: 60_000,
      reuseExistingServer: !process.env.CI,
      // 개발용 .env.development는 API를 http://localhost:8080으로 직접 부르므로, E2E는 상대 경로로 바꿔 프록시를 거치게 한다
      // (Vite는 이미 있는 환경변수를 .env 파일보다 우선한다)
      env: { VITE_API_BASE_URL: '/api/v1', VITE_API_PROXY_TARGET: `http://127.0.0.1:${BACKEND_PORT}` },
    },
  ],
});
