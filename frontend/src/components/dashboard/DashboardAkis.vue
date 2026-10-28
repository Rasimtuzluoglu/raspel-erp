<template>
  <div
    class="canli-akis"
    data-test="canli-akis"
    role="feed"
    :aria-label="t('dashboard.akisBaslik')"
    aria-busy="false"
  >
    <div
      v-if="yukleniyor && !akis.length"
      class="akis-iskelet"
      aria-hidden="true"
    >
      <div
        v-for="i in 4"
        :key="`akis-iskelet-${i}`"
        class="akis-iskelet-satir"
      >
        <span class="iskelet iskelet--nokta" />
        <span class="iskelet iskelet--metin" />
      </div>
    </div>

    <p
      v-else-if="hata"
      class="akis-durum akis-durum--hata"
    >
      <i
        class="pi pi-exclamation-circle"
        aria-hidden="true"
      />
      {{ t('dashboard.akisHata') }}
    </p>

    <p
      v-else-if="!akis.length"
      class="akis-durum"
    >
      <i
        class="pi pi-inbox"
        aria-hidden="true"
      />
      {{ t('dashboard.akisBos') }}
    </p>

    <ol
      v-else
      class="akis-liste"
    >
      <li
        v-for="(olay, index) in akis"
        :key="`${olay.tur}-${olay.tarih}-${index}`"
        class="akis-satir"
        role="article"
      >
        <span
          class="akis-nokta"
          :style="{ background: `${olay.renk || renk(olay.tur)}22`, color: olay.renk || renk(olay.tur) }"
        >
          <i
            :class="olay.ikon || ikon(olay.tur)"
            aria-hidden="true"
          />
        </span>
        <component
          :is="olay.route ? 'router-link' : 'div'"
          :to="olay.route || undefined"
          class="akis-icerik"
        >
          <span class="akis-ust">
            <span class="akis-baslik">{{ olay.baslik || turEtiketi(olay.tur) }}</span>
            <span class="akis-zaman">{{ goreliZaman(olay.tarih) }}</span>
          </span>
          <span
            v-if="olay.aciklama"
            class="akis-aciklama"
          >{{ olay.aciklama }}</span>
          <span
            v-if="Number(olay.tutar) > 0"
            class="akis-tutar gizli-veri"
          >{{ formatCurrency(olay.tutar) }}</span>
        </component>
      </li>
    </ol>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatCurrency } from '../../utils/format.js'

const props = defineProps({
  akis: { type: Array, default: () => [] },
  yukleniyor: { type: Boolean, default: false },
  hata: { type: String, default: null },
  // 60 sn'de bir sessiz yenileme yalnizca sayfa gorunurken yapilir.
  otomatikYenile: { type: Boolean, default: true }
})

const emit = defineEmits(['yenile'])

const { t } = useI18n()

const AKIS_TUR_ANAHTARLARI = {
  ONAY: 'dashboard.akisTurOnay',
  TAHSILAT: 'dashboard.akisTurTahsilat',
  ODEME: 'dashboard.akisTurOdeme',
  FATURA: 'dashboard.akisTurFatura',
  SIPARIS: 'dashboard.akisTurSiparis',
  STOK: 'dashboard.akisTurStok'
}

const IKONLAR = {
  ONAY: 'pi pi-check-circle',
  TAHSILAT: 'pi pi-arrow-down-left',
  ODEME: 'pi pi-arrow-up-right',
  FATURA: 'pi pi-file',
  SIPARIS: 'pi pi-shopping-cart',
  STOK: 'pi pi-box'
}

const RENKLER = {
  ONAY: '#3b82f6',
  TAHSILAT: '#10b981',
  ODEME: '#ef4444',
  FATURA: '#8b5cf6',
  SIPARIS: '#f59e0b',
  STOK: '#06b6d4'
}

const ikon = (tur) => IKONLAR[tur] || 'pi pi-history'
const renk = (tur) => RENKLER[tur] || '#64748b'
const turEtiketi = (tur) => (AKIS_TUR_ANAHTARLARI[tur] ? t(AKIS_TUR_ANAHTARLARI[tur]) : tur)

const goreliZaman = (tarih) => {
  if (!tarih) return ''
  const d = new Date(tarih)
  if (Number.isNaN(d.getTime())) return ''
  const fark = Math.max(0, Date.now() - d.getTime())
  const dakika = Math.floor(fark / 60000)
  if (dakika < 1) return t('dashboard.akisSimdi')
  if (dakika < 60) return t('dashboard.akisDakikaOnce', { n: dakika })
  const saat = Math.floor(dakika / 60)
  if (saat < 24) return t('dashboard.akisSaatOnce', { n: saat })
  return t('dashboard.akisGunOnce', { n: Math.floor(saat / 24) })
}

let zamanlayici = null

onMounted(() => {
  if (!props.otomatikYenile) return
  zamanlayici = setInterval(() => {
    // Arka plan sekmesinde gereksiz istek atmamak icin gorunurluk kontrolu.
    if (typeof document !== 'undefined' && document.visibilityState !== 'visible') return
    emit('yenile')
  }, 60000)
})

onUnmounted(() => {
  if (zamanlayici) clearInterval(zamanlayici)
  zamanlayici = null
})
</script>

<style scoped>
.canli-akis {
  min-height: 160px;
}
.akis-liste {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}
.akis-satir {
  position: relative;
  display: flex;
  gap: 10px;
  padding: 6px 0;
}
.akis-satir:not(:last-child)::after {
  content: '';
  position: absolute;
  left: 14px;
  top: 40px;
  bottom: -2px;
  width: 1px;
  background: var(--border);
}
.akis-nokta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 9px;
  flex-shrink: 0;
  font-size: 13px;
  z-index: 1;
}
.akis-icerik {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
  flex: 1;
  padding: 2px 6px;
  border-radius: 8px;
  color: inherit;
  text-decoration: none;
}
a.akis-icerik:hover {
  background: var(--bg-muted);
}
a.akis-icerik:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.akis-ust {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-width: 0;
}
.akis-baslik {
  font-size: 12.5px;
  font-weight: 700;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}
.akis-zaman {
  margin-left: auto;
  font-size: 10.5px;
  color: var(--text-muted);
  white-space: nowrap;
  flex-shrink: 0;
}
.akis-aciklama {
  font-size: 11.5px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.akis-tutar {
  font-size: 11.5px;
  font-weight: 700;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.akis-durum {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  padding: 18px 12px;
  border-radius: 12px;
  border: 1px dashed var(--border);
  color: var(--text-muted);
  font-size: 13px;
}
.akis-durum--hata {
  color: var(--danger);
}
.akis-iskelet {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.akis-iskelet-satir {
  display: flex;
  align-items: center;
  gap: 10px;
}
.iskelet {
  display: block;
  background: var(--bg-muted);
  animation: iskeletParlama 1.4s ease infinite;
}
.iskelet--nokta {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  flex-shrink: 0;
}
.iskelet--metin {
  flex: 1;
  height: 12px;
  border-radius: 6px;
}
@keyframes iskeletParlama {
  0% {
    opacity: 0.45;
  }
  50% {
    opacity: 0.85;
  }
  100% {
    opacity: 0.45;
  }
}
@media (prefers-reduced-motion: reduce) {
  .iskelet {
    animation: none;
  }
}
</style>
