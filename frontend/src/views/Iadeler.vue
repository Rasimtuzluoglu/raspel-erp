<template>
  <div class="iade-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        {{ t('iadeler.title') }}
      </h1>
      <Button
        :label="t('iadeler.yeniIade')"
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
        field="tarih"
        :header="t('common.date')"
        sortable
      >
        <template #body="{ data }">
          {{ formatDate(data.tarih) }}
        </template>
      </Column>
      <Column
        field="cariHesapAd"
        :header="t('iadeler.cariHesap')"
        sortable
      >
        <template #body="{ data }">
          {{ data.cariHesapAd || data.cariHesapId || '-' }}
        </template>
      </Column>
      <Column
        field="tutar"
        :header="t('common.amount')"
      >
        <template #body="{ data }">
          {{ formatCurrency(data.tutar) }}
        </template>
      </Column>
      <Column
        field="kalemSayisi"
        :header="t('iadeler.kalem')"
      >
        <template #body="{ data }">
          {{ t('iadeler.nKalem', { n: data.kalemler?.length || 0 }) }}
        </template>
      </Column>
      <Column
        field="durum"
        :header="t('common.status')"
      >
        <template #body="{ data }">
          <Tag
            :value="data.durum"
            :severity="data.durum === 'ONAYLANDI' ? 'success' : data.durum === 'IPTAL' ? 'danger' : 'warn'"
          />
        </template>
      </Column>
      <Column
        field="aciklama"
        :header="t('common.description')"
      />
      <Column
        :header="t('iadeler.islem')"
        style="width: 200px"
      >
        <template #body="{ data }">
          <Button
            v-if="data.durum !== 'ONAYLANDI'"
            icon="pi pi-check-circle"
            class="p-button-rounded p-button-text p-button-success"
            :title="t('iadeler.onayla')"
            @click="durumGuncelle(data, 'ONAYLANDI')"
          />
          <Button
            v-if="data.durum !== 'IPTAL'"
            icon="pi pi-times-circle"
            class="p-button-rounded p-button-text p-button-danger"
            :title="t('common.cancel')"
            @click="durumGuncelle(data, 'IPTAL')"
          />
          <Button
            icon="pi pi-trash"
            class="p-button-rounded p-button-text"
            @click="sil(data)"
          />
        </template>
      </Column>
    </DataTable>

    <EmptyState
      v-if="!yukleniyor && list.length === 0"
      :message="t('iadeler.empty')"
      :sub-message="t('iadeler.emptyHint')"
      icon="pi pi-undo"
      :action-label="t('iadeler.yeniIade')"
      action-icon="pi pi-plus"
      @action="dialogAc()"
    />

    <Dialog
      v-model:visible="dialog"
      :header="dialogHeader"
      modal
      :style="{ width: '700px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('iadeler.iadeTuruZorunlu') }}</label>
          <Dropdown
            v-model="form.tur"
            :options="[
              { label: t('iadeler.satisIadesi'), value: 'SATIS' },
              { label: t('iadeler.alisIadesi'), value: 'ALIS' }
            ]"
            option-label="label"
            option-value="value"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('iadeler.cariHesapZorunlu') }}</label>
          <Dropdown
            v-model="form.cariHesapId"
            :options="cariList"
            option-label="ad"
            option-value="id"
            :placeholder="t('iadeler.cariHesapSeciniz')"
            class="w-full"
            filter
            @change="cariSecildi"
          />
        </div>
        <div class="field">
          <label>{{ t('iadeler.tarihZorunlu') }}</label><DatePicker
            v-model="form.tarih"
            date-format="dd/mm/yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('common.description') }}</label><Textarea
            v-model="form.aciklama"
            rows="2"
            class="w-full"
          />
        </div>

        <div class="kalem-section">
          <div class="kalem-header">
            <h3>{{ t('iadeler.iadeKalemleri') }}</h3>
            <Button
              :label="t('iadeler.kalemEkle')"
              icon="pi pi-plus"
              size="small"
              @click="kalemEkle"
            />
          </div>

          <div
            v-for="(k, i) in form.kalemler"
            :key="i"
            class="kalem-row"
          >
            <Dropdown
              v-model="k.stokId"
              :options="stokList"
              option-label="ad"
              option-value="id"
              :placeholder="t('iadeler.stokSec')"
              class="kalem-stok"
              filter
            />
            <InputNumber
              v-model="k.miktar"
              :min="0"
              :min-fraction-digits="0"
              :placeholder="t('iadeler.miktar')"
              class="kalem-miktar"
            />
            <InputNumber
              v-model="k.birimFiyat"
              :min="0"
              :min-fraction-digits="2"
              :placeholder="t('iadeler.brFiyat')"
              class="kalem-fiyat"
            />
            <Dropdown
              v-model="k.kdvOrani"
              :options="[0, 10, 20]"
              class="kalem-kdv"
            />
            <Button
              icon="pi pi-trash"
              class="p-button-rounded p-button-danger p-button-sm"
              @click="form.kalemler.splice(i, 1)"
            />
          </div>

          <div
            v-if="form.kalemler.length"
            class="kalem-tutar"
          >
            <span>{{ t('iadeler.toplam') }}: {{ formatCurrency(kalemToplam) }}</span>
          </div>
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
import { iadeAPI, stokAPI, cariHesapAPI } from '../api/index.js'
import EmptyState from '../components/EmptyState.vue'
import { formatCurrency } from '../utils/format.js'
import { useI18n } from 'vue-i18n'

const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { t } = useI18n()
const list = ref([])
const stokList = ref([])
const cariList = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const form = ref({
  cariHesapId: null,
  cariHesapAd: '',
  tur: 'SATIS',
  tarih: new Date(),
  tutar: 0,
  aciklama: '',
  kalemler: []
})

const dialogHeader = computed(() => (duzenleme.value ? t('iadeler.duzenle') : t('iadeler.yeniIade')))

const kalemToplam = computed(() => {
  return form.value.kalemler.reduce((t, k) => t + (k.miktar || 0) * (k.birimFiyat || 0), 0)
})

import { formatTarih as formatDate } from '../utils/format.js'

const cariSecildi = () => {
  const secilen = cariList.value.find((c) => c.id === form.value.cariHesapId)
  if (secilen) form.value.cariHesapAd = secilen.ad
}

onMounted(async () => {
  yukleniyor.value = true
  try {
    const [r, stokRes, cariRes] = await Promise.all([iadeAPI.getAll(), stokAPI.getAll(), cariHesapAPI.getAll()])
    list.value = r.data?.content || r.data || []
    stokList.value = stokRes.data?.content || stokRes.data || []
    cariList.value = cariRes.data?.content || cariRes.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('iadeler.hataYukleme'))
  }
  yukleniyor.value = false
})

const kalemEkle = () => {
  form.value.kalemler.push({ stokId: null, miktar: 1, birimFiyat: 0, kdvOrani: 20 })
}

const dialogAc = (data) => {
  duzenleme.value = !!data
  form.value = data
    ? {
        ...data,
        tarih: data.tarih ? new Date(data.tarih) : new Date(),
        tur: data.tur || 'SATIS',
        kalemler: data.kalemler?.map((k) => ({ ...k })) || []
      }
    : { cariHesapId: null, cariHesapAd: '', tur: 'SATIS', tarih: new Date(), tutar: 0, aciklama: '', kalemler: [] }
  dialog.value = true
}

const kaydet = async () => {
  kaydediliyor.value = true
  try {
    const payload = {
      ...form.value,
      tarih: form.value.tarih?.toISOString?.().split('T')[0] ?? form.value.tarih,
      tutar: kalemToplam.value,
      kalemler: form.value.kalemler.map((k) => ({
        stokId: k.stokId,
        miktar: k.miktar,
        birimFiyat: k.birimFiyat,
        kdvOrani: k.kdvOrani
      }))
    }
    if (duzenleme.value) {
      await iadeAPI.update(form.value.id, payload)
      toastBildirim.basarili(t('iadeler.guncellendi'))
    } else {
      await iadeAPI.create(payload)
      toastBildirim.basarili(t('iadeler.olusturuldu'))
    }
    dialog.value = false
    const r = await iadeAPI.getAll()
    list.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('iadeler.islemBasarisiz'))
  }
  kaydediliyor.value = false
}

const durumGuncelle = async (data, durum) => {
  try {
    await iadeAPI.durumGuncelle(data.id, durum)
    const r = await iadeAPI.getAll()
    list.value = r.data?.content || r.data || []
    toastBildirim.basarili(t('iadeler.durumGuncellendi', { durum }))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('iadeler.hataDurum'))
  }
}

const sil = (data) => {
  confirm.require({
    message: t('iadeler.silOnayMesaj'),
    header: t('masraflar.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('masraflar.evetSil'),
    rejectLabel: t('common.cancel'),
    accept: async () => {
      try {
        await iadeAPI.delete(data.id)
        list.value = list.value.filter((x) => x.id !== data.id)
        toast.add({ severity: 'success', summary: t('iadeler.silindi'), detail: t('iadeler.iadeSilindi'), life: 3000 })
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('iadeler.silmeBasarisiz'))
      }
    }
  })
}
</script>

<style scoped>
.iade-container {
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
.kalem-section {
  border-top: 1px solid var(--border);
  padding-top: 12px;
}
.kalem-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.kalem-header h3 {
  margin: 0;
  font-size: 15px;
}
.kalem-row {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.kalem-stok {
  flex: 2;
  min-width: 160px;
}
.kalem-miktar {
  flex: 1;
  min-width: 80px;
}
.kalem-fiyat {
  flex: 1;
  min-width: 100px;
}
.kalem-kdv {
  width: 70px;
}
.kalem-tutar {
  text-align: right;
  font-weight: bold;
  font-size: 15px;
  padding: 8px 0;
  border-top: 1px solid var(--border);
  margin-top: 4px;
}
</style>
