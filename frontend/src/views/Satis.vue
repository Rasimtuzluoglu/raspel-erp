<template>
  <div class="satis-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        {{ t('satis.title') }}
      </h1>
      <Button
        :label="t('satis.yeniSatis')"
        icon="pi pi-plus"
        class="p-button-success"
        @click="openSatis"
      />
    </div>

    <div class="kpi-serit">
      <KpiKart
        :baslik="t('satis.kpiAdet')"
        :deger="ozet.adet"
        :para-birimi="false"
        icon="pi pi-receipt"
        renk="#14b8a6"
      />
      <KpiKart
        :baslik="t('satis.kpiCiro')"
        :deger="ozet.ciro"
        icon="pi pi-chart-line"
        renk="#3b82f6"
      />
      <KpiKart
        :baslik="t('satis.kpiTahsil')"
        :deger="ozet.tahsilEdilen"
        icon="pi pi-wallet"
        renk="#10b981"
      />
      <KpiKart
        :baslik="t('satis.kpiKalan')"
        :deger="ozet.kalan"
        icon="pi pi-exclamation-circle"
        renk="#ef4444"
      />
    </div>

    <div class="filtre-cubugu">
      <span class="p-input-icon-left arama-kutu">
        <i class="pi pi-search" />
        <InputText
          v-model="filtre"
          :placeholder="t('satis.aramaPlaceholder')"
        />
      </span>
      <TarihHizliSecim v-model="tarihAraligi" />
      <div class="durum-cipleri">
        <button
          type="button"
          class="durum-cip"
          :class="{ aktif: !seciliDurum }"
          @click="seciliDurum = ''"
        >
          {{ t('satis.tumu') }}
        </button>
        <button
          v-for="d in durumCipleri"
          :key="d"
          type="button"
          class="durum-cip"
          :class="[{ aktif: seciliDurum === d }, (d || '').toLowerCase()]"
          @click="seciliDurum = d"
        >
          {{ durumLabelUtil(d) }}
        </button>
      </div>
      <label class="vade-toggle">
        <Checkbox
          v-model="sadeceVadesiGecen"
          binary
        />
        <span>{{ t('satis.vadesiGecenler') }}</span>
      </label>
    </div>

    <div class="table-container">
      <DataTable
        :value="satislar"
        :lazy="true"
        :paginator="true"
        :rows="sayfaBoyutu"
        :first="sayfa * sayfaBoyutu"
        :total-records="toplamKayit"
        :loading="loading"
        paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport"
        :current-page-report-template="'{totalRecords} ' + $t('common.recordsWord') + ' · {first}-{last}'"
        striped-rows
        class="satis-tablo"
        @page="sayfaDegisti"
        @row-click="(e) => detayAc(e.data)"
      >
        <template #empty>
          <EmptyState />
        </template>
        <Column
          field="faturaNumarasi"
          :header="t('satis.colFaturaNo')"
          style="width: 150px"
        >
          <template #body="s">
            <span class="fatura-no-link">{{ s.data.faturaNumarasi }}</span>
          </template>
        </Column>
        <Column
          field="tarih"
          :header="t('common.date')"
          style="width: 105px"
        >
          <template #body="s">
            {{ formatDate(s.data.tarih) }}
          </template>
        </Column>
        <Column
          field="cariHesapAd"
          :header="t('satis.colMusteri')"
          style="width: 200px"
        >
          <template #body="s">
            <div class="musteri-hucre">
              <span class="musteri-avatar">{{ musteriBasHarfi(s.data.cariHesapAd) }}</span>
              <span class="musteri-ad">{{ s.data.cariHesapAd || t('satis.perakendeMusteri') }}</span>
            </div>
          </template>
        </Column>
        <Column
          field="kdv"
          :header="t('satis.colKdv')"
          style="width: 110px"
          body-class="tutar-hucre"
        >
          <template #body="s">
            {{ formatPara(s.data.kdv, s.data.paraBirimi) }}
          </template>
        </Column>
        <Column
          field="genelToplam"
          :header="t('common.amount')"
          style="width: 130px"
          body-class="tutar-hucre"
        >
          <template #body="s">
            <span class="genel-toplam-hucre">{{ formatPara(s.data.genelToplam, s.data.paraBirimi) }}</span>
          </template>
        </Column>
        <Column
          field="kalanTutar"
          :header="t('satis.colKalan')"
          style="width: 120px"
          body-class="tutar-hucre"
        >
          <template #body="s">
            <span :class="{ 'kalan-var': Number(s.data.kalanTutar || 0) > 0 }">
              {{ formatPara(s.data.kalanTutar, s.data.paraBirimi) }}
            </span>
          </template>
        </Column>
        <Column
          field="odemeDurumu"
          :header="t('satis.colOdeme')"
          style="width: 120px"
        >
          <template #body="s">
            <Tag
              v-if="s.data.odemeDurumu"
              :value="odemeDurumEtiketi(s.data.odemeDurumu)"
              :severity="odemeDurumSeverity(s.data.odemeDurumu)"
            />
          </template>
        </Column>
        <Column
          field="durum"
          :header="t('common.status')"
          style="width: 150px"
        >
          <template #body="s">
            <span :class="['durum-badge', (s.data.durum || '').toLowerCase()]">{{ durumLabel(s.data.durum) }}</span>
            <Tag
              v-if="gecikmisGun(s.data) > 0"
              class="gecikme-rozet"
              :value="t('satis.gunGecikti', { n: gecikmisGun(s.data) })"
              severity="danger"
            />
          </template>
        </Column>
        <Column
          :header="t('satis.colTeslimat')"
          style="width: 130px"
        >
          <template #body="s">
            <Tag
              v-if="s.data.teslimatVar"
              :value="teslimatDurumEtiketi(s.data.teslimatDurum)"
              :severity="TESLIMAT_DURUM_SEVERITY[s.data.teslimatDurum] || 'secondary'"
              :title="s.data.driverAd || ''"
              style="cursor: pointer"
              @click="router.push({ name: 'Teslimatlar' })"
            />
            <span
              v-else
              class="teslimat-yok"
            >—</span>
          </template>
        </Column>
        <Column
          :header="t('common.actions')"
          style="width: 120px"
        >
          <template #body="s">
            <SatirEylemleri
              :gorunur="{ duzenle: false, cogalt: false, sil: false }"
              :items="satisEylemleri(s.data)"
            />
          </template>
        </Column>
      </DataTable>
    </div>

    <Drawer
      v-model:visible="detayGorunur"
      position="right"
      :header="detayFatura?.faturaNumarasi || t('satis.detayBaslik')"
      class="satis-detay-drawer"
      :style="{ width: 'min(440px, 100vw)' }"
    >
      <div
        v-if="detayYukleniyor"
        class="detay-yukleniyor"
      >
        <i class="pi pi-spin pi-spinner" /> {{ t('common.loading') }}
      </div>
      <div
        v-else-if="detayFatura"
        class="detay-icerik"
      >
        <div class="detay-ust">
          <span :class="['durum-badge', (detayFatura.durum || '').toLowerCase()]">{{ durumLabel(detayFatura.durum) }}</span>
          <Tag
            v-if="detayFatura.odemeDurumu"
            :value="odemeDurumEtiketi(detayFatura.odemeDurumu)"
            :severity="odemeDurumSeverity(detayFatura.odemeDurumu)"
          />
        </div>
        <div class="detay-satir">
          <span>{{ t('satis.colMusteri') }}</span>
          <strong>{{ detayFatura.cariHesapAd || t('satis.perakendeMusteri') }}</strong>
        </div>
        <div class="detay-satir">
          <span>{{ t('common.date') }}</span>
          <strong>{{ formatDate(detayFatura.tarih) }}</strong>
        </div>
        <div
          v-if="detayFatura.vadeTarihi"
          class="detay-satir"
        >
          <span>{{ t('satis.vade') }}</span>
          <strong>{{ formatDate(detayFatura.vadeTarihi) }}</strong>
        </div>

        <h4 class="detay-baslik">
          {{ t('satis.satisKalemleri') }}
        </h4>
        <div class="detay-kalemler">
          <div
            v-for="(k, i) in detayFatura.kalemler || []"
            :key="i"
            class="detay-kalem"
          >
            <span class="detay-kalem-ad">{{ k.aciklama || k.ad }}</span>
            <span class="detay-kalem-adet">{{ k.adet }} × {{ formatCurrency(k.birimFiyat) }}</span>
            <span class="detay-kalem-tutar">{{ formatCurrency(k.tutar || (k.adet * k.birimFiyat)) }}</span>
          </div>
        </div>

        <div class="detay-toplamlar">
          <div class="detay-satir">
            <span>{{ t('faturaKalemleri.araToplam') }}</span>
            <strong>{{ formatPara(detayFatura.araToplam, detayFatura.paraBirimi) }}</strong>
          </div>
          <div class="detay-satir">
            <span>{{ t('satis.colKdv') }}</span>
            <strong>{{ formatPara(detayFatura.kdv, detayFatura.paraBirimi) }}</strong>
          </div>
          <div class="detay-satir detay-genel">
            <span>{{ t('common.amount') }}</span>
            <strong>{{ formatPara(detayFatura.genelToplam, detayFatura.paraBirimi) }}</strong>
          </div>
          <div
            v-if="Number(detayFatura.odenenTutar || 0) > 0"
            class="detay-satir"
          >
            <span>{{ t('hizliSatis.odenenTutar') }}</span>
            <strong>{{ formatPara(detayFatura.odenenTutar, detayFatura.paraBirimi) }}</strong>
          </div>
          <div
            v-if="Number(detayFatura.kalanTutar || 0) > 0"
            class="detay-satir kalan-var"
          >
            <span>{{ t('satis.colKalan') }}</span>
            <strong>{{ formatPara(detayFatura.kalanTutar, detayFatura.paraBirimi) }}</strong>
          </div>
        </div>

        <div class="detay-aksiyonlar">
          <Button
            v-if="detayFatura.durum !== 'IPTAL' && Number(detayFatura.odenenTutar || 0) === 0"
            :label="t('satis.duzenle')"
            icon="pi pi-pencil"
            class="p-button-outlined"
            @click="detayDuzenle"
          />
          <Button
            :label="t('satis.goruntule')"
            icon="pi pi-eye"
            class="p-button-outlined"
            @click="router.push(`/faturalar/${detayFatura.id}`)"
          />
          <Button
            v-if="detayFatura.durum === 'KESILDI' && Number(detayFatura.kalanTutar || 0) > 0"
            :label="t('satis.tahsilatAl')"
            icon="pi pi-money-bill"
            class="p-button-success"
            @click="tahsilataGit(detayFatura)"
          />
          <Button
            :label="t('satis.a4Yazdir')"
            icon="pi pi-print"
            class="p-button-text"
            @click="printFatura(detayFatura.id)"
          />
        </div>
      </div>
    </Drawer>

    <AppDialog
      v-model:visible="showSatisDialog"
      :header="dialogBaslik"
      :closable="false"
      width="920px"
    >
      <div
        v-if="!duzenlenenId"
        class="satis-modu"
      >
        <label class="bolum-etiket">{{ t('satis.islemModu') }}</label>
        <div class="modu-radio-group">
          <div
            :class="['modu-option', { active: satisModu === 'SATIS' }]"
            @click="satisModu = 'SATIS'"
          >
            <i class="pi pi-shopping-cart" /> {{ t('satis.satisYap') }}
          </div>
          <div
            :class="['modu-option', { active: satisModu === 'TEKLIF' }]"
            @click="satisModu = 'TEKLIF'"
          >
            <i class="pi pi-file" /> {{ t('satis.teklifOlustur') }}
          </div>
        </div>
      </div>

      <div class="form-grid-2">
        <div class="form-group">
          <label>{{ t('satis.musteri') }} <span v-if="satisModu === 'SATIS'">*</span></label>
          <AutoComplete
            v-model="musteriSecim"
            :suggestions="musteriOnerileri"
            option-label="ad"
            :placeholder="t('satis.musteriSeciniz')"
            :empty-search-message="t('satis.musteriBulunamadi')"
            class="w-full"
            dropdown
            force-selection
            @complete="musteriAra"
            @option-select="musteriSecildi"
          >
            <template #option="slotProps">
              <div class="musteri-opsiyon">
                <span class="musteri-opsiyon-ad">{{ slotProps.option.ad }}</span>
                <span class="musteri-opsiyon-detay">{{ slotProps.option.vergiNo || slotProps.option.telefon || '' }}</span>
              </div>
            </template>
          </AutoComplete>
        </div>
        <div class="form-group">
          <label>{{ t('satis.tarihZorunlu') }}</label>
          <DatePicker
            v-model="satisForm.tarih"
            date-format="dd.mm.yy"
            class="w-full"
          />
        </div>
      </div>

      <div class="kalem-bolum">
        <h3 class="bolum-baslik">
          {{ t('satis.satisKalemleri') }}
        </h3>
        <div class="kalem-tablo">
          <FaturaKalemleri
            :kalemler="satisForm.kalemler"
            :ara-toplam="araToplam"
            :kdv-toplam="kdvToplam"
            :genel-toplam="genelToplam"
            :kdv-secenekleri="kdvOranlari"
            :kdv-varsayilan="0"
            stok-arama
            @add="kalemEkle"
            @remove="(i) => satisForm.kalemler.splice(i, 1)"
            @stok-sec="stokSatirSecildi"
          />
        </div>
      </div>

      <div class="form-group">
        <label>{{ t('common.description') }}</label>
        <Textarea
          v-model="satisForm.aciklama"
          rows="2"
          class="w-full"
        />
      </div>

      <!-- Opsiyonel: Tahsilat (yoksa acik/vadeli hesap) -->
      <details
        v-if="satisModu === 'SATIS'"
        class="opsiyon-bolum"
      >
        <summary><i class="pi pi-wallet" /> {{ t('satis.tahsilatOpsiyon') }}</summary>
        <div class="form-grid-2">
          <div class="form-group">
            <label>{{ t('hizliSatis.odeme') }}</label>
            <SelectButton
              v-model="odemeDurumu"
              :options="odemeDurumSecenekleri"
              option-label="label"
              option-value="value"
              :allow-empty="false"
              class="w-full"
            />
          </div>
          <div class="form-group">
            <label>{{ t('hizliSatis.odenenTutar') }}</label>
            <InputNumber
              v-model="odenenTutar"
              :min="0"
              :max="genelToplam"
              mode="currency"
              currency="TRY"
              locale="tr-TR"
              class="w-full"
            />
          </div>
        </div>
        <div
          v-if="odemeDurumu !== 'yok'"
          class="form-grid-2"
        >
          <div class="form-group">
            <label>{{ t('hizliSatis.odemeYontemi') }}</label>
            <Dropdown
              v-model="odemeYontemi"
              :options="odemeYontemListesi"
              option-label="label"
              option-value="value"
              class="w-full"
            />
          </div>
          <div class="form-group">
            <label>{{ t('hizliSatis.kasa') }}</label>
            <Dropdown
              v-model="seciliKasa"
              :options="kasalar"
              option-label="ad"
              option-value="id"
              show-clear
              class="w-full"
            />
          </div>
        </div>
        <div
          v-if="odemeDurumu !== 'yok' && (odemeYontemi === 'KART' || odemeYontemi === 'HAVALE')"
          class="form-grid-2"
        >
          <div class="form-group">
            <label>{{ t('hizliSatis.bankaSecin') }}</label>
            <Dropdown
              v-model="seciliBanka"
              :options="bankalar"
              option-label="ad"
              option-value="id"
              show-clear
              class="w-full"
            />
          </div>
          <div
            v-if="odemeYontemi === 'KART'"
            class="form-group"
          >
            <label>{{ t('hizliSatis.posTerminali') }}</label>
            <Dropdown
              v-model="seciliPos"
              :options="posTerminalleri"
              option-label="ad"
              option-value="id"
              show-clear
              class="w-full"
            />
          </div>
        </div>
      </details>

      <!-- Opsiyonel: Teslimat -->
      <details
        v-if="satisModu === 'SATIS'"
        class="opsiyon-bolum"
      >
        <summary><i class="pi pi-truck" /> {{ t('satis.teslimatOpsiyon') }}</summary>
        <div class="form-grid-2">
          <div class="form-group">
            <label>{{ t('hizliSatis.sofor') }}</label>
            <Dropdown
              v-model="seciliSofor"
              :options="soforler"
              option-label="ad"
              option-value="id"
              filter
              show-clear
              :loading="soforlerYukleniyor"
              class="w-full"
            />
          </div>
          <div class="form-group">
            <label>{{ t('hizliSatis.teslimDurumu') }}</label>
            <Dropdown
              v-model="teslimDurumu"
              :options="teslimDurumSecenekleri"
              option-label="label"
              option-value="value"
              class="w-full"
            />
          </div>
        </div>
        <div
          v-if="seciliSofor"
          class="form-group"
        >
          <label>{{ t('hizliSatis.teslimatAdresi') }} <span class="zorunlu">*</span></label>
          <InputText
            v-model="teslimatAdresi"
            class="w-full"
          />
        </div>
        <div
          v-if="seciliSofor"
          class="form-group"
        >
          <label>{{ t('hizliSatis.teslimNotu') }}</label>
          <InputText
            v-model="teslimNotu"
            class="w-full"
          />
        </div>
      </details>

      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="showSatisDialog = false"
        />
        <Button
          :label="duzenlenenId ? t('satis.guncelle') : (satisModu === 'TEKLIF' ? t('satis.teklifiKaydet') : t('satis.satisiTamamla'))"
          icon="pi pi-check"
          :loading="saving"
          :disabled="satisForm.kalemler.length === 0 || (satisModu === 'SATIS' && !satisForm.cariHesapId)"
          @click="satisiTamamla"
        />
      </template>
    </AppDialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { unwrapList } from '../api/utils/unwrap.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { faturaAPI, teklifAPI, kasaAPI, bankaAPI, posAPI, teslimatAPI, cariHesapAPI } from '../api/index.js'
import { useCariHesapStore } from '../stores/cariHesapStore.js'
import { useStokStore } from '../stores/stokStore.js'
import { useAuthStore } from '../stores/authStore.js'
import { useRouter, useRoute } from 'vue-router'
import { useMarka } from '../composables/useMarka.js'
import { escapeHtml } from '../utils/escapeHtml.js'
import { fisPenceresiAcVeYazdir } from '../utils/fisYazdir.js'
import TarihHizliSecim from '../components/TarihHizliSecim.vue'
import FaturaKalemleri from '../components/FaturaKalemleri.vue'
import KpiKart from '../components/KpiKart.vue'
import AppDialog from '../components/AppDialog.vue'
import { formatCurrency, formatPara, getLocalDateString, durumLabel as durumLabelUtil } from '../utils/format.js'
import { kalemNetTutar, kalemKdv } from '../utils/faturaHesapla.js'
import { satisPayloadUret } from '../utils/satisPayload.js'
import { useI18n } from 'vue-i18n'

const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const router = useRouter()
const route = useRoute()
const { t } = useI18n()
const cariHesapStore = useCariHesapStore()
const stokStore = useStokStore()
const authStore = useAuthStore()
const { sirketLogosu } = useMarka()

const satislar = ref([])
// Sag panel detay drawer.
const detayGorunur = ref(false)
const detayFatura = ref(null)
const detayYukleniyor = ref(false)
const showSatisDialog = ref(false)
const saving = ref(false)
const filtre = ref('')
const satisModu = ref('SATIS')
// Dolu ise diyalog mevcut bir satisi duzenler (create yerine update).
const duzenlenenId = ref(null)
const duzenlenenDurum = ref('KESILDI')
const seciliDurum = ref('')
const sadeceVadesiGecen = ref(false)
const durumCipleri = ['KESILDI', 'TASLAK', 'IPTAL']
const ozet = ref({ adet: 0, ciro: 0, tahsilEdilen: 0, kalan: 0 })
const kdvOranlari = [0, 1, 8, 10, 18, 20]

// Sunucu tarafli sayfalama (tum fatura listesini cekip istemcide filtrelemek
// ilk 50 kayitla sinirliydi; eski satislar gorunmuyordu).
const sayfa = ref(0)
const sayfaBoyutu = ref(25)
const toplamKayit = ref(0)
const loading = ref(false)
let aramaZamanlayici = null

const satisForm = ref({
  cariHesapId: null,
  tarih: new Date(),
  aciklama: '',
  kalemler: []
})

// Musteri secimi: sunucu tarafli arama (tum carileri yuklemek yerine).
const musteriSecim = ref(null)
const musteriOnerileri = ref([])
let musteriAramaZamanlayici = null
const musteriAra = (event) => {
  const q = (event?.query || '').trim()
  if (musteriAramaZamanlayici) clearTimeout(musteriAramaZamanlayici)
  musteriAramaZamanlayici = setTimeout(async () => {
    try {
      const params = { page: 0, size: 20 }
      if (q) params.search = q
      const r = await cariHesapAPI.filtreli(params)
      musteriOnerileri.value = unwrapList(r)
    } catch {
      musteriOnerileri.value = []
    }
  }, 250)
}
const musteriSecildi = (event) => {
  const c = event?.value
  if (c) satisForm.value.cariHesapId = c.id
}

// Opsiyonel tahsilat + teslimat (POS ile ayni payload alanlari).
const odemeDurumu = ref('tam')
const odemeDurumSecenekleri = computed(() => [
  { label: t('hizliSatis.odemeTipTam'), value: 'tam' },
  { label: t('hizliSatis.odemeTipYarim'), value: 'yarim' },
  { label: t('hizliSatis.odemeTipYok'), value: 'yok' }
])
const odemeYontemi = ref('NAKIT')
const odemeYontemListesi = computed(() => [
  { label: t('hizliSatis.nakit'), value: 'NAKIT' },
  { label: t('hizliSatis.kart'), value: 'KART' },
  { label: t('hizliSatis.havale'), value: 'HAVALE' }
])
const odenenTutar = ref(0)
const seciliKasa = ref(null)
const kasalar = ref([])
const seciliBanka = ref(null)
const bankalar = ref([])
const seciliPos = ref(null)
const posTerminalleri = ref([])
const seciliSofor = ref(null)
const soforler = ref([])
const soforlerYukleniyor = ref(false)
const teslimatAdresi = ref('')
const teslimDurumu = ref('BEKLIYOR')
const teslimNotu = ref('')
const teslimDurumSecenekleri = [
  { label: 'Bekliyor', value: 'BEKLIYOR' },
  { label: 'Yolda', value: 'YOLDA' },
  { label: 'Teslim Edildi', value: 'TESLIM_EDILDI' }
]

// Odeme durumu -> backend enum'u (POS ile ayni kural).
const odemeDurumEnum = computed(() => {
  if (odemeDurumu.value === 'yok' || !odenenTutar.value) return 'ODENMEDI'
  if (odenenTutar.value >= genelToplam.value) return 'ODENDI'
  return 'KISMI_ODENDI'
})

const opsiyonlariSifirla = () => {
  odemeDurumu.value = 'tam'
  odemeYontemi.value = 'NAKIT'
  odenenTutar.value = 0
  seciliKasa.value = null
  seciliBanka.value = null
  seciliPos.value = null
  seciliSofor.value = null
  teslimatAdresi.value = ''
  teslimDurumu.value = 'BEKLIYOR'
  teslimNotu.value = ''
}

const opsiyonVerileriniYukle = async () => {
  await Promise.allSettled([
    kasaAPI.getAll().then((r) => { kasalar.value = unwrapList(r) }),
    bankaAPI.getAll().then((r) => { bankalar.value = unwrapList(r) }),
    posAPI.aktif().then((r) => { posTerminalleri.value = unwrapList(r) })
  ])
  soforlerYukleniyor.value = true
  try {
    soforler.value = unwrapList(await teslimatAPI.suruculer())
  } catch {
    soforler.value = []
  } finally {
    soforlerYukleniyor.value = false
  }
}

const tarihAraligi = ref(null)

const tarihParametreleri = () => {
  if (!tarihAraligi.value || tarihAraligi.value.length !== 2 || !tarihAraligi.value[0]) return {}
  return { bas: getLocalDateString(tarihAraligi.value[0]), bit: getLocalDateString(tarihAraligi.value[1]) }
}

onMounted(async () => {
  // Store'lar hata firlatir; bir hata digerlerini engellemesin.
  await Promise.allSettled([satislariYukle(), ozetiYukle(), cariHesapStore.getAllCariHesaplar(), stokStore.getAll()])
  opsiyonVerileriniYukle()
  // FaturaDetay'dan "Duzenle" ile gelindiyse ilgili satisi duzenleme modunda ac.
  const duzenleId = Number(route.query.duzenle)
  if (duzenleId) {
    await openSatisDuzenle({ id: duzenleId })
    // Diyalog kapandiginda query'yi temizle ki yeniden acilmasin.
    router.replace({ query: {} })
  }
})

const satislariYukle = async (yeniSayfa = sayfa.value, yeniBoyut = sayfaBoyutu.value) => {
  loading.value = true
  try {
    const params = { page: yeniSayfa, size: yeniBoyut, tur: 'SATIS', ...tarihParametreleri() }
    if (filtre.value.trim()) params.search = filtre.value.trim()
    if (seciliDurum.value) params.durum = seciliDurum.value
    if (sadeceVadesiGecen.value) params.vadesiGecen = true
    const r = await faturaAPI.getAll(params)
    satislar.value = unwrapList(r)
    toplamKayit.value = r.data?.totalElements ?? satislar.value.length
    sayfa.value = yeniSayfa
    sayfaBoyutu.value = yeniBoyut
  } catch {
    toastBildirim.hata(t('satis.satislarYuklenemedi'))
  } finally {
    loading.value = false
  }
}

// KPI şeridi: gerçekleşmiş (KESİLDİ) satışların özet toplamları.
const ozetiYukle = async () => {
  try {
    const r = await faturaAPI.ozet({ tur: 'SATIS', ...tarihParametreleri() })
    ozet.value = {
      adet: r.data?.adet || 0,
      ciro: r.data?.ciro || 0,
      tahsilEdilen: r.data?.tahsilEdilen || 0,
      kalan: r.data?.kalan || 0
    }
  } catch {
    /* özet kritik değil; sessizce geç */
  }
}

const musteriBasHarfi = (ad) => (ad || '?').trim().charAt(0).toUpperCase() || '?'

const sayfaDegisti = (e) => satislariYukle(e.page, e.rows)

watch(tarihAraligi, () => { satislariYukle(0, sayfaBoyutu.value); ozetiYukle() })
watch([seciliDurum, sadeceVadesiGecen], () => { satislariYukle(0, sayfaBoyutu.value); ozetiYukle() })
watch(filtre, () => {
  clearTimeout(aramaZamanlayici)
  aramaZamanlayici = setTimeout(() => satislariYukle(0, sayfaBoyutu.value), 300)
})

// Vadesi gecen kesilmis satislar icin gecikme gunu (rozet).
const gecikmisGun = (s) => {
  if (s.durum !== 'KESILDI' || !s.vadeTarihi || Number(s.kalanTutar || 0) <= 0) return 0
  const vade = new Date(s.vadeTarihi)
  if (isNaN(vade.getTime())) return 0
  const bugun = new Date()
  vade.setHours(0, 0, 0, 0)
  bugun.setHours(0, 0, 0, 0)
  const gun = Math.floor((bugun - vade) / 86400000)
  return gun > 0 ? gun : 0
}

const TESLIMAT_DURUM_SEVERITY = { BEKLEMEDE: 'warn', YOLDA: 'info', TESLIM_EDILDI: 'success', IPTAL: 'danger' }
const teslimatDurumEtiketi = (d) =>
  ({ BEKLEMEDE: t('teslimatlar.durumBeklemede'), YOLDA: t('teslimatlar.durumYolda'), TESLIM_EDILDI: t('teslimatlar.durumTeslimEdildi'), IPTAL: t('teslimatlar.durumIptal') })[d] || d

const tahsilataGit = (s) => router.push({ name: 'Tahsilat', query: { cariId: s.cariHesapId } })

// Satira tiklayinca sag panelde detay acilir (sayfa degismeden).
const detayAc = async (s) => {
  if (!s?.id) return
  detayGorunur.value = true
  detayYukleniyor.value = true
  detayFatura.value = s
  try {
    const r = await faturaAPI.getById(s.id)
    detayFatura.value = r.data || s
  } catch {
    detayFatura.value = s
  } finally {
    detayYukleniyor.value = false
  }
}

const detayDuzenle = () => {
  if (!detayFatura.value) return
  detayGorunur.value = false
  openSatisDuzenle(detayFatura.value)
}

const satisIptal = (s) => {
  confirm.require({
    message: t('satis.iptalOnay', { no: s.faturaNumarasi }),
    header: t('satis.iptalBaslik'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('common.onay'),
    rejectLabel: t('common.cancel'),
    accept: async () => {
      try {
        await faturaAPI.updateDurum(s.id, 'IPTAL')
        toastBildirim.basarili(t('satis.iptalEdildi'))
        await satislariYukle()
      } catch (e) {
        toastBildirim.hata(e?.response?.data?.message || t('satis.iptalBasarisiz'))
      }
    }
  })
}

// Mevcut satisi kopyalayarak yeni satis diyalogu acar.
const satisCogalt = async (s) => {
  try {
    const r = await faturaAPI.getById(s.id)
    const f = r.data || {}
    satisModu.value = 'SATIS'
    satisForm.value = {
      cariHesapId: f.cariHesapId || null,
      tarih: new Date(),
      aciklama: f.aciklama || '',
      kalemler: (f.kalemler || []).map((k) => ({
        aciklama: k.aciklama || '',
        adet: k.adet || 1,
        birimFiyat: k.birimFiyat || 0,
        iskontoOrani: k.iskontoOrani || 0,
        kdvOrani: k.kdvOrani ?? 20,
        stokId: k.stokId || null
      }))
    }
    showSatisDialog.value = true
  } catch (e) {
    toastBildirim.hata(e?.response?.data?.message || t('satis.satislarYuklenemedi'))
  }
}

const satisEylemleri = (s) => {
  const items = [
    { etiket: t('satis.goruntule'), ikon: 'pi pi-eye', islem: () => router.push(`/faturalar/${s.id}`) }
  ]
  // Duzenleme: iptal edilmemis ve odeme alinmamis faturalar (backend kuraliyla uyumlu).
  if (s.durum !== 'IPTAL' && Number(s.odenenTutar || 0) === 0) {
    items.push({ etiket: t('satis.duzenle'), ikon: 'pi pi-pencil', islem: () => openSatisDuzenle(s) })
  }
  items.push(
    { etiket: t('satis.a4Yazdir'), ikon: 'pi pi-print', islem: () => printFatura(s.id) },
    { etiket: t('satis.termalYazdir'), ikon: 'pi pi-receipt', islem: () => printTermalFis(s) },
    { etiket: t('satis.cogalt'), ikon: 'pi pi-copy', islem: () => satisCogalt(s) }
  )
  if (s.durum === 'KESILDI' && Number(s.kalanTutar || 0) > 0) {
    items.push({ etiket: t('satis.tahsilatAl'), ikon: 'pi pi-money-bill', islem: () => tahsilataGit(s) })
  }
  if (s.durum === 'KESILDI') {
    items.push({ etiket: t('satis.iadeOlustur'), ikon: 'pi pi-replay', islem: () => router.push({ name: 'Iadeler', query: { faturaId: s.id } }) })
    items.push({ etiket: t('satis.efatura'), ikon: 'pi pi-send', islem: () => router.push({ name: 'EFatura', query: { faturaId: s.id } }) })
    items.push({ etiket: t('common.cancel'), ikon: 'pi pi-ban', sinif: 'eylem-sil', islem: () => satisIptal(s) })
  }
  return items
}

// Kalem listesine yeni satir ekler (FaturaKalemleri hizli ekleme / cogaltma).
const kalemEkle = (row) => satisForm.value.kalemler.push(row)

// Satir icinde stok secilince satiri stok bilgisiyle doldurur (KDV: stokta
// tanimliysa o, tanimli degilse 0 kalir).
const stokSatirSecildi = ({ index, stok }) => {
  const k = satisForm.value.kalemler[index]
  if (!k || !stok) return
  k.stokId = stok.id
  k.aciklama = stok.ad
  if (!k.birimFiyat) k.birimFiyat = stok.satisFiyati || stok.fiyat || 0
  if (stok.kdvOrani != null) k.kdvOrani = Number(stok.kdvOrani)
}

const araToplam = computed(() =>
  satisForm.value.kalemler.reduce((t, k) => t + kalemNetTutar(k), 0)
)
const kdvToplam = computed(() =>
  satisForm.value.kalemler.reduce((t, k) => t + kalemKdv(k), 0)
)
const genelToplam = computed(() => araToplam.value + kdvToplam.value)

const dialogBaslik = computed(() => {
  if (duzenlenenId.value) return t('satis.duzenleBaslik')
  return satisModu.value === 'TEKLIF' ? t('satis.yeniTeklif') : t('satis.yeniSatisDialog')
})

const openSatis = () => {
  satisForm.value = { cariHesapId: null, tarih: new Date(), aciklama: '', kalemler: [] }
  musteriSecim.value = null
  musteriOnerileri.value = []
  duzenlenenId.value = null
  satisModu.value = 'SATIS'
  opsiyonlariSifirla()
  showSatisDialog.value = true
}

// Mevcut bir satisi duzenlemek icin formu doldurur (kalem kimlikleri korunur;
// backend stok/bakiye farkini faturaGuncelle ile otomatik duzeltir).
const openSatisDuzenle = async (s) => {
  try {
    const r = await faturaAPI.getById(s.id)
    const f = r.data || {}
    duzenlenenId.value = f.id
    duzenlenenDurum.value = f.durum || 'KESILDI'
    satisModu.value = 'SATIS'
    satisForm.value = {
      cariHesapId: f.cariHesapId || null,
      tarih: f.tarih ? new Date(f.tarih) : new Date(),
      aciklama: f.aciklama || '',
      kalemler: (f.kalemler || []).map((k) => ({
        id: k.id,
        stokId: k.stokId || null,
        aciklama: k.aciklama || '',
        adet: k.adet || 1,
        birimFiyat: k.birimFiyat || 0,
        iskontoOrani: k.iskontoOrani || 0,
        kdvOrani: k.kdvOrani ?? 0
      }))
    }
    // Musteri AutoComplete secili kaydi gostersin.
    musteriSecim.value = f.cariHesapAd ? { id: f.cariHesapId, ad: f.cariHesapAd } : null
    musteriOnerileri.value = []
    showSatisDialog.value = true
  } catch (e) {
    toastBildirim.hata(e?.response?.data?.message || t('satis.satislarYuklenemedi'))
  }
}

const satisiTamamla = async () => {
  if (satisModu.value === 'SATIS' && !satisForm.value.cariHesapId) {
    toastBildirim.uyari(t('satis.musteriSecinizUyari'))
    return
  }
  if (satisForm.value.kalemler.length === 0) {
    toastBildirim.uyari(t('satis.enAzBirUrun'))
    return
  }
  saving.value = true
  try {
    // Teklif modu: fatura yerine GERÇEK Teklif kaydı oluşturulur; böylece teklif
    // Teklifler modülünde görünür ve siparişe/faturaya dönüştürülebilir (eski
    // "durum=TEKLIF" faturası çıkmaz sokaktı).
    if (satisModu.value === 'TEKLIF') {
      await teklifAPI.create({
        cariHesapId: satisForm.value.cariHesapId,
        tarih: getLocalDateString(satisForm.value.tarih),
        tur: 'SATIS',
        durum: 'TASLAK',
        aciklama: satisForm.value.aciklama,
        kalemler: satisForm.value.kalemler.map((k) => ({
          stokId: k.stokId,
          aciklama: k.ad || k.aciklama || '',
          miktar: k.miktar,
          birimFiyat: k.birimFiyat,
          kdvOrani: k.kdvOrani
        }))
      })
      toastBildirim.basarili(t('satis.teklifKaydedildiTekliflerde'))
      showSatisDialog.value = false
      return
    }
    // Duzenlemede tahsilat/teslimat opsiyonlari tekrar uygulanmaz (kayitli
    // deger korunur); yalnizca yeni satista odeme/teslimat gonderilir.
    const ekstra = duzenlenenId.value ? {} : {
      odenenTutar: odemeDurumu.value === 'yok' ? 0 : odenenTutar.value,
      odemeDurumu: odemeDurumEnum.value,
      odemeYontemi: odemeDurumu.value === 'yok' ? '' : odemeYontemi.value,
      kasaId: odemeYontemi.value === 'NAKIT' ? (seciliKasa.value || null) : null,
      bankaId: (odemeYontemi.value === 'KART' || odemeYontemi.value === 'HAVALE') ? (seciliBanka.value || null) : null,
      posTerminaliId: odemeYontemi.value === 'KART' ? (seciliPos.value || null) : null,
      teslimEden: seciliSofor.value?.ad || null,
      teslimDurumu: seciliSofor.value ? teslimDurumu.value : null,
      teslimNotu: seciliSofor.value ? (teslimNotu.value || null) : null
    }
    const payload = satisPayloadUret({
      cariHesapId: satisForm.value.cariHesapId,
      tur: 'SATIS',
      durum: duzenlenenId.value ? duzenlenenDurum.value : 'KESILDI',
      tarih: getLocalDateString(satisForm.value.tarih),
      aciklama: satisForm.value.aciklama,
      kalemler: satisForm.value.kalemler,
      ekstra
    })
    if (duzenlenenId.value) {
      await faturaAPI.update(duzenlenenId.value, payload)
      toastBildirim.basarili(t('satis.duzenleEdildi'))
      showSatisDialog.value = false
      duzenlenenId.value = null
      await satislariYukle()
      return
    }
    // Teslimat adresi sofor secildiyse zorunlu.
    if (seciliSofor.value && !teslimatAdresi.value.trim()) {
      toastBildirim.uyari(t('hizliSatis.teslimatAdresiGerekli'))
      saving.value = false
      return
    }
    const yanit = await faturaAPI.create(payload)
    showSatisDialog.value = false
    // Sofor secildiyse satis sonrasi teslimat kaydi acilir (POS ile ayni akis).
    if (seciliSofor.value?.id && yanit?.data?.id) {
      const mutabakat = { BEKLIYOR: 'BEKLEMEDE', YOLDA: 'YOLDA', TESLIM_EDILDI: 'TESLIM_EDILDI' }
      try {
        await teslimatAPI.olustur({
          faturaId: yanit.data.id,
          driverId: seciliSofor.value.id,
          teslimatAdresi: teslimatAdresi.value.trim(),
          notlar: teslimNotu.value?.trim() || null,
          durum: mutabakat[teslimDurumu.value] || 'BEKLEMEDE'
        })
      } catch (e) {
        toastBildirim.uyari(e?.response?.data?.message || t('hizliSatis.teslimatKaydedilemedi'))
      }
    }
    toastBildirim.basarili(t('satis.satisTamamlandi'))
    await satislariYukle()
  } catch (err) {
    const msg = err.response?.data?.message || t('satis.satisBasarisiz')
    toastBildirim.hata(msg)
  } finally {
    saving.value = false
  }
}

const printFatura = (id) => {
  if (id) faturaAPI.yazdirmaKaydet(id, { format: 'A4' }).catch(() => {})
  window.open(`/faturalar/${id}?print=true`, '_blank')
}
const durumLabel = (d) => durumLabelUtil(d, t)

const odemeDurumEtiketi = (d) => ({
  ODENDI: t('faturaDetay.odendi'),
  KISMI_ODENDI: t('faturaDetay.kismiOdedi'),
  ODENMEDI: t('faturaDetay.odenmedi')
})[d] || '-'

const odemeDurumSeverity = (d) =>
  ({ ODENDI: 'success', KISMI_ODENDI: 'warn', ODENMEDI: 'danger' })[d] || 'secondary'
import { formatTarih as formatDate } from '../utils/format.js'
const printTermalFis = (satisData) => {
  if (satisData?.id) faturaAPI.yazdirmaKaydet(satisData.id, { format: 'TERMAL80' }).catch(() => {})

  const kalemlerHtml = (satisData.kalemler || [])
    .map(
      (k) => `
    <tr>
      <td style="text-align:left;">${escapeHtml(k.stokAd || k.ad || t('faturalar.urun'))} x${k.miktar || k.adet || 1}</td>
      <td style="text-align:right;">${formatCurrency(k.toplamTutar || k.miktar * k.birimFiyat || k.adet * k.birimFiyat)}</td>
    </tr>
  `
    )
    .join('')

  const content = `
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8">
      <title>${t('satis.fisBaslik')} - ${escapeHtml(satisData.faturaNumarasi || '')}</title>
      <style>
        @page { size: 80mm auto; margin: 0; }
        body { font-family: 'Courier New', Courier, monospace; width: 72mm; margin: 0 auto; padding: 10px 0; font-size: 12px; color: #000; }
        .text-center { text-align: center; }
        .text-right { text-align: right; }
        .bold { font-weight: bold; }
        .line { border-top: 1px dashed #000; margin: 6px 0; }
        table { width: 100%; border-collapse: collapse; margin: 6px 0; }
        td, th { padding: 3px 0; vertical-align: top; font-size: 11px; }
        .header { margin-bottom: 8px; }
        .header h2 { margin: 0; font-size: 16px; font-weight: bold; }
        .logo { display: block; max-height: 48px; max-width: 140px; margin: 0 auto 4px; object-fit: contain; }
        .raspel-mini { font-size: 9px; color: #666; margin: 0; }
        .header p { margin: 2px 0; font-size: 10px; }
        .footer { margin-top: 10px; text-align: center; font-size: 10px; }
        .no-print { text-align: center; margin-bottom: 12px; }
        .no-print button { padding: 6px 16px; background: var(--accent-hover); color: #fff; border: none; border-radius: 4px; cursor: pointer; font-weight: bold; }
        @media print { .no-print { display: none !important; } }
      </style>
    </head>
    <body>
      <div class="no-print">
        <button onclick="window.print()">Yazdır (Termal 80mm)</button>
        <button onclick="window.close()" style="background:#64748b; margin-left:6px;">${t('common.close')}</button>
      </div>
      <div class="header text-center">
        ${sirketLogosu.value ? `<img class="logo" src="${escapeHtml(sirketLogosu.value)}" alt="logo" />` : ''}
        <h2>${escapeHtml(authStore?.sirketAdi || 'RASPEL ERP')}</h2>
        <p class="raspel-mini">RasPel ERP</p>
        <p>SATIŞ FİŞİ</p>
        <p>${t('satis.fisNo')} ${escapeHtml(satisData.faturaNumarasi || 'FIS-' + (satisData.id || Date.now()))}</p>
        <p>${t('common.date')}: ${formatDate(satisData.tarih || new Date())}</p>
        <p>${t('satis.musteriLabel')} ${escapeHtml(satisData.cariHesapAd || t('satis.perakendeMusteri'))}</p>
      </div>
      <div class="line"></div>
      <table>
        <thead>
          <tr>
            <th style="text-align:left;">Ürün / Miktar</th>
            <th style="text-align:right;">Tutar</th>
          </tr>
        </thead>
        <tbody>
          ${kalemlerHtml.length ? kalemlerHtml : `<tr><td colspan="2">${t('satis.fisKalemFallback')}</td></tr>`}
        </tbody>
      </table>
      <div class="line"></div>
      <table>
        <tr>
          <td>ARA TOPLAM:</td>
          <td class="text-right bold">${formatCurrency(satisData.araToplam || satisData.genelToplam || 0)}</td>
        </tr>
        <tr>
          <td>KDV:</td>
          <td class="text-right">${formatCurrency(satisData.kdvToplam || 0)}</td>
        </tr>
        <tr style="font-size:13px;">
          <td class="bold">GENEL TOPLAM:</td>
          <td class="text-right bold">${formatCurrency(satisData.genelToplam || 0)}</td>
        </tr>
      </table>
      <div class="line"></div>
      <div class="footer">
        <p>Bizi tercih ettiğiniz için teşekkür ederiz!</p>
        <p>Yazilim: RasPel ERP</p>
        <p>İşlem Yapan: ${escapeHtml(authStore?.kullanici?.displayName || '-')}</p>
      </div>
    </body>
    </html>
  `
  const pencere = fisPenceresiAcVeYazdir(content)
  if (!pencere) {
    toastBildirim.hata(t('satis.pencereAcılmadi'))
  }
}
</script>

<style scoped>
.satis-container {
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
.sayfa-baslik {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.sayfa-baslik h1 {
  margin: 0;
}
.kpi-serit {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(200px, 100%), 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.filtre-cubugu {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 12px 16px;
  margin-bottom: 16px;
}
.arama-kutu {
  flex: 1 1 220px;
  min-width: 200px;
}
.arama-kutu :deep(.p-inputtext) {
  width: 100%;
}
.durum-cipleri {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.durum-cip {
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  background: var(--bg-muted, rgba(148, 163, 184, 0.08));
  border: 1px solid var(--border);
  color: var(--text-secondary);
  cursor: pointer;
  transition: background var(--dur-fast, 0.15s) ease, color var(--dur-fast, 0.15s) ease, border-color var(--dur-fast, 0.15s) ease;
}
.durum-cip:hover {
  color: var(--text-primary);
  border-color: var(--accent-border);
}
.durum-cip.aktif {
  background: var(--accent-soft-strong);
  color: var(--accent);
  border-color: var(--accent-border);
}
.durum-cip.kesildi.aktif {
  background: var(--success-soft);
  color: var(--success);
  border-color: var(--success-border);
}
.durum-cip.taslak.aktif {
  background: var(--warning-soft);
  color: var(--warning);
  border-color: var(--warning-border);
}
.durum-cip.iptal.aktif {
  background: var(--danger-soft);
  color: var(--danger);
  border-color: var(--danger-border);
}
.vade-toggle {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  color: var(--text-secondary);
  cursor: pointer;
  white-space: nowrap;
}
.table-container {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px;
}
.satis-tablo :deep(.p-datatable-thead > tr > th) {
  font-size: 11.5px;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  color: var(--text-muted);
}
.satis-tablo :deep(.p-datatable-tbody > tr) {
  transition: background var(--dur-fast, 0.15s) ease;
}
.tutar-hucre {
  text-align: right;
  font-variant-numeric: tabular-nums;
}
.genel-toplam-hucre {
  font-weight: 700;
  color: var(--text-primary);
}
.musteri-hucre {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.musteri-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12.5px;
  font-weight: 700;
  background: var(--accent-soft-strong);
  color: var(--accent);
}
.musteri-ad {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.form-grid-2 {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);
  gap: 16px;
}
.form-group {
  min-width: 0;
}
.form-group label {
  display: block;
  margin-bottom: 6px;
  font-weight: 600;
  color: var(--text-secondary);
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.urun-ekleme {
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: var(--bg-muted, rgba(148, 163, 184, 0.08));
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px;
}
.urun-ekle-satir {
  display: grid;
  grid-template-columns: minmax(0, 3fr) minmax(0, 1fr) minmax(0, 1fr) auto;
  gap: 12px;
  align-items: end;
}
.urun-ekle-btn {
  display: flex;
  align-items: flex-end;
}
.kalem-bolum {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.kalem-tablo {
  overflow-x: auto;
}
.bolum-etiket {
  color: var(--text-muted);
  font-weight: 600;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.bolum-baslik {
  margin: 0;
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 700;
}
.ipucu-metin {
  color: var(--text-muted);
}
.urun-opsiyon {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}
.urun-kod {
  color: var(--text-muted);
  font-family: monospace;
  font-size: 11px;
  flex-shrink: 0;
}
.urun-ad {
  flex: 1;
  color: var(--text-primary);
}
.urun-stok {
  color: var(--success);
  font-size: 12px;
  font-weight: 600;
}
.urun-fiyat {
  color: var(--text-secondary);
  font-size: 12px;
}
.durum-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
}
.durum-badge.taslak {
  background: var(--warning-soft);
  color: var(--warning);
}
.durum-badge.teklif {
  background: var(--accent-soft-strong);
  color: var(--accent);
}
.durum-badge.kesildi {
  background: var(--success-soft);
  color: var(--success);
}
.durum-badge.iptal {
  background: var(--bg-muted, rgba(148, 163, 184, 0.1));
  color: var(--text-muted);
}
.fatura-no-link {
  color: var(--accent);
  font-weight: 600;
  cursor: pointer;
}
.musteri-opsiyon {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  width: 100%;
}
.musteri-opsiyon-ad {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.musteri-opsiyon-detay {
  font-size: 11.5px;
  color: var(--text-muted);
  white-space: nowrap;
}
.satis-detay-drawer :deep(.p-drawer-content) {
  padding-top: 0;
}
.detay-yukleniyor {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-muted);
  padding: 20px 0;
}
.detay-ust {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}
.detay-satir {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 6px 0;
  font-size: 13.5px;
  color: var(--text-secondary);
}
.detay-satir strong {
  color: var(--text-primary);
  text-align: right;
}
.detay-baslik {
  margin: 16px 0 8px;
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--text-muted);
}
.detay-kalemler {
  display: flex;
  flex-direction: column;
  gap: 6px;
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 10px;
}
.detay-kalem {
  display: grid;
  grid-template-columns: 1fr auto auto;
  gap: 8px;
  align-items: center;
  font-size: 13px;
}
.detay-kalem-ad {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-primary);
}
.detay-kalem-adet {
  color: var(--text-muted);
  white-space: nowrap;
}
.detay-kalem-tutar {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
.detay-toplamlar {
  margin-top: 12px;
  border-top: 1px solid var(--border);
  padding-top: 8px;
}
.detay-toplamlar .detay-genel strong {
  font-size: 16px;
}
.detay-toplamlar .kalan-var strong {
  color: var(--danger);
}
.detay-aksiyonlar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 18px;
}
.satis-modu {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.modu-radio-group {
  display: flex;
  gap: 2px;
  background: var(--border);
  border-radius: 8px;
  padding: 3px;
}
.modu-option {
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
}
.modu-option:hover {
  color: var(--text-primary);
}
.modu-option.active {
  background: var(--accent);
  color: var(--accent-contrast);
}
.w-full {
  width: 100% !important;
}
/* Kalan tutar, gecikme ve teslimat rozetleri */
.kalan-var {
  color: var(--danger);
  font-weight: 700;
}
.gecikme-rozet {
  margin-left: 6px;
}
.teslimat-yok {
  color: var(--text-muted);
}
</style>
