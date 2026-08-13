<script setup lang="ts">
import { ref } from 'vue';

interface Props {
  modelValue: string | number;
  label?: string;
  placeholder?: string;
  type?: string;
  error?: string;
  disabled?: boolean;
}

defineProps<Props>();
defineEmits(['update:modelValue']);

const inputRef = ref<HTMLInputElement | null>(null);
const capsLockOn = ref(false);

const checkCapsLock = (event: KeyboardEvent) => {
  capsLockOn.value = event.getModifierState?.('CapsLock') ?? false;
};

defineExpose({
  focus: () => inputRef.value?.focus(),
});
</script>

<template>
  <div class="flex flex-col gap-1.5 w-full">
    <label v-if="label" class="text-xs font-semibold text-stone-700">
      {{ label }}
    </label>
    <input
      ref="inputRef"
      :type="type || 'text'"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
      @keydown="type === 'password' && checkCapsLock($event)"
      @keyup="type === 'password' && checkCapsLock($event)"
      class="px-3.5 py-2.5 bg-stone-50 border rounded-xl text-sm text-stone-800 focus:outline-none focus:ring-2 focus:ring-amber-500 focus:bg-white transition disabled:bg-stone-100 disabled:cursor-not-allowed"
      :class="error ? 'border-rose-400 focus:ring-rose-400' : 'border-stone-200'"
    />
    <span v-if="type === 'password' && capsLockOn" class="text-xs text-amber-600 flex items-center gap-1">
      ⚠ Caps Lock이 켜져 있습니다.
    </span>
    <span v-if="error" class="text-xs text-rose-500">{{ error }}</span>
  </div>
</template>
