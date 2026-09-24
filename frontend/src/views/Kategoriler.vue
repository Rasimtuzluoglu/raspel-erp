<template>
  <div class="kategori-container">
    <h1 class="page-title">
      {{ t('kategoriler.title') }}
    </h1>

    <Toolbar class="toolbar">
      <template #start>
        <Button
          :label="t('kategoriler.yeniKategori')"
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
          :header="t('kategoriler.kategoriAdi')"
        />
        <Column
          field="tur"
          :header="t('kategoriler.tur')"
          style="width: 100px"
        >
          <template #body="s">
            <span :class="['badge', s.data.tur === 'GELIR' ? 'gelir' : 'gider']">
              {{ s.data.tur === 'GELIR' ? t('kategoriler.gelir') : t('kategoriler.gider') }}
            </span>
          </template>
        </Column>
        <Column
          header=""
          style="width: 80px"
        >
          <template #body="s">
            <Button
              icon="pi pi-trash"
              :aria-label="$t('common.delete')"
              class="p-button-rounded p-button-danger p-button-sm"
              @click="confirmDel(s.data.id)"
            />
          </template>
        </Column>
      </DataTable>
      <EmptyState
        v-if="kategoriStore.kategoriler.length === 0"
        :message="t('kategoriler.empty')"
        :sub-message="t('kategoriler.emptyHint')"
        icon="pi pi-tags"
        :action-label="t('kategoriler.yeniKategori')"
        action-icon="pi pi-plus"
        @action="openDialog"
      />
    </div>

    <Dialog
      v-model:visible="showDialog"
      :header="t('kategoriler.yeniKategori')"
      :modal="true"
      style="width: 400px"
    >
      <div class="form-group">
        <label>{{ t('kategoriler.kategoriAdiZorunlu') }}</label>
        <InputText
          v-model="form.ad"
          :placeholder="t('kategoriler.kategoriAdiPlaceholder')"
          class="w-full"
        />
      </div>
      <div class="form-group">
        <label>{{ t('kategoriler.turZorunlu') }}</label>
        <Dropdown
          v-model="form.tur"
          :options="turSecenekleri"
          option-label="label"
          option-value="value"
          :placeholder="t('common.select')"
          class="w-full"
        />
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="showDialog = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :loading="saving"
          @click="save"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { useKategoriStore } from '../stores/kategoriStore.js'
import EmptyState from '../components/EmptyState.vue'

const { t } = useI18n()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const kategoriStore = useKategoriStore()

const showDialog = ref(false)
const saving = ref(false)
const form = ref({ ad: '', tur: '' })

const turSecenekleri = computed(() => [
  { label: t('kategoriler.gelir'), value: 'GELIR' },
  { label: t('kategoriler.gider'), value: 'GIDER' }
])

onMounted(() => {
  kategoriStore.getAllKategoriler().catch(() => { /* hata global olarak bildirilir */ })
})

const openDialog = () => {
  form.value = { ad: '', tur: '' }
  showDialog.value = true
}

const save = async () => {
  if (!form.value.ad.trim() || !form.value.tur) {
    toastBildirim.uyari(t('kategoriler.tumAlanlar'))
    return
  }
  saving.value = true
  try {
    await kategoriStore.addKategori(form.value)
    showDialog.value = false
    toastBildirim.basarili(t('kategoriler.kategoriEklendi'))
  } catch {
    toastBildirim.hata(t('kategoriler.islemBasarisiz'))
  } finally {
    saving.value = false
  }
}

const confirmDel = (id) => {
  confirm.require({
    message: t('kategoriler.silOnayMesaj'),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    accept: async () => {
      try {
        await kategoriStore.deleteKategori(id)
        toastBildirim.basarili(t('kategoriler.kategoriSilindi'))
      } catch {
        toastBildirim.hata(t('kategoriler.silmeBasarisiz'))
      }
    }
  })
}
</script>

<style scoped>
.kategori-container {
  padding: 0;
  max-width: 100%;
}
h1 {
  color: var(--text-primary);
  margin-bottom: 20px;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.5px;
}
.toolbar {
  margin-bottom: 20px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px 18px;
}
.table-container {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px;
}
.form-group {
  margin-bottom: 20px;
}
.form-group label {
  display: block;
  margin-bottom: 6px;
  font-weight: bold;
  color: #333;
  font-size: 13px;
}
.badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: bold;
}
.badge.gelir {
  background: #e8f5e9;
  color: #2e7d32;
}
.badge.gider {
  background: #ffebee;
  color: #c62828;
}
.w-full {
  width: 100% !important;
}
</style>
