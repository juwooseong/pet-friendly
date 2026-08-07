<script setup lang="ts">
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
</script>

<template>
  <div class="flex flex-col gap-1.5 w-full">
    <label v-if="label" class="text-xs font-semibold text-stone-700">
      {{ label }}
    </label>
    <input
      :type="type || 'text'"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
      class="px-3.5 py-2.5 bg-stone-50 border rounded-xl text-sm text-stone-800 focus:outline-none focus:ring-2 focus:ring-amber-500 focus:bg-white transition disabled:bg-stone-100 disabled:cursor-not-allowed"
      :class="error ? 'border-rose-400 focus:ring-rose-400' : 'border-stone-200'"
    />
    <span v-if="error" class="text-xs text-rose-500">{{ error }}</span>
  </div>
</template>
