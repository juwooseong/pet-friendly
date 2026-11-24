/**
 * 브랜드 캐릭터 그림 (TSK-FE-027, docs/BRAND.md). 투명 배경 WebP, 여백을 잘라낸 크기.
 * width/height는 실제 파일 크기 - 그림이 늦게 뜰 때 화면이 밀리지 않게 img에 그대로 넣는다.
 */
import kongiSniff from './kongi-sniff.webp';
import kongiCurious from './kongi-curious.webp';
import kongiCamera from './kongi-camera.webp';
import kongiLost from './kongi-lost.webp';
import nabiInspect from './nabi-inspect.webp';
import nabiMemo from './nabi-memo.webp';
import nabiReview from './nabi-review.webp';
import kongiFace from './kongi-face.webp';
import nabiFace from './nabi-face.webp';
import duoTogether from './duo-together.webp';

export const CHARACTER_IMAGES = {
  'kongi-sniff': { src: kongiSniff, width: 360, height: 282 },
  'kongi-curious': { src: kongiCurious, width: 236, height: 360 },
  'kongi-camera': { src: kongiCamera, width: 268, height: 360 },
  'kongi-lost': { src: kongiLost, width: 278, height: 360 },
  'nabi-inspect': { src: nabiInspect, width: 290, height: 360 },
  'nabi-memo': { src: nabiMemo, width: 329, height: 360 },
  'nabi-review': { src: nabiReview, width: 332, height: 360 },
  'kongi-face': { src: kongiFace, width: 222, height: 256 },
  'nabi-face': { src: nabiFace, width: 254, height: 256 },
  'duo-together': { src: duoTogether, width: 1200, height: 805 },
} as const;

export type CharacterPose = keyof typeof CHARACTER_IMAGES;
