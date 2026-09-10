<template>
  <div class="hareketler-container">
    <h1>{{ t('hareketler.title') }}</h1>

    <Toolbar class="toolbar">
      <template #start>
        <Button
          :label="t('hareketler.yeniHareket')"
          icon="pi pi-plus"
          class="p-button-success"
          @click="openDialog"
        />
      </template>
      <template #end>
        <TarihHizliSecim v-model="tarihAraligi" />
        <Button
          :label="t('hareketler.excel')"
          icon="pi pi-file-excel"
          class="p-button-sm p-button-outlined"
          style="margin-right: 4px"
          @click="excelIndir"
        />
        <Button
          :label="t('hareketler.csv')"
          icon="pi pi-download"
          class="p-button-sm p-button-outlined"
          style="margin-right: 8px"
          @click="csvExport"
        />
        <DatePicker
          v-model="filtreBaslangic"
          :placeholder="t('hareketler.baslangic')"
          date-format="dd.mm.yy"
          class="filter-date"
          @update:model-value="filtrele"
        />
        <DatePicker
          v-model="filtreBitis"
          :placeholder="t('hareketler.bitis')"
          date-format="dd.mm.yy"
          class="filter-date"
          @update:model-value="filtrele"
        />
        <Button
          v-if="filtreBaslangic || filtreBitis"
          icon="pi pi-times"
          class="p-button-rounded p-button-text p-button-sm"
          :title="t('hareketler.filtreyiTemizle')"
          @click="filtreTemizle"
        />
      </template>
    </Toolbar>

    <div
      v-if="loading"
      class="loading"
    >
      <p><i class="pi pi-spin pi-spinner" /> {{ t('common.loading') }}</p>
    </div>

    <div
      v-if="!loading"
      class="table-container"
    >
      <div
        v-if="selectedItems && selectedItems.length > 0"
        class="batch-action-bar"
      >
        <div class="batch-info">
          <i class="pi pi-check-square" />
          <span><strong>{{ selectedItems ? selectedItems.length : 0 }}</strong> {{ t('hareketler.kayitSecildi') }}</span>
        </div>
        <div class="batch-buttons">
          <Button
            :label="t('hareketler.secilenleriSil')"
            icon="pi pi-trash"
            class="p-button-danger p-button-sm"
            :loading="topluSiliniyor"
            @click="topluSil()"
          />
          <Button
            :label="t('common.clearSelection')"
            icon="pi pi-times"
            class="p-button-text p-button-sm"
            @click="selectedItems = []"
          />
        </div>
      </div>
      <DataTable
        v-model:selection="selectedItems"
        selection-mode="multiple"
        :value="tümHareketler"
        responsive-layout="scroll"
        striped-rows
        :rows="10"
        :paginator="true"
        paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport RowsPerPageDropdown"
        :rows-per-page-options="[10, 20, 50]"
        current-page-report-template="{first} - {last} ({totalRecords} kayıt)"
      >
        <Column
          field="cariHesapAd"
          :header="t('hareketler.cariHesap')"
          style="width: 200px"
        />
        <Column
          field="tur"
          :header="t('hareketler.tur')"
          style="width: 100px"
        >
          <template #body="slotProps">
            <span
              :class="[
                'badge',
                String(
                  typeof slotProps.data.tur === 'object' ? slotProps.data.tur?.value : slotProps.data.tur
                ).toUpperCase() === 'TAHSILAT'
                  ? 'tahsilat'
                  : 'odeme'
              ]"
            >
              {{
                String(
                  typeof slotProps.data.tur === 'object' ? slotProps.data.tur?.value : slotProps.data.tur
                ).toUpperCase() === 'TAHSILAT'
                  ? t('hareketler.tahsilat')
                  : t('hareketler.odeme')
              }}
            </span>
          </template>
        </Column>
        <Column
          field="odemeSekli"
          :header="t('hareketler.odemeSekli')"
          style="width: 120px"
        >
          <template #body="slotProps">
            <span v-if="slotProps.data.odemeSekli">{{ odemeSekliLabel(slotProps.data.odemeSekli) }}</span>
            <span
              v-else
              class="muted"
            >-</span>
          </template>
        </Column>
        <Column
          field="tutar"
          :header="t('common.amount')"
          style="width: 120px"
        >
          <template #body="slotProps">
            <span :class="slotProps.data.tur === 'TAHSILAT' ? 'positive' : 'negative'">
              {{ formatCurrency(slotProps.data.tutar) }}
            </span>
          </template>
        </Column>
        <Column
          field="hareketTarihi"
          :header="t('common.date')"
          style="width: 120px"
        >
          <template #body="slotProps">
            {{ formatDate(slotProps.data.hareketTarihi) }}
          </template>
        </Column>
        <Column
          field="aciklama"
          :header="t('common.description')"
        />
        <Column
          :header="t('common.actions')"
          style="width: 140px"
        >
          <template #body="slotProps">
            <Button
              icon="pi pi-pencil"
              class="p-button-rounded p-button-info p-button-sm"
              :title="t('common.edit')"
              style="margin-right: 6px"
              @click="openEditDialog(slotProps.data)"
            />
            <Button
              icon="pi pi-trash"
              class="p-button-rounded p-button-danger p-button-sm"
              :title="t('common.delete')"
              @click="confirmDelete(slotProps.data.id)"
            />
          </template>
        </Column>
      </DataTable>

      <EmptyState
        v-if="tümHareketler.length === 0"
        :message="t('hareketler.empty')"
        :sub-message="t('hareketler.emptyHint')"
        icon="pi pi-exchange"
        :action-label="t('hareketler.yeniHareket')"
        action-icon="pi pi-plus"
        @action="openDialog"
      />
    </div>

    <!-- Hareket Ekleme/Düzenleme Dialog -->
    <Dialog
      v-model:visible="showDialog"
      :header="editingId ? t('hareketler.duzenle') : t('hareketler.yeniEkle')"
      :modal="true"
      style="width: 500px"
    >
      <div class="form-group">
        <label for="cariHesapId">{{ t('hareketler.cariHesapZorunlu') }}</label>
        <Dropdown
          id="cariHesapId"
          v-model="form.cariHesapId"
          :options="cariHesapSeçenekleri"
          option-label="ad"
          option-value="id"
          :placeholder="t('hareketler.cariHesapSeciniz')"
          class="w-full"
        />
      </div>

      <div class="form-group">
        <label for="tur">{{ t('hareketler.hareketTuruZorunlu') }}</label>
        <Dropdown
          id="tur"
          v-model="form.tur"
          :options="hareketTurleri"
          option-label="label"
          option-value="value"
          :placeholder="t('hareketler.hareketTuruSeciniz')"
          class="w-full"
        />
      </div>

      <div class="form-group">
        <label for="odemeSekli">{{ t('hareketler.odemeSekli') }}</label>
        <Dropdown
          id="odemeSekli"
          v-model="form.odemeSekli"
          :options="odemeSekliSecenekleri"
          option-label="label"
          option-value="value"
          :placeholder="t('hareketler.odemeSekliSeciniz')"
          class="w-full"
        />
      </div>

      <div class="form-group">
        <label for="tutar">{{ t('hareketler.tutarZorunlu') }}</label>
        <InputNumber
          id="tutar"
          v-model="form.tutar"
          :use-grouping="false"
          :min-fraction-digits="2"
          :max-fraction-digits="2"
          placeholder="0,00"
          class="w-full"
        />
      </div>

      <div class="form-group">
        <label for="hareketTarihi">{{ t('hareketler.hareketTarihiZorunlu') }}</label>
        <DatePicker
          id="hareketTarihi"
          v-model="form.hareketTarihi"
          date-format="dd.mm.yy"
          class="w-full"
        />
      </div>

      <div
        v-if="form.cariHesapId && (faturaSecenekleri.length > 0 || faturalarYukleniyor)"
        class="form-group"
      >
        <label for="faturaId">{{ t('hareketler.bagliFatura') }}</label>
        <Dropdown
          id="faturaId"
          v-model="form.faturaId"
          :options="faturaSecenekleri"
          option-label="faturaNumarasi"
          option-value="id"
          :placeholder="t('hareketler.faturaSecinPlaceholder')"
          class="w-full"
          :show-clear="true"
          :loading="faturalarYukleniyor"
        />
        <small class="fatura-ipucu">{{ t('hareketler.faturaIpucu') }}</small>
      </div>

      <div class="form-group">
        <label for="aciklama">{{ t('common.description') }}</label>
        <Textarea
          id="aciklama"
          v-model="form.aciklama"
          :placeholder="t('hareketler.aciklamaPlaceholder')"
          rows="3"
          class="w-full"
        />
      </div>

      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="closeDialog"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :loading="saving"
          @click="saveHareket"
        />
      </template>
    </Dialog>

    <Message
      v-if="error"
      severity="error"
      :text="error"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { useCariHesapStore } from '../stores/cariHesapStore.js'
import { useHareketStore } from '../stores/hareketStore.js'
import { hareketAPI, faturaAPI, excelAPI } from '../api/index.js'
import EmptyState from '../components/EmptyState.vue'
import TarihHizliSecim from '../components/TarihHizliSecim.vue'
import { formatCurrency } from '../utils/format.js'
import { useI18n } from 'vue-i18n'

const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const cariHesapStore = useCariHesapStore()
const hareketStore = useHareketStore()
const { t } = useI18n()

const showDialog = ref(false)
const loading = ref(false)
const saving = ref(false)
const error = ref(null)
const editingId = ref(null)
const tümHareketler = ref([])
const filtreBaslangic = ref(null)
const filtreBitis = ref(null)
const tarihAraligi = ref(null)
const selectedItems = ref([])
const topluSiliniyor = ref(false)
let aramaZaman = null
onUnmounted(() => {
  if (aramaZaman) clearTimeout(aramaZaman)
})

watch(tarihAraligi, (v) => {
  if (v && v.length === 2 && v[0] && v[1]) {
    filtreBaslangic.value = v[0]
    filtreBitis.value = v[1]
    filtrele()
  } else if (!v || v.length === 0) {
    filtreTemizle()
  }
})

const form = ref({
  cariHesapId: null,
  tur: '',
  odemeSekli: null,
  tutar: null,
  hareketTarihi: new Date(),
  aciklama: '',
  faturaId: null
})

const faturaSecenekleri = ref([])
const faturalarYukleniyor = ref(false)

const seciliCariFaturalariYukle = async (cariHesapId) => {
  faturaSecenekleri.value = []
  if (!cariHesapId) return
  faturalarYukleniyor.value = true
  try {
    const r = await faturaAPI.getAll({ cariHesapId, page: 0, size: 100 })
    const faturalar = Array.isArray(r.data?.content) ? r.data.content : Array.isArray(r.data) ? r.data : []
    faturaSecenekleri.value = faturalar.filter(
      (f) => f.durum === 'KESILDI' && (f.kalanTutar || 0) > 0
    )
  } catch {
    faturaSecenekleri.value = []
  } finally {
    faturalarYukleniyor.value = false
  }
}

watch(() => form.value.cariHesapId, (yeni) => {
  form.value.faturaId = null
  seciliCariFaturalariYukle(yeni)
})

const hareketTurleri = computed(() => [
  { label: t('hareketler.tahsilat'), value: 'TAHSILAT' },
  { label: t('hareketler.odeme'), value: 'ODEME' }
])

const odemeSekliSecenekleri = computed(() => [
  { label: t('hareketler.nakit'), value: 'NAKIT' },
  { label: t('hareketler.krediKarti'), value: 'KREDI_KARTI' },
  { label: t('hareketler.havaleEft'), value: 'HAVALE_EFT' },
  { label: t('hareketler.cek'), value: 'CEK' },
  { label: t('hareketler.senet'), value: 'SENET' },
  { label: t('hareketler.banka'), value: 'BANKA' }
])

const odemeSekliLabel = (val) => {
  if (!val) return '-'
  const code = typeof val === 'object' ? val.value || val.label : val
  const item = odemeSekliSecenekleri.value.find((s) => s.value === code || s.label === code)
  return item ? item.label : String(code)
}

const cariHesapSeçenekleri = computed(() => {
  return cariHesapStore?.cariHesaplar || []
})

onMounted(async () => {
  await loadData()
})

const loadData = async () => {
  loading.value = true
  try {
    await cariHesapStore.getAllCariHesaplar()
    const hareketler = await hareketStore.getAllHareketler()
    tümHareketler.value = hareketler
  } catch (err) {
    error.value = t('hareketler.hataYukleme')
    toastBildirim.hata(t('hareketler.hataYukleme'))
  } finally {
    loading.value = false
  }
}

const openDialog = () => {
  editingId.value = null
  form.value = {
    cariHesapId: null,
    tur: '',
    odemeSekli: null,
    tutar: null,
    hareketTarihi: new Date(),
    aciklama: '',
    faturaId: null
  }
  faturaSecenekleri.value = []
  showDialog.value = true
}

const openEditDialog = (hareket) => {
  editingId.value = hareket.id
  form.value = {
    cariHesapId: hareket.cariHesapId,
    tur: hareket.tur,
    odemeSekli: hareket.odemeSekli || null,
    tutar: hareket.tutar,
    hareketTarihi: new Date(hareket.hareketTarihi),
    aciklama: hareket.aciklama || '',
    faturaId: hareket.faturaId || null
  }
  seciliCariFaturalariYukle(hareket.cariHesapId)
  showDialog.value = true
}

const closeDialog = () => {
  showDialog.value = false
}

const saveHareket = async () => {
  if (!form.value.cariHesapId) {
    toastBildirim.uyari(t('hareketler.cariHesapSecinUyari'))
    return
  }

  if (!form.value.tur) {
    toastBildirim.uyari(t('hareketler.hareketTuruSecinUyari'))
    return
  }

  if (!form.value.tutar || form.value.tutar <= 0) {
    toastBildirim.uyari(t('hareketler.gecerliTutarUyari'))
    return
  }

  saving.value = true
  try {
    const hareketDTO = {
      cariHesapId: form.value.cariHesapId,
      tur: form.value.tur,
      odemeSekli: form.value.odemeSekli || null,
      tutar: form.value.tutar,
      hareketTarihi: form.value.hareketTarihi ? form.value.hareketTarihi.toISOString().split('T')[0] : null,
      aciklama: form.value.aciklama,
      faturaId: form.value.faturaId || null
    }

    if (editingId.value) {
      await hareketStore.updateHareket(editingId.value, hareketDTO)
      toastBildirim.basarili(t('hareketler.guncellendi'))
    } else {
      await hareketStore.addHareket(hareketDTO)
      toastBildirim.basarili(t('hareketler.eklendi'))
    }

    tümHareketler.value = await hareketStore.getAllHareketler()
    closeDialog()
  } catch (err) {
    toastBildirim.hata(t('hareketler.islemBasarisiz'))
  } finally {
    saving.value = false
  }
}

const confirmDelete = (id) => {
  confirm.require({
    message: t('hareketler.silOnayMesaj'),
    header: t('kasa.onay'),
    icon: 'pi pi-exclamation-triangle',
    accept: () => deleteHareket(id),
    reject: () => {}
  })
}

const deleteHareket = async (id) => {
  try {
    await hareketStore.deleteHareket(id)
    tümHareketler.value = await hareketStore.getAllHareketler()
    toastBildirim.basarili(t('hareketler.silindi'))
  } catch (error) {
    toastBildirim.hata(t('hareketler.silmeHata'))
  }
}

const filtrele = async () => {
  try {
    const params = {}
    if (filtreBaslangic.value) params.baslangic = filtreBaslangic.value.toISOString().split('T')[0]
    if (filtreBitis.value) params.bitis = filtreBitis.value.toISOString().split('T')[0]
    const response = await hareketAPI.filtrele(params)
    tümHareketler.value = response.data?.content || response.data || []
  } catch (err) {
    toastBildirim.hata(t('hareketler.filtrelemeBasarisiz'))
  }
}

const filtreTemizle = async () => {
  filtreBaslangic.value = null
  filtreBitis.value = null
  await loadData()
}

const csvExport = () => {
  window.open('/api/hareketler/export/csv', '_blank')
}

const excelIndir = async () => {
  try {
    const res = await excelAPI.hareketler()
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', 'Hareketler.xlsx')
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch {
    /* silent */
  }
}

const topluSil = async () => {
  topluSiliniyor.value = true
  try {
    for (const item of selectedItems.value) {
      await hareketAPI.delete(item.id)
    }
    toastBildirim.basarili(t('hareketler.topluSilindi', { n: selectedItems.value.length }))
    selectedItems.value = []
    await loadData()
  } catch {
    toastBildirim.hata(t('hareketler.silmeBasarisiz'))
  } finally {
    topluSiliniyor.value = false
  }
}


import { formatTarih as formatDate } from '../utils/format.js'
</script>

<style scoped>
.hareketler-container {
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
  overflow-x: auto;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #666;
  font-size: 16px;
}

.form-group {
  margin-bottom: 20px;
}

.fatura-ipucu {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-muted, #64748b);
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: bold;
  color: #333;
}

.form-group :deep(.p-inputtext),
.form-group :deep(.p-dropdown),
.form-group :deep(.p-inputnumber),
.form-group :deep(.p-datepicker),
.form-group :deep(.p-textarea) {
  width: 100%;
}

.positive {
  color: #4caf50;
  font-weight: bold;
}

.negative {
  color: #f44336;
  font-weight: bold;
}

.badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: bold;
}

.badge.tahsilat {
  background-color: #e8f5e9;
  color: #2e7d32;
}

.badge.odeme {
  background-color: #ffebee;
  color: #c62828;
}

.w-full {
  width: 100% !important;
}

.filter-date {
  width: 140px !important;
  margin-left: 8px;
}
.batch-action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: var(--blue-50, #eff6ff);
  border: 1px solid var(--blue-200, #bfdbfe);
  border-radius: 8px;
  margin-bottom: 12px;
}
.batch-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--blue-700, #1d4ed8);
}
.batch-buttons {
  display: flex;
  gap: 8px;
}
</style>
