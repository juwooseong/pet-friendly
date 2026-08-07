<script setup lang="ts">
interface Props {
  isOpen: boolean;
  title?: string;
}

defineProps<Props>();
const emit = defineEmits(['close']);
</script>

<template>
  <Teleport to="body">
    <div
      v-if="isOpen"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm transition-opacity"
      @click.self="emit('close')"
    >
      <div class="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-stone-100 flex flex-col gap-4 animate-in fade-in zoom-in duration-200">
        <div class="flex items-center justify-between border-b border-stone-100 pb-3">
          <h3 v-if="title" class="font-bold text-lg text-stone-800">{{ title }}</h3>
          <button @click="emit('close')" class="text-stone-400 hover:text-stone-600 transition">
            <i class="ri-close-line text-xl"></i>
          </button>
        </div>

        <div>
          <slot></slot>
        </div>

        <div v-if="$slots.footer" class="pt-3 border-t border-stone-100 flex justify-end gap-2">
          <slot name="footer"></slot>
        </div>
      </div>
    </div>
  </Teleport>
</template>
