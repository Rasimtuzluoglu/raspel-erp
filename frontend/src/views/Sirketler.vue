<template>
  <div class="sirketler-sayfasi">
    <div class="sayfa-baslik">
      <h1 class="page-title">Şirketler</h1>
      <Button label="Yeni Şirket" icon="pi pi-plus" @click="dialogAc" />
    </div>

    <DataTable :value="sirketler" stripedRows responsiveLayout="scroll" :loading="yukleniyor">
      <Column field="id" header="#" style="width:60px" />
      <Column field="ad" header="Şirket Adı" sortable />
      <Column field="vergiNo" header="Vergi No" />
      <Column field="vergiDairesi" header="Vergi Dairesi" />
      <Column field="telefon" header="Telefon" />
      <Column field="email" header="E-posta" />
      <Column field="aktif" header="Durum">
        <template #body="{ data }">
          <Tag :value="data.aktif ? 'Aktif' : 'Pasif'" :severity="data.aktif ? 'success' : 'danger'" />
        </template>
      </Column>
      <Column header="İşlem" style="width:120px">
        <template #body="{ data }">
          <Button icon="pi pi-pencil" class="p-button-rounded p-button-text" @click="dialogAc(data)" />
          <Button icon="pi pi-trash" class="p-button-rounded p-button-text p-button-danger" @click="sil(data)" />
        </template>
      </Column>
    </DataTable>

    <Dialog v-model:visible="dialog" :header="duzenleme ? 'Şirket Düzenle' : 'Yeni Şirket'" modal :style="{ width: '500px' }">
      <Message v-if="uyariMesaji" severity="warn" :closable="false">
        <i class="pi pi-exclamation-triangle" style="margin-right:8px"></i>{{ uyariMesaji }}
      </Message>
      <div class="form-grid">
        <div class="field">
          <label>Şirket Adı *</label>
          <InputText v-model="form.ad" class="w-full" />
        </div>
        <div class="field">
          <label>Vergi No</label>
          <InputText v-model="form.vergiNo" class="w-full" />
        </div>
        <div class="field">
          <label>Vergi Dairesi</label>
          <InputText v-model="form.vergiDairesi" class="w-full" />
        </div>
        <div class="field">
          <label>Telefon</label>
          <InputText v-model="form.telefon" class="w-full" />
        </div>
        <div class="field">
          <label>E-posta</label>
          <InputText v-model="form.email" class="w-full" />
        </div>
        <div class="field">
          <label>Web Sitesi</label>
          <InputText v-model="form.webSite" class="w-full" />
        </div>
        <div class="field">
          <label>Logo</label>
          <div class="logo-upload-row">
            <InputText v-model="form.logoUrl" class="w-full" placeholder="URL girin veya dosya seçin" />
            <Button icon="pi pi-upload" class="p-button-outlined" @click="$refs.logoInput.click()" :loading="logoYukleniyor" />
            <input ref="logoInput" type="file" accept="image/*" style="display:none" @change="logoSec" />
          </div>
          <img v-if="form.logoUrl" :src="form.logoUrl" class="logo-preview" alt="Logo Önizleme" />
        </div>
        <div class="field">
          <label>Adres</label>
          <Textarea v-model="form.adres" rows="3" class="w-full" />
        </div>
        <div class="field">
          <label>Aktif</label>
          <InputSwitch v-model="form.aktif" />
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
import { ref, computed, onMounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import { sirketAPI, uploadAPI } from '../api/index.js'

const toast = useToast()
const confirm = useConfirm()

const sirketler = ref([])
const yukleniyor = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const kaydediliyor = ref(false)
const seciliId = ref(null)
const form = ref({ ad: '', vergiNo: '', vergiDairesi: '', adres: '', telefon: '', email: '', webSite: '', logoUrl: '', aktif: true })
const uyariMesaji = ref('')
const logoYukleniyor = ref(false)

onMounted(async () => {
  yukleniyor.value = true
  try { sirketler.value = (await sirketAPI.getAll()).data } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || err?.message || 'Şirketler yüklenirken hata oluştu', life: 5000 })
  }
  yukleniyor.value = false
})

const eskiAd = ref('')
const dialogAc = (data) => {
  duzenleme.value = !!data
  seciliId.value = data?.id || null
  form.value = data ? { ...data } : { ad: '', vergiNo: '', vergiDairesi: '', adres: '', telefon: '', email: '', webSite: '', logoUrl: '', aktif: true }
  eskiAd.value = data?.ad || ''
  uyariMesaji.value = ''
  if (data?.sonAdGuncellemeTarihi) {
    const son = new Date(data.sonAdGuncellemeTarihi)
    const simdi = new Date()
    const fark = Math.ceil((son.getTime() + 30 * 24 * 60 * 60 * 1000 - simdi.getTime()) / (24 * 60 * 60 * 1000))
    if (fark > 0) {
      uyariMesaji.value = `Şirket adı ${fark} gün içinde tekrar değiştirilemez. Son değişiklik: ${son.toLocaleDateString('tr-TR')}`
    }
  }
  dialog.value = true
}

const logoSec = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  logoYukleniyor.value = true
  try {
    const res = await uploadAPI.uploadSirketLogo(file)
    form.value.logoUrl = res.data.url
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: 'Logo yüklenemedi', life: 5000 })
  }
  logoYukleniyor.value = false
}

const kaydet = async () => {
  if (duzenleme.value && form.value.ad !== eskiAd.value && uyariMesaji.value) {
    confirm.require({
      message: `${uyariMesaji.value}\n\nDeğişiklik yapmak istediğinize emin misiniz?`,
      header: '30 Gün Kuralı Uyarısı',
      icon: 'pi pi-exclamation-triangle',
      accept: () => kaydetAction()
    })
    return
  }
  await kaydetAction()
}

const kaydetAction = async () => {
  kaydediliyor.value = true
  try {
    if (duzenleme.value) {
      await sirketAPI.update(seciliId.value, form.value)
      toast.add({ severity: 'success', summary: 'Başarılı', detail: 'Şirket güncellendi', life: 5000 })
    } else {
      await sirketAPI.create(form.value)
      toast.add({ severity: 'success', summary: 'Başarılı', detail: 'Şirket oluşturuldu', life: 5000 })
    }
    dialog.value = false
    const r = await sirketAPI.getAll()
    sirketler.value = r.data
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err.response?.data?.message || 'İşlem başarısız', life: 5000 })
  }
  kaydediliyor.value = false
}

const sil = async (data) => {
  try {
    await sirketAPI.delete(data.id)
    sirketler.value = sirketler.value.filter(s => s.id !== data.id)
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || err?.message || 'Şirket silinirken hata oluştu', life: 5000 })
  }
}
</script>

<style scoped>
.sirketler-sayfasi { padding: 0; }
.sayfa-baslik { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.sayfa-baslik h1 { margin: 0; }
.form-grid { display: flex; flex-direction: column; gap: 16px; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field label { font-size: 13px; font-weight: 600; color: var(--text-secondary); }
.w-full { width: 100%; }
.logo-upload-row { display: flex; gap: 8px; align-items: center; }
.logo-preview { max-width: 120px; max-height: 60px; margin-top: 8px; border-radius: 6px; object-fit: contain; }
</style>
