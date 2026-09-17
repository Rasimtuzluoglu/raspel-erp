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
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '../stores/authStore.js'

const route = useRoute()
const authStore = useAuthStore()
const { t } = useI18n()

const menu = computed(() => {
  if (authStore.isSaha) {
    return [
      { path: '/saha-portali', label: t('mobilMenu.portal'), icon: 'pi pi-compass' },
      { path: '/stoklar', label: t('mobilMenu.stok'), icon: 'pi pi-box' },
      { path: '/sohbet', label: t('mobilMenu.sohbet'), icon: 'pi pi-comments' },
      { path: '/hesap-ayarlari', label: t('mobilMenu.hesap'), icon: 'pi pi-cog' }
    ]
  }
  return [
    { path: '/', label: t('mobilMenu.panel'), icon: 'pi pi-home' },
    { path: '/stoklar', label: t('mobilMenu.stok'), icon: 'pi pi-box' },
    { path: '/faturalar', label: t('mobilMenu.fatura'), icon: 'pi pi-file' },
    { path: '/hesap-ayarlari', label: t('mobilMenu.hesap'), icon: 'pi pi-cog' }
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
    background: var(--bg-card, #141109);
    border-top: 1px solid var(--border);
    z-index: 900;
    box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.25);
  }
}
.mam-item {
  position: relative;
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
  min-width: 0;
  transition: color var(--dur-fast, 0.15s);
  overflow: hidden;
  touch-action: manipulation;
}
.mam-item span {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.mam-item i {
  font-size: 20px;
}
.mam-item.aktif {
  color: var(--accent);
}
.mam-item.aktif::before {
  content: '';
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 24px;
  height: 3px;
  border-radius: 0 0 3px 3px;
  background: var(--accent);
}
.mam-item:active {
  transform: scale(0.95);
}
</style>
