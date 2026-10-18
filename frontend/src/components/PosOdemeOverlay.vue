<template>
  <Dialog
    :visible="gorunur"
    modal
    class="pos-odeme-overlay"
    :style="{ width: '780px', maxWidth: '96vw' }"
    :dismissable-mask="false"
    @update:visible="$emit('update:gorunur', $event)"
  >
    <template #header>
      <div class="odeme-baslik">
        <div class="odeme-baslik-metin">
          <span class="odeme-baslik-ikon"><i class="pi pi-wallet" /></span>
          <span class="odeme-baslik-ad">{{ t('hizliSatis.odeme') }}</span>
        </div>
        <div class="odeme-baslik-tutar">
          <span>{{ t('hizliSatis.genelToplam') }}</span>
          <strong>{{ formatCurrency(genelToplam) }}</strong>
        </div>
      </div>
    </template>

    <div class="odeme-overlay-icerik pos-container">
      <!-- Odeme ozeti: odenen tutar (tek kaynak). Kalan tutarin tek otoritesi
           footer'dir; para ustu ise keypad icinde gosterilir. -->
      <div class="odeme-ozet">
        <div class="tutar-stat">
          <span class="tutar-stat-etiket">{{ t('hizliSatis.odenenTutar') }}</span>
          <strong class="tutar-stat-deger">{{ formatCurrency(odenenTutar) }}</strong>
        </div>
      </div>

      <div
        class="odeme-grid"
        :class="{ 'odeme-grid-nakit': nakitAktif }"
      >
        <!-- NAKIT: hizli tutar girisi icin tuş takimi. Diger yontemlerde
             gereksiz oldugu icin gizlenir; mevcut panel tum alanlari sunar. -->
        <div
          v-if="nakitAktif"
          class="odeme-keypad"
        >
          <div class="tutar-stat">
            <span class="tutar-stat-etiket">{{ t('hizliSatis.alinanNakit') }}</span>
            <strong class="tutar-stat-deger">{{ formatCurrency(alinanNakit) }}</strong>
          </div>
          <div class="keypad-grid">
            <button
              v-for="k in TUSLAR"
              :key="k"
              type="button"
              class="keypad-tus"
              :class="{ 'keypad-tus-genis': k === '00' }"
              @click="tusBas(k)"
            >
              {{ k }}
            </button>
            <button
              type="button"
              class="keypad-tus keypad-tus-islev"
              :aria-label="t('hizliSatis.temizle')"
              @click="nakitAyarla(0)"
            >
              C
            </button>
            <button
              type="button"
              class="keypad-tus keypad-tus-islev keypad-tus-tam"
              :aria-label="t('common.delete')"
              @click="geriAl"
            >
              <i class="pi pi-delete-left" />
            </button>
          </div>

          <div
            v-if="paraUstu > 0"
            class="tutar-stat tutar-stat-pozitif"
          >
            <span class="tutar-stat-etiket">{{ t('hizliSatis.paraUstu') }}</span>
            <strong class="tutar-stat-deger">{{ formatCurrency(paraUstu) }}</strong>
          </div>

          <div class="hizli-nakit">
            <button
              v-for="n in hizliNakit"
              :key="n"
              type="button"
              class="hizli-nakit-btn"
              @click="nakitEkle(n)"
            >
              ₺{{ n }}
            </button>
            <button
              type="button"
              class="hizli-nakit-btn tam"
              @click="nakitAyarla(genelToplam)"
            >
              <span class="hizli-nakit-tam-etiket">{{ t('hizliSatis.nakitTam') }}</span>
              <strong class="hizli-nakit-tam-tutar">{{ formatCurrency(genelToplam) }}</strong>
            </button>
          </div>
        </div>

        <!-- Mevcut odeme paneli: yontem, taksit, kasa/banka/pos, durum, kalan,
             hizli nakit kisayollari, para ustu. Baslik gizli (overlay basligi var).
             Nakit girisi keypad'de oldugu icin panelde tekrarlanmaz. -->
        <div class="odeme-detay">
          <PosOdemePaneli
            v-bind="$attrs"
            baslik-gizle
            :acik="true"
            :nakit-giris-gizle="nakitAktif"
            :odenen-giris-gizle="odemeDurumu !== 'yarim'"
          />
        </div>
      </div>
    </div>

    <template #footer>
      <div class="odeme-footer">
        <div
          v-if="kalanTutar > 0"
          class="odeme-footer-kalan"
        >
          <span>{{ t('hizliSatis.kalan') }}:</span>
          <strong>{{ formatCurrency(kalanTutar) }}</strong>
        </div>
        <div class="odeme-footer-btnler">
          <Button
            :label="t('common.cancel')"
            severity="secondary"
            text
            @click="$emit('update:gorunur', false)"
          />
          <Button
            :label="t('hizliSatis.satisiTamamla')"
            icon="pi pi-check"
            severity="success"
            class="odeme-tamamla-btn"
            @click="$emit('tamamla')"
          />
        </div>
      </div>
    </template>
  </Dialog>
</template>

<script setup>
import { computed, onBeforeUnmount, useAttrs, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import { formatCurrency } from '../utils/format.js'
import PosOdemePaneli from './PosOdemePaneli.vue'

// Dialog kendi DOM'unda $attrs istemiyoruz; tum durum prop'lari ve update
// dinleyicileri asagidaki PosOdemePaneli'ne AYNEN aktarilir (v-bind="$attrs").
defineOptions({ inheritAttrs: false })

const props = defineProps({
  gorunur: { type: Boolean, default: false }
})
defineEmits(['update:gorunur', 'tamamla'])

const attrs = useAttrs()
const { t } = useI18n()

/** Tuş takimi: 0-9 ve '00' (yuvarlak tutarlar icin). Ondalik TRY'de nadirdir. */
const TUSLAR = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '00', '0']

/** Nakit hizli tutar onerileri (composable varsayilaniyla ayni). */
const VARSAYILAN_HIZLI_NAKIT = [50, 100, 200, 500]

const sayi = (v) => Number(v || 0)

// SFC sablonunda `v-model:odeme-yontemi` gibi KEBAB adlar kullanildiginda
// `$attrs` anahtari kebab-case olabilir; camelCase anahtarlar da desteklenir.
// Iki bicimi de okuyarak binding'in nasil yazildigindan bagimsiz calisiriz.
const attr = (ad) => {
  if (attrs[ad] !== undefined) return attrs[ad]
  const kebab = ad.replace(/[A-Z]/g, (m) => '-' + m.toLowerCase())
  return attrs[kebab]
}

const alinanNakit = computed(() => sayi(attr('alinanNakit')))
const odenenTutar = computed(() => sayi(attr('odenenTutar')))
const paraUstu = computed(() => sayi(attr('paraUstu')))
const odemeYontemi = computed(() => attr('odemeYontemi'))
const odemeDurumu = computed(() => attr('odemeDurumu'))
const genelToplam = computed(() => sayi(attr('genelToplam')))
const kalanTutar = computed(() => sayi(attr('kalanTutar')))
const nakitAktif = computed(
  () => odemeDurumu.value !== 'yok' && odemeYontemi.value === 'NAKIT'
)
const hizliNakit = computed(() => {
  const liste = attr('hizliNakit')
  return Array.isArray(liste) && liste.length > 0 ? liste : VARSAYILAN_HIZLI_NAKIT
})

/** `alinanNakit` degerini gunceller (HizliSatis'teki v-model:alinan-nakit). */
const nakitAyarla = (v) => {
  const dinleyici = attrs['onUpdate:alinanNakit'] || attrs['onUpdate:alinan-nakit']
  dinleyici?.(v)
}

const nakitEkle = (tutar) => nakitAyarla(alinanNakit.value + tutar)

const tusBas = (k) => {
  const mevcut = Math.trunc(alinanNakit.value)
  if (k === '00') {
    nakitAyarla(mevcut * 100)
    return
  }
  // Basta 0 varken rakam yazmak 0'i korur ('05' -> 5).
  nakitAyarla(Number(String(mevcut) + k))
}

const geriAl = () => {
  const s = String(Math.trunc(alinanNakit.value))
  nakitAyarla(s.length <= 1 ? 0 : Number(s.slice(0, -1)))
}

/* --------------------------------------------------------- KLAVYE GIRISI ---
   Nakit modunda pencere acikken fiziksel klavyeden tutar girisi. Odak bir
   giris alaninda (input/select/textarea) ise karismayiz; yalnizca rakam ve
   Backspace islenir (harf kisayollari POS katmanina birakilir). */
const girdideMi = (el) =>
  Boolean(
    el &&
      (el.tagName === 'INPUT' ||
        el.tagName === 'SELECT' ||
        el.tagName === 'TEXTAREA' ||
        el.isContentEditable)
  )

const klavyeBas = (e) => {
  if (!nakitAktif.value) return
  if (e.ctrlKey || e.metaKey || e.altKey) return
  if (girdideMi(e.target)) return
  if (e.key >= '0' && e.key <= '9') {
    e.preventDefault()
    tusBas(e.key)
  } else if (e.key === 'Backspace') {
    e.preventDefault()
    geriAl()
  }
}

watch(
  () => props.gorunur,
  (acik) => {
    if (acik) window.addEventListener('keydown', klavyeBas)
    else window.removeEventListener('keydown', klavyeBas)
  },
  { immediate: true }
)

onBeforeUnmount(() => window.removeEventListener('keydown', klavyeBas))
</script>

<style scoped>
.odeme-overlay-icerik {
  display: flex;
  flex-direction: column;
  gap: 16px;
  /* `pos-container` sinifi yalnizca alt siniflardaki panel stillerinin
     (`.pos-container .odeme-*`) uygulanmasi icin eklenir. POS sayfasinin
     viewport kilidi kurallari (height/overflow) dialog icerigine sizmasin. */
  height: auto;
  min-height: 0;
  max-width: none;
  padding: 0;
  overflow: visible;
}

/* ------------------------------------------------------------------ BASLIK */
.odeme-baslik {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}
.odeme-baslik-metin {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
  font-size: 15px;
  color: var(--text-primary);
}
.odeme-baslik-ikon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 16px;
}
.odeme-baslik-tutar {
  display: inline-flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}
.odeme-baslik-tutar span {
  font-size: 11px;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.odeme-baslik-tutar strong {
  font-size: 22px;
  font-weight: 800;
  line-height: 1.1;
  color: var(--accent);
  font-variant-numeric: tabular-nums;
}

/* --------------------------------------------------- TUTAR STAT (ortak) ---
   Odenen / alinan nakit / para ustu ayni kalibi ve ayni puntoyu kullanir. */
.odeme-ozet {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 10px;
}
.tutar-stat {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-secondary);
}
.tutar-stat-etiket {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.tutar-stat-deger {
  font-size: 18px;
  font-weight: 800;
  line-height: 1.15;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.tutar-stat-pozitif {
  border-color: var(--success-border);
  background: var(--success-soft);
}
.tutar-stat-pozitif .tutar-stat-deger {
  color: var(--success);
}

/* ------------------------------------------------------------------- GRID */
.odeme-grid {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.odeme-grid-nakit {
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(0, 1.08fr);
  gap: 16px;
  align-items: stretch;
}
.odeme-detay {
  min-width: 0;
  display: flex;
  flex-direction: column;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--bg-primary);
}
/* Iki kart ayni yukseklige esitlenir; panel icerigi kart boyunca dagitilir. */
.odeme-grid-nakit .odeme-detay :deep(.pos-bolum) {
  flex: 1 1 auto;
}
.odeme-grid-nakit .odeme-detay :deep(.odeme-icerik) {
  flex: 1 1 auto;
  justify-content: space-between;
}

/* ---------------------------------------------------------------- KEYPAD */
.odeme-keypad {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--bg-primary);
}
.keypad-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}
.keypad-tus {
  min-height: 50px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-card);
  color: var(--text-primary);
  font-size: 19px;
  font-weight: 700;
  cursor: pointer;
  font-variant-numeric: tabular-nums;
  transition:
    background var(--dur-fast, 0.15s) ease,
    border-color var(--dur-fast, 0.15s) ease,
    color var(--dur-fast, 0.15s) ease;
}
.keypad-tus:hover {
  border-color: var(--accent-border);
  background: var(--accent-soft);
  color: var(--accent);
}
.keypad-tus:active {
  transform: translateY(1px);
}
.keypad-tus-genis {
  grid-column: span 1;
  font-size: 15px;
}
.keypad-tus-islev {
  background: var(--bg-muted);
  color: var(--text-secondary);
  font-size: 16px;
}
.keypad-tus-tam {
  grid-column: 1 / -1;
  min-height: 42px;
}

/* Keypad icine tasinan hizli nakit kisayollari (daha dar kolon: 3 sutun). */
.odeme-keypad .hizli-nakit {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}
.odeme-keypad .hizli-nakit-btn.tam {
  grid-column: span 2;
}

/* ----------------------------------------------------------------- FOOTER */
.odeme-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}
.odeme-footer-kalan {
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
}
.odeme-footer-kalan strong {
  color: var(--danger, #ef4444);
  font-size: 16px;
  font-variant-numeric: tabular-nums;
}
.odeme-footer-btnler {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}
.odeme-tamamla-btn {
  min-width: 170px;
}

/* ------------------------------------------------------------- RESPONSIVE */
@media (max-width: 700px) {
  .odeme-grid-nakit {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (prefers-reduced-motion: reduce) {
  .keypad-tus {
    transition: none;
  }
}
</style>
