<template>
  <FinansKarti
    :baslik="t('dashboard.kokpitKarMarjBaslik')"
    :alt-baslik="t('dashboard.kokpitKarMarjAltBaslik')"
    ikon="pi pi-chart-line"
    :yukleniyor="yukleniyor"
    :hata="hata"
    data-test="kar-marj-bolumu"
  >
    <template #ust>
      <router-link
        v-if="veriVar"
        to="/raporlar"
        class="bolum-hizli-link"
        data-test="kar-rapor-link"
      >
        {{ t('dashboard.raporlar') }}
        <i
          class="pi pi-arrow-up-right"
          aria-hidden="true"
        />
      </router-link>
    </template>

    <div
      v-if="!veriVar"
      class="kar-bos"
      data-test="kar-yetkisiz"
    >
      <i
        class="pi pi-lock"
        aria-hidden="true"
      />
      <span>{{ t('dashboard.kokpitKarMarjYetkisiz') }}</span>
    </div>

    <template v-else>
      <div class="kar-ozet">
        <div class="kar-stat">
          <span class="kar-stat-etiket">{{ t('dashboard.kokpitKarMarjCiro') }}</span>
          <strong class="kar-stat-deger gizli-veri">{{ formatPara(ciro, paraBirimi) }}</strong>
        </div>
        <div class="kar-stat">
          <span class="kar-stat-etiket">{{ t('dashboard.kokpitKarMarjMaliyet') }}</span>
          <strong class="kar-stat-deger gizli-veri">{{ formatPara(maliyet, paraBirimi) }}</strong>
        </div>
        <div class="kar-stat kar-stat--kar">
          <span class="kar-stat-etiket">{{ t('dashboard.kokpitKarMarjKar') }}</span>
          <strong class="kar-stat-deger gizli-veri">{{ formatPara(kar, paraBirimi) }}</strong>
        </div>
        <div class="kar-stat">
          <span class="kar-stat-etiket">{{ t('dashboard.kokpitKarMarjMarj') }}</span>
          <strong class="kar-stat-deger">{{ marjMetni }}</strong>
        </div>
      </div>

      <div class="kar-trend">
        <div class="kar-trend-ust">
          <span class="kar-bolum-baslik">{{ t('dashboard.kokpitKarMarjTrend') }}</span>
          <span class="kar-trend-legend">
            <span class="kar-legend-item kar-legend-item--ciro">{{ t('dashboard.kokpitKarMarjCiro') }}</span>
            <span class="kar-legend-item kar-legend-item--maliyet">{{ t('dashboard.kokpitKarMarjMaliyet') }}</span>
            <span class="kar-legend-item kar-legend-item--kar">{{ t('dashboard.kokpitKarMarjKar') }}</span>
          </span>
        </div>
        <MikroGrafik
          v-if="trend.length > 0"
          tip="line"
          :seriler="trendSerileri"
          :yukseklik="64"
          :aria-label="t('dashboard.kokpitKarMarjTrend')"
          data-test="kar-trend-grafik"
        />
        <p
          v-else
          class="kar-bos-metin"
        >
          {{ t('dashboard.kokpitKarMarjBos') }}
        </p>
        <div
          v-if="marjSeri.length > 1"
          class="kar-marj-trend"
        >
          <span class="kar-marj-trend-etiket">{{ t('dashboard.kokpitKarMarjMarj') }}</span>
          <MikroGrafik
            :seriler="[{ degerler: marjSeri, renk: 'var(--info)' }]"
            :yukseklik="28"
            :aria-label="t('dashboard.kokpitKarMarjMarj')"
            data-test="kar-marj-grafik"
          />
        </div>
      </div>

      <div
        v-if="maliyetsizKalem > 0"
        class="kar-uyari"
        data-test="kar-maliyetsiz-uyari"
      >
        <i
          class="pi pi-exclamation-triangle"
          aria-hidden="true"
        />
        <span>{{ t('dashboard.kokpitKarMarjMaliyetsizKalem', { n: maliyetsizKalem }) }}</span>
      </div>

      <div class="kar-tablolar">
        <div class="kar-tablo">
          <span class="kar-bolum-baslik">{{ t('dashboard.kokpitKarMarjUrunler') }}</span>
          <MiniTablo
            :kolonlar="kalemKolonlari"
            :satirlar="urunler"
            :bos-metin="t('dashboard.kokpitKarMarjBos')"
            :para-birimi="paraBirimi"
            :baslik="t('dashboard.kokpitKarMarjUrunler')"
            data-test="kar-urunler-tablo"
            @satir-sec="urunDetay"
          />
        </div>
        <div class="kar-tablo">
          <span class="kar-bolum-baslik">{{ t('dashboard.kokpitKarMarjCariler') }}</span>
          <MiniTablo
            :kolonlar="kalemKolonlari"
            :satirlar="cariler"
            :bos-metin="t('dashboard.kokpitKarMarjBos')"
            :para-birimi="paraBirimi"
            :baslik="t('dashboard.kokpitKarMarjCariler')"
            data-test="kar-cariler-tablo"
            @satir-sec="cariDetay"
          />
        </div>
      </div>
    </template>

    <template
      v-if="veriVar"
      #alt
    >
      <button
        type="button"
        class="finans-detay-buton"
        data-test="kar-detay"
        @click="emit('detay', hedef)"
      >
        {{ t('dashboard.kokpitTumunuGor') }}
        <i
          class="pi pi-arrow-right"
          aria-hidden="true"
        />
      </button>
    </template>
  </FinansKarti>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import FinansKarti from './FinansKarti.vue'
import MikroGrafik from './MikroGrafik.vue'
import MiniTablo from './MiniTablo.vue'
import { formatPara } from '../../../utils/format.js'

const props = defineProps({
  veri: { type: Object, default: null },
  yukleniyor: { type: Boolean, default: false },
  hata: { type: String, default: null },
  paraBirimi: { type: String, default: 'TRY' }
})

const emit = defineEmits(['detay'])
const { t } = useI18n()

const veriVar = computed(() => !!props.veri)
const ciro = computed(() => Number(props.veri?.ciro) || 0)
const maliyet = computed(() => Number(props.veri?.maliyet) || 0)
const kar = computed(() => {
  const deger = Number(props.veri?.kar)
  return Number.isFinite(deger) ? deger : ciro.value - maliyet.value
})
const maliyetsizKalem = computed(() => Number(props.veri?.maliyetsizKalem) || 0)

const marjMetni = computed(() => {
  if (props.veri?.marj === null || props.veri?.marj === undefined) return '—'
  const deger = Number(props.veri.marj)
  if (!Number.isFinite(deger)) return '—'
  return `${deger.toLocaleString('tr-TR', { maximumFractionDigits: 1 })}%`
})

const trend = computed(() => Array.isArray(props.veri?.trend) ? props.veri.trend : [])
const trendSerileri = computed(() => [
  {
    ad: t('dashboard.kokpitKarMarjCiro'),
    degerler: trend.value.map((ay) => Number(ay.ciro) || 0),
    renk: 'var(--accent)'
  },
  {
    ad: t('dashboard.kokpitKarMarjMaliyet'),
    degerler: trend.value.map((ay) => Number(ay.maliyet) || 0),
    renk: 'var(--warning)'
  },
  {
    ad: t('dashboard.kokpitKarMarjKar'),
    degerler: trend.value.map((ay) => Number(ay.kar) || 0),
    renk: 'var(--success)'
  }
])
const marjSeri = computed(() => trend.value.map((ay) => Number(ay.marj) || 0))

const urunler = computed(() =>
  (Array.isArray(props.veri?.urunler) ? props.veri.urunler : []).slice(0, 5)
)
const cariler = computed(() =>
  (Array.isArray(props.veri?.cariler) ? props.veri.cariler : []).slice(0, 5)
)

const kalemKolonlari = [
  { anahtar: 'ad', etiket: t('dashboard.kokpitAd') },
  { anahtar: 'kar', etiket: t('dashboard.kokpitKarMarjKar'), hiza: 'sag', bicim: 'para' },
  { anahtar: 'marj', etiket: t('dashboard.kokpitKarMarjMarj'), hiza: 'sag', bicim: 'yuzde', mobilGizle: true }
]

const hedef = computed(() => props.veri?.hedef || { route: '/raporlar', query: {} })

const urunDetay = (satir) => {
  if (!satir?.id) return
  emit('detay', { route: '/raporlar', query: { urunId: String(satir.id) } })
}

const cariDetay = (satir) => {
  if (!satir?.id) return
  emit('detay', { route: '/raporlar', query: { cariId: String(satir.id) } })
}
</script>

<style scoped>
.kar-bos {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 22px 8px;
  border: 1px dashed var(--border);
  border-radius: var(--radius-sm);
  font-size: 12px;
  color: var(--text-muted);
}
.bolum-hizli-link {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 10.5px;
  font-weight: 600;
  color: var(--accent);
  text-decoration: none;
  white-space: nowrap;
}
.bolum-hizli-link:hover,
.bolum-hizli-link:focus-visible {
  color: var(--accent-hover);
}
.bolum-hizli-link:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.bolum-hizli-link i {
  font-size: 8px;
}
.kar-ozet {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
}
.kar-stat {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
  padding: 7px 8px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-muted);
}
.kar-stat--kar {
  border-color: var(--success-border);
  background: var(--success-soft);
}
.kar-stat-etiket {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 10px;
  font-weight: 600;
  color: var(--text-muted);
}
.kar-stat-deger {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.kar-trend {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.kar-trend-ust {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}
.kar-bolum-baslik {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-muted);
}
.kar-trend-legend {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 9.5px;
  color: var(--text-muted);
}
.kar-legend-item {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}
.kar-legend-item::before {
  content: '';
  width: 6px;
  height: 2px;
  border-radius: var(--radius-pill);
}
.kar-legend-item--ciro::before {
  background: var(--accent);
}
.kar-legend-item--maliyet::before {
  background: var(--warning);
}
.kar-legend-item--kar::before {
  background: var(--success);
}
.kar-bos-metin {
  margin: 0;
  padding: 10px 6px;
  text-align: center;
  font-size: 11.5px;
  color: var(--text-muted);
}
.kar-marj-trend {
  display: flex;
  align-items: center;
  gap: 7px;
}
.kar-marj-trend-etiket {
  flex-shrink: 0;
  font-size: 10px;
  color: var(--text-muted);
}
.kar-marj-trend .mikro-grafik {
  flex: 1;
}
.kar-uyari {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px;
  border: 1px solid var(--warning-border);
  border-radius: var(--radius-sm);
  background: var(--warning-soft);
  color: var(--warning);
  font-size: 11px;
}
.kar-tablolar {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  min-width: 0;
}
.kar-tablo {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}
@media (max-width: 760px) {
  .kar-ozet {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .kar-tablolar {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
