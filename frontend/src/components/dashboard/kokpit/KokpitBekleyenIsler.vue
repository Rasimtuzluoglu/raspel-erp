<template>
  <section
    class="kokpit-bekleyen kart"
    data-test="kokpit-bekleyen-isler"
    :aria-label="t('dashboard.bekleyenIsler')"
  >
    <header class="bekleyen-baslik">
      <h2 class="bekleyen-baslik-metin">
        {{ t('dashboard.bekleyenIsler') }}
      </h2>
      <span
        v-if="ogeSayisi > 0"
        class="bekleyen-rozet"
      >{{ ogeSayisi }}</span>
    </header>

    <div
      v-if="yukleniyor && ogeler.length === 0"
      class="bekleyen-iskeletler"
      aria-hidden="true"
    >
      <span
        v-for="n in 3"
        :key="`bekleyen-iskelet-${n}`"
        class="bekleyen-iskelet"
      />
    </div>

    <p
      v-else-if="ogeler.length === 0"
      class="bekleyen-bos"
      data-test="kokpit-bekleyen-bos"
    >
      <i
        class="pi pi-check-circle"
        aria-hidden="true"
      />
      {{ t('dashboard.bekleyenIsYok') }}
    </p>

    <ul
      v-else
      class="bekleyen-liste"
    >
      <li
        v-for="oge in gorunen"
        :key="oge.anahtar"
      >
        <button
          type="button"
          class="bekleyen-satir"
          :data-test="`bekleyen-oge-${oge.anahtar}`"
          @click="emit('oge-sec', oge)"
        >
          <span
            class="bekleyen-ikon"
            :class="seviyeSinifi(oge.seviye)"
            :data-test="`bekleyen-ikon-${oge.anahtar}`"
            aria-hidden="true"
          >
            <i :class="seviyeIkonu(oge.seviye)" />
          </span>
          <span class="bekleyen-metin">{{ etiket(oge) }}</span>
          <strong class="bekleyen-deger">{{ degerMetni(oge) }}</strong>
          <i
            class="pi pi-angle-right bekleyen-ok"
            aria-hidden="true"
          />
        </button>
      </li>
    </ul>

    <button
      v-if="gizliSayi > 0"
      type="button"
      class="bekleyen-tumu"
      data-test="bekleyen-tumunu-gor"
      :aria-expanded="genisletildi"
      @click="genisletildi = !genisletildi"
    >
      <template v-if="genisletildi">
        {{ t('dashboard.akisDahaAz') }}
      </template>
      <template v-else>
        {{ t('dashboard.akisTumunuGor', { n: gizliSayi }) }}
      </template>
    </button>

    <button
      v-if="onayGorebilir"
      type="button"
      class="bekleyen-alt"
      data-test="bekleyen-onay-merkezi"
      @click="emit('onaylar')"
    >
      {{ t('dashboard.onayMerkezi') }}
      <i
        class="pi pi-arrow-right"
        aria-hidden="true"
      />
    </button>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatPara } from '../../../utils/format.js'
import { useMobilEkran } from '../../../composables/useMobilEkran.js'

const props = defineProps({
  ogeler: { type: Array, default: () => [] },
  yukleniyor: { type: Boolean, default: false },
  paraBirimi: { type: String, default: 'TRY' },
  onayGorebilir: { type: Boolean, default: false }
})

const emit = defineEmits(['oge-sec', 'onaylar'])
const { t, te } = useI18n()

const ANAHTARLAR = {
  vadesiGecen: 'dashboard.kokpitKritikSeritVadesiGecen',
  kritikStok: 'dashboard.kokpitKritikSeritKritikStok',
  bekleyenOnay: 'dashboard.kokpitKritikSeritBekleyenOnay',
  // /api/dashboard/gorevler türleri (Bekleyen İşler kartı).
  VADESI_GECEN: 'dashboard.gorevTuruVadesiGecen',
  KRITIK_STOK: 'dashboard.gorevTuruKritikStok',
  ONAY_IZIN: 'dashboard.gorevTuruOnayIzin',
  ONAY_MASRAF: 'dashboard.gorevTuruOnayMasraf',
  ONAY_SATINALMA: 'dashboard.gorevTuruOnaySatinalma',
  ONAY_SIPARIS: 'dashboard.gorevTuruOnaySiparis',
  BEKLEYEN_TESLIMAT: 'dashboard.gorevTuruBekleyenTeslimat'
}

const mobil = useMobilEkran()
const genisletildi = ref(false)

const ogeSayisi = computed(() => props.ogeler.length)

/** Mobilde ilk 3 kalem; "Tümünü gör" ile tamamı. */
const gorunen = computed(() => (
  mobil.value && !genisletildi.value ? props.ogeler.slice(0, 3) : props.ogeler
))
const gizliSayi = computed(() => props.ogeler.length - gorunen.value.length)

const etiket = (oge) => {
  const anahtar = ANAHTARLAR[oge?.anahtar]
  return anahtar && te(anahtar) ? t(anahtar) : oge?.etiket || ''
}

const seviyeSinifi = (seviye) => {
  if (seviye === 'KRITIK') return 'bekleyen-ikon--kritik'
  if (seviye === 'WARN') return 'bekleyen-ikon--uyari'
  return 'bekleyen-ikon--bilgi'
}

const seviyeIkonu = (seviye) => {
  if (seviye === 'KRITIK') return 'pi pi-exclamation-circle'
  if (seviye === 'WARN') return 'pi pi-exclamation-triangle'
  return 'pi pi-info-circle'
}

const degerMetni = (oge) => {
  const adet = Number(oge?.adet)
  if (Number.isFinite(adet) && adet > 0) return String(adet)
  if (oge?.tutar !== null && oge?.tutar !== undefined) {
    return formatPara(oge.tutar, props.paraBirimi)
  }
  return '—'
}
</script>

<style scoped>
.kokpit-bekleyen {
  display: flex;
  flex-direction: column;
  min-width: 0;
  height: 100%;
  gap: 8px;
}
.bekleyen-baslik {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 2px;
}
.bekleyen-baslik-metin {
  margin: 0;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: var(--text-secondary);
  text-transform: uppercase;
}
.bekleyen-rozet {
  min-width: 18px;
  padding: 1px 6px;
  border-radius: 999px;
  background: var(--danger-soft);
  color: var(--danger);
  font-size: 10px;
  font-weight: 700;
  text-align: center;
}
.bekleyen-liste {
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  margin: 0;
  padding: 0;
  list-style: none;
}
.bekleyen-satir {
  display: flex;
  align-items: center;
  gap: 9px;
  width: 100%;
  padding: 9px 6px;
  border: none;
  border-bottom: 1px solid var(--border);
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
  border-radius: 8px;
  transition: background var(--dur-fast) var(--ease-standard);
}
.bekleyen-liste li:last-child .bekleyen-satir {
  border-bottom: none;
}
.bekleyen-satir:hover,
.bekleyen-satir:focus-visible {
  background: var(--bg-secondary);
}
.bekleyen-satir:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 1px;
}
.bekleyen-ikon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  border-radius: 9px;
  font-size: 12px;
}
.bekleyen-ikon--kritik {
  background: var(--danger-soft);
  color: var(--danger);
}
.bekleyen-ikon--uyari {
  background: var(--warning-soft);
  color: var(--warning);
}
.bekleyen-ikon--bilgi {
  background: var(--info-soft);
  color: var(--info);
}
.bekleyen-metin {
  flex: 1;
  min-width: 0;
  font-size: 12.5px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.bekleyen-deger {
  font-size: 12.5px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--text-primary);
}
.bekleyen-ok {
  font-size: 10px;
  color: var(--text-muted);
}
.bekleyen-bos {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 6px 2px;
  color: var(--text-muted);
  font-size: 12px;
}
.bekleyen-alt {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: auto;
  padding: 7px 10px;
  border: 1px solid var(--border);
  border-radius: 9px;
  background: transparent;
  color: var(--text-secondary);
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
}
.bekleyen-tumu {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  width: 100%;
  padding: 6px 10px;
  border: 1px dashed var(--border);
  border-radius: 9px;
  background: transparent;
  color: var(--text-muted);
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
}
.bekleyen-tumu:hover,
.bekleyen-tumu:focus-visible {
  border-color: var(--accent-border);
  color: var(--accent);
}
.bekleyen-alt:hover,
.bekleyen-alt:focus-visible {
  border-color: var(--accent-border);
  color: var(--accent);
}
.bekleyen-iskeletler {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.bekleyen-iskelet {
  height: 34px;
  border-radius: 9px;
  background: var(--bg-secondary);
  animation: bekleyen-nabiz 1.4s ease-in-out infinite;
}
@keyframes bekleyen-nabiz {
  0%,
  100% {
    opacity: 0.55;
  }
  50% {
    opacity: 0.9;
  }
}
@media (prefers-reduced-motion: reduce) {
  .bekleyen-iskelet {
    animation: none;
  }
  .bekleyen-satir {
    transition: none;
  }
}
</style>
