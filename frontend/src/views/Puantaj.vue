<template>
  <div class="puantaj-container">
    <h1>Personel Puantaj Yönetimi</h1>
    <Toolbar class="toolbar">
      <template #start>
        <Button
          label="Yeni Kayıt"
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
          placeholder="Personel seçin"
          class="personel-dropdown"
          @change="loadData"
        />
        <DatePicker
          v-model="filtreBaslangic"
          placeholder="Başlangıç"
          date-format="dd.mm.yy"
          class="filter-date"
          @update:model-value="loadData"
        />
        <DatePicker
          v-model="filtreBitis"
          placeholder="Bitiş"
          date-format="dd.mm.yy"
          class="filter-date"
          @update:model-value="loadData"
        />
      </template>
    </Toolbar>
    <DataTable
      state-storage="session"
      state-key="puantaj-table-state"
      :value="list"
      striped-rows
      :loading="yukleniyor"
    >
      <Column
        field="tarih"
        header="Tarih"
      >
        <template #body="{ data }">
          {{ formatDate(data.tarih) }}
        </template>
      </Column>
      <Column
        field="personelAdi"
        header="Personel"
      />
      <Column
        field="durum"
        header="Durum"
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
        header="Açıklama"
      />
      <Column
        header="İşlem"
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
      v-if="list.length === 0"
      severity="info"
      text="Kayıt bulunamadı."
    />
    <Dialog
      v-model:visible="dialog"
      :header="duzenleme ? 'Puantaj Düzenle' : 'Yeni Puantaj'"
      modal
      :style="{ width: '500px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>Personel *</label><Dropdown
            v-model="form.personelId"
            :options="personelList"
            option-label="ad"
            option-value="id"
            placeholder="Seçiniz"
            class="w-full"
            filter
          />
        </div>
        <div class="field">
          <label>Tarih *</label><DatePicker
            v-model="form.tarih"
            date-format="dd.mm.yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Durum</label><Dropdown
            v-model="form.durum"
            :options="durumList"
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
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { puantajAPI, personelAPI } from '../api/index.js'

const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
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
    toastBildirim.hata('Personel listesi yüklenemedi')
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
    toastBildirim.hata('Puantaj verileri yüklenemedi')
  }
  yukleniyor.value = false
}

const formatDate = (d) =>
  d ? new Intl.DateTimeFormat('tr-TR', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(d)) : '-'

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
    toastBildirim.uyari('Personel seçiniz')
    return
  }
  kaydediliyor.value = true
  try {
    const payload = { ...form.value, tarih: form.value.tarih?.toISOString().split('T')[0] }
    if (duzenleme.value) {
      await puantajAPI.update(form.value.id, payload)
      toastBildirim.basarili('Puantaj güncellendi')
    } else {
      await puantajAPI.create(payload)
      toastBildirim.basarili('Puantaj eklendi')
    }
    dialog.value = false
    await loadData()
  } catch {
    toastBildirim.hata('İşlem başarısız')
  }
  kaydediliyor.value = false
}

const sil = (data) => {
  confirm.require({
    message: 'Bu kaydı silmek istediğinize emin misiniz?',
    header: 'Onay',
    icon: 'pi pi-exclamation-triangle',
    accept: async () => {
      try {
        await puantajAPI.delete(data.id)
        await loadData()
        toast.add({ severity: 'success', summary: 'Silindi', life: 3000 })
      } catch {
        toastBildirim.hata('Silme başarısız')
      }
    }
  })
}
</script>

<style scoped>
.puantaj-container {
  padding: 20px;
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
