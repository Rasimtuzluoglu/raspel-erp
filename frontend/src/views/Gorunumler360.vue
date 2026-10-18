<template>
  <div class="gorunum-360">
    <PageHeader
      :title="t('gorunum360.title')"
      :subtitle="t('gorunum360.subtitle')"
      icon="pi pi-compass"
    >
      <template #actions>
        <Button
          :label="t('common.refresh')"
          icon="pi pi-refresh"
          outlined
          size="small"
          :loading="yukleniyor"
          @click="yukle"
        />
      </template>
    </PageHeader>

    <TabView v-model:active-index="aktifSekme">
      <!-- Stok Kâr 360 -->
      <TabPanel>
        <template #header>
          <span class="tab-baslik">
            <i class="pi pi-chart-line" /> {{ t('gorunum360.stokKar') }}
            <span class="tab-rozet">{{ stokKar.toplamUrun }}</span>
          </span>
        </template>
        <div class="kpi-grid">
          <KpiKart
            :baslik="t('gorunum360.toplamCiro')"
            :deger="stokKar.toplamCiro"
            ikon="pi pi-dollar"
            renk="#3b82f6"
            para-birimi
          />
          <KpiKart
            :baslik="t('gorunum360.toplamKar')"
            :deger="stokKar.toplamKar"
            ikon="pi pi-chart-line"
            renk="#10b981"
            para-birimi
          />
          <KpiKart
            :baslik="t('gorunum360.genelMarj')"
            :deger="stokKar.genelMarj"
            ikon="pi pi-percentage"
            renk="#8b5cf6"
            :para-birimi="false"
          >
            <template #deger>
              {{ stokKar.genelMarj }}%
            </template>
          </KpiKart>
          <KpiKart
            :baslik="t('gorunum360.negatifMarj')"
            :deger="stokKar.negatifMarjAdet"
            ikon="pi pi-exclamation-triangle"
            renk="#ef4444"
            :para-birimi="false"
            class="kpi-tiklanabilir"
            :class="{ 'kpi-aktif': sadeceNegatif }"
            role="button"
            tabindex="0"
            @click="sadeceNegatif = !sadeceNegatif"
            @keydown.enter.prevent="sadeceNegatif = !sadeceNegatif"
            @keydown.space.prevent="sadeceNegatif = !sadeceNegatif"
          />
          <KpiKart
            :baslik="t('gorunum360.toplamUrun')"
            :deger="stokKar.toplamUrun"
            ikon="pi pi-box"
            renk="#f59e0b"
            :para-birimi="false"
          />
        </div>
        <div class="stok-arac">
          <DatePicker
            v-model="stokBaslangic"
            date-format="dd.mm.yy"
            show-icon
            :manual-input="false"
            class="filtre-tarih"
            :placeholder="t('gorunum360.baslangic')"
            @date-select="stokKarYukle"
          />
          <DatePicker
            v-model="stokBitis"
            date-format="dd.mm.yy"
            show-icon
            :manual-input="false"
            class="filtre-tarih"
            :placeholder="t('gorunum360.bitis')"
            @date-select="stokKarYukle"
          />
          <Dropdown
            v-model="kategoriFiltresi"
            :options="kategoriler"
            option-label="ad"
            option-value="deger"
            show-clear
            class="filtre-select"
            :placeholder="t('gorunum360.kategori')"
          />
          <Button
            v-if="stokFiltreVar"
            :label="t('gorunum360.temizle')"
            icon="pi pi-filter-slash"
            class="p-button-text p-button-sm"
            @click="stokFiltreTemizle"
          />
          <span class="arac-bosluk" />
          <Button
            :label="t('gorunum360.excel')"
            icon="pi pi-file-excel"
            outlined
            size="small"
            :loading="stokExcelYukleniyor"
            @click="stokExcelIndir"
          />
        </div>

        <div class="stok-cipler">
          <button
            type="button"
            class="stok-cip"
            :class="{ aktif: !sadeceNegatif }"
            :aria-pressed="!sadeceNegatif"
            @click="sadeceNegatif = false"
          >
            <i class="pi pi-list" /> {{ t('gorunum360.cipTumu') }}
          </button>
          <button
            type="button"
            class="stok-cip"
            :class="{ aktif: sadeceNegatif }"
            :aria-pressed="sadeceNegatif"
            @click="sadeceNegatif = true"
          >
            <i class="pi pi-exclamation-triangle" /> {{ t('gorunum360.cipNegatif') }}
          </button>
        </div>

        <AppDataTable
          :value="filtreliUrunler"
          striped-rows
          arama-aktif
          :arama-placeholder="t('common.searchPlaceholder')"
          :loading="yukleniyor"
          :rows="25"
          class="tiklanabilir-tablo stok-masa"
          gorunum-anahtari="gorunum360-stok-kar"
          @row-click="urunGit"
        >
          <template #loading>
            <SkeletonLoader
              :count="6"
              height="22px"
            />
          </template>
          <Column
            field="ad"
            :header="t('gorunum360.urun')"
            sortable
          />
          <Column
            field="stokKodu"
            :header="t('gorunum360.stokKodu')"
            style="width: 130px"
            sortable
          />
          <Column
            field="satisAdet"
            :header="t('gorunum360.satisAdet')"
            style="width: 110px"
            sortable
          />
          <Column
            field="ciro"
            :header="t('gorunum360.ciro')"
            style="width: 140px"
            sortable
          >
            <template #body="{ data }">
              {{ formatCurrency(data.ciro) }}
            </template>
          </Column>
          <Column
            field="maliyet"
            :header="t('gorunum360.maliyet')"
            style="width: 140px"
            sortable
          >
            <template #body="{ data }">
              {{ formatCurrency(data.maliyet) }}
            </template>
          </Column>
          <Column
            field="brutKar"
            :header="t('gorunum360.brutKar')"
            style="width: 140px"
            sortable
          >
            <template #body="{ data }">
              <span :class="data.brutKar >= 0 ? 'pozitif' : 'negatif'">{{ formatCurrency(data.brutKar) }}</span>
            </template>
          </Column>
          <Column
            field="marj"
            :header="t('gorunum360.marj')"
            style="width: 90px"
            sortable
          >
            <template #body="{ data }">
              <Tag
                :value="`${data.marj}%`"
                :severity="data.brutKar >= 0 ? 'success' : 'danger'"
              />
            </template>
          </Column>
          <Column
            field="stokMiktar"
            :header="t('gorunum360.stokMiktar')"
            style="width: 120px"
            sortable
          />
          <template #empty>
            <EmptyState
              :message="stokFiltreVar ? t('gorunum360.filtreBos') : t('gorunum360.veriYok')"
              icon="pi pi-chart-line"
              :action-label="stokFiltreVar ? t('gorunum360.temizle') : ''"
              action-icon="pi pi-filter-slash"
              @action="stokFiltreTemizle"
            />
          </template>
        </AppDataTable>

        <!-- Mobil kart görünümü -->
        <div class="stok-mobil">
          <SkeletonLoader
            v-if="yukleniyor && !filtreliUrunler.length"
            :count="4"
            height="64px"
          />
          <EmptyState
            v-else-if="!filtreliUrunler.length"
            :message="stokFiltreVar ? t('gorunum360.filtreBos') : t('gorunum360.veriYok')"
            icon="pi pi-chart-line"
            :action-label="stokFiltreVar ? t('gorunum360.temizle') : ''"
            action-icon="pi pi-filter-slash"
            @action="stokFiltreTemizle"
          />
          <div
            v-for="u in filtreliUrunler"
            :key="u.stokId || u.ad"
            class="mobil-kart tiklanabilir"
            role="button"
            tabindex="0"
            @click="urunGit({ data: u })"
            @keydown.enter.prevent="urunGit({ data: u })"
          >
            <div class="mobil-kart-ust">
              <strong>{{ u.ad }}</strong>
              <Tag
                :value="`${u.marj}%`"
                :severity="u.brutKar >= 0 ? 'success' : 'danger'"
              />
            </div>
            <div class="mobil-kart-alt">
              <span>{{ u.stokKodu || '-' }}</span>
              <span>{{ t('gorunum360.ciro') }}: <strong>{{ formatCurrency(u.ciro) }}</strong></span>
              <span :class="u.brutKar >= 0 ? 'pozitif' : 'negatif'">
                {{ t('gorunum360.brutKar') }}: {{ formatCurrency(u.brutKar) }}
              </span>
            </div>
          </div>
        </div>
      </TabPanel>

      <!-- Çalışan Performans 360 -->
      <TabPanel>
        <template #header>
          <span class="tab-baslik">
            <i class="pi pi-users" /> {{ t('gorunum360.calisanPerformans') }}
            <span class="tab-rozet">{{ calisan.calisanlar.length }}</span>
          </span>
        </template>
        <div class="kpi-grid">
          <KpiKart
            :baslik="t('gorunum360.toplamTeslimat')"
            :deger="calisan.toplamTeslimat"
            ikon="pi pi-truck"
            renk="#3b82f6"
            :para-birimi="false"
          />
          <KpiKart
            :baslik="t('gorunum360.tamamlananTeslimat')"
            :deger="calisan.tamamlananTeslimat"
            ikon="pi pi-check-circle"
            renk="#10b981"
            :para-birimi="false"
          />
          <KpiKart
            :baslik="t('gorunum360.tamamlanmaOrani')"
            :deger="tamamlanmaOrani"
            ikon="pi pi-percentage"
            renk="#8b5cf6"
            :para-birimi="false"
          >
            <template #deger>
              {{ tamamlanmaOrani }}%
            </template>
          </KpiKart>
          <KpiKart
            :baslik="t('gorunum360.calisanSayisi')"
            :deger="calisan.calisanlar.length"
            ikon="pi pi-users"
            renk="#f59e0b"
            :para-birimi="false"
          />
        </div>
        <div class="veri-arac">
          <IconField class="filtre-arama">
            <InputIcon class="pi pi-search" />
            <InputText
              v-model="calisanArama"
              :placeholder="t('gorunum360.calisanAra')"
            />
          </IconField>
          <span class="arac-bosluk" />
          <Button
            :label="t('gorunum360.excel')"
            icon="pi pi-file-excel"
            outlined
            size="small"
            :loading="calisanExcelYukleniyor"
            @click="calisanExcelIndir"
          />
        </div>
        <div class="kart-grid">
          <SkeletonLoader
            v-if="yukleniyor && !calisan.calisanlar.length"
            class="calisan-skeleton"
            :count="4"
            height="72px"
          />
          <div
            v-for="c in filtreliCalisanlar"
            :key="c.id"
            class="calisan-kart tiklanabilir"
            role="button"
            tabindex="0"
            @click="calisanGit(c)"
            @keydown.enter.prevent="calisanGit(c)"
            @keydown.space.prevent="calisanGit(c)"
          >
            <div class="calisan-avatar">
              {{ (c.ad || '?').charAt(0) }}
            </div>
            <div class="calisan-bilgi">
              <strong>{{ c.ad }}</strong>
              <span class="calisan-rol">{{ c.rol }} · %{{ calisanOran(c) }}</span>
              <span class="calisan-tutar">{{ formatCurrency(c.teslimTutari) }}</span>
            </div>
            <div class="calisan-sayilar">
              <div class="sayi-blok">
                <span class="sayi">{{ c.toplamTeslimat }}</span>
                <small>{{ t('gorunum360.teslimat') }}</small>
              </div>
              <div class="sayi-blok">
                <span class="sayi pozitif">{{ c.tamamlananTeslimat }}</span>
                <small>{{ t('gorunum360.tamamlandi') }}</small>
              </div>
              <div class="sayi-blok">
                <span class="sayi uyari">{{ c.bekleyenTeslimat }}</span>
                <small>{{ t('gorunum360.bekleyen') }}</small>
              </div>
            </div>
          </div>
          <EmptyState
            v-if="!yukleniyor && filtreliCalisanlar.length === 0"
            :message="t('gorunum360.veriYok')"
            icon="pi pi-users"
          />
        </div>
      </TabPanel>

      <!-- Müşteri Segmentasyonu -->
      <TabPanel>
        <template #header>
          <span class="tab-baslik">
            <i class="pi pi-tags" /> {{ t('gorunum360.musteriSegment') }}
            <span class="tab-rozet">{{ segment.musteriler.length }}</span>
          </span>
        </template>
        <div class="segment-ozet-grid">
          <div
            v-for="s in segment.segmentOzeti"
            :key="s.segment"
            class="segment-kart tiklanabilir"
            :class="[segmentSinif(s.segment), { aktif: segmentFiltresi === s.segment }]"
            role="button"
            tabindex="0"
            @click="segmentFiltreUygula(s.segment)"
            @keydown.enter.prevent="segmentFiltreUygula(s.segment)"
            @keydown.space.prevent="segmentFiltreUygula(s.segment)"
          >
            <span class="segment-ad">{{ t('gorunum360.segment_' + s.segment) || s.segment }}</span>
            <strong class="segment-adet">{{ s.adet }}</strong>
            <small>{{ formatCurrency(s.toplamCiro) }}</small>
          </div>
        </div>
        <div class="veri-arac">
          <Dropdown
            v-model="segmentFiltresi"
            :options="segmentSecenekleri"
            option-label="ad"
            option-value="deger"
            show-clear
            class="filtre-select"
            :placeholder="t('gorunum360.segmentSec')"
          />
          <Button
            v-if="segmentFiltreVar"
            :label="t('gorunum360.temizle')"
            icon="pi pi-filter-slash"
            class="p-button-text p-button-sm"
            @click="segmentFiltreTemizle"
          />
          <span class="arac-bosluk" />
          <Button
            :label="t('gorunum360.excel')"
            icon="pi pi-file-excel"
            outlined
            size="small"
            :loading="segmentExcelYukleniyor"
            @click="segmentExcelIndir"
          />
        </div>
        <AppDataTable
          :value="filtreliMusteriler"
          striped-rows
          arama-aktif
          :arama-placeholder="t('common.searchPlaceholder')"
          :loading="yukleniyor"
          :rows="25"
          class="tiklanabilir-tablo segment-masa"
          gorunum-anahtari="gorunum360-segment"
          @row-click="musteriGit"
        >
          <template #loading>
            <SkeletonLoader
              :count="6"
              height="22px"
            />
          </template>
          <Column
            field="ad"
            :header="t('gorunum360.musteri')"
            sortable
          />
          <Column
            field="segment"
            :header="t('gorunum360.segment')"
            style="width: 150px"
            sortable
          >
            <template #body="{ data }">
              <span
                v-tooltip.top="data.gerekce"
                :class="['segment-rozet', segmentSinif(data.segment)]"
              >
                {{ t('gorunum360.segment_' + data.segment) }}
              </span>
            </template>
          </Column>
          <Column
            field="ciro"
            :header="t('gorunum360.ciro')"
            style="width: 140px"
            sortable
          >
            <template #body="{ data }">
              {{ formatCurrency(data.ciro) }}
            </template>
          </Column>
          <Column
            field="kalanTutar"
            :header="t('gorunum360.kalan')"
            style="width: 130px"
            sortable
          >
            <template #body="{ data }">
              <span :class="data.kalanTutar > 0 ? 'negatif' : ''">{{ formatCurrency(data.kalanTutar) }}</span>
            </template>
          </Column>
          <Column
            field="faturaAdet"
            :header="t('gorunum360.faturaAdet')"
            style="width: 110px"
            sortable
          />
          <Column
            field="sonFaturaGunOnce"
            :header="t('gorunum360.sonFatura')"
            style="width: 140px"
            sortable
          >
            <template #body="{ data }">
              {{ data.sonFaturaGunOnce != null ? t('gorunum360.gunOnce', { n: data.sonFaturaGunOnce }) : '-' }}
            </template>
          </Column>
          <template #empty>
            <EmptyState
              :message="segmentFiltreVar ? t('gorunum360.filtreBos') : t('gorunum360.veriYok')"
              icon="pi pi-tags"
              :action-label="segmentFiltreVar ? t('gorunum360.temizle') : ''"
              action-icon="pi pi-filter-slash"
              @action="segmentFiltreTemizle"
            />
          </template>
        </AppDataTable>

        <!-- Mobil kart görünümü -->
        <div class="segment-mobil">
          <SkeletonLoader
            v-if="yukleniyor && !filtreliMusteriler.length"
            :count="4"
            height="64px"
          />
          <EmptyState
            v-else-if="!filtreliMusteriler.length"
            :message="segmentFiltreVar ? t('gorunum360.filtreBos') : t('gorunum360.veriYok')"
            icon="pi pi-tags"
            :action-label="segmentFiltreVar ? t('gorunum360.temizle') : ''"
            action-icon="pi pi-filter-slash"
            @action="segmentFiltreTemizle"
          />
          <div
            v-for="m in filtreliMusteriler"
            :key="m.cariId || m.ad"
            class="mobil-kart tiklanabilir"
            role="button"
            tabindex="0"
            @click="musteriGit({ data: m })"
            @keydown.enter.prevent="musteriGit({ data: m })"
          >
            <div class="mobil-kart-ust">
              <strong>{{ m.ad }}</strong>
              <span :class="['segment-rozet', segmentSinif(m.segment)]">
                {{ t('gorunum360.segment_' + m.segment) }}
              </span>
            </div>
            <div class="mobil-kart-alt">
              <span>{{ t('gorunum360.ciro') }}: <strong>{{ formatCurrency(m.ciro) }}</strong></span>
              <span :class="m.kalanTutar > 0 ? 'negatif' : ''">
                {{ t('gorunum360.kalan') }}: {{ formatCurrency(m.kalanTutar) }}
              </span>
            </div>
          </div>
        </div>
      </TabPanel>
    </TabView>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { raporAPI, excelAPI } from '../api/index.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { formatCurrency } from '../utils/format.js'
import PageHeader from '../components/PageHeader.vue'
import KpiKart from '../components/KpiKart.vue'
import EmptyState from '../components/EmptyState.vue'
import SkeletonLoader from '../components/SkeletonLoader.vue'

const { t } = useI18n()
const router = useRouter()
const toastBildirim = useToastBildirim()
const yukleniyor = ref(false)

// Aktif sekme kalıcı hatırlanır (0..2 aralığına kilitli).
const AKTIF_SEKME_ANAHTAR = 'raspel_gorunum360_tab'
const aktifSekme = ref(
  (() => {
    const v = Number(localStorage.getItem(AKTIF_SEKME_ANAHTAR))
    return Number.isInteger(v) && v >= 0 && v <= 2 ? v : 0
  })()
)
watch(aktifSekme, (v) => localStorage.setItem(AKTIF_SEKME_ANAHTAR, String(v)))

const stokKar = ref({ urunler: [], toplamCiro: 0, toplamKar: 0, genelMarj: 0, negatifMarjAdet: 0, toplamUrun: 0 })
const calisan = ref({ calisanlar: [], toplamTeslimat: 0, tamamlananTeslimat: 0 })
const segment = ref({ segmentOzeti: [], musteriler: [] })

// Stok Kâr filtreleri (tarih aralığı sunucu taraflı, kategori/negatif istemci taraflı).
const stokBaslangic = ref(null)
const stokBitis = ref(null)
const kategoriFiltresi = ref(null)
const sadeceNegatif = ref(false)
const stokExcelYukleniyor = ref(false)

const kategoriler = computed(() => {
  const set = new Set()
  ;(stokKar.value.urunler || []).forEach((u) => {
    if (u.kategori) set.add(u.kategori)
  })
  return Array.from(set)
    .sort((a, b) => a.localeCompare(b, 'tr'))
    .map((k) => ({ ad: k, deger: k }))
})

const filtreliUrunler = computed(() => {
  let liste = stokKar.value.urunler || []
  if (kategoriFiltresi.value) liste = liste.filter((u) => u.kategori === kategoriFiltresi.value)
  if (sadeceNegatif.value) liste = liste.filter((u) => Number(u.brutKar) < 0)
  return liste
})

const stokFiltreVar = computed(
  () => !!stokBaslangic.value || !!stokBitis.value || !!kategoriFiltresi.value || sadeceNegatif.value
)

// Çalışan sekmesi: arama + Excel.
const calisanArama = ref('')
const calisanExcelYukleniyor = ref(false)
const filtreliCalisanlar = computed(() => {
  const q = calisanArama.value.trim().toLowerCase()
  const liste = calisan.value.calisanlar || []
  if (!q) return liste
  return liste.filter((c) => (c.ad || '').toLowerCase().includes(q))
})
const calisanOran = (c) => {
  const toplam = Number(c?.toplamTeslimat) || 0
  if (toplam <= 0) return 0
  return Math.round(((Number(c?.tamamlananTeslimat) || 0) / toplam) * 100)
}

// Segment sekmesi: segment filtresi + Excel.
const segmentFiltresi = ref(null)
const segmentExcelYukleniyor = ref(false)
const segmentSecenekleri = computed(() =>
  (segment.value.segmentOzeti || []).map((s) => ({
    ad: t('gorunum360.segment_' + s.segment) || s.segment,
    deger: s.segment
  }))
)
const filtreliMusteriler = computed(() => {
  const liste = segment.value.musteriler || []
  if (!segmentFiltresi.value) return liste
  return liste.filter((m) => m.segment === segmentFiltresi.value)
})
const segmentFiltreVar = computed(() => !!segmentFiltresi.value)

// Çalışan tamamlanma oranı (%). Toplam 0 ise 0.
const tamamlanmaOrani = computed(() => {
  const toplam = Number(calisan.value.toplamTeslimat) || 0
  if (toplam <= 0) return 0
  return Math.round(((Number(calisan.value.tamamlananTeslimat) || 0) / toplam) * 100)
})

const segmentSinif = (s) => ({
  VIP: 'segment-vip',
  DUZENLI: 'segment-duzenli',
  YENI: 'segment-yeni',
  RISKLI: 'segment-riskli',
  PASIF: 'segment-pasif',
  GELISMEDE: 'segment-gelismede'
})[s] || ''

const yukle = async () => {
  yukleniyor.value = true
  const [s, c, m] = await Promise.allSettled([
    raporAPI.stokKar360(),
    raporAPI.calisanPerformans360(),
    raporAPI.musteriSegment()
  ])
  if (s.status === 'fulfilled') stokKar.value = s.value.data || stokKar.value
  if (c.status === 'fulfilled') calisan.value = c.value.data || calisan.value
  if (m.status === 'fulfilled') segment.value = m.value.data || segment.value
  if ([s, c, m].some((r) => r.status === 'rejected')) {
    toastBildirim.hata(t('gorunum360.hataYukleme'))
  }
  yukleniyor.value = false
}

const tarihIso = (d) => {
  if (!d) return null
  const date = d instanceof Date ? d : new Date(d)
  if (Number.isNaN(date.getTime())) return null
  const ay = String(date.getMonth() + 1).padStart(2, '0')
  const gun = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${ay}-${gun}`
}

const stokParams = () => {
  const p = {}
  if (stokBaslangic.value) p.baslangic = tarihIso(stokBaslangic.value)
  if (stokBitis.value) p.bitis = tarihIso(stokBitis.value)
  return p
}

const stokKarYukle = async () => {
  yukleniyor.value = true
  try {
    const r = await raporAPI.stokKar360(stokParams())
    stokKar.value = r.data || stokKar.value
  } catch {
    toastBildirim.hata(t('gorunum360.hataYukleme'))
  } finally {
    yukleniyor.value = false
  }
}

const stokFiltreTemizle = () => {
  stokBaslangic.value = null
  stokBitis.value = null
  kategoriFiltresi.value = null
  sadeceNegatif.value = false
  stokKarYukle()
}

// Excel indirme ortak yardımcısı (blob -> .xlsx).
const excelBlobIndir = async (istek, dosyaAdi, yukleniyorRef) => {
  yukleniyorRef.value = true
  try {
    const res = await istek()
    const url = window.URL.createObjectURL(
      new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    )
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `${dosyaAdi}-${new Date().toISOString().slice(0, 10)}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    toastBildirim.basarili(t('gorunum360.excelIndirildi'))
  } catch {
    toastBildirim.hata(t('gorunum360.excelHata'))
  } finally {
    yukleniyorRef.value = false
  }
}

const stokExcelIndir = () =>
  excelBlobIndir(() => excelAPI.gorunum360StokKar(stokParams()), 'gorunum360-stok-kar', stokExcelYukleniyor)
const calisanExcelIndir = () =>
  excelBlobIndir(() => excelAPI.gorunum360Calisan(), 'gorunum360-calisan', calisanExcelYukleniyor)
const segmentExcelIndir = () =>
  excelBlobIndir(() => excelAPI.gorunum360Segment(), 'gorunum360-segment', segmentExcelYukleniyor)

// Çapraz gezinme: ürün -> Stoklar, çalışan -> Teslimatlar, müşteri -> Cari Hesaplar.
const urunGit = (e) => {
  const u = e?.data
  if (u) router.push({ name: 'Stoklar', query: { q: u.stokKodu || u.ad || '' } })
}
const calisanGit = (c) => {
  router.push({ name: 'Teslimatlar', query: { sofor: c?.ad || '' } })
}
const musteriGit = (e) => {
  const ad = e?.data?.ad
  if (ad) router.push({ name: 'CariHesaplar', query: { q: ad } })
}
const segmentFiltreUygula = (segment) => {
  segmentFiltresi.value = segmentFiltresi.value === segment ? null : segment
}
const segmentFiltreTemizle = () => {
  segmentFiltresi.value = null
}

onMounted(yukle)
</script>

<style scoped>
.gorunum-360 {
  padding: 0;
}
.tab-baslik {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.tab-rozet {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: var(--accent-soft, rgba(245, 158, 11, 0.16));
  color: var(--accent);
  font-size: 10.5px;
  font-weight: 700;
}
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}
.kpi-tiklanabilir {
  cursor: pointer;
}
.kpi-tiklanabilir.kpi-aktif {
  border-color: var(--accent-border, rgba(245, 158, 11, 0.55));
  box-shadow: 0 0 0 2px var(--accent-soft, rgba(245, 158, 11, 0.18));
}
.kpi-tiklanabilir:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.stok-arac {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.filtre-tarih {
  min-width: 150px;
}
.filtre-select {
  min-width: 160px;
}
.arac-bosluk {
  flex: 1 1 auto;
}
.stok-cipler {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.stok-cip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.stok-cip:hover {
  border-color: var(--accent-border);
  color: var(--text-primary);
}
.stok-cip.aktif {
  background: var(--accent-soft, rgba(245, 158, 11, 0.16));
  border-color: var(--accent-border, rgba(245, 158, 11, 0.5));
  color: var(--accent);
}
.stok-cip i {
  font-size: 12px;
}
.veri-arac {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.filtre-arama {
  min-width: 220px;
  flex: 1 1 220px;
  max-width: 320px;
}
.calisan-kart.tiklanabilir {
  cursor: pointer;
  transition: transform var(--dur-base, 0.2s) var(--ease-standard, ease), border-color var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.calisan-kart.tiklanabilir:hover {
  transform: translateY(-2px);
  border-color: var(--accent-border);
}
.segment-kart.tiklanabilir {
  cursor: pointer;
  transition: border-color var(--dur-base, 0.2s) var(--ease-standard, ease), box-shadow var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.segment-kart.tiklanabilir:hover {
  border-color: var(--accent-border);
}
.segment-kart.aktif {
  border-color: var(--accent-border, rgba(245, 158, 11, 0.55));
  box-shadow: 0 0 0 2px var(--accent-soft, rgba(245, 158, 11, 0.18));
}
.tiklanabilir-tablo :deep(.p-datatable-tbody > tr) {
  cursor: pointer;
}

/* Mobil kart görünümleri (masaüstünde gizli). */
.stok-mobil,
.segment-mobil {
  display: none;
}
.mobil-kart {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-card);
}
.mobil-kart.tiklanabilir {
  cursor: pointer;
}
.mobil-kart-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.mobil-kart-ust strong {
  font-size: 13.5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.mobil-kart-alt {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 12.5px;
  color: var(--text-secondary);
}

@media (max-width: 768px) {
  .stok-masa,
  .segment-masa {
    display: none;
  }
  .stok-mobil,
  .segment-mobil {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .calisan-sayilar {
    gap: 10px;
  }
  .calisan-kart {
    padding: 12px;
    gap: 10px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .calisan-kart.tiklanabilir,
  .segment-kart.tiklanabilir,
  .stok-cip,
  .mobil-kart.tiklanabilir {
    transition: none;
  }
  .calisan-kart.tiklanabilir:hover {
    transform: none;
  }
}
.kart-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 14px;
}
.calisan-skeleton {
  grid-column: 1 / -1;
}
.calisan-kart {
  display: flex;
  align-items: center;
  gap: 14px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px;
}
.calisan-avatar {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent), var(--accent-hover));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 18px;
  flex-shrink: 0;
}
.calisan-bilgi {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1;
}
.calisan-rol {
  font-size: 11px;
  color: var(--text-muted);
}
.calisan-tutar {
  font-size: 11px;
  color: var(--text-secondary);
  font-variant-numeric: tabular-nums;
}
.calisan-sayilar {
  display: flex;
  gap: 16px;
}
.sayi-blok {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.sayi {
  font-size: 20px;
  font-weight: 800;
  color: var(--text-primary);
}
.sayi.pozitif {
  color: #4ade80;
}
.sayi.uyari {
  color: #fb923c;
}
.sayi-blok small {
  font-size: 10px;
  color: var(--text-muted);
}
.segment-ozet-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.segment-kart {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 14px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: var(--bg-card);
}
.segment-ad {
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.segment-adet {
  font-size: 24px;
}
.segment-rozet {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
  cursor: help;
}
.segment-vip { background: rgba(168, 85, 247, 0.15); color: #c084fc; }
.segment-duzenli { background: rgba(34, 197, 94, 0.15); color: #4ade80; }
.segment-yeni { background: rgba(96, 165, 250, 0.15); color: #60a5fa; }
.segment-riskli { background: rgba(239, 68, 68, 0.15); color: #f87171; }
.segment-pasif { background: rgba(148, 163, 184, 0.15); color: #94a3b8; }
.segment-gelismede { background: rgba(251, 191, 36, 0.15); color: #fbbf24; }
.pozitif { color: #4ade80; }
.negatif { color: #f87171; }
</style>
