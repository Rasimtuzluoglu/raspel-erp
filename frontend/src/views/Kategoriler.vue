<template>
  <div class="kategori-container">
    <h1>Gelir/Gider Kategorileri</h1>

    <Toolbar class="toolbar">
      <template #start>
        <Button
          label="Yeni Kategori"
          icon="pi pi-plus"
          class="p-button-success"
          @click="openDialog"
        />
      </template>
    </Toolbar>

    <div class="table-container">
      <DataTable
        :value="kategoriStore.kategoriler"
        striped-rows
        :rows="20"
      >
        <Column
          field="ad"
          header="Kategori Adı"
        />
        <Column
          field="tur"
          header="Tür"
          style="width:100px"
        >
          <template #body="s">
            <span :class="['badge', s.data.tur === 'GELIR' ? 'gelir' : 'gider']">
              {{ s.data.tur === 'GELIR' ? 'Gelir' : 'Gider' }}
            </span>
          </template>
        </Column>
        <Column
          header=""
          style="width:80px"
        >
          <template #body="s">
            <Button
              icon="pi pi-trash"
              class="p-button-rounded p-button-danger p-button-sm"
              @click="confirmDel(s.data.id)"
            />
          </template>
        </Column>
      </DataTable>
      <Message
        v-if="kategoriStore.kategoriler.length === 0"
        severity="info"
        text="Kategori bulunmamaktadır."
      />
    </div>

    <Dialog
      v-model:visible="showDialog"
      header="Yeni Kategori"
      :modal="true"
      style="width:400px"
    >
      <div class="form-group">
        <label>Kategori Adı *</label>
        <InputText
          v-model="form.ad"
          placeholder="Kategori adı"
          class="w-full"
        />
      </div>
      <div class="form-group">
        <label>Tür *</label>
        <Dropdown
          v-model="form.tur"
          :options="[{label:'Gelir',value:'GELIR'},{label:'Gider',value:'GIDER'}]"
          placeholder="Seçiniz"
          class="w-full"
        />
      </div>
      <template #footer>
        <Button
          label="İptal"
          icon="pi pi-times"
          class="p-button-text"
          @click="showDialog = false"
        />
        <Button
          label="Kaydet"
          icon="pi pi-check"
          :loading="saving"
          @click="save"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { useKategoriStore } from '../stores/kategoriStore.js'

const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const kategoriStore = useKategoriStore()

const showDialog = ref(false)
const saving = ref(false)
const form = ref({ ad: '', tur: '' })

onMounted(() => kategoriStore.getAllKategoriler())

const openDialog = () => { form.value = { ad: '', tur: '' }; showDialog.value = true }

const save = async () => {
  if (!form.value.ad.trim() || !form.value.tur) { toastBildirim.uyari('Tüm alanları doldurun'); return }
  saving.value = true
  try { await kategoriStore.addKategori(form.value); showDialog.value = false; toastBildirim.basarili('Kategori eklendi') }
  catch { toastBildirim.hata('İşlem başarısız') }
  finally { saving.value = false }
}

const confirmDel = (id) => {
  confirm.require({
    message: 'Bu kategoriyi silmek istediğinizden emin misiniz?', header: 'Onay',
    icon: 'pi pi-exclamation-triangle',
    accept: async () => {
      try { await kategoriStore.deleteKategori(id); toastBildirim.basarili('Kategori silindi') }
      catch { toastBildirim.hata('Silme başarısız') }
    }
  })
}
</script>

<style scoped>
.kategori-container { padding: 20px; }
h1 { color: var(--text-primary); margin-bottom: 20px; font-size: 28px; font-weight: 700; letter-spacing: -0.5px; }
.toolbar { margin-bottom: 20px; background: var(--bg-card); border: 1px solid var(--border); border-radius: 12px; padding: 14px 18px; }
.table-container { background: var(--bg-card); border: 1px solid var(--border); border-radius: 12px; padding: 14px; }
.form-group { margin-bottom: 20px; }
.form-group label { display: block; margin-bottom: 6px; font-weight: bold; color: #333; font-size: 13px; }
.badge { padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: bold; }
.badge.gelir { background: #e8f5e9; color: #2e7d32; }
.badge.gider { background: #ffebee; color: #c62828; }
.w-full { width: 100% !important; }
</style>