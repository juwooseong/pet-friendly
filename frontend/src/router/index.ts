import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { useAuthStore } from '@/stores/authStore';

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/pages/HomePage.vue'),
    meta: { layout: 'main' },
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/pages/LoginPage.vue'),
    meta: { layout: 'auth' },
  },
  {
    path: '/signup',
    name: 'Signup',
    component: () => import('@/pages/SignupPage.vue'),
    meta: { layout: 'auth' },
  },
  {
    path: '/find-id',
    name: 'FindId',
    component: () => import('@/pages/FindIdPage.vue'),
    meta: { layout: 'auth' },
  },
  {
    path: '/find-password',
    name: 'FindPassword',
    component: () => import('@/pages/FindPasswordPage.vue'),
    meta: { layout: 'auth' },
  },
  {
    path: '/search',
    name: 'Search',
    component: () => import('@/pages/SearchPage.vue'),
    meta: { layout: 'main' },
  },
  {
    path: '/place/:id',
    name: 'PlaceDetail',
    component: () => import('@/pages/PlaceDetailPage.vue'),
    meta: { layout: 'main' },
  },
  {
    path: '/mypage',
    name: 'MyPage',
    component: () => import('@/pages/MyPage.vue'),
    meta: { layout: 'main', requiresAuth: true },
  },
  {
    path: '/pets',
    name: 'Pets',
    component: () => import('@/pages/PetPage.vue'),
    meta: { layout: 'main', requiresAuth: true },
  },
  {
    path: '/favorites',
    name: 'Favorites',
    component: () => import('@/pages/FavoritesPage.vue'),
    meta: { layout: 'main', requiresAuth: true },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/pages/NotFoundPage.vue'),
    meta: { layout: 'auth' },
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

/** 강제 비밀번호 변경 상태에서도 접근을 허용하는 라우트 이름 목록 (로그인/로그아웃은 라우트 이동 없이도 항상 가능) */
const ALLOWED_ROUTE_NAMES_WHILE_PASSWORD_CHANGE_REQUIRED = ['Login'];

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore();

  if (
    authStore.requiresPasswordChange &&
    !ALLOWED_ROUTE_NAMES_WHILE_PASSWORD_CHANGE_REQUIRED.includes(to.name as string)
  ) {
    // 이미 앱 내부에 있었다면(뒤로가기/링크 클릭 등) 이동을 취소해 현재 화면(및 그 위의 강제 변경 레이어)을 유지한다.
    // 새로고침/URL 직접 입력 등 진입점이 없는 경우에만 로그인 화면으로 보낸다.
    if (from.name) {
      next(false);
    } else {
      next({ name: 'Login' });
    }
    return;
  }

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next({ name: 'Login', query: { redirect: to.fullPath } });
  } else {
    next();
  }
});

export default router;
