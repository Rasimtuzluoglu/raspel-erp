<template>
  <div class="bankalar-container">
    <h1>Banka Hesap Yönetimi</h1>

    <Toolbar class="toolbar">
      <template #start>
        <Button label="Yeni Banka Hesabı" icon="pi pi-plus" @click="openDialog" class="p-button-success" />
      </template>
      <template #end>
        <Button label="Excel" icon="pi pi-file-excel" class="p-button-sm p-button-outlined" @click="excelIndir" />
      </template>
    </Toolbar>

    <div class="loading" v-if="loading"><p><i class="pi pi-spin pi-spinner"></i> Yükleniyor...</p></div>

    <div class="table-container" v-if="!loading">
      <DataTable
        :value="bankaStore.bankalar"
        responsive-layout="scroll" striped-rows
        :rows="10" :paginator="true"
        paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport RowsPerPageDropdown"
        :rows-per-page-options="[10,20,50]"
        current-page-report-template="{first} - {last} ({totalRecords} kayıt)"
      >
        <Column field="ad" header="Banka Adı" style="width:200px"></Column>
        <Column field="hesapNo" header="Hesap No" style="width:150px">
          <template #body="s">{{ s.data.hesapNo || '-' }}</template>
        </Column>
        <Column field="iban" header="IBAN" style="width:200px">
          <template #body="s">{{ s.data.iban || '-' }}</template>
        </Column>
        <Column field="bakiye" header="Bakiye" style="width:130px">
          <template #body="s">
            <span :class="s.data.bakiye >= 0 ? 'positive' : 'negative'">{{ formatCurrency(s.data.bakiye) }}</span>
          </template>
        </Column>
        <Column header="İşlemler" style="width:140px">
          <template #body="s">
            <Button icon="pi pi-pencil" class="p-button-rounded p-button-info p-button-sm"
              @click="editBanka(s.data)" title="Düzenle" />
            <Button icon="pi pi-trash" class="p-button-rounded p-button-danger p-button-sm"
              @click="confirmDelete(s.data.id)" title="Sil" />
          </template>
        </Column>
      </DataTable>
      <Message v-if="bankaStore.bankalar.length === 0" severity="info" text="Banka hesabı bulunmamaktadır." />
    </div>

    <Dialog v-model:visible="showDialog" :header="editingId ? 'Banka Hesabı Düzenle' : 'Yeni Banka Hesabı'" :modal="true" style="width:500px">
      <div class="form-group">
        <label>Banka Adı *</label>
        <InputText v-model="form.ad" placeholder="Banka adını giriniz" class="w-full" />
      </div>
      <div class="form-group">
        <label>Hesap No</label>
        <InputText v-model="form.hesapNo" placeholder="Hesap numarası" class="w-full" />
      </div>
      <div class="form-group">
        <label>IBAN</label>
        <InputText v-model="form.iban" placeholder="IBAN numarası" class="w-full" />
      </div>
      <div class="form-group" v-if="!editingId">
        <label>Açılış Bakiyesi</label>
        <InputNumber v-model="form.bakiye" :min="0" :min-fraction-digits="2" :max-fraction-digits="2" class="w-full" />
      </div>
      <template #footer>
        <Button label="İptal" icon="pi pi-times" @click="closeDialog" class="p-button-text" />
        <Button :label="editingId ? 'Güncelle' : 'Kaydet'" icon="pi pi-check" @click="saveBanka" :loading="saving" />
      </template>
    </Dialog>

    <Message v-if="bankaStore.error" severity="error" :text="bankaStore.error" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import { useBankaStore } from '../stores/bankaStore.js'
import { excelAPI } from '../api/index.js'

const toast = useToast()
const confirm = useConfirm()
const bankaStore = useBankaStore()

const showDialog = ref(false)
const loading = ref(false)
const saving = ref(false)
const editingId = ref(null)

const form = ref({ ad: '', hesapNo: '', iban: '', bakiye: 0 })

onMounted(async () => {
  loading.value = true
  try { await bankaStore.getAllBankalar() }
  catch {     toast.add({ severity: 'error', summary: 'Hata', detail: 'Bankalar yüklenirken hata oluştu', life: 5000 }) }
  finally { loading.value = false }
})

const openDialog = () => {
  editingId.value = null
  form.value = { ad: '', hesapNo: '', iban: '', bakiye: 0 }
  showDialog.value = true
}

const closeDialog = () => { showDialog.value = false }

const editBanka = (banka) => {
  editingId.value = banka.id
  form.value = { ad: banka.ad, hesapNo: banka.hesapNo || '', iban: banka.iban || '', bakiye: 0 }
  showDialog.value = true
}

const saveBanka = async () => {
  if (!form.value.ad.trim()) {
    toast.add({ severity: 'warn', summary: 'Uyarı', detail: 'Banka adı boş olamaz', life: 5000 })
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await bankaStore.updateBanka(editingId.value, { ad: form.value.ad, hesapNo: form.value.hesapNo, iban: form.value.iban })
      toast.add({ severity: 'success', summary: 'Başarılı', detail: 'Banka hesabı güncellendi', life: 5000 })
    } else {
      await bankaStore.addBanka(form.value)
      toast.add({ severity: 'success', summary: 'Başarılı', detail: 'Banka hesabı oluşturuldu', life: 5000 })
    }
    closeDialog()
  } catch { toast.add({ severity: 'error', summary: 'Hata', detail: 'İşlem başarısız', life: 5000 }) }
  finally { saving.value = false }
}

const confirmDelete = (id) => {
  confirm.require({
    message: 'Bu banka hesabını silmek istediğinizden emin misiniz?',
    header: 'Onay', icon: 'pi pi-exclamation-triangle',
    accept: async () => {
      try { await bankaStore.deleteBanka(id); toast.add({ severity: 'success', summary: 'Başarılı', detail: 'Banka hesabı silindi', life: 5000 }) }
      catch { toast.add({ severity: 'error', summary: 'Hata', detail: 'Silme başarısız', life: 5000 }) }
    }
  })
}

const excelIndir = async () => {
  try {
    const res = await excelAPI.bankalar()
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', 'Bankalar.xlsx')
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch { /* silent */ }
}

const formatCurrency = (v) => {
  if (v === null || v === undefined) return '0,00 ₺'
  return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(v)
}
</script>

<style scoped>
.bankalar-container { padding: 20px; }
h1 { color: var(--text-primary); margin-bottom: 20px; font-size: 28px; font-weight: 700; letter-spacing: -0.5px; }
.toolbar { margin-bottom: 20px; background: var(--bg-card); border: 1px solid var(--border); border-radius: 12px; padding: 14px 18px; }
.table-container { background: var(--bg-card); border: 1px solid var(--border); border-radius: 12px; padding: 14px; overflow-x: auto; }
.loading { text-align: center; padding: 40px; color: #666; }
.form-group { margin-bottom: 20px; }
.form-group label { display: block; margin-bottom: 8px; font-weight: bold; color: #333; }
.positive { color: #4caf50; font-weight: bold; }
.negative { color: #f44336; font-weight: bold; }
.w-full { width: 100% !important; }
</style>
