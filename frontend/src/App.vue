<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { useAuthStore } from '@/stores/authStore';
import MainLayout from '@/layouts/MainLayout.vue';
import AuthLayout from '@/layouts/AuthLayout.vue';
import ForcePasswordChangeModal from '@/components/auth/ForcePasswordChangeModal.vue';

const route = useRoute();
const authStore = useAuthStore();

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
  <!-- 강제 비밀번호 변경이 필요한 동안 전 화면을 덮는 레이어. 라우트와 무관하게 항상 최상단에 표시되며 닫을 수 없다. -->
  <ForcePasswordChangeModal v-if="authStore.requiresPasswordChange" />
</template>

<style>
@import "@/css/variables.css";
</style>
