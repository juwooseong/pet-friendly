<script setup lang="ts">
import { useAuthStore } from '@/stores/authStore';
import { useRoute, useRouter } from 'vue-router';
import BrandLogo from '@/components/common/BrandLogo.vue';

const authStore = useAuthStore();
const router = useRouter();
const route = useRoute();

const handleLogout = () => {
  authStore.logout();
  router.push('/login');
};

/**
 * 휴대폰 하단 탭 (TSK-FE-023). 768px 미만에서는 상단 메뉴가 숨겨지므로 이 탭이 유일한 화면 이동 수단이다.
 * 내 반려동물·내 리뷰는 마이페이지 안에서 들어가므로 '마이' 탭이 함께 활성화된다.
 */
const tabs = [
  { to: '/', label: '홈', icon: 'ri-home-5-line', activeIcon: 'ri-home-5-fill', match: (p: string) => p === '/' },
  { to: '/search', label: '탐색', icon: 'ri-search-line', activeIcon: 'ri-search-fill', match: (p: string) => p === '/search' || p.startsWith('/place/') },
  { to: '/map', label: '지도', icon: 'ri-map-2-line', activeIcon: 'ri-map-2-fill', match: (p: string) => p === '/map' },
  { to: '/favorites', label: '즐겨찾기', icon: 'ri-heart-line', activeIcon: 'ri-heart-fill', match: (p: string) => p === '/favorites' },
  { to: '/mypage', label: '마이', icon: 'ri-user-line', activeIcon: 'ri-user-fill', match: (p: string) => p.startsWith('/mypage') || p === '/pets' },
];
</script>

<template>
  <!-- 휴대폰에서는 하단 탭 높이만큼 아래 여백을 둬 마지막 내용·푸터가 탭에 가리지 않게 한다 -->
  <div class="min-h-screen bg-cream-50 text-stone-800 flex flex-col font-sans pb-[calc(4rem+env(safe-area-inset-bottom))] md:pb-0">
    <!-- Header Navigation -->
    <header class="bg-white border-b border-stone-200 sticky top-0 z-40 shadow-sm">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        <!-- Logo -->
        <!-- 헤더 항목은 줄바꿈하지 않는다 - 좁은 화면(320px)에서 "PetSpo/t", "로그/인"처럼 끊기던 문제 (TSK-FE-024) -->
        <router-link to="/" class="flex items-center py-2 shrink-0">
          <BrandLogo />
        </router-link>

        <!-- Main Navigation Links (넓은 화면) -->
        <nav class="hidden md:flex items-center gap-6 font-medium text-stone-600">
          <router-link to="/" class="hover:text-amber-500 transition" active-class="text-amber-500 font-bold">홈</router-link>
          <router-link to="/search" class="hover:text-amber-500 transition" active-class="text-amber-500 font-bold">장소 탐색</router-link>
          <router-link to="/map" class="hover:text-amber-500 transition" active-class="text-amber-500 font-bold">지도</router-link>
          <router-link to="/favorites" class="hover:text-amber-500 transition" active-class="text-amber-500 font-bold">즐겨찾기</router-link>
          <router-link to="/pets" class="hover:text-amber-500 transition" active-class="text-amber-500 font-bold">내 반려동물</router-link>
          <router-link to="/mypage" class="hover:text-amber-500 transition" active-class="text-amber-500 font-bold">마이페이지</router-link>
        </nav>

        <!-- Right User Actions -->
        <div class="flex items-center gap-2 sm:gap-3 shrink-0 whitespace-nowrap">
          <template v-if="authStore.isAuthenticated">
            <span class="text-sm font-semibold text-stone-700 hidden sm:inline">
              {{ authStore.userNickname }}님
            </span>
            <button
              @click="handleLogout"
              class="px-3 py-2 md:py-1.5 text-xs font-semibold text-stone-600 bg-stone-100 hover:bg-stone-200 rounded-lg transition"
            >
              로그아웃
            </button>
          </template>
          <template v-else>
            <router-link
              to="/login"
              class="px-3 sm:px-4 py-2 sm:py-1.5 text-sm font-semibold text-amber-500 border border-amber-500 hover:bg-amber-50 rounded-lg transition"
            >
              로그인
            </router-link>
            <router-link
              to="/signup"
              class="px-3 sm:px-4 py-2 sm:py-1.5 text-sm font-semibold text-white bg-amber-500 hover:bg-amber-600 rounded-lg shadow-sm transition"
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
        <p class="font-semibold text-stone-600">헛걸음 없는 반려동물 여행</p>
        <p class="mt-1">© 2026 PetSpot</p>
      </div>
    </footer>

    <!-- 휴대폰 하단 탭 (768px 미만, TSK-FE-023) -->
    <nav
      aria-label="하단 메뉴"
      class="md:hidden fixed bottom-0 inset-x-0 z-40 bg-white border-t border-stone-200 shadow-[0_-1px_3px_rgba(0,0,0,0.05)] pb-[env(safe-area-inset-bottom)]"
    >
      <ul class="grid grid-cols-5 h-16">
        <li v-for="tab in tabs" :key="tab.to">
          <router-link
            :to="tab.to"
            class="h-full flex flex-col items-center justify-center gap-0.5 text-[11px] font-semibold transition"
            :class="tab.match(route.path) ? 'text-amber-500' : 'text-stone-500'"
            :aria-current="tab.match(route.path) ? 'page' : undefined"
          >
            <i :class="[tab.match(route.path) ? tab.activeIcon : tab.icon, 'text-xl leading-none']"></i>
            <span>{{ tab.label }}</span>
          </router-link>
        </li>
      </ul>
    </nav>
  </div>
</template>
