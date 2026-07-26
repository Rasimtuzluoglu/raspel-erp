<template>
  <div class="izinler-sayfasi">
    <div class="sayfa-baslik">
      <h1 class="page-title">İzin Talepleri</h1>
      <div class="filtre-grup">
        <SelectButton v-model="durumFiltre" :options="filtreSecenekleri" optionLabel="label" optionValue="value" />
      </div>
    </div>

    <DataTable :value="filtrelenmisIzinler" stripedRows :loading="yukleniyor" :paginator="true" :rows="20"
      paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport"
      current-page-report-template="{totalRecords} kayıttan {first}-{last}" sort-field="baslangic" :sort-order="-1">
      <Column field="personelAdi" header="Personel" sortable />
      <Column field="izinTuru" header="İzin Türü" sortable>
        <template #body="{ data }">
          {{ izinTuruLabel(data.izinTuru) }}
        </template>
      </Column>
      <Column field="baslangic" header="Başlangıç" sortable>
        <template #body="{ data }">{{ formatDate(data.baslangic) }}</template>
      </Column>
      <Column field="bitis" header="Bitiş" sortable>
        <template #body="{ data }">{{ formatDate(data.bitis) }}</template>
      </Column>
      <Column field="gunSayisi" header="Gün" sortable />
      <Column field="durum" header="Durum" sortable>
        <template #body="{ data }">
          <Tag :value="durumLabel(data.durum)" :severity="data.durum === 'ONAYLANDI' ? 'success' : data.durum === 'REDDEDILDI' ? 'danger' : 'warn'" />
        </template>
      </Column>
      <Column field="aciklama" header="Açıklama">
        <template #body="{ data }">{{ data.aciklama || '-' }}</template>
      </Column>
      <Column header="İşlem" style="width:180px">
        <template #body="{ data }">
          <Button v-if="data.durum === 'BEKLEMEDE'" icon="pi pi-check" class="p-button-rounded p-button-sm p-button-success" @click="onayla(data)" title="Onayla" />
          <Button v-if="data.durum === 'BEKLEMEDE'" icon="pi pi-times" class="p-button-rounded p-button-sm p-button-danger" @click="reddet(data)" title="Reddet" />
          <Button v-if="authStore.kullanici?.role === 'ADMIN'" icon="pi pi-trash" class="p-button-rounded p-button-sm p-button-text" @click="sil(data)" title="Sil" />
        </template>
      </Column>
    </DataTable>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import { useAuthStore } from '../stores/authStore.js'
import { personelIzinAPI } from '../api/index.js'

const toast = useToast()
const confirm = useConfirm()
const authStore = useAuthStore()

const yukleniyor = ref(false)
const tumIzinler = ref([])
const durumFiltre = ref('TUMU')
const filtreSecenekleri = [
  { label: 'Tümü', value: 'TUMU' },
  { label: 'Beklemede', value: 'BEKLEMEDE' },
  { label: 'Onaylandı', value: 'ONAYLANDI' },
  { label: 'Reddedildi', value: 'REDDEDILDI' }
]

const filtrelenmisIzinler = computed(() => {
  if (durumFiltre.value === 'TUMU') return tumIzinler.value
  return tumIzinler.value.filter(i => i.durum === durumFiltre.value)
})

const izinTuruLabel = (t) => ({
  YILLIK_IZIN: 'Yıllık İzin', HASTA_IZNI: 'Hasta İzni', MAZERET_IZNI: 'Mazeret İzni',
  DOGUM_IZNI: 'Doğum İzni', BABALIK_IZNI: 'Babalık İzni', EVLILIK_IZNI: 'Evlilik İzni',
  UCRETSIZ_IZIN: 'Ücretsiz İzin'
})[t] || t

const durumLabel = (d) => ({ BEKLEMEDE: 'Beklemede', ONAYLANDI: 'Onaylandı', REDDEDILDI: 'Reddedildi' })[d] || d

const formatDate = (d) => {
  if (!d) return '-'
  return new Intl.DateTimeFormat('tr-TR', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(d))
}

onMounted(async () => {
  yukleniyor.value = true
  try {
    const r = await personelIzinAPI.getAll()
    tumIzinler.value = r.data
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || 'Veriler yüklenemedi', life: 5000 })
  }
  yukleniyor.value = false
})

const onayla = (data) => {
  confirm.require({
    message: `${data.personelAdi} - ${izinTuruLabel(data.izinTuru)} iznini onaylamak istiyor musunuz?`,
    header: 'İzin Onayı', icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Onayla', rejectLabel: 'İptal',
    accept: async () => {
      try {
        await personelIzinAPI.durumGuncelle(data.id, 'ONAYLANDI', kullaniciAdi.value)
        tumIzinler.value = (await personelIzinAPI.getAll()).data
        toast.add({ severity: 'success', summary: 'Başarılı', detail: 'İzin onaylandı', life: 5000 })
      } catch (err) {
        toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || 'İşlem başarısız', life: 5000 })
      }
    }
  })
}

const reddet = (data) => {
  confirm.require({
    message: `${data.personelAdi} - ${izinTuruLabel(data.izinTuru)} iznini reddetmek istiyor musunuz?`,
    header: 'İzin Reddi', icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Reddet', rejectLabel: 'İptal',
    accept: async () => {
      try {
        await personelIzinAPI.durumGuncelle(data.id, 'REDDEDILDI', kullaniciAdi.value)
        tumIzinler.value = (await personelIzinAPI.getAll()).data
        toast.add({ severity: 'success', summary: 'Başarılı', detail: 'İzin reddedildi', life: 5000 })
      } catch (err) {
        toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || 'İşlem başarısız', life: 5000 })
      }
    }
  })
}

const kullaniciAdi = computed(() => authStore.kullanici?.displayName || authStore.kullanici?.username || 'Admin')

const sil = (data) => {
  confirm.require({
    message: 'Bu izin kaydını silmek istediğinize emin misiniz?',
    header: 'Silme Onayı', icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Evet, Sil', rejectLabel: 'İptal',
    accept: async () => {
      try {
        await personelIzinAPI.delete(data.id)
        tumIzinler.value = tumIzinler.value.filter(i => i.id !== data.id)
        toast.add({ severity: 'success', summary: 'Silindi', detail: 'İzin kaydı silindi', life: 5000 })
      } catch (err) {
        toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || 'Silme başarısız', life: 5000 })
      }
    }
  })
}
</script>

<style scoped>
.izinler-sayfasi { padding: 0; }
.sayfa-baslik { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; flex-wrap: wrap; gap: 12px; }
.sayfa-baslik h1 { margin: 0; }
.filtre-grup { display: flex; gap: 8px; }
</style>
