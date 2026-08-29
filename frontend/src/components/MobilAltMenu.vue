<template>
  <nav class="mobil-alt-menu">
    <router-link
      v-for="m in menu"
      :key="m.path"
      :to="m.path"
      class="mam-item"
      :class="{ aktif: aktif(m.path) }"
    >
      <i :class="m.icon" />
      <span>{{ m.label }}</span>
    </router-link>
  </nav>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../stores/authStore.js'

const route = useRoute()
const authStore = useAuthStore()

const menu = computed(() => {
  if (authStore.isSaha) {
    return [
      { path: '/saha-portali', label: 'Portal', icon: 'pi pi-compass' },
      { path: '/stoklar', label: 'Stok', icon: 'pi pi-box' },
      { path: '/sohbet', label: 'Sohbet', icon: 'pi pi-comments' },
      { path: '/hesap-ayarlari', label: 'Hesap', icon: 'pi pi-cog' }
    ]
  }
  return [
    { path: '/', label: 'Panel', icon: 'pi pi-home' },
    { path: '/stoklar', label: 'Stok', icon: 'pi pi-box' },
    { path: '/faturalar', label: 'Fatura', icon: 'pi pi-file' },
    { path: '/hesap-ayarlari', label: 'Hesap', icon: 'pi pi-cog' }
  ]
})

const aktif = (path) => {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}
</script>

<style scoped>
.mobil-alt-menu {
  display: none;
}
@media (max-width: 900px) {
  .mobil-alt-menu {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    height: calc(56px + env(safe-area-inset-bottom));
    padding-bottom: env(safe-area-inset-bottom);
    display: flex;
    align-items: stretch;
    background: var(--bg-card, #0f172a);
    border-top: 1px solid var(--border);
    z-index: 900;
    box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.25);
  }
}
.mam-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  color: var(--text-muted);
  text-decoration: none;
  font-size: 11px;
  font-weight: 600;
  min-height: 48px;
  transition: color 0.15s;
}
.mam-item i {
  font-size: 20px;
}
.mam-item.aktif {
  color: var(--primary-color, #3b82f6);
}
.mam-item:active {
  transform: scale(0.95);
}
</style>
