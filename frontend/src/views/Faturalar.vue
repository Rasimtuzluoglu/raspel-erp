<template>
  <div class="faturalar-container">
    <h1>Fatura Yönetimi</h1>

    <IlkZiyaretIpuclari
      anahtar="faturalar"
      baslik="Fatura Oluşturma"
      metin="'Yeni Fatura' ile satış/alış faturası kesin. Satış faturası stok düşer, alış faturası fabrikadan/tedarikçiden gelen ürünleri stoğa ekler ve tedarikçiyi ürüne işler. Cari seçince son aldığı ürünler önerilir."
    />

    <Toolbar class="toolbar">
      <template #start>
        <Button
          label="Yeni Fatura"
          icon="pi pi-plus"
          class="p-button-success"
          @click="openCreateDialog"
        />
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
        v-if="selectedItems.length > 0"
        class="batch-action-bar"
      >
        <div class="batch-info">
          <i class="pi pi-check-square" />
          <span><strong>{{ selectedItems.length }}</strong> kayıt seçildi</span>
        </div>
        <div class="batch-buttons">
          <Button
            label="Seçilenleri Sil"
            icon="pi pi-trash"
            class="p-button-danger p-button-sm"
            :loading="topluSiliniyor"
            @click="topluSil()"
          />
          <Button
            label="Seçimi Temizle"
            icon="pi pi-times"
            class="p-button-text p-button-sm"
            @click="selectedItems = []"
          />
        </div>
      </div>
      <DataTable
        v-model:selection="selectedItems"
        state-storage="session"
        state-key="faturalar-table-state"
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
          header="Fatura No"
          style="width: 160px"
        />
        <Column
          field="tarih"
          header="Tarih"
          style="width: 110px"
        >
          <template #body="s">
            {{ formatDate(s.data.tarih) }}
          </template>
        </Column>
        <Column
          field="tur"
          header="Tür"
          style="width: 90px"
        >
          <template #body="s">
            <span :class="['badge', s.data.tur === 'SATIS' ? 'satis' : 'alis']">
              {{ s.data.tur === 'SATIS' ? 'Satış' : 'Alış' }}
            </span>
          </template>
        </Column>
        <Column
          field="cariHesapAd"
          header="Cari Hesap"
          style="width: 180px"
        >
          <template #body="s">
            {{ s.data.cariHesapAd || '-' }}
          </template>
        </Column>
        <Column
          field="genelToplam"
          header="Toplam"
          style="width: 130px"
        >
          <template #body="s">
            {{ formatCurrency(s.data.genelToplam) }}
          </template>
        </Column>
        <Column
          field="durum"
          header="Durum"
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
          header="İşlemi Yapan"
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
          header="Teslim Eden"
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
          header="İşlemler"
          style="width: 310px"
        >
          <template #body="s">
            <Button
              icon="pi pi-eye"
              class="p-button-rounded p-button-sm p-button-info"
              title="Görüntüle"
              @click="viewFatura(s.data.id)"
            />
            <Button
              icon="pi pi-print"
              class="p-button-rounded p-button-sm p-button-secondary"
              title="Yazdır & Şablon Tasarla"
              @click="tasarlaVeYazdir(s.data.id)"
            />
            <Button
              icon="pi pi-download"
              class="p-button-rounded p-button-sm p-button-help"
              title="PDF İndir"
              @click="pdfIndir(s.data)"
            />
            <Button
              icon="pi pi-whatsapp"
              class="p-button-rounded p-button-sm p-button-success"
              title="WhatsApp İle Gönder"
              style="background: #25d366; border-color: #25d366"
              @click="whatsappGonder(s.data)"
            />
            <Button
              icon="pi pi-copy"
              class="p-button-rounded p-button-sm p-button-secondary"
              title="Çoğalt"
              @click="cogalt(s.data)"
            />
            <Button
              v-if="s.data.durum === 'TASLAK'"
              icon="pi pi-pencil"
              class="p-button-rounded p-button-sm p-button-warning"
              title="Düzenle"
              @click="editFatura(s.data)"
            />
            <Button
              v-if="s.data.durum === 'TASLAK'"
              icon="pi pi-check"
              class="p-button-rounded p-button-sm p-button-success"
              title="Kes"
              @click="confirmKes(s.data.id)"
            />
            <Button
              v-if="s.data.durum !== 'IPTAL'"
              icon="pi pi-ban"
              class="p-button-rounded p-button-sm p-button-danger"
              title="İptal"
              @click="confirmIptal(s.data.id)"
            />
          </template>
        </Column>
      </DataTable>
      <EmptyState
        v-if="filtrelenmisFaturalar.length === 0"
        message="Henüz fatura yok"
        sub-message="İlk faturanızı oluşturarak satış sürecinizi başlatın."
        icon="pi pi-file"
        action-label="İlk Faturayı Oluştur"
        action-icon="pi pi-plus"
        @action="openCreateDialog"
      />
    </div>

    <Dialog
      v-model:visible="showDialog"
      :header="dialogBaslik"
      :modal="true"
      style="width: 750px"
      :closable="false"
    >
      <div class="form-grid">
        <div class="form-group">
          <label>{{ form.tur === 'ALIS' ? 'Tedarikçi (Fabrika)' : 'Müşteri' }}</label>
          <AutoComplete
            v-model="seciliCariNesnesi"
            :suggestions="cariOnerileri"
            option-label="ad"
            option-value="id"
            :placeholder="
              form.tur === 'ALIS'
                ? 'Tedarikçi ara ve seç (isim, vergi no, telefon)...'
                : 'Cari ara ve seç (isim, vergi no, telefon)...'
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
            label="Son Faturayı Kopyala"
            icon="pi pi-copy"
            class="p-button-text p-button-sm son-fatura-kopyala"
            :loading="sonFaturaYukleniyor"
            @click="sonFaturayiKopyala"
          />
        </div>
        <div class="form-group">
          <label>Fatura Türü *</label>
          <Dropdown
            v-model="form.tur"
            :options="turSecenekler"
            option-label="label"
            option-value="value"
            placeholder="Seçiniz"
            class="w-full"
          />
        </div>
        <div
          v-if="form.tur === 'ALIS'"
          class="form-group"
        >
          <label>Giriş Deposu</label>
          <Dropdown
            v-model="form.depoId"
            :options="depolar"
            option-label="ad"
            option-value="id"
            placeholder="Depo Seçin"
            class="w-full"
            :show-clear="true"
          />
        </div>
        <div class="form-group">
          <label>Tarih *</label>
          <DatePicker
            v-model="form.tarih"
            date-format="dd.mm.yy"
            class="w-full"
          />
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>Para Birimi</label>
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
            <label>Kur Bilgisi (TL Karşılığı)</label>
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
          <label>Teslim Eden Personel</label>
          <div class="teslim-eden-grup">
            <Dropdown
              v-model="form.teslimEden"
              :options="personelSecenekleri"
              option-label="label"
              option-value="value"
              filter
              editable
              placeholder="Götüren personeli seçin veya yazın"
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
          <label>Teslim Durumu</label>
          <Dropdown
            v-model="form.teslimDurumu"
            :options="teslimDurumSecenekleri"
            option-label="label"
            option-value="value"
            class="w-full"
          />
        </div>
        <div class="form-group">
          <label>Teslim Notu</label>
          <Textarea
            v-model="form.teslimNotu"
            rows="2"
            placeholder="Teslimat notu (isteğe bağlı)"
            class="w-full"
          />
        </div>
        <div class="form-group">
          <label>Açıklama</label>
          <Textarea
            v-model="form.aciklama"
            rows="2"
            placeholder="İsteğe bağlı"
            class="w-full"
          />
        </div>
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
            <label>Ürün Seç (Stoktan Otomatik Ekle)</label>
            <Dropdown
              v-model="urunSecimi"
              :options="stokStore.stoklar"
              filter
              option-label="ad"
              option-value="id"
              placeholder="Ürün ara ve seç..."
              class="w-full"
              @change="urunSecildi"
            >
              <template #option="s">
                <div style="display: flex; align-items: center; gap: 10px">
                  <span style="flex: 1; color: #f1f5f9">{{ s.option.ad }}</span>
                  <span
                    v-if="kritikStokMu(s.option)"
                    class="kitlik-rozeti"
                    :title="'Kritik stok seviyesi: minimum ' + (s.option.minMiktar || 0)"
                  >Son {{ Math.floor(s.option.miktar) }} adet</span>
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
            <label>Miktar</label>
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

      <div
        v-if="fiyatGecmisi && fiyatGecmisi.gecmis && fiyatGecmisi.gecmis.length"
        class="fiyat-gecmisi-panel"
      >
        <div class="fiyat-gecmisi-ust">
          <i class="pi pi-chart-line" />
          <span>Alış Fiyat Geçmişi</span>
          <span :class="['trend-rozet', (fiyatGecmisi.trend || '').toLowerCase()]">{{
            trendLabel(fiyatGecmisi.trend)
          }}</span>
        </div>
        <div class="fiyat-gecmisi-liste">
          <div
            v-for="(kayit, i) in fiyatGecmisi.gecmis"
            :key="i"
            class="fiyat-gecmisi-item"
          >
            <span class="fg-tarih">{{ formatDate(kayit.tarih) }}</span>
            <span class="fg-fatura">{{ kayit.faturaNumarasi }}</span>
            <span class="fg-fiyat">{{ formatCurrency(kayit.birimFiyat) }}</span>
          </div>
        </div>
        <div
          v-if="fiyatGecmisi.guncelFiyat"
          class="fiyat-gecmisi-guncel"
        >
          Güncel Satış Fiyatı: <strong>{{ formatCurrency(fiyatGecmisi.guncelFiyat) }}</strong>
        </div>
      </div>

      <div
        v-if="form.cariHesapId && cariSonUrunler.length > 0"
        class="son-urunler-panel"
      >
        <div class="son-urunler-ust">
          <i class="pi pi-history" />
          <span>Bu cari son olarak şunları aldı</span>
          <button
            type="button"
            class="son-urunler-kapat"
            title="Gizle"
            @click="cariSonUrunlerGizle = true"
          >
            <i class="pi pi-times" />
          </button>
        </div>
        <div class="son-urunler-liste">
          <button
            v-for="(u, idx) in (cariSonUrunler || [])"
            :key="u?.stokId || idx"
            type="button"
            class="son-urun-item"
            @click="sonUrunuEkle(u)"
          >
            <span class="son-urun-ad">{{ u?.stokAd || (u?.stokId ? 'Ürün #' + u.stokId : 'Ürün') }}</span>
            <span class="son-urun-bilgi">{{ u?.sonAlisTarihi || '' }} · {{ u?.adet || 1 }} adet</span>
            <span class="son-urun-fiyat">{{ formatCurrency(u?.sonBirimFiyat || 0) }}</span>
            <i class="pi pi-plus son-urun-ekle" />
          </button>
        </div>
      </div>

      <h3 style="margin: 20px 0 10px">
        Fatura Kalemleri
      </h3>
      <DataTable
        state-storage="session"
        state-key="faturalar-table-state"
        :value="form.kalemler"
        striped-rows
      >
        <Column
          header="#"
          style="width: 40px"
        >
          <template #body="s">
            {{ s.index + 1 }}
          </template>
        </Column>
        <Column header="Açıklama *">
          <template #body="s">
            <InputText
              v-model="s.data.aciklama"
              placeholder="Kalem açıklaması"
              class="w-full"
            />
          </template>
        </Column>
        <Column
          header="Adet *"
          style="width: 90px"
        >
          <template #body="s">
            <InputNumber
              v-model="s.data.adet"
              :min="1"
              class="w-full"
            />
          </template>
        </Column>
        <Column
          header="Birim Fiyat *"
          style="width: 130px"
        >
          <template #body="s">
            <InputNumber
              v-model="s.data.birimFiyat"
              :min="0"
              :min-fraction-digits="2"
              :max-fraction-digits="2"
              class="w-full"
            />
          </template>
        </Column>
        <Column
          header="İskonto %"
          style="width: 100px"
        >
          <template #body="s">
            <InputNumber
              v-model="s.data.iskontoOrani"
              :min="0"
              :max="100"
              :min-fraction-digits="0"
              class="w-full"
            />
          </template>
        </Column>
        <Column
          header="KDV %"
          style="width: 80px"
        >
          <template #body="s">
            <Dropdown
              v-model="s.data.kdvOrani"
              :options="[0, 10, 20]"
              class="w-full"
            />
          </template>
        </Column>
        <Column
          header="Tutar"
          style="width: 120px"
        >
          <template #body="s">
            {{ formatCurrency(kalemTutar(s.data)) }}
          </template>
        </Column>
        <Column
          header=""
          style="width: 50px"
        >
          <template #body="s">
            <Button
              icon="pi pi-trash"
              class="p-button-rounded p-button-danger p-button-sm"
              @click="removeKalem(s.index)"
            />
          </template>
        </Column>
      </DataTable>
      <div style="margin-top: 10px">
        <Button
          label="+ Kalem Ekle"
          icon="pi pi-plus"
          class="p-button-sm p-button-outlined"
          @click="addKalem"
        />
      </div>

      <div class="summary-box">
        <div class="summary-row">
          <span>Ara Toplam:</span><span>{{ formatCurrency(araToplam) }}</span>
        </div>
        <div class="summary-row">
          <span>KDV:</span><span>{{ formatCurrency(kdvToplam) }}</span>
        </div>
        <div class="summary-row total">
          <span>Genel Toplam:</span><span>{{ formatCurrency(genelToplam) }}</span>
        </div>
      </div>

      <template #footer>
        <Button
          label="İptal"
          icon="pi pi-times"
          class="p-button-text"
          @click="closeDialog"
        />
        <Button
          :label="editingId ? 'Faturayı Güncelle' : 'Faturayı Oluştur'"
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
import { useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { useFaturaStore } from '../stores/faturaStore.js'
import { useCariHesapStore } from '../stores/cariHesapStore.js'
import { useStokStore } from '../stores/stokStore.js'
import { useDovizStore } from '../stores/dovizStore.js'

const dovizStore = useDovizStore()
import { faturaAPI, excelAPI, pdfAPI, personelAPI, depoAPI } from '../api/index.js'
import { useKisayollar } from '../composables/useKisayollar.js'
import { useTaslakKayit } from '../composables/useTaslakKayit.js'
import { useFormKorumasi } from '../composables/useFormKorumasi.js'
import TarihHizliSecim from '../components/TarihHizliSecim.vue'
import IlkZiyaretIpuclari from '../components/IlkZiyaretIpuclari.vue'
import FaturaTasarimModal from '../components/FaturaTasarimModal.vue'
import { formatCurrency } from '../utils/format.js'

const router = useRouter()
const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
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

const turSecenekler = [
  { label: 'Satış', value: 'SATIS' },
  { label: 'Alış', value: 'ALIS' }
]

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
  kalemler: []
})

const depolar = ref([])

const teslimDurumSecenekleri = [
  { label: 'Bekliyor', value: 'BEKLIYOR' },
  { label: 'Yolda', value: 'YOLDA' },
  { label: 'Teslim Edildi', value: 'TESLIM_EDILDI' }
]

const urunSecimi = ref(null)
const urunAdet = ref(1)

const dialogBaslik = computed(() => (editingId.value ? 'Fatura Düzenle' : 'Yeni Fatura Oluştur'))

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
      summary: 'Taslak Geri Yüklendi',
      detail: 'Kesilmemiş faturanız geri yüklendi.',
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
      depolarıYukle()
    ])
  } catch (err) {
    toastBildirim.hata('Veriler yüklenirken hata oluştu')
  } finally {
    loading.value = false
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

const trendLabel = (trend) => ({ ARTIS: 'Yükseliyor', AZALIS: 'Düşüyor', STABIL: 'Sabit' })[trend] || '-'

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
  const kaynak = cariHesapStore.cariHesaplar || []
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
  toastBildirim.basarili('Kalem eklendi')
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
    toastBildirim.basarili('Son fatura kopyalandı')
  } catch {
    toastBildirim.hata('Son fatura yüklenemedi')
  } finally {
    sonFaturaYukleniyor.value = false
  }
}

const removeKalem = (index) => {
  form.value.kalemler.splice(index, 1)
}

const kalemTutar = (kalem) => {
  const brf = kalem.birimFiyat || 0
  const adt = kalem.adet || 0
  const iskontoOran = (kalem.iskontoOrani || 0) / 100
  const net = brf * adt * (1 - iskontoOran)
  const kdvOran = (kalem.kdvOrani || 0) / 100
  return net + net * kdvOran
}

const araToplam = computed(() => {
  return form.value.kalemler.reduce((t, k) => {
    const brf = k.birimFiyat || 0
    const adt = k.adet || 0
    const iskontoOran = (k.iskontoOrani || 0) / 100
    return t + brf * adt * (1 - iskontoOran)
  }, 0)
})

const kdvToplam = computed(() => {
  return form.value.kalemler.reduce((t, k) => {
    const brf = k.birimFiyat || 0
    const adt = k.adet || 0
    const iskontoOran = (k.iskontoOrani || 0) / 100
    const net = brf * adt * (1 - iskontoOran)
    return t + net * ((k.kdvOrani || 0) / 100)
  }, 0)
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
    kalemler: [{ aciklama: '', adet: 1, birimFiyat: 0, kdvOrani: 20 }]
  }
  formTemizle()
  showDialog.value = true
}

const editFatura = (fatura) => {
  editingId.value = fatura.id
  seciliCariNesnesi.value = cariHesapStore.cariHesaplar.find((c) => c.id === fatura.cariHesapId) || null
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
  toastBildirim.basarili('Fatura çoğaltıldı, değişiklikleri kaydedin')
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
      toastBildirim.basarili('Fatura güncellendi')
    } else {
      await faturaStore.addFatura(payload)
      toastBildirim.basarili('Fatura oluşturuldu')
    }
    taslakTemizle()
    formTemizle()
    closeDialog()
  } catch (err) {
    const msg = err.response?.data?.message || 'İşlem başarısız'
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
    toastBildirim.hata('PDF indirilemedi')
  }
}

const confirmKes = (id) => {
  confirm.require({
    message: 'Faturayı kesmek istediğinizden emin misiniz?',
    header: 'Fatura Kes',
    icon: 'pi pi-check-circle',
    accept: async () => {
      try {
        await faturaStore.updateDurum(id, 'KESILDI')
        toastBildirim.basarili('Fatura kesildi')
      } catch {
        toastBildirim.hata('İşlem başarısız')
      }
    }
  })
}

const confirmIptal = (id) => {
  confirm.require({
    message: 'Faturayı iptal etmek istediğinizden emin misiniz?',
    header: 'Fatura İptal',
    icon: 'pi pi-exclamation-triangle',
    accept: async () => {
      try {
        await faturaStore.updateDurum(id, 'IPTAL')
        toastBildirim.basarili('Fatura iptal edildi')
      } catch {
        toastBildirim.hata('İşlem başarısız')
      }
    }
  })
}

const durumLabel = (d) => {
  const lbl = { TASLAK: 'Taslak', TEKLIF: 'Teklif', KESILDI: 'Kesildi', IPTAL: 'İptal' }
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
    toastBildirim.basarili(`${selectedItems.value.length} kayıt silindi`)
    selectedItems.value = []
    await faturaStore.getAllFaturalar()
  } catch {
    toastBildirim.hata('Silme işlemi başarısız')
  } finally {
    topluSiliniyor.value = false
  }
}


const formatDate = (dateString) => {
  if (!dateString) return '-'
  return new Intl.DateTimeFormat('tr-TR', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(
    new Date(dateString)
  )
}
</script>

<style scoped>
.faturalar-container {
  padding: 20px;
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
  grid-template-columns: 1fr 1fr;
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
.fiyat-gecmisi-panel {
  background: rgba(139, 92, 246, 0.06);
  border: 1px solid rgba(139, 92, 246, 0.25);
  border-radius: 10px;
  padding: 12px 14px;
  margin-bottom: 15px;
}
.fiyat-gecmisi-ust {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #a78bfa;
  margin-bottom: 8px;
}
.fiyat-gecmisi-liste {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.fiyat-gecmisi-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: var(--text-secondary);
}
.fg-tarih {
  width: 80px;
  flex-shrink: 0;
}
.fg-fatura {
  flex: 1;
}
.fg-fiyat {
  font-weight: 600;
  color: var(--text-primary);
}
.fiyat-gecmisi-guncel {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid rgba(139, 92, 246, 0.2);
  font-size: 12px;
  color: var(--text-secondary);
}
.trend-rozet {
  margin-left: auto;
  padding: 1px 8px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
}
.trend-rozet.artis {
  background: rgba(34, 197, 94, 0.15);
  color: #4ade80;
}
.trend-rozet.azalis {
  background: rgba(239, 68, 68, 0.15);
  color: #f87171;
}
.trend-rozet.stabil {
  background: rgba(148, 163, 184, 0.15);
  color: #94a3b8;
}
.urun-ekleme {
  background: rgba(59, 130, 246, 0.05);
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 10px;
  padding: 14px;
  margin: 15px 0;
}
.son-urunler-panel {
  background: rgba(16, 185, 129, 0.06);
  border: 1px solid rgba(16, 185, 129, 0.25);
  border-radius: 10px;
  padding: 12px 14px;
  margin-bottom: 15px;
}
.son-urunler-ust {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #34d399;
  margin-bottom: 10px;
}
.son-urunler-kapat {
  margin-left: auto;
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  font-size: 13px;
  padding: 2px 4px;
}
.son-urunler-kapat:hover {
  color: #f87171;
}
.son-urunler-liste {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.son-urun-item {
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--bg-card);
  border: 1px solid rgba(16, 185, 129, 0.2);
  border-radius: 8px;
  padding: 8px 12px;
  cursor: pointer;
  transition: all 0.2s;
  color: var(--text-primary);
}
.son-urun-item:hover {
  border-color: #10b981;
  background: rgba(16, 185, 129, 0.1);
  transform: translateY(-1px);
}
.son-urun-ad {
  font-size: 13px;
  font-weight: 600;
}
.son-urun-bilgi {
  font-size: 11px;
  color: var(--text-muted);
}
.son-urun-fiyat {
  font-size: 12px;
  color: #34d399;
  font-weight: 600;
}
.son-urun-ekle {
  font-size: 12px;
  color: #10b981;
}
.summary-box {
  background: var(--border);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 15px;
  margin-top: 15px;
}
.summary-row {
  display: flex;
  justify-content: space-between;
  padding: 5px 0;
  font-size: 14px;
  color: var(--text-secondary);
}
.summary-row.total {
  font-weight: 700;
  font-size: 18px;
  border-top: 2px solid #3b82f6;
  margin-top: 5px;
  padding-top: 10px;
  color: var(--text-primary);
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
