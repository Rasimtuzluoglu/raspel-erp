<template>
  <div v-for="i in count" :key="i" class="skeleton-item">
    <Skeleton :width="width" :height="height" :borderRadius="borderRadius" />
  </div>
</template>

<script setup>
import Skeleton from 'primevue/skeleton'

defineProps({
  width: { type: String, default: '100%' },
  height: { type: String, default: '20px' },
  count: { type: Number, default: 1 },
  borderRadius: { type: String, default: '8px' }
})
</script>

<style scoped>
.skeleton-item + .skeleton-item {
  margin-top: 12px;
}
</style>
