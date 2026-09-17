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
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppBreadcrumb from './AppBreadcrumb.vue'
import { useAuthStore } from '../stores/authStore.js'

const route = useRoute()
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

@media (max-width: 900px) {
  .topbar {
    display: none;
  }
}
</style>
