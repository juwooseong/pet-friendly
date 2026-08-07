<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import MainLayout from '@/layouts/MainLayout.vue';
import AuthLayout from '@/layouts/AuthLayout.vue';

const route = useRoute();

const layoutComponent = computed(() => {
  if (route.meta.layout === 'auth') {
    return AuthLayout;
  }
  return MainLayout;
});
</script>

<template>
  <component :is="layoutComponent">
    <router-view />
  </component>
</template>

<style>
@import "@/css/variables.css";
</style>
