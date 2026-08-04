<template>
  <div class="fiyat-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        Fiyat Listesi
      </h1>
      <Button
        label="Yeni Fiyat"
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
        field="alisFiyati"
        header="Alış Fiyatı"
      >
        <template #body="{ data }">
          {{ formatCurrency(data.alisFiyati) }}
        </template>
      </Column>
      <Column
        field="satisFiyati"
        header="Satış Fiyatı"
      >
        <template #body="{ data }">
          {{ formatCurrency(data.satisFiyati) }}
        </template>
      </Column>
      <Column
        field="gecerlilikBaslangic"
        header="Başlangıç"
      >
        <template #body="{ data }">
          {{ formatDate(data.gecerlilikBaslangic) }}
        </template>
      </Column>
      <Column
        field="gecerlilikBitis"
        header="Bitiş"
      >
        <template #body="{ data }">
          {{ formatDate(data.gecerlilikBitis) }}
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
          <label>Alış Fiyatı</label><InputNumber
            v-model="form.alisFiyati"
            mode="currency"
            currency="TRY"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Satış Fiyatı *</label><InputNumber
            v-model="form.satisFiyati"
            mode="currency"
            currency="TRY"
            class="w-full"
          />
        </div>
        <div class="field-row">
          <div class="field">
            <label>Geçerlilik Başlangıç</label><DatePicker
              v-model="form.gecerlilikBaslangic"
              date-format="dd/mm/yy"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>Geçerlilik Bitiş</label><DatePicker
              v-model="form.gecerlilikBitis"
              date-format="dd/mm/yy"
              class="w-full"
            />
          </div>
        </div>
        <div class="field">
          <label>Açıklama</label><Textarea
            v-model="form.aciklama"
            rows="2"
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
import { fiyatListesiAPI, stokAPI } from '../api/index.js'

const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const list = ref([])
const stokListesi = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const form = ref({ stokId: null, alisFiyati: 0, satisFiyati: 0, gecerlilikBaslangic: new Date(), gecerlilikBitis: null, aciklama: '' })

const dialogHeader = computed(() => duzenleme.value ? 'Fiyat Düzenle' : 'Yeni Fiyat')

const formatCurrency = (v) => {
  if (v === null || v === undefined) return '0,00 ₺'
  return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(v)
}
const formatDate = (d) => {
  if (!d) return '-'
  return new Intl.DateTimeFormat('tr-TR', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(d))
}

onMounted(async () => {
  yukleniyor.value = true
  try {
    const [fR, sR] = await Promise.all([fiyatListesiAPI.getAll(), stokAPI.getAll()])
    list.value = fR.data?.content || fR.data || []
    stokListesi.value = sR.data.content || sR.data
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Veriler yüklenemedi')
  }
  yukleniyor.value = false
})

const dialogAc = (data) => {
  duzenleme.value = !!data
  form.value = data
    ? { ...data, gecerlilikBaslangic: data.gecerlilikBaslangic ? new Date(data.gecerlilikBaslangic) : new Date(), gecerlilikBitis: data.gecerlilikBitis ? new Date(data.gecerlilikBitis) : null }
    : { stokId: null, alisFiyati: 0, satisFiyati: 0, gecerlilikBaslangic: new Date(), gecerlilikBitis: null, aciklama: '' }
  dialog.value = true
}

const formatDateForApi = (d) => d?.toISOString?.().split('T')[0] ?? d

const kaydet = async () => {
  kaydediliyor.value = true
  try {
    const payload = { ...form.value, gecerlilikBaslangic: formatDateForApi(form.value.gecerlilikBaslangic), gecerlilikBitis: formatDateForApi(form.value.gecerlilikBitis) }
    if (duzenleme.value) {
      await fiyatListesiAPI.update(form.value.id, payload)
      toastBildirim.basarili('Fiyat güncellendi')
    } else {
      await fiyatListesiAPI.create(payload)
      toastBildirim.basarili('Fiyat oluşturuldu')
    }
    dialog.value = false
    const r = await fiyatListesiAPI.getAll(); list.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'İşlem başarısız')
  }
  kaydediliyor.value = false
}

const sil = (data) => {
  const urunAd = data.stokAd || data.id
  confirm.require({
    message: `"${urunAd}" fiyat kaydını silmek istediğinize emin misiniz?`,
    header: 'Silme Onayı',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Evet, Sil',
    rejectLabel: 'İptal',
    accept: async () => {
      try {
        await fiyatListesiAPI.delete(data.id)
        list.value = list.value.filter(x => x.id !== data.id)
        toast.add({ severity: 'success', summary: 'Silindi', detail: 'Fiyat kaydı silindi', life: 3000 })
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || 'Silme başarısız')
      }
    }
  })
}
</script>

<style scoped>
.fiyat-container { padding: 0; }
.sayfa-baslik { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.form-grid { display: flex; flex-direction: column; gap: 16px; }
.field-row { display: flex; gap: 12px; }
.field-row .field { flex: 1; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field label { font-size: 13px; font-weight: 600; color: var(--text-secondary); }
.w-full { width: 100%; }
</style>