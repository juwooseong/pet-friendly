<script setup lang="ts">
interface Props {
  text?: string;
}

defineProps<Props>();
</script>

<template>
  <div class="flex flex-col items-center justify-center p-8 gap-3 text-amber-500">
    <div class="w-10 h-10 border-4 border-amber-200 border-t-amber-500 rounded-full animate-spin"></div>
    <span v-if="text" class="text-xs font-semibold text-stone-500">{{ text }}</span>
  </div>
</template>
