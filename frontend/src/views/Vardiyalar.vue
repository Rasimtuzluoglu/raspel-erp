<template>
  <div class="vardiya-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">Vardiya Yönetimi</h1>
      <Button label="Yeni Vardiya" icon="pi pi-plus" @click="dialogAc()" />
    </div>

    <DataTable :value="list" stripedRows :loading="yukleniyor">
      <Column field="personelAd" header="Personel" sortable />
      <Column field="tarih" header="Tarih" sortable>
        <template #body="{ data }">{{ formatDate(data.tarih) }}</template>
      </Column>
      <Column field="baslangic" header="Başlangıç">
        <template #body="{ data }">{{ data.baslangic }}</template>
      </Column>
      <Column field="bitis" header="Bitiş">
        <template #body="{ data }">{{ data.bitis }}</template>
      </Column>
      <Column field="tur" header="Tür">
        <template #body="{ data }">
          <Tag :value="data.tur" :severity="data.tur === 'SABAH' ? 'info' : data.tur === 'AKSAM' ? 'warn' : 'contrast'" />
        </template>
      </Column>
      <Column header="İşlem" style="width:120px">
        <template #body="{ data }">
          <Button icon="pi pi-trash" class="p-button-rounded p-button-text" @click="sil(data)" />
        </template>
      </Column>
    </DataTable>

    <Dialog v-model:visible="dialog" :header="dialogHeader" modal :style="{ width: '500px' }">
      <div class="form-grid">
        <div class="field"><label>Personel *</label>
          <Dropdown v-model="form.personelId" :options="personelListesi" optionLabel="displayName" optionValue="id" placeholder="Personel Seç" class="w-full" filter />
        </div>
        <div class="field"><label>Tarih *</label><DatePicker v-model="form.tarih" dateFormat="dd/mm/yy" class="w-full" /></div>
        <div class="field-row">
          <div class="field"><label>Başlangıç *</label><InputText v-model="form.baslangic" placeholder="08:00" class="w-full" /></div>
          <div class="field"><label>Bitiş *</label><InputText v-model="form.bitis" placeholder="16:00" class="w-full" /></div>
        </div>
        <div class="field"><label>Tür *</label>
          <Dropdown v-model="form.tur" :options="turSecenekleri" placeholder="Seçin" class="w-full" />
        </div>
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
import { vardiyaAPI, personelAPI } from '../api/index.js'

const toast = useToast()
const confirm = useConfirm()
const list = ref([])
const personelListesi = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const form = ref({ personelId: null, tarih: new Date(), baslangic: '08:00', bitis: '16:00', tur: 'SABAH' })
const turSecenekleri = ['SABAH', 'AKSAM', 'GECE']

const dialogHeader = computed(() => duzenleme.value ? 'Vardiya Düzenle' : 'Yeni Vardiya')

const formatDate = (d) => {
  if (!d) return '-'
  return new Intl.DateTimeFormat('tr-TR', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(d))
}

onMounted(async () => {
  yukleniyor.value = true
  try {
    const [vR, pR] = await Promise.all([vardiyaAPI.getAll(), personelAPI.getAll()])
    list.value = vR.data?.content || vR.data || []
    personelListesi.value = pR.data.map(p => ({ ...p, displayName: p.ad && p.soyad ? `${p.ad} ${p.soyad}` : p.ad || p.id }))
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || 'Veriler yüklenemedi', life: 5000 })
  }
  yukleniyor.value = false
})

const dialogAc = (data) => {
  duzenleme.value = !!data
  form.value = data
    ? { ...data, tarih: data.tarih ? new Date(data.tarih) : new Date() }
    : { personelId: null, tarih: new Date(), baslangic: '08:00', bitis: '16:00', tur: 'SABAH' }
  dialog.value = true
}

const kaydet = async () => {
  kaydediliyor.value = true
  try {
    const payload = {
      ...form.value,
      tarih: form.value.tarih?.toISOString?.().split('T')[0] ?? form.value.tarih
    }
    if (duzenleme.value) {
      await vardiyaAPI.update(form.value.id, payload)
      toast.add({ severity: 'success', summary: 'Başarılı', detail: 'Vardiya güncellendi', life: 3000 })
    } else {
      await vardiyaAPI.create(payload)
      toast.add({ severity: 'success', summary: 'Başarılı', detail: 'Vardiya oluşturuldu', life: 3000 })
    }
    dialog.value = false
    const r = await vardiyaAPI.getAll(); list.value = r.data?.content || r.data || []
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || 'İşlem başarısız', life: 5000 })
  }
  kaydediliyor.value = false
}

const sil = (data) => {
  const personelAd = data.personelAd || data.id
  confirm.require({
    message: `"${personelAd}" vardiyasını silmek istediğinize emin misiniz?`,
    header: 'Silme Onayı',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Evet, Sil',
    rejectLabel: 'İptal',
    accept: async () => {
      try {
        await vardiyaAPI.delete(data.id)
        list.value = list.value.filter(x => x.id !== data.id)
        toast.add({ severity: 'success', summary: 'Silindi', detail: 'Vardiya silindi', life: 3000 })
      } catch (err) {
        toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || 'Silme başarısız', life: 5000 })
      }
    }
  })
}
</script>

<style scoped>
.vardiya-container { padding: 0; }
.sayfa-baslik { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.form-grid { display: flex; flex-direction: column; gap: 16px; }
.field-row { display: flex; gap: 12px; }
.field-row .field { flex: 1; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field label { font-size: 13px; font-weight: 600; color: var(--text-secondary); }
.w-full { width: 100%; }
</style>
