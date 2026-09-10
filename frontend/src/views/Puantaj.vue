<template>
  <div class="puantaj-container">
    <h1>{{ t('puantaj.title') }}</h1>
    <Toolbar class="toolbar">
      <template #start>
        <Button
          :label="t('puantaj.yeniKayit')"
          icon="pi pi-plus"
          class="p-button-success"
          @click="dialogAc()"
        />
      </template>
      <template #end>
        <Dropdown
          v-model="seciliPersonelId"
          :options="personelList"
          option-label="ad"
          option-value="id"
          :placeholder="t('puantaj.personelSecin')"
          class="personel-dropdown"
          @change="loadData"
        />
        <DatePicker
          v-model="filtreBaslangic"
          :placeholder="t('puantaj.baslangic')"
          date-format="dd.mm.yy"
          class="filter-date"
          @update:model-value="loadData"
        />
        <DatePicker
          v-model="filtreBitis"
          :placeholder="t('puantaj.bitis')"
          date-format="dd.mm.yy"
          class="filter-date"
          @update:model-value="loadData"
        />
      </template>
    </Toolbar>
    <DataTable
      :value="list"
      striped-rows
      :loading="yukleniyor"
    >
      <Column
        field="tarih"
        :header="t('common.date')"
      >
        <template #body="{ data }">
          {{ formatDate(data.tarih) }}
        </template>
      </Column>
      <Column
        field="personelAdi"
        :header="t('puantaj.personel')"
      />
      <Column
        field="durum"
        :header="t('common.status')"
      >
        <template #body="{ data }">
          <Tag
            :value="data.durum || 'GELMEDI'"
            :severity="data.durum === 'GELDI' ? 'success' : data.durum === 'IZINLI' ? 'warn' : 'danger'"
          />
        </template>
      </Column>
      <Column
        field="aciklama"
        :header="t('common.description')"
      />
      <Column
        :header="t('puantaj.islem')"
        style="width: 120px"
      >
        <template #body="{ data }">
          <Button
            icon="pi pi-pencil"
            class="p-button-rounded p-button-info p-button-sm"
            @click="dialogAc(data)"
          />
          <Button
            icon="pi pi-trash"
            class="p-button-rounded p-button-danger p-button-sm"
            @click="sil(data)"
          />
        </template>
      </Column>
    </DataTable>
    <Message
      v-if="list && list.length === 0"
      severity="info"
      :text="t('puantaj.kayitBulunamadi')"
    />
    <Dialog
      v-model:visible="dialog"
      :header="duzenleme ? t('puantaj.duzenle') : t('puantaj.yeni')"
      modal
      :style="{ width: '500px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('puantaj.personelZorunlu') }}</label><Dropdown
            v-model="form.personelId"
            :options="personelList"
            option-label="ad"
            option-value="id"
            :placeholder="t('faturalar.seciniz')"
            class="w-full"
            filter
          />
        </div>
        <div class="field">
          <label>{{ t('puantaj.tarihZorunlu') }}</label><DatePicker
            v-model="form.tarih"
            date-format="dd.mm.yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('common.status') }}</label><Dropdown
            v-model="form.durum"
            :options="durumList"
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
import { ref, onMounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { puantajAPI, personelAPI } from '../api/index.js'
import { useI18n } from 'vue-i18n'

const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { t } = useI18n()
const list = ref([])
const personelList = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const seciliPersonelId = ref(null)
const filtreBaslangic = ref(new Date(new Date().getFullYear(), new Date().getMonth(), 1))
const filtreBitis = ref(new Date())
const durumList = ['GELDI', 'GELMEDI', 'IZINLI', 'MAZERETLI']
const form = ref({ personelId: null, tarih: new Date(), durum: 'GELDI', aciklama: '' })

onMounted(async () => {
  try {
    const personelRes = await personelAPI.getAll()
    personelList.value = personelRes.data || []
    if (personelList.value.length) seciliPersonelId.value = personelList.value[0].id
    await loadData()
  } catch {
    toastBildirim.hata(t('puantaj.personelYuklenemedi'))
  }
})

const loadData = async () => {
  if (!seciliPersonelId.value) return
  yukleniyor.value = true
  try {
    const bas = filtreBaslangic.value?.toISOString().split('T')[0]
    const bit = filtreBitis.value?.toISOString().split('T')[0]
    const r = await puantajAPI.getByPersonel(seciliPersonelId.value, bas, bit)
    list.value = r.data || []
  } catch {
    toastBildirim.hata(t('puantaj.veriYuklenemedi'))
  }
  yukleniyor.value = false
}

import { formatTarih as formatDate } from '../utils/format.js'

const dialogAc = (data) => {
  duzenleme.value = !!data
  form.value = data
    ? {
        personelId: data.personelId,
        tarih: new Date(data.tarih),
        durum: data.durum || 'GELDI',
        aciklama: data.aciklama || ''
      }
    : { personelId: seciliPersonelId.value, tarih: new Date(), durum: 'GELDI', aciklama: '' }
  dialog.value = true
}

const kaydet = async () => {
  if (!form.value.personelId) {
    toastBildirim.uyari(t('puantaj.personelSeciniz'))
    return
  }
  kaydediliyor.value = true
  try {
    const payload = { ...form.value, tarih: form.value.tarih?.toISOString().split('T')[0] }
    if (duzenleme.value) {
      await puantajAPI.update(form.value.id, payload)
      toastBildirim.basarili(t('puantaj.guncellendi'))
    } else {
      await puantajAPI.create(payload)
      toastBildirim.basarili(t('puantaj.eklendi'))
    }
    dialog.value = false
    await loadData()
  } catch {
    toastBildirim.hata(t('puantaj.islemBasarisiz'))
  }
  kaydediliyor.value = false
}

const sil = (data) => {
  confirm.require({
    message: t('common.confirmDelete'),
    header: t('kasa.onay'),
    icon: 'pi pi-exclamation-triangle',
    accept: async () => {
      try {
        await puantajAPI.delete(data.id)
        await loadData()
        toast.add({ severity: 'success', summary: t('puantaj.silindi'), life: 3000 })
      } catch {
        toastBildirim.hata(t('puantaj.silmeBasarisiz'))
      }
    }
  })
}
</script>

<style scoped>
.puantaj-container {
  padding: 0;
  max-width: 100%;
}
h1 {
  color: var(--text-primary);
  margin-bottom: 20px;
  font-size: 28px;
  font-weight: 700;
}
.toolbar {
  margin-bottom: 20px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px 18px;
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
.personel-dropdown {
  width: 250px !important;
}
.filter-date {
  width: 140px !important;
  margin-left: 8px;
}
</style>
