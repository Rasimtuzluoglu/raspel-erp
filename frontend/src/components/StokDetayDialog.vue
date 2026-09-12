<template>
  <Dialog
    :visible="visible"
    :header="stok?.ad || 'Ürün Detayı'"
    :modal="true"
    style="width: 920px"
    @update:visible="$emit('update:visible', $event)"
  >
    <TabView>
      <TabPanel :header="t('stoklar.analiz.genel')">
        <div
          v-if="stok"
          class="detail-grid"
        >
          <div class="detail-item">
            <span class="detail-label">Stok Kodu</span>
            <span class="detail-value">{{ stok.stokKodu || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">Barkod</span>
            <span class="detail-value">{{ stok.barkod || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">Birim</span>
            <span class="detail-value">{{ stok.birim || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">Miktar</span>
            <span
              class="detail-value"
              :class="stok.minMiktar && stok.miktar <= stok.minMiktar ? 'kritik' : 'normal'"
            >
              {{ stok.miktar }} {{ stok.birim || '' }}
            </span>
          </div>
          <div class="detail-item">
            <span class="detail-label">Alış Fiyatı</span>
            <span class="detail-value">{{ formatCurrency(stok.fiyat) }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">Satış Fiyatı</span>
            <span class="detail-value">{{ formatCurrency(stok.satisFiyati) }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">Kategori</span>
            <span class="detail-value">{{ stok.kategori || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">Marka</span>
            <span class="detail-value">{{ stok.marka || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">Min. Stok</span>
            <span class="detail-value">{{ stok.minMiktar || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">Raf No</span>
            <span class="detail-value">{{ stok.rafNo || '-' }}</span>
          </div>
        </div>
        <div class="form-section-title">
          Stok Hareketleri
        </div>
        <div
          v-if="hareketlerYukleniyor"
          class="loading"
        >
          <p><i class="pi pi-spin pi-spinner" /> Yükleniyor...</p>
        </div>
        <EmptyState
          v-else-if="hareketler.length === 0"
          message="Hareket bulunamadı"
          sub-message="Bu ürüne ait stok hareketi bulunmamaktadır."
          icon="pi pi-list"
        />
        <DataTable
          v-else
          :value="hareketler"
          size="small"
          striped-rows
          :paginator="hareketler.length > 10"
          :rows="10"
        >
          <Column
            header="Tarih"
            style="width: 110px"
          >
            <template #body="s">
              {{ formatDate(s.data.hareketTarihi || s.data.tarih) }}
            </template>
          </Column>
          <Column
            header="Tür"
            style="width: 90px"
          >
            <template #body="s">
              <span :class="['badge', s.data.tur === 'GIRIS' ? 'giris' : 'cikis']">
                {{ s.data.tur === 'GIRIS' ? 'Giriş' : 'Çıkış' }}
              </span>
            </template>
          </Column>
          <Column
            header="Miktar"
            style="width: 90px"
          >
            <template #body="s">
              <span :class="s.data.tur === 'GIRIS' ? 'positive' : 'negative'">{{ s.data.miktar }}</span>
            </template>
          </Column>
          <Column header="Açıklama">
            <template #body="s">
              {{ s.data.aciklama || '-' }}
            </template>
          </Column>
        </DataTable>
      </TabPanel>
      <TabPanel :header="t('stoklar.analiz.urunAnalizi')">
        <div class="analiz-filtre">
          <div class="analiz-filtre-grup">
            <label>{{ t('stoklar.analiz.baslangic') }}</label>
            <DatePicker
              v-model="baslangicTarih"
              date-format="dd.mm.yy"
              show-icon
              class="w-full"
              @change="tarihDegisti"
            />
          </div>
          <div class="analiz-filtre-grup">
            <label>{{ t('stoklar.analiz.bitis') }}</label>
            <DatePicker
              v-model="bitisTarih"
              date-format="dd.mm.yy"
              show-icon
              class="w-full"
              @change="tarihDegisti"
            />
          </div>
        </div>
        <div
          v-if="analizYukleniyor"
          class="loading"
        >
          <p><i class="pi pi-spin pi-spinner" /> {{ t('stoklar.analiz.yukleniyor') }}</p>
        </div>
        <div
          v-else-if="analizHata"
          class="analiz-hata"
        >
          <i class="pi pi-exclamation-triangle" /> {{ analizHata }}
        </div>
        <template v-else-if="analiz">
          <div class="analiz-ozet">
            <div
              v-for="o in ozetItems"
              :key="o.label"
              class="analiz-kart"
            >
              <span class="analiz-kart-label">{{ o.label }}</span>
              <span class="analiz-kart-deger">{{ o.value }}</span>
            </div>
          </div>

          <div class="form-section-title">
            {{ t('stoklar.analiz.islemGecmisi') }}
          </div>
          <DataTable
            v-if="islemler.length"
            :value="islemler"
            size="small"
            striped-rows
            :rows="islemSayfaBoyut"
            :first="islemSayfa * islemSayfaBoyut"
            :total-records="islemToplam"
            :lazy="true"
            :loading="islemYukleniyor"
            :paginator="islemToplam > islemSayfaBoyut"
            @page="islemSayfaDegisti"
          >
            <Column
              :header="t('stoklar.analiz.tarih')"
              style="width: 100px"
            >
              <template #body="s">
                {{ formatDate(s.data.tarih) }}
              </template>
            </Column>
            <Column :header="t('stoklar.analiz.belgeNo')">
              <template #body="s">
                {{ s.data.belgeNo }}
              </template>
            </Column>
            <Column
              :header="t('stoklar.analiz.tur')"
              style="width: 100px"
            >
              <template #body="s">
                <span :class="['badge', turSinifi(s.data.tur)]">{{ turLabel(s.data.tur) }}</span>
              </template>
            </Column>
            <Column
              :header="t('stoklar.analiz.miktar')"
              style="width: 90px"
            >
              <template #body="s">
                {{ s.data.miktar }}
              </template>
            </Column>
            <Column
              :header="t('stoklar.analiz.birimFiyat')"
              style="width: 120px"
            >
              <template #body="s">
                {{ formatCurrency(s.data.birimFiyat) }}
              </template>
            </Column>
            <Column
              :header="t('stoklar.analiz.tutar')"
              style="width: 120px"
            >
              <template #body="s">
                {{ formatCurrency(s.data.tutar) }}
              </template>
            </Column>
          </DataTable>
          <EmptyState
            v-else
            :message="t('stoklar.analiz.veriYok')"
            icon="pi pi-list"
          />

          <div class="cari-tablolar">
            <div class="cari-tablo">
              <div class="form-section-title">
                {{ t('stoklar.analiz.musteriBazli') }}
              </div>
              <DataTable
                v-if="musteriler.length"
                :value="musteriler"
                size="small"
                striped-rows
              >
                <Column :header="t('stoklar.analiz.cariHesap')">
                  <template #body="s">
                    {{ s.data.cariHesapAd }}
                  </template>
                </Column>
                <Column
                  :header="t('stoklar.analiz.toplamMiktar')"
                  style="width: 100px"
                >
                  <template #body="s">
                    {{ s.data.toplamMiktar }}
                  </template>
                </Column>
                <Column
                  :header="t('stoklar.analiz.toplamTutar')"
                  style="width: 130px"
                >
                  <template #body="s">
                    {{ formatCurrency(s.data.toplamTutar) }}
                  </template>
                </Column>
                <Column
                  :header="t('stoklar.analiz.islemSayisi')"
                  style="width: 80px"
                >
                  <template #body="s">
                    {{ s.data.islemSayisi }}
                  </template>
                </Column>
              </DataTable>
              <EmptyState
                v-else
                :message="t('stoklar.analiz.veriYok')"
                icon="pi pi-users"
              />
            </div>
            <div class="cari-tablo">
              <div class="form-section-title">
                {{ t('stoklar.analiz.tedarikciBazli') }}
              </div>
              <DataTable
                v-if="tedarikciler.length"
                :value="tedarikciler"
                size="small"
                striped-rows
              >
                <Column :header="t('stoklar.analiz.cariHesap')">
                  <template #body="s">
                    {{ s.data.cariHesapAd }}
                  </template>
                </Column>
                <Column
                  :header="t('stoklar.analiz.toplamMiktar')"
                  style="width: 100px"
                >
                  <template #body="s">
                    {{ s.data.toplamMiktar }}
                  </template>
                </Column>
                <Column
                  :header="t('stoklar.analiz.toplamTutar')"
                  style="width: 130px"
                >
                  <template #body="s">
                    {{ formatCurrency(s.data.toplamTutar) }}
                  </template>
                </Column>
                <Column
                  :header="t('stoklar.analiz.islemSayisi')"
                  style="width: 80px"
                >
                  <template #body="s">
                    {{ s.data.islemSayisi }}
                  </template>
                </Column>
              </DataTable>
              <EmptyState
                v-else
                :message="t('stoklar.analiz.veriYok')"
                icon="pi pi-building"
              />
            </div>
          </div>

          <div class="form-section-title">
            {{ t('stoklar.analiz.aylikSeri') }}
          </div>
          <div
            v-if="aylik.length"
            class="chart-container"
            style="height: 220px; margin-bottom: 16px"
          >
            <Line
              :data="fiyatSerisiVerisi"
              :options="fiyatSerisiOptions"
            />
          </div>
          <DataTable
            v-if="aylik.length"
            :value="aylik"
            size="small"
            striped-rows
          >
            <Column
              :header="t('stoklar.analiz.ay')"
              style="width: 100px"
            >
              <template #body="s">
                {{ ayEtiketi(s.data) }}
              </template>
            </Column>
            <Column
              :header="t('stoklar.analiz.ortalamaAlis')"
              style="width: 130px"
            >
              <template #body="s">
                {{ formatCurrency(s.data.ortalamaAlisFiyati) }}
              </template>
            </Column>
            <Column
              :header="t('stoklar.analiz.ortalamaSatis')"
              style="width: 130px"
            >
              <template #body="s">
                {{ formatCurrency(s.data.ortalamaSatisFiyati) }}
              </template>
            </Column>
            <Column
              :header="t('stoklar.analiz.toplamAlisMiktar')"
              style="width: 110px"
            >
              <template #body="s">
                {{ s.data.toplamAlisMiktar }}
              </template>
            </Column>
            <Column
              :header="t('stoklar.analiz.toplamSatisMiktar')"
              style="width: 110px"
            >
              <template #body="s">
                {{ s.data.toplamSatisMiktar }}
              </template>
            </Column>
          </DataTable>
          <EmptyState
            v-else
            :message="t('stoklar.analiz.veriYok')"
            icon="pi pi-chart-bar"
          />
        </template>
      </TabPanel>
    </TabView>
  </Dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { stokAPI } from '../api/index.js'
import { formatCurrency, formatTarih as formatDate } from '../utils/format.js'
import { Line } from 'vue-chartjs'
import {
  Chart as ChartJS,
  Title,
  Tooltip,
  Legend,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement
} from 'chart.js'

ChartJS.register(Title, Tooltip, Legend, CategoryScale, LinearScale, PointElement, LineElement)

const props = defineProps({
  visible: { type: Boolean, default: false },
  stok: { type: Object, default: null },
  hareketler: { type: Array, default: () => [] },
  hareketlerYukleniyor: { type: Boolean, default: false }
})

defineEmits(['update:visible'])

const { t } = useI18n()

const analiz = ref(null)
const islemler = ref([])
const aylik = ref([])
const musteriler = ref([])
const tedarikciler = ref([])
const analizYukleniyor = ref(false)
const analizHata = ref('')
const islemYukleniyor = ref(false)
const islemToplam = ref(0)
const islemSayfa = ref(0)
const islemSayfaBoyut = ref(10)
const baslangicTarih = ref(new Date(new Date().getFullYear(), 0, 1))
const bitisTarih = ref(new Date())

const sayi = (v) => (v == null ? 0 : v)

const tarihParam = (d) => {
  if (!d) return undefined
  if (d instanceof Date && !isNaN(d.getTime())) {
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const g = String(d.getDate()).padStart(2, '0')
    return `${y}-${m}-${g}`
  }
  return d
}

const analizParamlari = () => {
  const p = {}
  const b = tarihParam(baslangicTarih.value)
  const t = tarihParam(bitisTarih.value)
  if (b) p.baslangic = b
  if (t) p.bitis = t
  return p
}

const ozetItems = computed(() => {
  const alis = analiz.value?.alisOzet || {}
  const satis = analiz.value?.satisOzet || {}
  const k = analiz.value?.karlilik || {}
  const birim = analiz.value?.birim || ''
  return [
    { label: t('stoklar.analiz.toplamAlisTutari'), value: formatCurrency(alis.toplamAlisTutari) },
    { label: t('stoklar.analiz.toplamSatisTutari'), value: formatCurrency(satis.toplamSatisTutari) },
    { label: t('stoklar.analiz.stokMiktari'), value: `${sayi(alis.stokMiktar)} ${birim}`.trim() },
    { label: t('stoklar.analiz.stokMaliyeti'), value: formatCurrency(k.stokMaliyeti) },
    { label: t('stoklar.analiz.ortMaliyet'), value: formatCurrency(k.ortalamaMaliyet) },
    { label: t('stoklar.analiz.ortalamaSatis'), value: formatCurrency(k.ortalamaSatisFiyati) },
    { label: t('stoklar.analiz.toplamBrutKar'), value: formatCurrency(k.toplamBrutKar) },
    { label: t('stoklar.analiz.brutKarMarji'), value: k.brutKarMarji == null ? '-' : `${sayi(k.brutKarMarji)}%` },
    { label: t('stoklar.analiz.satilanMiktar'), value: `${sayi(k.satilanMiktar)} ${birim}`.trim() },
    { label: t('stoklar.analiz.satisIadeTutari'), value: formatCurrency(k.satisIadeTutari) }
  ]
})

const turLabel = (tur) => {
  const map = { ALIS: 'alis', SATIS: 'satis', ALIS_IADE: 'alisIade', SATIS_IADE: 'satisIade' }
  return t(`stoklar.analiz.${map[tur] || 'satis'}`)
}

const turSinifi = (tur) => (tur === 'ALIS' || tur === 'ALIS_IADE' ? 'alis' : 'satis')

const ayEtiketi = (a) => `${sayi(a.ay)}/${a.yil}`

const fiyatSerisiVerisi = computed(() => ({
  labels: aylik.value.map((a) => ayEtiketi(a)),
  datasets: [
    {
      label: t('stoklar.analiz.ortalamaAlis'),
      borderColor: '#10b981',
      backgroundColor: 'rgba(16, 185, 129, 0.12)',
      fill: true,
      tension: 0.35,
      pointRadius: 4,
      pointBackgroundColor: '#10b981',
      data: aylik.value.map((a) => a.ortalamaAlisFiyati ?? 0)
    },
    {
      label: t('stoklar.analiz.ortalamaSatis'),
      borderColor: '#3b82f6',
      backgroundColor: 'rgba(59, 130, 246, 0.12)',
      fill: true,
      tension: 0.35,
      pointRadius: 4,
      pointBackgroundColor: '#3b82f6',
      data: aylik.value.map((a) => a.ortalamaSatisFiyati ?? 0)
    }
  ]
}))

const fiyatSerisiOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { position: 'top' },
    tooltip: {
      callbacks: {
        label: (ctx) => `${ctx.dataset.label}: ${formatCurrency(ctx.raw || 0)}`
      }
    }
  },
  scales: {
    y: {
      grid: { color: 'rgba(0,0,0,0.05)' },
      ticks: {
        callback: (v) => formatCurrency(v)
      }
    },
    x: {
      grid: { display: false }
    }
  }
}

const tarihDegisti = () => {
  islemSayfa.value = 0
  analiziYukle()
}

const analiziYukle = async () => {
  if (!props.stok?.id) return
  analizYukleniyor.value = true
  analizHata.value = ''
  islemSayfa.value = 0
  try {
    const [a, m, mu, te] = await Promise.all([
      stokAPI.analiz(props.stok.id, analizParamlari()),
      stokAPI.aylikFiyat(props.stok.id, analizParamlari()),
      stokAPI.musteriAnaliz(props.stok.id, analizParamlari()),
      stokAPI.tedarikciAnaliz(props.stok.id, analizParamlari())
    ])
    analiz.value = a.data
    aylik.value = m.data || []
    musteriler.value = mu.data || []
    tedarikciler.value = te.data || []
  } catch (err) {
    analizHata.value = err?.response?.data?.message || err?.message || t('stoklar.analiz.yuklenemiyor')
  } finally {
    analizYukleniyor.value = false
  }
  await islemGecmisiYukle()
}

const islemGecmisiYukle = async () => {
  if (!props.stok?.id) return
  islemYukleniyor.value = true
  try {
    const y = await stokAPI.islemGecmisiSayfali(props.stok.id, {
      ...analizParamlari(),
      sayfa: islemSayfa.value,
      boyut: islemSayfaBoyut.value
    })
    islemler.value = y.data?.satirlar || []
    islemToplam.value = y.data?.toplam || 0
  } catch (err) {
    islemler.value = []
    islemToplam.value = 0
  } finally {
    islemYukleniyor.value = false
  }
}

const islemSayfaDegisti = (e) => {
  if (e?.rows) islemSayfaBoyut.value = e.rows
  islemSayfa.value = e?.page ?? 0
  islemGecmisiYukle()
}

watch(
  () => props.visible,
  (v) => {
    if (v) analiziYukle()
  }
)
</script>

<style scoped>
.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 8px;
}
.detail-item {
  padding: 6px 0;
}
.detail-label {
  display: block;
  font-size: 11px;
  color: var(--text-muted);
  text-transform: uppercase;
  margin-bottom: 3px;
}
.detail-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}
.detail-value.normal {
  color: #4ade80;
}
.detail-value.kritik {
  color: #f87171;
}
.form-section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border);
}
.loading {
  text-align: center;
  padding: 40px;
  color: var(--text-secondary);
}
.analiz-hata {
  padding: 14px;
  border-radius: 8px;
  background: rgba(239, 68, 68, 0.12);
  color: #f87171;
  font-size: 13px;
}
.analiz-ozet {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 24px;
}
.analiz-kart {
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--bg-card);
}
.analiz-kart-label {
  display: block;
  font-size: 11px;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 6px;
}
.analiz-kart-deger {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}
.cari-tablolar {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 8px;
}
.cari-tablo {
  min-width: 0;
}
.analiz-filtre {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.analiz-filtre-grup {
  min-width: 160px;
}
.analiz-filtre-grup label {
  display: block;
  font-size: 11px;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 5px;
}
.chart-container {
  position: relative;
  width: 100%;
}
.badge {
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 700;
}
.badge.giris {
  background: rgba(34, 197, 94, 0.15);
  color: #4ade80;
}
.badge.cikis {
  background: rgba(239, 68, 68, 0.15);
  color: #f87171;
}
.badge.alis {
  background: rgba(34, 197, 94, 0.15);
  color: #4ade80;
}
.badge.satis {
  background: rgba(59, 130, 246, 0.15);
  color: #60a5fa;
}
.positive {
  color: #4ade80;
  font-weight: 700;
}
.negative {
  color: #f87171;
  font-weight: 700;
}
</style>