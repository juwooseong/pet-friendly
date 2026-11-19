<script setup lang="ts">
import { ref, nextTick, onMounted, onUnmounted } from 'vue';
import { formatRating } from '@/utils/rating';
import { useRouter } from 'vue-router';
import { loadKakaoMaps } from '@/utils/kakaoMap';
import apiClient from '@/api/apiClient';
import type { ApiResponse, PageResponse } from '@/types/api';
import type { PlaceSearchItem, PlaceSearchParams } from '@/types/place';
import LoadingSpinner from '@/components/common/LoadingSpinner.vue';
import ErrorMessage from '@/components/common/ErrorMessage.vue';
import EmptyState from '@/components/common/EmptyState.vue';

/** 위치 정보를 사용할 수 없을 때의 기본 지도 중심(서울시청) */
const DEFAULT_CENTER = { latitude: 37.5665, longitude: 126.978 };

/** 지도 중심 기준 주변 장소 조회 반경(km). 기존 장소 검색 API(`/places/search`)의 radiusKm 조건을 그대로 사용한다. */
const NEARBY_RADIUS_KM = 5;
/** 지도 마커 표시용이므로 페이지네이션 없이 한 번에 조회할 최소 페이지 크기 */
const NEARBY_PAGE_SIZE = 20;

const router = useRouter();

const mapContainer = ref<HTMLElement | null>(null);
let kakaoSdk: any = null;
let mapInstance: any = null;
let markers: any[] = [];

const sdkLoading = ref(true);
const sdkError = ref('');
const geolocationError = ref('');

const placesLoading = ref(true);
const placesError = ref('');
const places = ref<PlaceSearchItem[]>([]);
const selectedPlace = ref<PlaceSearchItem | null>(null);

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

/**
 * 지도 중심 좌표 기준으로 기존 장소 검색 API(`GET /places/search`)를 호출해 주변 장소를 조회한다.
 * 지도 자체 렌더링과는 독립적으로 실패를 처리해, API 오류가 나도 지도는 계속 유지된다.
 */
const fetchNearbyPlaces = async (center: { latitude: number; longitude: number }) => {
  placesLoading.value = true;
  placesError.value = '';
  try {
    const params: PlaceSearchParams = {
      latitude: center.latitude,
      longitude: center.longitude,
      radiusKm: NEARBY_RADIUS_KM,
      page: 0,
      size: NEARBY_PAGE_SIZE,
    };
    const response = await apiClient.get<ApiResponse<PageResponse<PlaceSearchItem>>>('/places/search', { params });
    if (response.data?.success) {
      places.value = response.data.data.content || [];
    }
  } catch (err) {
    console.error('Failed to fetch nearby places:', err);
    placesError.value = '주변 장소를 불러오는 중 오류가 발생했습니다.';
  } finally {
    placesLoading.value = false;
  }
};

/** 현재 `places` 목록을 기준으로 Kakao Map 마커를 다시 그린다. */
const renderMarkers = () => {
  markers.forEach((marker) => marker.setMap?.(null));
  markers = [];

  if (!kakaoSdk || !mapInstance) return;

  places.value.forEach((place) => {
    const marker = new kakaoSdk.Marker({
      position: new kakaoSdk.LatLng(place.latitude, place.longitude),
      map: mapInstance,
    });
    kakaoSdk.event.addListener(marker, 'click', () => {
      selectedPlace.value = place;
    });
    markers.push(marker);
  });
};

const goToPlaceDetail = (placeId: string) => {
  router.push(`/place/${placeId}`);
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

  await fetchNearbyPlaces(center);
  if (isUnmounted) return;

  renderMarkers();
});

onUnmounted(() => {
  isUnmounted = true;
  // Kakao Maps는 별도의 destroy API를 제공하지 않으므로, 이후 재초기화/GC가 가능하도록
  // 컴포넌트가 보유한 지도/SDK/마커 참조만 정리한다.
  markers.forEach((marker) => marker.setMap?.(null));
  markers = [];
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
      <ErrorMessage v-if="placesError" :message="placesError" />

      <div ref="mapContainer" class="w-full h-[500px] rounded-2xl border border-stone-100 overflow-hidden bg-stone-100"></div>

      <EmptyState
        v-if="!placesLoading && !placesError && places.length === 0"
        icon="ri-map-pin-line"
        title="주변에 등록된 장소가 없습니다."
      />

      <div
        v-if="selectedPlace"
        class="bg-white p-5 rounded-2xl border border-stone-100 shadow-sm space-y-2"
      >
        <div class="flex items-start justify-between gap-3">
          <div>
            <span class="bg-amber-100 text-amber-700 text-xs font-bold px-2.5 py-1 rounded-lg">
              {{ selectedPlace.category }}
            </span>
            <h3 class="font-bold text-stone-800 text-base mt-1.5">{{ selectedPlace.name }}</h3>
          </div>
          <button
            type="button"
            @click="selectedPlace = null"
            class="text-stone-400 hover:text-stone-600"
            aria-label="닫기"
          >
            <i class="ri-close-line"></i>
          </button>
        </div>
        <div class="flex items-center gap-2 text-sm text-amber-600">
          <span>★ {{ formatRating(selectedPlace.rating, selectedPlace.reviewCount) }}</span>
          <span class="text-stone-400">·</span>
          <span class="text-stone-500">리뷰 {{ selectedPlace.reviewCount }}개</span>
        </div>
        <button
          type="button"
          @click="goToPlaceDetail(selectedPlace.id)"
          class="w-full px-4 py-2 rounded-xl text-sm font-bold bg-amber-500 text-white hover:bg-amber-600 transition"
        >
          상세보기
        </button>
      </div>
    </template>
  </div>
</template>
