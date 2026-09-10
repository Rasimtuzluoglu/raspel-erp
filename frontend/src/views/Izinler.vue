<template>
  <div class="izinler-sayfasi">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        {{ t('izinler.title') }}
      </h1>
      <div class="filtre-grup">
        <SelectButton
          v-model="durumFiltre"
          :options="filtreSecenekleri"
          option-label="label"
          option-value="value"
        />
      </div>
    </div>

    <DataTable
      :value="filtrelenmisIzinler"
      striped-rows
      :loading="yukleniyor"
      :paginator="true"
      :rows="20"
      paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport"
      current-page-report-template="{totalRecords} kayıttan {first}-{last}"
      sort-field="baslangic"
      :sort-order="-1"
    >
      <Column
        field="personelAdi"
        :header="t('izinler.personel')"
        sortable
      />
      <Column
        field="izinTuru"
        :header="t('izinler.izinTuru')"
        sortable
      >
        <template #body="{ data }">
          {{ izinTuruLabel(data.izinTuru) }}
        </template>
      </Column>
      <Column
        field="baslangic"
        :header="t('izinler.baslangic')"
        sortable
      >
        <template #body="{ data }">
          {{ formatDate(data.baslangic) }}
        </template>
      </Column>
      <Column
        field="bitis"
        :header="t('izinler.bitis')"
        sortable
      >
        <template #body="{ data }">
          {{ formatDate(data.bitis) }}
        </template>
      </Column>
      <Column
        field="gunSayisi"
        :header="t('izinler.gun')"
        sortable
      />
      <Column
        field="durum"
        :header="t('common.status')"
        sortable
      >
        <template #body="{ data }">
          <Tag
            :value="durumLabel(data.durum)"
            :severity="data.durum === 'ONAYLANDI' ? 'success' : data.durum === 'REDDEDILDI' ? 'danger' : 'warn'"
          />
        </template>
      </Column>
      <Column
        field="aciklama"
        :header="t('common.description')"
      >
        <template #body="{ data }">
          {{ data.aciklama || '-' }}
        </template>
      </Column>
      <Column
        :header="t('common.actions')"
        style="width: 180px"
      >
        <template #body="{ data }">
          <Button
            v-if="data.durum === 'BEKLEMEDE'"
            icon="pi pi-check"
            class="p-button-rounded p-button-sm p-button-success"
            :title="t('izinler.onayla')"
            @click="onayla(data)"
          />
          <Button
            v-if="data.durum === 'BEKLEMEDE'"
            icon="pi pi-times"
            class="p-button-rounded p-button-sm p-button-danger"
            :title="t('izinler.reddet')"
            @click="reddet(data)"
          />
          <Button
            v-if="authStore?.kullanici?.role === 'ADMIN'"
            icon="pi pi-trash"
            class="p-button-rounded p-button-sm p-button-text"
            :title="t('common.delete')"
            @click="sil(data)"
          />
        </template>
      </Column>
    </DataTable>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { useAuthStore } from '../stores/authStore.js'
import { personelIzinAPI } from '../api/index.js'
import { useI18n } from 'vue-i18n'

const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const authStore = useAuthStore()
const { t } = useI18n()

const yukleniyor = ref(false)
const tumIzinler = ref([])
const durumFiltre = ref('TUMU')
const filtreSecenekleri = computed(() => [
  { label: t('izinler.tumu'), value: 'TUMU' },
  { label: t('izinler.beklemede'), value: 'BEKLEMEDE' },
  { label: t('izinler.onaylandi'), value: 'ONAYLANDI' },
  { label: t('izinler.reddedildi'), value: 'REDDEDILDI' }
])

const filtrelenmisIzinler = computed(() => {
  if (durumFiltre.value === 'TUMU') return tumIzinler.value
  return tumIzinler.value.filter((i) => i.durum === durumFiltre.value)
})

const izinTuruLabel = (tip) =>
  ({
    YILLIK_IZIN: t('izinler.yillikIzin'),
    HASTA_IZNI: t('izinler.hastaIzni'),
    MAZERET_IZNI: t('izinler.mazeretIzni'),
    DOGUM_IZNI: t('izinler.dogumIzni'),
    BABALIK_IZNI: t('izinler.babalikIzni'),
    EVLILIK_IZNI: t('izinler.evlilikIzni'),
    UCRETSIZ_IZIN: t('izinler.ucretsizIzin')
  })[tip] || tip

const durumLabel = (d) => ({ BEKLEMEDE: t('izinler.beklemede'), ONAYLANDI: t('izinler.onaylandi'), REDDEDILDI: t('izinler.reddedildi') })[d] || d

import { formatTarih as formatDate } from '../utils/format.js'

onMounted(async () => {
  yukleniyor.value = true
  try {
    const r = await personelIzinAPI.getAll()
    tumIzinler.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('izinler.hataYukleme'))
  }
  yukleniyor.value = false
})

const onayla = (data) => {
  confirm.require({
    message: t('izinler.onayOnayMesaj', { personel: data.personelAdi, tur: izinTuruLabel(data.izinTuru) }),
    header: t('izinler.izinOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('izinler.onayla'),
    rejectLabel: t('common.cancel'),
    accept: async () => {
      try {
        await personelIzinAPI.durumGuncelle(data.id, 'ONAYLANDI', kullaniciAdi.value)
        const r = await personelIzinAPI.getAll()
        tumIzinler.value = r.data?.content || r.data || []
        toastBildirim.basarili(t('izinler.izinOnaylandi'))
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('izinler.islemBasarisiz'))
      }
    }
  })
}

const reddet = (data) => {
  confirm.require({
    message: t('izinler.redOnayMesaj', { personel: data.personelAdi, tur: izinTuruLabel(data.izinTuru) }),
    header: t('izinler.izinReddi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('izinler.reddet'),
    rejectLabel: t('common.cancel'),
    accept: async () => {
      try {
        await personelIzinAPI.durumGuncelle(data.id, 'REDDEDILDI', kullaniciAdi.value)
        const r = await personelIzinAPI.getAll()
        tumIzinler.value = r.data?.content || r.data || []
        toastBildirim.basarili(t('izinler.izinReddedildi'))
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('izinler.islemBasarisiz'))
      }
    }
  })
}

const kullaniciAdi = computed(() => authStore?.kullanici?.displayName || authStore?.kullanici?.username || 'Admin')

const sil = (data) => {
  confirm.require({
    message: t('izinler.silOnayMesaj'),
    header: t('masraflar.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('masraflar.evetSil'),
    rejectLabel: t('common.cancel'),
    accept: async () => {
      try {
        await personelIzinAPI.delete(data.id)
        tumIzinler.value = tumIzinler.value.filter((i) => i.id !== data.id)
        toast.add({ severity: 'success', summary: t('izinler.silindi'), detail: t('izinler.izinSilindi'), life: 5000 })
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('izinler.silmeBasarisiz'))
      }
    }
  })
}
</script>

<style scoped>
.izinler-sayfasi {
  padding: 0;
}
.sayfa-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 12px;
}
.sayfa-baslik h1 {
  margin: 0;
}
.filtre-grup {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  max-width: 100%;
}
</style>
