<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import apiClient, { extractErrorMessage } from '@/api/apiClient';
import { loadKakaoMaps } from '@/utils/kakaoMap';
import type { ApiResponse, PageResponse } from '@/types/api';
import type { PlaceSearchItem, PlaceSearchParams } from '@/types/place';
import LoadingSpinner from '@/components/common/LoadingSpinner.vue';
import ErrorMessage from '@/components/common/ErrorMessage.vue';

const router = useRouter();

/** 위치 정보를 사용할 수 없을 때의 기본 지도 중심(서울시청) */
const DEFAULT_CENTER = { latitude: 37.5665, longitude: 126.978 };
/** 현재 위치/기본 중심 기준으로 장소를 조회할 반경(km). 기존 `/places/search` API를 그대로 재사용한다. */
const SEARCH_RADIUS_KM = 5;

const mapContainer = ref<HTMLElement | null>(null);
let kakaoSdk: any = null;
let mapInstance: any = null;
let markers: any[] = [];

const sdkLoading = ref(true);
const sdkError = ref('');
const geolocationError = ref('');

const placesLoading = ref(false);
const placesErrorMessage = ref('');
const places = ref<PlaceSearchItem[]>([]);
const selectedPlace = ref<PlaceSearchItem | null>(null);

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

/** 기존 `GET /places/search` API를 재사용해 지도 중심 기준 주변 장소를 조회한다(신규 API 없음). */
const fetchPlaces = async (center: { latitude: number; longitude: number }) => {
  placesLoading.value = true;
  placesErrorMessage.value = '';
  try {
    const params: PlaceSearchParams = {
      latitude: center.latitude,
      longitude: center.longitude,
      radiusKm: SEARCH_RADIUS_KM,
      size: 50,
    };
    const response = await apiClient.get<ApiResponse<PageResponse<PlaceSearchItem>>>('/places/search', { params });
    if (response.data && response.data.success) {
      places.value = response.data.data.content || [];
    }
  } catch (err) {
    placesErrorMessage.value = extractErrorMessage(err, '지도에 표시할 장소를 불러오는 중 오류가 발생했습니다.');
  } finally {
    placesLoading.value = false;
  }
};

/** 기존 마커를 지우고 현재 `places` 목록 기준으로 마커를 새로 그린다. */
const renderMarkers = () => {
  if (!mapInstance || !kakaoSdk) return;

  markers.forEach((marker) => marker.setMap(null));
  markers = [];

  places.value.forEach((place) => {
    if (place.latitude == null || place.longitude == null) return;

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
  router.push({ name: 'PlaceDetail', params: { id: placeId } });
};

onMounted(async () => {
  try {
    kakaoSdk = await loadKakaoMaps();
  } catch (err: any) {
    sdkError.value = err?.message || 'Kakao Maps SDK를 불러오지 못했습니다.';
    sdkLoading.value = false;
    return;
  }
  sdkLoading.value = false;

  const coords = await requestGeolocation();
  const center = coords || DEFAULT_CENTER;

  await nextTick();
  if (mapContainer.value) {
    mapInstance = new kakaoSdk.Map(mapContainer.value, {
      center: new kakaoSdk.LatLng(center.latitude, center.longitude),
      level: 5,
    });
  }

  await fetchPlaces(center);
  renderMarkers();
});
</script>

<template>
  <div class="space-y-4">
    <h1 class="text-2xl font-bold text-stone-800">🗺️ 지도에서 찾기</h1>

    <LoadingSpinner v-if="sdkLoading" text="지도를 불러오는 중..." />

    <ErrorMessage v-else-if="sdkError" :message="sdkError" />

    <template v-else>
      <ErrorMessage v-if="geolocationError" :message="geolocationError" />
      <ErrorMessage v-if="placesErrorMessage" :message="placesErrorMessage" />

      <div class="relative">
        <div ref="mapContainer" class="w-full h-[500px] rounded-2xl border border-stone-100 overflow-hidden bg-stone-100"></div>

        <div v-if="placesLoading" class="absolute inset-0 flex items-center justify-center bg-white/70">
          <LoadingSpinner text="주변 장소를 불러오는 중..." />
        </div>

        <div
          v-if="!placesLoading && !placesErrorMessage && places.length === 0"
          class="absolute top-3 left-1/2 -translate-x-1/2 bg-white shadow-sm border border-stone-100 rounded-xl px-4 py-2 text-sm text-stone-500"
        >
          주변에 표시할 장소가 없습니다.
        </div>

        <div
          v-if="selectedPlace"
          class="absolute bottom-4 left-4 right-4 sm:right-auto sm:w-80 bg-white rounded-2xl shadow-lg border border-stone-100 p-4 space-y-2"
        >
          <button
            type="button"
            class="absolute top-2 right-2 text-stone-400 hover:text-stone-600"
            @click="selectedPlace = null"
          >
            <i class="ri-close-line"></i>
          </button>
          <span class="bg-amber-100 text-amber-700 text-xs font-bold px-2.5 py-1 rounded-lg">
            {{ selectedPlace.categoryName }}
          </span>
          <h3 class="font-bold text-stone-800">{{ selectedPlace.name }}</h3>
          <p class="text-xs text-stone-500">{{ selectedPlace.address }}</p>
          <div class="flex items-center justify-between pt-1">
            <span class="text-amber-500 text-sm font-bold">
              ★ {{ selectedPlace.rating ? selectedPlace.rating.toFixed(1) : '5.0' }}
            </span>
            <button
              type="button"
              class="px-3 py-1.5 rounded-xl text-xs font-bold bg-amber-500 text-white hover:bg-amber-600 transition"
              @click="goToPlaceDetail(selectedPlace.id)"
            >
              상세보기 →
            </button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>
