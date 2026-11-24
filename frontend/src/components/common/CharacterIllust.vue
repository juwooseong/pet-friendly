<script setup lang="ts">
import { computed } from 'vue';
import { CHARACTER_IMAGES, type CharacterPose } from '@/assets/characters';

/**
 * 캐릭터 그림 (TSK-FE-027). 콩이·나비는 비었거나·기다리거나·칭찬할 때만 쓴다(docs/BRAND.md 7장).
 * 옆의 글이 의미를 전하므로 그림은 장식으로 두고 화면 읽기 프로그램이 건너뛰게 한다.
 */
const props = withDefaults(
  defineProps<{
    pose: CharacterPose;
    /** 높이 기준 크기 (xs 48px · sm 72px · md 112px · lg 160px) */
    size?: 'xs' | 'sm' | 'md' | 'lg';
    loading?: 'lazy' | 'eager';
  }>(),
  { size: 'md', loading: 'lazy' }
);

const image = computed(() => CHARACTER_IMAGES[props.pose]);

const SIZE_CLASSES = {
  xs: 'h-12',
  sm: 'h-[72px]',
  md: 'h-28',
  lg: 'h-40',
} as const;
</script>

<template>
  <img
    :src="image.src"
    :width="image.width"
    :height="image.height"
    alt=""
    aria-hidden="true"
    :loading="loading"
    decoding="async"
    draggable="false"
    :class="['w-auto max-w-full object-contain select-none', SIZE_CLASSES[size]]"
    :data-pose="pose"
  />
</template>
