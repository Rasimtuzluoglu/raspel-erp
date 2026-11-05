<template>
  <div
    v-if="visible"
    class="db-drawer-katman"
    data-test="kar-analizi-katman"
    @click.self="kapat"
  >
    <aside
      ref="panel"
      class="db-drawer db-kar-drawer"
      role="dialog"
      aria-modal="true"
      :aria-labelledby="baslikId"
      tabindex="-1"
      :aria-busy="yukleniyor ? 'true' : 'false'"
      data-test="kar-analizi-drawer"
      @keydown="tusYonet"
    >
      <header class="db-drawer-ust">
        <div class="db-drawer-baslik-alan">
          <h2
            :id="baslikId"
            class="db-drawer-baslik"
          >
            {{ t('dashboard.karAnalizi') }}
          </h2>
          <p class="db-drawer-alt">
            <span data-test="kar-analizi-donem">{{ donemEtiketi }}</span>
            <span v-if="donemAraligi"> · {{ donemAraligi }}</span>
          </p>
        </div>
        <button
          type="button"
          class="db-drawer-kapat"
          :aria-label="t('common.close')"
          :title="t('common.close')"
          @click="kapat"
        >
          <i
            class="pi pi-times"
            aria-hidden="true"
          />
        </button>
      </header>

      <div
        v-if="yukleniyor && !veri"
        class="kar-iskelet"
        aria-hidden="true"
        data-test="kar-analizi-iskelet"
      >
        <div class="kar-iskelet-ozet">
          <span
            v-for="i in 4"
            :key="`kar-iskelet-ozet-${i}`"
            class="kar-iskelet-hucre"
          />
        </div>
        <span class="kar-iskelet-grafik" />
        <span class="kar-iskelet-satir" />
        <span class="kar-iskelet-grafik" />
      </div>

      <div
        v-else-if="hata"
        class="kar-durum kar-durum--hata"
        role="alert"
        data-test="kar-analizi-hata"
      >
        <span>
          <i
            class="pi pi-exclamation-circle"
            aria-hidden="true"
          />
          {{ t('dashboard.karAnaliziHata') }}
        </span>
        <button
          type="button"
          class="kar-btn"
          data-test="kar-analizi-yeniden-dene"
          @click="yukle"
        >
          {{ t('dashboard.yenidenDene') }}
        </button>
      </div>

      <template v-else-if="veri">
        <p
          v-if="maliyetsizKalem > 0"
          class="kar-uyari"
          role="status"
          data-test="kar-analizi-maliyetsiz-uyari"
        >
          <i
            class="pi pi-exclamation-triangle"
            aria-hidden="true"
          />
          <span>{{ t('dashboard.karAnaliziMaliyetsizUyari', { n: maliyetsizKalem }) }}</span>
        </p>

        <div
          class="kar-ozet"
          role="group"
          :aria-label="t('dashboard.karAnaliziOzet')"
        >
          <div class="kar-ozet-hucre">
            <span class="kar-ozet-etiket">{{ t('dashboard.karAnaliziCiro') }}</span>
            <strong
              class="kar-ozet-deger gizli-veri"
              data-test="kar-analizi-ciro"
            >{{ para(ozet.ciro) }}</strong>
          </div>
          <div class="kar-ozet-hucre">
            <span class="kar-ozet-etiket">{{ t('dashboard.karAnaliziMaliyet') }}</span>
            <strong
              class="kar-ozet-deger gizli-veri"
              data-test="kar-analizi-maliyet"
            >{{ para(ozet.maliyet) }}</strong>
          </div>
          <div class="kar-ozet-hucre">
            <span class="kar-ozet-etiket">{{ t('dashboard.karAnaliziKar') }}</span>
            <strong
              class="kar-ozet-deger kar-ozet-deger--kar gizli-veri"
              data-test="kar-analizi-kar"
            >{{ para(ozet.kar) }}</strong>
          </div>
          <div class="kar-ozet-hucre">
            <span class="kar-ozet-etiket">{{ t('dashboard.karAnaliziMarj') }}</span>
            <strong
              class="kar-ozet-deger"
              data-test="kar-analizi-marj"
            >{{ marjMetni(ozet.marj) }}</strong>
          </div>
        </div>

        <div
          v-if="bosMu"
          class="kar-durum kar-durum--bos"
          data-test="kar-analizi-bos"
        >
          <i
            class="pi pi-chart-line"
            aria-hidden="true"
          />
          <span>{{ t('dashboard.karAnaliziVeriYok') }}</span>
        </div>

        <template v-else>
          <section
            v-if="urunler.length"
            class="kar-bolum"
            :aria-labelledby="urunBaslikId"
            data-test="kar-analizi-urunler"
          >
            <h3
              :id="urunBaslikId"
              class="kar-bolum-baslik"
            >
              {{ t('dashboard.karAnaliziEnKarliUrunler') }}
            </h3>
            <div
              class="kar-grafik"
              :style="{ height: `${grafikYuksekligi(urunler)}px` }"
            >
              <Bar
                :data="urunGrafik"
                :options="grafikOpsiyonlari"
              />
            </div>
            <div class="kar-tablo-sarmal">
              <table
                class="kar-tablo"
                data-test="kar-analizi-urun-tablo"
              >
                <caption class="sr-only">
                  {{ t('dashboard.karAnaliziEnKarliUrunler') }}
                </caption>
                <thead>
                  <tr>
                    <th scope="col">
                      {{ t('dashboard.karAnaliziUrun') }}
                    </th>
                    <th
                      scope="col"
                      class="kar-sayi"
                    >
                      {{ t('dashboard.karAnaliziCiro') }}
                    </th>
                    <th
                      scope="col"
                      class="kar-sayi"
                    >
                      {{ t('dashboard.karAnaliziMaliyet') }}
                    </th>
                    <th
                      scope="col"
                      class="kar-sayi"
                    >
                      {{ t('dashboard.karAnaliziKar') }}
                    </th>
                    <th
                      scope="col"
                      class="kar-sayi"
                    >
                      {{ t('dashboard.karAnaliziMarj') }}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="(urun, sira) in urunler.slice(0, 10)"
                    :key="urun.stokId ?? `urun-${sira}`"
                    :data-test="`kar-urun-satir-${sira}`"
                  >
                    <th
                      scope="row"
                      class="kar-ad"
                      :title="urun.ad || ''"
                    >
                      {{ urun.ad || '—' }}
                    </th>
                    <td class="kar-sayi gizli-veri">
                      {{ para(urun.ciro) }}
                    </td>
                    <td class="kar-sayi gizli-veri">
                      {{ para(urun.maliyet) }}
                    </td>
                    <td class="kar-sayi kar-sayi--kar gizli-veri">
                      {{ para(urun.kar) }}
                    </td>
                    <td class="kar-sayi">
                      {{ marjMetni(urun.marj) }}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>

          <section
            v-if="cariler.length"
            class="kar-bolum"
            :aria-labelledby="cariBaslikId"
            data-test="kar-analizi-cariler"
          >
            <h3
              :id="cariBaslikId"
              class="kar-bolum-baslik"
            >
              {{ t('dashboard.karAnaliziEnKarliCariler') }}
            </h3>
            <div
              class="kar-grafik"
              :style="{ height: `${grafikYuksekligi(cariler)}px` }"
            >
              <Bar
                :data="cariGrafik"
                :options="grafikOpsiyonlari"
              />
            </div>
            <div class="kar-tablo-sarmal">
              <table
                class="kar-tablo"
                data-test="kar-analizi-cari-tablo"
              >
                <caption class="sr-only">
                  {{ t('dashboard.karAnaliziEnKarliCariler') }}
                </caption>
                <thead>
                  <tr>
                    <th scope="col">
                      {{ t('dashboard.karAnaliziCari') }}
                    </th>
                    <th
                      scope="col"
                      class="kar-sayi"
                    >
                      {{ t('dashboard.karAnaliziCiro') }}
                    </th>
                    <th
                      scope="col"
                      class="kar-sayi"
                    >
                      {{ t('dashboard.karAnaliziMaliyet') }}
                    </th>
                    <th
                      scope="col"
                      class="kar-sayi"
                    >
                      {{ t('dashboard.karAnaliziKar') }}
                    </th>
                    <th
                      scope="col"
                      class="kar-sayi"
                    >
                      {{ t('dashboard.karAnaliziMarj') }}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="(cari, sira) in cariler.slice(0, 10)"
                    :key="cari.cariId ?? `cari-${sira}`"
                    :data-test="`kar-cari-satir-${sira}`"
                  >
                    <th
                      scope="row"
                      class="kar-ad"
                      :title="cari.ad || ''"
                    >
                      {{ cari.ad || '—' }}
                    </th>
                    <td class="kar-sayi gizli-veri">
                      {{ para(cari.ciro) }}
                    </td>
                    <td class="kar-sayi gizli-veri">
                      {{ para(cari.maliyet) }}
                    </td>
                    <td class="kar-sayi kar-sayi--kar gizli-veri">
                      {{ para(cari.kar) }}
                    </td>
                    <td class="kar-sayi">
                      {{ marjMetni(cari.marj) }}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        </template>
      </template>
    </aside>
  </div>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Bar } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  Tooltip
} from 'chart.js'
import { dashboardAPI } from '../../api/index.js'
import { useChartTema } from '../../composables/useChartTema.js'
import { formatCurrency, formatDate } from '../../utils/format.js'

ChartJS.register(CategoryScale, LinearScale, BarElement, Tooltip)

const props = defineProps({
  visible: { type: Boolean, default: false },
  /** BU_AY | GECEN_AY | BU_CEYREK | BU_YIL */
  donem: { type: String, default: 'BU_AY' }
})

const emit = defineEmits(['update:visible'])

const { t } = useI18n()
const { palet } = useChartTema()

const baslikId = 'db-kar-analizi-baslik'
const urunBaslikId = 'db-kar-analizi-urunler-baslik'
const cariBaslikId = 'db-kar-analizi-cariler-baslik'

const DONEM_ANAHTARLARI = {
  BU_AY: 'dashboard.donemBuAy',
  GECEN_AY: 'dashboard.donemGecenAy',
  BU_CEYREK: 'dashboard.donemBuCeyrek',
  BU_YIL: 'dashboard.donemBuYil'
}

const panel = ref(null)
let oncekiOdak = null
let istekSayaci = 0

const veri = ref(null)
const yukleniyor = ref(false)
const hata = ref('')

const ozet = computed(() => veri.value?.ozet || {})
const urunler = computed(() => (Array.isArray(veri.value?.urunler) ? veri.value.urunler : []))
const cariler = computed(() => (Array.isArray(veri.value?.cariler) ? veri.value.cariler : []))
const maliyetsizKalem = computed(() => Number(ozet.value.maliyetsizKalem) || 0)
const bosMu = computed(() => !urunler.value.length && !cariler.value.length)

const donemEtiketi = computed(() => {
  const etiket = veri.value?.donem?.etiket
  if (etiket) return etiket
  const anahtar = DONEM_ANAHTARLARI[props.donem]
  return anahtar ? t(anahtar) : props.donem
})

const donemAraligi = computed(() => {
  const baslangic = veri.value?.donem?.baslangic
  const bitis = veri.value?.donem?.bitis
  if (!baslangic || !bitis) return ''
  return `${formatDate(baslangic)} – ${formatDate(bitis)}`
})

const para = (deger) => {
  if (deger === null || deger === undefined || deger === '') return '—'
  const sayi = Number(deger)
  if (!Number.isFinite(sayi)) return '—'
  return formatCurrency(sayi)
}

const marjMetni = (marj) => {
  if (marj === null || marj === undefined || marj === '') return '—'
  const sayi = Number(marj)
  if (!Number.isFinite(sayi)) return '—'
  return `%${sayi.toFixed(1)}`
}

const eksenPara = (deger) => {
  const sayi = Number(deger) || 0
  return new Intl.NumberFormat('tr-TR', {
    notation: 'compact',
    maximumFractionDigits: 1
  }).format(sayi)
}

const grafikVerisi = (liste) => {
  const ilkOn = liste.slice(0, 10)
  return {
    labels: ilkOn.map((kayit) => kayit.ad || '—'),
    datasets: [
      {
        label: t('dashboard.karAnaliziKar'),
        data: ilkOn.map((kayit) => Number(kayit.kar) || 0),
        backgroundColor: palet.value.vurgu,
        hoverBackgroundColor: palet.value.vurgu,
        borderRadius: 6,
        maxBarThickness: 18
      }
    ]
  }
}

const urunGrafik = computed(() => grafikVerisi(urunler.value))
const cariGrafik = computed(() => grafikVerisi(cariler.value))

const grafikOpsiyonlari = computed(() => ({
  indexAxis: 'y',
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
    tooltip: {
      callbacks: {
        label: (ctx) => `${t('dashboard.karAnaliziKar')}: ${formatCurrency(ctx.parsed.x)}`
      }
    }
  },
  scales: {
    x: {
      beginAtZero: true,
      ticks: { color: palet.value.metin, callback: eksenPara },
      grid: { color: palet.value.izgara }
    },
    y: {
      ticks: { color: palet.value.metin, autoSkip: false },
      grid: { display: false }
    }
  }
}))

const grafikYuksekligi = (liste) => Math.max(160, Math.min(liste.length, 10) * 28 + 48)

const yukle = async () => {
  if (!props.visible) return
  const istek = ++istekSayaci
  yukleniyor.value = true
  hata.value = ''
  veri.value = null
  try {
    const cevap = await dashboardAPI.karAnalizi({ donem: props.donem })
    if (istek !== istekSayaci) return
    veri.value = cevap?.data || null
  } catch (err) {
    if (istek !== istekSayaci) return
    hata.value = err?.response?.data?.message || err?.message || t('dashboard.karAnaliziHata')
  } finally {
    if (istek === istekSayaci) yukleniyor.value = false
  }
}

const kapat = () => emit('update:visible', false)

const tusYonet = (olay) => {
  if (olay.key === 'Escape') {
    kapat()
    return
  }
  if (olay.key !== 'Tab') return
  const ogeler = Array.from(
    panel.value?.querySelectorAll(
      'button:not(:disabled), [href], input:not(:disabled), textarea:not(:disabled), select:not(:disabled), [tabindex]:not([tabindex="-1"])'
    ) || []
  )
  if (!ogeler.length) return
  const ilk = ogeler[0]
  const son = ogeler[ogeler.length - 1]
  if (olay.shiftKey && document.activeElement === ilk) {
    olay.preventDefault()
    son.focus()
  } else if (!olay.shiftKey && document.activeElement === son) {
    olay.preventDefault()
    ilk.focus()
  }
}

watch(
  () => props.visible,
  (acik) => {
    if (acik) {
      oncekiOdak = typeof document !== 'undefined' ? document.activeElement : null
      yukle()
      nextTick(() => panel.value?.focus?.())
    } else if (oncekiOdak && typeof oncekiOdak.focus === 'function') {
      oncekiOdak.focus?.()
      oncekiOdak = null
    }
  },
  { immediate: true }
)

// Drawer acikken donem degisirse ayni panelde veri tazelenir.
watch(
  () => props.donem,
  () => {
    if (props.visible) yukle()
  }
)
</script>

<style scoped>
/* Kabuk (.db-drawer-katman / .db-drawer / baslik) dashboard.css icinde
   tanimlidir; burada yalnizca kar analizine ozgu stiller bulunur. */
.db-kar-drawer {
  width: 560px;
}

/* --- Durumlar ------------------------------------------------------------ */
.kar-iskelet {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.kar-iskelet-ozet {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}
.kar-iskelet-hucre {
  height: 62px;
  border-radius: 10px;
  background: var(--bg-muted);
  animation: karParlama 1.4s ease infinite;
}
.kar-iskelet-grafik {
  height: 168px;
  border-radius: 10px;
  background: var(--bg-muted);
  animation: karParlama 1.4s ease infinite;
}
.kar-iskelet-satir {
  height: 14px;
  border-radius: 6px;
  background: var(--bg-muted);
  animation: karParlama 1.4s ease infinite;
}
.kar-durum {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin: 0;
  padding: 14px 4px;
  font-size: 12.5px;
  color: var(--text-muted);
}
.kar-durum--hata {
  color: var(--danger);
  justify-content: space-between;
}
.kar-durum--bos {
  flex-direction: column;
  justify-content: center;
  gap: 6px;
  padding: 28px 12px;
  border: 1px dashed var(--border);
  border-radius: 12px;
  text-align: center;
}
.kar-durum--bos i {
  font-size: 22px;
  color: var(--text-muted);
  opacity: 0.7;
}
.kar-btn {
  min-height: 30px;
  padding: 0 10px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 11.5px;
  font-weight: 700;
  cursor: pointer;
  transition:
    border-color var(--dur-base, 0.2s) var(--ease-standard, ease),
    color var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.kar-btn:hover {
  color: var(--accent);
  border-color: var(--accent-border);
}
.kar-btn:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

/* --- Maliyetsiz kalem uyarisi -------------------------------------------- */
.kar-uyari {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin: 0;
  padding: 9px 11px;
  border: 1px solid var(--warning-border);
  border-radius: 10px;
  background: var(--warning-soft);
  color: var(--text-primary);
  font-size: 11.5px;
  line-height: 1.45;
}
.kar-uyari i {
  flex-shrink: 0;
  margin-top: 1px;
  color: var(--warning);
}

/* --- Ozet ---------------------------------------------------------------- */
.kar-ozet {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}
.kar-ozet-hucre {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-muted);
}
.kar-ozet-etiket {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--text-muted);
}
.kar-ozet-deger {
  font-size: 13.5px;
  font-weight: 800;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.kar-ozet-deger--kar {
  color: var(--accent);
}

/* --- Bolum / grafik / tablo --------------------------------------------- */
.kar-bolum {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 4px;
  border-top: 1px solid var(--border);
}
.kar-bolum-baslik {
  margin: 0;
  font-size: 12.5px;
  font-weight: 700;
  color: var(--text-primary);
}
.kar-grafik {
  position: relative;
  width: 100%;
  min-width: 0;
}
.kar-tablo-sarmal {
  width: 100%;
  overflow-x: auto;
}
.kar-tablo {
  width: 100%;
  border-collapse: collapse;
  font-size: 11.5px;
}
.kar-tablo thead th {
  position: sticky;
  top: 0;
  padding: 6px 8px;
  border-bottom: 1px solid var(--border);
  background: var(--bg-card);
  color: var(--text-muted);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.03em;
  text-transform: uppercase;
  text-align: left;
  white-space: nowrap;
}
.kar-tablo tbody th,
.kar-tablo tbody td {
  padding: 7px 8px;
  border-bottom: 1px solid var(--border);
  color: var(--text-secondary);
  font-variant-numeric: tabular-nums;
}
.kar-tablo tbody tr:last-child th,
.kar-tablo tbody tr:last-child td {
  border-bottom: none;
}
.kar-tablo tbody tr:hover th,
.kar-tablo tbody tr:hover td {
  background: var(--bg-muted);
}
.kar-tablo .kar-ad {
  max-width: 180px;
  color: var(--text-primary);
  font-weight: 600;
  text-align: left;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.kar-tablo .kar-sayi {
  text-align: right;
  white-space: nowrap;
}
.kar-tablo .kar-sayi--kar {
  color: var(--accent);
  font-weight: 700;
}

@keyframes karParlama {
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
  .kar-iskelet-hucre,
  .kar-iskelet-grafik,
  .kar-iskelet-satir,
  .kar-btn {
    animation: none;
    transition: none;
  }
}
@media (max-width: 600px) {
  .db-kar-drawer {
    width: 92vw;
  }
  .kar-iskelet-ozet,
  .kar-ozet {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .kar-ozet-deger {
    font-size: 12.5px;
  }
  .kar-tablo {
    font-size: 11px;
  }
}
</style>
