<script setup lang="ts">
/**
 * 사진 크게 보기 팝업 (TSK-FE-032). 사진을 자르지 않고 원본 비율 그대로 화면 가득 보여 준다.
 * - 이전/다음 버튼, 키보드(←/→/Esc), 휴대폰은 옆으로 밀어서 넘기기
 * - 보고 있는 사진 번호는 v-model:index로 부모와 맞춘다(닫으면 상세 화면도 그 사진으로)
 * - 열려 있는 동안 뒤 화면 스크롤을 막고, 닫으면 연 버튼으로 초점을 되돌린다
 */
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue';
import type { PlaceGalleryImage } from '@/types/place';

const props = defineProps<{
  images: PlaceGalleryImage[];
  index: number;
  alt: string;
  /** 사진 아래 작은 글씨(출처 등) */
  caption?: string;
}>();
const emit = defineEmits<{ 'update:index': [value: number]; close: [] }>();

const dialogRef = ref<HTMLElement | null>(null);
const closeButtonRef = ref<HTMLButtonElement | null>(null);
const loadError = ref(false);

const total = computed(() => props.images.length);
const current = computed(() => props.images[props.index] ?? null);
const hasMany = computed(() => total.value > 1);

watch(() => props.index, () => (loadError.value = false));

const go = (step: number) => {
  if (!hasMany.value) return;
  emit('update:index', (props.index + step + total.value) % total.value);
};

/** 다이얼로그 밖으로 초점이 나가지 않게 Tab을 안에서만 돌린다 */
const trapTab = (event: KeyboardEvent) => {
  const focusables = dialogRef.value?.querySelectorAll<HTMLElement>('button:not([disabled])');
  if (!focusables || focusables.length === 0) return;
  const first = focusables[0];
  const last = focusables[focusables.length - 1];
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault();
    last.focus();
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault();
    first.focus();
  }
};

const onKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape') emit('close');
  else if (event.key === 'ArrowLeft') go(-1);
  else if (event.key === 'ArrowRight') go(1);
  else if (event.key === 'Tab') trapTab(event);
};

// 휴대폰: 가로로 50px 넘게 밀면 넘긴다 (세로로 더 많이 움직였으면 무시)
let touchStart: { x: number; y: number } | null = null;
const onTouchStart = (event: TouchEvent) => {
  const t = event.changedTouches[0];
  touchStart = { x: t.clientX, y: t.clientY };
};
const onTouchEnd = (event: TouchEvent) => {
  if (!touchStart) return;
  const t = event.changedTouches[0];
  const dx = t.clientX - touchStart.x;
  const dy = t.clientY - touchStart.y;
  touchStart = null;
  if (Math.abs(dx) > 50 && Math.abs(dx) > Math.abs(dy)) go(dx < 0 ? 1 : -1);
};

let previousFocus: HTMLElement | null = null;
let previousOverflow = '';
onMounted(async () => {
  previousFocus = document.activeElement as HTMLElement | null;
  previousOverflow = document.body.style.overflow;
  document.body.style.overflow = 'hidden';
  window.addEventListener('keydown', onKeydown);
  await nextTick();
  closeButtonRef.value?.focus();
});
onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown);
  document.body.style.overflow = previousOverflow;
  previousFocus?.focus?.();
});
</script>

<template>
  <Teleport to="body">
    <div
      ref="dialogRef"
      role="dialog"
      aria-modal="true"
      :aria-label="`${alt} 사진 크게 보기`"
      class="fixed inset-0 z-50 flex flex-col bg-stone-950 text-white"
      data-testid="photo-viewer"
      @touchstart.passive="onTouchStart"
      @touchend="onTouchEnd"
    >
      <div class="flex items-center justify-between px-4 py-3 shrink-0">
        <p class="text-sm font-semibold tabular-nums" aria-live="polite" data-testid="photo-viewer-counter">
          <template v-if="hasMany">{{ index + 1 }} / {{ total }}</template>
        </p>
        <button
          ref="closeButtonRef"
          type="button"
          aria-label="닫기"
          class="w-10 h-10 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center transition"
          @click="emit('close')"
        >
          <i class="ri-close-line text-2xl" aria-hidden="true"></i>
        </button>
      </div>

      <div class="relative flex-1 min-h-0 flex items-center justify-center px-2 sm:px-16" @click.self="emit('close')">
        <img
          v-if="current && !loadError"
          :key="current.url"
          :src="current.url"
          :alt="hasMany ? `${alt} 사진 ${index + 1}` : alt"
          class="max-w-full max-h-full object-contain select-none"
          data-testid="photo-viewer-image"
          @error="loadError = true"
        />
        <p v-else class="text-sm text-stone-300">사진을 불러오지 못했어요.</p>

        <template v-if="hasMany">
          <button
            type="button"
            aria-label="이전 사진"
            class="absolute left-2 sm:left-4 top-1/2 -translate-y-1/2 w-11 h-11 rounded-full bg-black/50 ring-1 ring-white/20 hover:bg-black/70 flex items-center justify-center transition"
            @click="go(-1)"
          >
            <i class="ri-arrow-left-s-line text-3xl" aria-hidden="true"></i>
          </button>
          <button
            type="button"
            aria-label="다음 사진"
            class="absolute right-2 sm:right-4 top-1/2 -translate-y-1/2 w-11 h-11 rounded-full bg-black/50 ring-1 ring-white/20 hover:bg-black/70 flex items-center justify-center transition"
            @click="go(1)"
          >
            <i class="ri-arrow-right-s-line text-3xl" aria-hidden="true"></i>
          </button>
        </template>
      </div>

      <p v-if="caption" class="shrink-0 px-4 py-3 text-center text-[11px] text-stone-400" data-testid="photo-viewer-caption">
        {{ caption }}
      </p>
    </div>
  </Teleport>
</template>
