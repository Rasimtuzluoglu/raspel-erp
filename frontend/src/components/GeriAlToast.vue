<template>
  <transition name="undo">
    <div v-if="durum.gorunur" class="geri-al-bar">
      <i class="pi pi-replay"></i>
      <span>{{ durum.metin }}</span>
      <Button label="Geri Al" icon="pi pi-undo" class="p-button-sm p-button-outlined" @click="geriAl" />
      <Button icon="pi pi-times" class="p-button-sm p-button-text" @click="gizle" />
    </div>
  </transition>
</template>

<script setup>
import { useGeriAl } from '../composables/useGeriAl.js'

const { durum, geriAl, gizle } = useGeriAl()
</script>

<style scoped>
.geri-al-bar {
  position: fixed; left: 50%; bottom: 24px; transform: translateX(-50%);
  z-index: 100000; display: flex; align-items: center; gap: 10px;
  background: var(--bg-card); border: 1px solid var(--border);
  border-radius: 12px; padding: 8px 12px; box-shadow: 0 12px 40px rgba(0,0,0,0.4);
  font-size: 13px; color: var(--text-primary);
}
.geri-al-bar > i { color: #3b82f6; }
.undo-enter-active, .undo-leave-active { transition: all 0.25s ease; }
.undo-enter-from, .undo-leave-to { opacity: 0; transform: translateX(-50%) translateY(16px); }
</style>
