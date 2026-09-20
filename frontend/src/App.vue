<template>
  <div class="app-container">
    <a
      href="#main"
      class="skip-link"
    >{{ $t('common.skipToContent') }}</a>
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
      id="main"
      class="main-content"
      :class="{ 'giris-sayfasi': !authStore.isLoggedIn }"
    >
      <transition name="slide-down">
        <div
          v-if="offlineBannerVisible"
          class="offline-banner"
        >
          <i class="pi pi-wifi" />
          <span>Cevrimdisi Mod — Internet baglantisi kesildi. Kayitli veriler gosteriliyor, yeni degisiklikler
            kaydedilmeyecek.</span>
          <button
            class="offline-tekrar-dene"
            @click="window.location.reload()"
          >
            <i class="pi pi-refresh" /> Tekrar Baglan
          </button>
        </div>
      </transition>
      <transition name="slide-down">
        <div
          v-if="authStore.isLoggedIn && sunumAktif"
          class="sunum-banner"
        >
          <i class="pi pi-eye-slash" />
          <span class="sunum-banner-metin">{{ $t('sunumModu.serit') }}</span>
          <span class="sunum-banner-maske">
            <button
              type="button"
              :class="{ aktif: sunumMaske === 'bulanik' }"
              @click="sunumMaskeAyarla('bulanik')"
            >
              {{ $t('sunumModu.bulanik') }}
            </button>
            <button
              type="button"
              :class="{ aktif: sunumMaske === 'gizle' }"
              @click="sunumMaskeAyarla('gizle')"
            >
              {{ $t('sunumModu.gizle') }}
            </button>
          </span>
          <button
            type="button"
            class="sunum-banner-kapat"
            @click="sunumDegistir()"
          >
            <i class="pi pi-times" /> {{ $t('sunumModu.kapat') }}
          </button>
        </div>
      </transition>
      <AppBreadcrumb v-if="authStore.isLoggedIn" />
      <ErrorBoundary>
        <router-view v-slot="{ Component }">
          <transition
            name="sayfa-gecis"
            mode="out-in"
          >
            <component :is="Component" />
          </transition>
        </router-view>
      </ErrorBoundary>
    </main>

    <QuickSearch
      :visible="quickSearchVisible"
      @update:visible="quickSearchVisible = $event"
    />
    <PasswordChangeModal
      v-if="sifreDialog"
      :visible="sifreDialog"
      @update:visible="sifreDialog = $event"
    />
    <GuncellemeNotlari v-if="authStore.isLoggedIn" />
    <GeriAlToast />
    <KisayolRehberi />
    <HesapMakinesi
      v-if="hesapMakinesiAcik"
      :visible="hesapMakinesiAcik"
      @update:visible="hesapMakinesiAcik = $event"
    />
    <DovizCevirici
      v-if="dovizCeviriciAcik"
      :visible="dovizCeviriciAcik"
      @update:visible="dovizCeviriciAcik = $event"
    />
    <KdvHesaplayici
      v-if="kdvAcik"
      :visible="kdvAcik"
      @update:visible="kdvAcik = $event"
    />
    <TaksitHesaplayici
      v-if="taksitAcik"
      :visible="taksitAcik"
      @update:visible="taksitAcik = $event"
    />
    <KarMarjiHesaplayici
      v-if="marjAcik"
      :visible="marjAcik"
      @update:visible="marjAcik = $event"
    />
    <IbanDogrulayici
      v-if="ibanAcik"
      :visible="ibanAcik"
      @update:visible="ibanAcik = $event"
    />
    <TcKimlikDogrulayici
      v-if="tcAcik"
      :visible="tcAcik"
      @update:visible="tcAcik = $event"
    />
    <Toast
      position="top-right"
      :life="5000"
    />
    <ConfirmDialog />

    <MobilAltMenu v-if="altMenuGoster" />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, defineAsyncComponent } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from './stores/authStore.js'
import { networkStatus } from './api/index.js'
import { useSunumModu } from './composables/useSunumModu.js'
import { useToast } from 'primevue/usetoast'
import { useMagicKeys } from '@vueuse/core'

import AppSidebar from './components/AppSidebar.vue'
import ErrorBoundary from './components/ErrorBoundary.vue'
import GuncellemeNotlari from './components/GuncellemeNotlari.vue'
import QuickSearch from './components/QuickSearch.vue'
import GeriAlToast from './components/GeriAlToast.vue'
import AppBreadcrumb from './components/AppBreadcrumb.vue'
import MobilAltMenu from './components/MobilAltMenu.vue'
import KisayolRehberi from './components/KisayolRehberi.vue'

// Nadiren kullanilan araclar/modallar: yalnizca acildiklarinda yuklenir
// (entry chunk kuculur, ilk yukleme hizlanir).
const HesapMakinesi = defineAsyncComponent(() => import('./components/HesapMakinesi.vue'))
const DovizCevirici = defineAsyncComponent(() => import('./components/DovizCevirici.vue'))
const KdvHesaplayici = defineAsyncComponent(() => import('./components/KdvHesaplayici.vue'))
const TaksitHesaplayici = defineAsyncComponent(() => import('./components/TaksitHesaplayici.vue'))
const KarMarjiHesaplayici = defineAsyncComponent(() => import('./components/KarMarjiHesaplayici.vue'))
const IbanDogrulayici = defineAsyncComponent(() => import('./components/IbanDogrulayici.vue'))
const TcKimlikDogrulayici = defineAsyncComponent(() => import('./components/TcKimlikDogrulayici.vue'))
const PasswordChangeModal = defineAsyncComponent(() => import('./components/PasswordChangeModal.vue'))

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()
// POS/hizli satis tam ekran calisma alanidir; sabit alt menü fiş önizleme ve
// yazdırma butonlarinin uzerine biniyor. Bu route'ta alt menüyü gostermeyiz.
const altMenuGoster = computed(() => authStore.isLoggedIn && !route.path.startsWith('/hizli-satis'))
const toast = useToast()
const { aktif: sunumAktif, maske: sunumMaske, degistir: sunumDegistir, maskeAyarla: sunumMaskeAyarla } = useSunumModu()
const { ctrl_k, cmd_k, escape } = useMagicKeys()

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

// Klavye Kisayollari (Ctrl+K / Cmd+K aramayi acar)
watch([ctrl_k, cmd_k], ([ctrl, cmd]) => {
  if ((ctrl || cmd) && authStore.isLoggedIn) {
    quickSearchVisible.value = true
  }
})

// Esc tusu ile acik olan tum araclari kapatir
watch(escape, (v) => {
  if (v) {
    quickSearchVisible.value = false
    hesapMakinesiAcik.value = false
    dovizCeviriciAcik.value = false
    kdvAcik.value = false
    taksitAcik.value = false
    marjAcik.value = false
    ibanAcik.value = false
    tcAcik.value = false
  }
})

// Global API Hata Bildirimleri
const handleApiError = (e) => {
  toast.add({
    severity: 'error',
    summary: 'Islem Basarisiz',
    detail: e.detail?.message || 'Bilinmeyen bir hata olustu',
    life: 5000
  })
}

const handleGlobalShortcuts = (e) => {
  if (!authStore.isLoggedIn) return
  // Sunum (musteri) modu: Ctrl/⌘ + Shift + H
  if ((e.ctrlKey || e.metaKey) && e.shiftKey && e.key.toLowerCase() === 'h') {
    e.preventDefault()
    sunumDegistir()
    return
  }
  // Sayfa ozel kisayol (useKisayollar) islediyse global fallback'i tetikleme
  if (e.defaultPrevented) return
  if (e.key === 'F2') {
    e.preventDefault()
    router.push({ name: 'HizliSatis' })
  } else if (e.key === 'F4') {
    e.preventDefault()
    router.push({ name: 'Stoklar' })
  }
}

onMounted(() => {
  window.addEventListener('api-error', handleApiError)
  window.addEventListener('keydown', handleGlobalShortcuts)
})

onUnmounted(() => {
  window.removeEventListener('api-error', handleApiError)
  window.removeEventListener('keydown', handleGlobalShortcuts)
})

const sirketRenkPaletleri = [
  { accent: '#0f766e', accentHover: '#0d9488' },
  { accent: '#3b82f6', accentHover: '#2563eb' },
  { accent: '#8b5cf6', accentHover: '#7c3aed' },
  { accent: '#f59e0b', accentHover: '#d97706' },
  { accent: '#ec4899', accentHover: '#db2777' },
  { accent: '#06b6d4', accentHover: '#0891b2' }
]

const sirketTemasiniUygula = (sirketId) => {
  if (sirketId == null) return
  // Kullanıcı kendi vurgu rengini seçtiyse şirket paleti onu ezmesin.
  if (localStorage.getItem('raspel_primary_color')) return
  const palet = sirketRenkPaletleri[Number(sirketId) % sirketRenkPaletleri.length]
  const root = document.documentElement
  root.style.setProperty('--accent', palet.accent)
  root.style.setProperty('--accent-hover', palet.accentHover)
}

watch(
  () => authStore.sirketId,
  (yeni) => {
    sirketTemasiniUygula(yeni)
  },
  { immediate: true }
)
</script>

<style>
.offline-banner {
  position: sticky;
  top: 0;
  z-index: 999;
  display: flex;
  align-items: center;
  gap: 10px;
  background: #fef3c7;
  color: #92400e;
  padding: 10px 16px;
  font-size: 13px;
  font-weight: 500;
  border-bottom: 1px solid #f59e0b;
  border-radius: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
[data-theme='dark'] .offline-banner {
  background: rgba(245, 158, 11, 0.15);
  color: #fbbf24;
  border-bottom-color: rgba(245, 158, 11, 0.3);
}
.offline-banner i {
  font-size: 16px;
  flex-shrink: 0;
}
.offline-banner span {
  flex: 1;
  min-width: 0;
}
.offline-tekrar-dene {
  margin-left: auto;
  flex-shrink: 0;
  background: #f59e0b;
  color: white;
  border: none;
  border-radius: 6px;
  padding: 6px 14px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
}
.offline-tekrar-dene:hover {
  background: #d97706;
}
@media (max-width: 900px) {
  /* Mobilde ust bar sabit konumda; cevrimdisi banner onun altinda kalsin */
  .offline-banner {
    top: 56px;
    z-index: 1001;
  }
}
.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s ease;
}
.slide-down-enter-from,
.slide-down-leave-to {
  transform: translateY(-100%);
  opacity: 0;
}
.sayfa-gecis-enter-active,
.sayfa-gecis-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}
.sayfa-gecis-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.sayfa-gecis-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
.oturum-uyari {
  text-align: center;
}
.oturum-ikon {
  font-size: 2.5rem;
  color: #f59e0b;
  margin-bottom: 0.75rem;
}
.oturum-uyari p {
  margin: 0 0 0.5rem;
  color: var(--text-secondary);
}
.oturum-ipucu {
  font-size: 0.85rem;
  color: var(--text-muted);
}
</style>
