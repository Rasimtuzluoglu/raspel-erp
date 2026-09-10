<template>
  <div class="donemler-sayfasi">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        {{ t('donemler.title') }}
      </h1>
      <div class="baslik-aksiyon">
        <Dropdown
          v-model="seciliSirketId"
          :options="sirketler"
          option-label="ad"
          option-value="id"
          :placeholder="t('donemler.sirketSecin')"
          class="sirket-dropdown"
          @change="donemleriYukle"
        />
        <Button
          :label="t('donemler.yeniDonem')"
          icon="pi pi-plus"
          :disabled="!seciliSirketId"
          @click="dialogAc"
        />
      </div>
    </div>

    <DataTable
      :value="donemler"
      striped-rows
      responsive-layout="scroll"
      :loading="yukleniyor"
    >
      <Column
        field="id"
        header="#"
        style="width: 60px"
      />
      <Column
        field="ad"
        :header="t('donemler.donemAdi')"
        sortable
      />
      <Column
        field="baslangic"
        :header="t('donemler.baslangic')"
      >
        <template #body="{ data }">
          {{ data.baslangic }}
        </template>
      </Column>
      <Column
        field="bitis"
        :header="t('donemler.bitis')"
      >
        <template #body="{ data }">
          {{ data.bitis }}
        </template>
      </Column>
      <Column
        field="aktif"
        :header="t('common.status')"
      >
        <template #body="{ data }">
          <Tag
            :value="data.aktif ? t('donemler.aktif') : t('donemler.pasif')"
            :severity="data.aktif ? 'success' : 'danger'"
          />
        </template>
      </Column>
      <Column
        :header="t('donemler.islem')"
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
            class="p-button-rounded p-button-text p-button-danger"
            @click="sil(data)"
          />
        </template>
      </Column>
    </DataTable>

    <Dialog
      v-model:visible="dialog"
      :header="duzenleme ? t('donemler.donemDuzenle') : t('donemler.yeniDonem')"
      modal
      :style="{ width: '450px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('donemler.donemAdiZorunlu') }}</label>
          <InputText
            v-model="form.ad"
            class="w-full"
            :placeholder="t('donemler.donemAdiPlaceholder')"
          />
        </div>
        <div class="field">
          <label>{{ t('donemler.baslangicTarihiZorunlu') }}</label>
          <DatePicker
            v-model="form.baslangic"
            date-format="dd/mm/yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('donemler.bitisTarihiZorunlu') }}</label>
          <DatePicker
            v-model="form.bitis"
            date-format="dd/mm/yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('donemler.aktif') }}</label>
          <InputSwitch v-model="form.aktif" />
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
import { useConfirm } from 'primevue/useconfirm'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { donemAPI, sirketAPI } from '../api/index.js'
import { useI18n } from 'vue-i18n'
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { t } = useI18n()

const donemler = ref([])
const sirketler = ref([])
const seciliSirketId = ref(null)
const yukleniyor = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const kaydediliyor = ref(false)
const seciliId = ref(null)
const form = ref({ ad: '', baslangic: null, bitis: null, aktif: true })

onMounted(async () => {
  try {
    const r = await sirketAPI.getAktif()
    sirketler.value = r.data
    if (sirketler.value.length > 0) {
      seciliSirketId.value = sirketler.value[0].id
      await donemleriYukle()
    }
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('donemler.hataSirketler'))
  }
})

const donemleriYukle = async () => {
  if (!seciliSirketId.value) {
    donemler.value = []
    return
  }
  yukleniyor.value = true
  try {
    const r = await donemAPI.getBySirket(seciliSirketId.value)
    donemler.value = r.data
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('donemler.hataYukleme'))
  }
  yukleniyor.value = false
}

const dialogAc = (data) => {
  duzenleme.value = !!data
  seciliId.value = data?.id || null
  form.value = data
    ? {
        ...data,
        baslangic: data.baslangic ? new Date(data.baslangic) : null,
        bitis: data.bitis ? new Date(data.bitis) : null
      }
    : { ad: '', baslangic: null, bitis: null, sirketId: seciliSirketId.value, aktif: true }
  dialog.value = true
}

const kaydet = async () => {
  kaydediliyor.value = true
  try {
    const payload = {
      ...form.value,
      sirketId: seciliSirketId.value,
      baslangic: form.value.baslangic?.toISOString().split('T')[0],
      bitis: form.value.bitis?.toISOString().split('T')[0]
    }
    if (duzenleme.value) {
      await donemAPI.update(seciliId.value, payload)
    } else {
      await donemAPI.create(payload)
    }
    dialog.value = false
    await donemleriYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('donemler.hataKaydet'))
  }
  kaydediliyor.value = false
}

const sil = (data) => {
  confirm.require({
    message: t('common.confirmDelete'),
    header: t('masraflar.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('masraflar.evetSil'),
    rejectLabel: t('common.cancel'),
    accept: async () => {
      try {
        await donemAPI.delete(data.id)
        await donemleriYukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || err?.message || t('donemler.hataSil'))
      }
    },
    reject: () => {}
  })
}
</script>

<style scoped>
.donemler-sayfasi {
  padding: 0;
}
.sayfa-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  gap: 16px;
  flex-wrap: wrap;
}
.sayfa-baslik h1 {
  margin: 0;
}
.baslik-aksiyon {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}
.sirket-dropdown {
  min-width: min(200px, 100%);
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
