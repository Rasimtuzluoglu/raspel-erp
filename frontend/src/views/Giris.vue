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
import GirisAcilis from '../components/GirisAcilis.vue'
import GirisMarkaPaneli from '../components/GirisMarkaPaneli.vue'
import GirisFormKarti from '../components/GirisFormKarti.vue'
import GirisAltBilgi from '../components/GirisAltBilgi.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
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
  /* Faz 1 görsel dil tokenları (spec §3.1) */
  --giris-aksan: var(--accent, #14b8a6);
  --giris-aksan-parlak: #2dd4bf;
  --giris-aksan-koyu: #0d9488;
  --giris-zemin-1: #070b10;
  --giris-zemin-2: #0b1219;
  --giris-yuzey: rgba(16, 24, 33, 0.78);
  --giris-kenar: rgba(148, 163, 184, 0.14);
  --giris-metin: #e8eef5;
  --giris-metin-2: #9fb0c0;
  --giris-metin-3: #6f8294;
  --giris-radius: 18px;

  /* Giriş her zaman koyu kalır: tema açık olsa bile form ve ilk kurulum
     sihirbazı okunaklı olsun. */
  --text-primary: #e8eef5;
  --text-secondary: #9fb0c0;
  --text-muted: #6f8294;
  --bg-primary: #0b1219;
  --bg-card: #101821;
  --border: rgba(148, 163, 184, 0.18);
  --primary-color: #14b8a6;

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
