<template>
  <div class="empty-state">
    <div class="empty-state-icon-wrapper">
      <div class="empty-state-glow" />
      <i :class="icon" class="empty-state-icon" />
    </div>
    <p class="empty-state-text">
      {{ message }}
    </p>
    <p
      v-if="subMessage"
      class="empty-state-sub"
    >
      {{ subMessage }}
    </p>
    <Button
      v-if="actionLabel"
      :label="actionLabel"
      :icon="actionIcon"
      class="p-button-sm p-button-outlined empty-state-action"
      @click="$emit('action')"
    />
  </div>
</template>

<script setup>
defineProps({
  message: { type: String, default: 'Henüz kayıt bulunamadı' },
  subMessage: { type: String, default: '' },
  icon: { type: String, default: 'pi pi-inbox' },
  actionLabel: { type: String, default: '' },
  actionIcon: { type: String, default: 'pi pi-plus' }
})
defineEmits(['action'])
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
.empty-state-icon-wrapper {
  position: relative;
  width: 88px;
  height: 88px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}
.empty-state-glow {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(59,130,246,0.18) 0%, rgba(59,130,246,0.06) 55%, transparent 70%);
  animation: empty-pulse 2.5s ease-in-out infinite;
}
.empty-state-icon {
  font-size: 40px;
  color: var(--text-secondary, #64748b);
  position: relative;
  z-index: 1;
  text-shadow: 0 4px 16px rgba(0,0,0,0.15);
}
@keyframes empty-pulse {
  0%, 100% { transform: scale(1); opacity: 0.8; }
  50% { transform: scale(1.08); opacity: 1; }
}
.empty-state-text {
  font-size: 15px;
  color: var(--text-secondary, #94a3b8);
  margin: 0;
}
.empty-state-sub {
  font-size: 13px;
  color: var(--text-muted, #64748b);
  margin: 6px 0 0;
  max-width: 320px;
  line-height: 1.5;
}
.empty-state-action {
  margin-top: 16px;
}
</style>
