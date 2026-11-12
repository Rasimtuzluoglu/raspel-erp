<template>
  <div class="faturalar-container">
    <PageHeader
      :title="t('faturalar.title')"
      icon="pi pi-file-invoice"
    >
      <template #actions>
        <Button
          :label="t('faturalar.yeniFatura')"
          icon="pi pi-plus"
          class="p-button-success"
          @click="openCreateDialog"
        />
        <TarihHizliSecim v-model="tarihAraligi" />
        <Button
          label="Excel"
          icon="pi pi-file-excel"
          class="p-button-sm p-button-outlined"
          @click="excelIndir"
        />
      </template>
    </PageHeader>

    <IlkZiyaretIpuclari
      anahtar="faturalar"
      :baslik="t('faturalar.ipucuBaslik')"
      :metin="t('faturalar.ipucuMetin')"
    />

    <div
      v-if="!ilkYukleme"
      class="fatura-kpi"
    >
      <KpiKart
        :baslik="t('faturalar.kpiAdet')"
        :deger="faturaOzet.adet ?? 0"
        :para-birimi="false"
        ikon="pi pi-file"
        renk="#14b8a6"
      />
      <KpiKart
        :baslik="t('faturalar.kpiCiro')"
        :deger="faturaOzet.ciro ?? 0"
        ikon="pi pi-chart-line"
        renk="#22c55e"
      />
      <KpiKart
        :baslik="t('faturalar.kpiTahsilEdilen')"
        :deger="faturaOzet.tahsilEdilen ?? 0"
        ikon="pi pi-wallet"
        renk="#3b82f6"
      />
      <KpiKart
        :baslik="t('faturalar.kpiKalan')"
        :deger="faturaOzet.kalan ?? 0"
        ikon="pi pi-exclamation-circle"
        renk="#ef4444"
      />
    </div>

    <!-- Liste filtreleri: arama + tur secimi + durum cipleri + gelismis popover -->
    <div class="filtre-cubugu">
      <IconField class="arama-kutu">
        <InputIcon class="pi pi-search" />
        <InputText
          v-model="arama"
          :placeholder="t('faturalar.aramaPlaceholder')"
          class="arama-input"
          @input="aramaDebounce"
        />
      </IconField>
      <Dropdown
        v-model="turFiltre"
        :options="turFiltreSecenekleri"
        option-label="label"
        option-value="value"
        class="filtre-secim tur-filtre-secim"
        :placeholder="t('faturalar.filtreTumu')"
        data-test="fatura-tur-filtre"
      />
      <span class="filtre-ayrac" />
      <div
        class="durum-cipleri"
        role="group"
        :aria-label="t('common.status')"
      >
        <button
          v-for="d in durumFiltreSecenekleri"
          :key="d.value ?? 'tumu'"
          type="button"
          class="durum-cip"
          :class="{ aktif: durumFiltre === d.value }"
          :aria-pressed="durumFiltre === d.value ? 'true' : 'false'"
          @click="durumFiltre = d.value"
        >
          {{ d.label }}
        </button>
      </div>
      <button
        type="button"
        class="filtre-gelismis-btn"
        :class="{ aktif: gelismisFiltreSayisi > 0 }"
        :title="t('faturalar.gelismisFiltre')"
        :aria-label="t('faturalar.gelismisFiltre')"
        @click="filtrePopover.toggle($event)"
      >
        <i class="pi pi-filter" />
        <span class="filtre-gelismis-metin">{{ t('faturalar.gelismisFiltre') }}</span>
        <span
          v-if="gelismisFiltreSayisi > 0"
          class="filtre-rozet"
        >{{ gelismisFiltreSayisi }}</span>
      </button>
      <Popover ref="filtrePopover">
        <div class="filtre-panel">
          <div class="filtre-panel-baslik">
            {{ t('faturalar.gelismisFiltre') }}
          </div>
          <div class="filtre-alan">
            <label>{{ t('faturalar.colOdemeDurumu') }}</label>
            <Dropdown
              v-model="odemeFiltre"
              :options="odemeFiltreSecenekleri"
              option-label="label"
              option-value="value"
              show-clear
              :placeholder="t('faturalar.filtreTumu')"
              class="w-full"
            />
          </div>
          <label class="filtre-switch">
            <span>{{ t('faturalar.vadesiGecen') }}</span>
            <ToggleSwitch v-model="vadesiGecenFiltre" />
          </label>
          <label class="filtre-switch">
            <span>{{ t('faturalar.teslimatiOlan') }}</span>
            <ToggleSwitch v-model="teslimatiOlanFiltre" />
          </label>
          <Button
            :label="t('faturalar.filtreTemizle')"
            icon="pi pi-filter-slash"
            class="p-button-text p-button-sm"
            @click="filtreTemizle"
          />
        </div>
      </Popover>
      <div class="filtre-sag">
        <TabloAyarlari
          tablo-key="fatura"
          :kolonlar="kolonlar"
          @update:kolonlar="kolonGuncelle"
          @update:yogunluk="tabloYogunluk = $event"
        />
      </div>
    </div>

    <div
      v-if="aktifFiltreler.length > 0"
      class="fatura-aktif-filtreler"
      data-test="fatura-aktif-filtreler"
    >
      <span class="fatura-aktif-etiket">{{ t('faturalar.aktifFiltreler') }}</span>
      <button
        v-for="f in aktifFiltreler"
        :key="f.tip"
        type="button"
        class="fatura-filtre-cip"
        :aria-label="`${f.etiket} — ${t('faturalar.filtreKaldir')}`"
        @click="filtreCipKaldir(f.tip)"
      >
        {{ f.etiket }}
        <i
          class="pi pi-times"
          aria-hidden="true"
        />
      </button>
      <Button
        :label="t('faturalar.temizle')"
        icon="pi pi-filter-slash"
        class="p-button-text p-button-sm fatura-filtre-temizle"
        data-test="fatura-filtre-temizle"
        @click="filtreTemizle"
      />
    </div>

    <div
      v-if="ilkYukleme"
      class="loading-iskelet"
      :aria-label="t('common.loading')"
    >
      <SkeletonLoader
        :count="6"
        height="44px"
      />
    </div>

    <div
      v-if="!ilkYukleme"
      class="table-container"
    >
      <AppDataTable
        v-model:selection="selectedItems"
        selection-mode="multiple"
        :value="faturaStore.faturalar"
        :loading="loading"
        :size="tabloYogunluk === 'compact' ? 'small' : 'normal'"
        striped-rows
        :lazy="true"
        :total-records="faturaStore.toplamKayit"
        :rows="sayfaBoyutu"
        :paginator="true"
        :sort-field="siralama.field"
        :sort-order="siralama.order"
        paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport RowsPerPageDropdown"
        :rows-per-page-options="[10, 20, 50]"
        :current-page-report-template="'{first} - {last} ({totalRecords} ' + $t('common.recordsWord') + ')'"
        gorunum-anahtari="faturalar"
        @page="sayfaDegisti"
        @sort="sirala"
      >
        <Column
          selection-mode="multiple"
          header-style="width: 3rem"
          class="fatura-secim-kolon"
        />
        <Column
          v-if="kolonGorunur('faturaNumarasi')"
          field="faturaNumarasi"
          :header="t('faturalar.colFaturaNo')"
          sortable
          class="fatura-no-kolon"
          style="width: 160px"
        />
        <Column
          v-if="kolonGorunur('tarih')"
          field="tarih"
          :header="t('common.date')"
          sortable
          style="width: 110px"
        >
          <template #body="s">
            {{ formatDate(s.data.tarih) }}
          </template>
        </Column>
        <Column
          v-if="kolonGorunur('tur')"
          field="tur"
          :header="t('faturalar.colTur')"
          style="width: 90px"
        >
          <template #body="s">
            <span :class="['badge', s.data.tur === 'SATIS' ? 'satis' : 'alis']">
              {{ s.data.tur === 'SATIS' ? t('faturalar.satis') : t('faturalar.alis') }}
            </span>
          </template>
        </Column>
        <Column
          v-if="kolonGorunur('cariHesapAd')"
          field="cariHesapAd"
          :header="t('faturalar.colCari')"
          sortable
          style="width: 180px"
        >
          <template #body="s">
            {{ s.data.cariHesapAd || '-' }}
          </template>
        </Column>
        <Column
          v-if="kolonGorunur('genelToplam')"
          field="genelToplam"
          :header="t('faturalar.colToplam')"
          class="sayisal"
          sortable
          style="width: 130px"
        >
          <template #body="s">
            {{ formatPara(s.data.genelToplam, s.data.paraBirimi) }}
          </template>
        </Column>
        <Column
          v-if="kolonGorunur('odemeDurumu')"
          field="odemeDurumu"
          :header="t('faturalar.colOdemeDurumu')"
          style="width: 130px"
        >
          <template #body="s">
            <span :class="['odeme-badge', (s.data.odemeDurumu || '').toLowerCase()]">
              {{ odemeDurumEtiket(s.data.odemeDurumu) }}
            </span>
          </template>
        </Column>
        <Column
          v-if="kolonGorunur('kalanTutar')"
          field="kalanTutar"
          :header="t('faturalar.colKalan')"
          class="sayisal"
          sortable
          style="width: 120px"
        >
          <template #body="s">
            <span
              v-if="s.data.kalanTutar && s.data.kalanTutar > 0"
              class="kalan-tutar"
            >{{ formatPara(s.data.kalanTutar, s.data.paraBirimi) }}</span>
            <span
              v-else
              class="islem-yapan-bos"
            >-</span>
          </template>
        </Column>
        <Column
          v-if="kolonGorunur('durum')"
          field="durum"
          :header="t('common.status')"
          style="width: 150px"
        >
          <template #body="s">
            <div class="durum-hucre">
              <span :class="['durum-badge', (s.data.durum || '').toLowerCase()]">
                {{ durumLabel(s.data.durum) }}
              </span>
              <Tag
                v-if="gecikmisGun(s.data) > 0"
                class="gecikme-rozet"
                :value="t('faturalar.gunGecikti', { n: gecikmisGun(s.data) })"
                severity="danger"
              />
            </div>
          </template>
        </Column>
        <Column
          v-if="kolonGorunur('yazdirmaSayisi')"
          field="yazdirmaSayisi"
          :header="t('faturalar.colYazdirma')"
          style="width: 110px"
        >
          <template #body="s">
            <span
              v-if="s.data.yazdirmaSayisi"
              class="yazdirma-rozet"
              :title="yazdirmaBaslik(s.data)"
            >
              <i class="pi pi-print" /> {{ s.data.yazdirmaSayisi }}×
              <small v-if="s.data.sonYazdirmaFormat">{{ s.data.sonYazdirmaFormat }}</small>
            </span>
            <span
              v-else
              class="islem-yapan-bos"
            >-</span>
          </template>
        </Column>
        <Column
          v-if="kolonGorunur('olusturanKullaniciAdi')"
          field="olusturanKullaniciAdi"
          :header="t('faturalar.colIslemiYapan')"
          style="width: 140px"
        >
          <template #body="s">
            <span
              v-if="s.data.olusturanKullaniciAdi"
              class="islem-yapan"
            >{{ s.data.olusturanKullaniciAdi }}</span>
            <span
              v-else
              class="islem-yapan-bos"
            >-</span>
          </template>
        </Column>
        <Column
          v-if="kolonGorunur('teslimEden')"
          field="teslimEden"
          :header="t('faturalar.colTeslimEden')"
          style="width: 140px"
        >
          <template #body="s">
            <span
              v-if="s.data.teslimEden"
              class="teslim-eden-list"
            ><i class="pi pi-truck" /> {{ s.data.teslimEden }}</span>
            <span
              v-else
              class="islem-yapan-bos"
            >-</span>
          </template>
        </Column>
        <Column
          v-if="kolonGorunur('teslimatDurum')"
          :header="t('faturalar.colTeslimatDurum')"
          style="width: 130px"
        >
          <template #body="s">
            <Tag
              v-if="s.data.teslimatVar"
              :value="teslimatDurumEtiket(s.data.teslimatDurum)"
              :severity="TESLIMAT_DURUM_SEVERITY[s.data.teslimatDurum] || 'secondary'"
              :title="s.data.driverAd || ''"
              style="cursor: pointer"
              @click="router.push({ name: 'Teslimatlar' })"
            />
            <span
              v-else
              class="islem-yapan-bos"
            >-</span>
          </template>
        </Column>
        <Column
          :header="t('common.actions')"
          class="fatura-islem-kolon"
          style="width: 110px"
        >
          <template #body="s">
            <div class="satir-islemler fatura-satir-eylemler">
              <Button
                icon="pi pi-eye"
                class="p-button-rounded p-button-sm p-button-text p-button-info"
                :title="t('faturalar.goruntule')"
                :aria-label="t('faturalar.goruntule')"
                data-test="fatura-goruntule"
                @click="viewFatura(s.data.id)"
              />
              <Button
                v-if="tahsilatYapilabilir(s.data)"
                icon="pi pi-money-bill"
                class="p-button-rounded p-button-sm p-button-text p-button-success"
                :title="t('faturalar.tahsilatAl')"
                :aria-label="t('faturalar.tahsilatAl')"
                data-test="fatura-tahsilat"
                @click="tahsilataGit(s.data)"
              />
              <SatirEylemleri
                :gorunur="{ duzenle: false, cogalt: false, sil: false }"
                :items="faturaEylemleri(s.data)"
                :baslik="`${s.data.faturaNumarasi} · ${s.data.cariHesapAd || ''}`"
              />
            </div>
          </template>
        </Column>
        <template #batch-actions>
          <Button
            v-permission="'FATURA_DELETE'"
            :label="t('faturalar.secilenleriSil')"
            icon="pi pi-trash"
            class="p-button-danger p-button-sm"
            :loading="topluSiliniyor"
            @click="topluSil()"
          />
        </template>
        <template #empty>
          <EmptyState
            v-if="faturaStore.faturalar && faturaStore.faturalar.length === 0"
            :message="t('faturalar.empty')"
            :sub-message="t('faturalar.emptyHint')"
            icon="pi pi-file"
            :action-label="t('faturalar.emptyAction')"
            action-icon="pi pi-plus"
            @action="openCreateDialog"
          />
        </template>
      </AppDataTable>
    </div>

    <!-- Kucuk ekran: tablo yerine kart listesi (CSS ile <768px gosterilir) -->
    <div
      v-if="!ilkYukleme"
      class="fatura-kart-liste"
      :aria-busy="loading"
    >
      <div
        v-for="f in (faturaStore.faturalar || [])"
        :key="f.id"
        class="fatura-kart"
      >
        <div class="fatura-kart-ust">
          <div class="fatura-kart-baslik">
            <strong>{{ f.faturaNumarasi }}</strong>
            <span :class="['badge', f.tur === 'SATIS' ? 'satis' : 'alis']">
              {{ f.tur === 'SATIS' ? t('faturalar.satis') : t('faturalar.alis') }}
            </span>
          </div>
          <span class="fatura-kart-tutar">{{ formatPara(f.genelToplam, f.paraBirimi) }}</span>
        </div>
        <div class="fatura-kart-alt">
          <span class="fatura-kart-cari"><i class="pi pi-user" /> {{ f.cariHesapAd || '-' }}</span>
          <span><i class="pi pi-calendar" /> {{ formatDate(f.tarih) }}</span>
          <span v-if="f.vadeTarihi"><i class="pi pi-clock" /> {{ formatDate(f.vadeTarihi) }}</span>
          <Tag
            v-if="gecikmisGun(f) > 0"
            class="gecikme-rozet"
            :value="t('faturalar.gunGecikti', { n: gecikmisGun(f) })"
            severity="danger"
          />
        </div>
        <div class="fatura-kart-rozetler">
          <span :class="['odeme-badge', (f.odemeDurumu || '').toLowerCase()]">{{ odemeDurumEtiket(f.odemeDurumu) }}</span>
          <span :class="['durum-badge', (f.durum || '').toLowerCase()]">{{ durumLabel(f.durum) }}</span>
          <Tag
            v-if="f.teslimatVar"
            :value="teslimatDurumEtiket(f.teslimatDurum)"
            :severity="TESLIMAT_DURUM_SEVERITY[f.teslimatDurum] || 'secondary'"
            :title="f.driverAd || ''"
            data-test="fatura-kart-teslimat"
          />
          <span
            v-if="f.kalanTutar && f.kalanTutar > 0"
            class="kalan-tutar"
          >{{ formatPara(f.kalanTutar, f.paraBirimi) }}</span>
        </div>
        <div class="fatura-kart-islemler">
          <div class="fatura-kart-eylem-grup">
            <Button
              icon="pi pi-eye"
              class="p-button-rounded p-button-sm p-button-info"
              :title="t('faturalar.goruntule')"
              :aria-label="t('faturalar.goruntule')"
              data-test="fatura-kart-goruntule"
              @click="viewFatura(f.id)"
            />
            <Button
              v-if="tahsilatYapilabilir(f)"
              icon="pi pi-money-bill"
              class="p-button-rounded p-button-sm p-button-success"
              :title="t('faturalar.tahsilatAl')"
              :aria-label="t('faturalar.tahsilatAl')"
              data-test="fatura-kart-tahsilat"
              @click="tahsilataGit(f)"
            />
            <SatirEylemleri
              :gorunur="{ duzenle: false, cogalt: false, sil: false }"
              :items="faturaEylemleri(f)"
              :baslik="`${f.faturaNumarasi} · ${f.cariHesapAd || ''}`"
            />
          </div>
        </div>
      </div>
    </div>


    <Message
      v-if="faturaStore.error"
      severity="error"
      :text="faturaStore.error"
    />

    <FaturaTasarimModal
      v-model:visible="tasarimModalAcik"
      :fatura-id="seciliFaturaId"
    />

    <FaturaGecmisDialog
      v-model:visible="gecmisAcik"
      :fatura="gecmisFatura"
    />

    <SoforAtaDialog
      ref="soforDialogRef"
      @saved="loadFaturalar"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { useI18n } from 'vue-i18n'
import { useFaturaStore } from '../stores/faturaStore.js'
import { useAuthStore } from '../stores/authStore.js'
import { faturaAPI, excelAPI, pdfAPI } from '../api/index.js'
import { useKisayollar } from '../composables/useKisayollar.js'
import TarihHizliSecim from '../components/TarihHizliSecim.vue'
import TabloAyarlari from '../components/TabloAyarlari.vue'
import IlkZiyaretIpuclari from '../components/IlkZiyaretIpuclari.vue'
import FaturaTasarimModal from '../components/FaturaTasarimModal.vue'
import SoforAtaDialog from '../components/SoforAtaDialog.vue'
import { formatPara, getLocalDateString } from '../utils/format.js'

const router = useRouter()
const route = useRoute()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { t } = useI18n()
const faturaStore = useFaturaStore()
const authStore = useAuthStore()

const tasarimModalAcik = ref(false)
const seciliFaturaId = ref(null)

const gecmisAcik = ref(false)
const gecmisFatura = ref(null)
const gecmisAc = (f) => {
  gecmisFatura.value = f
  gecmisAcik.value = true
}

const tasarlaVeYazdir = (id) => {
  seciliFaturaId.value = id
  tasarimModalAcik.value = true
}

useKisayollar({
  yeni: () => openCreateDialog()
})

const loading = ref(false)
// Ilk yukleme skeleton'i yalnizca bir kez gosterilir; sonraki filtre/sayfa
// yuklemelerinde KPI ve tablo DOM'da kalir (flicker onlenir).
const ilkYukleme = ref(true)
const tarihAraligi = ref(null)
const selectedItems = ref([])
const topluSiliniyor = ref(false)
const arama = ref('')
let aramaZamanlayici = null
const sayfa = ref(0)
const sayfaBoyutu = ref(10)
const siralama = ref({ field: 'tarih', order: -1 })

// Tablo kolon ayarlari: varsayilan gorunumde yalnizca gunluk kullanim kolonlari
// acik; tur, yazdirma, islemi yapan, teslim eden ve teslimat durumu ayarlardan
// acilabilir. Fatura no ve islem kolonlari yatay kaydirmada sabittir.
const tabloYogunluk = ref('comfortable')
const varsayilanKolonGorunurluk = {
  tur: false,
  yazdirmaSayisi: false,
  olusturanKullaniciAdi: false,
  teslimEden: false,
  teslimatDurum: false
}
const kolonGorunurluk = ref({ ...varsayilanKolonGorunurluk })
const varsayilanKolonlar = computed(() => [
  { field: 'faturaNumarasi', header: t('faturalar.colFaturaNo') },
  { field: 'tarih', header: t('common.date') },
  { field: 'tur', header: t('faturalar.colTur') },
  { field: 'cariHesapAd', header: t('faturalar.colCari') },
  { field: 'genelToplam', header: t('faturalar.colToplam') },
  { field: 'odemeDurumu', header: t('faturalar.colOdemeDurumu') },
  { field: 'kalanTutar', header: t('faturalar.colKalan') },
  { field: 'durum', header: t('common.status') },
  { field: 'yazdirmaSayisi', header: t('faturalar.colYazdirma') },
  { field: 'olusturanKullaniciAdi', header: t('faturalar.colIslemiYapan') },
  { field: 'teslimEden', header: t('faturalar.colTeslimEden') },
  { field: 'teslimatDurum', header: t('faturalar.colTeslimatDurum') }
])
const kolonlar = computed(() =>
  varsayilanKolonlar.value.map((k) => ({ ...k, visible: kolonGorunurluk.value[k.field] !== false }))
)
const kolonGorunur = (field) => kolonGorunurluk.value[field] !== false
const kolonGuncelle = (yeni) => {
  const map = {}
  ;(yeni || []).forEach((k) => {
    map[k.field] = k.visible !== false
  })
  kolonGorunurluk.value = map
}

// Liste filtreleri (sunucu tarafli): tur/durum/odeme/vadesi gecen/teslimat.
const turFiltre = ref(null)
const durumFiltre = ref(null)
const odemeFiltre = ref(null)
const vadesiGecenFiltre = ref(false)
const teslimatiOlanFiltre = ref(false)
const filtrePopover = ref(null)

const tarihParametreleri = () => {
  const params = {}
  const aralik = tarihAraligi.value
  if (aralik && aralik.length === 2 && aralik[0] && aralik[1]) {
    params.bas = getLocalDateString(aralik[0])
    params.bit = getLocalDateString(aralik[1])
  }
  return params
}

// KPI ozeti: gerceklesmis (KESILDI) faturalar; tur + tarih araligina gore.
const faturaOzet = ref({})
const ozetYukle = async () => {
  try {
    const p = { ...tarihParametreleri() }
    if (arama.value.trim()) p.search = arama.value.trim()
    if (turFiltre.value) p.tur = turFiltre.value
    if (durumFiltre.value) p.durum = durumFiltre.value
    if (odemeFiltre.value) p.odemeDurumu = odemeFiltre.value
    if (vadesiGecenFiltre.value) p.vadesiGecen = true
    if (teslimatiOlanFiltre.value) p.teslimatVar = true
    const r = await faturaAPI.ozet(p)
    faturaOzet.value = r.data || {}
  } catch {
    faturaOzet.value = {}
  }
}

const loadFaturalar = async (yeniSayfa = sayfa.value, yeniBoyut = sayfaBoyutu.value) => {
  loading.value = true
  try {
    const params = {
      page: yeniSayfa,
      size: yeniBoyut,
      sort: `${siralama.value.field},${siralama.value.order === -1 ? 'desc' : 'asc'}`,
      ...tarihParametreleri()
    }
    if (arama.value.trim()) params.search = arama.value.trim()
    if (turFiltre.value) params.tur = turFiltre.value
    if (durumFiltre.value) params.durum = durumFiltre.value
    if (odemeFiltre.value) params.odemeDurumu = odemeFiltre.value
    if (vadesiGecenFiltre.value) params.vadesiGecen = true
    if (teslimatiOlanFiltre.value) params.teslimatVar = true
    await faturaStore.getAllFaturalar(params)
  } catch {
    /* toast yok */
  } finally {
    loading.value = false
  }
}

const sirala = (e) => {
  siralama.value = {
    field: e?.sortField || 'tarih',
    order: e?.sortOrder === 1 ? 1 : -1
  }
  sayfa.value = 0
  loadFaturalar(0, sayfaBoyutu.value)
}

const sayfaDegisti = (e) => {
  sayfa.value = e.page
  sayfaBoyutu.value = e.rows
  loadFaturalar(e.page, e.rows)
}

const aramaDebounce = () => {
  if (aramaZamanlayici) clearTimeout(aramaZamanlayici)
  aramaZamanlayici = setTimeout(() => {
    sayfa.value = 0
    loadFaturalar(0, sayfaBoyutu.value)
    ozetYukle()
  }, 300)
}

// --- Liste filtre cubugu (tur / durum / gelismis) ---
const turFiltreSecenekleri = computed(() => [
  { label: t('faturalar.filtreTumu'), value: null },
  { label: t('faturalar.satis'), value: 'SATIS' },
  { label: t('faturalar.alis'), value: 'ALIS' }
])
const durumFiltreSecenekleri = computed(() => [
  { label: t('faturalar.filtreTumu'), value: null },
  { label: t('faturalar.durumTaslak'), value: 'TASLAK' },
  { label: t('faturalar.durumKesildi'), value: 'KESILDI' },
  { label: t('faturalar.durumIptal'), value: 'IPTAL' }
])
const odemeFiltreSecenekleri = computed(() => [
  { label: t('faturaDetay.odenmedi'), value: 'ODENMEDI' },
  { label: t('faturaDetay.kismiOdedi'), value: 'KISMI_ODENDI' },
  { label: t('faturaDetay.odendi'), value: 'ODENDI' }
])
const gelismisFiltreSayisi = computed(
  () => (odemeFiltre.value ? 1 : 0) + (vadesiGecenFiltre.value ? 1 : 0) + (teslimatiOlanFiltre.value ? 1 : 0)
)

// Aktif filtreler: her biri tek tek kaldirilabilen cipler olarak gosterilir.
const aktifFiltreler = computed(() => {
  const liste = []
  const aralik = tarihAraligi.value
  if (aralik && aralik.length === 2 && aralik[0] && aralik[1]) {
    liste.push({ tip: 'tarih', etiket: `${formatDate(aralik[0])} – ${formatDate(aralik[1])}` })
  }
  if (arama.value.trim()) {
    liste.push({ tip: 'arama', etiket: t('faturalar.filtreArama', { q: arama.value.trim() }) })
  }
  if (turFiltre.value) {
    const o = turFiltreSecenekleri.value.find((s) => s.value === turFiltre.value)
    liste.push({ tip: 'tur', etiket: o?.label || turFiltre.value })
  }
  if (durumFiltre.value) {
    const o = durumFiltreSecenekleri.value.find((s) => s.value === durumFiltre.value)
    liste.push({ tip: 'durum', etiket: o?.label || durumFiltre.value })
  }
  if (odemeFiltre.value) {
    const o = odemeFiltreSecenekleri.value.find((s) => s.value === odemeFiltre.value)
    liste.push({ tip: 'odeme', etiket: o?.label || odemeFiltre.value })
  }
  if (vadesiGecenFiltre.value) liste.push({ tip: 'vadesiGecen', etiket: t('faturalar.vadesiGecen') })
  if (teslimatiOlanFiltre.value) liste.push({ tip: 'teslimat', etiket: t('faturalar.teslimatiOlan') })
  return liste
})

const filtreCipKaldir = (tip) => {
  if (tip === 'tarih') {
    tarihAraligi.value = null
    return
  }
  if (tip === 'arama') {
    arama.value = ''
    sayfa.value = 0
    loadFaturalar(0, sayfaBoyutu.value)
    ozetYukle()
    return
  }
  // Diger filtreler watch uzerinden listeyi + KPI'yi yeniler.
  if (tip === 'tur') turFiltre.value = null
  else if (tip === 'durum') durumFiltre.value = null
  else if (tip === 'odeme') odemeFiltre.value = null
  else if (tip === 'vadesiGecen') vadesiGecenFiltre.value = false
  else if (tip === 'teslimat') teslimatiOlanFiltre.value = false
}
const filtreTemizle = () => {
  arama.value = ''
  tarihAraligi.value = null
  turFiltre.value = null
  durumFiltre.value = null
  odemeFiltre.value = null
  vadesiGecenFiltre.value = false
  teslimatiOlanFiltre.value = false
  sayfa.value = 0
  loadFaturalar(0, sayfaBoyutu.value)
  ozetYukle()
}

watch([turFiltre, durumFiltre, odemeFiltre, vadesiGecenFiltre, teslimatiOlanFiltre], () => {
  sayfa.value = 0
  loadFaturalar(0, sayfaBoyutu.value)
  ozetYukle()
})

onUnmounted(() => {
  if (aramaZamanlayici) clearTimeout(aramaZamanlayici)
})

watch(tarihAraligi, () => {
  sayfa.value = 0
  loadFaturalar(0, sayfaBoyutu.value)
  ozetYukle()
})

onMounted(async () => {
  loading.value = true
  try {
    await Promise.all([
      loadFaturalar(0, sayfaBoyutu.value),
      ozetYukle()
    ])
  } catch (err) {
    toastBildirim.hata(t('faturalar.hataYukleme'))
  } finally {
    loading.value = false
    ilkYukleme.value = false
  }
  // Cari/kokpit kisayollari: form ayri rotada acilir.
  if (route.query.cariId) {
    router.replace({ name: 'FaturaYeni', query: { cariId: route.query.cariId } })
  } else if (route?.query?.yeni === '1') {
    router.replace({ name: 'FaturaYeni' })
  }
  // Fatura numarasından derin bağlantı: /faturalar?no=FTR-... -> ilgili fatura detayına git
  if (route.query.no) {
    try {
      const r = await faturaAPI.getByNumara(String(route.query.no))
      if (r.data?.id) router.push(`/faturalar/${r.data.id}`)
    } catch {
      toastBildirim.hata(t('faturalar.numaraBulunamadi', { no: route.query.no }))
    }
  }
})

const whatsappGonder = async (fatura) => {
  // WhatsApp dosya ekleyemediği için PDF'i indirip mesajla birlikte elle eklenebilir hale getir.
  try {
    await pdfIndir(fatura)
  } catch {
    /* indirme başarısız olsa da WhatsApp açılır */
  }
  const cariAd = fatura.cariHesapAd || t('faturalar.musterimiz')
  const tutar = fatura.genelToplam
    ? fatura.genelToplam.toLocaleString('tr-TR', { minimumFractionDigits: 2 }) + ' TL'
    : ''
  const mesaj = t('faturalar.whatsappMesaj', { ad: cariAd, no: fatura.faturaNumarasi || 'Fatura', tutar })
  const url = `https://api.whatsapp.com/send?text=${encodeURIComponent(mesaj)}`
  window.open(url, '_blank')
}

// Faturayı PNG görsel olarak müşteriye gönderir (yarı-otomatik):
// görseli panoya kopyalar + indirir, ardından WhatsApp sohbetini mesajla açar.
const faturaGorselGonder = async (fatura) => {
  try {
    const r = await pdfAPI.faturaGorsel(fatura.id)
    const blob = new Blob([r.data], { type: 'image/png' })
    let kopyalandi = false
    try {
      if (navigator.clipboard && typeof window.ClipboardItem !== 'undefined') {
        await navigator.clipboard.write([new window.ClipboardItem({ 'image/png': blob })])
        kopyalandi = true
      }
    } catch {
      /* pano izni yoksa indirme yeterli */
    }
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `fatura_${fatura.faturaNumarasi || fatura.id}.png`
    document.body.appendChild(a)
    a.click()
    a.remove()
    setTimeout(() => window.URL.revokeObjectURL(url), 30000)

    const cariAd = fatura.cariHesapAd || t('faturalar.musterimiz')
    const tutar = fatura.genelToplam
      ? fatura.genelToplam.toLocaleString('tr-TR', { minimumFractionDigits: 2 }) + ' TL'
      : ''
    const mesaj = t('faturalar.gorselMesaj', { ad: cariAd, no: fatura.faturaNumarasi || 'Fatura', tutar })
    const tel = (fatura.cariTelefon || '').replace(/\D/g, '')
    const link = tel
      ? `https://wa.me/${tel}?text=${encodeURIComponent(mesaj)}`
      : `https://api.whatsapp.com/send?text=${encodeURIComponent(mesaj)}`
    window.open(link, '_blank')

    toastBildirim.basarili(kopyalandi ? t('faturalar.gorselKopyalandi') : t('faturalar.gorselIndirildi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('faturalar.gorselHata'))
  }
}

const onizle = async (fatura) => {
  try {
    const res = await pdfAPI.faturaOnizleme(fatura.id)
    const url = window.URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }))
    window.open(url, '_blank')
    setTimeout(() => window.URL.revokeObjectURL(url), 60000)
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('faturalar.pdfIndirilemedi'))
  }
}

const openCreateDialog = () => {
  router.push({ name: 'FaturaYeni' })
}

const editFatura = (fatura) => {
  router.push({ name: 'FaturaDuzenle', params: { id: fatura.id } })
}

const cogalt = (fatura) => {
  router.push({ name: 'FaturaYeni', query: { kopya: fatura.id } })
  toastBildirim.basarili(t('faturalar.faturaCogaltildi'))
}


const viewFatura = (id) => {
  router.push(`/faturalar/${id}`)
}
const pdfIndir = async (fatura) => {
  try {
    const res = await pdfAPI.fatura(fatura.id)
    const url = window.URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `fatura_${fatura.faturaNumarasi || fatura.id}.pdf`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    setTimeout(() => window.URL.revokeObjectURL(url), 60000)
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('faturalar.pdfIndirilemedi'))
  }
}

const confirmKes = (id) => {
  confirm.require({
    message: t('faturalar.kesOnayMesaj'),
    header: t('faturalar.kesOnayBaslik'),
    icon: 'pi pi-check-circle',
    accept: async () => {
      try {
        await faturaStore.updateDurum(id, 'KESILDI')
        toastBildirim.basarili(t('faturalar.faturaKesildi'))
      } catch {
        toastBildirim.hata(t('faturalar.islemBasarisiz'))
      }
    }
  })
}

const confirmIptal = (id) => {
  confirm.require({
    message: t('faturalar.iptalOnayMesaj'),
    header: t('faturalar.iptalOnayBaslik'),
    icon: 'pi pi-exclamation-triangle',
    accept: async () => {
      try {
        await faturaStore.updateDurum(id, 'IPTAL')
        toastBildirim.basarili(t('faturalar.faturaIptalEdildi'))
      } catch (err) {
        // Ödeme blokajı gibi durumlarda sunucu mesajı yol gösterir; ham mesajı göster.
        toastBildirim.hata(err?.response?.data?.message || t('faturalar.islemBasarisiz'))
      }
    }
  })
}

const confirmIptaliGeriAl = (id) => {
  confirm.require({
    message: t('faturalar.iptaliGeriAlOnayMesaj'),
    header: t('faturalar.iptaliGeriAl'),
    icon: 'pi pi-undo',
    accept: async () => {
      try {
        await faturaAPI.iptaliGeriAl(id)
        await faturaStore.getAllFaturalar()
        toastBildirim.basarili(t('faturalar.iptaliGeriAlindi'))
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('faturalar.islemBasarisiz'))
      }
    }
  })
}

const yazdirmaBaslik = (f) => {
  const tarih = f.sonYazdirmaTarihi ? new Date(f.sonYazdirmaTarihi).toLocaleString('tr-TR') : ''
  return [f.sonYazdirmaFormat, tarih, f.sonYazdirmaYazici].filter(Boolean).join(' • ')
}

const soforDialogRef = ref(null)

/** Acik satis faturasi: kalan tutar varsa tahsilat aksiyonu sunulur. */
const tahsilatYapilabilir = (f) =>
  f?.tur === 'SATIS' && f?.durum === 'KESILDI' && Number(f?.kalanTutar || 0) > 0

const tahsilataGit = (f) => router.push({ name: 'Tahsilat', query: { cariId: f.cariHesapId } })

const faturaEylemleri = (f) => {
  const gruplar = []

  gruplar.push({
    grup: t('faturalar.grupYazdirma'),
    items: [
      { etiket: t('faturalar.yazdirTasarla'), ikon: 'pi pi-print', islem: () => tasarlaVeYazdir(f.id) },
      { etiket: t('faturalar.pdfIndir'), ikon: 'pi pi-download', islem: () => pdfIndir(f) },
      { etiket: t('faturalar.onizle'), ikon: 'pi pi-eye', islem: () => onizle(f) }
    ]
  })

  gruplar.push({
    grup: t('faturalar.grupGonderim'),
    items: [
      { etiket: t('faturalar.whatsapp'), ikon: 'pi pi-whatsapp', islem: () => whatsappGonder(f) },
      { etiket: t('faturalar.gorselGonder'), ikon: 'pi pi-image', islem: () => faturaGorselGonder(f) }
    ]
  })

  const kayit = [
    { etiket: t('common.duplicate'), ikon: 'pi pi-copy', islem: () => cogalt(f) },
    { etiket: t('faturaGecmis.title'), ikon: 'pi pi-history', islem: () => gecmisAc(f) }
  ]
  if (f.durum === 'TASLAK' || f.durum === 'KESILDI') {
    kayit.push({
      etiket: f.durum === 'KESILDI' ? t('faturalar.revize') : t('common.edit'),
      ikon: 'pi pi-pencil',
      islem: () => editFatura(f)
    })
  }
  if (f.durum === 'TASLAK') {
    kayit.push({ etiket: t('faturalar.kes'), ikon: 'pi pi-check', sinif: 'eylem-pozitif', islem: () => confirmKes(f.id) })
  }
  if (f.durum !== 'IPTAL') {
    kayit.push({ etiket: t('common.cancel'), ikon: 'pi pi-ban', sinif: 'eylem-sil', islem: () => confirmIptal(f.id) })
  }
  if (f.durum === 'IPTAL' && authStore.isAdmin) {
    kayit.push({ etiket: t('faturalar.iptaliGeriAl'), ikon: 'pi pi-undo', islem: () => confirmIptaliGeriAl(f.id) })
  }
  gruplar.push({ grup: t('faturalar.grupKayit'), items: kayit })

  const lojistik = []
  if (f.tur === 'SATIS' && f.durum !== 'IPTAL' && !f.teslimatVar) {
    lojistik.push({ etiket: t('faturalar.soforAta'), ikon: 'pi pi-truck', islem: () => soforDialogRef.value?.open(f) })
  }
  if (f.teslimatVar) {
    lojistik.push({
      etiket: t('faturalar.teslimatta'),
      ikon: 'pi pi-check-circle',
      islem: () => router.push({ name: 'Teslimatlar' })
    })
  }
  if (lojistik.length) gruplar.push({ grup: t('faturalar.grupLojistik'), items: lojistik })

  if (tahsilatYapilabilir(f)) {
    gruplar.push({
      grup: t('faturalar.grupFinans'),
      items: [
        {
          etiket: t('faturalar.tahsilatAl'),
          ikon: 'pi pi-money-bill',
          sinif: 'eylem-pozitif',
          islem: () => tahsilataGit(f)
        }
      ]
    })
  }

  // Gruplari ayraclarla duzlestir.
  const items = []
  gruplar.forEach((g, i) => {
    if (i > 0) items.push({ ayrac: true })
    items.push({ grup: g.grup })
    items.push(...g.items)
  })
  return items
}

const durumLabel = (d) => {
  const lbl = {
    TASLAK: t('faturalar.durumTaslak'),
    TEKLIF: t('faturalar.durumTeklif'),
    KESILDI: t('faturalar.durumKesildi'),
    IPTAL: t('faturalar.durumIptal')
  }
  return lbl[d] || d
}

const odemeDurumEtiket = (d) => ({
  ODENDI: t('faturaDetay.odendi'),
  KISMI_ODENDI: t('faturaDetay.kismiOdedi'),
  ODENMEDI: t('faturaDetay.odenmedi')
})[d] || '-'

// Vadesi gecen kesilmis faturalarda gecikme gunu (rozet).
const gecikmisGun = (f) => {
  if (f.durum !== 'KESILDI' || !f.vadeTarihi || Number(f.kalanTutar || 0) <= 0) return 0
  const vade = new Date(f.vadeTarihi)
  if (isNaN(vade.getTime())) return 0
  const bugun = new Date()
  vade.setHours(0, 0, 0, 0)
  bugun.setHours(0, 0, 0, 0)
  const gun = Math.floor((bugun - vade) / 86400000)
  return gun > 0 ? gun : 0
}

const TESLIMAT_DURUM_SEVERITY = { BEKLEMEDE: 'warn', YOLDA: 'info', TESLIM_EDILDI: 'success', IPTAL: 'danger' }
const teslimatDurumEtiket = (d) =>
  ({
    BEKLEMEDE: t('teslimatlar.durumBeklemede'),
    YOLDA: t('teslimatlar.durumYolda'),
    TESLIM_EDILDI: t('teslimatlar.durumTeslimEdildi'),
    IPTAL: t('teslimatlar.durumIptal')
  })[d] || d

const excelIndir = async () => {
  try {
    const res = await excelAPI.faturalar()
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', 'Faturalar.xlsx')
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch {
    /* silent */
  }
}

const topluSil = () => {
  if (!selectedItems.value.length) return
  confirm.require({
    message: t('common.topluSilmeOnayMesaji', { n: selectedItems.value.length }),
    header: t('common.topluSilmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    rejectProps: { label: t('common.vazgec'), severity: 'secondary', outlined: true, size: 'small' },
    acceptProps: { label: t('common.evetSil'), severity: 'danger', size: 'small' },
    accept: async () => {
      topluSiliniyor.value = true
      try {
        for (const item of selectedItems.value) {
          await faturaAPI.delete(item.id)
        }
        toastBildirim.basarili(t('faturalar.kayitSilindi', { n: selectedItems.value.length }))
        selectedItems.value = []
        await loadFaturalar()
      } catch {
        toastBildirim.hata(t('faturalar.silmeBasarisiz'))
      } finally {
        topluSiliniyor.value = false
      }
    }
  })
}


import { formatTarih as formatDate } from '../utils/format.js'
</script>

<style scoped>
.fatura-kpi {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.fatura-kart-liste {
  display: none;
}
.fatura-kart {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
  padding: 14px;
  margin-bottom: 10px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
}
.fatura-kart-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
}
.fatura-kart-baslik {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.fatura-kart-baslik strong {
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.fatura-kart-tutar {
  font-weight: 800;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
.fatura-kart-alt {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 13px;
  color: var(--text-secondary);
  min-width: 0;
}
.fatura-kart-alt .fatura-kart-cari {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}
.fatura-kart-alt i {
  color: var(--text-muted);
  margin-right: 2px;
}
.fatura-kart-rozetler {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.fatura-kart-islemler {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-top: 10px;
  border-top: 1px solid var(--border);
}
.fatura-kart-eylem-grup {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 2px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: var(--bg-muted);
}
.fatura-kart-eylem-grup :deep(.p-button) {
  width: 32px;
  height: 32px;
  padding: 0;
}
.fatura-kart-eylem-grup :deep(.p-button .pi) {
  font-size: 14px;
}
@media (max-width: 768px) {
  .table-container {
    display: none;
  }
  .fatura-kart-liste {
    display: block;
  }
}
.faturalar-container {
  padding: 0;
  max-width: 100%;
}
h1 {
  color: var(--text-primary);
  margin-bottom: 20px;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.5px;
}
.toolbar {
  margin-bottom: 20px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px 18px;
}
.arama-kutu {
  margin-left: 12px;
}
.arama-input {
  width: 260px;
}
.table-container {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px;
}
/* Yatay kaydirmada secim, fatura no ve islem kolonlari gorunur kalsin. */
.table-container :deep(.fatura-secim-kolon),
.table-container :deep(.fatura-no-kolon),
.table-container :deep(.fatura-islem-kolon) {
  position: sticky;
  z-index: 2;
  background: var(--bg-card);
}
.table-container :deep(.fatura-secim-kolon) {
  left: 0;
}
.table-container :deep(.fatura-no-kolon) {
  left: 3rem;
  box-shadow: 8px 0 12px -12px rgba(0, 0, 0, 0.8);
}
.table-container :deep(.fatura-islem-kolon) {
  right: 0;
  box-shadow: -8px 0 12px -12px rgba(0, 0, 0, 0.8);
}
.table-container :deep(thead .fatura-secim-kolon),
.table-container :deep(thead .fatura-no-kolon),
.table-container :deep(thead .fatura-islem-kolon) {
  z-index: 3;
}
/* Islem kolonu: uc aksiyon ayni boyutta, saga hizali ve tek satirda. */
.table-container :deep(th.fatura-islem-kolon),
.table-container :deep(td.fatura-islem-kolon) {
  width: 110px;
  min-width: 110px;
}
.fatura-satir-eylemler {
  justify-content: flex-end;
  gap: 2px;
  min-width: 96px;
  padding: 2px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: var(--bg-muted);
}
.fatura-satir-eylemler :deep(.p-button) {
  width: 28px;
  height: 28px;
  padding: 0;
}
.fatura-satir-eylemler :deep(.p-button .pi) {
  font-size: 13px;
}
/* Tutar hucreleri tek satirda kalsin (sayisal sinif sag hizalar). */
.table-container :deep(td.sayisal),
.table-container :deep(th.sayisal) {
  white-space: nowrap;
}
.satir-islemler {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: nowrap;
}
.loading {
  text-align: center;
  padding: 40px;
  color: var(--text-muted);
}
.badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}
.badge.satis {
  background: var(--accent-soft-strong);
  color: var(--accent);
}
.badge.alis {
  background: var(--danger-soft);
  color: var(--danger);
}
.durum-hucre {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.durum-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}
.durum-badge.taslak {
  background: var(--warning-soft);
  color: var(--warning);
}
.durum-badge.teklif {
  background: var(--info-soft);
  color: var(--info);
}
.durum-badge.kesildi {
  background: var(--success-soft);
  color: var(--success);
}
.durum-badge.iptal {
  background: var(--bg-muted);
  color: var(--text-muted);
}
.odeme-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}
.odeme-badge.odendi {
  background: var(--success-soft);
  color: var(--success);
}
.odeme-badge.kismi_odendi {
  background: var(--warning-soft);
  color: var(--warning);
}
.odeme-badge.odenmedi {
  background: var(--danger-soft);
  color: var(--danger);
}
.kalan-tutar {
  font-size: 12px;
  font-weight: 700;
  color: var(--danger);
}
.islem-yapan {
  font-size: 12px;
  color: var(--text-secondary);
}
.islem-yapan-bos {
  font-size: 12px;
  color: var(--text-muted);
}
.yazdirma-rozet {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
  background: rgba(139, 92, 246, 0.15);
  color: #8b5cf6;
  cursor: default;
}
.yazdirma-rozet small {
  font-weight: 500;
  opacity: 0.8;
}
.teslim-eden-list {
  font-size: 12px;
  color: var(--text-secondary);
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.teslim-eden-list i {
  color: var(--text-muted);
  font-size: 12px;
}
.w-full {
  width: 100% !important;
}
/* Liste filtre cubugu: tek kart icinde gruplanmis, esit yukseklikte kontroller. */
.filtre-cubugu {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin: 0 0 12px;
  padding: 7px 10px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
}
.arama-kutu {
  flex: 1 1 220px;
  max-width: 340px;
}
.arama-input {
  width: 100%;
}
.filtre-secim {
  min-width: 128px;
}
.filtre-secim :deep(.p-select) {
  width: 100%;
}
.filtre-ayrac {
  width: 1px;
  height: 22px;
  background: var(--border);
}
/* Kolon ayarlari dugmesi filtre cubugunun sagina yaslanir. */
.filtre-sag {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 6px;
}
/* Aktif filtre cipleri: yalnizca filtre seciliyken gorunur. */
.fatura-aktif-filtreler {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin: -4px 0 14px;
}
.fatura-aktif-etiket {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: var(--text-muted);
}
.fatura-filtre-cip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid var(--border);
  background: var(--bg-muted);
  color: var(--text-secondary);
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}
.fatura-filtre-cip:hover {
  border-color: var(--accent);
  color: var(--accent);
}
.fatura-filtre-cip i {
  font-size: 10px;
}
.fatura-filtre-temizle {
  margin-left: 2px;
}
.tur-filtre-secim {
  flex-shrink: 0;
}
.durum-cipleri {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}
.durum-cip {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  border: 1px solid var(--border);
  background: var(--bg-card);
  color: var(--text-secondary);
  border-radius: 999px;
  padding: 0 12px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}
.durum-cip.aktif {
  background: var(--accent-soft);
  border-color: var(--accent-border);
  color: var(--accent);
}
.filtre-gelismis-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 32px;
  border: 1px solid var(--border);
  background: var(--bg-card);
  color: var(--text-secondary);
  border-radius: 10px;
  padding: 0 12px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}
.filtre-gelismis-btn.aktif {
  border-color: var(--accent-border);
  color: var(--accent);
  background: var(--accent-soft);
}
.filtre-gelismis-btn .filtre-rozet {
  min-width: 17px;
  height: 17px;
  padding: 0 5px;
  border-radius: 999px;
  background: var(--accent);
  color: var(--accent-contrast, #04211d);
  font-size: 11px;
  font-weight: 800;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
@media (max-width: 1280px) {
  .filtre-gelismis-metin {
    display: none;
  }
  .filtre-gelismis-btn {
    width: 34px;
    justify-content: center;
    padding: 0;
  }
}

@media (max-width: 768px) {
  .filtre-cubugu {
    gap: 6px;
  }
  .arama-kutu {
    flex-basis: 100%;
    max-width: none;
  }
  .filtre-ayrac {
    display: none;
  }
  .filtre-sag {
    margin-left: 0;
  }
}

.filtre-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 250px;
}.filtre-panel-baslik {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
}
.filtre-alan label {
  display: block;
  font-size: 11px;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 4px;
}
.filtre-switch {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
}
.gecikme-rozet {
  margin-left: 0;
  white-space: nowrap;
}
</style>
