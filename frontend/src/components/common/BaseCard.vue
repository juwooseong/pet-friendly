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
    class="bg-white border border-stone-100 rounded-2xl p-5 shadow-[0_2px_10px_rgba(120,72,20,0.06)] transition"
    :class="{ 'hover:shadow-[0_10px_24px_rgba(120,72,20,0.12)] hover:-translate-y-1 motion-reduce:hover:translate-y-0 cursor-pointer': hoverable }"
  >
    <slot></slot>
  </div>
</template>
