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
        />
        <DatePicker
          v-model="baslangic"
          date-format="dd/mm/yy"
          :placeholder="t('karlilik.baslangic')"
          class="tarih-girdi"
        />
        <DatePicker
          v-model="bitis"
          date-format="dd/mm/yy"
          :placeholder="t('karlilik.bitis')"
          class="tarih-girdi"
        />
        <Button
          icon="pi pi-refresh"
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

      <div class="grafik-grid">
        <Card class="grafik-kart">
          <template #title>
            {{ t('karlilik.aylikTrend') }}
          </template>
          <Line
            :data="trendData"
            :options="lineOptions"
          />
        </Card>
        <Card class="grafik-kart">
          <template #title>
            {{ t('karlilik.kirilimGrafik') }}
          </template>
          <Bar
            :data="kirilimData"
            :options="barOptions"
          />
        </Card>
        <Card class="grafik-kart">
          <template #title>
            {{ t('karlilik.ciroPayi') }}
          </template>
          <Doughnut
            :data="payData"
            :options="pieOptions"
          />
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
        >
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
import { formatCurrency } from '../utils/format.js'

ChartJS.register(Title, Tooltip, Legend, BarElement, CategoryScale, LinearScale, ArcElement, PointElement, LineElement, Filler)

const { t } = useI18n()
const toast = useToast()
const { palet, lejant } = useChartTema()

const bugun = new Date()
const veri = ref(null)
const yukleniyor = ref(false)
const grup = ref('KATEGORI')
const baslangic = ref(new Date(bugun.getFullYear(), bugun.getMonth(), 1))
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
  plugins: { legend: lejant() },
  scales: {
    x: { ticks: { color: palet.value.metin }, grid: { color: palet.value.izgara } },
    y: { ticks: { color: palet.value.metin }, grid: { color: palet.value.izgara } }
  }
}))
const barOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { display: false } },
  scales: {
    x: { ticks: { color: palet.value.metin }, grid: { display: false } },
    y: { ticks: { color: palet.value.metin }, grid: { color: palet.value.izgara } }
  }
}))
const pieOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
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
.grafik-kart :deep(canvas) {
  max-height: 260px;
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
</style>
