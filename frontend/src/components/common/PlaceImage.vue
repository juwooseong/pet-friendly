<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { imageFitClass } from '@/utils/publicData';
import { CHARACTER_IMAGES, type CharacterPose } from '@/assets/characters';

/**
 * 장소 사진 (TSK-FE-025). 작은 사진 → 원본 사진 순으로 시도하고, 둘 다 없거나 깨지면 캐릭터 그림과
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
    loading?: 'lazy' | 'eager';
  }>(),
  { imageUrl: null, thumbnailUrl: null, preferThumbnail: true, category: null, copyrightType: null, loading: 'lazy' }
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

/**
 * 사진이 없을 때의 캐릭터 (TSK-FE-027, docs/BRAND.md 7장): 카페·음식점·쇼핑은 돋보기로 살펴보는 나비,
 * 숙박·공원·관광지 등 나머지는 카메라를 든 콩이.
 */
const NABI_CATEGORIES = new Set(['CAFE', 'SHOPPING']);
const placeholderPose = computed<CharacterPose>(() =>
  NABI_CATEGORIES.has(props.category ?? '') ? 'nabi-inspect' : 'kongi-camera'
);
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
    class="w-full h-full flex flex-col items-center justify-center gap-1 p-2 bg-linear-to-br from-cream-100 to-amber-50"
    data-testid="place-image-placeholder"
    :data-pose="placeholderPose"
  >
    <!-- 상자 높이에 맞춰 줄어들도록 CharacterIllust 대신 직접 그린다 (목록 96px ~ 상세 224px) -->
    <img
      :src="CHARACTER_IMAGES[placeholderPose].src"
      :width="CHARACTER_IMAGES[placeholderPose].width"
      :height="CHARACTER_IMAGES[placeholderPose].height"
      alt=""
      aria-hidden="true"
      loading="lazy"
      decoding="async"
      draggable="false"
      class="min-h-0 flex-1 max-h-32 w-auto object-contain select-none"
    />
    <span class="shrink-0 text-[11px] font-medium text-stone-500">사진 준비 중</span>
  </div>
</template>
