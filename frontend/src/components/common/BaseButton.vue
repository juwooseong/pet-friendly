<script setup lang="ts">
interface Props {
  variant?: 'primary' | 'secondary' | 'outline' | 'danger';
  size?: 'sm' | 'md' | 'lg';
  loading?: boolean;
  disabled?: boolean;
  type?: 'button' | 'submit' | 'reset';
}

withDefaults(defineProps<Props>(), {
  variant: 'primary',
  size: 'md',
  loading: false,
  disabled: false,
  type: 'button',
});

const variantClasses = {
  primary: 'bg-amber-500 hover:bg-amber-600 text-white shadow-sm',
  secondary: 'bg-stone-100 hover:bg-stone-200 text-stone-700',
  outline: 'border border-amber-500 text-amber-600 hover:bg-amber-50',
  danger: 'bg-rose-500 hover:bg-rose-600 text-white shadow-sm',
};

const sizeClasses = {
  sm: 'px-3 py-1.5 text-xs rounded-lg',
  md: 'px-4 py-2 text-sm rounded-xl',
  lg: 'px-6 py-3 text-base rounded-2xl font-bold',
};
</script>

<template>
  <button
    :type="type"
    :disabled="disabled || loading"
    class="inline-flex items-center justify-center font-medium transition active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed"
    :class="[variantClasses[variant], sizeClasses[size]]"
  >
    <span v-if="loading" class="mr-2 border-2 border-current border-t-transparent rounded-full w-4 h-4 animate-spin"></span>
    <slot></slot>
  </button>
</template>
