<template>
  <div
    class="giris-sayfasi"
    :class="{ 'pwa-modu': pwaModu }"
  >
    <!-- Sakin, statik sahne: CSS mesh + ince grid (blur/canvas yok) -->
    <div
      class="giris-sahne"
      aria-hidden="true"
    >
      <span class="giris-mesh" />
      <span class="giris-grid" />
    </div>

    <!-- Marka açılışı: her ziyarette, atlanabilir -->
    <GirisAcilis
      v-if="acilisAktif"
      @bitti="acilisBitti"
    />

    <div
      class="giris-yerlesim"
      :inert="acilisAktif || undefined"
    >
      <!-- Masaüstü: statik marka paneli -->
      <GirisMarkaPaneli class="giris-marka-alani" />

      <main class="giris-icerik">
        <div
          class="giris-tema-secim"
          role="group"
          :aria-label="t('theme.mode')"
        >
          <button
            type="button"
            class="giris-tema-btn"
            :class="{ 'giris-tema-btn--aktif': !isDark }"
            data-test="giris-tema-light"
            :aria-label="t('theme.light')"
            :title="t('theme.light')"
            :aria-pressed="!isDark ? 'true' : 'false'"
            @click="applyMode('light')"
          >
            <i
              class="pi pi-sun"
              aria-hidden="true"
            />
            <span>{{ t('theme.lightShort') }}</span>
          </button>
          <button
            type="button"
            class="giris-tema-btn"
            :class="{ 'giris-tema-btn--aktif': isDark }"
            data-test="giris-tema-dark"
            :aria-label="t('theme.dark')"
            :title="t('theme.dark')"
            :aria-pressed="isDark ? 'true' : 'false'"
            @click="applyMode('dark')"
          >
            <i
              class="pi pi-moon"
              aria-hidden="true"
            />
            <span>{{ t('theme.darkShort') }}</span>
          </button>
        </div>

        <!-- Mobil/tablet: kompakt marka -->
        <div class="giris-mobil-marka">
          <img
            :src="markaLogo"
            alt=""
            class="giris-mobil-logo"
            width="38"
            height="38"
          >
          <div class="giris-mobil-metin">
            <span class="giris-mobil-ad">RasPel ERP</span>
            <p class="giris-mobil-vaat">
              {{ t('giris.mobileTagline') }}
            </p>
          </div>
        </div>

        <GirisFormKarti
          :sirket-logo="sirketLogo"
          :son-sirket-ad="sonSirketAd"
          @giris-basarili="girisBasarili"
        />

        <GirisAltBilgi />
      </main>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '../stores/authStore.js'
import { useTheme } from '../composables/useTheme.js'
import GirisAcilis from '../components/GirisAcilis.vue'
import GirisMarkaPaneli from '../components/GirisMarkaPaneli.vue'
import GirisFormKarti from '../components/GirisFormKarti.vue'
import GirisAltBilgi from '../components/GirisAltBilgi.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const { isDark, applyMode } = useTheme()
const { t } = useI18n()

const SW_YENILEME_ANAHTARI = 'raspel_sw_yenileme'
const swYenilemesi = (() => {
  try {
    const ham = sessionStorage.getItem(SW_YENILEME_ANAHTARI)
    if (ham) sessionStorage.removeItem(SW_YENILEME_ANAHTARI)
    if (!ham) return false
    if (ham === '1') return true
    const zaman = Number(ham)
    return Number.isFinite(zaman) && Date.now() - zaman < 15000
  } catch {
    return false
  }
})()

const acilisAktif = ref(!swYenilemesi)
const pwaModu = ref(false)
const sirketLogo = ref('')
const sonSirketAd = ref('')
const markaLogo = '/logo-icon.png'

const acilisBitti = async () => {
  acilisAktif.value = false
  await nextTick()
  try {
    const alan = document.querySelector('[data-test="giris-username"]')
    if (alan && !alan.value) alan.focus()
  } catch {
    /* yoksay */
  }
}

const hedefYol = () => {
  const hedef = route.query.redirect
  if (typeof hedef === 'string' && hedef.startsWith('/') && !hedef.startsWith('//')) {
    return hedef
  }
  return '/'
}

const girisBasarili = async () => {
  await router.push(hedefYol())
}

onMounted(() => {
  // PWA (yüklenmiş uygulama) modunda mı? Safe-area boşlukları buna göre ayarlanır.
  try {
    pwaModu.value = window.matchMedia('(display-mode: standalone)').matches
      || window.matchMedia('(display-mode: fullscreen)').matches
      || window.navigator.standalone === true
  } catch {
    pwaModu.value = false
  }

  sonSirketAd.value = localStorage.getItem('raspel_erp_son_sirket_ad') || ''
  sirketLogo.value = localStorage.getItem('raspel_erp_son_sirket_logo') || ''

  if (authStore.isLoggedIn) {
    acilisAktif.value = false
    router.push(hedefYol())
    return
  }

  // Giriş sonrası geçişi hızlandırmak için Dashboard chunk'ı boş zamanda önden
  // yüklenir. Veri tasarrufu modunda veya yavaş bağlantıda atlanır.
  try {
    const tasarruf = navigator.connection && navigator.connection.saveData === true
    if (!tasarruf) {
      const bosZaman = window.requestIdleCallback || ((fn) => setTimeout(fn, 1200))
      bosZaman(() => { import('./Dashboard.vue').catch(() => {}) })
    }
  } catch {
    /* yoksay */
  }
})
</script>

<style scoped>
.giris-sayfasi {
  /* Giriş ekranı görsel dil tokenları — koyu ve açık tema buradan beslenir. */
  --giris-aksan: var(--accent, #14b8a6);
  --giris-aksan-parlak: #2dd4bf;
  --giris-aksan-koyu: #0d9488;
  --giris-aksan-yumusak: rgba(20, 184, 166, 0.1);
  --giris-aksan-yumusak-2: rgba(20, 184, 166, 0.15);
  --giris-aksan-kenar: rgba(20, 184, 166, 0.3);
  --giris-aksan-kenar-2: rgba(20, 184, 166, 0.45);
  --giris-aksan-halka: rgba(20, 184, 166, 0.2);
  --giris-buton-1: #14b8a6;
  --giris-buton-2: #0d9488;
  --giris-buton-metin: #04211d;
  --giris-zemin-1: #070b10;
  --giris-zemin-2: #0b1219;
  --giris-yuzey: rgba(16, 24, 33, 0.78);
  --giris-yuzey-2: rgba(7, 11, 16, 0.72);
  --giris-yuzey-3: rgba(7, 11, 16, 0.52);
  --giris-yuzey-4: rgba(7, 11, 16, 0.42);
  --giris-kenar: rgba(148, 163, 184, 0.14);
  --giris-kenar-2: rgba(148, 163, 184, 0.18);
  --giris-kenar-3: rgba(148, 163, 184, 0.3);
  --giris-kenar-4: rgba(148, 163, 184, 0.22);
  --giris-hover: rgba(148, 163, 184, 0.1);
  --giris-metin: #e8eef5;
  --giris-metin-2: #9fb0c0;
  --giris-metin-3: #6f8294;
  --giris-vurgu: rgba(255, 255, 255, 0.04);
  --giris-vurgu-2: rgba(255, 255, 255, 0.08);
  --giris-golge-renk: rgba(0, 0, 0, 0.45);
  --giris-hata-metin: #fca5a5;
  --giris-hata-yumusak: rgba(239, 68, 68, 0.12);
  --giris-hata-kenar: rgba(239, 68, 68, 0.36);
  --giris-uyari: #fbbf24;
  --giris-uyari-yumusak: rgba(245, 158, 11, 0.14);
  --giris-uyari-kenar: rgba(245, 158, 11, 0.3);
  --giris-basari: #5eead4;
  --giris-radius: 18px;

  --text-primary: var(--giris-metin);
  --text-secondary: var(--giris-metin-2);
  --text-muted: var(--giris-metin-3);
  --bg-primary: var(--giris-zemin-2);
  --bg-card: #101821;
  --border: var(--giris-kenar-2);
  --primary-color: var(--giris-aksan);

  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  max-width: 100%;
  min-height: calc(100vh / var(--uyg-olcek));
  min-height: calc(100dvh / var(--uyg-olcek));
  padding:
    calc(24px + env(safe-area-inset-top))
    calc(20px + env(safe-area-inset-right))
    calc(24px + env(safe-area-inset-bottom))
    calc(20px + env(safe-area-inset-left));
  background: linear-gradient(180deg, var(--giris-zemin-1) 0%, var(--giris-zemin-2) 100%);
  color: var(--giris-metin);
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior: none;
  -webkit-tap-highlight-color: transparent;
}

[data-theme='light'] .giris-sayfasi {
  --giris-aksan: #0d9488;
  --giris-aksan-parlak: #0f766e;
  --giris-aksan-koyu: #115e59;
  --giris-aksan-yumusak: rgba(13, 148, 136, 0.09);
  --giris-aksan-yumusak-2: rgba(13, 148, 136, 0.14);
  --giris-aksan-kenar: rgba(13, 148, 136, 0.28);
  --giris-aksan-kenar-2: rgba(13, 148, 136, 0.45);
  --giris-aksan-halka: rgba(13, 148, 136, 0.16);
  --giris-buton-1: #0d9488;
  --giris-buton-2: #115e59;
  --giris-buton-metin: #ffffff;
  --giris-zemin-1: #f8fafc;
  --giris-zemin-2: #eef2f7;
  --giris-yuzey: rgba(255, 255, 255, 0.9);
  --giris-yuzey-2: #ffffff;
  --giris-yuzey-3: #f8fafc;
  --giris-yuzey-4: #f1f5f9;
  --giris-kenar: rgba(15, 23, 42, 0.1);
  --giris-kenar-2: rgba(15, 23, 42, 0.14);
  --giris-kenar-3: rgba(15, 23, 42, 0.24);
  --giris-kenar-4: rgba(15, 23, 42, 0.18);
  --giris-hover: rgba(15, 23, 42, 0.06);
  --giris-metin: #0f172a;
  --giris-metin-2: #475569;
  --giris-metin-3: #64748b;
  --giris-vurgu: rgba(255, 255, 255, 0.6);
  --giris-vurgu-2: rgba(255, 255, 255, 0.9);
  --giris-golge-renk: rgba(15, 23, 42, 0.12);
  --giris-hata-metin: #b91c1c;
  --giris-hata-yumusak: rgba(220, 38, 38, 0.08);
  --giris-hata-kenar: rgba(220, 38, 38, 0.3);
  --giris-uyari: #b45309;
  --giris-uyari-yumusak: rgba(180, 83, 9, 0.1);
  --giris-uyari-kenar: rgba(180, 83, 9, 0.28);
  --giris-basari: #0f766e;

  --text-primary: var(--giris-metin);
  --text-secondary: var(--giris-metin-2);
  --text-muted: var(--giris-metin-3);
  --bg-primary: var(--giris-zemin-2);
  --bg-card: #ffffff;
  --border: var(--giris-kenar-2);
  --primary-color: var(--giris-aksan);
}

[data-theme='light'] .giris-mesh {
  background:
    radial-gradient(38% 44% at 20% 18%, rgba(13, 148, 136, 0.12), transparent 70%),
    radial-gradient(42% 46% at 82% 24%, rgba(15, 118, 110, 0.08), transparent 72%),
    radial-gradient(44% 48% at 66% 84%, rgba(13, 148, 136, 0.08), transparent 74%);
}

[data-theme='light'] .giris-grid {
  background-image:
    linear-gradient(rgba(15, 23, 42, 0.055) 1px, transparent 1px),
    linear-gradient(90deg, rgba(15, 23, 42, 0.055) 1px, transparent 1px);
}

.giris-sahne {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.giris-mesh {
  position: absolute;
  inset: -18%;
  background:
    radial-gradient(38% 44% at 20% 18%, rgba(20, 184, 166, 0.14), transparent 70%),
    radial-gradient(42% 46% at 82% 24%, rgba(45, 212, 191, 0.09), transparent 72%),
    radial-gradient(44% 48% at 66% 84%, rgba(13, 148, 136, 0.1), transparent 74%);
}

.giris-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.05) 1px, transparent 1px);
  background-size: 52px 52px;
  -webkit-mask-image: radial-gradient(circle at 50% 42%, #000 0%, transparent 78%);
  mask-image: radial-gradient(circle at 50% 42%, #000 0%, transparent 78%);
  opacity: 0.7;
}

.giris-yerlesim {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 420px);
  align-items: center;
  gap: clamp(32px, 5vw, 64px);
  width: 100%;
  max-width: 1120px;
  min-width: 0;
}

.giris-marka-alani {
  min-width: 0;
}

.giris-icerik {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-self: end;
  width: 100%;
  max-width: 420px;
  min-width: 0;
}

.giris-tema-secim {
  display: inline-flex;
  align-self: flex-end;
  gap: 2px;
  margin-bottom: 10px;
  padding: 3px;
  border: 1px solid var(--giris-kenar-2);
  border-radius: 999px;
  background: var(--giris-yuzey-2);
  box-shadow: var(--shadow);
  backdrop-filter: blur(10px);
}

.giris-tema-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 32px;
  padding: 0 11px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--giris-metin-3);
  font-family: inherit;
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
  transition:
    background 160ms ease-out,
    color 160ms ease-out,
    box-shadow 160ms ease-out;
}

.giris-tema-btn i {
  font-size: 11.5px;
}

.giris-tema-btn:hover {
  background: var(--giris-hover);
  color: var(--giris-metin);
}

.giris-tema-btn--aktif {
  background: var(--giris-aksan-yumusak-2);
  color: var(--giris-aksan-parlak);
  box-shadow: inset 0 0 0 1px var(--giris-aksan-kenar);
}

.giris-tema-btn:focus-visible {
  outline: 2px solid var(--giris-aksan);
  outline-offset: 2px;
}

.giris-mobil-marka {
  display: none;
  align-items: center;
  gap: 10px;
  width: 100%;
  margin-bottom: 16px;
}

.giris-mobil-logo {
  width: 38px;
  height: 38px;
  object-fit: contain;
  flex-shrink: 0;
}

.giris-mobil-metin {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.giris-mobil-ad {
  font-size: 16px;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--giris-metin);
}

.giris-mobil-vaat {
  margin: 0;
  font-size: 12px;
  line-height: 1.4;
  color: var(--giris-metin-2);
}

/* <960px: tek kolon; marka paneli yerine kompakt logo + vaat */
@media (max-width: 960px) {
  .giris-yerlesim {
    grid-template-columns: minmax(0, 1fr);
    max-width: 440px;
    gap: 0;
  }
  .giris-marka-alani {
    display: none;
  }
  .giris-icerik {
    justify-self: center;
  }
  .giris-mobil-marka {
    display: flex;
  }
  .giris-sayfasi {
    align-items: flex-start;
    padding-top: calc(20px + env(safe-area-inset-top));
  }
}

@media (max-width: 520px) {
  .giris-tema-btn {
    width: 36px;
    min-height: 34px;
    padding: 0;
  }

  .giris-tema-btn span {
    display: none;
  }
}

@media (max-width: 480px) {
  .giris-sayfasi {
    padding:
      calc(16px + env(safe-area-inset-top))
      calc(14px + env(safe-area-inset-right))
      calc(20px + env(safe-area-inset-bottom))
      calc(14px + env(safe-area-inset-left));
  }
}

/* Yüklenmiş PWA: durum çubuğu/notch için ek boşluk */
.giris-sayfasi.pwa-modu {
  padding-top: calc(12px + env(safe-area-inset-top));
  padding-bottom: calc(12px + env(safe-area-inset-bottom));
}
</style>
