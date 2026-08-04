<template>
  <div class="subeler-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        Şubeler
      </h1>
      <Button
        label="Yeni Şube"
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
        header="Şube Adı"
        sortable
      />
      <Column
        field="yetkili"
        header="Yetkili"
      />
      <Column
        field="telefon"
        header="Telefon"
      />
      <Column
        field="adres"
        header="Adres"
      />
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
          <label>Şube Adı *</label><InputText
            v-model="form.ad"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Yetkili</label><InputText
            v-model="form.yetkili"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Telefon</label><InputText
            v-model="form.telefon"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Adres</label><Textarea
            v-model="form.adres"
            rows="3"
            class="w-full"
          />
        </div>
        <div
          v-if="duzenleme"
          class="field"
        >
          <label>Aktif</label><InputSwitch v-model="form.aktif" />
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
import { subeAPI } from '../api/index.js'

const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const list = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const form = ref({ ad: '', yetkili: '', telefon: '', adres: '', aktif: true })

const dialogHeader = computed(() => duzenleme.value ? 'Şube Düzenle' : 'Yeni Şube')

onMounted(async () => {
  yukleniyor.value = true
  try { const r = await subeAPI.getAll(); list.value = r.data?.content || r.data || [] } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Şubeler yüklenemedi')
  }
  yukleniyor.value = false
})

const dialogAc = (data) => {
  duzenleme.value = !!data
  form.value = data ? { ...data } : { ad: '', yetkili: '', telefon: '', adres: '', aktif: true }
  dialog.value = true
}

const kaydet = async () => {
  kaydediliyor.value = true
  try {
    if (duzenleme.value) {
      await subeAPI.update(form.value.id, form.value)
      toastBildirim.basarili('Şube güncellendi')
    } else {
      await subeAPI.create(form.value)
      toastBildirim.basarili('Şube oluşturuldu')
    }
    dialog.value = false
    const r = await subeAPI.getAll(); list.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'İşlem başarısız')
  }
  kaydediliyor.value = false
}

const sil = (data) => {
  confirm.require({
    message: `${data.ad} şubesini silmek istediğinize emin misiniz?`,
    header: 'Silme Onayı',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Evet, Sil',
    rejectLabel: 'İptal',
    accept: async () => {
      try {
        await subeAPI.delete(data.id)
        list.value = list.value.filter(x => x.id !== data.id)
        toast.add({ severity: 'success', summary: 'Silindi', detail: 'Şube silindi', life: 3000 })
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || 'Silme başarısız')
      }
    }
  })
}
</script>

<style scoped>
.subeler-container { padding: 0; }
.sayfa-baslik { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.form-grid { display: flex; flex-direction: column; gap: 16px; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field label { font-size: 13px; font-weight: 600; color: var(--text-secondary); }
.w-full { width: 100%; }
</style>