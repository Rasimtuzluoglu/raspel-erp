<template>
  <div class="app-container">
    <template v-if="authStore.isLoggedIn">
      <AppSidebar
        @open-search="quickSearchVisible = true"
        @open-password-modal="sifreDialog = true"
        @open-calculator="hesapMakinesiAcik = true"
        @open-currency="dovizCeviriciAcik = true"
        @open-kdv="kdvAcik = true"
        @open-taksit="taksitAcik = true"
        @open-marj="marjAcik = true"
        @open-iban="ibanAcik = true"
        @open-tc="tcAcik = true"
      />
      <div class="sidebar-spacer" />
    </template>

    <main
      class="main-content"
      :class="{ 'giris-sayfasi': !authStore.isLoggedIn }"
    >
      <transition name="slide-down">
        <div
          v-if="offlineBannerVisible"
          class="offline-banner"
        >
          <i class="pi pi-wifi" />
          <span>Cevrimdisi Mod — Internet baglantisi kesildi. Kayitli veriler gosteriliyor, yeni degisiklikler kaydedilmeyecek.</span>
          <button class="offline-tekrar-dene" @click="window.location.reload()">
            <i class="pi pi-refresh" /> Tekrar Baglan
          </button>
        </div>
      </transition>
      <AppBreadcrumb v-if="authStore.isLoggedIn" />
      <ErrorBoundary>
        <router-view />
      </ErrorBoundary>
    </main>

    <QuickSearch
      :visible="quickSearchVisible"
      @update:visible="quickSearchVisible = $event"
    />
    <PasswordChangeModal
      :visible="sifreDialog"
      @update:visible="sifreDialog = $event"
    />
    <GuncellemeNotlari />
    <GeriAlToast />
    <HesapMakinesi :visible="hesapMakinesiAcik" @update:visible="hesapMakinesiAcik = $event" />
    <DovizCevirici :visible="dovizCeviriciAcik" @update:visible="dovizCeviriciAcik = $event" />
    <KdvHesaplayici :visible="kdvAcik" @update:visible="kdvAcik = $event" />
    <TaksitHesaplayici :visible="taksitAcik" @update:visible="taksitAcik = $event" />
    <KarMarjiHesaplayici :visible="marjAcik" @update:visible="marjAcik = $event" />
    <IbanDogrulayici :visible="ibanAcik" @update:visible="ibanAcik = $event" />
    <TcKimlikDogrulayici :visible="tcAcik" @update:visible="tcAcik = $event" />
    <Toast
      position="top-right"
      :life="5000"
    />
    <ConfirmDialog />

    <Dialog
      v-model:visible="oturum.goster"
      header="Oturum Süresi Dolmak Üzere"
      :modal="true"
      :closable="false"
      style="width: 400px"
    >
      <div class="oturum-uyari">
        <i class="pi pi-exclamation-triangle oturum-ikon" />
        <p>Oturumunuz <strong>{{ oturum.kalanSaniye }} saniye</strong> içinde sona erecek.</p>
        <p class="oturum-ipucu">
          Devam etmek için "Oturumu Uzat" butonuna tıklayın.
        </p>
      </div>
      <template #footer>
        <Button
          label="Çıkış Yap"
          icon="pi pi-sign-out"
          class="p-button-text"
          @click="oturum.cikis"
        />
        <Button
          label="Oturumu Uzat"
          icon="pi pi-refresh"
          class="p-button-primary"
          @click="oturum.devamEt"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useAuthStore } from './stores/authStore.js'
import { networkStatus } from './api/index.js'
import { useOturumUyarisi } from './composables/useOturumUyarisi.js'
import AppSidebar from './components/AppSidebar.vue'
import PasswordChangeModal from './components/PasswordChangeModal.vue'
import ErrorBoundary from './components/ErrorBoundary.vue'
import GuncellemeNotlari from './components/GuncellemeNotlari.vue'
import QuickSearch from './components/QuickSearch.vue'
import GeriAlToast from './components/GeriAlToast.vue'
import AppBreadcrumb from './components/AppBreadcrumb.vue'
import HesapMakinesi from './components/HesapMakinesi.vue'
import DovizCevirici from './components/DovizCevirici.vue'
import KdvHesaplayici from './components/KdvHesaplayici.vue'
import TaksitHesaplayici from './components/TaksitHesaplayici.vue'
import KarMarjiHesaplayici from './components/KarMarjiHesaplayici.vue'
import IbanDogrulayici from './components/IbanDogrulayici.vue'
import TcKimlikDogrulayici from './components/TcKimlikDogrulayici.vue'

const authStore = useAuthStore()
const quickSearchVisible = ref(false)
const sifreDialog = ref(false)
const hesapMakinesiAcik = ref(false)
const dovizCeviriciAcik = ref(false)
const kdvAcik = ref(false)
const taksitAcik = ref(false)
const marjAcik = ref(false)
const ibanAcik = ref(false)
const tcAcik = ref(false)
const offlineBannerVisible = computed(() => !networkStatus.online && networkStatus.showBanner)
const oturum = useOturumUyarisi()
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
.offline-tekrar-dene {
  margin-left: auto; flex-shrink: 0;
  background: #f59e0b; color: white; border: none;
  border-radius: 6px; padding: 6px 14px; font-size: 13px; font-weight: 600;
  cursor: pointer; display: flex; align-items: center; gap: 6px;
}
.offline-tekrar-dene:hover { background: #d97706; }
.slide-down-enter-active, .slide-down-leave-active { transition: all 0.3s ease; }
.slide-down-enter-from, .slide-down-leave-to { transform: translateY(-100%); opacity: 0; }
.oturum-uyari { text-align: center; }
.oturum-ikon { font-size: 2.5rem; color: #f59e0b; margin-bottom: 0.75rem; }
.oturum-uyari p { margin: 0 0 0.5rem; color: var(--text-secondary); }
.oturum-ipucu { font-size: 0.85rem; color: var(--text-muted); }
</style>
