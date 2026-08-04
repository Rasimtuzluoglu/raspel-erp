<template>
  <div class="stokseri-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        Seri/Lot Takibi
      </h1>
      <Button
        label="Yeni Seri/Lot"
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
        field="stokAd"
        header="Ürün"
        sortable
      />
      <Column
        field="seriNo"
        header="Seri No"
        sortable
      />
      <Column
        field="lotNo"
        header="Lot No"
      />
      <Column
        field="skt"
        header="SKT"
      >
        <template #body="{ data }">
          {{ formatDate(data.skt) }}
        </template>
      </Column>
      <Column
        header="İşlem"
        style="width:120px"
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
          <label>Ürün *</label>
          <Dropdown
            v-model="form.stokId"
            :options="stokListesi"
            option-label="ad"
            option-value="id"
            placeholder="Ürün Seç"
            class="w-full"
            filter
          />
        </div>
        <div class="field">
          <label>Seri No *</label><InputText
            v-model="form.seriNo"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Lot No</label><InputText
            v-model="form.lotNo"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Son Kullanma Tarihi</label><DatePicker
            v-model="form.skt"
            date-format="dd/mm/yy"
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
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { stokSeriAPI, stokAPI } from '../api/index.js'

const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const list = ref([])
const stokListesi = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const form = ref({ stokId: null, seriNo: '', lotNo: '', skt: null })

const dialogHeader = computed(() => duzenleme.value ? 'Seri/Lot Düzenle' : 'Yeni Seri/Lot')

const formatDate = (d) => {
  if (!d) return '-'
  return new Intl.DateTimeFormat('tr-TR', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(d))
}

onMounted(async () => {
  yukleniyor.value = true
  try {
    const [sR, stR] = await Promise.all([stokSeriAPI.getAll(), stokAPI.getAll()])
    list.value = sR.data?.content || sR.data || []
    stokListesi.value = stR.data.content || stR.data
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Veriler yüklenemedi')
  }
  yukleniyor.value = false
})

const dialogAc = (data) => {
  duzenleme.value = !!data
  form.value = data ? { ...data, skt: data.skt ? new Date(data.skt) : null } : { stokId: null, seriNo: '', lotNo: '', skt: null }
  dialog.value = true
}

const kaydet = async () => {
  kaydediliyor.value = true
  try {
    const payload = { ...form.value, skt: form.value.skt?.toISOString?.().split('T')[0] ?? form.value.skt }
    if (duzenleme.value) {
      await stokSeriAPI.update(form.value.id, payload)
      toastBildirim.basarili('Seri/Lot güncellendi')
    } else {
      await stokSeriAPI.create(payload)
      toastBildirim.basarili('Seri/Lot oluşturuldu')
    }
    dialog.value = false
    const r = await stokSeriAPI.getAll(); list.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'İşlem başarısız')
  }
  kaydediliyor.value = false
}

const sil = (data) => {
  confirm.require({
    message: `"${data.seriNo}" seri numaralı kaydı silmek istediğinize emin misiniz?`,
    header: 'Silme Onayı',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Evet, Sil',
    rejectLabel: 'İptal',
    accept: async () => {
      try {
        await stokSeriAPI.delete(data.id)
        list.value = list.value.filter(x => x.id !== data.id)
        toast.add({ severity: 'success', summary: 'Silindi', detail: 'Seri/Lot silindi', life: 3000 })
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || 'Silme başarısız')
      }
    }
  })
}
</script>

<style scoped>
.stokseri-container { padding: 0; }
.sayfa-baslik { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.form-grid { display: flex; flex-direction: column; gap: 16px; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field label { font-size: 13px; font-weight: 600; color: var(--text-secondary); }
.w-full { width: 100%; }
</style>