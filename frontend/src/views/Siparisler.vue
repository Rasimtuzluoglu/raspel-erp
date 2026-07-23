<template>
  <div class="siparisler-sayfasi">
    <div class="sayfa-baslik">
      <h1 class="page-title">Siparişler & Teklifler</h1>
      <Button label="Yeni Teklif" icon="pi pi-plus" @click="dialogAc()" />
    </div>

    <DataTable :value="siparisler" stripedRows :loading="yukleniyor">
      <Column field="siparisNo" header="No" sortable />
      <Column field="tarih" header="Tarih" />
      <Column field="cariHesapAdi" header="Müşteri" />
      <Column field="tur" header="Tür" />
      <Column field="genelToplam" header="Tutar">
        <template #body="{ data }">{{ data.genelToplam?.toFixed(2) }} ₺</template>
      </Column>
      <Column field="durum" header="Durum">
        <template #body="{ data }">
          <Tag :value="data.durum" :severity="data.durum === 'SIPARIS' ? 'info' : data.durum === 'FATURA_KESILDI' ? 'success' : data.durum === 'IPTAL' ? 'danger' : 'warn'" />
        </template>
      </Column>
      <Column header="İşlem" style="width:160px">
        <template #body="{ data }">
          <Button icon="pi pi-check-circle" class="p-button-rounded p-button-text p-button-info" v-if="data.durum === 'TEKLIF'" @click="durumGuncelle(data, 'SIPARIS')" title="Siparişe Çevir" />
          <Button icon="pi pi-file" class="p-button-rounded p-button-text p-button-success" v-if="data.durum === 'SIPARIS'" @click="durumGuncelle(data, 'FATURA_KESILDI')" title="Faturalaştır" />
          <Button icon="pi pi-trash" class="p-button-rounded p-button-text" @click="sil(data)" :disabled="data.durum === 'FATURA_KESILDI'" />
        </template>
      </Column>
    </DataTable>

    <Dialog v-model:visible="dialog" header="Yeni Teklif / Sipariş" modal :style="{ width: '550px' }">
      <div class="form-grid">
        <div class="field"><label>Teklif No *</label><InputText v-model="form.siparisNo" class="w-full" /></div>
        <div class="field"><label>Tarih</label><DatePicker v-model="form.tarih" dateFormat="dd/mm/yy" class="w-full" /></div>
        <div class="field"><label>Müşteri *</label>
          <Dropdown v-model="form.cariHesapId" :options="cariler" optionLabel="ad" optionValue="id" placeholder="Seçin" class="w-full" />
        </div>
        <div class="field"><label>Açıklama</label><Textarea v-model="form.aciklama" rows="2" class="w-full" /></div>
      </div>
      <template #footer>
        <Button label="İptal" icon="pi pi-times" class="p-button-text" @click="dialog = false" />
        <Button label="Kaydet" icon="pi pi-check" @click="kaydet" :loading="kaydediliyor" />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import { siparisAPI, cariHesapAPI } from '../api/index.js'
const toast = useToast()
const confirm = useConfirm()

const siparisler = ref([])
const cariler = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const dialog = ref(false)
const form = ref({ siparisNo: '', tarih: new Date(), cariHesapId: null, aciklama: '' })

onMounted(async () => {
  yukleniyor.value = true
  try {
    const [sR, cR] = await Promise.all([siparisAPI.getAll(), cariHesapAPI.getAll()])
    siparisler.value = sR.data
    cariler.value = cR.data
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || err?.message || 'Siparişler yüklenirken hata oluştu', life: 5000 })
  }
  yukleniyor.value = false
})

const dialogAc = () => {
  form.value = { siparisNo: 'TKF-' + Date.now(), tarih: new Date(), cariHesapId: null, aciklama: '' }
  dialog.value = true
}

const kaydet = async () => {
  kaydediliyor.value = true
  try {
    await siparisAPI.create({ ...form.value, tarih: form.value.tarih?.toISOString().split('T')[0] })
    dialog.value = false
    siparisler.value = (await siparisAPI.getAll()).data
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || err?.message || 'Sipariş kaydedilirken hata oluştu', life: 5000 })
  }
  kaydediliyor.value = false
}

const durumGuncelle = async (data, durum) => {
  try {
    await siparisAPI.durumGuncelle(data.id, durum)
    siparisler.value = (await siparisAPI.getAll()).data
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || err?.message || 'Durum güncellenirken hata oluştu', life: 5000 })
  }
}

const sil = (data) => {
  confirm.require({
    message: 'Bu kaydı silmek istediğinize emin misiniz?',
    header: 'Silme Onayı',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Evet, Sil',
    rejectLabel: 'İptal',
    accept: async () => {
      try {
        await siparisAPI.delete(data.id)
        siparisler.value = siparisler.value.filter(s => s.id !== data.id)
      } catch (err) {
        toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || err?.message || 'Sipariş silinirken hata oluştu', life: 5000 })
      }
    },
    reject: () => {}
  })
}
</script>

<style scoped>
.siparisler-sayfasi { padding: 0; }
.sayfa-baslik { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.sayfa-baslik h1 { margin: 0; }
.form-grid { display: flex; flex-direction: column; gap: 16px; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field label { font-size: 13px; font-weight: 600; color: var(--text-secondary); }
.w-full { width: 100%; }
</style>
