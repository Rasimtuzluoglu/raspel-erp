<template>
  <div class="irsaliye-sayfasi">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        İrsaliyeler
      </h1>
      <Button
        label="Yeni İrsaliye"
        icon="pi pi-plus"
        @click="dialogAc()"
      />
    </div>

    <DataTable
      state-storage="session"
      state-key="irsaliyeler-table-state"
      :value="list"
      striped-rows
      :loading="yukleniyor"
    >
      <Column
        field="irsaliyeNo"
        header="İrsaliye No"
        sortable
      />
      <Column
        field="tarih"
        header="Tarih"
      />
      <Column
        field="cariHesapAdi"
        header="Cari Hesap"
      />
      <Column
        field="tur"
        header="Tür"
      />
      <Column
        field="durum"
        header="Durum"
      >
        <template #body="{ data }">
          <Tag
            :value="data.durum"
            :severity="data.durum === 'KESILDI' ? 'success' : data.durum === 'IPTAL' ? 'danger' : 'warn'"
          />
        </template>
      </Column>
      <Column
        header="İşlem"
        style="width: 140px"
      >
        <template #body="{ data }">
          <Button
            v-if="data.durum === 'TASLAK'"
            icon="pi pi-check"
            class="p-button-rounded p-button-text p-button-success"
            title="Kes"
            @click="durumGuncelle(data, 'KESILDI')"
          />
          <Button
            v-if="data.durum !== 'IPTAL'"
            icon="pi pi-times"
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

    <EmptyState
      v-if="!yukleniyor && list.length === 0"
      message="Henüz irsaliye bulunamadı"
      sub-message="İlk irsaliyenizi eklemek için Yeni İrsaliye butonuna tıklayın"
      icon="pi pi-truck"
      action-label="Yeni İrsaliye"
      action-icon="pi pi-plus"
      @action="dialogAc()"
    />

    <Dialog
      v-model:visible="dialog"
      header="Yeni İrsaliye"
      modal
      :style="{ width: '500px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>İrsaliye No *</label><InputText
            v-model="form.irsaliyeNo"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Tarih</label><DatePicker
            v-model="form.tarih"
            date-format="dd/mm/yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Cari Hesap *</label>
          <Dropdown
            v-model="form.cariHesapId"
            :options="cariler"
            option-label="ad"
            option-value="id"
            placeholder="Seçin"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Tür</label>
          <Dropdown
            v-model="form.tur"
            :options="['SATIS', 'ALIS']"
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
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { irsaliyeAPI, cariHesapAPI } from '../api/index.js'
import EmptyState from '../components/EmptyState.vue'
const toastBildirim = useToastBildirim()
const confirm = useConfirm()

const list = ref([])
const cariler = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const dialog = ref(false)
const form = ref({ irsaliyeNo: '', tarih: new Date(), cariHesapId: null, tur: 'SATIS', aciklama: '' })

onMounted(async () => {
  yukleniyor.value = true
  try {
    const [r, c] = await Promise.all([irsaliyeAPI.getAll(), cariHesapAPI.getAll()])
    list.value = r.data?.content || r.data || []
    cariler.value = c.data?.content || c.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || 'İrsaliyeler yüklenirken hata oluştu')
  }
  yukleniyor.value = false
})

const dialogAc = () => {
  form.value = { irsaliyeNo: 'IRS-' + Date.now(), tarih: new Date(), cariHesapId: null, tur: 'SATIS', aciklama: '' }
  dialog.value = true
}
const kaydet = async () => {
  kaydediliyor.value = true
  try {
    await irsaliyeAPI.create({ ...form.value, tarih: form.value.tarih?.toISOString().split('T')[0] })
    dialog.value = false
    const r = await irsaliyeAPI.getAll()
    list.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || 'İrsaliye kaydedilirken hata oluştu')
  }
  kaydediliyor.value = false
}
const durumGuncelle = async (data, durum) => {
  try {
    await irsaliyeAPI.durumGuncelle(data.id, durum)
    const r = await irsaliyeAPI.getAll()
    list.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || 'Durum güncellenirken hata oluştu')
  }
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
        await irsaliyeAPI.delete(data.id)
        list.value = list.value.filter((x) => x.id !== data.id)
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || err?.message || 'İrsaliye silinirken hata oluştu')
      }
    },
    reject: () => {}
  })
}
</script>

<style scoped>
.irsaliye-sayfasi {
  padding: 0;
}
.sayfa-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.sayfa-baslik h1 {
  margin: 0;
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
