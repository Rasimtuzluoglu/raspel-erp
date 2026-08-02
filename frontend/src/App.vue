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
      <transition name="slide-down">
        <div v-if="offlineBannerVisible" class="offline-banner">
          <i class="pi pi-wifi"></i>
          İnternet bağlantınız kesildi. Bağlantı sağlandığında işlemlere kaldığınız yerden devam edebilirsiniz.
          <button class="offline-kapat" @click="networkStatus.showBanner = false">&times;</button>
        </div>
      </transition>
      <ErrorBoundary>
        <router-view />
      </ErrorBoundary>
    </main>

    <QuickSearch :visible="quickSearchVisible" @update:visible="quickSearchVisible = $event" />
    <PasswordChangeModal :visible="sifreDialog" @update:visible="sifreDialog = $event" />
    <Toast position="top-right" :life="5000" />
    <ConfirmDialog />

    <Dialog v-model:visible="oturum.goster" header="Oturum Süresi Dolmak Üzere" :modal="true" :closable="false" style="width: 400px">
      <div class="oturum-uyari">
        <i class="pi pi-exclamation-triangle oturum-ikon"></i>
        <p>Oturumunuz <strong>{{ oturum.kalanSaniye }} saniye</strong> içinde sona erecek.</p>
        <p class="oturum-ipucu">Devam etmek için "Oturumu Uzat" butonuna tıklayın.</p>
      </div>
      <template #footer>
        <Button label="Çıkış Yap" icon="pi pi-sign-out" class="p-button-text" @click="oturum.cikis" />
        <Button label="Oturumu Uzat" icon="pi pi-refresh" class="p-button-primary" @click="oturum.devamEt" />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from './stores/authStore.js'
import { networkStatus } from './api/index.js'
import { useOturumUyarisi } from './composables/useOturumUyarisi.js'
import AppSidebar from './components/AppSidebar.vue'
import PasswordChangeModal from './components/PasswordChangeModal.vue'
import ErrorBoundary from './components/ErrorBoundary.vue'
import QuickSearch from './components/QuickSearch.vue'

const authStore = useAuthStore()
const quickSearchVisible = ref(false)
const sifreDialog = ref(false)
const offlineBannerVisible = computed(() => !networkStatus.online && networkStatus.showBanner)
const oturum = useOturumUyarisi()

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
.offline-banner {
  position: sticky; top: 0; z-index: 999;
  display: flex; align-items: center; gap: 10px;
  background: #fef3c7; color: #92400e;
  padding: 10px 16px; font-size: 13px; font-weight: 500;
  border-bottom: 1px solid #f59e0b;
}
[data-theme="dark"] .offline-banner {
  background: rgba(245,158,11,0.15); color: #fbbf24;
  border-bottom-color: rgba(245,158,11,0.3);
}
.offline-banner i { font-size: 16px; flex-shrink: 0; }
.offline-kapat {
  margin-left: auto; background: none; border: none;
  font-size: 20px; cursor: pointer; color: inherit; opacity: 0.7; padding: 0 4px;
}
.offline-kapat:hover { opacity: 1; }
.slide-down-enter-active, .slide-down-leave-active { transition: all 0.3s ease; }
.slide-down-enter-from, .slide-down-leave-to { transform: translateY(-100%); opacity: 0; }
.oturum-uyari { text-align: center; }
.oturum-ikon { font-size: 2.5rem; color: #f59e0b; margin-bottom: 0.75rem; }
.oturum-uyari p { margin: 0 0 0.5rem; color: var(--text-secondary); }
.oturum-ipucu { font-size: 0.85rem; color: var(--text-muted); }
</style>
