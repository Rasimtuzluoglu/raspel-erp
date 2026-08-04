<template>
  <div class="iade-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        İade Yönetimi
      </h1>
      <Button
        label="Yeni İade"
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
        header="Tarih"
        sortable
      >
        <template #body="{ data }">
          {{ formatDate(data.tarih) }}
        </template>
      </Column>
      <Column
        field="cariHesapAd"
        header="Cari Hesap"
        sortable
      >
        <template #body="{ data }">
          {{ data.cariHesapAd || data.cariHesapId || '-' }}
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
        field="kalemSayisi"
        header="Kalem"
      >
        <template #body="{ data }">
          {{ data.kalemler?.length || 0 }} kalem
        </template>
      </Column>
      <Column
        field="durum"
        header="Durum"
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
        header="Açıklama"
      />
      <Column
        header="İşlem"
        style="width:200px"
      >
        <template #body="{ data }">
          <Button
            v-if="data.durum !== 'ONAYLANDI'"
            icon="pi pi-check-circle"
            class="p-button-rounded p-button-text p-button-success"
            title="Onayla"
            @click="durumGuncelle(data, 'ONAYLANDI')"
          />
          <Button
            v-if="data.durum !== 'IPTAL'"
            icon="pi pi-times-circle"
            class="p-button-rounded p-button-text p-button-danger"
            title="İptal"
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

    <Dialog
      v-model:visible="dialog"
      :header="dialogHeader"
      modal
      :style="{ width: '700px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>Cari Hesap (Müşteri/Tedarikçi) *</label>
          <Dropdown
            v-model="form.cariHesapId"
            :options="cariList"
            option-label="ad"
            option-value="id"
            placeholder="Cari Hesap Seçiniz"
            class="w-full"
            filter
            @change="cariSecildi"
          />
        </div>
        <div class="field">
          <label>Tarih *</label><DatePicker
            v-model="form.tarih"
            date-format="dd/mm/yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Açıklama</label><Textarea
            v-model="form.aciklama"
            rows="2"
            class="w-full"
          />
        </div>

        <div class="kalem-section">
          <div class="kalem-header">
            <h3>İade Kalemleri</h3>
            <Button
              label="Kalem Ekle"
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
              placeholder="Stok seç"
              class="kalem-stok"
              filter
            />
            <InputNumber
              v-model="k.miktar"
              :min="0"
              :min-fraction-digits="0"
              placeholder="Miktar"
              class="kalem-miktar"
            />
            <InputNumber
              v-model="k.birimFiyat"
              :min="0"
              :min-fraction-digits="2"
              placeholder="Br. Fiyat"
              class="kalem-fiyat"
            />
            <Dropdown
              v-model="k.kdvOrani"
              :options="[0,10,20]"
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
            <span>Toplam: {{ formatCurrency(kalemToplam) }}</span>
          </div>
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
import { iadeAPI, stokAPI, cariHesapAPI } from '../api/index.js'

const toast = useToast()
const confirm = useConfirm()
const list = ref([])
const stokList = ref([])
const cariList = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const form = ref({ cariHesapId: null, cariHesapAd: '', tarih: new Date(), tutar: 0, aciklama: '', kalemler: [] })

const dialogHeader = computed(() => duzenleme.value ? 'İade Düzenle' : 'Yeni İade')

const kalemToplam = computed(() => {
  return form.value.kalemler.reduce((t, k) => t + ((k.miktar || 0) * (k.birimFiyat || 0)), 0)
})

const formatCurrency = (v) => {
  if (v === null || v === undefined) return '0,00 ₺'
  return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(v)
}
const formatDate = (d) => {
  if (!d) return '-'
  return new Intl.DateTimeFormat('tr-TR', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(d))
}

const cariSecildi = () => {
  const secilen = cariList.value.find(c => c.id === form.value.cariHesapId)
  if (secilen) form.value.cariHesapAd = secilen.ad
}

onMounted(async () => {
  yukleniyor.value = true
  try {
    const [r, stokRes, cariRes] = await Promise.all([
      iadeAPI.getAll(),
      stokAPI.getAll(),
      cariHesapAPI.getAll()
    ])
    list.value = r.data?.content || r.data || []
    stokList.value = stokRes.data?.content || stokRes.data || []
    cariList.value = cariRes.data?.content || cariRes.data || []
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || 'Veriler yüklenemedi', life: 5000 })
  }
  yukleniyor.value = false
})

const kalemEkle = () => {
  form.value.kalemler.push({ stokId: null, miktar: 1, birimFiyat: 0, kdvOrani: 20 })
}

const dialogAc = (data) => {
  duzenleme.value = !!data
  form.value = data
    ? { ...data, tarih: data.tarih ? new Date(data.tarih) : new Date(), kalemler: data.kalemler?.map(k => ({ ...k })) || [] }
    : { cariHesapId: null, cariHesapAd: '', tarih: new Date(), tutar: 0, aciklama: '', kalemler: [] }
  dialog.value = true
}

const kaydet = async () => {
  kaydediliyor.value = true
  try {
    const payload = {
      ...form.value,
      tarih: form.value.tarih?.toISOString?.().split('T')[0] ?? form.value.tarih,
      tutar: kalemToplam.value,
      kalemler: form.value.kalemler.map(k => ({
        stokId: k.stokId,
        miktar: k.miktar,
        birimFiyat: k.birimFiyat,
        kdvOrani: k.kdvOrani
      }))
    }
    if (duzenleme.value) {
      await iadeAPI.update(form.value.id, payload)
      toast.add({ severity: 'success', summary: 'Başarılı', detail: 'İade güncellendi', life: 3000 })
    } else {
      await iadeAPI.create(payload)
      toast.add({ severity: 'success', summary: 'Başarılı', detail: 'İade oluşturuldu', life: 3000 })
    }
    dialog.value = false
    const r = await iadeAPI.getAll(); list.value = r.data?.content || r.data || []
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || 'İşlem başarısız', life: 5000 })
  }
  kaydediliyor.value = false
}

const durumGuncelle = async (data, durum) => {
  try {
    await iadeAPI.durumGuncelle(data.id, durum)
    const r = await iadeAPI.getAll(); list.value = r.data?.content || r.data || []
    toast.add({ severity: 'success', summary: 'Başarılı', detail: `İade durumu "${durum}" olarak güncellendi`, life: 3000 })
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || 'Durum güncellenirken hata oluştu', life: 5000 })
  }
}

const sil = (data) => {
  confirm.require({
    message: `Bu iade kaydını silmek istediğinize emin misiniz?`,
    header: 'Silme Onayı',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Evet, Sil',
    rejectLabel: 'İptal',
    accept: async () => {
      try {
        await iadeAPI.delete(data.id)
        list.value = list.value.filter(x => x.id !== data.id)
        toast.add({ severity: 'success', summary: 'Silindi', detail: 'İade kaydı silindi', life: 3000 })
      } catch (err) {
        toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || 'Silme başarısız', life: 5000 })
      }
    }
  })
}
</script>

<style scoped>
.iade-container { padding: 0; }
.sayfa-baslik { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.form-grid { display: flex; flex-direction: column; gap: 16px; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field label { font-size: 13px; font-weight: 600; color: var(--text-secondary); }
.w-full { width: 100%; }
.kalem-section { border-top: 1px solid var(--border); padding-top: 12px; }
.kalem-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.kalem-header h3 { margin: 0; font-size: 15px; }
.kalem-row { display: flex; gap: 8px; align-items: center; margin-bottom: 8px; }
.kalem-stok { flex: 2; min-width: 160px; }
.kalem-miktar { flex: 1; min-width: 80px; }
.kalem-fiyat { flex: 1; min-width: 100px; }
.kalem-kdv { width: 70px; }
.kalem-tutar { text-align: right; font-weight: bold; font-size: 15px; padding: 8px 0; border-top: 1px solid var(--border); margin-top: 4px; }
</style>
