<script setup lang="ts">
import { onMounted } from 'vue';
import { useAuthStore } from '@/stores/authStore';
import { usePetStore } from '@/stores/petStore';

const authStore = useAuthStore();
const petStore = usePetStore();

onMounted(() => {
  // Initialize mock demo user session for Sprint 0 verification
  authStore.setUser({
    id: 'user-demo',
    email: 'petlover@petspot.io',
    nickname: '김집사',
    avatarUrl: '🐶',
    pets: [
      {
        id: 'pet-1',
        userId: 'user-demo',
        name: '초코',
        species: 'DOG',
        breed: '토이 푸들',
        weightKg: 4.2,
        sizeCategory: 'SMALL',
        ageYears: 3,
        isVaccinated: true,
        photoUrl: 'https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?auto=format&fit=crop&w=300&q=80'
      },
      {
        id: 'pet-2',
        userId: 'user-demo',
        name: '빅터',
        species: 'DOG',
        breed: '골든 리트리버',
        weightKg: 28.5,
        sizeCategory: 'LARGE',
        ageYears: 5,
        isVaccinated: true,
        photoUrl: 'https://images.unsplash.com/photo-1552053831-71594a27632d?auto=format&fit=crop&w=300&q=80'
      }
    ]
  });

  petStore.setPets(authStore.currentUser?.pets || []);
});
</script>

<template>
  <div id="app-wrapper">
    <header class="header">
      <div class="logo">
        <span class="logo-icon">🐾</span>
        <span class="logo-title">PetSpot</span>
        <span class="badge">Vue 3 + Spring Boot 3</span>
      </div>

      <div class="user-chip" v-if="petStore.activePet">
        <span class="pet-avatar">🐶</span>
        <span>{{ petStore.activePet?.name }} ({{ petStore.activePet?.weightKg }}kg)</span>
      </div>
    </header>

    <main class="content">
      <div class="card">
        <h2>🚀 Sprint 0 Scaffold Setup Complete</h2>
        <p>PostgreSQL 15 + PostGIS Docker Compose, Flyway V1 Migration, Spring Boot 3 Security/JWT & Vue 3 + TypeScript Pinia Scaffold가 정상 가동 준비되었습니다.</p>
      </div>
    </main>
  </div>
</template>

<style scoped>
#app-wrapper {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: var(--bg-main);
  color: var(--text-main);
  font-family: var(--font-family);
}

.header {
  height: 64px;
  background: var(--bg-surface);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 1.5rem;
}

.logo {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 800;
  font-size: 1.25rem;
}

.badge {
  font-size: 0.7rem;
  padding: 2px 8px;
  background: var(--primary-100);
  color: var(--primary-700);
  border-radius: var(--radius-full);
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  background: var(--primary-50);
  border: 1px solid var(--primary-400);
  padding: 4px 12px;
  border-radius: var(--radius-full);
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--primary-700);
}

.content {
  padding: 2rem;
  display: flex;
  justify-content: center;
}

.card {
  background: var(--bg-surface);
  border: 1px solid var(--border-color);
  padding: 2rem;
  border-radius: var(--radius-lg);
  max-width: 600px;
  box-shadow: var(--shadow-md);
}
</style>
