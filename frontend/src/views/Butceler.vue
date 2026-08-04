<template>
  <div class="butce-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        Bütçe Yönetimi
      </h1>
      <Button
        label="Yeni Bütçe"
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
        field="ad"
        header="Ad"
        sortable
      />
      <Column
        field="yil"
        header="Yıl"
        sortable
      />
      <Column
        field="ay"
        header="Ay"
        sortable
      />
      <Column
        field="tur"
        header="Tür"
      >
        <template #body="{ data }">
          <Tag
            :value="data.tur === 'GELIR' ? 'Gelir' : 'Gider'"
            :severity="data.tur === 'GELIR' ? 'success' : 'danger'"
          />
        </template>
      </Column>
      <Column
        field="tutar"
        header="Tutar"
      >
        <template #body="{ data }">
          {{ formatCurrency(data.tutar) }}
        </template>
      </Column>
      <Column
        field="kategori"
        header="Kategori"
      />
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
          <label>Ad *</label><InputText
            v-model="form.ad"
            class="w-full"
          />
        </div>
        <div class="field-row">
          <div class="field">
            <label>Yıl</label><InputNumber
              v-model="form.yil"
              class="w-full"
              :min="2000"
              :max="2100"
              :use-grouping="false"
            />
          </div>
          <div class="field">
            <label>Ay (1-12)</label><InputNumber
              v-model="form.ay"
              class="w-full"
              :min="1"
              :max="12"
              :use-grouping="false"
            />
          </div>
        </div>
        <div class="field">
          <label>Tür *</label>
          <Dropdown
            v-model="form.tur"
            :options="turSecenekleri"
            placeholder="Seçin"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Tutar</label><InputNumber
            v-model="form.tutar"
            mode="currency"
            currency="TRY"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Kategori</label><InputText
            v-model="form.kategori"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Açıklama</label><Textarea
            v-model="form.aciklama"
            rows="3"
            class="w-full"
          />
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
import { ref, onMounted, computed } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import { butceAPI } from '../api/index.js'

const toast = useToast()
const confirm = useConfirm()
const list = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const form = ref({ ad: '', yil: new Date().getFullYear(), ay: new Date().getMonth() + 1, tur: 'GELIR', tutar: 0, kategori: '', aciklama: '' })
const turSecenekleri = ['GELIR', 'GIDER']

const dialogHeader = computed(() => duzenleme.value ? 'Bütçe Düzenle' : 'Yeni Bütçe')

const formatCurrency = (v) => {
  if (v === null || v === undefined) return '0,00 ₺'
  return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(v)
}

onMounted(async () => {
  yukleniyor.value = true
  try { const r = await butceAPI.getAll(); list.value = r.data?.content || r.data || [] } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || 'Bütçeler yüklenemedi', life: 5000 })
  }
  yukleniyor.value = false
})

const dialogAc = (data) => {
  duzenleme.value = !!data
  form.value = data ? { ...data } : { ad: '', yil: new Date().getFullYear(), ay: new Date().getMonth() + 1, tur: 'GELIR', tutar: 0, kategori: '', aciklama: '' }
  dialog.value = true
}

const kaydet = async () => {
  kaydediliyor.value = true
  try {
    if (duzenleme.value) {
      await butceAPI.update(form.value.id, form.value)
      toast.add({ severity: 'success', summary: 'Başarılı', detail: 'Bütçe güncellendi', life: 3000 })
    } else {
      await butceAPI.create(form.value)
      toast.add({ severity: 'success', summary: 'Başarılı', detail: 'Bütçe oluşturuldu', life: 3000 })
    }
    dialog.value = false
    const r = await butceAPI.getAll(); list.value = r.data?.content || r.data || []
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || 'İşlem başarısız', life: 5000 })
  }
  kaydediliyor.value = false
}

const sil = (data) => {
  confirm.require({
    message: `${data.ad} bütçesini silmek istediğinize emin misiniz?`,
    header: 'Silme Onayı',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Evet, Sil',
    rejectLabel: 'İptal',
    accept: async () => {
      try {
        await butceAPI.delete(data.id)
        list.value = list.value.filter(x => x.id !== data.id)
        toast.add({ severity: 'success', summary: 'Silindi', detail: 'Bütçe silindi', life: 3000 })
      } catch (err) {
        toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || 'Silme başarısız', life: 5000 })
      }
    }
  })
}
</script>

<style scoped>
.butce-container { padding: 0; }
.sayfa-baslik { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.form-grid { display: flex; flex-direction: column; gap: 16px; }
.field-row { display: flex; gap: 12px; }
.field-row .field { flex: 1; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field label { font-size: 13px; font-weight: 600; color: var(--text-secondary); }
.w-full { width: 100%; }
</style>
