<template>
  <div class="maas-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        Maaş Bordro
      </h1>
      <Button
        label="Yeni Bordro"
        icon="pi pi-plus"
        @click="dialogAc()"
      />
    </div>

    <DataTable
      state-storage="session"
      state-key="maasbordro-table-state"
      :value="list"
      striped-rows
      :loading="yukleniyor"
    >
      <Column
        field="personelAd"
        header="Personel"
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
        field="brutMaas"
        header="Brüt"
      >
        <template #body="{ data }">
          {{ formatCurrency(data.brutMaas) }}
        </template>
      </Column>
      <Column
        field="kesintiler"
        header="Kesintiler"
      >
        <template #body="{ data }">
          {{ formatCurrency(data.kesintiler) }}
        </template>
      </Column>
      <Column
        field="netMaas"
        header="Net"
      >
        <template #body="{ data }">
          {{ formatCurrency(data.netMaas) }}
        </template>
      </Column>
      <Column
        field="odemeTarihi"
        header="Ödeme Tarihi"
      >
        <template #body="{ data }">
          {{ formatDate(data.odemeTarihi) }}
        </template>
      </Column>
      <Column
        header="İşlem"
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

    <Dialog
      v-model:visible="dialog"
      :header="dialogHeader"
      modal
      :style="{ width: '550px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>Personel *</label>
          <Dropdown
            v-model="form.personelId"
            :options="personelListesi"
            option-label="displayName"
            option-value="id"
            placeholder="Personel Seç"
            class="w-full"
            filter
          />
        </div>
        <div class="field-row">
          <div class="field">
            <label>Yıl</label><InputNumber
              v-model="form.yil"
              class="w-full"
              :min="2000"
              :max="2100"
            />
          </div>
          <div class="field">
            <label>Ay</label><InputNumber
              v-model="form.ay"
              class="w-full"
              :min="1"
              :max="12"
            />
          </div>
        </div>
        <div class="field">
          <label>Brüt Maaş *</label><InputNumber
            v-model="form.brutMaas"
            mode="currency"
            currency="TRY"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Kesintiler</label><InputNumber
            v-model="form.kesintiler"
            mode="currency"
            currency="TRY"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Net Maaş (Hesaplanan)</label>
          <span style="font-weight: 700; font-size: 18px; color: #4ade80">{{
            formatCurrency((form.brutMaas || 0) - (form.kesintiler || 0))
          }}</span>
        </div>
        <div class="field">
          <label>Ödeme Tarihi</label><DatePicker
            v-model="form.odemeTarihi"
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
import { maasBordroAPI, personelAPI } from '../api/index.js'

const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const list = ref([])
const personelListesi = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const form = ref({
  personelId: null,
  yil: new Date().getFullYear(),
  ay: new Date().getMonth() + 1,
  brutMaas: 0,
  kesintiler: 0,
  odemeTarihi: new Date()
})

const dialogHeader = computed(() => (duzenleme.value ? 'Bordro Düzenle' : 'Yeni Bordro'))

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
    const [mR, pR] = await Promise.all([maasBordroAPI.getAll(), personelAPI.getAll()])
    list.value = mR.data?.content || mR.data || []
    personelListesi.value = pR.data.map((p) => ({
      ...p,
      displayName: p.ad && p.soyad ? `${p.ad} ${p.soyad}` : p.ad || p.id
    }))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Veriler yüklenemedi')
  }
  yukleniyor.value = false
})

const dialogAc = (data) => {
  duzenleme.value = !!data
  form.value = data
    ? { ...data, odemeTarihi: data.odemeTarihi ? new Date(data.odemeTarihi) : new Date() }
    : {
        personelId: null,
        yil: new Date().getFullYear(),
        ay: new Date().getMonth() + 1,
        brutMaas: 0,
        kesintiler: 0,
        odemeTarihi: new Date()
      }
  dialog.value = true
}

const kaydet = async () => {
  kaydediliyor.value = true
  try {
    const netMaas = (form.value.brutMaas || 0) - (form.value.kesintiler || 0)
    const payload = {
      ...form.value,
      netMaas,
      odemeTarihi: form.value.odemeTarihi?.toISOString?.().split('T')[0] ?? form.value.odemeTarihi
    }
    if (duzenleme.value) {
      await maasBordroAPI.update(form.value.id, payload)
      toastBildirim.basarili('Bordro güncellendi')
    } else {
      await maasBordroAPI.create(payload)
      toastBildirim.basarili('Bordro oluşturuldu')
    }
    dialog.value = false
    const r = await maasBordroAPI.getAll()
    list.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'İşlem başarısız')
  }
  kaydediliyor.value = false
}

const sil = (data) => {
  const personelAd = data.personelAd || data.id
  confirm.require({
    message: `"${personelAd}" bordrosunu silmek istediğinize emin misiniz?`,
    header: 'Silme Onayı',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Evet, Sil',
    rejectLabel: 'İptal',
    accept: async () => {
      try {
        await maasBordroAPI.delete(data.id)
        list.value = list.value.filter((x) => x.id !== data.id)
        toast.add({ severity: 'success', summary: 'Silindi', detail: 'Bordro silindi', life: 3000 })
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || 'Silme başarısız')
      }
    }
  })
}
</script>

<style scoped>
.maas-container {
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
.field-row {
  display: flex;
  gap: 12px;
}
.field-row .field {
  flex: 1;
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
