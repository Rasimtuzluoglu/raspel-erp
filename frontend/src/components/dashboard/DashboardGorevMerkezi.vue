<template>
  <div
    class="gorev-merkezi"
    data-test="gorev-merkezi"
  >
    <div
      v-if="yukleniyor && !gorevler.length"
      class="gorev-iskelet"
      aria-hidden="true"
    >
      <div
        v-for="i in 4"
        :key="`gorev-iskelet-${i}`"
        class="gorev-iskelet-satir"
      >
        <span class="iskelet iskelet--daire" />
        <span class="iskelet iskelet--metin" />
      </div>
    </div>

    <div
      v-else-if="hata"
      class="gorev-durum gorev-durum--hata"
    >
      <span class="gorev-durum-metin">
        <i
          class="pi pi-exclamation-circle"
          aria-hidden="true"
        />
        {{ t('dashboard.gorevHata') }}
      </span>
      <button
        type="button"
        class="gorev-dene"
        @click="emit('yenile')"
      >
        {{ t('dashboard.gorevYenidenDene') }}
      </button>
    </div>

    <p
      v-else-if="!siraliGorevler.length"
      class="gorev-durum gorev-durum--bos"
    >
      <i
        class="pi pi-check-circle"
        aria-hidden="true"
      />
      {{ t('dashboard.gorevBos') }}
    </p>

    <ul
      v-else
      class="gorev-liste"
    >
      <li
        v-for="g in siraliGorevler"
        :key="g.tur"
        class="gorev-satir"
        :class="oncelikSinifi(g.oncelik)"
        :data-test="`gorev-${g.tur}`"
      >
        <span
          class="gorev-ikon"
          :style="{ background: `${g.renk || '#3b82f6'}22`, color: g.renk || '#3b82f6' }"
        >
          <i
            :class="g.ikon || 'pi pi-flag'"
            aria-hidden="true"
          />
        </span>
        <span class="gorev-icerik">
          <span class="gorev-baslik">{{ gorevBasligi(g) }}</span>
          <span
            v-if="g.aciklama"
            class="gorev-aciklama"
            :title="g.aciklama"
          >{{ g.aciklama }}</span>
        </span>
        <span class="gorev-sag">
          <strong class="gorev-adet">{{ g.adet || 0 }}</strong>
          <span
            v-if="Number(g.tutar) > 0"
            class="gorev-tutar gizli-veri"
          >{{ formatCurrency(g.tutar) }}</span>
        </span>
        <router-link
          :to="aksiyon(g).to"
          class="gorev-eylem"
        >
          {{ aksiyon(g).metin }}
          <i
            class="pi pi-arrow-right"
            aria-hidden="true"
          />
        </router-link>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatCurrency } from '../../utils/format.js'

const props = defineProps({
  gorevler: { type: Array, default: () => [] },
  yukleniyor: { type: Boolean, default: false },
  hata: { type: String, default: null }
})

const emit = defineEmits(['yenile'])

const { t } = useI18n()

const GOREV_TUR_ANAHTARLARI = {
  ONAY_IZIN: 'dashboard.gorevTuruOnayIzin',
  ONAY_MASRAF: 'dashboard.gorevTuruOnayMasraf',
  ONAY_SATINALMA: 'dashboard.gorevTuruOnaySatinalma',
  ONAY_SIPARIS: 'dashboard.gorevTuruOnaySiparis',
  VADESI_GECEN: 'dashboard.gorevTuruVadesiGecen',
  VADESI_YAKLASAN: 'dashboard.gorevTuruVadesiYaklasan',
  KRITIK_STOK: 'dashboard.gorevTuruKritikStok',
  BEKLEYEN_TESLIMAT: 'dashboard.gorevTuruBekleyenTeslimat'
}

const VADE_TURLERI = ['VADESI_GECEN', 'VADESI_YAKLASAN']

const ONAY_TURLERI = {
  ONAY_IZIN: 'IZIN',
  ONAY_MASRAF: 'MASRAF',
  ONAY_SATINALMA: 'SATINALMA',
  ONAY_SIPARIS: 'SIPARIS'
}

const gorevBasligi = (g) => {
  const anahtar = GOREV_TUR_ANAHTARLARI[g.tur]
  if (g.baslik) return g.baslik
  return anahtar ? t(anahtar) : g.tur
}

const oncelikSinifi = (oncelik) => {
  const deger = Number(oncelik) || 0
  if (deger >= 70) return 'gorev-satir--kritik'
  if (deger >= 40) return 'gorev-satir--uyari'
  return 'gorev-satir--bilgi'
}

/**
 * Satir aksiyonu: vade -> tahsilat, kritik stok -> stok listesi, teslimat ->
 * teslimatlar, onaylar -> onaylar (tur filtresiyle), digerleri kendi rotasi.
 */
const aksiyon = (g) => {
  if (VADE_TURLERI.includes(g?.tur)) {
    return { to: '/tahsilat', metin: t('dashboard.gorevTahsilatLink') }
  }
  if (g?.tur === 'KRITIK_STOK') {
    return { to: '/stoklar?kritik=1', metin: t('dashboard.gorevAksiyonStok') }
  }
  if (g?.tur === 'BEKLEYEN_TESLIMAT') {
    return { to: '/teslimatlar', metin: t('dashboard.gorevAksiyonAc') }
  }
  const onay = ONAY_TURLERI[g?.tur]
  if (onay) {
    return { to: `/onaylar?tur=${onay}`, metin: t('dashboard.gorevAksiyonAc') }
  }
  return { to: g?.route || '/onaylar', metin: t('dashboard.gorevAksiyonAc') }
}

// Backend oncelik sirali dondurur; istemci de ayni sozlesmeyi garanti eder.
// En fazla 6 satir: kart bir "bugun" listesidir, tam kayit tablosu degil.
const siraliGorevler = computed(() =>
  [...props.gorevler]
    .filter((g) => g && (Number(g.adet) || 0) > 0)
    .sort((a, b) => (Number(b.oncelik) || 0) - (Number(a.oncelik) || 0))
    .slice(0, 6)
)
</script>

<style scoped>
/*
 * Tek liste: satirlar duz, sol tarafta bos kolon yok. Her satir dort parcadan
 * olusur (ikon, icerik, adet/tutar, aksiyon); aralarda 1px ayirici.
 */
.gorev-merkezi {
  min-width: 0;
}
.gorev-liste {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}
.gorev-satir {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 52px;
  min-width: 0;
  padding: 5px 6px;
  border-radius: 8px;
  transition: background var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.gorev-satir + .gorev-satir {
  border-top: 1px solid var(--border);
  border-top-left-radius: 0;
  border-top-right-radius: 0;
}
.gorev-satir:hover {
  background: var(--bg-muted);
}
.gorev-ikon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 10px;
  font-size: 14px;
  flex-shrink: 0;
}
.gorev-icerik {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
  flex: 1;
}
.gorev-baslik {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.gorev-aciklama {
  font-size: 11.5px;
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.gorev-sag {
  display: flex;
  align-items: baseline;
  gap: 8px;
  flex-shrink: 0;
}
.gorev-adet {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  min-height: 20px;
  padding: 0 7px;
  border-radius: 999px;
  background: var(--bg-muted);
  font-size: 12px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  line-height: 1;
}
.gorev-tutar {
  font-size: 11px;
  color: var(--text-secondary);
  font-variant-numeric: tabular-nums;
}
.gorev-satir--kritik .gorev-adet {
  color: var(--danger);
}
.gorev-satir--uyari .gorev-adet {
  color: var(--warning);
}
.gorev-satir--bilgi .gorev-adet {
  color: var(--accent);
}
.gorev-eylem {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  flex-shrink: 0;
  padding: 4px 8px;
  border-radius: 7px;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--accent);
  text-decoration: none;
  white-space: nowrap;
}
.gorev-eylem:hover {
  background: var(--accent-soft);
  color: var(--accent-hover);
}
.gorev-eylem:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.gorev-eylem i {
  font-size: 9px;
}
.gorev-durum {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  padding: 8px 0;
  color: var(--text-muted);
  font-size: 12.5px;
  min-height: 40px;
}
.gorev-durum--bos i {
  color: var(--success);
}
.gorev-durum--hata {
  color: var(--danger);
  justify-content: space-between;
  flex-wrap: wrap;
}
.gorev-durum-metin {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.gorev-dene {
  min-height: 30px;
  padding: 0 10px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
}
.gorev-dene:hover {
  color: var(--accent);
  border-color: var(--accent-border);
}
.gorev-dene:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.gorev-iskelet {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.gorev-iskelet-satir {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 0;
}
.gorev-iskelet-satir + .gorev-iskelet-satir {
  border-top: 1px solid var(--border);
}
.iskelet {
  display: block;
  border-radius: 8px;
  background: var(--bg-muted);
  animation: iskeletParlama 1.4s ease infinite;
}
.iskelet--daire {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  flex-shrink: 0;
}
.iskelet--metin {
  flex: 1;
  height: 13px;
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
  .gorev-satir,
  .iskelet {
    transition: none;
    animation: none;
  }
}
@media (max-width: 600px) {
  .gorev-satir {
    gap: 10px;
    padding: 5px 0;
  }
  .gorev-tutar {
    display: none;
  }
}
</style>
