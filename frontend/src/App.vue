<template>
  <div class="app-container">
    <template v-if="authStore.isLoggedIn">
      <AppSidebar
        @open-search="quickSearchVisible = true"
        @open-password-modal="sifreDialog = true"
      />
      <div class="sidebar-spacer"></div>
    </template>

    <main class="main-content" :class="{ 'giris-sayfasi': !authStore.isLoggedIn }">
      <ErrorBoundary>
        <router-view />
      </ErrorBoundary>
    </main>

    <QuickSearch :visible="quickSearchVisible" @update:visible="quickSearchVisible = $event" />
    <PasswordChangeModal :visible="sifreDialog" @update:visible="sifreDialog = $event" />
    <Toast position="top-right" :life="5000" />
    <ConfirmDialog />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from './stores/authStore.js'
import AppSidebar from './components/AppSidebar.vue'
import PasswordChangeModal from './components/PasswordChangeModal.vue'
import ErrorBoundary from './components/ErrorBoundary.vue'
import QuickSearch from './components/QuickSearch.vue'

const authStore = useAuthStore()
const quickSearchVisible = ref(false)
const sifreDialog = ref(false)

const kisaYolHandler = (e) => {
  if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
    e.preventDefault()
    quickSearchVisible.value = !quickSearchVisible.value
  }
}

onMounted(() => {
  document.addEventListener('keydown', kisaYolHandler)
})

onUnmounted(() => {
  document.removeEventListener('keydown', kisaYolHandler)
})
</script>

<style>
/* Main layout container styles */
</style>
