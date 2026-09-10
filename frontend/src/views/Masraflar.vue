<template>
  <div class="masraf-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        {{ t('masraflar.title') }}
      </h1>
      <Button
        :label="t('masraflar.yeniMasraf')"
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
        field="kategori"
        :header="t('masraflar.kategori')"
        sortable
      />
      <Column
        field="aciklama"
        :header="t('common.description')"
      />
      <Column
        field="tutar"
        :header="t('common.amount')"
      >
        <template #body="{ data }">
          {{ formatCurrency(data.tutar) }}
        </template>
      </Column>
      <Column
        field="belgeNo"
        :header="t('masraflar.belgeNo')"
      />
      <Column
        :header="t('common.actions')"
        style="width: 120px"
      >
        <template #body="{ data }">
          <Button
            icon="pi pi-pencil"
            class="p-button-rounded p-button-text"
            @click="dialogAc(data)"
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
      :message="t('masraflar.empty')"
      :sub-message="t('masraflar.emptyHint')"
      icon="pi pi-receipt"
      :action-label="t('masraflar.yeniMasraf')"
      action-icon="pi pi-plus"
      @action="dialogAc()"
    />

    <Dialog
      v-model:visible="dialog"
      :header="dialogHeader"
      modal
      :style="{ width: '500px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('masraflar.tarihZorunlu') }}</label><DatePicker
            v-model="form.tarih"
            date-format="dd/mm/yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('masraflar.kategori') }}</label><InputText
            v-model="form.kategori"
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
        <div class="field">
          <label>{{ t('masraflar.tutar') }}</label><InputNumber
            v-model="form.tutar"
            mode="currency"
            currency="TRY"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('masraflar.belgeNo') }}</label><InputText
            v-model="form.belgeNo"
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
import { masrafAPI } from '../api/index.js'
import EmptyState from '../components/EmptyState.vue'
import { formatCurrency } from '../utils/format.js'
import { useI18n } from 'vue-i18n'

const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { t } = useI18n()
const list = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const form = ref({ tarih: new Date(), kategori: '', aciklama: '', tutar: 0, belgeNo: '' })

const dialogHeader = computed(() => (duzenleme.value ? t('masraflar.duzenle') : t('masraflar.yeniMasraf')))

import { formatTarih as formatDate } from '../utils/format.js'

onMounted(async () => {
  yukleniyor.value = true
  try {
    const r = await masrafAPI.getAll()
    list.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('masraflar.hataYukleme'))
  }
  yukleniyor.value = false
})

const dialogAc = (data) => {
  duzenleme.value = !!data
  form.value = data
    ? { ...data, tarih: data.tarih ? new Date(data.tarih) : new Date() }
    : { tarih: new Date(), kategori: '', aciklama: '', tutar: 0, belgeNo: '' }
  dialog.value = true
}

const kaydet = async () => {
  kaydediliyor.value = true
  try {
    const payload = { ...form.value, tarih: form.value.tarih?.toISOString?.().split('T')[0] ?? form.value.tarih }
    if (duzenleme.value) {
      await masrafAPI.update(form.value.id, payload)
      toastBildirim.basarili(t('masraflar.guncellendi'))
    } else {
      await masrafAPI.create(payload)
      toastBildirim.basarili(t('masraflar.olusturuldu'))
    }
    dialog.value = false
    const r = await masrafAPI.getAll()
    list.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('masraflar.islemBasarisiz'))
  }
  kaydediliyor.value = false
}

const sil = (data) => {
  confirm.require({
    message: t('masraflar.silOnayMesaj', { n: data.kategori || data.id }),
    header: t('masraflar.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('masraflar.evetSil'),
    rejectLabel: t('common.cancel'),
    accept: async () => {
      try {
        await masrafAPI.delete(data.id)
        list.value = list.value.filter((x) => x.id !== data.id)
        toast.add({ severity: 'success', summary: t('masraflar.silindi'), detail: t('masraflar.masrafSilindi'), life: 3000 })
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('masraflar.silmeBasarisiz'))
      }
    }
  })
}
</script>

<style scoped>
.masraf-container {
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
</style>
