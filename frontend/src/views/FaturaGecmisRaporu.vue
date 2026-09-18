<template>
  <div class="fgr-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        {{ t('faturaGecmisRapor.title') }}
      </h1>
      <div class="filtreler">
        <DatePicker
          v-model="baslangic"
          date-format="dd/mm/yy"
          :placeholder="t('faturaGecmisRapor.baslangic')"
          class="tarih-girdi"
        />
        <DatePicker
          v-model="bitis"
          date-format="dd/mm/yy"
          :placeholder="t('faturaGecmisRapor.bitis')"
          class="tarih-girdi"
        />
        <SelectButton
          v-model="tur"
          :options="turSecenekleri"
          option-label="label"
          option-value="value"
          :allow-empty="false"
          size="small"
        />
        <Dropdown
          v-model="olay"
          :options="olaySecenekleri"
          option-label="label"
          option-value="value"
          size="small"
          class="olay-sec"
        />
        <span class="p-input-icon-left arama">
          <i class="pi pi-search" />
          <InputText
            v-model="arama"
            :placeholder="t('faturaGecmisRapor.arama')"
            @keyup.enter="yukle"
          />
        </span>
        <Button
          icon="pi pi-refresh"
          :loading="yukleniyor"
          @click="yukle"
        />
        <Button
          icon="pi pi-file-pdf"
          class="p-button-danger"
          :disabled="!kayitlar.length"
          :title="t('faturaGecmisRapor.pdf')"
          @click="pdfIndir"
        />
        <Button
          icon="pi pi-file-excel"
          class="p-button-success"
          :disabled="!kayitlar.length"
          :title="t('faturaGecmisRapor.excel')"
          @click="excelIndir"
        />
      </div>
    </div>

    <div class="ozet-grid">
      <div class="ozet-kart">
        <span>{{ t('faturaGecmisRapor.toplam') }}</span>
        <strong>{{ kayitlar.length }}</strong>
      </div>
      <div class="ozet-kart">
        <span>{{ t('faturaGecmisRapor.olusturma') }}</span>
        <strong>{{ sayim('OLUSTUR') }}</strong>
      </div>
      <div class="ozet-kart">
        <span>{{ t('faturaGecmisRapor.duzenleme') }}</span>
        <strong>{{ sayim('GUNCELLE') + sayim('DURUM') }}</strong>
      </div>
      <div class="ozet-kart">
        <span>{{ t('faturaGecmisRapor.yazdirma') }}</span>
        <strong>{{ sayim('YAZDIR') }}</strong>
      </div>
    </div>

    <Card class="grafik-kart">
      <Bar
        :data="olayData"
        :options="barOptions"
      />
    </Card>

    <Card class="tablo-kart">
      <DataTable
        :value="kayitlar"
        :loading="yukleniyor"
        :rows="20"
        paginator
        striped-rows
        size="small"
        scrollable
      >
        <Column
          field="tarih"
          :header="t('faturaGecmisRapor.tarih')"
          style="width: 160px"
        >
          <template #body="{ data }">
            {{ formatDateTime(data.tarih) }}
          </template>
        </Column>
        <Column
          field="faturaNumarasi"
          :header="t('faturaGecmisRapor.faturaNo')"
          style="width: 140px"
        />
        <Column
          field="faturaTur"
          :header="t('faturaGecmisRapor.tur')"
          style="width: 90px"
        />
        <Column
          field="cariHesapAd"
          :header="t('faturaGecmisRapor.cari')"
          style="width: 170px"
        >
          <template #body="{ data }">
            {{ data.cariHesapAd || '-' }}
          </template>
        </Column>
        <Column
          field="olay"
          :header="t('faturaGecmisRapor.olay')"
          style="width: 120px"
        >
          <template #body="{ data }">
            <span :class="['olay-badge', data.olay.toLowerCase()]">{{ olayEtiket(data.olay) }}</span>
          </template>
        </Column>
        <Column
          field="kullaniciAdi"
          :header="t('faturaGecmisRapor.kullanici')"
          style="width: 130px"
        >
          <template #body="{ data }">
            {{ data.kullaniciAdi || '-' }}
          </template>
        </Column>
        <Column
          field="yazdirmaFormat"
          :header="t('faturaGecmisRapor.bicim')"
          style="width: 110px"
        >
          <template #body="{ data }">
            {{ data.yazdirmaFormat || '-' }}
          </template>
        </Column>
        <Column
          field="yaziciAdi"
          :header="t('faturaGecmisRapor.yazici')"
          style="width: 150px"
        >
          <template #body="{ data }">
            {{ data.yaziciAdi || '-' }}
          </template>
        </Column>
        <Column
          field="kopyaNo"
          :header="t('faturaGecmisRapor.kopya')"
          style="width: 80px"
        >
          <template #body="{ data }">
            {{ data.kopyaNo || '-' }}
          </template>
        </Column>
        <Column
          field="aciklama"
          :header="t('faturaGecmisRapor.aciklama')"
        />
      </DataTable>
    </Card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { raporAPI } from '../api/index.js'
import { useToast } from 'primevue/usetoast'
import { Bar } from 'vue-chartjs'
import { Chart as ChartJS, Title, Tooltip, Legend, BarElement, CategoryScale, LinearScale } from 'chart.js'
import { formatDateTime } from '../utils/format.js'

ChartJS.register(Title, Tooltip, Legend, BarElement, CategoryScale, LinearScale)

const { t } = useI18n()
const toast = useToast()

const bugun = new Date()
const kayitlar = ref([])
const yukleniyor = ref(false)
const baslangic = ref(new Date(bugun.getFullYear(), bugun.getMonth(), 1))
const bitis = ref(bugun)
const tur = ref('TUM')
const olay = ref('TUM')
const arama = ref('')

const turSecenekleri = computed(() => [
  { label: t('faturaGecmisRapor.tumTur'), value: 'TUM' },
  { label: t('faturaGecmisRapor.satis'), value: 'SATIS' },
  { label: t('faturaGecmisRapor.alis'), value: 'ALIS' }
])
const olaySecenekleri = computed(() => [
  { label: t('faturaGecmisRapor.tumOlay'), value: 'TUM' },
  { label: t('faturaGecmis.olay.olustur'), value: 'OLUSTUR' },
  { label: t('faturaGecmis.olay.guncelle'), value: 'GUNCELLE' },
  { label: t('faturaGecmis.olay.durumDegisikligi'), value: 'DURUM' },
  { label: t('faturaGecmis.olay.sil'), value: 'SIL' },
  { label: t('faturaGecmis.olay.yazdir'), value: 'YAZDIR' }
])

const iso = (d) => {
  if (!d) return null
  const x = new Date(d)
  return `${x.getFullYear()}-${String(x.getMonth() + 1).padStart(2, '0')}-${String(x.getDate()).padStart(2, '0')}`
}

const params = () => ({
  baslangic: iso(baslangic.value),
  bitis: iso(bitis.value),
  tur: tur.value === 'TUM' ? undefined : tur.value,
  olay: olay.value === 'TUM' ? undefined : olay.value,
  q: arama.value || undefined
})

const yukle = async () => {
  yukleniyor.value = true
  try {
    const r = await raporAPI.faturaGecmis(params())
    kayitlar.value = r.data || []
  } catch (err) {
    toast.add({ severity: 'error', summary: t('common.unexpectedError'), detail: err?.response?.data?.message || t('faturaGecmisRapor.hata'), life: 4000 })
  } finally {
    yukleniyor.value = false
  }
}

const sayim = (o) => kayitlar.value.filter((k) => k.olay === o).length

const olayEtiket = (o) => t(`faturaGecmis.olay.${({ OLUSTUR: 'olustur', GUNCELLE: 'guncelle', DURUM: 'durumDegisikligi', SIL: 'sil', YAZDIR: 'yazdir' }[o]) || 'diger'}`)

const olayData = computed(() => {
  const etiketler = ['OLUSTUR', 'GUNCELLE', 'DURUM', 'SIL', 'YAZDIR']
  return {
    labels: etiketler.map((e) => olayEtiket(e)),
    datasets: [{
      label: t('faturaGecmisRapor.toplam'),
      data: etiketler.map((e) => sayim(e)),
      backgroundColor: ['#3b82f6', '#f59e0b', '#8b5cf6', '#ef4444', '#10b981']
    }]
  }
})

const barOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { display: false } },
  scales: { y: { beginAtZero: true, ticks: { precision: 0 } } }
}

const indir = (blob, ad) => {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = ad
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}

const pdfIndir = async () => {
  try {
    const r = await raporAPI.faturaGecmisPdf(params())
    indir(new Blob([r.data], { type: 'application/pdf' }), 'fatura-gecmis.pdf')
  } catch {
    toast.add({ severity: 'error', summary: t('common.unexpectedError'), detail: t('faturaGecmisRapor.hata'), life: 4000 })
  }
}

const excelIndir = async () => {
  try {
    const r = await raporAPI.faturaGecmisExcel(params())
    indir(new Blob([r.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }), 'fatura-gecmis.xlsx')
  } catch {
    toast.add({ severity: 'error', summary: t('common.unexpectedError'), detail: t('faturaGecmisRapor.hata'), life: 4000 })
  }
}

onMounted(yukle)
</script>

<style scoped>
.fgr-container {
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
.olay-sec {
  width: 170px;
}
.arama :deep(input) {
  width: 200px;
}
.ozet-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}
.ozet-kart {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 14px 16px;
  border-radius: 12px;
  background: var(--bg-card);
  border: 1px solid var(--border);
}
.ozet-kart span {
  font-size: 0.78rem;
  color: var(--text-secondary);
}
.ozet-kart strong {
  font-size: 1.4rem;
  color: var(--text-primary);
}
.grafik-kart {
  margin-bottom: 18px;
}
.grafik-kart :deep(canvas) {
  max-height: 220px;
}
.olay-badge {
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
  color: #fff;
}
.olay-badge.olustur { background: var(--accent); }
.olay-badge.guncelle { background: #f59e0b; }
.olay-badge.durum { background: #8b5cf6; }
.olay-badge.sil { background: #ef4444; }
.olay-badge.yazdir { background: #10b981; }
</style>
