<template>
  <div class="cari-hesaplar-container">
    <h1>Cari Hesaplar Yönetimi</h1>

    <Toolbar class="toolbar">
      <template #start>
        <Button 
          label="Yeni Cari Hesap" 
          icon="pi pi-plus"
          @click="openDialog"
          class="p-button-success"
        />
        <div v-if="selectedCariHesaplar.length > 0" class="batch-actions">
          <span class="batch-count">{{ selectedCariHesaplar.length }} seçili</span>
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
        <TabloAyarlari tablo-key="cari" :kolonlar="kolonlar" @update:yogunluk="tabloYogunluk = $event" />
        <Button label="Excel" icon="pi pi-file-excel" class="p-button-sm p-button-outlined" style="margin-right:4px" @click="excelIndir" />
        <Button 
          label="CSV" 
          icon="pi pi-download"
          @click="csvExport"
          class="p-button-sm p-button-outlined"
          style="margin-right: 8px"
        />
        <span class="p-input-icon-left">
          <i class="pi pi-search" />
          <InputText 
            v-model="aramaMetni" 
            placeholder="Cari ara (Ctrl+F)..." 
            @input="ara"
          />
        </span>
      </template>
    </Toolbar>

    <div class="loading" v-if="loading">
      <p><i class="pi pi-spin pi-spinner"></i> Yükleniyor...</p>
    </div>

    <div class="table-container" v-if="!loading">
      <DataTable
        :value="cariHesapStore.cariHesaplar"
        v-model:selection="selectedCariHesaplar"
        selection-mode="multiple"
        data-key="id"
        responsive-layout="scroll"
        striped-rows
        :size="tabloYogunluk === 'compact' ? 'small' : 'normal'"
        :rows="10"
        :paginator="true"
        paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport RowsPerPageDropdown"
        :rows-per-page-options="[10, 20, 50]"
        current-page-report-template="{first} - {last} ({totalRecords} kayıt)"
      >
        <Column selection-mode="multiple" headerStyle="width: 3rem"></Column>
        <Column v-if="kolonlar[0].visible" field="id" header="ID" style="width: 60px"></Column>
        <Column v-if="kolonlar[1].visible" field="ad" header="Adı" sortable style="width: 200px"></Column>
        <Column v-if="kolonlar[2].visible" field="tur" header="Tür" style="width: 100px">
          <template #body="s">
            <Tag :value="s.data.tur || '-'" :severity="s.data.tur === 'Musteri' ? 'info' : s.data.tur === 'Tedarikci' ? 'warn' : 'secondary'" />
          </template>
        </Column>
        <Column v-if="kolonlar[3].visible" field="yetkiliKisi" header="Yetkili" style="width: 130px"></Column>
        <Column v-if="kolonlar[4].visible" field="telefon" header="Telefon" style="width: 130px">
          <template #body="s">
            <span v-if="s.data.telefon" class="kopyalanabilir" @click="kopyala(s.data.telefon, 'Telefon Kopyalandı')">
              {{ s.data.telefon }} <i class="pi pi-copy kopyala-ikon"></i>
            </span>
            <span v-else>-</span>
          </template>
        </Column>
        <Column v-if="kolonlar[5].visible" field="krediLimiti" header="Kredi Limiti" style="width: 120px">
          <template #body="s">{{ s.data.krediLimiti ? formatCurrency(s.data.krediLimiti) : '-' }}</template>
        </Column>
        <Column v-if="kolonlar[6].visible" field="odemeVadesi" header="Vade (Gün)" style="width: 80px"></Column>
        <Column v-if="kolonlar[7].visible" field="bakiye" header="Bakiye" style="width: 120px">
          <template #body="slotProps">
            <span :class="slotProps.data.bakiye >= 0 ? 'positive' : 'negative'">
              {{ formatCurrency(slotProps.data.bakiye) }}
            </span>
          </template>
        </Column>
        <Column header="İşlemler" style="width: 190px">
          <template #body="slotProps">
            <Button 
              icon="pi pi-pencil"
              class="p-button-rounded p-button-info p-button-sm"
              @click="editCariHesap(slotProps.data)"
              title="Düzenle"
            />
            <Button 
              icon="pi pi-copy"
              class="p-button-rounded p-button-secondary p-button-sm"
              @click="kopyalaCari(slotProps.data)"
              title="Kopyala (yeni kayıt için şablon)"
            />
            <Button 
              icon="pi pi-list"
              class="p-button-rounded p-button-warning p-button-sm"
              @click="viewHareketler(slotProps.data)"
              title="Hareketleri Göster"
            />
            <Button 
              icon="pi pi-trash"
              class="p-button-rounded p-button-danger p-button-sm"
              @click="confirmDelete(slotProps.data.id)"
              title="Sil"
            />
          </template>
        </Column>
      </DataTable>

      <Message v-if="cariHesapStore.cariHesaplar.length === 0" severity="info" text="Cari hesap bulunmamaktadır." />
    </div>

    <!-- Cari Hesap Dialog -->
    <Dialog 
      v-model:visible="showDialog"
      :header="editingId ? 'Cari Hesap Düzenle' : 'Yeni Cari Hesap'"
      :modal="true"
      style="width: 650px"
      :draggable="false"
    >
      <div class="dialog-form">
        <div class="form-section">
          <div class="form-section-title">Genel Bilgiler</div>
          <div class="form-group">
            <label for="ad">Cari Adı <span class="required">*</span></label>
            <InputText v-model="form.ad" id="ad" placeholder="Müşteri veya tedarikçi adı" class="w-full" />
          </div>
          <div class="form-row">
            <div class="form-group">
              <label for="tur">Cari Türü</label>
              <Dropdown v-model="form.tur" :options="['Musteri','Tedarikci','Her Ikisi']" placeholder="Seçiniz" class="w-full" />
            </div>
            <div class="form-group">
              <label for="vergiNumarasi">Vergi No / TC Kimlik</label>
              <InputText v-model="form.vergiNumarasi" id="vergiNumarasi" placeholder="Vergi veya TC kimlik no" class="w-full" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label for="telefon">Telefon</label>
              <InputText v-model="form.telefon" id="telefon" placeholder="05XX XXX XX XX" class="w-full" />
            </div>
            <div class="form-group">
              <label for="email">E-posta</label>
              <InputText v-model="form.email" id="email" placeholder="ornek@firma.com" class="w-full" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label for="vergiDairesi">Vergi Dairesi</label>
              <InputText v-model="form.vergiDairesi" id="vergiDairesi" placeholder="Bağlı olunan vergi dairesi" class="w-full" />
            </div>
            <div class="form-group">
              <label for="iban">IBAN</label>
              <InputText v-model="form.iban" id="iban" placeholder="TR..." class="w-full" />
            </div>
          </div>
        </div>

        <div class="form-section">
          <div class="form-section-title">Adres Bilgileri</div>
          <div class="form-row">
            <div class="form-group">
              <label for="il">İl</label>
              <InputText v-model="form.il" id="il" placeholder="İl" class="w-full" />
            </div>
            <div class="form-group">
              <label for="ilce">İlçe</label>
              <InputText v-model="form.ilce" id="ilce" placeholder="İlçe" class="w-full" />
            </div>
          </div>
          <div class="form-group">
            <label for="adres">Adres</label>
            <Textarea v-model="form.adres" id="adres" placeholder="Mahalle, cadde, sokak, no..." rows="2" class="w-full" />
          </div>
        </div>

        <div class="form-section">
          <div class="form-section-title">Yetkili Kişi</div>
          <div class="form-row">
            <div class="form-group">
              <label for="yetkiliKisi">Ad Soyad</label>
              <InputText v-model="form.yetkiliKisi" id="yetkiliKisi" placeholder="Yetkili kişi adı soyadı" class="w-full" />
            </div>
            <div class="form-group">
              <label for="yetkiliTelefon">Telefon</label>
              <InputText v-model="form.yetkiliTelefon" id="yetkiliTelefon" placeholder="0XXX XXX XX XX" class="w-full" />
            </div>
          </div>
        </div>

        <div class="form-section">
          <div class="form-section-title">Kredi & Vade</div>
          <div class="form-row">
            <div class="form-group">
              <label for="krediLimiti">Kredi Limiti (TL)</label>
              <InputNumber v-model="form.krediLimiti" id="krediLimiti" :min="0" :min-fraction-digits="2" class="w-full" />
            </div>
            <div class="form-group">
              <label for="odemeVadesi">Ödeme Vadesi (Gün)</label>
              <InputNumber v-model="form.odemeVadesi" id="odemeVadesi" :min="0" :min-fraction-digits="0" class="w-full" />
            </div>
          </div>
        </div>

        <div class="form-section">
          <div class="form-section-title">Ek Bilgiler</div>
          <div class="form-group">
            <label for="notlar">Notlar</label>
            <Textarea v-model="form.notlar" id="notlar" placeholder="Özel notlar..." rows="3" class="w-full" />
          </div>
          <div class="form-group" v-if="editingId">
            <label>Aktif</label>
            <InputSwitch v-model="form.aktif" />
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <Button 
            label="İptal" 
            icon="pi pi-times"
            @click="closeDialog"
            class="p-button-text"
          />
          <Button 
            :label="editingId ? 'Güncelle' : 'Kaydet'" 
            icon="pi pi-check"
            @click="saveCariHesap"
            :loading="saving"
          />
        </div>
      </template>
    </Dialog>

    <!-- Hareketler Dialog -->
    <Dialog 
      v-model:visible="showHareketlerDialog"
      header="Cari Hesap Hareketleri"
      :modal="true"
      style="width: 800px"
    >
      <div class="hareket-info">
        <h3>{{ selectedCariHesap?.ad }}</h3>
        <p>Bakiye: <strong :class="selectedCariHesap?.bakiye >= 0 ? 'positive' : 'negative'">{{ formatCurrency(selectedCariHesap?.bakiye) }}</strong></p>
      </div>

      <div class="table-container">
        <DataTable
          :value="hareketler"
          responsive-layout="scroll"
          striped-rows
          :rows="10"
          :paginator="true"
        >
          <Column field="tur" header="Tür" style="width: 100px">
            <template #body="slotProps">
              <span :class="['badge', slotProps.data.tur === 'TAHSILAT' ? 'tahsilat' : 'odeme']">
                {{ slotProps.data.tur === 'TAHSILAT' ? 'Tahsilat' : 'Ödeme' }}
              </span>
            </template>
          </Column>
          <Column field="tutar" header="Tutar" style="width: 120px">
            <template #body="slotProps">
              <span :class="slotProps.data.tur === 'TAHSILAT' ? 'positive' : 'negative'">
                {{ formatCurrency(slotProps.data.tutar) }}
              </span>
            </template>
          </Column>
          <Column field="hareketTarihi" header="Tarih" style="width: 120px">
            <template #body="slotProps">
              {{ formatDate(slotProps.data.hareketTarihi) }}
            </template>
          </Column>
          <Column field="aciklama" header="Açıklama"></Column>
        </DataTable>

        <Message v-if="hareketler.length === 0" severity="info" text="Bu cari hesaba ait hareket bulunmamaktadır." />
      </div>

      <template #footer>
        <Button 
          label="Kapat" 
          icon="pi pi-times"
          @click="showHareketlerDialog = false"
        />
      </template>
    </Dialog>

    <Message v-if="cariHesapStore.error" severity="error" :text="cariHesapStore.error" />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import { useCariHesapStore } from '../stores/cariHesapStore.js'
import { useHareketStore } from '../stores/hareketStore.js'
import { excelAPI } from '../api/index.js'
import { useKisayollar } from '../composables/useKisayollar.js'
import { usePanoyaKopyala } from '../composables/usePanoyaKopyala.js'
import TabloAyarlari from '../components/TabloAyarlari.vue'

const toast = useToast()
const confirm = useConfirm()
const cariHesapStore = useCariHesapStore()
const hareketStore = useHareketStore()
const { kopyala } = usePanoyaKopyala()

const tabloYogunluk = ref('comfortable')
const kolonlar = ref([
  { field: 'id', header: 'ID', visible: true },
  { field: 'ad', header: 'Adı', visible: true },
  { field: 'tur', header: 'Tür', visible: true },
  { field: 'yetkiliKisi', header: 'Yetkili', visible: true },
  { field: 'telefon', header: 'Telefon', visible: true },
  { field: 'krediLimiti', header: 'Kredi Limiti', visible: true },
  { field: 'odemeVadesi', header: 'Vade (Gün)', visible: true },
  { field: 'bakiye', header: 'Bakiye', visible: true }
])

useKisayollar({
  yeni: () => openDialog(),
  iptal: () => { showDialog.value = false },
  kaydet: () => saveCariHesap()
})

const showDialog = ref(false)
const showHareketlerDialog = ref(false)
const loading = ref(false)
const saving = ref(false)
const editingId = ref(null)
const hareketler = ref([])
const selectedCariHesaplar = ref([])
const selectedCariHesap = ref(null)
const aramaMetni = ref('')
let aramaZamanlayici = null
onUnmounted(() => { if (aramaZamanlayici) clearTimeout(aramaZamanlayici) })

const submitted = ref(false)
const form = ref({
  ad: '', tur: '', vergiNumarasi: '', vergiDairesi: '',
  telefon: '', email: '', iban: '',
  il: '', ilce: '', adres: '',
  yetkiliKisi: '', yetkiliTelefon: '',
  krediLimiti: null, odemeVadesi: 0,
  notlar: '', aktif: true
})

onMounted(async () => {
  await loadCariHesaplar()
})

const loadCariHesaplar = async () => {
  loading.value = true
  try {
    await cariHesapStore.getAllCariHesaplar()
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Hata', detail: 'Cari hesaplar yüklenirken hata oluştu', life: 5000 })
  } finally {
    loading.value = false
  }
}

const ara = () => {
  if (aramaZamanlayici) clearTimeout(aramaZamanlayici)
  aramaZamanlayici = setTimeout(async () => {
    if (!aramaMetni.value.trim()) {
      await cariHesapStore.getAllCariHesaplar()
    } else {
      await cariHesapStore.ara(aramaMetni.value)
    }
  }, 300)
}

const openDialog = () => {
  editingId.value = null
  form.value = { ad: '', tur: '', vergiNumarasi: '', vergiDairesi: '', telefon: '', email: '', iban: '', il: '', ilce: '', adres: '', yetkiliKisi: '', yetkiliTelefon: '', krediLimiti: null, odemeVadesi: 0, notlar: '', aktif: true }
  submitted.value = false
  showDialog.value = true
}

const kopyalaCari = (cari) => {
  editingId.value = null
  form.value = { ...cari, id: undefined }
  submitted.value = false
  showDialog.value = true
  toast.add({ severity: 'info', summary: 'Kopyalandı', detail: 'Yeni kayıt için şablon oluşturuldu. Kaydetmeden önce bilgileri güncelleyin.', life: 4000 })
}

const closeDialog = () => {
  showDialog.value = false
  submitted.value = false
}

const editCariHesap = (cariHesap) => {
  editingId.value = cariHesap.id
  form.value = {
    ad: cariHesap.ad, tur: cariHesap.tur || '',
    vergiNumarasi: cariHesap.vergiNumarasi || '', vergiDairesi: cariHesap.vergiDairesi || '',
    telefon: cariHesap.telefon || '', email: cariHesap.email || '', iban: cariHesap.iban || '',
    il: cariHesap.il || '', ilce: cariHesap.ilce || '', adres: cariHesap.adres || '',
    yetkiliKisi: cariHesap.yetkiliKisi || '', yetkiliTelefon: cariHesap.yetkiliTelefon || '',
    krediLimiti: cariHesap.krediLimiti || null, odemeVadesi: cariHesap.odemeVadesi ?? 0,
    notlar: cariHesap.notlar || '', aktif: cariHesap.aktif !== false
  }
  submitted.value = false
  showDialog.value = true
}

const saveCariHesap = async () => {
  submitted.value = true
  if (!form.value.ad.trim()) {
    toast.add({ severity: 'warn', summary: 'Uyarı', detail: 'Cari adı boş olamaz', life: 5000 })
    return
  }

  saving.value = true
  try {
    if (editingId.value) {
      await cariHesapStore.updateCariHesap(editingId.value, form.value)
      toast.add({ severity: 'success', summary: 'Başarılı', detail: 'Cari hesap güncellendi', life: 5000 })
    } else {
      await cariHesapStore.addCariHesap(form.value)
      toast.add({ severity: 'success', summary: 'Başarılı', detail: 'Cari hesap oluşturuldu', life: 5000 })
    }
    closeDialog()
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Hata', detail: 'İşlem başarısız oldu', life: 5000 })
  } finally {
    saving.value = false
  }
}

const confirmDelete = (id) => {
  confirm.require({
    message: 'Bu cari hesabı silmek istediğinizden emin misiniz?',
    header: 'Onay',
    icon: 'pi pi-exclamation-triangle',
    accept: () => deleteCariHesap(id),
    reject: () => {}
  })
}

const deleteCariHesap = async (id) => {
  try {
    await cariHesapStore.deleteCariHesap(id)
    toast.add({ severity: 'success', summary: 'Başarılı', detail: 'Cari hesap silindi', life: 5000 })
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Hata', detail: 'Cari hesap silinirken hata oluştu', life: 5000 })
  }
}

const batchSil = () => {
  if (selectedCariHesaplar.value.length === 0) return
  confirm.require({
    message: `${selectedCariHesaplar.value.length} cari hesap silinecek. Emin misiniz?`,
    header: 'Toplu Silme Onayı',
    icon: 'pi pi-exclamation-triangle',
    accept: async () => {
      const sonuclar = await Promise.allSettled(
        [...selectedCariHesaplar.value].map(c =>
          cariHesapStore.deleteCariHesap(c.id)
        )
      )
      sonuclar.forEach(r => {
        if (r.status === 'rejected') {
          toast.add({ severity: 'error', summary: 'Hata', detail: r.reason?.response?.data?.message || r.reason?.message || 'Cari hesap silinirken hata oluştu', life: 5000 })
        }
      })
      selectedCariHesaplar.value = []
      await loadCariHesaplar()
      toast.add({ severity: 'success', summary: 'Başarılı', detail: 'Seçili cari hesaplar silindi', life: 5000 })
    }
  })
}

const batchCsvExport = () => {
  const ids = selectedCariHesaplar.value.map(c => c.id).join(',')
  window.open(`/api/cari-hesaplar/export/csv?ids=${ids}`, '_blank')
}

const viewHareketler = async (cariHesap) => {
  selectedCariHesap.value = cariHesap
  try {
    const data = await hareketStore.getHareketlerByCariHesap(cariHesap.id)
    hareketler.value = data
    showHareketlerDialog.value = true
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Hata', detail: 'Hareketler yüklenirken hata oluştu', life: 5000 })
  }
}

const csvExport = () => {
  window.open('/api/cari-hesaplar/export/csv', '_blank')
}

const excelIndir = async () => {
  try {
    const res = await excelAPI.cariHesaplar()
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', 'CariHesaplar.xlsx')
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch { /* silent */ }
}

const formatCurrency = (value) => {
  if (value === null || value === undefined) return '0,00 ₺'
  return new Intl.NumberFormat('tr-TR', {
    style: 'currency',
    currency: 'TRY'
  }).format(value)
}

const formatDate = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return new Intl.DateTimeFormat('tr-TR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).format(date)
}
</script>

<style scoped>
.cari-hesaplar-container {
  padding: 20px;
}

h1 {
  color: var(--text-primary);
  margin-bottom: 20px;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.5px;
}

h3 {
  color: var(--text-primary);
  margin: 0 0 10px 0;
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
  font-size: 16px;
}

.dialog-form { padding: 0; }
.form-section { margin-bottom: 20px; }
.form-section:last-child { margin-bottom: 0; }
.form-section-title {
  font-size: 13px; font-weight: 600; color: var(--text-secondary);
  text-transform: uppercase; letter-spacing: 0.5px;
  margin-bottom: 16px; padding-bottom: 8px;
  border-bottom: 1px solid var(--border);
}
.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-weight: 600;
  color: #cbd5e1;
  font-size: 13px;
}
.form-group .required { color: #f87171; }

.form-group :deep(.p-inputtext),
.form-group :deep(.p-textarea) {
  width: 100%;
  background: var(--bg-primary) !important;
  border: 1px solid var(--border) !important;
  border-radius: 8px;
  padding: 10px 12px !important;
  color: var(--text-primary) !important;
  font-size: 14px;
}
.form-group :deep(.p-inputtext:enabled:focus),
.form-group :deep(.p-textarea:enabled:focus) {
  border-color: #3b82f6 !important;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15) !important;
}
.form-group :deep(.p-inputtext.p-invalid),
.form-group :deep(.p-textarea.p-invalid) { border-color: #f87171 !important; }

.error { display: block; color: #f87171; font-size: 11px; margin-top: 4px; }
.dialog-footer { display: flex; justify-content: flex-end; gap: 8px; }

.positive {
  color: #4caf50;
  font-weight: bold;
}

.negative {
  color: #f44336;
  font-weight: bold;
}

.badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: bold;
}

.badge.tahsilat {
  background-color: #e8f5e9;
  color: #2e7d32;
}

.badge.odeme {
  background-color: #ffebee;
  color: #c62828;
}

.hareket-info {
  background: #f5f5f5;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 15px;
}

.hareket-info p {
  margin: 5px 0 0 0;
}

.w-full {
  width: 100% !important;
}

.batch-actions {
  display: inline-flex; align-items: center; gap: 8px;
  margin-left: 12px; padding-left: 12px;
  border-left: 1px solid var(--border);
}
.batch-count {
  font-size: 12px; color: #60a5fa; font-weight: 600;
}
.kopyalanabilir { cursor: pointer; display: inline-flex; align-items: center; gap: 6px; }
.kopyalanabilir:hover { color: var(--accent); }
.kopyala-ikon { font-size: 11px; opacity: 0.5; }
.kopyalanabilir:hover .kopyala-ikon { opacity: 1; }
</style>
