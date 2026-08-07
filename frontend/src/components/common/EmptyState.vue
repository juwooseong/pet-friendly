<script setup lang="ts">
interface Props {
  icon?: string;
  title: string;
  description?: string;
}

withDefaults(defineProps<Props>(), {
  icon: 'ri-inbox-line',
});
</script>

<template>
  <div class="flex flex-col items-center justify-center p-8 text-center gap-2 border-2 border-dashed border-stone-200 rounded-2xl bg-stone-50/50">
    <i :class="[icon, 'text-4xl text-stone-300']"></i>
    <h4 class="font-bold text-stone-700 text-base">{{ title }}</h4>
    <p v-if="description" class="text-xs text-stone-500 max-w-sm">{{ description }}</p>
    <div v-if="$slots.action" class="mt-2">
      <slot name="action"></slot>
    </div>
  </div>
</template>
