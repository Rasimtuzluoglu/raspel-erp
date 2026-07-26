<template>
  <div class="empty-state">
    <i :class="icon" class="empty-state-icon"></i>
    <p class="empty-state-text">{{ message }}</p>
  </div>
</template>

<script setup>
defineProps({
  message: { type: String, default: 'Henüz kayıt bulunamadı' },
  icon: { type: String, default: 'pi pi-inbox' }
})
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  text-align: center;
}
.empty-state-icon {
  font-size: 48px;
  color: var(--text-secondary, #64748b);
  margin-bottom: 12px;
}
.empty-state-text {
  font-size: 15px;
  color: var(--text-secondary, #94a3b8);
  margin: 0;
}
</style>
