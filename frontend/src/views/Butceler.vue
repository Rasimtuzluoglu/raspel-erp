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
      state-storage="session"
      state-key="butceler-table-state"
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

    <EmptyState
      v-if="!yukleniyor && list.length === 0"
      message="Henüz bütçe bulunamadı"
      sub-message="İlk bütçenizi eklemek için Yeni Bütçe butonuna tıklayın"
      icon="pi pi-chart-bar"
      action-label="Yeni Bütçe"
      action-icon="pi pi-plus"
      @action="dialogAc()"
    />

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
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { butceAPI } from '../api/index.js'
import EmptyState from '../components/EmptyState.vue'
import { formatCurrency } from '../utils/format.js'

const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const list = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const form = ref({
  ad: '',
  yil: new Date().getFullYear(),
  ay: new Date().getMonth() + 1,
  tur: 'GELIR',
  tutar: 0,
  kategori: '',
  aciklama: ''
})
const turSecenekleri = ['GELIR', 'GIDER']

const dialogHeader = computed(() => (duzenleme.value ? 'Bütçe Düzenle' : 'Yeni Bütçe'))


onMounted(async () => {
  yukleniyor.value = true
  try {
    const r = await butceAPI.getAll()
    list.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Bütçeler yüklenemedi')
  }
  yukleniyor.value = false
})

const dialogAc = (data) => {
  duzenleme.value = !!data
  form.value = data
    ? { ...data }
    : {
        ad: '',
        yil: new Date().getFullYear(),
        ay: new Date().getMonth() + 1,
        tur: 'GELIR',
        tutar: 0,
        kategori: '',
        aciklama: ''
      }
  dialog.value = true
}

const kaydet = async () => {
  kaydediliyor.value = true
  try {
    if (duzenleme.value) {
      await butceAPI.update(form.value.id, form.value)
      toastBildirim.basarili('Bütçe güncellendi')
    } else {
      await butceAPI.create(form.value)
      toastBildirim.basarili('Bütçe oluşturuldu')
    }
    dialog.value = false
    const r = await butceAPI.getAll()
    list.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'İşlem başarısız')
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
        list.value = list.value.filter((x) => x.id !== data.id)
        toast.add({ severity: 'success', summary: 'Silindi', detail: 'Bütçe silindi', life: 3000 })
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || 'Silme başarısız')
      }
    }
  })
}
</script>

<style scoped>
.butce-container {
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
