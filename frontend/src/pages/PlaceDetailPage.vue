<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { formatRating } from '@/utils/rating';
import { TOUR_API_SOURCE, imageFitClass, isHttpUrl, kakaoDirectionsUrl, koglLicenseLabel } from '@/utils/publicData';
import { useRoute, useRouter } from 'vue-router';
import apiClient, { extractErrorMessage } from '@/api/apiClient';
import { useAuthStore } from '@/stores/authStore';
import type { ApiResponse } from '@/types/api';
import type { PlaceGalleryImage, PlaceSearchItem } from '@/types/place';
import { useFavoriteStore } from '@/stores/favoriteStore';
import type { Review } from '@/types/review';
import LoadingSpinner from '@/components/common/LoadingSpinner.vue';
import ErrorMessage from '@/components/common/ErrorMessage.vue';
import EmptyState from '@/components/common/EmptyState.vue';
import CharacterIllust from '@/components/common/CharacterIllust.vue';
import CharacterSpeech from '@/components/common/CharacterSpeech.vue';
import PlaceImage from '@/components/common/PlaceImage.vue';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const placeId = route.params.id as string;

const place = ref<PlaceSearchItem | null>(null);
const loading = ref(true);
const notFound = ref(false);
const errorMessage = ref('');
const imageLoadError = ref(false);

/**
 * 반려동물 동반 수칙: allowedSizes, maxWeightLimitKg, isIndoorAllowed(TSK-BE-031), petPolicyDetail.
 * 실외 동반·목줄 자유구역은 근거 데이터가 없어 API에 없다.
 */
const policyDetail = computed(() => place.value?.petPolicyDetail ?? {});

/** 반려동물 동반 조건 배지 (근거가 있는 항목만, null=정보 없음은 표시하지 않음) */
const petRequirementBadges = computed(() => {
  const d = policyDetail.value;
  const badges: string[] = [];
  if (d.accompanyScope === 'ALL_AREAS') badges.push('전 구역 동반 가능');
  if (d.accompanyScope === 'PARTIAL_AREAS') badges.push('일부 구역만 동반 가능');
  if (place.value?.isIndoorAllowed === true) badges.push('실내 동반 가능');
  if (place.value?.isIndoorAllowed === false) badges.push('실내 출입 불가');
  if (d.leashRequired) badges.push('목줄 필수');
  if (d.carrierRequired) badges.push('이동장·유모차 필요');
  if (d.vaccinationRequired) badges.push('예방접종 필수');
  if (d.muzzleRequiredForDangerousDogs) badges.push('맹견 입마개 필수');
  if (d.maxPetCount) badges.push(`최대 ${d.maxPetCount}마리`);
  return badges;
});

/** 반려견 편의시설·품목 (원문) */
const petAmenities = computed(() => {
  const d = policyDetail.value;
  return [
    { label: '구비 시설', value: d.facilities },
    { label: '비치 품목', value: d.furnishedItems },
    { label: '구매 가능', value: d.purchasableItems },
    { label: '대여 가능', value: d.rentalItems },
  ].filter((item): item is { label: string; value: string } => !!item.value);
});

/** 동반 조건 나비 말풍선 (TSK-FE-029) - 정보가 있어도 없어도 "확인부터 해요" */
const policySpeech = computed(() => {
  if (hasPetPolicyInfo.value) return '공식 정보도 바뀔 수 있어요. 가기 전에 한 번 더, 확인부터 해요.';
  if (place.value?.phone) return '동반 조건이 아직 등록되지 않았어요. 가기 전에 전화로 확인부터 해요.';
  return '동반 조건이 아직 등록되지 않았어요. 다녀오시면 리뷰로 알려 주세요.';
});

const hasPetPolicyInfo = computed(
  () =>
    !!place.value &&
    ((place.value.allowedSizes?.length ?? 0) > 0 ||
      place.value.maxWeightLimitKg != null ||
      petRequirementBadges.value.length > 0 ||
      !!policyDetail.value.allowedPetsText ||
      !!policyDetail.value.etcText)
);

/** 사진: 상세 사진(detailImage2)이 있으면 그 목록, 없으면 대표 이미지 1장 */
const selectedImageIndex = ref(0);
const galleryImages = computed<PlaceGalleryImage[]>(() => {
  if (!place.value) return [];
  const gallery = place.value.galleryImages;
  if (gallery && gallery.length > 0) return gallery;
  if (!place.value.imageUrl) return [];
  return [
    {
      url: place.value.imageUrl,
      thumbnailUrl: place.value.imageUrl,
      copyrightType: place.value.imageCopyrightType ?? undefined,
    },
  ];
});
/** 큰 화면에 보여줄 사진. 출처 문구의 공공누리 유형도 이 사진 기준으로 표시한다(사진마다 유형이 다를 수 있음) */
const currentImage = computed(() => galleryImages.value[selectedImageIndex.value] ?? null);
/** 썸네일 클릭 - 이전 사진의 로드 실패 상태를 지워야 새 사진이 다시 표시된다 */
const selectImage = (index: number) => {
  selectedImageIndex.value = index;
  imageLoadError.value = false;
};

/** 한국관광공사(TourAPI) 데이터인 장소에만 출처를 표시한다. 사용자가 직접 등록한 장소는 publicDataId가 없다 */
const isPublicDataPlace = computed(() => !!place.value?.publicDataId);
/** 서버에서 http(s)만 저장하지만, 화면에서도 한 번 더 확인해 javascript: 같은 값이 링크로 걸리지 않게 한다 */
const homepageUrl = computed(() => {
  const homepage = place.value?.detailInfo?.homepage;
  return isHttpUrl(homepage) ? homepage : null;
});
/** 부가 정보 중 값이 있는 항목만 표시 (상세 화면 첫 조회 때 TourAPI에서 채워짐) */
const extraInfoItems = computed(() => {
  const info = place.value?.detailInfo;
  if (!info) return [];
  return [
    { icon: 'ri-parking-box-line', label: '주차', value: info.parking },
    { icon: 'ri-customer-service-2-line', label: '문의', value: info.infoCenter },
    { icon: 'ri-walk-line', label: '유모차', value: info.babyCarriage },
    { icon: 'ri-ticket-2-line', label: '이용 요금', value: info.useFee },
  ].filter((item): item is { icon: string; label: string; value: string } => !!item.value);
});
/** 카카오맵 길찾기 (기존 좌표 숫자 표시를 대체, 별도 API 호출 없음) */
const directionsUrl = computed(() =>
  place.value ? kakaoDirectionsUrl(place.value.name, place.value.latitude, place.value.longitude) : ''
);

const favoriteStore = useFavoriteStore();
const isFavorited = computed(() => authStore.isAuthenticated && favoriteStore.isFavorited(placeId));
const favoriteActionLoading = ref(false);
const favoriteErrorMessage = ref('');

const sizeLabel = (size: string): string => {
  if (size === 'SMALL') return '소형견';
  if (size === 'MEDIUM') return '중형견';
  if (size === 'LARGE') return '대형견';
  return size;
};

const fetchPlaceDetail = async () => {
  loading.value = true;
  notFound.value = false;
  errorMessage.value = '';
  imageLoadError.value = false;
  selectedImageIndex.value = 0;
  try {
    const response = await apiClient.get<ApiResponse<PlaceSearchItem>>(`/places/${placeId}`);
    if (response.data && response.data.success) {
      place.value = response.data.data;
    }
  } catch (err: any) {
    if (err?.response?.status === 404) {
      notFound.value = true;
    } else {
      errorMessage.value = extractErrorMessage(err, '장소 정보를 불러오는 중 오류가 발생했습니다.');
    }
  } finally {
    loading.value = false;
  }
};

/**
 * 현재 장소의 즐겨찾기 여부 (TSK-BE-030: 전체 목록을 받지 않음).
 * 즐겨찾기 화면에서 이미 목록을 받았으면 요청하지 않는다 (TSK-FE-015).
 */
const fetchFavoriteStatus = async () => {
  if (!authStore.isAuthenticated) return;
  try {
    await favoriteStore.ensureStatus(placeId);
  } catch (err) {
    console.error('Failed to fetch favorite status:', err);
  }
};

const toggleFavorite = async () => {
  if (!authStore.isAuthenticated) {
    router.push({ name: 'Login', query: { redirect: route.fullPath } });
    return;
  }
  if (favoriteActionLoading.value) return;

  favoriteActionLoading.value = true;
  favoriteErrorMessage.value = '';
  const wasFavorited = isFavorited.value;
  try {
    if (wasFavorited) {
      await favoriteStore.removeFavorite(placeId);
    } else {
      await favoriteStore.addFavorite(placeId);
    }
  } catch (err) {
    // 요청 실패 시 화면 상태는 요청 이전 값을 그대로 유지한다 (낙관적 갱신을 하지 않으므로 별도 롤백 불필요).
    favoriteErrorMessage.value = extractErrorMessage(
      err,
      wasFavorited ? '즐겨찾기 해제 중 오류가 발생했습니다.' : '즐겨찾기 추가 중 오류가 발생했습니다.'
    );
  } finally {
    favoriteActionLoading.value = false;
  }
};

const reviews = ref<Review[]>([]);
const reviewsLoading = ref(true);
const reviewsErrorMessage = ref('');

const newRating = ref(0);
const newContent = ref('');
const reviewSubmitLoading = ref(false);
/** 이번 방문에서 리뷰를 등록했는지 - 작성 폼 대신 나비의 칭찬을 보여 준다 (TSK-FE-029) */
const reviewSubmitted = ref(false);
const reviewSubmitErrorMessage = ref('');
const reviewFormValidationMessage = ref('');

const formatReviewDate = (createdAt: string): string => {
  const date = new Date(createdAt);
  if (Number.isNaN(date.getTime())) return createdAt;
  return date.toLocaleDateString('ko-KR');
};

const fetchReviews = async () => {
  reviewsLoading.value = true;
  reviewsErrorMessage.value = '';
  try {
    const response = await apiClient.get<ApiResponse<Review[]>>(`/places/${placeId}/reviews`);
    if (response.data && response.data.success) {
      reviews.value = Array.isArray(response.data.data) ? response.data.data : [];
    }
  } catch (err) {
    reviewsErrorMessage.value = extractErrorMessage(err, '리뷰 목록을 불러오는 중 오류가 발생했습니다.');
  } finally {
    reviewsLoading.value = false;
  }
};

const goToLogin = () => {
  router.push({ name: 'Login', query: { redirect: route.fullPath } });
};

const submitReview = async () => {
  reviewFormValidationMessage.value = '';
  reviewSubmitErrorMessage.value = '';

  if (newRating.value < 1 || !newContent.value.trim()) {
    reviewFormValidationMessage.value = '평점과 리뷰 내용을 모두 입력해주세요.';
    return;
  }
  if (reviewSubmitLoading.value) return;

  reviewSubmitLoading.value = true;
  try {
    await apiClient.post('/reviews', {
      placeId,
      rating: newRating.value,
      content: newContent.value.trim(),
    });
    newRating.value = 0;
    newContent.value = '';
    reviewSubmitted.value = true;
    await Promise.all([fetchReviews(), fetchPlaceDetail()]);
  } catch (err) {
    reviewSubmitErrorMessage.value = extractErrorMessage(err, '리뷰 등록 중 오류가 발생했습니다.');
  } finally {
    reviewSubmitLoading.value = false;
  }
};

onMounted(async () => {
  await fetchPlaceDetail();
  await fetchFavoriteStatus();
  await fetchReviews();
});
</script>

<template>
  <div class="space-y-6">
    <LoadingSpinner v-if="loading" text="장소 상세 정보 로딩 중..." />

    <EmptyState
      v-else-if="notFound"
      character="kongi-lost"
      title="장소 정보를 찾을 수 없습니다."
      description="삭제되었거나 잘못된 주소일 수 있습니다."
    />

    <ErrorMessage v-else-if="errorMessage" :message="errorMessage" illustrated />

    <div v-else-if="place" class="bg-white p-8 rounded-3xl border border-stone-100 shadow-sm space-y-4">
      <div class="space-y-2" data-testid="place-gallery">
        <div class="h-56 bg-stone-100 rounded-2xl overflow-hidden">
          <img
            v-if="currentImage && !imageLoadError"
            :src="currentImage.url"
            :alt="place.name"
            :class="['w-full h-full', imageFitClass(currentImage.copyrightType)]"
            data-testid="gallery-main-image"
            @error="imageLoadError = true"
          />
          <!-- 사진이 없거나 깨지면 장소 유형 아이콘과 "사진 준비 중" (TSK-FE-025) -->
          <PlaceImage v-else :alt="place.name" :category="place.category" icon-class="text-5xl" />
        </div>
        <div v-if="galleryImages.length > 1" class="flex gap-2 overflow-x-auto pb-1">
          <button
            v-for="(image, index) in galleryImages"
            :key="image.url"
            type="button"
            :aria-label="`사진 ${index + 1}`"
            class="w-16 h-12 flex-shrink-0 rounded-lg overflow-hidden border-2 transition"
            :class="index === selectedImageIndex ? 'border-amber-500' : 'border-transparent opacity-70 hover:opacity-100'"
            @click="selectImage(index)"
          >
            <img :src="image.thumbnailUrl" :alt="`${place.name} 사진 ${index + 1}`" loading="lazy" :class="['w-full h-full', imageFitClass(image.copyrightType)]" />
          </button>
        </div>
        <p v-if="isPublicDataPlace" class="text-[11px] text-stone-400" data-testid="image-source">
          사진·정보 출처: {{ TOUR_API_SOURCE }} · {{ koglLicenseLabel(currentImage?.copyrightType ?? place.imageCopyrightType) }}
        </p>
      </div>

      <!-- 휴대폰에서는 이름 아래에 즐겨찾기·평점을 한 줄로, 넓은 화면에서는 오른쪽 열로 (TSK-FE-023) -->
      <div class="flex flex-col gap-3 sm:flex-row sm:justify-between sm:items-start">
        <div class="min-w-0">
          <span class="bg-amber-100 text-amber-700 text-xs font-bold px-2.5 py-1 rounded-lg">
            {{ place.categoryName || place.category }}
          </span>
          <h1 class="text-2xl sm:text-3xl font-extrabold text-stone-800 mt-2">{{ place.name }}</h1>
          <p class="text-stone-500 text-sm mt-1">{{ place.address }}</p>
        </div>
        <div class="flex flex-row-reverse items-center justify-between gap-2 shrink-0 sm:flex-col sm:items-end sm:justify-start">
          <button
            type="button"
            @click="toggleFavorite"
            :disabled="favoriteActionLoading"
            class="flex items-center gap-1.5 px-3 py-2 sm:py-1.5 rounded-xl text-sm font-bold border whitespace-nowrap transition disabled:opacity-50 disabled:cursor-not-allowed"
            :class="
              isFavorited
                ? 'bg-rose-50 border-rose-200 text-rose-500 hover:bg-rose-100'
                : 'bg-stone-50 border-stone-200 text-stone-500 hover:bg-stone-100'
            "
          >
            <i :class="isFavorited ? 'ri-heart-fill' : 'ri-heart-line'"></i>
            <span>{{ isFavorited ? '즐겨찾기됨' : '즐겨찾기' }}</span>
          </button>
          <div class="text-amber-500 font-extrabold text-xl sm:text-2xl sm:text-right flex items-baseline gap-2 sm:block">
            <div class="whitespace-nowrap">★ {{ formatRating(place.rating, place.reviewCount) }}</div>
            <div class="text-stone-400 text-xs font-normal sm:mt-0.5 whitespace-nowrap">리뷰 {{ place.reviewCount }}개</div>
          </div>
        </div>
      </div>

      <ErrorMessage v-if="favoriteErrorMessage" :message="favoriteErrorMessage" />

      <div class="pt-4 border-t border-stone-100 space-y-2">
        <h3 class="text-sm font-bold text-stone-700 flex items-center gap-1.5">
          <i class="ri-shield-check-line text-mint-600"></i>
          <span>반려동물 동반 수칙</span>
        </h3>
        <template v-if="hasPetPolicyInfo">
          <div v-if="place.allowedSizes && place.allowedSizes.length > 0" class="flex flex-wrap gap-1.5">
            <span
              v-for="size in place.allowedSizes"
              :key="size"
              class="text-xs font-bold px-2.5 py-1 rounded-full bg-mint-50 text-mint-700 border border-mint-200"
            >
              {{ sizeLabel(size) }}
            </span>
          </div>
          <p v-if="place.maxWeightLimitKg != null" class="text-sm text-stone-600 flex items-center gap-2">
            <i class="ri-scales-3-line text-mint-600"></i>
            <span>최대 체중 {{ place.maxWeightLimitKg }}kg까지 동반 가능</span>
          </p>
          <div v-if="petRequirementBadges.length > 0" class="flex flex-wrap gap-1.5" data-testid="pet-requirements">
            <span
              v-for="badge in petRequirementBadges"
              :key="badge"
              class="text-xs font-bold px-2.5 py-1 rounded-full bg-stone-50 text-stone-600 border border-stone-200"
            >
              {{ badge }}
            </span>
          </div>
          <p v-if="policyDetail.allowedPetsText" class="text-sm text-stone-600 whitespace-pre-line">
            {{ policyDetail.allowedPetsText }}
          </p>
          <p v-if="policyDetail.etcText" class="text-xs text-stone-500 whitespace-pre-line">{{ policyDetail.etcText }}</p>
        </template>
        <p v-else class="text-sm text-stone-400">등록된 반려동물 동반 수칙 정보가 없습니다.</p>
        <CharacterSpeech pose="nabi-inspect" speaker="나비" class="pt-1">{{ policySpeech }}</CharacterSpeech>
      </div>

      <div v-if="place.description" class="pt-4 border-t border-stone-100 space-y-2" data-testid="place-description">
        <h3 class="text-sm font-bold text-stone-700 flex items-center gap-1.5">
          <i class="ri-information-line text-amber-500"></i>
          <span>장소 소개</span>
        </h3>
        <!-- 서버가 HTML을 제거한 평문으로 내려준다. v-html을 쓰지 않고 텍스트로만 렌더링한다(XSS 방지). -->
        <p class="text-sm text-stone-600 leading-relaxed whitespace-pre-line">{{ place.description }}</p>
      </div>

      <div v-if="place.operatingHours" class="pt-4 border-t border-stone-100 space-y-2" data-testid="place-operating-hours">
        <h3 class="text-sm font-bold text-stone-700 flex items-center gap-1.5">
          <i class="ri-time-line text-amber-500"></i>
          <span>운영 시간</span>
        </h3>
        <p class="text-sm text-stone-600 leading-relaxed whitespace-pre-line">{{ place.operatingHours }}</p>
      </div>

      <div v-if="petAmenities.length > 0" class="pt-4 border-t border-stone-100 space-y-2" data-testid="pet-amenities">
        <h3 class="text-sm font-bold text-stone-700 flex items-center gap-1.5">
          <i class="ri-service-line text-amber-500"></i>
          <span>반려견 편의시설</span>
        </h3>
        <dl class="text-sm text-stone-600 space-y-1">
          <div v-for="item in petAmenities" :key="item.label" class="flex gap-2">
            <dt class="font-bold text-stone-500 flex-shrink-0">{{ item.label }}</dt>
            <dd class="whitespace-pre-line">{{ item.value }}</dd>
          </div>
        </dl>
      </div>

      <div
        v-if="extraInfoItems.length > 0 || homepageUrl"
        class="pt-4 border-t border-stone-100 space-y-2 text-sm text-stone-600"
        data-testid="place-extra-info"
      >
        <div v-for="item in extraInfoItems" :key="item.label" class="flex gap-2">
          <i :class="[item.icon, 'text-amber-500']"></i>
          <span class="font-bold text-stone-500 flex-shrink-0">{{ item.label }}</span>
          <span class="whitespace-pre-line">{{ item.value }}</span>
        </div>
        <a
          v-if="homepageUrl"
          :href="homepageUrl"
          target="_blank"
          rel="noopener noreferrer"
          class="inline-flex items-center gap-1.5 py-2 text-amber-600 font-bold hover:underline"
        >
          <i class="ri-global-line"></i>
          <span>홈페이지</span>
        </a>
      </div>

      <div class="pt-4 border-t border-stone-100 grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm text-stone-600">
        <div v-if="place.phone" class="flex items-center gap-2">
          <i class="ri-phone-line text-amber-500"></i>
          <span>{{ place.phone }}</span>
        </div>
        <a
          :href="directionsUrl"
          target="_blank"
          rel="noopener noreferrer"
          class="flex items-center gap-2 py-2 text-amber-600 font-bold hover:underline"
          data-testid="kakao-directions"
        >
          <i class="ri-route-line"></i>
          <span>카카오맵 길찾기</span>
        </a>
      </div>
    </div>

    <div v-if="place" class="bg-white p-8 rounded-3xl border border-stone-100 shadow-sm space-y-6">
      <h2 class="text-xl font-extrabold text-stone-800">리뷰</h2>

      <div
        v-if="reviewSubmitted"
        class="flex items-center gap-4 p-4 rounded-2xl bg-mint-50 border border-mint-200"
        role="status"
        data-testid="review-thanks"
      >
        <CharacterIllust pose="nabi-review" size="sm" class="shrink-0" />
        <div class="text-sm">
          <p class="font-bold text-stone-800">리뷰가 등록됐어요. 고마워요!</p>
          <p class="text-stone-600 mt-0.5">다음에 올 친구들이 헛걸음하지 않게 도와주셨어요.</p>
        </div>
      </div>
      <div v-else-if="authStore.isAuthenticated" class="space-y-3 p-4 rounded-2xl bg-stone-50 border border-stone-100">
        <div class="flex items-center gap-3">
          <CharacterIllust pose="nabi-memo" size="xs" class="shrink-0" />
          <p class="text-xs text-stone-500">크기 제한, 실내 출입 같은 동반 조건도 함께 적어 주시면 다음 친구들에게 큰 도움이 돼요.</p>
        </div>
        <div class="flex items-center gap-1">
          <button
            v-for="star in 5"
            :key="star"
            type="button"
            @click="newRating = star"
            class="text-2xl"
            :class="star <= newRating ? 'text-amber-500' : 'text-stone-300'"
          >
            ★
          </button>
        </div>
        <textarea
          v-model="newContent"
          maxlength="1000"
          rows="3"
          placeholder="이 장소에 대한 리뷰를 남겨주세요."
          class="w-full rounded-xl border border-stone-200 p-3 text-sm text-stone-700 focus:outline-none focus:ring-2 focus:ring-amber-300"
        ></textarea>
        <ErrorMessage v-if="reviewFormValidationMessage" :message="reviewFormValidationMessage" />
        <ErrorMessage v-if="reviewSubmitErrorMessage" :message="reviewSubmitErrorMessage" />
        <button
          type="button"
          @click="submitReview"
          :disabled="reviewSubmitLoading"
          class="px-4 py-2 rounded-xl text-sm font-bold bg-amber-500 text-white hover:bg-amber-600 transition disabled:opacity-50 disabled:cursor-not-allowed"
        >
          리뷰 등록
        </button>
      </div>
      <div v-else class="p-4 rounded-2xl bg-stone-50 border border-stone-100 text-sm text-stone-600 flex items-center justify-between gap-3">
        <span>리뷰를 작성하려면 로그인이 필요합니다.</span>
        <button
          type="button"
          @click="goToLogin"
          class="px-3 py-1.5 rounded-xl text-sm font-bold bg-amber-500 text-white hover:bg-amber-600 transition shrink-0"
        >
          로그인
        </button>
      </div>

      <LoadingSpinner v-if="reviewsLoading" text="리뷰 로딩 중..." />

      <ErrorMessage v-else-if="reviewsErrorMessage" :message="reviewsErrorMessage" />

      <EmptyState
        v-else-if="reviews.length === 0"
        character="nabi-review"
        title="아직 작성된 리뷰가 없습니다."
        description="가장 먼저 리뷰를 남겨보세요!"
      />

      <div v-else class="space-y-4">
        <div
          v-for="review in reviews"
          :key="review.id"
          class="p-4 rounded-2xl border border-stone-100 space-y-1.5"
        >
          <div class="flex items-center justify-between">
            <span class="font-bold text-stone-700 text-sm">{{ review.userNickname }}</span>
            <span class="text-amber-500 text-sm font-bold">{{ '★'.repeat(review.rating) }}</span>
          </div>
          <p class="text-stone-600 text-sm whitespace-pre-wrap">{{ review.content }}</p>
          <p class="text-stone-400 text-xs">{{ formatReviewDate(review.createdAt) }}</p>
        </div>
      </div>
    </div>
  </div>
</template>
