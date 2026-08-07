<script setup lang="ts">
import { useAuthStore } from '@/stores/authStore';
import { useRouter } from 'vue-router';

const authStore = useAuthStore();
const router = useRouter();

const handleLogout = () => {
  authStore.logout();
  router.push('/login');
};
</script>

<template>
  <div class="min-h-screen bg-stone-50 text-stone-800 flex flex-col font-sans">
    <!-- Header Navigation -->
    <header class="bg-white border-b border-stone-200 sticky top-0 z-40 shadow-sm">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        <!-- Logo -->
        <router-link to="/" class="flex items-center gap-2 text-amber-500 font-extrabold text-2xl">
          <i class="ri-footprint-fill"></i>
          <span>PetSpot</span>
        </router-link>

        <!-- Main Navigation Links -->
        <nav class="hidden md:flex items-center gap-6 font-medium text-stone-600">
          <router-link to="/" class="hover:text-amber-500 transition" active-class="text-amber-500 font-bold">홈</router-link>
          <router-link to="/search" class="hover:text-amber-500 transition" active-class="text-amber-500 font-bold">장소 탐색</router-link>
          <router-link to="/favorites" class="hover:text-amber-500 transition" active-class="text-amber-500 font-bold">즐겨찾기</router-link>
          <router-link to="/pets" class="hover:text-amber-500 transition" active-class="text-amber-500 font-bold">내 반려동물</router-link>
          <router-link to="/mypage" class="hover:text-amber-500 transition" active-class="text-amber-500 font-bold">마이페이지</router-link>
        </nav>

        <!-- Right User Actions -->
        <div class="flex items-center gap-3">
          <template v-if="authStore.isAuthenticated">
            <span class="text-sm font-semibold text-stone-700 hidden sm:inline">
              {{ authStore.userNickname }}님
            </span>
            <button
              @click="handleLogout"
              class="px-3 py-1.5 text-xs font-semibold text-stone-600 bg-stone-100 hover:bg-stone-200 rounded-lg transition"
            >
              로그아웃
            </button>
          </template>
          <template v-else>
            <router-link
              to="/login"
              class="px-4 py-1.5 text-sm font-semibold text-amber-500 border border-amber-500 hover:bg-amber-50 rounded-lg transition"
            >
              로그인
            </router-link>
            <router-link
              to="/signup"
              class="px-4 py-1.5 text-sm font-semibold text-white bg-amber-500 hover:bg-amber-600 rounded-lg shadow-sm transition"
            >
              회원가입
            </router-link>
          </template>
        </div>
      </div>
    </header>

    <!-- Main Content Area -->
    <main class="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6 lg:p-8">
      <slot></slot>
    </main>

    <!-- Footer -->
    <footer class="bg-white border-t border-stone-200 py-6 text-center text-xs text-stone-500">
      <div class="max-w-7xl mx-auto px-4">
        <p>© 2026 PetSpot. 모든 반려동물의 행복한 동반을 응원합니다.</p>
      </div>
    </footer>
  </div>
</template>
