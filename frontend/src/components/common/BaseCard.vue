<script setup lang="ts">
interface Props {
  hoverable?: boolean;
}

withDefaults(defineProps<Props>(), {
  hoverable: false,
});
</script>

<template>
  <div
    class="bg-white border border-stone-100 rounded-2xl p-5 shadow-sm transition"
    :class="{ 'hover:shadow-md hover:-translate-y-0.5 cursor-pointer': hoverable }"
  >
    <slot></slot>
  </div>
</template>
