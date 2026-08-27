<script setup lang="ts">
import { ref, nextTick, onMounted, onUnmounted } from 'vue';
import { loadKakaoMaps } from '@/utils/kakaoMap';
import LoadingSpinner from '@/components/common/LoadingSpinner.vue';
import ErrorMessage from '@/components/common/ErrorMessage.vue';

/** 위치 정보를 사용할 수 없을 때의 기본 지도 중심(서울시청) */
const DEFAULT_CENTER = { latitude: 37.5665, longitude: 126.978 };

const mapContainer = ref<HTMLElement | null>(null);
let kakaoSdk: any = null;
let mapInstance: any = null;

const sdkLoading = ref(true);
const sdkError = ref('');
const geolocationError = ref('');

/**
 * 컴포넌트가 언마운트된 뒤에는 진행 중이던 비동기 초기화(SDK 로드/위치 요청)의 결과를
 * 반영하지 않도록 막는 가드. `onUnmounted`에서 true로 전환한다.
 */
let isUnmounted = false;

/**
 * 브라우저 Geolocation API로 현재 위치를 요청한다. 지원하지 않거나 거부된 경우 null을 반환하고
 * `geolocationError`에 안내 메시지를 남긴다(지도 자체는 기본 중심 좌표로 계속 표시).
 */
const requestGeolocation = (): Promise<{ latitude: number; longitude: number } | null> => {
  return new Promise((resolve) => {
    if (!('geolocation' in navigator)) {
      geolocationError.value = '이 브라우저는 위치 정보를 지원하지 않아 기본 위치를 기준으로 지도를 표시합니다.';
      resolve(null);
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (position) => {
        geolocationError.value = '';
        resolve({ latitude: position.coords.latitude, longitude: position.coords.longitude });
      },
      () => {
        geolocationError.value = '위치 정보 접근이 거부되어 기본 위치를 기준으로 지도를 표시합니다.';
        resolve(null);
      },
      { timeout: 5000 }
    );
  });
};

onMounted(async () => {
  let sdk: any;
  try {
    sdk = await loadKakaoMaps();
  } catch (err: any) {
    if (isUnmounted) return;
    sdkError.value = err?.message || 'Kakao Maps SDK를 불러오지 못했습니다.';
    sdkLoading.value = false;
    return;
  }
  if (isUnmounted) return;

  kakaoSdk = sdk;
  sdkLoading.value = false;

  const coords = await requestGeolocation();
  if (isUnmounted) return;

  const center = coords || DEFAULT_CENTER;

  await nextTick();
  if (isUnmounted) return;

  if (mapContainer.value) {
    mapInstance = new kakaoSdk.Map(mapContainer.value, {
      center: new kakaoSdk.LatLng(center.latitude, center.longitude),
      level: 5,
    });
  }
});

onUnmounted(() => {
  isUnmounted = true;
  // Kakao Maps는 별도의 destroy API를 제공하지 않으므로, 이후 재초기화/GC가 가능하도록
  // 컴포넌트가 보유한 지도/SDK 참조만 정리한다.
  mapInstance = null;
  kakaoSdk = null;
});
</script>

<template>
  <div class="space-y-4">
    <h1 class="text-2xl font-bold text-stone-800">🗺️ 지도에서 찾기</h1>

    <LoadingSpinner v-if="sdkLoading" text="지도를 불러오는 중..." />

    <ErrorMessage v-else-if="sdkError" :message="sdkError" />

    <template v-else>
      <ErrorMessage v-if="geolocationError" :message="geolocationError" />

      <div ref="mapContainer" class="w-full h-[500px] rounded-2xl border border-stone-100 overflow-hidden bg-stone-100"></div>
    </template>
  </div>
</template>
