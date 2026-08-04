<template>
  <div class="donemler-sayfasi">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        Dönemler
      </h1>
      <div class="baslik-aksiyon">
        <Dropdown
          v-model="seciliSirketId"
          :options="sirketler"
          option-label="ad"
          option-value="id"
          placeholder="Şirket Seçin"
          class="sirket-dropdown"
          @change="donemleriYukle"
        />
        <Button
          label="Yeni Dönem"
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
        style="width:60px"
      />
      <Column
        field="ad"
        header="Dönem Adı"
        sortable
      />
      <Column
        field="baslangic"
        header="Başlangıç"
      >
        <template #body="{ data }">
          {{ data.baslangic }}
        </template>
      </Column>
      <Column
        field="bitis"
        header="Bitiş"
      >
        <template #body="{ data }">
          {{ data.bitis }}
        </template>
      </Column>
      <Column
        field="aktif"
        header="Durum"
      >
        <template #body="{ data }">
          <Tag
            :value="data.aktif ? 'Aktif' : 'Pasif'"
            :severity="data.aktif ? 'success' : 'danger'"
          />
        </template>
      </Column>
      <Column
        header="İşlem"
        style="width:120px"
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
      :header="duzenleme ? 'Dönem Düzenle' : 'Yeni Dönem'"
      modal
      :style="{ width: '450px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>Dönem Adı *</label>
          <InputText
            v-model="form.ad"
            class="w-full"
            placeholder="Örn: 2026 Yılı"
          />
        </div>
        <div class="field">
          <label>Başlangıç Tarihi *</label>
          <DatePicker
            v-model="form.baslangic"
            date-format="dd/mm/yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Bitiş Tarihi *</label>
          <DatePicker
            v-model="form.bitis"
            date-format="dd/mm/yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Aktif</label>
          <InputSwitch v-model="form.aktif" />
        </div>
      </div>
      <template #footer>
        <Button
          label="İptal"
          icon="pi pi-times"
          class="p-button-text"
          @click="dialog = false"
        />
        <Button
          label="Kaydet"
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
const toastBildirim = useToastBildirim()
const confirm = useConfirm()

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
    toastBildirim.hata(err?.response?.data?.message || err?.message || 'Şirketler yüklenirken hata oluştu')
  }
})

const donemleriYukle = async () => {
  if (!seciliSirketId.value) { donemler.value = []; return }
  yukleniyor.value = true
  try {
    const r = await donemAPI.getBySirket(seciliSirketId.value)
    donemler.value = r.data
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || 'Dönemler yüklenirken hata oluştu')
  }
  yukleniyor.value = false
}

const dialogAc = (data) => {
  duzenleme.value = !!data
  seciliId.value = data?.id || null
  form.value = data ? { ...data, baslangic: data.baslangic ? new Date(data.baslangic) : null, bitis: data.bitis ? new Date(data.bitis) : null } : { ad: '', baslangic: null, bitis: null, sirketId: seciliSirketId.value, aktif: true }
  dialog.value = true
}

const kaydet = async () => {
  kaydediliyor.value = true
  try {
    const payload = { ...form.value, sirketId: seciliSirketId.value, baslangic: form.value.baslangic?.toISOString().split('T')[0], bitis: form.value.bitis?.toISOString().split('T')[0] }
    if (duzenleme.value) {
      await donemAPI.update(seciliId.value, payload)
    } else {
      await donemAPI.create(payload)
    }
    dialog.value = false
    await donemleriYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || 'Dönem kaydedilirken hata oluştu')
  }
  kaydediliyor.value = false
}

const sil = (data) => {
  confirm.require({
    message: 'Bu kaydı silmek istediğinize emin misiniz?',
    header: 'Silme Onayı',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Evet, Sil',
    rejectLabel: 'İptal',
    accept: async () => {
      try {
        await donemAPI.delete(data.id)
        await donemleriYukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || err?.message || 'Dönem silinirken hata oluştu')
      }
    },
    reject: () => {}
  })
}
</script>

<style scoped>
.donemler-sayfasi { padding: 0; }
.sayfa-baslik { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; gap: 16px; }
.sayfa-baslik h1 { margin: 0; }
.baslik-aksiyon { display: flex; gap: 12px; align-items: center; }
.sirket-dropdown { min-width: 200px; }
.form-grid { display: flex; flex-direction: column; gap: 16px; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field label { font-size: 13px; font-weight: 600; color: var(--text-secondary); }
.w-full { width: 100%; }
</style>
