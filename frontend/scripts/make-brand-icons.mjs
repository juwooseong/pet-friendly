// 브랜드 아이콘·공유 이미지 생성 (TSK-FE-031). Chromium으로 HTML을 그려 PNG로 저장한다.
// 실행: frontend 폴더에서 `node scripts/make-brand-icons.mjs` (캐릭터 그림·로고를 바꾸면 다시 실행)
// 글꼴은 실행한 PC의 시스템 글꼴(Windows: 맑은 고딕)을 쓴다.
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { chromium } from '@playwright/test';

const FE = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const OUT = FE + '/public';
const dataUrl = (p, type = 'image/webp') => `data:${type};base64,` + fs.readFileSync(p).toString('base64');
const kongi = dataUrl(FE + '/src/assets/characters/kongi-face.webp');
const nabi = dataUrl(FE + '/src/assets/characters/nabi-face.webp');
const duo = dataUrl(FE + '/public/images/duo-together.webp');

// 로고의 지도 핀 + 발자국 (BrandLogo.vue와 같은 모양)
const PIN = (fill = '#2fb585') => `<svg viewBox="0 0 32 40" xmlns="http://www.w3.org/2000/svg">
  <path d="M16 39 C12 34 2 24 2 15 A14 14 0 1 1 30 15 C30 24 20 34 16 39 Z" fill="${fill}"/>
  <g fill="#fff"><ellipse cx="16" cy="19.5" rx="5" ry="4.2"/><circle cx="9.8" cy="14" r="2.3"/><circle cx="13.6" cy="10.3" r="2.3"/><circle cx="18.4" cy="10.3" r="2.3"/><circle cx="22.2" cy="14" r="2.3"/></g>
</svg>`;

// 파비콘 SVG: 정사각형 안에 핀을 가운데 놓는다
const faviconSvg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40">
  <path transform="translate(4 0)" d="M16 39 C12 34 2 24 2 15 A14 14 0 1 1 30 15 C30 24 20 34 16 39 Z" fill="#2fb585"/>
  <g transform="translate(4 0)" fill="#fff"><ellipse cx="16" cy="19.5" rx="5" ry="4.2"/><circle cx="9.8" cy="14" r="2.3"/><circle cx="13.6" cy="10.3" r="2.3"/><circle cx="18.4" cy="10.3" r="2.3"/><circle cx="22.2" cy="14" r="2.3"/></g>
</svg>
`;
fs.writeFileSync(OUT + '/favicon.svg', faviconSvg);

const base = `<style>*{margin:0;box-sizing:border-box}html,body{background:transparent}
body{font-family:'Malgun Gothic','Apple SD Gothic Neo',sans-serif}</style>`;

// 앱 아이콘: 크림 바탕 + 콩이 얼굴. maskable은 가운데 80% 안전 영역에 들어오게 더 작게.
const appIcon = (size, faceRatio) => `${base}<div style="width:${size}px;height:${size}px;display:flex;align-items:center;justify-content:center;
  background:linear-gradient(160deg,#fff4e2,#fffbf5 55%,#d3f5e6)">
  <img src="${kongi}" style="width:${Math.round(size * faceRatio)}px;height:auto"></div>`;

// 공유 미리보기 1200x630
const og = `${base}<div style="width:1200px;height:630px;display:flex;align-items:center;gap:24px;padding:0 64px 0 80px;
  background:linear-gradient(135deg,#fff4e2,#fffbf5 50%,#eefbf5)">
  <div style="flex:1">
    <div style="display:flex;align-items:center;gap:14px">
      <div style="width:56px;height:70px">${PIN()}</div>
      <div style="font-size:64px;font-weight:800;letter-spacing:-1px"><span style="color:#292524">Pet</span><span style="color:#22966d">Spot</span></div>
    </div>
    <div style="margin-top:36px;font-size:60px;font-weight:800;line-height:1.25;color:#292524">헛걸음 없는<br>반려동물 여행</div>
    <div style="margin-top:24px;font-size:28px;color:#57534e;white-space:nowrap">우리 아이와 갈 수 있는 곳만 골라 보세요</div>
    <div style="margin-top:28px;display:inline-flex;align-items:center;gap:10px;font-size:24px;font-weight:700;color:#1d7758;background:#eefbf5;border:2px solid #a9ebd0;border-radius:999px;padding:8px 20px">
      <img src="${kongi}" style="height:34px"><img src="${nabi}" style="height:34px">콩이와 나비가 먼저 다녀왔어요
    </div>
  </div>
  <img src="${duo}" style="width:520px;height:auto">
</div>`;

const jobs = [
  ['favicon-32.png', `${base}<div style="width:32px;height:32px">${faviconSvg}</div>`, 32, 32, true],
  ['apple-touch-icon.png', appIcon(180, 0.72), 180, 180, false],
  ['icon-192.png', appIcon(192, 0.72), 192, 192, false],
  ['icon-512.png', appIcon(512, 0.72), 512, 512, false],
  ['icon-maskable-512.png', appIcon(512, 0.56), 512, 512, false],
  ['images/og-image.png', og, 1200, 630, false],
];

const browser = await chromium.launch();
for (const [name, html, w, h, transparent] of jobs) {
  const page = await browser.newPage({ viewport: { width: w, height: h } });
  await page.setContent(html, { waitUntil: 'load' });
  await page.evaluate(() => Promise.all([...document.images].map((i) => i.decode())));
  await page.screenshot({ path: `${OUT}/${name}`, omitBackground: transparent, clip: { x: 0, y: 0, width: w, height: h } });
  console.log(name, fs.statSync(`${OUT}/${name}`).size);
  await page.close();
}
await browser.close();
