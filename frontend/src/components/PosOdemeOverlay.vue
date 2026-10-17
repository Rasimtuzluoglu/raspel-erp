<template>
  <Dialog
    :visible="gorunur"
    modal
    class="pos-odeme-overlay"
    :style="{ width: '640px', maxWidth: '96vw' }"
    :dismissable-mask="false"
    @update:visible="$emit('update:gorunur', $event)"
  >
    <template #header>
      <div class="odeme-baslik">
        <div class="odeme-baslik-metin">
          <i class="pi pi-wallet" />
          <span>{{ t('hizliSatis.odeme') }}</span>
        </div>
        <div class="odeme-baslik-tutar">
          <span>{{ t('hizliSatis.genelToplam') }}</span>
          <strong>{{ formatCurrency(genelToplam) }}</strong>
        </div>
      </div>
    </template>

    <div class="odeme-overlay-icerik">
      <!-- NAKIT: hizli tutar girisi icin tuş takimi. Diger yontemlerde
           gereksiz oldugu icin gizlenir; mevcut panel zaten tum alanlari sunar. -->
      <div
        v-if="nakitAktif"
        class="odeme-keypad"
      >
        <div class="keypad-baslik">
          <span>{{ t('hizliSatis.alinanNakit') }}</span>
          <strong class="keypad-ekran">{{ formatCurrency(alinanNakit) }}</strong>
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
            class="keypad-tus keypad-tus-islev"
            :aria-label="t('common.delete')"
            @click="geriAl"
          >
            <i class="pi pi-delete-left" />
          </button>
        </div>
      </div>

      <!-- Mevcut odeme paneli: yontem, taksit, kasa/banka/pos, durum, kalan,
           hizli nakit kisayollari, para ustu. Baslik gizli (overlay basligi var). -->
      <div class="odeme-detay">
        <PosOdemePaneli
          v-bind="$attrs"
          baslik-gizle
          :acik="true"
        />
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
import { computed, useAttrs } from 'vue'
import { useI18n } from 'vue-i18n'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import { formatCurrency } from '../utils/format.js'
import PosOdemePaneli from './PosOdemePaneli.vue'

// Dialog kendi DOM'unda $attrs istemiyoruz; tum durum prop'lari ve update
// dinleyicileri asagidaki PosOdemePaneli'ne AYNEN aktarilir (v-bind="$attrs").
defineOptions({ inheritAttrs: false })

defineProps({
  gorunur: { type: Boolean, default: false }
})
defineEmits(['update:gorunur', 'tamamla'])

const attrs = useAttrs()
const { t } = useI18n()

/** Tuş takimi: 0-9 ve '00' (yuvarlak tutarlar icin). Ondalik TRY'de nadirdir. */
const TUSLAR = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '00', '0']

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
const odemeYontemi = computed(() => attr('odemeYontemi'))
const odemeDurumu = computed(() => attr('odemeDurumu'))
const genelToplam = computed(() => sayi(attr('genelToplam')))
const kalanTutar = computed(() => sayi(attr('kalanTutar')))
const nakitAktif = computed(
  () => odemeDurumu.value !== 'yok' && odemeYontemi.value === 'NAKIT'
)

/** `alinanNakit` degerini gunceller (HizliSatis'teki v-model:alinan-nakit). */
const nakitAyarla = (v) => {
  const dinleyici = attrs['onUpdate:alinanNakit'] || attrs['onUpdate:alinan-nakit']
  dinleyici?.(v)
}

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
</script>

<style scoped>
.odeme-overlay-icerik {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
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
  gap: 8px;
  font-weight: 700;
  font-size: 15px;
  color: var(--text-primary);
}
.odeme-baslik-metin i {
  color: var(--accent);
  font-size: 16px;
}
.odeme-baslik-tutar {
  display: inline-flex;
  align-items: baseline;
  gap: 8px;
}
.odeme-baslik-tutar span {
  font-size: 12px;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.4px;
}
.odeme-baslik-tutar strong {
  font-size: 22px;
  font-weight: 800;
  color: var(--accent);
  font-variant-numeric: tabular-nums;
}
.odeme-keypad {
  border: 1px solid var(--border, #e2e8f0);
  border-radius: var(--pos-alan-radius, 12px);
  padding: 12px;
  background: var(--bg-primary, #fff);
}
.keypad-baslik {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
  font-size: var(--pos-etiket-font, 12px);
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.keypad-ekran {
  font-size: 22px;
  font-weight: 800;
  color: var(--accent, #14b8a6);
  font-variant-numeric: tabular-nums;
}
.keypad-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}
.keypad-tus {
  min-height: 52px;
  border: 1px solid var(--border, #e2e8f0);
  border-radius: 10px;
  background: var(--bg-card, #fff);
  color: var(--text-primary);
  font-size: 19px;
  font-weight: 700;
  cursor: pointer;
  font-variant-numeric: tabular-nums;
  transition: background var(--dur-fast, 0.15s) ease, border-color var(--dur-fast, 0.15s) ease;
}
.keypad-tus:hover {
  border-color: var(--accent, #14b8a6);
  color: var(--accent, #14b8a6);
}
.keypad-tus:active {
  transform: translateY(1px);
}
.keypad-tus-genis {
  grid-column: span 1;
  font-size: 15px;
}
.keypad-tus-islev {
  background: var(--bg-secondary, #f1f5f9);
  font-size: 16px;
}
.odeme-detay {
  border-top: 1px solid var(--border, #e2e8f0);
  padding-top: 12px;
}
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
  min-width: 160px;
}
</style>
