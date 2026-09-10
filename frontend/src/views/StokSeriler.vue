<template>
  <div class="stokseri-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        {{ t('stokSeriler.title') }}
      </h1>
      <Button
        :label="t('stokSeriler.yeniSeriLot')"
        icon="pi pi-plus"
        @click="dialogAc()"
      />
    </div>

    <div
      v-if="sonKullanma.length"
      class="skt-uyari"
    >
      <i class="pi pi-exclamation-triangle" />
      <span>{{ t('stokSeriler.sktUyari', { n: sonKullanma.length }) }}</span>
      <div
        v-for="s in sonKullanma.slice(0, 5)"
        :key="s.id"
        class="skt-satir"
      >
        {{ s.stokAdi }} — {{ s.seriNo }} (SKT: {{ formatDate(s.sonKullanmaTarihi) }})
      </div>
    </div>

    <DataTable
      :value="list"
      striped-rows
      :loading="yukleniyor"
    >
      <Column
        field="stokAd"
        :header="t('stokSeriler.urun')"
        sortable
      />
      <Column
        field="seriNo"
        :header="t('stokSeriler.seriNo')"
        sortable
      />
      <Column
        field="lotNo"
        :header="t('stokSeriler.lotNo')"
      />
      <Column
        field="skt"
        :header="t('stokSeriler.skt')"
      >
        <template #body="{ data }">
          {{ formatDate(data.skt) }}
        </template>
      </Column>
      <Column
        :header="t('stokSeriler.islem')"
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
          <label>{{ t('stokSeriler.urunZorunlu') }}</label>
          <Dropdown
            v-model="form.stokId"
            :options="stokListesi"
            option-label="ad"
            option-value="id"
            :placeholder="t('stokSeriler.urunSec')"
            class="w-full"
            filter
          />
        </div>
        <div class="field">
          <label>{{ t('stokSeriler.seriNoZorunlu') }}</label><InputText
            v-model="form.seriNo"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('stokSeriler.lotNo') }}</label><InputText
            v-model="form.lotNo"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('stokSeriler.sonKullanmaTarihi') }}</label><DatePicker
            v-model="form.skt"
            date-format="dd/mm/yy"
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
import { stokSeriAPI, stokAPI } from '../api/index.js'
import { useI18n } from 'vue-i18n'

const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { t } = useI18n()
const list = ref([])
const stokListesi = ref([])
const sonKullanma = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const form = ref({ stokId: null, seriNo: '', lotNo: '', skt: null })

const dialogHeader = computed(() => (duzenleme.value ? t('stokSeriler.seriLotDuzenle') : t('stokSeriler.yeniSeriLot')))

import { formatTarih as formatDate } from '../utils/format.js'

onMounted(async () => {
  yukleniyor.value = true
  try {
    const [sR, stR] = await Promise.all([stokSeriAPI.getAll(), stokAPI.getAll()])
    list.value = sR.data?.content || sR.data || []
    stokListesi.value = stR.data.content || stR.data
    try {
      const sk = await stokSeriAPI.sonKullanma(30)
      sonKullanma.value = sk.data || []
    } catch {
      sonKullanma.value = []
    }
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('stokSeriler.hataYukleme'))
  }
  yukleniyor.value = false
})

const dialogAc = (data) => {
  duzenleme.value = !!data
  form.value = data
    ? { ...data, skt: data.skt ? new Date(data.skt) : null }
    : { stokId: null, seriNo: '', lotNo: '', skt: null }
  dialog.value = true
}

const kaydet = async () => {
  kaydediliyor.value = true
  try {
    const payload = { ...form.value, skt: form.value.skt?.toISOString?.().split('T')[0] ?? form.value.skt }
    if (duzenleme.value) {
      await stokSeriAPI.update(form.value.id, payload)
      toastBildirim.basarili(t('stokSeriler.guncellendi'))
    } else {
      await stokSeriAPI.create(payload)
      toastBildirim.basarili(t('stokSeriler.olusturuldu'))
    }
    dialog.value = false
    const r = await stokSeriAPI.getAll()
    list.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('stokSeriler.islemBasarisiz'))
  }
  kaydediliyor.value = false
}

const sil = (data) => {
  confirm.require({
    message: t('stokSeriler.silOnayMesaj', { seriNo: data.seriNo }),
    header: t('masraflar.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('masraflar.evetSil'),
    rejectLabel: t('common.cancel'),
    accept: async () => {
      try {
        await stokSeriAPI.delete(data.id)
        list.value = list.value.filter((x) => x.id !== data.id)
        toast.add({ severity: 'success', summary: t('stokSeriler.silindi'), detail: t('stokSeriler.seriLotSilindi'), life: 3000 })
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('stokSeriler.silmeBasarisiz'))
      }
    }
  })
}
</script>

<style scoped>
.stokseri-container {
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
.skt-uyari {
  background: rgba(245, 158, 11, 0.12);
  border: 1px solid rgba(245, 158, 11, 0.35);
  border-radius: 10px;
  padding: 12px 14px;
  margin-bottom: 16px;
  color: var(--text-primary);
  font-size: 13px;
}
.skt-uyari > i {
  color: #f59e0b;
  margin-right: 6px;
}
.skt-satir {
  padding: 2px 0 2px 22px;
  font-size: 12px;
  color: var(--text-secondary);
}
</style>
