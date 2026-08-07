<script setup lang="ts">
interface Props {
  message: string;
}

defineProps<Props>();
</script>

<template>
  <div class="flex items-center gap-2 p-4 bg-rose-50 border border-rose-200 text-rose-700 text-sm rounded-xl">
    <i class="ri-error-warning-line text-lg flex-shrink-0"></i>
    <span>{{ message }}</span>
  </div>
</template>
