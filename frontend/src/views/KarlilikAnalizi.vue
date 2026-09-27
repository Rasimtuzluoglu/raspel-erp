<template>
  <div class="karlilik-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        {{ t('karlilik.title') }}
      </h1>
      <div class="filtreler">
        <SelectButton
          v-model="grup"
          :options="grupSecenekleri"
          option-label="label"
          option-value="value"
          size="small"
          @change="yukle"
        />
        <DatePicker
          v-model="baslangic"
          date-format="dd.mm.yy"
          :placeholder="t('karlilik.baslangic')"
          class="tarih-girdi"
          @date-select="yukle"
        />
        <DatePicker
          v-model="bitis"
          date-format="dd.mm.yy"
          :placeholder="t('karlilik.bitis')"
          class="tarih-girdi"
          @date-select="yukle"
        />
        <Button
          icon="pi pi-refresh"
          :aria-label="$t('common.refresh')"
          :loading="yukleniyor"
          @click="yukle"
        />
      </div>
    </div>

    <div
      v-if="yukleniyor && !veri"
      class="yukleniyor"
    >
      <i class="pi pi-spin pi-spinner" /> {{ t('common.loading') }}
    </div>

    <template v-if="veri">
      <div class="kpi-grid">
        <KpiKart
          :baslik="t('karlilik.ciro')"
          :deger="veri.ozet.ciro"
          ikon="pi pi-shopping-cart"
          renk="#3b82f6"
        />
        <KpiKart
          :baslik="t('karlilik.maliyet')"
          :deger="veri.ozet.maliyet"
          ikon="pi pi-box"
          renk="#f59e0b"
        />
        <KpiKart
          :baslik="t('karlilik.brutKar')"
          :deger="veri.ozet.brutKar"
          ikon="pi pi-chart-line"
          renk="#10b981"
        />
        <KpiKart
          :baslik="t('karlilik.brutKarMarji')"
          :deger="veri.ozet.brutKarMarji"
          ikon="pi pi-percentage"
          renk="#8b5cf6"
          :para-birimi="false"
        />
      </div>

      <div
        v-if="!veri.aylikTrend?.length && !veri.kirilim?.length"
        class="bos-durum"
      >
        <EmptyState
          icon="pi pi-chart-line"
          :message="t('karlilik.veriYok')"
        />
      </div>

      <div class="grafik-grid">
        <Card class="grafik-kart">
          <template #title>
            {{ t('karlilik.aylikTrend') }}
          </template>
          <div class="grafik-yukseklik">
            <Line
              :data="trendData"
              :options="lineOptions"
            />
          </div>
        </Card>
        <Card class="grafik-kart">
          <template #title>
            {{ t('karlilik.kirilimGrafik') }}
          </template>
          <div class="grafik-yukseklik">
            <Bar
              :data="kirilimData"
              :options="barOptions"
            />
          </div>
        </Card>
        <Card class="grafik-kart">
          <template #title>
            {{ t('karlilik.ciroPayi') }}
          </template>
          <div class="grafik-yukseklik">
            <Doughnut
              :data="payData"
              :options="pieOptions"
            />
          </div>
        </Card>
      </div>

      <Card
        v-if="veri.negatifMarjli && veri.negatifMarjli.length"
        class="uyari-kart"
      >
        <template #title>
          <span class="uyari-baslik"><i class="pi pi-exclamation-triangle" /> {{ t('karlilik.negatifMarj') }}</span>
        </template>
        <div
          v-for="n in veri.negatifMarjli"
          :key="n.ad"
          class="uyari-satir"
        >
          <span class="uyari-ad">{{ n.ad }}</span>
          <span class="uyari-marj">%{{ fmt(n.marj) }}</span>
          <span class="uyari-tutar">{{ formatCurrency(n.brutKar) }}</span>
        </div>
      </Card>

      <Card class="tablo-kart">
        <template #title>
          {{ t('karlilik.kirilim') }}
        </template>
        <DataTable
          :value="veri.kirilim"
          :rows="15"
          paginator
          striped-rows
          size="small"
          sort-field="brutKar"
          :sort-order="-1"
          class="kirilim-tablo"
          @row-click="detayAc($event.data)"
        >
          <template #empty>
            <EmptyState />
          </template>
          <Column
            field="ad"
            :header="alanBasligi"
            sortable
          />
          <Column
            field="ciro"
            :header="t('karlilik.ciro')"
            sortable
          >
            <template #body="{ data }">
              {{ formatCurrency(data.ciro) }}
            </template>
          </Column>
          <Column
            field="maliyet"
            :header="t('karlilik.maliyet')"
            sortable
          >
            <template #body="{ data }">
              {{ formatCurrency(data.maliyet) }}
            </template>
          </Column>
          <Column
            field="brutKar"
            :header="t('karlilik.brutKar')"
            sortable
          >
            <template #body="{ data }">
              <span :class="data.brutKar < 0 ? 'negatif' : 'pozitif'">{{ formatCurrency(data.brutKar) }}</span>
            </template>
          </Column>
          <Column
            field="marj"
            :header="t('karlilik.marj')"
            sortable
          >
            <template #body="{ data }">
              <span :class="data.marj < 0 ? 'negatif' : 'pozitif'">%{{ fmt(data.marj) }}</span>
            </template>
          </Column>
          <Column
            field="pay"
            :header="t('karlilik.pay')"
            sortable
          >
            <template #body="{ data }">
              %{{ fmt(data.pay) }}
            </template>
          </Column>
        </DataTable>
      </Card>
    </template>

    <Dialog
      v-model:visible="detayDialog"
      :header="detayBaslik"
      modal
      :style="{ width: '900px', maxWidth: '96vw' }"
    >
      <div
        v-if="detayYukleniyor"
        class="yukleniyor"
      >
        <i class="pi pi-spin pi-spinner" /> {{ t('common.loading') }}
      </div>
      <template v-else-if="detayVeri">
        <div class="detay-ozet">
          <span>{{ t('karlilik.ciro') }}: <strong>{{ formatCurrency(detayVeri.ciro) }}</strong></span>
          <span>{{ t('karlilik.maliyet') }}: <strong>{{ formatCurrency(detayVeri.maliyet) }}</strong></span>
          <span>{{ t('karlilik.brutKar') }}:
            <strong :class="detayVeri.brutKar < 0 ? 'negatif' : 'pozitif'">{{ formatCurrency(detayVeri.brutKar) }}</strong>
          </span>
          <span>{{ t('karlilik.marj') }}:
            <strong :class="detayVeri.marj < 0 ? 'negatif' : 'pozitif'">%{{ fmt(detayVeri.marj) }}</strong>
          </span>
        </div>
        <TabView>
          <TabPanel :header="t('karlilik.altKirilim')">
            <DataTable
              :value="detayVeri.altKirilim"
              striped-rows
              size="small"
              scrollable
              scroll-height="360px"
            >
              <template #empty>
                <EmptyState />
              </template>
              <Column
                field="ad"
                :header="altGrupBasligi"
              />
              <Column
                field="ciro"
                :header="t('karlilik.ciro')"
              >
                <template #body="{ data }">
                  {{ formatCurrency(data.ciro) }}
                </template>
              </Column>
              <Column
                field="maliyet"
                :header="t('karlilik.maliyet')"
              >
                <template #body="{ data }">
                  {{ formatCurrency(data.maliyet) }}
                </template>
              </Column>
              <Column
                field="brutKar"
                :header="t('karlilik.brutKar')"
              >
                <template #body="{ data }">
                  <span :class="data.brutKar < 0 ? 'negatif' : 'pozitif'">{{ formatCurrency(data.brutKar) }}</span>
                </template>
              </Column>
              <Column
                field="marj"
                :header="t('karlilik.marj')"
              >
                <template #body="{ data }">
                  <span :class="data.marj < 0 ? 'negatif' : 'pozitif'">%{{ fmt(data.marj) }}</span>
                </template>
              </Column>
              <Column
                field="pay"
                :header="t('karlilik.pay')"
              >
                <template #body="{ data }">
                  %{{ fmt(data.pay) }}
                </template>
              </Column>
            </DataTable>
          </TabPanel>
          <TabPanel :header="t('karlilik.belgeler')">
            <DataTable
              :value="detayVeri.belgeler"
              striped-rows
              size="small"
              scrollable
              scroll-height="360px"
              :paginator="detayVeri.belgeler.length > 12"
              :rows="12"
            >
              <template #empty>
                <EmptyState />
              </template>
              <Column
                field="tarih"
                :header="t('common.date')"
                style="width: 110px"
              >
                <template #body="{ data }">
                  {{ data.tarih ? formatDate(data.tarih) : '-' }}
                </template>
              </Column>
              <Column
                field="faturaNumarasi"
                :header="t('karlilik.belgeNo')"
                style="width: 150px"
              />
              <Column
                field="cariAd"
                :header="t('cariHesaplar.title')"
              />
              <Column
                field="urunAd"
                :header="t('stoklar.title')"
              />
              <Column
                field="adet"
                :header="t('karlilik.adet')"
                style="width: 90px"
              />
              <Column
                field="ciro"
                :header="t('karlilik.ciro')"
              >
                <template #body="{ data }">
                  <span :class="data.iade ? 'negatif' : ''">{{ formatCurrency(data.ciro) }}</span>
                </template>
              </Column>
              <Column
                field="brutKar"
                :header="t('karlilik.brutKar')"
              >
                <template #body="{ data }">
                  <span :class="data.brutKar < 0 ? 'negatif' : 'pozitif'">{{ formatCurrency(data.brutKar) }}</span>
                </template>
              </Column>
            </DataTable>
          </TabPanel>
        </TabView>
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { raporAPI } from '../api/index.js'
import { useToast } from 'primevue/usetoast'
import { Line, Bar, Doughnut } from 'vue-chartjs'
import {
  Chart as ChartJS,
  Title,
  Tooltip,
  Legend,
  BarElement,
  CategoryScale,
  LinearScale,
  ArcElement,
  PointElement,
  LineElement,
  Filler
} from 'chart.js'
import KpiKart from '../components/KpiKart.vue'
import { useChartTema } from '../composables/useChartTema.js'
import { formatCurrency, formatTarih as formatDate } from '../utils/format.js'

ChartJS.register(Title, Tooltip, Legend, BarElement, CategoryScale, LinearScale, ArcElement, PointElement, LineElement, Filler)

const { t } = useI18n()
const toast = useToast()
const { palet, lejant } = useChartTema()

const bugun = new Date()
const veri = ref(null)
const yukleniyor = ref(false)
const grup = ref('KATEGORI')
// Varsayilan aralik: yil basi -> bugun (YTD); boylece tablo/grafikler dolu gelir.
const baslangic = ref(new Date(bugun.getFullYear(), 0, 1))
const bitis = ref(bugun)

const grupSecenekleri = computed(() => [
  { label: t('karlilik.grupKategori'), value: 'KATEGORI' },
  { label: t('karlilik.grupUrun'), value: 'URUN' },
  { label: t('karlilik.grupCari'), value: 'CARI' }
])

const alanBasligi = computed(() => ({
  KATEGORI: t('karlilik.grupKategori'),
  URUN: t('karlilik.grupUrun'),
  CARI: t('karlilik.grupCari')
}[grup.value] || t('karlilik.grupKategori')))

const iso = (d) => {
  if (!d) return null
  const t2 = new Date(d)
  return `${t2.getFullYear()}-${String(t2.getMonth() + 1).padStart(2, '0')}-${String(t2.getDate()).padStart(2, '0')}`
}
const fmt = (v) => (Number(v) || 0).toLocaleString('tr-TR', { maximumFractionDigits: 2 })

const yukle = async () => {
  yukleniyor.value = true
  try {
    const r = await raporAPI.karlilikAnalizi({
      baslangic: iso(baslangic.value),
      bitis: iso(bitis.value),
      grup: grup.value
    })
    veri.value = r.data
  } catch (err) {
    toast.add({ severity: 'error', summary: t('common.unexpectedError'), detail: err?.response?.data?.message || t('karlilik.hata'), life: 4000 })
  } finally {
    yukleniyor.value = false
  }
}

const renkler = ['#3b82f6', '#10b981', '#f59e0b', '#8b5cf6', '#ef4444', '#06b6d4', '#a3e635']

// ---- Drill-down: satıra tıklayınca alt kırılım + belge dökümü ----
const detayDialog = ref(false)
const detayVeri = ref(null)
const detayYukleniyor = ref(false)

const detayBaslik = computed(() => detayVeri.value
  ? `${t('karlilik.detay')}: ${detayVeri.value.deger}`
  : t('karlilik.detay'))

const altGrupBasligi = computed(() => ({
  URUN: t('karlilik.grupUrun'),
  CARI: t('karlilik.grupCari'),
  KATEGORI: t('karlilik.grupKategori')
}[detayVeri.value?.altGrup] || t('karlilik.grupUrun')))

const detayAc = async (satir) => {
  if (!satir) return
  detayDialog.value = true
  detayYukleniyor.value = true
  detayVeri.value = null
  try {
    const params = {
      baslangic: iso(baslangic.value),
      bitis: iso(bitis.value),
      grup: grup.value,
      deger: satir.ad
    }
    if (satir.id != null) params.degerId = satir.id
    const r = await raporAPI.karlilikDetay(params)
    detayVeri.value = r.data
  } catch (err) {
    toast.add({ severity: 'error', summary: t('common.unexpectedError'), detail: err?.response?.data?.message || t('karlilik.hata'), life: 4000 })
  } finally {
    detayYukleniyor.value = false
  }
}

const trendData = computed(() => {
  const tr = veri.value?.aylikTrend || []
  return {
    labels: tr.map((x) => x.ay),
    datasets: [
      { label: t('karlilik.ciro'), data: tr.map((x) => x.ciro), borderColor: '#3b82f6', backgroundColor: 'rgba(59,130,246,.15)', fill: true, tension: 0.3 },
      { label: t('karlilik.maliyet'), data: tr.map((x) => x.maliyet), borderColor: '#f59e0b', backgroundColor: 'rgba(245,158,11,.15)', fill: true, tension: 0.3 },
      { label: t('karlilik.brutKar'), data: tr.map((x) => x.brutKar), borderColor: '#10b981', backgroundColor: 'rgba(16,185,129,.15)', fill: true, tension: 0.3 }
    ]
  }
})

const kirilimData = computed(() => {
  const top = (veri.value?.kirilim || []).slice(0, 8)
  return {
    labels: top.map((x) => x.ad),
    datasets: [
      { label: t('karlilik.brutKar'), data: top.map((x) => x.brutKar), backgroundColor: top.map((x) => (x.brutKar < 0 ? '#ef4444' : '#10b981')) }
    ]
  }
})

const payData = computed(() => {
  const top = (veri.value?.kirilim || []).slice(0, 6)
  return {
    labels: top.map((x) => x.ad),
    datasets: [{ data: top.map((x) => x.ciro), backgroundColor: renkler, borderWidth: 0 }]
  }
})

const lineOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  animation: { duration: 300 },
  plugins: { legend: lejant() },
  scales: {
    x: { ticks: { color: palet.value.metin }, grid: { color: palet.value.izgara } },
    y: { ticks: { color: palet.value.metin }, grid: { color: palet.value.izgara } }
  }
}))
const barOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  animation: { duration: 300 },
  plugins: { legend: { display: false } },
  scales: {
    x: { ticks: { color: palet.value.metin }, grid: { display: false } },
    y: { ticks: { color: palet.value.metin }, grid: { color: palet.value.izgara } }
  }
}))
const pieOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  animation: { duration: 300 },
  plugins: { legend: lejant() }
}))

onMounted(yukle)
</script>

<style scoped>
.karlilik-container {
  padding: 0;
}
.sayfa-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
}
.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
}
.filtreler {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.tarih-girdi {
  width: 150px;
}
.yukleniyor {
  text-align: center;
  padding: 3rem;
  color: var(--text-muted);
}
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}
.grafik-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}
.grafik-kart :deep(.p-card-body) {
  min-height: 320px;
}
.grafik-yukseklik {
  position: relative;
  height: 260px;
}
.grafik-kart :deep(canvas) {
  max-height: 260px;
}
.bos-durum {
  margin-bottom: 20px;
}
.tablo-kart {
  margin-top: 8px;
}
.uyari-kart {
  margin-bottom: 20px;
  border: 1px solid rgba(245, 158, 11, 0.4);
}
.uyari-baslik {
  color: #f59e0b;
  font-weight: 600;
}
.uyari-satir {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 6px 0;
  border-bottom: 1px dashed var(--border);
  font-size: 0.88rem;
}
.uyari-ad {
  flex: 1;
  color: var(--text-primary);
}
.uyari-marj {
  color: #ef4444;
  font-weight: 600;
}
.uyari-tutar {
  color: #ef4444;
  min-width: 110px;
  text-align: right;
}
.pozitif {
  color: #10b981;
  font-weight: 600;
}
.negatif {
  color: #ef4444;
  font-weight: 600;
}
.kirilim-tablo :deep(.p-datatable-tbody > tr) {
  cursor: pointer;
}
.kirilim-tablo :deep(.p-datatable-tbody > tr:hover) {
  background: var(--surface-hover, rgba(148, 163, 184, 0.12));
}
.detay-ozet {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  padding: 10px 12px;
  margin-bottom: 12px;
  border-radius: 10px;
  background: var(--surface-100, rgba(148, 163, 184, 0.08));
  font-size: 0.9rem;
  color: var(--text-secondary);
}
</style>
