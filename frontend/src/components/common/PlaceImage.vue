<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { imageFitClass } from '@/utils/publicData';

/**
 * 장소 사진 (TSK-FE-025). 작은 사진 → 원본 사진 순으로 시도하고, 둘 다 없거나 깨지면 장소 유형 아이콘과
 * "사진 준비 중"을 보여 준다. 부모가 크기를 정한 상자 안을 꽉 채운다.
 *
 * - 관광공사 작은 사진(firstimage2)은 원본이 멀쩡해도 404인 경우가 있다(표본 약 3%) - 원본으로 다시 시도한다.
 * - 숙박의 78%, 음식점·카페의 44%는 관광공사 데이터에 사진이 아예 없다 - 빈 회색 칸 대신 의도된 화면으로 보이게 한다.
 */
const props = withDefaults(
  defineProps<{
    imageUrl?: string | null;
    thumbnailUrl?: string | null;
    /** 작은 사진을 먼저 쓸지 (목록 카드: true, 큰 사진 영역: false) */
    preferThumbnail?: boolean;
    alt: string;
    category?: string | null;
    copyrightType?: string | null;
    /** 사진이 없을 때 아이콘 크기 클래스 */
    iconClass?: string;
    loading?: 'lazy' | 'eager';
  }>(),
  { imageUrl: null, thumbnailUrl: null, preferThumbnail: true, category: null, copyrightType: null, iconClass: 'text-3xl', loading: 'lazy' }
);

const sources = computed(() => {
  const ordered = props.preferThumbnail ? [props.thumbnailUrl, props.imageUrl] : [props.imageUrl, props.thumbnailUrl];
  return [...new Set(ordered.filter((s): s is string => !!s && s.trim() !== ''))];
});

const sourceIndex = ref(0);
watch(sources, () => {
  sourceIndex.value = 0;
});
const currentSrc = computed(() => sources.value[sourceIndex.value] ?? null);

const CATEGORY_ICONS: Record<string, string> = {
  PARK: 'ri-tree-line',
  CAFE: 'ri-cup-line',
  HOTEL: 'ri-hotel-bed-line',
  CULTURE: 'ri-bank-line',
  SHOPPING: 'ri-shopping-bag-3-line',
  FESTIVAL: 'ri-calendar-event-line',
  COURSE: 'ri-route-line',
};
const placeholderIcon = computed(() => CATEGORY_ICONS[props.category ?? ''] ?? 'ri-map-pin-2-line');
</script>

<template>
  <img
    v-if="currentSrc"
    :key="currentSrc"
    :src="currentSrc"
    :alt="alt"
    :loading="loading"
    :class="['w-full h-full', imageFitClass(copyrightType)]"
    @error="sourceIndex++"
  />
  <div
    v-else
    class="w-full h-full flex flex-col items-center justify-center gap-1 bg-gradient-to-br from-amber-50 to-stone-100 text-amber-400"
    data-testid="place-image-placeholder"
  >
    <i :class="[placeholderIcon, iconClass]"></i>
    <span class="text-[11px] font-medium text-stone-400">사진 준비 중</span>
  </div>
</template>
