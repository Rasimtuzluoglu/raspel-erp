<template>
  <div class="faturalar-container">
    <h1>{{ t('faturalar.title') }}</h1>

    <IlkZiyaretIpuclari
      anahtar="faturalar"
      :baslik="t('faturalar.ipucuBaslik')"
      :metin="t('faturalar.ipucuMetin')"
    />

    <Toolbar class="toolbar">
      <template #start>
        <Button
          :label="t('faturalar.yeniFatura')"
          icon="pi pi-plus"
          class="p-button-success"
          @click="openCreateDialog"
        />
        <span
          class="p-input-icon-left arama-kutu"
        >
          <i class="pi pi-search" />
          <InputText
            v-model="arama"
            :placeholder="t('faturalar.aramaPlaceholder')"
            class="arama-input"
            @input="aramaDebounce"
          />
        </span>
      </template>
      <template #end>
        <TarihHizliSecim
          v-model="tarihAraligi"
          style="margin-right: 8px"
        />
        <Button
          label="Excel"
          icon="pi pi-file-excel"
          class="p-button-sm p-button-outlined"
          @click="excelIndir"
        />
      </template>
    </Toolbar>

    <div
      v-if="loading"
      class="loading"
    >
      <p><i class="pi pi-spin pi-spinner" /> Yükleniyor...</p>
    </div>

    <div
      v-if="!loading"
      class="table-container"
    >
      <div
        v-if="selectedItems && selectedItems.length > 0"
        class="batch-action-bar"
      >
        <div class="batch-info">
          <i class="pi pi-check-square" />
          <span><strong>{{ selectedItems ? selectedItems.length : 0 }}</strong> {{ t('faturalar.kayitSecildi') }}</span>
        </div>
        <div class="batch-buttons">
          <Button
            :label="t('faturalar.secilenleriSil')"
            icon="pi pi-trash"
            class="p-button-danger p-button-sm"
            :loading="topluSiliniyor"
            @click="topluSil()"
          />
          <Button
            :label="t('common.clearSelection')"
            icon="pi pi-times"
            class="p-button-text p-button-sm"
            @click="selectedItems = []"
          />
        </div>
      </div>
      <DataTable
        v-model:selection="selectedItems"
        selection-mode="multiple"
        :value="filtrelenmisFaturalar"
        responsive-layout="scroll"
        striped-rows
        :rows="10"
        :paginator="true"
        paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport RowsPerPageDropdown"
        :rows-per-page-options="[10, 20, 50]"
        current-page-report-template="{first} - {last} ({totalRecords} kayıt)"
      >
        <Column
          field="faturaNumarasi"
          :header="t('faturalar.colFaturaNo')"
          style="width: 160px"
        />
        <Column
          field="tarih"
          :header="t('common.date')"
          style="width: 110px"
        >
          <template #body="s">
            {{ formatDate(s.data.tarih) }}
          </template>
        </Column>
        <Column
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
          field="cariHesapAd"
          :header="t('faturalar.colCari')"
          style="width: 180px"
        >
          <template #body="s">
            {{ s.data.cariHesapAd || '-' }}
          </template>
        </Column>
        <Column
          field="genelToplam"
          :header="t('faturalar.colToplam')"
          style="width: 130px"
        >
          <template #body="s">
            {{ formatCurrency(s.data.genelToplam) }}
          </template>
        </Column>
        <Column
          field="durum"
          :header="t('common.status')"
          style="width: 110px"
        >
          <template #body="s">
            <span :class="['durum-badge', (s.data.durum || '').toLowerCase()]">
              {{ durumLabel(s.data.durum) }}
            </span>
          </template>
        </Column>
        <Column
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
          :header="t('common.actions')"
          style="width: 310px"
        >
          <template #body="s">
            <Button
              icon="pi pi-eye"
              class="p-button-rounded p-button-sm p-button-info"
              :title="t('faturalar.goruntule')"
              @click="viewFatura(s.data.id)"
            />
            <Button
              icon="pi pi-print"
              class="p-button-rounded p-button-sm p-button-secondary"
              :title="t('faturalar.yazdirTasarla')"
              @click="tasarlaVeYazdir(s.data.id)"
            />
            <Button
              icon="pi pi-download"
              class="p-button-rounded p-button-sm p-button-help"
              :title="t('faturalar.pdfIndir')"
              @click="pdfIndir(s.data)"
            />
            <Button
              icon="pi pi-whatsapp"
              class="p-button-rounded p-button-sm p-button-success"
              :title="t('faturalar.whatsapp')"
              style="background: #25d366; border-color: #25d366"
              @click="whatsappGonder(s.data)"
            />
            <Button
              icon="pi pi-copy"
              class="p-button-rounded p-button-sm p-button-secondary"
              :title="t('common.duplicate')"
              @click="cogalt(s.data)"
            />
            <Button
              v-if="s.data.durum === 'TASLAK' || s.data.durum === 'KESILDI'"
              icon="pi pi-pencil"
              class="p-button-rounded p-button-sm p-button-warning"
              :title="s.data.durum === 'KESILDI' ? t('faturalar.revize') : t('common.edit')"
              @click="editFatura(s.data)"
            />
            <Button
              v-if="s.data.durum === 'TASLAK'"
              icon="pi pi-check"
              class="p-button-rounded p-button-sm p-button-success"
              :title="t('faturalar.kes')"
              @click="confirmKes(s.data.id)"
            />
            <Button
              v-if="s.data.durum !== 'IPTAL'"
              icon="pi pi-ban"
              class="p-button-rounded p-button-sm p-button-danger"
              :title="t('common.cancel')"
              @click="confirmIptal(s.data.id)"
            />
          </template>
        </Column>
      </DataTable>
      <EmptyState
        v-if="filtrelenmisFaturalar && filtrelenmisFaturalar.length === 0"
        :message="t('faturalar.empty')"
        :sub-message="t('faturalar.emptyHint')"
        icon="pi pi-file"
        :action-label="t('faturalar.emptyAction')"
        action-icon="pi pi-plus"
        @action="openCreateDialog"
      />
    </div>

    <Dialog
      v-model:visible="showDialog"
      :header="dialogBaslik"
      :modal="true"
      style="width: 920px; max-width: 96vw"
      :closable="false"
    >
      <div class="form-grid">
        <div class="form-group">
          <label>{{ form.tur === 'ALIS' ? t('faturalar.tedarikci') : t('faturalar.musteri') }}</label>
          <AutoComplete
            v-model="seciliCariNesnesi"
            :suggestions="cariOnerileri"
            option-label="ad"
            option-value="id"
            :placeholder="
              form.tur === 'ALIS'
                ? t('faturalar.tedarikciAra')
                : t('faturalar.cariAra')
            "
            class="w-full"
            :force-selection="false"
            @complete="cariAra($event)"
            @option-select="cariSecildi"
          >
            <template #option="slotProps">
              <div class="cari-opsiyon">
                <span>{{ slotProps.option.ad }}</span>
                <span class="cari-opsiyon-detay">{{
                  slotProps.option.vergiNumarasi || slotProps.option.telefon || ''
                }}</span>
              </div>
            </template>
          </AutoComplete>
          <Button
            v-if="form.cariHesapId"
            :label="t('faturalar.sonFaturaKopyala')"
            icon="pi pi-copy"
            class="p-button-text p-button-sm son-fatura-kopyala"
            :loading="sonFaturaYukleniyor"
            @click="sonFaturayiKopyala"
          />
        </div>
        <div class="form-group">
          <label>{{ t('faturalar.faturaTuru') }}</label>
          <Dropdown
            v-model="form.tur"
            :options="turSecenekler"
            option-label="label"
            option-value="value"
            :placeholder="t('faturalar.seciniz')"
            class="w-full"
          />
        </div>
        <div
          v-if="form.tur === 'ALIS'"
          class="form-group"
        >
          <label>{{ t('faturalar.girisDeposu') }}</label>
          <Dropdown
            v-model="form.depoId"
            :options="depolar"
            option-label="ad"
            option-value="id"
            :placeholder="t('faturalar.depoSecin')"
            class="w-full"
            :show-clear="true"
          />
        </div>
        <div class="form-group">
          <label>{{ t('faturalar.tarihZorunlu') }}</label>
          <DatePicker
            v-model="form.tarih"
            date-format="dd.mm.yy"
            class="w-full"
          />
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>{{ t('faturalar.paraBirimi') }}</label>
            <Dropdown
              v-model="form.paraBirimi"
              :options="['TRY', 'USD', 'EUR', 'GBP', 'SAR', 'GAU']"
              class="w-full"
              placeholder="TRY (₺)"
            />
          </div>
          <div
            v-if="form.paraBirimi && form.paraBirimi !== 'TRY'"
            class="form-group"
          >
            <label>{{ t('faturalar.kurBilgisi') }}</label>
            <div class="kur-bilgi-box">
              1 {{ form.paraBirimi }} =
              {{
                dovizStore.formatPara(
                  dovizStore.getKur(form.paraBirimi).satisFiyati || dovizStore.getKur(form.paraBirimi).satisKuru,
                  'TRY'
                )
              }}
            </div>
          </div>
        </div>
        <div class="form-group">
          <label>{{ t('faturalar.teslimEdenPersonel') }}</label>
          <div class="teslim-eden-grup">
            <Dropdown
              v-model="form.teslimEden"
              :options="personelSecenekleri"
              option-label="label"
              option-value="value"
              filter
              editable
              :placeholder="t('faturalar.teslimEdenPlaceholder')"
              class="w-full"
              :show-clear="true"
            >
              <template #option="s">
                <div class="personel-opsiyon">
                  <i class="pi pi-truck" />
                  <span>{{ s.option.label }}</span>
                </div>
              </template>
            </Dropdown>
          </div>
        </div>
        <div class="form-group">
          <label>{{ t('faturalar.teslimDurumu') }}</label>
          <Dropdown
            v-model="form.teslimDurumu"
            :options="teslimDurumSecenekleri"
            option-label="label"
            option-value="value"
            class="w-full"
          />
        </div>
        <div class="form-group">
          <label>{{ t('faturalar.teslimNotu') }}</label>
          <Textarea
            v-model="form.teslimNotu"
            rows="2"
            :placeholder="t('faturalar.teslimNotuPlaceholder')"
            class="w-full"
          />
        </div>
        <div class="form-group">
          <label>{{ t('common.description') }}</label>
          <Textarea
            v-model="form.aciklama"
            rows="2"
            :placeholder="t('faturalar.istegeBagli')"
            class="w-full"
          />
        </div>

        <template v-if="form.tur === 'SATIS'">
          <div
            class="form-group teslimat-baslik"
            style="grid-column: 1 / -1; margin-top: 6px"
          >
            <label style="font-weight: 700; color: var(--text-primary)">
              <i
                class="pi pi-truck"
                style="margin-right: 6px"
              /> {{ t('faturalar.teslimatBilgileri') }}
            </label>
          </div>
          <div class="form-group">
            <label>{{ t('faturalar.sofor') }}</label>
            <Dropdown
              v-model="form.driverId"
              :options="suruculer"
              option-label="ad"
              option-value="id"
              :placeholder="t('faturalar.soforSecin')"
              class="w-full"
              :show-clear="true"
            />
          </div>
          <div class="form-group">
            <label>{{ t('faturalar.beklenenTeslimTarihi') }}</label>
            <DatePicker
              v-model="form.beklenenTeslimTarihi"
              date-format="dd.mm.yy"
              class="w-full"
            />
          </div>
          <div class="form-group">
            <label>{{ t('faturalar.teslimatAdresi') }}</label>
            <Textarea
              v-model="form.teslimatAdresi"
              rows="2"
              :placeholder="t('faturalar.adresPlaceholder')"
              class="w-full"
            />
          </div>
          <div class="form-group">
            <label>{{ t('faturalar.teslimNotu') }}</label>
            <Textarea
              v-model="form.teslimatNotu"
              rows="2"
              :placeholder="t('faturalar.istegeBagli')"
              class="w-full"
            />
          </div>
        </template>
      </div>

      <div class="urun-ekleme">
        <div
          class="form-row"
          style="display: flex; gap: 10px; align-items: flex-end"
        >
          <div
            class="form-group"
            style="flex: 3; margin: 0"
          >
            <label>{{ t('faturalar.urunSec') }}</label>
            <Dropdown
              v-model="urunSecimi"
              :options="stokStore.stoklar"
              filter
              option-label="ad"
              option-value="id"
              :placeholder="t('faturalar.urunAra')"
              class="w-full"
              @change="urunSecildi"
            >
              <template #option="s">
                <div style="display: flex; align-items: center; gap: 10px">
                  <span style="flex: 1; color: #f1f5f9">{{ s.option.ad }}</span>
                  <span
                    v-if="kritikStokMu(s.option)"
                    class="kitlik-rozeti"
                    :title="t('faturalar.kritikStokTitle', { n: s.option.minMiktar || 0 })"
                  >{{ t('faturalar.sonAdet', { n: Math.floor(s.option.miktar) }) }}</span>
                  <span style="color: #4ade80; font-size: 12px; font-weight: 600">{{ s.option.miktar }} {{ s.option.birim || 'Adet' }}</span>
                  <span style="color: #94a3b8; font-size: 12px">{{ formatCurrency(s.option.fiyat) }}</span>
                </div>
              </template>
            </Dropdown>
          </div>
          <div
            class="form-group"
            style="flex: 1; margin: 0"
          >
            <label>{{ t('faturalar.miktar') }}</label>
            <InputNumber
              v-model="urunAdet"
              :min="1"
              class="w-full"
            />
          </div>
          <Button
            icon="pi pi-plus"
            class="p-button-success"
            style="margin-bottom: 2px"
            :disabled="!urunSecimi || !urunAdet"
            @click="urunEkleKalem"
          />
        </div>
      </div>

      <FaturaFiyatGecmisi
        v-if="fiyatGecmisi && fiyatGecmisi.gecmis && fiyatGecmisi.gecmis.length"
        :fiyat-gecmisi="fiyatGecmisi"
      />

      <FaturaSonUrunler
        v-if="form.cariHesapId && !cariSonUrunlerGizle && cariSonUrunler.length > 0"
        :urunler="cariSonUrunler"
        @ekle="sonUrunuEkle"
        @gizle="cariSonUrunlerGizle = true"
      />

      <h3 style="margin: 20px 0 10px">
        {{ t('faturalar.faturaKalemleri') }}
      </h3>
      <FaturaKalemleri
        :kalemler="form.kalemler"
        :ara-toplam="araToplam"
        :kdv-toplam="kdvToplam"
        :genel-toplam="genelToplam"
        @add="addKalem"
        @remove="removeKalem"
      />

      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="closeDialog"
        />
        <Button
          :label="editingId ? t('faturalar.guncelle') : t('faturalar.olustur')"
          icon="pi pi-check"
          :loading="saving"
          @click="saveFatura"
        />
      </template>
    </Dialog>

    <Message
      v-if="faturaStore.error"
      severity="error"
      :text="faturaStore.error"
    />

    <FaturaTasarimModal
      v-model:visible="tasarimModalAcik"
      :fatura-id="seciliFaturaId"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { useI18n } from 'vue-i18n'
import { useFaturaStore } from '../stores/faturaStore.js'
import { useCariHesapStore } from '../stores/cariHesapStore.js'
import { useStokStore } from '../stores/stokStore.js'
import { useDovizStore } from '../stores/dovizStore.js'

const dovizStore = useDovizStore()
import { faturaAPI, excelAPI, pdfAPI, personelAPI, depoAPI, teslimatAPI } from '../api/index.js'
import { useKisayollar } from '../composables/useKisayollar.js'
import { useTaslakKayit } from '../composables/useTaslakKayit.js'
import { useFormKorumasi } from '../composables/useFormKorumasi.js'
import TarihHizliSecim from '../components/TarihHizliSecim.vue'
import IlkZiyaretIpuclari from '../components/IlkZiyaretIpuclari.vue'
import FaturaTasarimModal from '../components/FaturaTasarimModal.vue'
import FaturaKalemleri from '../components/FaturaKalemleri.vue'
import FaturaFiyatGecmisi from '../components/FaturaFiyatGecmisi.vue'
import FaturaSonUrunler from '../components/FaturaSonUrunler.vue'
import { formatCurrency } from '../utils/format.js'
import { kalemNetTutar, kalemKdv } from '../utils/faturaHesapla.js'

const router = useRouter()
const route = useRoute()
const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { t } = useI18n()
const faturaStore = useFaturaStore()

const tasarimModalAcik = ref(false)
const seciliFaturaId = ref(null)

const tasarlaVeYazdir = (id) => {
  seciliFaturaId.value = id
  tasarimModalAcik.value = true
}
const cariHesapStore = useCariHesapStore()
const stokStore = useStokStore()

useKisayollar({
  yeni: () => openCreateDialog(),
  iptal: () => {
    showDialog.value = false
  },
  kaydet: () => saveFatura()
})

const showDialog = ref(false)
const loading = ref(false)
const saving = ref(false)
const editingId = ref(null)
const tarihAraligi = ref(null)
const selectedItems = ref([])
const topluSiliniyor = ref(false)
const arama = ref('')
let aramaZamanlayici = null

const aramaDebounce = () => {
  if (aramaZamanlayici) clearTimeout(aramaZamanlayici)
  aramaZamanlayici = setTimeout(async () => {
    loading.value = true
    try {
      await faturaStore.getAllFaturalar(arama.value.trim() || undefined)
    } catch {
      /* toast yok */
    }
    loading.value = false
  }, 300)
}

const turSecenekler = computed(() => [
  { label: t('faturalar.satis'), value: 'SATIS' },
  { label: t('faturalar.alis'), value: 'ALIS' }
])

const form = ref({
  cariHesapId: null,
  tur: '',
  tarih: new Date(),
  teslimEden: '',
  teslimDurumu: 'BEKLIYOR',
  teslimNotu: '',
  depoId: null,
  paraBirimi: 'TRY',
  aciklama: '',
  driverId: null,
  beklenenTeslimTarihi: null,
  teslimatAdresi: '',
  teslimatNotu: '',
  kalemler: []
})

const depolar = ref([])
const suruculer = ref([])

const teslimDurumSecenekleri = computed(() => [
  { label: t('faturalar.durumBekliyor'), value: 'BEKLIYOR' },
  { label: t('faturalar.durumYolda'), value: 'YOLDA' },
  { label: t('faturalar.durumTeslimEdildi'), value: 'TESLIM_EDILDI' }
])

const urunSecimi = ref(null)
const urunAdet = ref(1)

const dialogBaslik = computed(() => (editingId.value ? t('faturalar.dialogDuzenle') : t('faturalar.dialogYeni')))

const filtrelenmisFaturalar = computed(() => {
  if (!tarihAraligi.value || tarihAraligi.value.length !== 2 || !tarihAraligi.value[0]) {
    return faturaStore.faturalar
  }
  const bas = new Date(tarihAraligi.value[0])
  bas.setHours(0, 0, 0, 0)
  const bit = new Date(tarihAraligi.value[1])
  bit.setHours(23, 59, 59, 999)
  return faturaStore.faturalar.filter((f) => {
    if (!f.tarih) return false
    const t = new Date(f.tarih)
    return t >= bas && t <= bit
  })
})

const { temizle: taslakTemizle } = useTaslakKayit('fatura', form, {
  onRestore: () => {
    toast.add({
      severity: 'info',
      summary: t('faturalar.taslakGeriYuklendi'),
      detail: t('faturalar.taslakGeriYuklendiDetay'),
      life: 5000
    })
  }
})

const { temizle: formTemizle } = useFormKorumasi(form)

onMounted(async () => {
  loading.value = true
  try {
    await Promise.all([
      faturaStore.getAllFaturalar(),
      cariHesapStore.getAllCariHesaplar(),
      stokStore.getAll(),
      personelListesiniYukle(),
      depolarıYukle(),
      suruculeriYukle()
    ])
  } catch (err) {
    toastBildirim.hata(t('faturalar.hataYukleme'))
  } finally {
    loading.value = false
  }
  // Cari ekranından gelen "Yeni Fatura" kısayolu: cariId query'si varsa cariyi seçip dialog aç
  if (route.query.cariId) {
    const cariId = Number(route.query.cariId)
    const cari = cariHesapStore?.cariHesaplar?.find((c) => c.id === cariId)
    if (cari) {
      seciliCariNesnesi.value = cari
      openCreateDialog()
      form.value.cariHesapId = cariId
    }
  }
})

const personelListesi = ref([])

const depolarıYukle = async () => {
  try {
    const r = await depoAPI.getAll({ size: 500 })
    depolar.value = r.data?.content || r.data || []
  } catch {
    depolar.value = []
  }
}

const suruculeriYukle = async () => {
  try {
    const r = await teslimatAPI.suruculer()
    suruculer.value = r.data || []
  } catch {
    suruculer.value = []
  }
}

const personelSecenekleri = computed(() =>
  personelListesi.value
    .filter((p) => p.aktif !== false)
    .map((p) => ({ label: `${p.ad || ''} ${p.soyad || ''}`.trim(), value: `${p.ad || ''} ${p.soyad || ''}`.trim() }))
)

const personelListesiniYukle = async () => {
  try {
    const r = await personelAPI.getAll({ size: 500 })
    personelListesi.value = r.data?.content || r.data || []
  } catch {
    personelListesi.value = []
  }
}

const addKalem = () => {
  form.value.kalemler.push({ aciklama: '', adet: 1, birimFiyat: 0, iskontoOrani: 0, kdvOrani: 20 })
}

const urunSecildi = () => {
  if (!urunSecimi.value) return
  const u = stokStore.stoklar.find((s) => s.id === urunSecimi.value)
  if (u) urunAdet.value = 1
  fiyatGecmisiYukle(urunSecimi.value)
}

const fiyatGecmisi = ref(null)
const fiyatGecmisiYukleniyor = ref(false)

const fiyatGecmisiYukle = async (stokId) => {
  if (!stokId) {
    fiyatGecmisi.value = null
    return
  }
  fiyatGecmisiYukleniyor.value = true
  try {
    const r = await faturaAPI.stokFiyatGecmisi(stokId)
    fiyatGecmisi.value = r.data
  } catch {
    fiyatGecmisi.value = null
  } finally {
    fiyatGecmisiYukleniyor.value = false
  }
}

const kritikStokMu = (stok) => {
  if (!stok?.miktar) return false
  if (stok.minMiktar != null && stok.miktar <= stok.minMiktar) return true
  return stok.miktar <= 10
}

const urunEkleKalem = () => {
  if (!urunSecimi.value || !urunAdet.value) return
  const u = stokStore.stoklar.find((s) => s.id === urunSecimi.value)
  if (!u) return
  form.value.kalemler.push({
    aciklama: u.ad,
    adet: urunAdet.value,
    birimFiyat: u.fiyat,
    iskontoOrani: 0,
    kdvOrani: 20,
    stokId: u.id
  })
  urunSecimi.value = null
  urunAdet.value = 1
}

const seciliCariNesnesi = ref(null)
const cariOnerileri = ref([])

const cariAra = (event) => {
  const q = (event.query || '').toLowerCase().trim()
  const kaynak = cariHesapStore?.cariHesaplar || []
  if (!q) {
    cariOnerileri.value = kaynak.slice(0, 20)
    return
  }
  cariOnerileri.value = kaynak
    .filter(
      (c) =>
        c.ad?.toLowerCase().includes(q) ||
        c.vergiNumarasi?.toLowerCase().includes(q) ||
        c.telefon?.toLowerCase().includes(q)
    )
    .slice(0, 20)
}

const cariSecildi = (event) => {
  form.value.cariHesapId = event.value?.id || null
  const adres = event.value?.adres || ''
  if (adres && !form.value.teslimatAdresi) {
    form.value.teslimatAdresi = adres
  }
}

const cariSonUrunler = ref([])
const cariSonUrunlerGizle = ref(false)
let cariSonUrunlerZamanlayici = null

watch(
  () => form.value.cariHesapId,
  (yeniCariId) => {
    cariSonUrunlerGizle.value = false
    if (cariSonUrunlerZamanlayici) clearTimeout(cariSonUrunlerZamanlayici)
    if (!yeniCariId) {
      cariSonUrunler.value = []
      return
    }
    // Debounce - kullanici cari secerken istek yagmasi
    cariSonUrunlerZamanlayici = setTimeout(async () => {
      try {
        const r = await faturaAPI.cariSonUrunler(yeniCariId, 10)
        cariSonUrunler.value = r.data || []
      } catch {
        cariSonUrunler.value = []
      }
    }, 400)
  }
)

const sonUrunuEkle = (urun) => {
  if (!urun) return
  const u = urun.stokId ? stokStore.stoklar.find((s) => s.id === urun.stokId) : null
  form.value.kalemler.push({
    aciklama: urun.stokAd || (u ? u.ad : 'Ürün'),
    adet: 1,
    birimFiyat: urun.sonBirimFiyat || (u ? u.fiyat : 0),
    iskontoOrani: 0,
    kdvOrani: 20,
    stokId: urun.stokId || null
  })
    toastBildirim.basarili(t('faturalar.kalemEklendi'))
}

const sonFaturaYukleniyor = ref(false)

const sonFaturayiKopyala = async () => {
  if (!form.value.cariHesapId) return
  sonFaturaYukleniyor.value = true
  try {
    const r = await faturaAPI.cariSonFatura(form.value.cariHesapId)
    if (!r.data) {
      toastBildirim.bilgi('Bu cariye ait daha önce fatura yok')
      return
    }
    const kaynak = r.data
    form.value.aciklama = kaynak.aciklama || ''
    form.value.tur = kaynak.tur || 'SATIS'
    form.value.kalemler = (kaynak.kalemler || []).map((k) => ({
      aciklama: k.aciklama || '',
      adet: k.adet || 1,
      birimFiyat: k.birimFiyat || 0,
      iskontoOrani: k.iskontoOrani || 0,
      kdvOrani: k.kdvOrani || 20,
      stokId: k.stokId || null
    }))
    if (form.value.kalemler.length === 0) {
      form.value.kalemler.push({ aciklama: '', adet: 1, birimFiyat: 0, kdvOrani: 20 })
    }
    toastBildirim.basarili(t('faturalar.sonFaturaKopyalandi'))
  } catch {
    toastBildirim.hata(t('faturalar.sonFaturaYuklenemedi'))
  } finally {
    sonFaturaYukleniyor.value = false
  }
}

const removeKalem = (index) => {
  form.value.kalemler.splice(index, 1)
}

const araToplam = computed(() => {
  return form.value.kalemler.reduce((t, k) => t + kalemNetTutar(k), 0)
})

const kdvToplam = computed(() => {
  return form.value.kalemler.reduce((t, k) => t + kalemKdv(k), 0)
})

const genelToplam = computed(() => araToplam.value + kdvToplam.value)

const whatsappGonder = (fatura) => {
  const cariAd = fatura.cariHesapAd || 'Müşterimiz'
  const tutar = fatura.genelToplam
    ? fatura.genelToplam.toLocaleString('tr-TR', { minimumFractionDigits: 2 }) + ' TL'
    : ''
  const mesaj = `Sayın ${cariAd},\n${fatura.faturaNumarasi || 'Fatura'} numaralı, ${tutar} tutarındaki faturanız düzenlenmiştir. Bilginize sunarız.\nRaspel ERP`
  const url = `https://api.whatsapp.com/send?text=${encodeURIComponent(mesaj)}`
  window.open(url, '_blank')
}

const openCreateDialog = () => {
  editingId.value = null
  seciliCariNesnesi.value = null
  form.value = {
    cariHesapId: null,
    tur: '',
    tarih: new Date(),
    teslimEden: '',
    teslimDurumu: 'BEKLIYOR',
    teslimNotu: '',
    depoId: null,
    paraBirimi: 'TRY',
    aciklama: '',
    driverId: null,
    beklenenTeslimTarihi: null,
    teslimatAdresi: '',
    teslimatNotu: '',
    kalemler: [{ aciklama: '', adet: 1, birimFiyat: 0, kdvOrani: 20 }]
  }
  formTemizle()
  showDialog.value = true
}

const editFatura = (fatura) => {
  editingId.value = fatura.id
  seciliCariNesnesi.value = cariHesapStore?.cariHesaplar?.find((c) => c.id === fatura.cariHesapId) || null
  form.value = {
    cariHesapId: fatura.cariHesapId,
    tur: fatura.tur,
    tarih: new Date(fatura.tarih),
    teslimEden: fatura.teslimEden || '',
    teslimDurumu: fatura.teslimDurumu || 'BEKLIYOR',
    teslimNotu: fatura.teslimNotu || '',
    depoId: fatura.depoId || null,
    paraBirimi: fatura.paraBirimi || 'TRY',
    aciklama: fatura.aciklama || '',
    driverId: null,
    beklenenTeslimTarihi: null,
    teslimatAdresi: '',
    teslimatNotu: '',
    kalemler: fatura.kalemler.map((k) => ({
      id: k.id,
      aciklama: k.aciklama,
      adet: k.adet,
      birimFiyat: k.birimFiyat,
      iskontoOrani: k.iskontoOrani || 0,
      kdvOrani: k.kdvOrani,
      stokId: k.stokId || null
    }))
  }
  formTemizle()
  showDialog.value = true
}

const cogalt = (fatura) => {
  editingId.value = null
  form.value = {
    cariHesapId: fatura.cariHesapId,
    tur: fatura.tur,
    tarih: new Date(fatura.tarih),
    teslimEden: fatura.teslimEden || '',
    teslimDurumu: fatura.teslimDurumu || 'BEKLIYOR',
    teslimNotu: fatura.teslimNotu || '',
    paraBirimi: fatura.paraBirimi || 'TRY',
    depoId: fatura.depoId || null,
    aciklama: fatura.aciklama || '',
    driverId: null,
    beklenenTeslimTarihi: null,
    teslimatAdresi: '',
    teslimatNotu: '',
    kalemler: fatura.kalemler.map((k) => ({
      aciklama: k.aciklama,
      adet: k.adet,
      birimFiyat: k.birimFiyat,
      iskontoOrani: k.iskontoOrani || 0,
      kdvOrani: k.kdvOrani,
      stokId: k.stokId || null
    }))
  }
  formTemizle()
  showDialog.value = true
    toastBildirim.basarili(t('faturalar.faturaCogaltildi'))
}

const closeDialog = () => {
  showDialog.value = false
}

const saveFatura = async () => {
  if (!form.value.tur) {
    toastBildirim.uyari('Fatura türü seçiniz')
    return
  }
  const gecersiz = form.value.kalemler.some((k) => !k.aciklama.trim() || !k.adet || !k.birimFiyat)
  if (gecersiz) {
    toastBildirim.uyari('Tüm kalemleri eksiksiz doldurun')
    return
  }

  const payload = {
    cariHesapId: form.value.cariHesapId,
    tur: form.value.tur,
    tarih: form.value.tarih ? form.value.tarih.toISOString().split('T')[0] : null,
    teslimEden: form.value.teslimEden || null,
    teslimDurumu: form.value.teslimDurumu || 'BEKLIYOR',
    teslimNotu: form.value.teslimNotu || null,
    depoId: form.value.depoId || null,
    paraBirimi: form.value.paraBirimi || 'TRY',
    aciklama: form.value.aciklama,
    genelIskontoTutari: 0,
    odenenTutar: 0,
    odemeDurumu: 'ODENMEDI',
    kalemler: form.value.kalemler.map((k) => ({
      id: k.id || null,
      aciklama: k.aciklama,
      adet: k.adet,
      birimFiyat: k.birimFiyat,
      iskontoOrani: k.iskontoOrani || 0,
      kdvOrani: k.kdvOrani || 0,
      stokId: k.stokId || null
    }))
  }

  saving.value = true
  try {
    if (editingId.value) {
      await faturaStore.updateFatura(editingId.value, payload)
      toastBildirim.basarili(t('faturalar.faturaGuncellendi'))
    } else {
      const yeni = await faturaStore.addFatura(payload)
      toastBildirim.basarili(t('faturalar.faturaOlusturuldu'))
      if (form.value.driverId && form.value.teslimatAdresi?.trim()) {
        try {
          await teslimatAPI.olustur({
            faturaId: yeni.id,
            driverId: form.value.driverId,
            teslimatAdresi: form.value.teslimatAdresi,
            beklenenTeslimTarihi: form.value.beklenenTeslimTarihi
              ? form.value.beklenenTeslimTarihi.toISOString().split('T')[0]
              : null,
            notlar: form.value.teslimatNotu || null
          })
        } catch (teslimatHata) {
          toastBildirim.hata(
            t('faturalar.teslimatOlusturulamadi') + (teslimatHata?.response?.data?.message || t('faturalar.bilinmeyenHata'))
          )
        }
      }
    }
    taslakTemizle()
    formTemizle()
    closeDialog()
  } catch (err) {
    const msg = err.response?.data?.message || t('faturalar.islemBasarisiz')
    toastBildirim.hata(msg)
  } finally {
    saving.value = false
  }
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
    window.URL.revokeObjectURL(url)
  } catch {
    toastBildirim.hata(t('faturalar.pdfIndirilemedi'))
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
      } catch {
        toastBildirim.hata(t('faturalar.islemBasarisiz'))
      }
    }
  })
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

const topluSil = async () => {
  topluSiliniyor.value = true
  try {
    for (const item of selectedItems.value) {
      await faturaAPI.delete(item.id)
    }
    toastBildirim.basarili(t('faturalar.kayitSilindi', { n: selectedItems.value.length }))
    selectedItems.value = []
    await faturaStore.getAllFaturalar()
  } catch {
    toastBildirim.hata(t('faturalar.silmeBasarisiz'))
  } finally {
    topluSiliniyor.value = false
  }
}


import { formatTarih as formatDate } from '../utils/format.js'
</script>

<style scoped>
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
  overflow-x: auto;
}
.loading {
  text-align: center;
  padding: 40px;
  color: #666;
}
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 15px;
}
.form-group {
  margin-bottom: 15px;
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
.son-fatura-kopyala {
  margin-top: 6px;
  padding: 4px 8px;
  font-size: 12px;
}
.personel-opsiyon {
  display: flex;
  align-items: center;
  gap: 8px;
}
.personel-opsiyon i {
  font-size: 12px;
  color: var(--text-muted);
}
.cari-opsiyon {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}
.cari-opsiyon-detay {
  font-size: 11px;
  color: var(--text-muted);
}
.kitlik-rozeti {
  background: rgba(239, 68, 68, 0.15);
  color: #f87171;
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 20px;
  padding: 1px 8px;
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
}
.urun-ekleme {
  background: rgba(59, 130, 246, 0.05);
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 10px;
  padding: 14px;
  margin: 15px 0;
}
.badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
}
.badge.satis {
  background: rgba(59, 130, 246, 0.15);
  color: #60a5fa;
}
.badge.alis {
  background: rgba(239, 68, 68, 0.15);
  color: #f87171;
}
.durum-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
}
.durum-badge.taslak {
  background: rgba(255, 152, 0, 0.15);
  color: #fb923c;
}
.durum-badge.teklif {
  background: rgba(96, 165, 250, 0.15);
  color: #60a5fa;
}
.durum-badge.kesildi {
  background: rgba(34, 197, 94, 0.15);
  color: #4ade80;
}
.durum-badge.iptal {
  background: rgba(148, 163, 184, 0.1);
  color: #94a3b8;
}
.islem-yapan {
  font-size: 12px;
  color: var(--text-secondary);
}
.islem-yapan-bos {
  font-size: 12px;
  color: var(--text-muted);
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
.batch-action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 16px;
  background: var(--blue-50, #eff6ff);
  border: 1px solid var(--blue-200, #bfdbfe);
  border-radius: 8px;
  margin-bottom: 12px;
}
.batch-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--blue-700, #1d4ed8);
}
.batch-buttons {
  display: flex;
  gap: 8px;
}
</style>
