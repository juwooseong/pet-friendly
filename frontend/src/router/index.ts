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

router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore();
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next({ name: 'Login', query: { redirect: to.fullPath } });
  } else {
    next();
  }
});

export default router;
