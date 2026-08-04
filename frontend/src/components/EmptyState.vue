<template>
  <div class="empty-state">
    <i
      :class="icon"
      class="empty-state-icon"
    />
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
.empty-state-sub {
  font-size: 13px;
  color: var(--text-muted, #64748b);
  margin: 6px 0 0;
}
.empty-state-action {
  margin-top: 16px;
}
</style>
