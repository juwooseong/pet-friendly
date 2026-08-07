<script setup lang="ts">
import EmptyState from '@/components/common/EmptyState.vue';
import BaseButton from '@/components/common/BaseButton.vue';
</script>

<template>
  <div class="min-h-[60vh] flex flex-col items-center justify-center p-4">
    <EmptyState
      icon="ri-error-warning-line"
      title="페이지를 찾을 수 없습니다 (404)"
      description="요청하신 페이지가 존재하지 않거나 이동되었을 수 있습니다."
    >
      <template #action>
        <BaseButton @click="$router.push('/')" variant="primary">
          홈으로 돌아가기
        </BaseButton>
      </template>
    </EmptyState>
  </div>
</template>
