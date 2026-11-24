<script setup lang="ts">
import { ref, nextTick, onMounted, onUnmounted } from 'vue';
import { formatRating } from '@/utils/rating';
import { useRouter } from 'vue-router';
import { loadKakaoMaps } from '@/utils/kakaoMap';
import { debounce } from '@/utils/debounce';
import { clamp, distanceKm, type LatLngPoint } from '@/utils/geo';
import apiClient from '@/api/apiClient';
import type { ApiResponse, PageResponse } from '@/types/api';
import type { PlaceSearchItem, PlaceSearchParams } from '@/types/place';
import LoadingSpinner from '@/components/common/LoadingSpinner.vue';
import ErrorMessage from '@/components/common/ErrorMessage.vue';
import EmptyState from '@/components/common/EmptyState.vue';

/** 위치 정보를 사용할 수 없을 때의 기본 지도 중심(서울시청) */
const DEFAULT_CENTER = { latitude: 37.5665, longitude: 126.978 };

/**
 * 지도에 보이는 영역 크기를 알 수 없을 때 쓰는 기본 조회 반경(km).
 * 보이는 영역을 알 수 있으면 지도 중심 ~ 모서리 거리를 반경으로 쓴다(`readSearchArea`).
 */
const DEFAULT_RADIUS_KM = 5;
/** 조회 반경 하한/상한(km). 많이 축소해도 상한까지만 조회하고, 그 안에서 가까운 순으로 표시한다. */
const MIN_RADIUS_KM = 1;
const MAX_RADIUS_KM = 50;
/** 지도 마커 표시용이므로 페이지네이션 없이 한 번에 조회할 개수(가까운 순, 쇼핑은 서버가 뒤로 보냄 - TSK-FE-020) */
const NEARBY_PAGE_SIZE = 100;
/** 지도 이동/확대·축소가 끝난(`idle`) 뒤 재검색까지 기다리는 시간(ms) */
const RESEARCH_DEBOUNCE_MS = 400;
/** 직전 검색 반경 대비 이 비율 이상 중심이 움직였을 때만 재검색 */
const MIN_MOVE_RATIO = 0.25;
/** 직전 검색 반경 대비 이 비율 이상 반경이 바뀌었을 때(확대·축소)만 재검색 */
const MIN_RADIUS_CHANGE_RATIO = 0.2;

interface SearchArea {
  center: LatLngPoint;
  radiusKm: number;
}

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
/** 현재 영역의 전체 장소 수(표시는 가까운 순 NEARBY_PAGE_SIZE곳까지) */
const totalElements = ref(0);
const selectedPlace = ref<PlaceSearchItem | null>(null);

/** 마지막으로 검색을 요청한 영역. 작은 이동에는 재검색하지 않기 위한 기준 */
let lastSearchArea: SearchArea | null = null;
/** 늦게 도착한 이전 요청의 응답이 최신 결과를 덮어쓰지 않도록 요청마다 증가시키는 번호 */
let latestRequestId = 0;

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
 * 지금 지도에 보이는 영역을 검색 조건(중심 + 반경)으로 바꾼다.
 * 반경은 지도 중심에서 보이는 영역 북동쪽 모서리까지의 거리로, 화면 전체를 덮도록 잡는다.
 * 지도 인스턴스가 중심/영역 정보를 주지 못하면 `fallbackCenter`와 기본 반경을 쓴다.
 */
const readSearchArea = (fallbackCenter: LatLngPoint): SearchArea => {
  const centerLatLng = mapInstance?.getCenter?.();
  const center: LatLngPoint = centerLatLng
    ? { latitude: centerLatLng.getLat(), longitude: centerLatLng.getLng() }
    : fallbackCenter;

  const northEast = mapInstance?.getBounds?.()?.getNorthEast?.();
  if (!northEast) {
    return { center, radiusKm: DEFAULT_RADIUS_KM };
  }
  const radius = distanceKm(center, { latitude: northEast.getLat(), longitude: northEast.getLng() });
  return { center, radiusKm: Math.round(clamp(radius, MIN_RADIUS_KM, MAX_RADIUS_KM) * 100) / 100 };
};

/** 직전 검색 영역과 비교해 다시 검색할 만큼 움직였거나 확대·축소했는지 판단한다. */
const shouldResearch = (area: SearchArea): boolean => {
  if (!lastSearchArea) return true;
  const moved = distanceKm(lastSearchArea.center, area.center);
  const radiusChange = Math.abs(area.radiusKm - lastSearchArea.radiusKm) / lastSearchArea.radiusKm;
  return moved >= lastSearchArea.radiusKm * MIN_MOVE_RATIO || radiusChange >= MIN_RADIUS_CHANGE_RATIO;
};

/**
 * 검색 영역 기준으로 기존 장소 검색 API(`GET /places/search`)를 호출하고 마커를 다시 그린다.
 * 지도 자체 렌더링과는 독립적으로 실패를 처리해, API 오류가 나도 지도와 기존 마커는 유지된다.
 */
const searchPlacesInArea = async (area: SearchArea) => {
  const requestId = ++latestRequestId;
  lastSearchArea = area;
  placesLoading.value = true;
  placesError.value = '';
  try {
    const params: PlaceSearchParams = {
      latitude: area.center.latitude,
      longitude: area.center.longitude,
      radiusKm: area.radiusKm,
      page: 0,
      size: NEARBY_PAGE_SIZE,
    };
    const response = await apiClient.get<ApiResponse<PageResponse<PlaceSearchItem>>>('/places/search', { params });
    if (isUnmounted || requestId !== latestRequestId) return;
    if (response.data?.success) {
      places.value = response.data.data.content || [];
      totalElements.value = response.data.data.totalElements ?? places.value.length;
      renderMarkers();
    }
  } catch (err) {
    if (isUnmounted || requestId !== latestRequestId) return;
    console.error('Failed to fetch nearby places:', err);
    placesError.value = '주변 장소를 불러오는 중 오류가 발생했습니다.';
    // 실패한 영역은 기준으로 남기지 않아, 같은 자리에서 다시 움직이면 재시도된다.
    lastSearchArea = null;
  } finally {
    if (requestId === latestRequestId) {
      placesLoading.value = false;
    }
  }
};

/** 지도 이동/확대·축소가 끝나면(`idle`) 잠시 기다렸다가 충분히 바뀐 경우에만 재검색한다. */
const handleMapIdle = debounce(() => {
  if (isUnmounted || !mapInstance) return;
  const area = readSearchArea(lastSearchArea?.center ?? DEFAULT_CENTER);
  if (!shouldResearch(area)) return;
  void searchPlacesInArea(area);
}, RESEARCH_DEBOUNCE_MS);

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

  await searchPlacesInArea(readSearchArea(center));
  if (isUnmounted || !mapInstance) return;

  // 첫 검색이 끝난 뒤부터 지도 이동/확대·축소에 반응한다. 지도 생성 직후의 idle 이벤트는
  // 첫 검색 영역과 같으므로 shouldResearch에서 걸러진다.
  kakaoSdk.event.addListener(mapInstance, 'idle', handleMapIdle);
});

onUnmounted(() => {
  isUnmounted = true;
  handleMapIdle.cancel();
  if (kakaoSdk && mapInstance) {
    kakaoSdk.event.removeListener?.(mapInstance, 'idle', handleMapIdle);
  }
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

    <ErrorMessage v-else-if="sdkError" :message="sdkError" illustrated />

    <template v-else>
      <ErrorMessage v-if="geolocationError" :message="geolocationError" />
      <ErrorMessage v-if="placesError" :message="placesError" />

      <p class="text-sm text-stone-500 min-h-[1.25rem]" data-testid="map-status">
        <template v-if="placesLoading">
          <i class="ri-loader-4-line animate-spin inline-block align-middle"></i> 이 지역 장소를 찾는 중...
        </template>
        <template v-else-if="places.length > 0">
          이 지역 장소 <strong class="text-stone-700">{{ places.length }}곳</strong>
          <span v-if="totalElements > places.length">
            (전체 {{ totalElements.toLocaleString() }}곳 중 가까운 순, 쇼핑은 뒤로 · 지도를 확대하면 더 자세히 볼 수 있어요)
          </span>
        </template>
        <template v-else-if="!placesError">지도를 움직이면 그 지역의 장소를 다시 찾아요.</template>
      </p>

      <div ref="mapContainer" class="w-full h-[500px] rounded-2xl border border-stone-100 overflow-hidden bg-stone-100"></div>

      <EmptyState
        v-if="!placesLoading && !placesError && places.length === 0"
        character="kongi-curious"
        title="주변에 등록된 장소가 없습니다."
        description="지도를 이동하거나 축소하면 다른 지역의 장소를 볼 수 있어요."
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
