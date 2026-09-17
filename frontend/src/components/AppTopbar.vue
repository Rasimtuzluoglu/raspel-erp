<template>
  <div
    v-if="goster"
    class="topbar"
  >
    <div class="topbar-sol">
      <AppBreadcrumb v-if="itemsUzun" />
      <span
        v-else
        class="topbar-baslik"
      >{{ baslik }}</span>
    </div>

    <div class="topbar-sag">
      <button
        type="button"
        class="topbar-eylem"
        :title="t('quickSearch.placeholder')"
        :aria-label="t('quickSearch.placeholder')"
        @click="$emit('open-search')"
      >
        <i class="pi pi-search" />
        <kbd>Ctrl K</kbd>
      </button>
      <ThemeSwitcher />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import ThemeSwitcher from './ThemeSwitcher.vue'
import AppBreadcrumb from './AppBreadcrumb.vue'
import { useAuthStore } from '../stores/authStore.js'

defineEmits(['open-search'])

const route = useRoute()
const { t } = useI18n()
const authStore = useAuthStore()

const gizliRotalar = ['/hizli-satis']
const goster = computed(() => !gizliRotalar.includes(route.path))
const itemsUzun = computed(() => route.path.split('/').filter(Boolean).length > 0)

const baslik = computed(() => authStore.sirketAdi || authStore.companyName || 'RasPel ERP')
</script>

<style scoped>
.topbar {
  position: sticky;
  top: 0;
  z-index: var(--z-sticky, 900);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: -20px -24px 16px;
  padding: 10px 24px;
  background: var(--bg-header, rgba(18, 24, 33, 0.82));
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--border);
}
.topbar-sol {
  min-width: 0;
  flex: 1;
  display: flex;
  align-items: center;
}
.topbar-sol :deep(.breadcrumb) {
  padding: 0;
  margin: 0;
  border-bottom: none;
}
.topbar-baslik {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.topbar-sag {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.topbar-eylem {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 34px;
  padding: 0 12px;
  border-radius: var(--radius-md, 10px);
  border: 1px solid var(--border);
  background: var(--bg-muted, rgba(148, 163, 184, 0.08));
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: color var(--dur-fast, 0.15s) var(--ease-standard), border-color var(--dur-fast, 0.15s);
  touch-action: manipulation;
}
.topbar-eylem:hover {
  color: var(--accent);
  border-color: var(--accent-border);
}
.topbar-eylem i {
  font-size: 13px;
}
.topbar-eylem kbd {
  font-family: inherit;
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 5px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  color: var(--text-muted);
}

@media (max-width: 900px) {
  .topbar {
    display: none;
  }
}
</style>
