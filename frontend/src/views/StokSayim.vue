<template>
  <div class="stoksayim-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        Stok Sayımı
      </h1>
      <Button
        label="Yeni Sayım"
        icon="pi pi-plus"
        @click="dialogAc()"
      />
    </div>

    <DataTable
      state-storage="session"
      state-key="stoksayim-table-state"
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
        field="stokAd"
        header="Ürün"
        sortable
      />
      <Column
        field="beklenenMiktar"
        header="Beklenen"
      >
        <template #body="{ data }">
          {{ formatNumber(data.beklenenMiktar) }}
        </template>
      </Column>
      <Column
        field="sayilanMiktar"
        header="Sayılan"
      >
        <template #body="{ data }">
          {{ formatNumber(data.sayilanMiktar) }}
        </template>
      </Column>
      <Column
        field="fark"
        header="Fark"
      >
        <template #body="{ data }">
          <span :class="(data.fark ?? data.sayilanMiktar - data.beklenenMiktar) >= 0 ? 'positive' : 'negative'">
            {{ formatNumber(data.fark ?? data.sayilanMiktar - data.beklenenMiktar) }}
          </span>
        </template>
      </Column>
      <Column
        field="durum"
        header="Durum"
      >
        <template #body="{ data }">
          <Tag
            :value="data.durum"
            :severity="data.durum === 'TAMAMLANDI' ? 'success' : data.durum === 'IPTAL' ? 'danger' : 'warn'"
          />
        </template>
      </Column>
      <Column
        header="İşlem"
        style="width: 200px"
      >
        <template #body="{ data }">
          <Button
            v-if="data.durum !== 'TAMAMLANDI'"
            icon="pi pi-check-circle"
            class="p-button-rounded p-button-text p-button-success"
            title="Tamamla"
            @click="durumGuncelle(data, 'TAMAMLANDI')"
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
      :style="{ width: '500px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>Tarih *</label><DatePicker
            v-model="form.tarih"
            date-format="dd/mm/yy"
            class="w-full"
          />
        </div>
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
            @change="onStokSec"
          />
        </div>
        <div class="field">
          <label>Beklenen Miktar</label><InputNumber
            v-model="form.beklenenMiktar"
            class="w-full"
            :min="0"
            disabled
          />
        </div>
        <div class="field">
          <label>Sayılan Miktar *</label><InputNumber
            v-model="form.sayilanMiktar"
            class="w-full"
            :min="0"
          />
        </div>
        <div class="field">
          <label>Fark</label>
          <span
            :class="{
              positive: form.sayilanMiktar - form.beklenenMiktar >= 0,
              negative: form.sayilanMiktar - form.beklenenMiktar < 0
            }"
            style="font-weight: 700; font-size: 18px"
          >
            {{ formatNumber(form.sayilanMiktar - form.beklenenMiktar) }}
          </span>
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
import { stokSayimAPI, stokAPI } from '../api/index.js'

const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const list = ref([])
const stokListesi = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const form = ref({ tarih: new Date(), stokId: null, beklenenMiktar: 0, sayilanMiktar: 0 })

const dialogHeader = computed(() => (duzenleme.value ? 'Sayım Düzenle' : 'Yeni Sayım'))

const formatDate = (d) => {
  if (!d) return '-'
  return new Intl.DateTimeFormat('tr-TR', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(d))
}
const formatNumber = (v) => {
  if (v === null || v === undefined) return '0'
  return new Intl.NumberFormat('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(v)
}

onMounted(async () => {
  yukleniyor.value = true
  try {
    const [sR, stR] = await Promise.all([stokSayimAPI.getAll(), stokAPI.getAll()])
    list.value = sR.data?.content || sR.data || []
    stokListesi.value = stR.data.content || stR.data
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Veriler yüklenemedi')
  }
  yukleniyor.value = false
})

const dialogAc = (data) => {
  duzenleme.value = !!data
  form.value = data
    ? { ...data, tarih: data.tarih ? new Date(data.tarih) : new Date() }
    : { tarih: new Date(), stokId: null, beklenenMiktar: 0, sayilanMiktar: 0 }
  dialog.value = true
}

const onStokSec = () => {
  const stok = stokListesi.value.find((s) => s.id === form.value.stokId)
  form.value.beklenenMiktar = stok?.miktar ?? 0
}

const kaydet = async () => {
  kaydediliyor.value = true
  try {
    const fark = form.value.sayilanMiktar - form.value.beklenenMiktar
    const payload = {
      ...form.value,
      tarih: form.value.tarih?.toISOString?.().split('T')[0] ?? form.value.tarih,
      fark
    }
    if (duzenleme.value) {
      await stokSayimAPI.update(form.value.id, payload)
      toastBildirim.basarili('Sayım güncellendi')
    } else {
      await stokSayimAPI.create(payload)
      toastBildirim.basarili('Sayım oluşturuldu')
    }
    dialog.value = false
    const r = await stokSayimAPI.getAll()
    list.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'İşlem başarısız')
  }
  kaydediliyor.value = false
}

const durumGuncelle = async (data, durum) => {
  try {
    await stokSayimAPI.durumGuncelle(data.id, durum)
    const r = await stokSayimAPI.getAll()
    list.value = r.data?.content || r.data || []
    toastBildirim.basarili(`Sayım durumu "${durum}" olarak güncellendi`)
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Durum güncellenirken hata oluştu')
  }
}

const sil = (data) => {
  confirm.require({
    message: `Bu sayım kaydını silmek istediğinize emin misiniz?`,
    header: 'Silme Onayı',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Evet, Sil',
    rejectLabel: 'İptal',
    accept: async () => {
      try {
        await stokSayimAPI.delete(data.id)
        list.value = list.value.filter((x) => x.id !== data.id)
        toast.add({ severity: 'success', summary: 'Silindi', detail: 'Sayım kaydı silindi', life: 3000 })
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || 'Silme başarısız')
      }
    }
  })
}
</script>

<style scoped>
.stoksayim-container {
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
.positive {
  color: #4ade80;
}
.negative {
  color: #f87171;
}
</style>
