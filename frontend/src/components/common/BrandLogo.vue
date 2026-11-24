<script setup lang="ts">
/** 브랜드 로고: 민트 지도 핀 + 흰 발자국 + "PetSpot" (TSK-FE-026, docs/BRAND.md 5장) */
interface Props {
  size?: 'md' | 'lg';
}

withDefaults(defineProps<Props>(), {
  size: 'md',
});
</script>

<template>
  <span
    class="inline-flex items-center gap-1.5 whitespace-nowrap font-extrabold tracking-tight"
    :class="size === 'lg' ? 'text-3xl' : 'text-xl sm:text-2xl'"
  >
    <svg
      viewBox="0 0 32 40"
      aria-hidden="true"
      class="shrink-0"
      :class="size === 'lg' ? 'h-10 w-8' : 'h-7 w-[22px] sm:h-8 sm:w-[26px]'"
    >
      <path d="M16 39 C12 34 2 24 2 15 A14 14 0 1 1 30 15 C30 24 20 34 16 39 Z" class="fill-mint-500" />
      <g fill="#fff">
        <ellipse cx="16" cy="19.5" rx="5" ry="4.2" />
        <circle cx="9.8" cy="14" r="2.3" />
        <circle cx="13.6" cy="10.3" r="2.3" />
        <circle cx="18.4" cy="10.3" r="2.3" />
        <circle cx="22.2" cy="14" r="2.3" />
      </g>
    </svg>
    <span><span class="text-stone-800">Pet</span><span class="text-mint-600">Spot</span></span>
  </span>
</template>
