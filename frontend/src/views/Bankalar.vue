<template>
  <div class="bankalar-container">
    <h1>Banka Hesap Yönetimi</h1>

    <Toolbar class="toolbar">
      <template #start>
        <Button
          label="Yeni Banka Hesabı"
          icon="pi pi-plus"
          class="p-button-success"
          @click="openDialog"
        />
      </template>
      <template #end>
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
      <DataTable
        :value="bankaStore.bankalar"
        responsive-layout="scroll"
        striped-rows
        :rows="10"
        :paginator="true"
        paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport RowsPerPageDropdown"
        :rows-per-page-options="[10,20,50]"
        current-page-report-template="{first} - {last} ({totalRecords} kayıt)"
      >
        <Column
          field="ad"
          header="Banka Adı"
          style="width:200px"
        />
        <Column
          field="hesapNo"
          header="Hesap No"
          style="width:150px"
        >
          <template #body="s">
            {{ s.data.hesapNo || '-' }}
          </template>
        </Column>
        <Column
          field="iban"
          header="IBAN"
          style="width:230px"
        >
          <template #body="s">
            <span
              v-if="s.data.iban"
              class="kopyalanabilir"
              @click="kopyala(s.data.iban, 'IBAN Kopyalandı')"
            >
              {{ s.data.iban }} <i class="pi pi-copy kopyala-ikon" />
            </span>
            <span v-else>-</span>
          </template>
        </Column>
        <Column
          field="bakiye"
          header="Bakiye"
          style="width:130px"
        >
          <template #body="s">
            <span :class="s.data.bakiye >= 0 ? 'positive' : 'negative'">{{ formatCurrency(s.data.bakiye) }}</span>
          </template>
        </Column>
        <Column
          header="İşlemler"
          style="width:140px"
        >
          <template #body="s">
            <Button
              icon="pi pi-pencil"
              class="p-button-rounded p-button-info p-button-sm"
              title="Düzenle"
              @click="editBanka(s.data)"
            />
            <Button
              icon="pi pi-trash"
              class="p-button-rounded p-button-danger p-button-sm"
              title="Sil"
              @click="confirmDelete(s.data.id)"
            />
          </template>
        </Column>
      </DataTable>
      <Message
        v-if="bankaStore.bankalar.length === 0"
        severity="info"
        text="Banka hesabı bulunmamaktadır."
      />
    </div>

    <Dialog
      v-model:visible="showDialog"
      :header="editingId ? 'Banka Hesabı Düzenle' : 'Yeni Banka Hesabı'"
      :modal="true"
      style="width:500px"
    >
      <div class="form-group">
        <label>Banka Adı *</label>
        <InputText
          v-model="form.ad"
          placeholder="Banka adını giriniz"
          class="w-full"
        />
      </div>
      <div class="form-group">
        <label>Hesap No</label>
        <InputText
          v-model="form.hesapNo"
          placeholder="Hesap numarası"
          class="w-full"
        />
      </div>
      <div class="form-group">
        <label>IBAN</label>
        <InputText
          v-model="form.iban"
          placeholder="IBAN numarası"
          class="w-full"
        />
      </div>
      <div
        v-if="!editingId"
        class="form-group"
      >
        <label>Açılış Bakiyesi</label>
        <InputNumber
          v-model="form.bakiye"
          :min="0"
          :min-fraction-digits="2"
          :max-fraction-digits="2"
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
          :label="editingId ? 'Güncelle' : 'Kaydet'"
          icon="pi pi-check"
          :loading="saving"
          @click="saveBanka"
        />
      </template>
    </Dialog>

    <Message
      v-if="bankaStore.error"
      severity="error"
      :text="bankaStore.error"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { useBankaStore } from '../stores/bankaStore.js'
import { usePanoyaKopyala } from '../composables/usePanoyaKopyala.js'
import { excelAPI } from '../api/index.js'

const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const bankaStore = useBankaStore()
const { kopyala } = usePanoyaKopyala()

const showDialog = ref(false)
const loading = ref(false)
const saving = ref(false)
const editingId = ref(null)

const form = ref({ ad: '', hesapNo: '', iban: '', bakiye: 0 })

onMounted(async () => {
  loading.value = true
  try { await bankaStore.getAllBankalar() }
  catch {     toastBildirim.hata('Bankalar yüklenirken hata oluştu') }
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
    toastBildirim.uyari('Banka adı boş olamaz')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await bankaStore.updateBanka(editingId.value, { ad: form.value.ad, hesapNo: form.value.hesapNo, iban: form.value.iban })
      toastBildirim.basarili('Banka hesabı güncellendi')
    } else {
      await bankaStore.addBanka(form.value)
      toastBildirim.basarili('Banka hesabı oluşturuldu')
    }
    closeDialog()
  } catch { toastBildirim.hata('İşlem başarısız') }
  finally { saving.value = false }
}

const confirmDelete = (id) => {
  confirm.require({
    message: 'Bu banka hesabını silmek istediğinizden emin misiniz?',
    header: 'Onay', icon: 'pi pi-exclamation-triangle',
    accept: async () => {
      try { await bankaStore.deleteBanka(id); toastBildirim.basarili('Banka hesabı silindi') }
      catch { toastBildirim.hata('Silme başarısız') }
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
.kopyalanabilir { cursor: pointer; display: inline-flex; align-items: center; gap: 6px; }
.kopyalanabilir:hover { color: var(--accent); }
.kopyala-ikon { font-size: 11px; opacity: 0.5; }
.kopyalanabilir:hover .kopyala-ikon { opacity: 1; }
.w-full { width: 100% !important; }
</style>