<template>
  <div class="stoklar-container">
    <h1>{{ t('stoklar.title') }}</h1>
    <IlkZiyaretIpuclari
      anahtar="stoklar"
      :baslik="t('stoklar.ipucuBaslik')"
      :metin="t('stoklar.ipucuMetin')"
    />
    <Toolbar class="toolbar">
      <template #start>
        <Button
          :label="t('stoklar.yeniUrun')"
          icon="pi pi-plus"
          class="p-button-success"
          @click="openDialog"
        />
        <Button
          :label="t('stoklar.topluFiyatGuncelle')"
          icon="pi pi-dollar"
          class="p-button-help"
          style="margin-left: 8px"
          @click="batchFiyatDialog = true"
        />
        <div
          v-if="seciliStoklar && seciliStoklar.length > 0"
          class="batch-actions"
        >
          <span class="batch-count">{{ seciliStoklar ? seciliStoklar.length : 0 }} {{ t('stoklar.secili') }}</span>
          <Button
            :label="t('stoklar.topluSil')"
            icon="pi pi-trash"
            class="p-button-sm p-button-danger"
            @click="batchSil"
          />
          <Button
            :label="t('stoklar.csvAktar')"
            icon="pi pi-download"
            class="p-button-sm p-button-outlined"
            @click="batchCsvExport"
          />
        </div>
      </template>
      <template #end>
        <Button
          label="Excel"
          icon="pi pi-file-excel"
          class="p-button-sm p-button-outlined"
          style="margin-right: 8px"
          @click="excelIndir"
        />
        <div class="toolbar-end">
          <span class="p-input-icon-left">
            <i class="pi pi-search" />
            <InputText
              v-model="aramaMetni"
              :placeholder="t('stoklar.aramaPlaceholder')"
              @input="ara"
            />
          </span>
          <Button
            :icon="gosterim === 'tablo' ? 'pi pi-th-large' : 'pi pi-list'"
            class="p-button-text p-button-sm"
            :title="gosterim === 'tablo' ? t('stoklar.kartGorunumu') : t('stoklar.tabloGorunumu')"
            @click="gosterim = gosterim === 'tablo' ? 'kart' : 'tablo'"
          />
        </div>
      </template>
    </Toolbar>

    <div class="filter-bar">
      <span class="p-input-icon-left">
        <i class="pi pi-search" />
        <InputText
          v-model="filtreArama"
          :placeholder="t('stoklar.filtreArama')"
          @input="filtreDegisti"
        />
      </span>
      <InputText
        v-model="filtreKategori"
        :placeholder="t('stoklar.filtreKategori')"
        class="filter-input"
        @input="filtreDegisti"
      />
      <InputText
        v-model="filtreMarka"
        :placeholder="t('stoklar.filtreMarka')"
        class="filter-input"
        @input="filtreDegisti"
      />
      <Dropdown
        v-model="filtreStokGrubu"
        :options="['', 'Hammadde', 'Mamul', 'Yari Mamul', 'Sarf', 'Aksesuar']"
        :placeholder="t('stoklar.filtreStokGrubu')"
        class="filter-dropdown"
        @change="filtreDegisti"
      />
      <InputNumber
        v-model="filtreMinFiyat"
        :placeholder="t('stoklar.minFiyat')"
        class="filter-input-sm"
        @input="filtreDegisti"
      />
      <InputNumber
        v-model="filtreMaxFiyat"
        :placeholder="t('stoklar.maxFiyat')"
        class="filter-input-sm"
        @input="filtreDegisti"
      />
      <Button
        icon="pi pi-times"
        class="p-button-text p-button-sm"
        :title="t('stoklar.temizle')"
        @click="filtreTemizle"
      />
    </div>

    <div
      v-if="stokStore.loading"
      class="loading"
    >
      <p><i class="pi pi-spin pi-spinner" /> Yükleniyor...</p>
    </div>

    <template v-if="!stokStore.loading && gosterim === 'tablo'">
      <DataTable
        v-model:selection="seciliStoklar"
        :value="stokStore.stoklar"
        :paginator="true"
        :rows="25"
        :rows-per-page-options="[15, 25, 50, 100]"
        :lazy="true"
        :total-records="stokStore.toplamKayit"
        paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown CurrentPageReport"
        current-page-report-template="{totalRecords} kayıttan {first}-{last}"
        selection-mode="multiple"
        data-key="id"
        striped-rows
        sort-field="miktar"
        :sort-order="1"
        class="p-datatable-sm"
        :global-filter-fields="['ad', 'stokKodu', 'birim']"
        @page="stokSayfaDegisti"
        @row-click="stokSec($event.data)"
      >
        <template #header>
          <div class="table-header">
            <span class="toplam-bilgi">{{ stokStore.toplamKayit }} {{ t('stoklar.urun') }}</span>
            <span
              v-if="kritikAdet > 0"
              class="kritik-bilgi"
            ><i class="pi pi-exclamation-triangle" /> {{ kritikAdet }} {{ t('stoklar.kritik') }}</span>
          </div>
        </template>
        <template #empty>
          <EmptyState
            :message="t('stoklar.empty')"
            :sub-message="t('stoklar.emptyHint')"
            icon="pi pi-box"
            :action-label="t('stoklar.emptyAction')"
            action-icon="pi pi-plus"
            @action="openDialog"
          />
        </template>
        <Column
          selection-mode="multiple"
          header-style="width: 2.5rem"
        />
        <Column
          field="stokKodu"
          :header="t('stoklar.colStokKodu')"
          sortable
          style="width: 120px"
        />
        <Column
          field="ad"
          :header="t('stoklar.colUrunAdi')"
          sortable
          style="min-width: 180px"
        />
        <Column
          field="birim"
          :header="t('stoklar.colBirim')"
          sortable
          style="width: 90px"
        />
        <Column
          field="miktar"
          :header="t('stoklar.colMiktar')"
          sortable
          style="width: 110px"
        >
          <template #body="s">
            <span :class="s.data.minMiktar && s.data.miktar <= s.data.minMiktar ? 'kritik' : 'normal'">
              {{ s.data.miktar }} {{ s.data.birim || '' }}
            </span>
          </template>
        </Column>
        <Column
          field="fiyat"
          :header="t('stoklar.colBirimFiyat')"
          sortable
          style="width: 130px"
        >
          <template #body="s">
            {{ formatCurrency(s.data.fiyat) }}
          </template>
        </Column>
        <Column
          field="tedarikciAd"
          :header="t('stoklar.colTedarikci')"
          sortable
          style="width: 150px"
        >
          <template #body="s">
            <span v-if="s.data.tedarikciAd"><i
              class="pi pi-building"
              style="margin-right: 6px; color: #3b82f6"
            />{{ s.data.tedarikciAd }}</span>
            <span
              v-else
              class="text-muted"
            >-</span>
          </template>
        </Column>
        <Column
          :header="t('stoklar.colStokDegeri')"
          sortable
          style="width: 130px"
        >
          <template #body="s">
            {{ formatCurrency((s.data.miktar || 0) * (s.data.fiyat || 0)) }}
          </template>
        </Column>
        <Column
          :header="t('stoklar.colKritik')"
          style="width: 80px"
        >
          <template #body="s">
            <i
              v-if="s.data.minMiktar && s.data.miktar <= s.data.minMiktar"
              class="pi pi-exclamation-triangle"
              style="color: #f87171; font-size: 16px"
            />
          </template>
        </Column>
        <Column
          header=""
          style="width: 100px"
        >
          <template #body="s">
            <Button
              icon="pi pi-pencil"
              class="p-button-rounded p-button-info p-button-sm"
              style="margin-right: 6px"
              @click.stop="editStok(s.data)"
            />
            <Button
              icon="pi pi-barcode"
              class="p-button-rounded p-button-success p-button-sm"
              style="margin-right: 6px"
              @click.stop="barkodEtiket(s.data)"
            />
            <Button
              icon="pi pi-trash"
              class="p-button-rounded p-button-danger p-button-sm"
              @click.stop="confirmDel(s.data.id)"
            />
          </template>
        </Column>
      </DataTable>
    </template>

    <div
      v-if="!stokStore.loading && gosterim === 'kart'"
      class="stok-kartlar"
    >
      <StokKart
        v-for="s in stokStore.stoklar"
        :key="s.id"
        :stok="s"
        @sec="stokSec"
        @duzenle="editStok"
        @sil="confirmDel"
      />
      <Message
        v-if="filtrelenmisStoklar && filtrelenmisStoklar.length === 0"
        severity="info"
        :text="t('stoklar.eslesenYok')"
        class="full-width"
      />
    </div>

    <StokHareketBolum
      v-if="seciliStok"
      :stok="seciliStok"
      :hareketler="stokHareketler"
      @giris="openHareketDialog('GIRIS')"
      @cikis="openHareketDialog('CIKIS')"
      @kapat="seciliStok = null"
      @sil="delHareket"
    />

    <Dialog
      v-model:visible="showDialog"
      :header="editingId ? t('stoklar.urunDuzenle') : t('stoklar.yeniUrunDialog')"
      :modal="true"
      style="width: 650px"
    >
      <div class="form-section">
        <div class="form-section-title">
          {{ t('stoklar.temelBilgiler') }}
        </div>
        <div class="form-row">
          <div class="form-grup flex-2">
            <label>{{ t('stoklar.urunAdi') }}</label>
            <InputText
              v-model="form.ad"
              :placeholder="t('stoklar.urunAdiPlaceholder')"
              class="w-full"
            />
          </div>
          <div class="form-grup">
            <label>{{ t('stoklar.birim') }}</label>
            <Dropdown
              v-model="form.birim"
              :options="['Adet', 'Koli', 'Kg', 'Metre', 'Litre', 'Paket']"
              :placeholder="t('stoklar.seciniz')"
              class="w-full"
            />
          </div>
        </div>
        <div class="form-row">
          <div class="form-grup">
            <label>{{ t('stoklar.stokKodu') }}</label>
            <InputText
              v-model="form.stokKodu"
              :placeholder="t('stoklar.stokKoduPlaceholder')"
              class="w-full"
            />
          </div>
          <div class="form-grup">
            <label>{{ t('stoklar.barkod') }}</label>
            <InputText
              v-model="form.barkod"
              :placeholder="t('stoklar.barkodPlaceholder')"
              class="w-full"
            />
          </div>
        </div>
        <div class="form-row">
          <div class="form-grup">
            <label>{{ t('stoklar.marka') }}</label>
            <InputText
              v-model="form.marka"
              :placeholder="t('stoklar.markaPlaceholder')"
              class="w-full"
            />
          </div>
          <div class="form-grup">
            <label>{{ t('stoklar.kategori') }}</label>
            <InputText
              v-model="form.kategori"
              :placeholder="t('stoklar.kategoriPlaceholder')"
              class="w-full"
            />
          </div>
        </div>
        <div class="form-row">
          <div class="form-grup">
            <label>{{ t('stoklar.stokGrubu') }}</label>
            <InputText
              v-model="form.stokGrubu"
              :placeholder="t('stoklar.stokGrubuPlaceholder')"
              class="w-full"
            />
          </div>
          <div class="form-grup">
            <label>{{ t('stoklar.rafNo') }}</label>
            <InputText
              v-model="form.rafNo"
              :placeholder="t('stoklar.rafNoPlaceholder')"
              class="w-full"
            />
          </div>
        </div>
        <div class="form-row">
          <div class="form-grup">
            <label>{{ t('stoklar.birim2') }}</label>
            <InputText
              v-model="form.birim2"
              :placeholder="t('stoklar.birim2Placeholder')"
              class="w-full"
            />
          </div>
          <div class="form-grup">
            <label>{{ t('stoklar.cevrimKatsayisi') }}</label>
            <InputNumber
              v-model="form.cevrimKatsayisi"
              :min="0"
              :min-fraction-digits="4"
              class="w-full"
              placeholder="1.0000"
            />
          </div>
        </div>
      </div>
      <div class="form-section">
        <div class="form-section-title">
          {{ t('stoklar.fiyatStok') }}
        </div>
        <div class="form-row">
          <div class="form-grup">
            <label>{{ t('stoklar.alisFiyati') }}</label>
            <InputNumber
              v-model="form.fiyat"
              :min="0"
              :min-fraction-digits="2"
              class="w-full"
            />
          </div>
          <div class="form-grup">
            <label>{{ t('stoklar.satisFiyati') }}</label>
            <InputNumber
              v-model="form.satisFiyati"
              :min="0"
              :min-fraction-digits="2"
              class="w-full"
            />
          </div>
        </div>
        <div class="form-row">
          <div class="form-grup">
            <label>{{ t('stoklar.kdvOrani') }}</label>
            <InputNumber
              v-model="form.kdvOrani"
              :min="0"
              :max="100"
              class="w-full"
              placeholder="%"
            />
          </div>
          <div class="form-grup">
            <label>{{ t('stoklar.agirlik') }}</label>
            <InputNumber
              v-model="form.agirlik"
              :min="0"
              :min-fraction-digits="2"
              class="w-full"
            />
          </div>
        </div>
        <div class="form-row">
          <div class="form-grup">
            <label>{{ t('stoklar.mevcutMiktar') }}</label>
            <InputNumber
              v-model="form.miktar"
              :min="0"
              :min-fraction-digits="0"
              class="w-full"
            />
          </div>
          <div class="form-grup">
            <label>{{ t('stoklar.minStok') }}</label>
            <InputNumber
              v-model="form.minMiktar"
              :min="0"
              :min-fraction-digits="0"
              class="w-full"
            />
          </div>
        </div>
        <div class="form-row">
          <div class="form-grup">
            <label>{{ t('stoklar.maliyetYontemi') }}</label>
            <Dropdown
              v-model="form.maliyetYontemi"
              :options="maliyetYontemiSecenekleri"
              class="w-full"
            />
          </div>
          <div class="form-grup">
            <label />
          </div>
        </div>
      </div>
      <div class="form-section">
        <div class="form-section-title">
          {{ t('stoklar.tedarikciBilgileri') }}
        </div>
        <div class="form-row">
          <div class="form-grup">
            <label>{{ t('stoklar.tedarikci') }}</label>
            <Dropdown
              v-model="form.tedarikciId"
              :options="cariHesapStore?.cariHesaplar || []"
              option-label="ad"
              option-value="id"
              :placeholder="t('stoklar.tedarikciSecin')"
              class="w-full"
            />
          </div>
          <div class="form-grup">
            <label>{{ t('stoklar.tedarikciStokKodu') }}</label>
            <InputText
              v-model="form.tedarikciStokKodu"
              :placeholder="t('stoklar.tedarikciStokKoduPlaceholder')"
              class="w-full"
            />
          </div>
        </div>
        <div class="form-row">
          <div class="form-grup">
            <label>{{ t('stoklar.tedarikciFiyati') }}</label>
            <InputNumber
              v-model="form.tedarikciFiyat"
              :min="0"
              :min-fraction-digits="2"
              class="w-full"
            />
          </div>
          <div class="form-grup">
            <label />
          </div>
        </div>
      </div>
      <div class="form-section">
        <div class="form-section-title">
          {{ t('stoklar.ekBilgiler') }}
        </div>
        <div class="form-grup">
          <label>{{ t('stoklar.urunFotografi') }}</label>
          <div class="foto-satir">
            <img
              v-if="form.fotoUrl"
              :src="form.fotoUrl"
              class="foto-onizle"
              alt="foto"
            >
            <input
              ref="fotoInput"
              type="file"
              accept="image/*"
              hidden
              @change="fotoSec"
            >
            <Button
              :label="t('stoklar.fotografYukle')"
              icon="pi pi-image"
              class="p-button-outlined"
              @click="$refs.fotoInput.click()"
            />
            <Button
              v-if="form.fotoUrl"
              :label="t('stoklar.kaldir')"
              icon="pi pi-times"
              class="p-button-text p-button-danger"
              @click="form.fotoUrl = ''"
            />
          </div>
        </div>
        <div class="form-grup">
          <label>{{ t('common.description') }}</label>
          <Textarea
            v-model="form.aciklama"
            rows="2"
            class="w-full"
          />
        </div>

        <div class="form-grup coklu-fiyat-bolumu">
          <div class="coklu-fiyat-baslik">
            <label>{{ t('stoklar.fiyatlar') }}</label>
            <span class="coklu-fiyat-ipucu">{{ t('stoklar.fiyatIpuclari') }}</span>
          </div>
          <div
            v-for="f in form.fiyatlar"
            :key="f.id || f.ad"
            class="coklu-fiyat-satir"
          >
            <InputText
              v-model="f.ad"
              :placeholder="t('stoklar.fiyatAdiPlaceholder')"
              class="fiyat-ad-input"
            />
            <InputNumber
              v-model="f.fiyat"
              mode="currency"
              currency="TRY"
              locale="tr-TR"
              :min-fraction-digits="2"
              class="fiyat-tutar-input"
            />
            <Button
              icon="pi pi-trash"
              class="p-button-rounded p-button-text p-button-danger"
              @click="fiyatSil(f)"
            />
          </div>
          <Button
            :label="t('stoklar.fiyatEkle')"
            icon="pi pi-plus"
            size="small"
            class="p-button-outlined"
            @click="fiyatEkle"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="showDialog = false"
        />
        <Button
          :label="editingId ? t('stoklar.guncelle') : t('common.save')"
          icon="pi pi-check"
          :loading="saving"
          @click="saveStok"
        />
      </template>
    </Dialog>

    <StokHareketDialog
      v-model:visible="showHareketDialog"
      v-model:miktar="hareketForm.miktar"
      v-model:hareket-tarihi="hareketForm.hareketTarihi"
      v-model:cari-hesap-id="hareketForm.cariHesapId"
      v-model:aciklama="hareketForm.aciklama"
      :baslik="hareketBaslik"
      :cari-hesaplar="cariHesapStore?.cariHesaplar || []"
      :loading="saving"
      @kaydet="saveHareket"
    />

    <StokTopluFiyatDialog
      v-model:visible="batchFiyatDialog"
      v-model:yon="batchFiyatForm.yon"
      v-model:oran="batchFiyatForm.oran"
      v-model:kategori="batchFiyatForm.kategori"
      v-model:stok-grubu="batchFiyatForm.stokGrubu"
      :loading="batchLoading"
      @uygula="batchFiyatUygula"
    />

    <StokDetayDialog
      v-model:visible="showDetailDialog"
      :stok="detailStok"
      :hareketler="hareketler"
      :hareketler-yukleniyor="hareketlerYukleniyor"
    />

    <BarkodEtiketDialog
      v-model:visible="etiketDialog"
      :stok="etiketStok"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { useI18n } from 'vue-i18n'
import { useStokStore } from '../stores/stokStore.js'
import { useCariHesapStore } from '../stores/cariHesapStore.js'
import { stokAPI, excelAPI, uploadAPI } from '../api/index.js'
import EmptyState from '../components/EmptyState.vue'
import IlkZiyaretIpuclari from '../components/IlkZiyaretIpuclari.vue'
import StokHareketDialog from '../components/StokHareketDialog.vue'
import StokTopluFiyatDialog from '../components/StokTopluFiyatDialog.vue'
import StokDetayDialog from '../components/StokDetayDialog.vue'
import StokKart from '../components/StokKart.vue'
import StokHareketBolum from '../components/StokHareketBolum.vue'
import BarkodEtiketDialog from '../components/BarkodEtiketDialog.vue'
import { useKisayollar } from '../composables/useKisayollar.js'
import { useFormKorumasi } from '../composables/useFormKorumasi.js'
import { useGeriAl } from '../composables/useGeriAl.js'
import { formatCurrency } from '../utils/format.js'

const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { t } = useI18n()
const stokStore = useStokStore()
const cariHesapStore = useCariHesapStore()

useKisayollar({
  yeni: () => openDialog(),
  iptal: () => {
    showDialog.value = false
  },
  kaydet: () => saveStok()
})

const aramaMetni = ref('')
let aramaZaman = null
onUnmounted(() => {
  if (aramaZaman) clearTimeout(aramaZaman)
})

const filtreArama = ref('')
const filtreKategori = ref('')
const filtreMarka = ref('')
const filtreStokGrubu = ref('')
const filtreMinFiyat = ref(null)
const filtreMaxFiyat = ref(null)

const stokSayfa = ref(0)
const stokSayfaBoyutu = ref(25)
let stokAramaZaman = null

const seciliStok = ref(null)
const seciliStokId = ref(null)
const seciliStoklar = ref([])
const stokHareketler = ref([])
const showDetailDialog = ref(false)
const detailStok = ref(null)
const hareketler = ref([])
const hareketlerYukleniyor = ref(false)
const saving = ref(false)
const gosterim = ref('tablo')

const etiketDialog = ref(false)
const etiketStok = ref(null)

const barkodEtiket = (stok) => {
  etiketStok.value = stok
  etiketDialog.value = true
}

const showDialog = ref(false)
const editingId = ref(null)
const maliyetYontemiSecenekleri = computed(() => [
  { label: t('stoklar.ortalamaMaliyet'), value: 'ORTALAMA' },
  { label: t('stoklar.fifo'), value: 'FIFO' },
  { label: t('stoklar.lifo'), value: 'LIFO' }
])
const form = ref({
  stokKodu: '',
  barkod: '',
  ad: '',
  birim: '',
  birim2: '',
  cevrimKatsayisi: null,
  marka: '',
  stokGrubu: '',
  kategori: '',
  rafNo: '',
  fiyat: 0,
  satisFiyati: null,
  kdvOrani: null,
  agirlik: null,
  miktar: 0,
  minMiktar: null,
  tedarikciId: null,
  tedarikciStokKodu: '',
  tedarikciFiyat: null,
  maliyetYontemi: 'ORTALAMA',
  aciklama: '',
  fotoUrl: '',
  fiyatlar: []
})

const { temizle: formTemizle } = useFormKorumasi(form)
const { silVeGeriAl } = useGeriAl()

const showHareketDialog = ref(false)
const hareketTur = ref('GIRIS')
const hareketForm = ref({ miktar: null, hareketTarihi: new Date(), cariHesapId: null, aciklama: '' })

const hareketBaslik = computed(() => (hareketTur.value === 'GIRIS' ? t('stoklar.hareketGiris') : t('stoklar.hareketCikis')))

const filtrelenmisStoklar = computed(() => {
  return stokStore.stoklar.filter((s) => {
    const q = filtreArama.value.toLowerCase()
    if (
      filtreArama.value &&
      !s.ad?.toLowerCase().includes(q) &&
      !s.stokKodu?.toLowerCase().includes(q) &&
      !s.barkod?.toLowerCase().includes(q)
    )
      return false
    if (filtreKategori.value && s.kategori !== filtreKategori.value) return false
    if (filtreMarka.value && !s.marka?.toLowerCase().includes(filtreMarka.value.toLowerCase())) return false
    if (filtreStokGrubu.value && s.stokGrubu !== filtreStokGrubu.value) return false
    if (filtreMinFiyat.value != null && (s.fiyat || 0) < filtreMinFiyat.value) return false
    if (filtreMaxFiyat.value != null && (s.fiyat || 0) > filtreMaxFiyat.value) return false
    return true
  })
})

const stoklariYukle = async () => {
  const params = { page: stokSayfa.value, size: stokSayfaBoyutu.value }
  if (filtreArama.value.trim()) params.q = filtreArama.value.trim()
  if (filtreKategori.value) params.kategori = filtreKategori.value
  if (filtreMarka.value) params.marka = filtreMarka.value
  if (filtreStokGrubu.value) params.stokGrubu = filtreStokGrubu.value
  if (filtreMinFiyat.value != null) params.minFiyat = filtreMinFiyat.value
  if (filtreMaxFiyat.value != null) params.maxFiyat = filtreMaxFiyat.value
  await stokStore.filtreli(params)
}

const stokSayfaDegisti = (event) => {
  stokSayfa.value = event.page
  stokSayfaBoyutu.value = event.rows
  stoklariYukle()
}

// Filtre değişince debounce ile yeniden yükle
const filtreDegisti = () => {
  if (stokAramaZaman) clearTimeout(stokAramaZaman)
  stokAramaZaman = setTimeout(() => {
    stokSayfa.value = 0
    stoklariYukle()
  }, 300)
}

const kritikAdet = computed(() => stokStore.stoklar.filter((s) => s.minMiktar && s.miktar <= s.minMiktar).length)

onMounted(async () => {
  await Promise.all([stoklariYukle(), cariHesapStore.getAllCariHesaplar()])
})

const filtreTemizle = () => {
  filtreArama.value = ''
  filtreKategori.value = ''
  filtreMarka.value = ''
  filtreStokGrubu.value = ''
  filtreMinFiyat.value = null
  filtreMaxFiyat.value = null
  stokSayfa.value = 0
  stoklariYukle()
}

const stokSec = async (s) => {
  seciliStok.value = s
  seciliStokId.value = s.id
  try {
    const r = await stokAPI.getHareketler(s.id)
    stokHareketler.value = r.data
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('stoklar.hareketYuklenemedi'))
  }
  detailStok.value = s
  showDetailDialog.value = true
  stokHareketleriYukle(s.id)
}

const openDialog = () => {
  editingId.value = null
  form.value = {
    stokKodu: '',
    barkod: '',
    ad: '',
    birim: '',
    birim2: '',
    cevrimKatsayisi: null,
    marka: '',
    stokGrubu: '',
    kategori: '',
    rafNo: '',
    fiyat: 0,
    satisFiyati: null,
    kdvOrani: null,
    agirlik: null,
    miktar: 0,
    minMiktar: null,
    tedarikciId: null,
    tedarikciStokKodu: '',
    tedarikciFiyat: null,
    maliyetYontemi: 'ORTALAMA',
    aciklama: '',
    fiyatlar: [
      { ad: 'Perakende', fiyat: 0 },
      { ad: 'Toptan', fiyat: 0 },
      { ad: 'Kurumsal', fiyat: 0 }
    ]
  }
  formTemizle()
  showDialog.value = true
}

const editStok = (s) => {
  editingId.value = s.id
  form.value = {
    stokKodu: s.stokKodu || '',
    barkod: s.barkod || '',
    ad: s.ad,
    birim: s.birim || '',
    birim2: s.birim2 || '',
    cevrimKatsayisi: s.cevrimKatsayisi || null,
    marka: s.marka || '',
    stokGrubu: s.stokGrubu || '',
    kategori: s.kategori || '',
    rafNo: s.rafNo || '',
    fiyat: s.fiyat,
    satisFiyati: s.satisFiyati,
    kdvOrani: s.kdvOrani,
    agirlik: s.agirlik,
    miktar: s.miktar,
    minMiktar: s.minMiktar,
    tedarikciId: s.tedarikciId || null,
    tedarikciStokKodu: s.tedarikciStokKodu || '',
    tedarikciFiyat: s.tedarikciFiyat || null,
    maliyetYontemi: s.maliyetYontemi || 'ORTALAMA',
    aciklama: s.aciklama || '',
    fotoUrl: s.fotoUrl || '',
    fiyatlar: (s.fiyatlar || []).map((f) => ({ ...f }))
  }
  formTemizle()
  showDialog.value = true
}

const fiyatEkle = () => {
  form.value.fiyatlar.push({ ad: '', fiyat: 0 })
}

const fiyatSil = async (f) => {
  if (f.id) {
    try {
      await stokAPI.fiyatSil(f.id)
      toastBildirim.basarili(t('stoklar.fiyatSilindi'))
    } catch (err) {
      toastBildirim.hata(err?.response?.data?.message || t('stoklar.fiyatSilinemedi'))
      return
    }
  }
  form.value.fiyatlar = form.value.fiyatlar.filter((x) => x !== f)
}

const saveStok = async () => {
  if (!form.value.ad.trim()) {
    toastBildirim.uyari('Ürün adı giriniz')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await stokStore.updateStok(editingId.value, form.value)
      await fiyatlariKaydet(editingId.value)
      toastBildirim.basarili(t('stoklar.urunGuncellendi'))
    } else {
      const yeniStok = await stokStore.addStok(form.value)
      // Yeni stokun ID'siyle fiyatları kaydet
      if (yeniStok?.id) {
        await fiyatlariKaydet(yeniStok.id)
      }
      toastBildirim.basarili(t('stoklar.urunEklendi'))
    }
    formTemizle()
    showDialog.value = false
    await stokStore.getAll()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('stoklar.islemBasarisiz'))
  } finally {
    saving.value = false
  }
}

const fiyatlariKaydet = async (stokId) => {
  for (const f of form.value.fiyatlar) {
    if (!f.ad || !f.ad.trim() || f.fiyat == null || f.fiyat <= 0) continue
    if (f.id) {
      await stokAPI.fiyatGuncelle(f.id, { ad: f.ad, fiyat: f.fiyat })
    } else {
      await stokAPI.fiyatEkle(stokId, { ad: f.ad, fiyat: f.fiyat })
    }
  }
}

const fotoSec = async (e) => {
  const file = e.target.files[0]
  if (!file) return
  try {
    const r = await uploadAPI.foto(file)
    form.value.fotoUrl = r.data?.url || ''
    toastBildirim.basarili(t('stoklar.fotografYuklendi'))
  } catch (err) {
    toastBildirim.hata(t('stoklar.fotografYuklenemedi'))
  }
}

const confirmDel = (id) => {
  const silinecek = stokStore.stoklar.find((s) => s.id === id)
  confirm.require({
    message: t('stoklar.silOnayMesaj'),
    header: t('stoklar.onay'),
    icon: 'pi pi-exclamation-triangle',
    accept: async () => {
      try {
        await stokStore.deleteStok(id)
        if (seciliStokId.value === id) {
          seciliStok.value = null
          seciliStokId.value = null
          stokHareketler.value = []
        }
        toastBildirim.basarili(t('stoklar.urunSilindi'))
        if (silinecek)
          silVeGeriAl({ veri: silinecek, metin: `${silinecek.ad} silindi`, geriYukle: (v) => stokStore.addStok(v) })
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || err?.message || t('stoklar.silmeBasarisiz'))
      }
    }
  })
}

const openHareketDialog = (tur) => {
  hareketTur.value = tur
  hareketForm.value = { miktar: null, hareketTarihi: new Date(), cariHesapId: null, aciklama: '' }
  showHareketDialog.value = true
}

const batchSil = () => {
  if (!seciliStoklar.value.length) return
  confirm.require({
    message: t('stoklar.topluSilOnayMesaj', { n: seciliStoklar.value.length }),
    header: t('stoklar.topluSilOnay'),
    icon: 'pi pi-exclamation-triangle',
    accept: async () => {
      let basarili = 0,
        hatali = 0
      for (const s of [...seciliStoklar.value]) {
        try {
          await stokStore.deleteStok(s.id)
          basarili++
        } catch {
          hatali++
        }
      }
      seciliStoklar.value = []
      toast.add({
        severity: hatali ? 'warn' : 'success',
        summary: t('stoklar.tamamlandi'),
        detail: `${basarili} ${t('stoklar.silindi')}${hatali ? ', ' + hatali + ' ' + t('stoklar.hata') : ''}`,
        life: 5000
      })
    }
  })
}

const batchCsvExport = () => {
  if (!seciliStoklar.value.length) return
  const kolonlar = ['ad', 'stokKodu', 'barkod', 'birim', 'fiyat', 'miktar', 'minMiktar']
  const baslik = kolonlar.join(';')
  const satirlar = seciliStoklar.value.map((s) => kolonlar.map((k) => s[k] ?? '').join(';'))
  const csv = '\uFEFF' + [baslik, ...satirlar].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.setAttribute('download', `stoklar-${new Date().toISOString().split('T')[0]}.csv`)
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

const saveHareket = async () => {
  if (!hareketForm.value.miktar || hareketForm.value.miktar <= 0) {
    toastBildirim.uyari('Geçerli miktar giriniz')
    return
  }
  saving.value = true
  try {
    await stokAPI.addHareket(seciliStokId.value, {
      tur: hareketTur.value,
      miktar: hareketForm.value.miktar,
      hareketTarihi: hareketForm.value.hareketTarihi.toISOString().split('T')[0],
      cariHesapId: hareketForm.value.cariHesapId,
      aciklama: hareketForm.value.aciklama
    })
    const [hr, sr] = await Promise.all([stokAPI.getHareketler(seciliStokId.value), stokStore.getAll({ size: 1000 })])
    stokHareketler.value = hr.data
    seciliStok.value = sr.find((s) => s.id === seciliStokId.value)
    showHareketDialog.value = false
    toastBildirim.basarili(t('stoklar.hareketEklendi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('stoklar.islemBasarisiz'))
  } finally {
    saving.value = false
  }
}

const delHareket = async (id) => {
  try {
    await stokAPI.deleteHareket(id)
    const [hr, sr] = await Promise.all([stokAPI.getHareketler(seciliStokId.value), stokStore.getAll({ size: 1000 })])
    stokHareketler.value = hr.data
    seciliStok.value = sr.find((s) => s.id === seciliStokId.value)
    toastBildirim.basarili(t('stoklar.hareketSilindi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('stoklar.silmeBasarisiz'))
  }
}

const batchFiyatDialog = ref(false)
const batchLoading = ref(false)
const batchFiyatForm = ref({ oran: 0, yon: 'ARTIR', kategori: '', stokGrubu: '' })

const batchFiyatUygula = async () => {
  if (!batchFiyatForm.value.oran || batchFiyatForm.value.oran <= 0) {
    toastBildirim.uyari('Lütfen 0\'dan büyük geçerli bir oran girin.')
    return
  }
  batchLoading.value = true
  try {
    const r = await stokAPI.topluFiyatGuncelle({
      kategori: batchFiyatForm.value.kategori || null,
      stokGrubu: batchFiyatForm.value.stokGrubu || null,
      marka: null,
      yon: batchFiyatForm.value.yon,
      oran: batchFiyatForm.value.oran
    })
    const guncellenen = r.data?.etkilenenStokSayisi || r.data?.guncellenen || 0
    await stokStore.getAll({ size: 1000 })
    batchFiyatDialog.value = false
    toastBildirim.basarili(t('stoklar.fiyatGuncellendi', { n: guncellenen }))
  } catch (e) {
    toastBildirim.hata(e.response?.data?.message || t('stoklar.topluFiyatBasarisiz'))
  } finally {
    batchLoading.value = false
  }
}

const excelIndir = async () => {
  try {
    const res = await excelAPI.stoklar()
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', 'Stoklar.xlsx')
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch {
    /* silent */
  }
}

const stokHareketleriYukle = async (stokId) => {
  hareketlerYukleniyor.value = true
  hareketler.value = []
  try {
    const r = await stokAPI.getHareketler(stokId)
    hareketler.value = r.data
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('stoklar.hareketYuklenemedi'))
  } finally {
    hareketlerYukleniyor.value = false
  }
}
</script>

<style scoped>
.foto-satir {
  display: flex;
  align-items: center;
  gap: 10px;
}
.foto-onizle {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid var(--border);
}

.stoklar-container {
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
h2 {
  color: var(--text-primary);
  font-size: 20px;
  margin: 0;
}
.toolbar {
  margin-bottom: 20px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px 18px;
}
.toolbar-end {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  min-width: 0;
}
.table-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 0;
}
.toplam-bilgi {
  color: var(--text-secondary);
  font-size: 13px;
}
.kritik-bilgi {
  color: #f87171;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.loading {
  text-align: center;
  padding: 40px;
  color: var(--text-secondary);
}
.stok-kartlar {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(280px, 100%), 1fr));
  gap: 15px;
  margin-bottom: 30px;
}
.form-grup {
  margin-bottom: 18px;
}
.coklu-fiyat-bolumu {
  border-top: 1px solid var(--border);
  padding-top: 14px;
}
.coklu-fiyat-baslik {
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.coklu-fiyat-ipucu {
  font-size: 11px;
  color: var(--text-muted);
  text-transform: none;
  letter-spacing: 0;
  font-weight: 400;
}
.coklu-fiyat-satir {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.fiyat-ad-input {
  flex: 1;
}
.fiyat-tutar-input {
  width: 160px;
}
.form-grup label {
  display: block;
  margin-bottom: 6px;
  font-weight: 600;
  color: var(--text-secondary);
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.form-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 15px;
}
.form-row .flex-2 {
  grid-column: span 2;
}
.form-section {
  margin-bottom: 24px;
}
.form-section:last-child {
  margin-bottom: 0;
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
.w-full {
  width: 100% !important;
}
.full-width {
  grid-column: 1/-1;
}
.filter-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}
.filter-input {
  width: min(150px, 100%) !important;
}
.filter-input-sm {
  width: min(120px, 100%) !important;
}
.filter-dropdown {
  width: min(160px, 100%) !important;
}
.batch-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-left: 12px;
  padding-left: 12px;
  border-left: 1px solid var(--border);
}
.batch-count {
  font-size: 12px;
  color: #60a5fa;
  font-weight: 600;
}
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
</style>
