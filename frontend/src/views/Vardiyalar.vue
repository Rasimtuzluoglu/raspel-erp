<template>
  <div class="vardiya-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        {{ t('vardiyalar.title') }}
      </h1>
      <Button
        :label="t('vardiyalar.yeniVardiya')"
        icon="pi pi-plus"
        @click="dialogAc()"
      />
    </div>

    <DataTable
      :value="list"
      striped-rows
      :loading="yukleniyor"
    >
      <Column
        field="personelAd"
        :header="t('vardiyalar.personel')"
        sortable
      />
      <Column
        field="tarih"
        :header="t('common.date')"
        sortable
      >
        <template #body="{ data }">
          {{ formatDate(data.tarih) }}
        </template>
      </Column>
      <Column
        field="baslangic"
        :header="t('vardiyalar.baslangic')"
      >
        <template #body="{ data }">
          {{ data.baslangic }}
        </template>
      </Column>
      <Column
        field="bitis"
        :header="t('vardiyalar.bitis')"
      >
        <template #body="{ data }">
          {{ data.bitis }}
        </template>
      </Column>
      <Column
        field="tur"
        :header="t('vardiyalar.tur')"
      >
        <template #body="{ data }">
          <Tag
            :value="data.tur"
            :severity="data.tur === 'SABAH' ? 'info' : data.tur === 'AKSAM' ? 'warn' : 'contrast'"
          />
        </template>
      </Column>
      <Column
        :header="t('vardiyalar.islem')"
        style="width: 120px"
      >
        <template #body="{ data }">
          <Button
            icon="pi pi-trash"
            class="p-button-rounded p-button-text"
            @click="sil(data)"
          />
        </template>
      </Column>
    </DataTable>

    <Dialog
      v-model:visible="dialog"
      :header="dialogHeader"
      modal
      :style="{ width: '500px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('vardiyalar.personelZorunlu') }}</label>
          <Dropdown
            v-model="form.personelId"
            :options="personelListesi"
            option-label="displayName"
            option-value="id"
            :placeholder="t('vardiyalar.personelSec')"
            class="w-full"
            filter
          />
        </div>
        <div class="field">
          <label>{{ t('vardiyalar.tarihZorunlu') }}</label><DatePicker
            v-model="form.tarih"
            date-format="dd/mm/yy"
            class="w-full"
          />
        </div>
        <div class="field-row">
          <div class="field">
            <label>{{ t('vardiyalar.baslangicZorunlu') }}</label><InputText
              v-model="form.baslangic"
              placeholder="08:00"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('vardiyalar.bitisZorunlu') }}</label><InputText
              v-model="form.bitis"
              placeholder="16:00"
              class="w-full"
            />
          </div>
        </div>
        <div class="field">
          <label>{{ t('vardiyalar.turZorunlu') }}</label>
          <Dropdown
            v-model="form.tur"
            :options="turSecenekleri"
            :placeholder="t('common.select')"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="dialog = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="kaydet"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { vardiyaAPI, personelAPI } from '../api/index.js'
import { useI18n } from 'vue-i18n'

const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { t } = useI18n()
const list = ref([])
const personelListesi = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const form = ref({ personelId: null, tarih: new Date(), baslangic: '08:00', bitis: '16:00', tur: 'SABAH' })
const turSecenekleri = ['SABAH', 'AKSAM', 'GECE']

const dialogHeader = computed(() => (duzenleme.value ? t('vardiyalar.duzenle') : t('vardiyalar.yeniVardiya')))

import { formatTarih as formatDate } from '../utils/format.js'

onMounted(async () => {
  yukleniyor.value = true
  try {
    const [vR, pR] = await Promise.all([vardiyaAPI.getAll(), personelAPI.getAll()])
    list.value = vR.data?.content || vR.data || []
    personelListesi.value = pR.data.map((p) => ({
      ...p,
      displayName: p.ad && p.soyad ? `${p.ad} ${p.soyad}` : p.ad || p.id
    }))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('vardiyalar.hataYukleme'))
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
      toastBildirim.basarili(t('vardiyalar.guncellendi'))
    } else {
      await vardiyaAPI.create(payload)
      toastBildirim.basarili(t('vardiyalar.olusturuldu'))
    }
    dialog.value = false
    const r = await vardiyaAPI.getAll()
    list.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('vardiyalar.islemBasarisiz'))
  }
  kaydediliyor.value = false
}

const sil = (data) => {
  const personelAd = data.personelAd || data.id
  confirm.require({
    message: t('vardiyalar.silOnayMesaj', { ad: personelAd }),
    header: t('masraflar.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('masraflar.evetSil'),
    rejectLabel: t('common.cancel'),
    accept: async () => {
      try {
        await vardiyaAPI.delete(data.id)
        list.value = list.value.filter((x) => x.id !== data.id)
        toast.add({ severity: 'success', summary: t('vardiyalar.silindi'), detail: t('vardiyalar.vardiyaSilindi'), life: 3000 })
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('vardiyalar.silmeBasarisiz'))
      }
    }
  })
}
</script>

<style scoped>
.vardiya-container {
  padding: 0;
}
.sayfa-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.form-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.field-row {
  display: flex;
  gap: 12px;
}
.field-row .field {
  flex: 1;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}
.w-full {
  width: 100%;
}
</style>
