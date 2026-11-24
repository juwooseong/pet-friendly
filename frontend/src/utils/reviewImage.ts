/**
 * 리뷰 사진 올리기 전 준비 (리뷰 사진 1장 첨부).
 * 휴대폰 사진(수 MB)을 긴 변 1600px JPEG로 줄여 올린다. 브라우저가 그릴 때 사진의 회전 정보를 반영하므로
 * 눕혀 찍은 사진도 바로 서고, 새로 그린 그림이라 촬영 위치(GPS) 같은 부가 정보는 따라가지 않는다.
 * 서버(ReviewImageService)도 같은 한도로 다시 줄이고 썸네일을 만든다.
 */

export const REVIEW_IMAGE_MAX_EDGE = 1600;
/** 고를 수 있는 원본 크기 한도 (줄이기 전). 줄인 뒤에는 보통 1MB 미만 */
export const REVIEW_IMAGE_MAX_SOURCE_BYTES = 20 * 1024 * 1024;

/** 고른 파일이 올릴 수 있는 사진인지 확인. 문제가 있으면 안내 문구, 괜찮으면 빈 문자열 */
export function reviewImageProblem(file: File): string {
  if (!file.type.startsWith('image/')) return '사진 파일만 올릴 수 있어요.';
  if (file.size > REVIEW_IMAGE_MAX_SOURCE_BYTES) return '사진이 너무 커요. 20MB 이하 사진을 골라 주세요.';
  return '';
}

async function decode(file: File): Promise<CanvasImageSource & { width: number; height: number }> {
  if (typeof createImageBitmap === 'function') {
    try {
      return await createImageBitmap(file, { imageOrientation: 'from-image' });
    } catch {
      // 일부 브라우저는 옵션을 지원하지 않는다 → 아래 <img>로 읽기
    }
  }
  const url = URL.createObjectURL(file);
  try {
    const img = new Image();
    img.src = url;
    await img.decode();
    return Object.assign(img, { width: img.naturalWidth, height: img.naturalHeight });
  } finally {
    URL.revokeObjectURL(url);
  }
}

/** 긴 변 1600px 이하 JPEG로 줄인다 */
export async function shrinkReviewImage(file: File): Promise<Blob> {
  const source = await decode(file);
  const scale = Math.min(1, REVIEW_IMAGE_MAX_EDGE / Math.max(source.width, source.height));
  const width = Math.max(1, Math.round(source.width * scale));
  const height = Math.max(1, Math.round(source.height * scale));

  const canvas = document.createElement('canvas');
  canvas.width = width;
  canvas.height = height;
  const ctx = canvas.getContext('2d');
  if (!ctx) throw new Error('사진을 준비하지 못했어요.');
  ctx.fillStyle = '#ffffff'; // 투명 배경 PNG는 흰 바탕으로
  ctx.fillRect(0, 0, width, height);
  ctx.imageSmoothingQuality = 'high';
  ctx.drawImage(source, 0, 0, width, height);
  if ('close' in source && typeof source.close === 'function') source.close();

  return new Promise((resolve, reject) =>
    canvas.toBlob((blob) => (blob ? resolve(blob) : reject(new Error('사진을 준비하지 못했어요.'))), 'image/jpeg', 0.85)
  );
}
