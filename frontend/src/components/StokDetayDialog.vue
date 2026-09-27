<template>
  <Dialog
    :visible="visible"
    :header="stok?.ad || $t('stokDetay.urunDetayi')"
    :modal="true"
    class="stok-detay-dialog"
    @update:visible="$emit('update:visible', $event)"
  >
    <TabView>
      <TabPanel :header="t('stoklar.analiz.genel')">
        <div
          v-if="stok"
          class="detay-ust"
        >
          <div class="detay-gorsel">
            <img
              v-if="stok.fotoUrl || stok.fotoThumbUrl"
              :src="stok.fotoUrl || stok.fotoThumbUrl"
              :alt="stok.ad"
              loading="lazy"
              decoding="async"
            >
            <i
              v-else
              class="pi pi-box"
            />
          </div>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">{{ $t('stoklar.stokKodu') }}</span>
              <span class="detail-value">{{ stok.stokKodu || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">{{ $t('stoklar.barkod') }}</span>
              <span class="detail-value">{{ stok.barkod || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">{{ $t('stoklar.birim') }}</span>
              <span class="detail-value">{{ stok.birim || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">{{ $t('stoklar.analiz.miktar') }}</span>
              <span
                class="detail-value"
                :class="stok.minMiktar && stok.miktar <= stok.minMiktar ? 'kritik' : 'normal'"
              >
                {{ stok.miktar }} {{ stok.birim || '' }}
              </span>
            </div>
            <div class="detail-item">
              <span class="detail-label">{{ $t('stoklar.alisFiyati') }}</span>
              <span class="detail-value gizli-veri">{{ formatCurrency(stok.fiyat) }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">{{ $t('stoklar.satisFiyati') }}</span>
              <span class="detail-value gizli-veri">{{ formatCurrency(stok.satisFiyati) }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">{{ $t('stoklar.karMarji') }}</span>
              <span
                v-if="karMarji != null"
                class="detail-value"
                :class="karMarji < 0 ? 'kritik' : 'normal'"
              >%{{ karMarji }}</span>
              <span
                v-else
                class="detail-value"
              >-</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">{{ $t('stoklar.stokGrubu') }}</span>
              <span class="detail-value">{{ stok.stokGrubu || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">{{ $t('stoklar.kategori') }}</span>
              <span class="detail-value">{{ stok.kategori || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">{{ $t('stoklar.marka') }}</span>
              <span class="detail-value">{{ stok.marka || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">{{ $t('stoklar.minStok') }}</span>
              <span class="detail-value">{{ stok.minMiktar || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">{{ $t('stoklar.rafNo') }}</span>
              <span class="detail-value">{{ stok.rafNo || '-' }}</span>
            </div>
          </div>
        </div>
        <div class="form-section-title">
          {{ $t('stokDetay.stokHareketleri') }}
        </div>
        <div
          v-if="hareketlerYukleniyor"
          class="loading"
        >
          <p><i class="pi pi-spin pi-spinner" /> {{ $t('common.loading') }}</p>
        </div>
        <EmptyState
          v-else-if="hareketler.length === 0"
          :message="$t('stokDetay.hareketBulunamadi')"
          :sub-message="$t('stokDetay.hareketYok')"
          icon="pi pi-list"
        />
        <AppDataTable
          v-else
          :value="hareketler"
          size="small"
          striped-rows
          :paginator="hareketler.length > 10"
          :rows="10"
        >
          <Column
            :header="t('stoklar.analiz.toplamTutar')"
            style="width: 130px"
          >
            <template #body="s">
              <span class="gizli-veri">{{ formatCurrency(s.data.toplamTutar) }}</span>
            </template>
          </Column>
          <Column
            :header="$t('stokDetay.tur')"
            style="width: 90px"
          >
            <template #body="s">
              <span :class="['badge', s.data.tur === 'GIRIS' ? 'giris' : 'cikis']">
                {{ s.data.tur === 'GIRIS' ? $t('stokDetay.giris') : $t('stokDetay.cikis') }}
              </span>
            </template>
          </Column>
          <Column
            :header="$t('stoklar.analiz.miktar')"
            style="width: 90px"
          >
            <template #body="s">
              <span :class="s.data.tur === 'GIRIS' ? 'positive' : 'negative'">{{ s.data.miktar }}</span>
            </template>
          </Column>
          <Column :header="$t('stokDetay.aciklama')">
            <template #body="s">
              {{ s.data.aciklama || '-' }}
            </template>
          </Column>
        </AppDataTable>

        <div class="form-section-title">
          {{ $t('stoklar.fiyatlar') }}
        </div>
        <AppDataTable
          v-if="ozelFiyatlar.length"
          :value="ozelFiyatlar"
          size="small"
          striped-rows
          :paginator="false"
        >
          <Column
            field="ad"
            :header="$t('stoklar.fiyatAdi')"
          />
          <Column
            field="fiyat"
            :header="$t('stoklar.satisFiyati')"
            style="width: 150px"
          >
            <template #body="s">
              <span class="gizli-veri">{{ formatCurrency(s.data.fiyat) }}</span>
            </template>
          </Column>
        </AppDataTable>
        <span
          v-else
          class="text-muted"
        >{{ $t('stoklar.ozelFiyatYok') }}</span>
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
              <span
                class="analiz-kart-deger"
                :class="{ 'gizli-veri': o.gizli }"
              >{{ o.value }}</span>
            </div>
          </div>

          <div class="form-section-title">
            {{ t('stoklar.analiz.islemGecmisi') }}
          </div>
          <AppDataTable
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
          </AppDataTable>
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
              <AppDataTable
                v-if="musteriler.length"
                :value="musteriler"
                size="small"
                striped-rows
                :paginator="false"
              >
                <Column :header="t('stoklar.analiz.cariHesap')">
                  <template #body="s">
                    <span class="gizli-veri">{{ s.data.cariHesapAd }}</span>
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
                    <span class="gizli-veri">{{ formatCurrency(s.data.toplamTutar) }}</span>
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
              </AppDataTable>
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
              <AppDataTable
                v-if="tedarikciler.length"
                :value="tedarikciler"
                size="small"
                striped-rows
                :paginator="false"
              >
                <Column :header="t('stoklar.analiz.cariHesap')">
                  <template #body="s">
                    <span class="gizli-veri">{{ s.data.cariHesapAd }}</span>
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
                    <span class="gizli-veri">{{ formatCurrency(s.data.toplamTutar) }}</span>
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
              </AppDataTable>
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
          <AppDataTable
            v-if="aylik.length"
            :value="aylik"
            size="small"
            striped-rows
            :paginator="false"
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
          </AppDataTable>
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
const ozelFiyatlar = ref([])
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

// Kâr marjı: satış - alış üzerinden (%)
const karMarji = computed(() => {
  const alis = Number(props.stok?.fiyat || 0)
  const satis = Number(props.stok?.satisFiyati || 0)
  if (!alis || !satis) return null
  return Math.round(((satis - alis) / alis) * 1000) / 10
})

const ozelFiyatlariYukle = async () => {
  if (!props.stok?.id) return
  try {
    const r = await stokAPI.getFiyatlar(props.stok.id)
    ozelFiyatlar.value = Array.isArray(r?.data) ? r.data : []
  } catch {
    ozelFiyatlar.value = []
  }
}

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
    { label: t('stoklar.analiz.toplamAlisTutari'), value: formatCurrency(alis.toplamAlisTutari), gizli: true },
    { label: t('stoklar.analiz.toplamSatisTutari'), value: formatCurrency(satis.toplamSatisTutari), gizli: true },
    { label: t('stoklar.analiz.stokMiktari'), value: `${sayi(alis.stokMiktar)} ${birim}`.trim() },
    { label: t('stoklar.analiz.stokMaliyeti'), value: formatCurrency(k.stokMaliyeti), gizli: true },
    { label: t('stoklar.analiz.ortMaliyet'), value: formatCurrency(k.ortalamaMaliyet), gizli: true },
    { label: t('stoklar.analiz.ortalamaSatis'), value: formatCurrency(k.ortalamaSatisFiyati), gizli: true },
    { label: t('stoklar.analiz.toplamBrutKar'), value: formatCurrency(k.toplamBrutKar), gizli: true },
    { label: t('stoklar.analiz.brutKarMarji'), value: k.brutKarMarji == null ? '-' : `${sayi(k.brutKarMarji)}%`, gizli: true },
    { label: t('stoklar.analiz.satilanMiktar'), value: `${sayi(k.satilanMiktar)} ${birim}`.trim() },
    { label: t('stoklar.analiz.satisIadeTutari'), value: formatCurrency(k.satisIadeTutari), gizli: true }
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
    if (v) {
      analiziYukle()
      ozelFiyatlariYukle()
    }
  }
)
</script>

<style scoped>
.stok-detay-dialog {
  width: 980px;
  max-width: 96vw;
}
/* Genel sekmesi: ürün görseli + bilgi kartları */
.detay-ust {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-card);
  margin-bottom: 18px;
}
.detay-gorsel {
  width: 150px;
  height: 150px;
  border-radius: 12px;
  overflow: hidden;
  background: var(--bg-muted, rgba(148, 163, 184, 0.08));
  display: flex;
  align-items: center;
  justify-content: center;
}
.detay-gorsel img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.detay-gorsel i {
  font-size: 40px;
  color: var(--text-muted);
  opacity: 0.5;
}
.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 10px 16px;
}
.detail-item {
  padding: 4px 0;
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
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 10px;
  margin-bottom: 24px;
}
.analiz-kart {
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--bg-card);
  transition: border-color var(--dur-fast, 0.15s) var(--ease-standard, ease);
}
.analiz-kart:hover {
  border-color: var(--accent-border);
}
.analiz-kart-label {
  display: block;
  font-size: 11px;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 5px;
}
.analiz-kart-deger {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}
.cari-tablolar {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 8px;
}
@media (max-width: 860px) {
  .cari-tablolar {
    grid-template-columns: 1fr;
  }
  .detay-ust {
    grid-template-columns: 1fr;
  }
  .detay-gorsel {
    width: 100%;
    height: 180px;
  }
}
.cari-tablo {
  min-width: 0;
}
.analiz-filtre {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 18px;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-card);
}
.analiz-filtre-grup {
  min-width: 170px;
  flex: 1;
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
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 10px;
  background: var(--bg-card);
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
  background: var(--accent-soft-strong);
  color: var(--accent);
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