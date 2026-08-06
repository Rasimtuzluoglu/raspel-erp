<template>
  <div class="stoklar-container">
    <h1>Stok Yönetimi</h1>
    <Toolbar class="toolbar">
      <template #start>
        <Button
          label="Yeni Ürün"
          icon="pi pi-plus"
          class="p-button-success"
          @click="openDialog"
        />
        <Button
          label="Toplu Fiyat Güncelle"
          icon="pi pi-dollar"
          class="p-button-help"
          style="margin-left: 8px"
          @click="batchFiyatDialog = true"
        />
        <div
          v-if="seciliStoklar.length > 0"
          class="batch-actions"
        >
          <span class="batch-count">{{ seciliStoklar.length }} seçili</span>
          <Button
            label="Toplu Sil"
            icon="pi pi-trash"
            class="p-button-sm p-button-danger"
            @click="batchSil"
          />
          <Button
            label="CSV Aktar"
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
          style="margin-right:8px"
          @click="excelIndir"
        />
        <div class="toolbar-end">
          <span class="p-input-icon-left">
            <i class="pi pi-search" />
            <InputText
              v-model="aramaMetni"
              placeholder="Ürün adı, kod veya birim..."
              @input="ara"
            />
          </span>
          <Button
            :icon="gosterim === 'tablo' ? 'pi pi-th-large' : 'pi pi-list'"
            class="p-button-text p-button-sm"
            :title="gosterim === 'tablo' ? 'Kart Görünümü' : 'Tablo Görünümü'"
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
          placeholder="Urun adi, kod, barkod..."
          @input="filtrele"
        />
      </span>
      <InputText
        v-model="filtreKategori"
        placeholder="Kategori"
        class="filter-input"
        @input="filtrele"
      />
      <InputText
        v-model="filtreMarka"
        placeholder="Marka"
        class="filter-input"
        @input="filtrele"
      />
      <Dropdown
        v-model="filtreStokGrubu"
        :options="['','Hammadde','Mamul','Yari Mamul','Sarf','Aksesuar']"
        placeholder="Stok Grubu"
        class="filter-dropdown"
        @change="filtrele"
      />
      <InputNumber
        v-model="filtreMinFiyat"
        placeholder="Min Fiyat"
        class="filter-input-sm"
        @input="filtrele"
      />
      <InputNumber
        v-model="filtreMaxFiyat"
        placeholder="Max Fiyat"
        class="filter-input-sm"
        @input="filtrele"
      />
      <Button
        icon="pi pi-times"
        class="p-button-text p-button-sm"
        title="Temizle"
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
        :value="filtrelenmisStoklar"
        :paginator="true"
        :rows="25"
        :rows-per-page-options="[15,25,50,100]"
        paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown CurrentPageReport"
        current-page-report-template="{totalRecords} kayıttan {first}-{last}"
        selection-mode="multiple"
        data-key="id"
        striped-rows
        sort-field="miktar"
        :sort-order="1"
        class="p-datatable-sm"
        :global-filter-fields="['ad','stokKodu','birim']"
        @row-click="stokSec($event.data)"
      >
        <template #header>
          <div class="table-header">
            <span class="toplam-bilgi">{{ stokStore.stoklar.length }} ürün</span>
            <span
              v-if="kritikAdet > 0"
              class="kritik-bilgi"
            ><i class="pi pi-exclamation-triangle" /> {{ kritikAdet }} kritik</span>
          </div>
        </template>
        <template #empty>
          <EmptyState
            message="Henüz ürün yok"
            sub-message="Stok ürünlerinizi ekleyerek envanterinizi oluşturun."
            icon="pi pi-box"
            action-label="İlk Ürünü Ekle"
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
          header="Stok Kodu"
          sortable
          style="width:120px"
        />
        <Column
          field="ad"
          header="Ürün Adı"
          sortable
          style="min-width:180px"
        />
        <Column
          field="birim"
          header="Birim"
          sortable
          style="width:90px"
        />
        <Column
          field="miktar"
          header="Miktar"
          sortable
          style="width:110px"
        >
          <template #body="s">
            <span :class="s.data.minMiktar && s.data.miktar <= s.data.minMiktar ? 'kritik' : 'normal'">
              {{ s.data.miktar }} {{ s.data.birim || '' }}
            </span>
          </template>
        </Column>
        <Column
          field="fiyat"
          header="Birim Fiyat"
          sortable
          style="width:130px"
        >
          <template #body="s">
            {{ formatCurrency(s.data.fiyat) }}
          </template>
        </Column>
        <Column
          header="Stok Değeri"
          sortable
          style="width:130px"
        >
          <template #body="s">
            {{ formatCurrency((s.data.miktar || 0) * (s.data.fiyat || 0)) }}
          </template>
        </Column>
        <Column
          header="Kritik"
          style="width:80px"
        >
          <template #body="s">
            <i
              v-if="s.data.minMiktar && s.data.miktar <= s.data.minMiktar"
              class="pi pi-exclamation-triangle"
              style="color:#f87171;font-size:16px"
            />
          </template>
        </Column>
        <Column
          header=""
          style="width:100px"
        >
          <template #body="s">
            <Button
              icon="pi pi-pencil"
              class="p-button-rounded p-button-info p-button-sm"
              style="margin-right: 6px"
              @click.stop="editStok(s.data)"
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
      <div
        v-for="s in filtrelenmisStoklar"
        :key="s.id"
        class="stok-kart"
        :class="{ 'dusuk-stok': s.minMiktar && s.miktar <= s.minMiktar }"
        @click="stokSec(s)"
      >
        <div class="kart-ust">
          <div
            v-if="s.stokKodu"
            class="stok-kod"
          >
            {{ s.stokKodu }}
          </div>
          <span
            v-if="s.minMiktar && s.miktar <= s.minMiktar"
            class="uyari-eti"
          ><i class="pi pi-exclamation-triangle" /> Kritik</span>
        </div>
        <h3>{{ s.ad }}</h3>
        <div class="kart-bilgi">
          <div class="bilgi-item">
            <span class="bilgi-label">Miktar</span>
            <span
              class="bilgi-deger"
              :class="s.miktar <= (s.minMiktar || 0) ? 'kritik' : 'normal'"
            >
              {{ s.miktar }} {{ s.birim || '' }}
            </span>
          </div>
          <div class="bilgi-item">
            <span class="bilgi-label">Birim Fiyat</span>
            <span class="bilgi-deger">{{ formatCurrency(s.fiyat) }}</span>
          </div>
        </div>
        <div class="kart-islem">
          <Button
            icon="pi pi-pencil"
            class="p-button-rounded p-button-info p-button-sm"
            @click.stop="editStok(s)"
          />
          <Button
            icon="pi pi-trash"
            class="p-button-rounded p-button-danger p-button-sm"
            @click.stop="confirmDel(s.id)"
          />
        </div>
      </div>
      <Message
        v-if="filtrelenmisStoklar.length === 0"
        severity="info"
        text="Eşleşen ürün bulunamadı."
        class="full-width"
      />
    </div>

    <div
      v-if="seciliStok"
      class="hareket-bolumu"
    >
      <div class="hareket-header">
        <h2>{{ seciliStok.ad }} <small style="color:#64748b;font-weight:400">({{ seciliStok.miktar }} {{ seciliStok.birim || 'Adet' }})</small></h2>
        <Button
          label="+ Stok Giriş"
          icon="pi pi-plus-circle"
          class="p-button-success p-button-sm"
          @click="openHareketDialog('GIRIS')"
        />
        <Button
          label="- Stok Çıkış"
          icon="pi pi-minus-circle"
          class="p-button-danger p-button-sm"
          @click="openHareketDialog('CIKIS')"
        />
        <Button
          icon="pi pi-chevron-up"
          class="p-button-text p-button-sm"
          title="Kapat"
          @click="seciliStok = null"
        />
      </div>
      <div class="table-container">
        <DataTable
          :value="stokHareketler"
          striped-rows
          :rows="8"
          :paginator="true"
          paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport"
          current-page-report-template="{first} - {last} ({totalRecords} kayıt)"
        >
          <Column
            header="Tarih"
            style="width:100px"
          >
            <template #body="s">
              {{ formatDate(s.data.hareketTarihi) }}
            </template>
          </Column>
          <Column
            header="Tür"
            style="width:90px"
          >
            <template #body="s">
              <span :class="['badge', s.data.tur === 'GIRIS' ? 'giris' : 'cikis']">
                {{ s.data.tur === 'GIRIS' ? 'Giriş' : 'Çıkış' }}
              </span>
            </template>
          </Column>
          <Column
            header="Miktar"
            style="width:90px"
          >
            <template #body="s">
              <span :class="s.data.tur === 'GIRIS' ? 'positive' : 'negative'">{{ s.data.miktar }}</span>
            </template>
          </Column>
          <Column
            header="Cari Hesap"
            style="width:160px"
          >
            <template #body="s">
              {{ s.data.cariHesapAd || '-' }}
            </template>
          </Column>
          <Column header="Açıklama" />
          <Column
            header=""
            style="width:60px"
          >
            <template #body="s">
              <Button
                icon="pi pi-trash"
                class="p-button-rounded p-button-danger p-button-sm"
                @click="delHareket(s.data.id)"
              />
            </template>
          </Column>
        </DataTable>
        <Message
          v-if="stokHareketler.length === 0"
          severity="info"
          text="Hareket bulunmamaktadır."
        />
      </div>
    </div>

    <Dialog
      v-model:visible="showDialog"
      :header="editingId ? 'Ürün Düzenle' : 'Yeni Ürün'"
      :modal="true"
      style="width:650px"
    >
      <div class="form-section">
        <div class="form-section-title">
          Temel Bilgiler
        </div>
        <div class="form-row">
          <div class="form-grup flex-2">
            <label>Ürün Adı *</label>
            <InputText
              v-model="form.ad"
              placeholder="Ürün adı"
              class="w-full"
            />
          </div>
          <div class="form-grup">
            <label>Birim</label>
            <Dropdown
              v-model="form.birim"
              :options="['Adet','Koli','Kg','Metre','Litre','Paket']"
              placeholder="Seçiniz"
              class="w-full"
            />
          </div>
        </div>
        <div class="form-row">
          <div class="form-grup">
            <label>Stok Kodu</label>
            <InputText
              v-model="form.stokKodu"
              placeholder="Örn: URN-001"
              class="w-full"
            />
          </div>
          <div class="form-grup">
            <label>Barkod</label>
            <InputText
              v-model="form.barkod"
              placeholder="Barkod numarası"
              class="w-full"
            />
          </div>
        </div>
        <div class="form-row">
          <div class="form-grup">
            <label>Marka</label>
            <InputText
              v-model="form.marka"
              placeholder="Ürün markası"
              class="w-full"
            />
          </div>
          <div class="form-grup">
            <label>Kategori</label>
            <InputText
              v-model="form.kategori"
              placeholder="Ürün kategorisi"
              class="w-full"
            />
          </div>
        </div>
        <div class="form-row">
          <div class="form-grup">
            <label>Stok Grubu</label>
            <InputText
              v-model="form.stokGrubu"
              placeholder="Örn: Hammadde, Mamül"
              class="w-full"
            />
          </div>
          <div class="form-grup">
            <label>Raf No</label>
            <InputText
              v-model="form.rafNo"
              placeholder="Örn: A-12"
              class="w-full"
            />
          </div>
        </div>
        <div class="form-row">
          <div class="form-grup">
            <label>2. Birim</label>
            <InputText
              v-model="form.birim2"
              placeholder="İkinci birim"
              class="w-full"
            />
          </div>
          <div class="form-grup">
            <label>Çevrim Katsayısı</label>
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
          Fiyat & Stok
        </div>
        <div class="form-row">
          <div class="form-grup">
            <label>Alış Fiyatı *</label>
            <InputNumber
              v-model="form.fiyat"
              :min="0"
              :min-fraction-digits="2"
              class="w-full"
            />
          </div>
          <div class="form-grup">
            <label>Satış Fiyatı</label>
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
            <label>KDV Oranı (%)</label>
            <InputNumber
              v-model="form.kdvOrani"
              :min="0"
              :max="100"
              class="w-full"
              placeholder="%"
            />
          </div>
          <div class="form-grup">
            <label>Ağırlık (kg)</label>
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
            <label>Mevcut Miktar</label>
            <InputNumber
              v-model="form.miktar"
              :min="0"
              :min-fraction-digits="0"
              class="w-full"
            />
          </div>
          <div class="form-grup">
            <label>Min. Stok Seviyesi</label>
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
            <label>Maliyet Yöntemi</label>
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
          Tedarikçi Bilgileri
        </div>
        <div class="form-row">
          <div class="form-grup">
            <label>Tedarikçi</label>
            <Dropdown
              v-model="form.tedarikciId"
              :options="cariHesapStore.cariHesaplar"
              option-label="ad"
              option-value="id"
              placeholder="Tedarikçi seçin"
              class="w-full"
            />
          </div>
          <div class="form-grup">
            <label>Tedarikçi Stok Kodu</label>
            <InputText
              v-model="form.tedarikciStokKodu"
              placeholder="Tedarikçideki stok kodu"
              class="w-full"
            />
          </div>
        </div>
        <div class="form-row">
          <div class="form-grup">
            <label>Tedarikçi Fiyatı</label>
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
          Ek Bilgiler
        </div>
        <div class="form-grup">
          <label>Açıklama</label>
          <Textarea
            v-model="form.aciklama"
            rows="2"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          label="İptal"
          icon="pi pi-times"
          class="p-button-text"
          @click="showDialog = false"
        />
        <Button
          :label="editingId ? 'Güncelle' : 'Kaydet'"
          icon="pi pi-check"
          :loading="saving"
          @click="saveStok"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="showHareketDialog"
      :header="hareketBaslik"
      :modal="true"
      style="width:500px"
    >
      <div class="form-grup">
        <label>Miktar *</label>
        <InputNumber
          v-model="hareketForm.miktar"
          :min="0.01"
          :min-fraction-digits="1"
          class="w-full"
        />
      </div>
      <div class="form-grup">
        <label>Tarih *</label>
        <DatePicker
          v-model="hareketForm.hareketTarihi"
          date-format="dd.mm.yy"
          class="w-full"
        />
      </div>
      <div class="form-grup">
        <label>Cari Hesap</label>
        <Dropdown
          v-model="hareketForm.cariHesapId"
          :options="cariHesapStore.cariHesaplar"
          option-label="ad"
          option-value="id"
          placeholder="İsteğe bağlı"
          class="w-full"
        />
      </div>
      <div class="form-grup">
        <label>Açıklama</label>
        <Textarea
          v-model="hareketForm.aciklama"
          rows="2"
          class="w-full"
        />
      </div>
      <template #footer>
        <Button
          label="İptal"
          icon="pi pi-times"
          class="p-button-text"
          @click="showHareketDialog = false"
        />
        <Button
          label="Kaydet"
          icon="pi pi-check"
          :loading="saving"
          @click="saveHareket"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="batchFiyatDialog"
      header="Toplu Fiyat Güncelleme"
      :modal="true"
      style="width:480px"
    >
      <div class="form-grup">
        <label>İşlem Yönü</label>
        <Dropdown
          v-model="batchFiyatForm.yon"
          :options="['ARTIR','AZALT']"
          class="w-full"
        />
      </div>
      <div class="form-grup">
        <label>Oran (%)</label>
        <InputNumber
          v-model="batchFiyatForm.oran"
          :min="0"
          :max="100"
          :min-fraction-digits="1"
          class="w-full"
        />
      </div>
      <div class="form-grup">
        <label>Kategori Filtre (opsiyonel)</label>
        <InputText
          v-model="batchFiyatForm.kategori"
          placeholder="Tüm kategoriler"
          class="w-full"
        />
      </div>
      <div class="form-grup">
        <label>Stok Grubu Filtre (opsiyonel)</label>
        <InputText
          v-model="batchFiyatForm.stokGrubu"
          placeholder="Tüm gruplar"
          class="w-full"
        />
      </div>
      <template #footer>
        <Button
          label="İptal"
          icon="pi pi-times"
          class="p-button-text"
          @click="batchFiyatDialog = false"
        />
        <Button
          label="Uygula"
          icon="pi pi-check"
          :loading="batchLoading"
          @click="batchFiyatUygula"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { useStokStore } from '../stores/stokStore.js'
import { useCariHesapStore } from '../stores/cariHesapStore.js'
import { stokAPI, excelAPI } from '../api/index.js'
import { useKisayollar } from '../composables/useKisayollar.js'
import { useFormKorumasi } from '../composables/useFormKorumasi.js'

const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const stokStore = useStokStore()
const cariHesapStore = useCariHesapStore()

useKisayollar({
  yeni: () => openDialog(),
  iptal: () => { showDialog.value = false },
  kaydet: () => saveStok()
})

const { temizle: formTemizle } = useFormKorumasi(form)

const aramaMetni = ref('')
let aramaZaman = null
onUnmounted(() => { if (aramaZaman) clearTimeout(aramaZaman) })

const filtreArama = ref('')
const filtreKategori = ref('')
const filtreMarka = ref('')
const filtreStokGrubu = ref('')
const filtreMinFiyat = ref(null)
const filtreMaxFiyat = ref(null)

const seciliStok = ref(null)
const seciliStokId = ref(null)
const seciliStoklar = ref([])
const stokHareketler = ref([])
const saving = ref(false)
const gosterim = ref('tablo')

const showDialog = ref(false)
const editingId = ref(null)
const maliyetYontemiSecenekleri = [
  { label: 'Ortalama Maliyet', value: 'ORTALAMA' },
  { label: 'FIFO', value: 'FIFO' },
  { label: 'LIFO', value: 'LIFO' }
]
const form = ref({ stokKodu: '', barkod: '', ad: '', birim: '', birim2: '', cevrimKatsayisi: null, marka: '', stokGrubu: '', kategori: '', rafNo: '', fiyat: 0, satisFiyati: null, kdvOrani: null, agirlik: null, miktar: 0, minMiktar: null, tedarikciId: null, tedarikciStokKodu: '', tedarikciFiyat: null, maliyetYontemi: 'ORTALAMA', aciklama: '' })

const showHareketDialog = ref(false)
const hareketTur = ref('GIRIS')
const hareketForm = ref({ miktar: null, hareketTarihi: new Date(), cariHesapId: null, aciklama: '' })

const hareketBaslik = computed(() => hareketTur.value === 'GIRIS' ? 'Stok Girişi' : 'Stok Çıkışı')

const filtrelenmisStoklar = computed(() => {
  return stokStore.stoklar.filter(s => {
    const q = filtreArama.value.toLowerCase()
    if (filtreArama.value && !s.ad?.toLowerCase().includes(q) && !s.stokKodu?.toLowerCase().includes(q) && !s.barkod?.toLowerCase().includes(q)) return false
    if (filtreKategori.value && s.kategori !== filtreKategori.value) return false
    if (filtreMarka.value && !s.marka?.toLowerCase().includes(filtreMarka.value.toLowerCase())) return false
    if (filtreStokGrubu.value && s.stokGrubu !== filtreStokGrubu.value) return false
    if (filtreMinFiyat.value != null && (s.fiyat || 0) < filtreMinFiyat.value) return false
    if (filtreMaxFiyat.value != null && (s.fiyat || 0) > filtreMaxFiyat.value) return false
    return true
  })
})

const kritikAdet = computed(() => stokStore.stoklar.filter(s => s.minMiktar && s.miktar <= s.minMiktar).length)

onMounted(async () => {
  await Promise.all([stokStore.getAll({ size: 1000 }), cariHesapStore.getAllCariHesaplar()])
})

const ara = () => {
  filtreArama.value = aramaMetni.value
}

const filtrele = () => {
  // reactivity handles filtering via computed
}

const filtreTemizle = () => {
  filtreArama.value = ''
  filtreKategori.value = ''
  filtreMarka.value = ''
  filtreStokGrubu.value = ''
  filtreMinFiyat.value = null
  filtreMaxFiyat.value = null
}

const stokSec = async (s) => {
  seciliStok.value = s; seciliStokId.value = s.id
  try { const r = await stokAPI.getHareketler(s.id); stokHareketler.value = r.data }
  catch (err) { toastBildirim.hata(err?.response?.data?.message || err?.message || 'Hareketler yüklenemedi') }
}

const openDialog = () => {
  editingId.value = null; form.value = { stokKodu: '', barkod: '', ad: '', birim: '', birim2: '', cevrimKatsayisi: null, marka: '', stokGrubu: '', kategori: '', rafNo: '', fiyat: 0, satisFiyati: null, kdvOrani: null, agirlik: null, miktar: 0, minMiktar: null, tedarikciId: null, tedarikciStokKodu: '', tedarikciFiyat: null, maliyetYontemi: 'ORTALAMA', aciklama: '' }
  formTemizle()
  showDialog.value = true
}

const editStok = (s) => {
  editingId.value = s.id; form.value = { stokKodu: s.stokKodu || '', barkod: s.barkod || '', ad: s.ad, birim: s.birim || '', birim2: s.birim2 || '', cevrimKatsayisi: s.cevrimKatsayisi || null, marka: s.marka || '', stokGrubu: s.stokGrubu || '', kategori: s.kategori || '', rafNo: s.rafNo || '', fiyat: s.fiyat, satisFiyati: s.satisFiyati, kdvOrani: s.kdvOrani, agirlik: s.agirlik, miktar: s.miktar, minMiktar: s.minMiktar, tedarikciId: s.tedarikciId || null, tedarikciStokKodu: s.tedarikciStokKodu || '', tedarikciFiyat: s.tedarikciFiyat || null, maliyetYontemi: s.maliyetYontemi || 'ORTALAMA', aciklama: s.aciklama || '' }
  formTemizle()
  showDialog.value = true
}

const saveStok = async () => {
  if (!form.value.ad.trim()) { toastBildirim.uyari('Ürün adı giriniz'); return }
  saving.value = true
  try {
    if (editingId.value) { await stokStore.updateStok(editingId.value, form.value); toastBildirim.basarili('Ürün güncellendi') }
    else { await stokStore.addStok(form.value); toastBildirim.basarili('Ürün eklendi') }
    formTemizle()
    showDialog.value = false
  } catch (err) { toastBildirim.hata(err?.response?.data?.message || err?.message || 'İşlem başarısız') }
  finally { saving.value = false }
}

const confirmDel = (id) => {
  confirm.require({
    message: 'Bu ürünü silmek istediğinizden emin misiniz?', header: 'Onay',
    icon: 'pi pi-exclamation-triangle',
    accept: async () => {
      try { await stokStore.deleteStok(id); if (seciliStokId.value === id) { seciliStok.value = null; seciliStokId.value = null; stokHareketler.value = [] } toastBildirim.basarili('Ürün silindi') }
      catch (err) { toastBildirim.hata(err?.response?.data?.message || err?.message || 'Silme başarısız') }
    }
  })
}

const openHareketDialog = (tur) => {
  hareketTur.value = tur; hareketForm.value = { miktar: null, hareketTarihi: new Date(), cariHesapId: null, aciklama: '' }
  showHareketDialog.value = true
}

const batchSil = () => {
  if (!seciliStoklar.value.length) return
  confirm.require({
    message: `${seciliStoklar.value.length} ürün silinecek. Emin misiniz?`, header: 'Toplu Silme Onayı',
    icon: 'pi pi-exclamation-triangle',
    accept: async () => {
      let basarili = 0, hatali = 0
      for (const s of [...seciliStoklar.value]) {
        try { await stokStore.deleteStok(s.id); basarili++ } catch { hatali++ }
      }
      seciliStoklar.value = []
      toast.add({ severity: hatali ? 'warn' : 'success', summary: 'Tamamlandı', detail: `${basarili} silindi${hatali ? ', ' + hatali + ' hata' : ''}`, life: 5000 })
    }
  })
}

const batchCsvExport = () => {
  if (!seciliStoklar.value.length) return
  const kolonlar = ['ad', 'stokKodu', 'barkod', 'birim', 'fiyat', 'miktar', 'minMiktar']
  const baslik = kolonlar.join(';')
  const satirlar = seciliStoklar.value.map(s => kolonlar.map(k => s[k] ?? '').join(';'))
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
  if (!hareketForm.value.miktar || hareketForm.value.miktar <= 0) { toastBildirim.uyari('Geçerli miktar giriniz'); return }
  saving.value = true
  try {
    await stokAPI.addHareket(seciliStokId.value, {
      tur: hareketTur.value, miktar: hareketForm.value.miktar,
      hareketTarihi: hareketForm.value.hareketTarihi.toISOString().split('T')[0],
      cariHesapId: hareketForm.value.cariHesapId, aciklama: hareketForm.value.aciklama
    })
    const [hr, sr] = await Promise.all([stokAPI.getHareketler(seciliStokId.value), stokStore.getAll({ size: 1000 })])
    stokHareketler.value = hr.data; seciliStok.value = sr.find(s => s.id === seciliStokId.value)
    showHareketDialog.value = false; toastBildirim.basarili('Hareket eklendi')
  } catch (err) { toastBildirim.hata(err?.response?.data?.message || err?.message || 'İşlem başarısız') }
  finally { saving.value = false }
}

const delHareket = async (id) => {
  try {
    await stokAPI.deleteHareket(id)
    const [hr, sr] = await Promise.all([stokAPI.getHareketler(seciliStokId.value), stokStore.getAll({ size: 1000 })])
    stokHareketler.value = hr.data; seciliStok.value = sr.find(s => s.id === seciliStokId.value)
    toastBildirim.basarili('Hareket silindi')
  } catch (err) { toastBildirim.hata(err?.response?.data?.message || err?.message || 'Silme başarısız') }
}

const batchFiyatDialog = ref(false)
const batchLoading = ref(false)
const batchFiyatForm = ref({ oran: 0, yon: 'ARTIR', kategori: '', stokGrubu: '' })

const batchFiyatUygula = async () => {
  batchLoading.value = true
  let guncellenen = 0
  for (const s of stokStore.stoklar) {
    if (batchFiyatForm.value.kategori && s.kategori !== batchFiyatForm.value.kategori) continue
    if (batchFiyatForm.value.stokGrubu && s.stokGrubu !== batchFiyatForm.value.stokGrubu) continue
    const oran = batchFiyatForm.value.yon === 'ARTIR' ? (1 + batchFiyatForm.value.oran / 100) : (1 - batchFiyatForm.value.oran / 100)
    try {
      await stokAPI.update(s.id, { ...s, fiyat: Math.round(s.fiyat * oran * 100) / 100 })
      guncellenen++
    } catch (e) { /* skip */ }
  }
  await stokStore.getAll({ size: 1000 })
  batchFiyatDialog.value = false
  toastBildirim.basarili(`${guncellenen} ürün güncellendi`)
  batchLoading.value = false
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
  } catch { /* silent */ }
}

const formatCurrency = (v) => v ?? 0 ? new Intl.NumberFormat('tr-TR',{style:'currency',currency:'TRY'}).format(v) : '0,00 ₺'
const formatDate = (d) => d ? new Intl.DateTimeFormat('tr-TR',{year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date(d)) : '-'
</script>

<style scoped>
.stoklar-container { padding: 20px; }
h1 { color: var(--text-primary); margin-bottom: 20px; font-size: 28px; font-weight: 700; letter-spacing: -0.5px; }
h2 { color: var(--text-primary); font-size: 20px; margin: 0; }
.toolbar { margin-bottom: 20px; background: var(--bg-card); border: 1px solid var(--border); border-radius: 12px; padding: 14px 18px; }
.toolbar-end { display: flex; align-items: center; gap: 8px; }
.table-header { display: flex; align-items: center; gap: 12px; padding: 4px 0; }
.toplam-bilgi { color: var(--text-secondary); font-size: 13px; }
.kritik-bilgi { color: #f87171; font-size: 13px; display: flex; align-items: center; gap: 4px; }
.loading { text-align: center; padding: 40px; color: var(--text-secondary); }
.stok-kartlar { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 15px; margin-bottom: 30px; }
.stok-kart { background: var(--bg-card); border: 1px solid var(--border); border-radius: 14px; padding: 18px; cursor: pointer; transition: all 0.3s ease; }
.stok-kart:hover { transform: translateY(-3px); box-shadow: 0 8px 24px rgba(0,0,0,0.3); border-color: rgba(59,130,246,0.25); }
.stok-kart.dusuk-stok { border-color: rgba(239,68,68,0.3); }
.stok-kart.dusuk-stok:hover { border-color: rgba(239,68,68,0.5); }
.kart-ust { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.stok-kod { font-size: 11px; color: var(--text-muted); font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; }
.uyari-eti { font-size: 11px; color: #f87171; background: rgba(239,68,68,0.15); padding: 2px 8px; border-radius: 6px; display: flex; align-items: center; gap: 3px; }
.stok-kart h3 { margin: 0 0 12px; font-size: 15px; color: var(--text-primary); }
.kart-bilgi { display: flex; gap: 15px; margin-bottom: 14px; }
.bilgi-item { flex: 1; }
.bilgi-label { display: block; font-size: 11px; color: var(--text-muted); margin-bottom: 3px; text-transform: uppercase; }
.bilgi-deger { font-size: 16px; font-weight: 700; }
.bilgi-deger.normal { color: #4ade80; }
.bilgi-deger.kritik { color: #f87171; }
.kart-islem { display: flex; gap: 8px; justify-content: flex-end; padding-top: 12px; border-top: 1px solid var(--border); }
.hareket-bolumu { background: var(--bg-card); border: 1px solid var(--border); border-radius: 14px; padding: 20px; }
.hareket-header { display: flex; align-items: center; gap: 15px; margin-bottom: 18px; flex-wrap: wrap; }
.table-container { overflow-x: auto; }
.form-grup { margin-bottom: 18px; }
.form-grup label { display: block; margin-bottom: 6px; font-weight: 600; color: var(--text-secondary); font-size: 12px; text-transform: uppercase; letter-spacing: 0.5px; }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; }
.form-row .flex-2 { grid-column: span 2; }
.form-section { margin-bottom: 24px; }
.form-section:last-child { margin-bottom: 0; }
.form-section-title {
  font-size: 13px; font-weight: 600; color: var(--text-secondary);
  text-transform: uppercase; letter-spacing: 0.5px;
  margin-bottom: 16px; padding-bottom: 8px;
  border-bottom: 1px solid var(--border);
}
.badge { padding: 4px 10px; border-radius: 20px; font-size: 11px; font-weight: 700; }
.badge.giris { background: rgba(34,197,94,0.15); color: #4ade80; }
.badge.cikis { background: rgba(239,68,68,0.15); color: #f87171; }
.positive { color: #4ade80; font-weight: 700; }
.negative { color: #f87171; font-weight: 700; }
.w-full { width: 100% !important; }
.full-width { grid-column: 1/-1; }
.filter-bar { display: flex; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; align-items: center; }
.filter-input { width: 150px !important; }
.filter-input-sm { width: 120px !important; }
.filter-dropdown { width: 160px !important; }
.batch-actions {
  display: inline-flex; align-items: center; gap: 8px;
  margin-left: 12px; padding-left: 12px;
  border-left: 1px solid var(--border);
}
.batch-count {
  font-size: 12px; color: #60a5fa; font-weight: 600;
}
</style>