<template>
  <div class="iade-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">İade Yönetimi</h1>
      <Button label="Yeni İade" icon="pi pi-plus" @click="dialogAc()" />
    </div>

    <DataTable :value="list" stripedRows :loading="yukleniyor">
      <Column field="tarih" header="Tarih" sortable>
        <template #body="{ data }">{{ formatDate(data.tarih) }}</template>
      </Column>
      <Column field="tutar" header="Tutar">
        <template #body="{ data }">{{ formatCurrency(data.tutar) }}</template>
      </Column>
      <Column field="durum" header="Durum">
        <template #body="{ data }">
          <Tag :value="data.durum" :severity="data.durum === 'ONAYLANDI' ? 'success' : data.durum === 'IPTAL' ? 'danger' : 'warn'" />
        </template>
      </Column>
      <Column field="aciklama" header="Açıklama" />
      <Column header="İşlem" style="width:200px">
        <template #body="{ data }">
          <Button icon="pi pi-check-circle" class="p-button-rounded p-button-text p-button-success" v-if="data.durum !== 'ONAYLANDI'" @click="durumGuncelle(data, 'ONAYLANDI')" title="Onayla" />
          <Button icon="pi pi-times-circle" class="p-button-rounded p-button-text p-button-danger" v-if="data.durum !== 'IPTAL'" @click="durumGuncelle(data, 'IPTAL')" title="İptal" />
          <Button icon="pi pi-trash" class="p-button-rounded p-button-text" @click="sil(data)" />
        </template>
      </Column>
    </DataTable>

    <Dialog v-model:visible="dialog" :header="dialogHeader" modal :style="{ width: '500px' }">
      <div class="form-grid">
        <div class="field"><label>Tarih *</label><DatePicker v-model="form.tarih" dateFormat="dd/mm/yy" class="w-full" /></div>
        <div class="field"><label>Tutar *</label><InputNumber v-model="form.tutar" mode="currency" currency="TRY" class="w-full" /></div>
        <div class="field"><label>Açıklama</label><Textarea v-model="form.aciklama" rows="3" class="w-full" /></div>
      </div>
      <template #footer>
        <Button label="İptal" icon="pi pi-times" class="p-button-text" @click="dialog = false" />
        <Button label="Kaydet" icon="pi pi-check" @click="kaydet" :loading="kaydediliyor" />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import { iadeAPI } from '../api/index.js'

const toast = useToast()
const confirm = useConfirm()
const list = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const form = ref({ tarih: new Date(), tutar: 0, aciklama: '' })

const dialogHeader = computed(() => duzenleme.value ? 'İade Düzenle' : 'Yeni İade')

const formatCurrency = (v) => {
  if (v === null || v === undefined) return '0,00 ₺'
  return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(v)
}
const formatDate = (d) => {
  if (!d) return '-'
  return new Intl.DateTimeFormat('tr-TR', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(d))
}

onMounted(async () => {
  yukleniyor.value = true
  try { list.value = (await iadeAPI.getAll()).data } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || 'İadeler yüklenemedi', life: 5000 })
  }
  yukleniyor.value = false
})

const dialogAc = (data) => {
  duzenleme.value = !!data
  form.value = data ? { ...data, tarih: data.tarih ? new Date(data.tarih) : new Date() } : { tarih: new Date(), tutar: 0, aciklama: '' }
  dialog.value = true
}

const kaydet = async () => {
  kaydediliyor.value = true
  try {
    const payload = { ...form.value, tarih: form.value.tarih?.toISOString?.().split('T')[0] ?? form.value.tarih }
    if (duzenleme.value) {
      await iadeAPI.update(form.value.id, payload)
      toast.add({ severity: 'success', summary: 'Başarılı', detail: 'İade güncellendi', life: 3000 })
    } else {
      await iadeAPI.create(payload)
      toast.add({ severity: 'success', summary: 'Başarılı', detail: 'İade oluşturuldu', life: 3000 })
    }
    dialog.value = false
    list.value = (await iadeAPI.getAll()).data
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || 'İşlem başarısız', life: 5000 })
  }
  kaydediliyor.value = false
}

const durumGuncelle = async (data, durum) => {
  try {
    await iadeAPI.durumGuncelle(data.id, durum)
    list.value = (await iadeAPI.getAll()).data
    toast.add({ severity: 'success', summary: 'Başarılı', detail: `İade durumu "${durum}" olarak güncellendi`, life: 3000 })
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || 'Durum güncellenirken hata oluştu', life: 5000 })
  }
}

const sil = (data) => {
  confirm.require({
    message: `Bu iade kaydını silmek istediğinize emin misiniz?`,
    header: 'Silme Onayı',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Evet, Sil',
    rejectLabel: 'İptal',
    accept: async () => {
      try {
        await iadeAPI.delete(data.id)
        list.value = list.value.filter(x => x.id !== data.id)
        toast.add({ severity: 'success', summary: 'Silindi', detail: 'İade kaydı silindi', life: 3000 })
      } catch (err) {
        toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || 'Silme başarısız', life: 5000 })
      }
    }
  })
}
</script>

<style scoped>
.iade-container { padding: 0; }
.sayfa-baslik { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.form-grid { display: flex; flex-direction: column; gap: 16px; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field label { font-size: 13px; font-weight: 600; color: var(--text-secondary); }
.w-full { width: 100%; }
</style>
