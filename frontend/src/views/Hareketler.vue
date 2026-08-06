<template>
  <div class="hareketler-container">
    <h1>Cari Hareket Yönetimi</h1>

    <Toolbar class="toolbar">
      <template #start>
        <Button 
          label="Yeni Hareket" 
          icon="pi pi-plus"
          class="p-button-success"
          @click="openDialog"
        />
      </template>
      <template #end>
        <TarihHizliSecim v-model="tarihAraligi" />
        <Button
          label="Excel"
          icon="pi pi-file-excel"
          class="p-button-sm p-button-outlined"
          style="margin-right:4px"
          @click="excelIndir"
        />
        <Button 
          label="CSV" 
          icon="pi pi-download"
          class="p-button-sm p-button-outlined"
          style="margin-right: 8px"
          @click="csvExport"
        />
        <DatePicker 
          v-model="filtreBaslangic" 
          placeholder="Başlangıç" 
          date-format="dd.mm.yy"
          class="filter-date"
          @update:model-value="filtrele"
        />
        <DatePicker 
          v-model="filtreBitis" 
          placeholder="Bitiş" 
          date-format="dd.mm.yy"
          class="filter-date"
          @update:model-value="filtrele"
        />
        <Button 
          v-if="filtreBaslangic || filtreBitis"
          icon="pi pi-times"
          class="p-button-rounded p-button-text p-button-sm"
          title="Filtreyi Temizle"
          @click="filtreTemizle"
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
      <DataTable
        :value="tümHareketler"
        responsive-layout="scroll"
        striped-rows
        :rows="10"
        :paginator="true"
        paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport RowsPerPageDropdown"
        :rows-per-page-options="[10, 20, 50]"
        current-page-report-template="{first} - {last} ({totalRecords} kayıt)"
      >
        <Column
          field="cariHesapAd"
          header="Cari Hesap"
          style="width: 200px"
        />
        <Column
          field="tur"
          header="Tür"
          style="width: 100px"
        >
          <template #body="slotProps">
            <span :class="['badge', String(typeof slotProps.data.tur === 'object' ? slotProps.data.tur?.value : slotProps.data.tur).toUpperCase() === 'TAHSILAT' ? 'tahsilat' : 'odeme']">
              {{ String(typeof slotProps.data.tur === 'object' ? slotProps.data.tur?.value : slotProps.data.tur).toUpperCase() === 'TAHSILAT' ? 'Tahsilat' : 'Ödeme' }}
            </span>
          </template>
        </Column>
        <Column
          field="odemeSekli"
          header="Ödeme Şekli"
          style="width: 120px"
        >
          <template #body="slotProps">
            <span v-if="slotProps.data.odemeSekli">{{ odemeSekliLabel(slotProps.data.odemeSekli) }}</span>
            <span
              v-else
              class="muted"
            >-</span>
          </template>
        </Column>
        <Column
          field="tutar"
          header="Tutar"
          style="width: 120px"
        >
          <template #body="slotProps">
            <span :class="slotProps.data.tur === 'TAHSILAT' ? 'positive' : 'negative'">
              {{ formatCurrency(slotProps.data.tutar) }}
            </span>
          </template>
        </Column>
        <Column
          field="hareketTarihi"
          header="Tarih"
          style="width: 120px"
        >
          <template #body="slotProps">
            {{ formatDate(slotProps.data.hareketTarihi) }}
          </template>
        </Column>
        <Column
          field="aciklama"
          header="Açıklama"
        />
        <Column
          header="İşlemler"
          style="width: 140px"
        >
          <template #body="slotProps">
            <Button 
              icon="pi pi-pencil"
              class="p-button-rounded p-button-info p-button-sm"
              title="Düzenle"
              style="margin-right: 6px"
              @click="openEditDialog(slotProps.data)"
            />
            <Button 
              icon="pi pi-trash"
              class="p-button-rounded p-button-danger p-button-sm"
              title="Sil"
              @click="confirmDelete(slotProps.data.id)"
            />
          </template>
        </Column>
      </DataTable>

      <EmptyState
        v-if="tümHareketler.length === 0"
        message="Henüz hareket bulunamadı"
        sub-message="İlk hareketinizi eklemek için Yeni Hareket butonuna tıklayın"
        icon="pi pi-exchange"
        action-label="Yeni Hareket"
        action-icon="pi pi-plus"
        @action="openDialog"
      />
    </div>

    <!-- Hareket Ekleme/Düzenleme Dialog -->
    <Dialog 
      v-model:visible="showDialog"
      :header="editingId ? 'Hareket Düzenle' : 'Yeni Hareket Ekle'"
      :modal="true"
      style="width: 500px"
    >
      <div class="form-group">
        <label for="cariHesapId">Cari Hesap *</label>
        <Dropdown 
          id="cariHesapId"
          v-model="form.cariHesapId"
          :options="cariHesapSeçenekleri"
          option-label="ad"
          option-value="id"
          placeholder="Cari hesap seçiniz"
          class="w-full"
        />
      </div>

      <div class="form-group">
        <label for="tur">Hareket Türü *</label>
        <Dropdown 
          id="tur"
          v-model="form.tur"
          :options="hareketTurleri"
          option-label="label"
          option-value="value"
          placeholder="Hareket türü seçiniz"
          class="w-full"
        />
      </div>

      <div class="form-group">
        <label for="odemeSekli">Ödeme Şekli</label>
        <Dropdown 
          id="odemeSekli"
          v-model="form.odemeSekli"
          :options="odemeSekliSecenekleri"
          option-label="label"
          option-value="value"
          placeholder="Ödeme şekli seçiniz"
          class="w-full"
        />
      </div>

      <div class="form-group">
        <label for="tutar">Tutar *</label>
        <InputNumber 
          id="tutar"
          v-model="form.tutar"
          :use-grouping="false"
          :min-fraction-digits="2"
          :max-fraction-digits="2"
          placeholder="0,00"
          class="w-full"
        />
      </div>

      <div class="form-group">
        <label for="hareketTarihi">Hareket Tarihi *</label>
        <DatePicker 
          id="hareketTarihi"
          v-model="form.hareketTarihi"
          date-format="dd.mm.yy"
          class="w-full"
        />
      </div>

      <div class="form-group">
        <label for="aciklama">Açıklama</label>
        <Textarea 
          id="aciklama"
          v-model="form.aciklama"
          placeholder="Hareket açıklamasını giriniz"
          rows="3"
          class="w-full"
        />
      </div>

      <template #footer>
        <Button 
          label="İptal" 
          icon="pi pi-times"
          class="p-button-text"
          @click="closeDialog"
        />
        <Button 
          label="Kaydet" 
          icon="pi pi-check"
          :loading="saving"
          @click="saveHareket"
        />
      </template>
    </Dialog>

    <Message
      v-if="error"
      severity="error"
      :text="error"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { useCariHesapStore } from '../stores/cariHesapStore.js'
import { useHareketStore } from '../stores/hareketStore.js'
import { hareketAPI, excelAPI } from '../api/index.js'
import EmptyState from '../components/EmptyState.vue'
import TarihHizliSecim from '../components/TarihHizliSecim.vue'

const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const cariHesapStore = useCariHesapStore()
const hareketStore = useHareketStore()

const showDialog = ref(false)
const loading = ref(false)
const saving = ref(false)
const error = ref(null)
const editingId = ref(null)
const tümHareketler = ref([])
const filtreBaslangic = ref(null)
const filtreBitis = ref(null)
const tarihAraligi = ref(null)
let aramaZaman = null
onUnmounted(() => { if (aramaZaman) clearTimeout(aramaZaman) })

watch(tarihAraligi, (v) => {
  if (v && v.length === 2 && v[0] && v[1]) {
    filtreBaslangic.value = v[0]
    filtreBitis.value = v[1]
    filtrele()
  } else if (!v || v.length === 0) {
    filtreTemizle()
  }
})

const form = ref({
  cariHesapId: null,
  tur: '',
  odemeSekli: null,
  tutar: null,
  hareketTarihi: new Date(),
  aciklama: ''
})

const hareketTurleri = [
  { label: 'Tahsilat', value: 'TAHSILAT' },
  { label: 'Ödeme', value: 'ODEME' }
]

const odemeSekliSecenekleri = [
  { label: 'Nakit', value: 'NAKIT' },
  { label: 'Kredi Kartı', value: 'KREDI_KARTI' },
  { label: 'Havale/EFT', value: 'HAVALE_EFT' },
  { label: 'Çek', value: 'CEK' },
  { label: 'Senet', value: 'SENET' },
  { label: 'Banka', value: 'BANKA' }
]

const odemeSekliLabel = (val) => {
  if (!val) return '-'
  const code = typeof val === 'object' ? (val.value || val.label) : val
  const item = odemeSekliSecenekleri.find(s => s.value === code || s.label === code)
  return item ? item.label : String(code)
}

const cariHesapSeçenekleri = computed(() => {
  return cariHesapStore.cariHesaplar || []
})

onMounted(async () => {
  await loadData()
})

const loadData = async () => {
  loading.value = true
  try {
    await cariHesapStore.getAllCariHesaplar()
    const hareketler = await hareketStore.getAllHareketler()
    tümHareketler.value = hareketler
  } catch (err) {
    error.value = 'Veriler yüklenirken hata oluştu'
    toastBildirim.hata('Veriler yüklenirken hata oluştu')
  } finally {
    loading.value = false
  }
}

const openDialog = () => {
  editingId.value = null
  form.value = {
    cariHesapId: null,
    tur: '',
    odemeSekli: null,
    tutar: null,
    hareketTarihi: new Date(),
    aciklama: ''
  }
  showDialog.value = true
}

const openEditDialog = (hareket) => {
  editingId.value = hareket.id
  form.value = {
    cariHesapId: hareket.cariHesapId,
    tur: hareket.tur,
    odemeSekli: hareket.odemeSekli || null,
    tutar: hareket.tutar,
    hareketTarihi: new Date(hareket.hareketTarihi),
    aciklama: hareket.aciklama || ''
  }
  showDialog.value = true
}

const closeDialog = () => {
  showDialog.value = false
}

const saveHareket = async () => {
  if (!form.value.cariHesapId) {
    toastBildirim.uyari('Cari hesap seçiniz')
    return
  }

  if (!form.value.tur) {
    toastBildirim.uyari('Hareket türü seçiniz')
    return
  }

  if (!form.value.tutar || form.value.tutar <= 0) {
    toastBildirim.uyari('Geçerli bir tutar giriniz')
    return
  }

  saving.value = true
  try {
    const hareketDTO = {
      cariHesapId: form.value.cariHesapId,
      tur: form.value.tur,
      odemeSekli: form.value.odemeSekli || null,
      tutar: form.value.tutar,
      hareketTarihi: form.value.hareketTarihi ? form.value.hareketTarihi.toISOString().split('T')[0] : null,
      aciklama: form.value.aciklama
    }

    if (editingId.value) {
      await hareketStore.updateHareket(editingId.value, hareketDTO)
      toastBildirim.basarili('Hareket güncellendi')
    } else {
      await hareketStore.addHareket(hareketDTO)
      toastBildirim.basarili('Hareket eklendi')
    }

    tümHareketler.value = await hareketStore.getAllHareketler()
    closeDialog()
  } catch (err) {
    toastBildirim.hata('İşlem başarısız oldu')
  } finally {
    saving.value = false
  }
}

const confirmDelete = (id) => {
  confirm.require({
    message: 'Bu hareketi silmek istediğinizden emin misiniz?',
    header: 'Onay',
    icon: 'pi pi-exclamation-triangle',
    accept: () => deleteHareket(id),
    reject: () => {}
  })
}

const deleteHareket = async (id) => {
  try {
    await hareketStore.deleteHareket(id)
    tümHareketler.value = await hareketStore.getAllHareketler()
    toastBildirim.basarili('Hareket silindi')
  } catch (error) {
    toastBildirim.hata('Hareket silinirken hata oluştu')
  }
}

const filtrele = async () => {
  try {
    const params = {}
    if (filtreBaslangic.value) params.baslangic = filtreBaslangic.value.toISOString().split('T')[0]
    if (filtreBitis.value) params.bitis = filtreBitis.value.toISOString().split('T')[0]
    const response = await hareketAPI.filtrele(params)
    tümHareketler.value = response.data?.content || response.data || []
  } catch (err) {
    toastBildirim.hata('Filtreleme başarısız')
  }
}

const filtreTemizle = async () => {
  filtreBaslangic.value = null
  filtreBitis.value = null
  await loadData()
}

const csvExport = () => {
  window.open('/api/hareketler/export/csv', '_blank')
}

const excelIndir = async () => {
  try {
    const res = await excelAPI.hareketler()
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', 'Hareketler.xlsx')
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
.hareketler-container {
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
  font-size: 16px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: bold;
  color: #333;
}

.form-group :deep(.p-inputtext),
.form-group :deep(.p-dropdown),
.form-group :deep(.p-inputnumber),
.form-group :deep(.p-datepicker),
.form-group :deep(.p-textarea) {
  width: 100%;
}

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

.w-full {
  width: 100% !important;
}

.filter-date {
  width: 140px !important;
  margin-left: 8px;
}
</style>
