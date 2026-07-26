<template>
  <div class="page-header">
    <div class="page-header-title">
      <i v-if="icon" :class="icon" class="page-header-icon"></i>
      <h1 class="page-header-heading">{{ title }}</h1>
    </div>
    <p v-if="description" class="page-header-desc">{{ description }}</p>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  icon: { type: String, default: null },
  description: { type: String, default: null }
})
</script>

<style scoped>
.page-header {
  margin-bottom: 24px;
}
.page-header-title {
  display: flex;
  align-items: center;
  gap: 10px;
}
.page-header-icon {
  font-size: 24px;
  color: var(--primary-color, #3b82f6);
}
.page-header-heading {
  font-size: 24px;
  font-weight: 600;
  margin: 0;
  color: var(--text-primary, #f1f5f9);
}
.page-header-desc {
  margin: 6px 0 0 0;
  font-size: 14px;
  color: var(--text-secondary, #94a3b8);
}
</style>
