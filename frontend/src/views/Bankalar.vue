<template>
  <div class="bankalar-container">
    <h1 class="page-title">
      {{ t('bankalar.title') }}
    </h1>

    <Toolbar class="toolbar">
      <template #start>
        <Button
          :label="t('bankalar.yeniBanka')"
          icon="pi pi-plus"
          class="p-button-success"
          @click="openDialog"
        />
      </template>
      <template #end>
        <Button
          :label="t('bankalar.excel')"
          icon="pi pi-file-excel"
          class="p-button-sm p-button-outlined"
          @click="excelIndir"
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
      <AppDataTable
        :value="bankaStore.bankalar"
        striped-rows
        :rows="10"
        :paginator="true"
        paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport RowsPerPageDropdown"
        :rows-per-page-options="[10, 20, 50]"
        :current-page-report-template="'{first} - {last} ({totalRecords} ' + $t('common.recordsWord') + ')'"
        gorunum-anahtari="bankalar"
      >
        <Column
          field="ad"
          :header="t('bankalar.colBankaAdi')"
          style="width: 200px"
        />
        <Column
          field="hesapNo"
          :header="t('bankalar.colHesapNo')"
          style="width: 150px"
        >
          <template #body="s">
            {{ s.data.hesapNo || '-' }}
          </template>
        </Column>
        <Column
          field="iban"
          :header="t('bankalar.iban')"
          style="width: 230px"
        >
          <template #body="s">
            <span
              v-if="s.data.iban"
              class="kopyalanabilir"
              @click="kopyala(s.data.iban, t('bankalar.ibanKopyalandi'))"
            >
              {{ s.data.iban }} <i class="pi pi-copy kopyala-ikon" />
            </span>
            <span v-else>-</span>
          </template>
        </Column>
        <Column
          field="bakiye"
          :header="t('bankalar.colBakiye')"
          style="width: 130px"
        >
          <template #body="s">
            <span
              class="gizli-veri"
              :class="s.data.bakiye >= 0 ? 'positive' : 'negative'"
            >{{ formatCurrency(s.data.bakiye) }}</span>
          </template>
        </Column>
        <Column
          :header="t('common.actions')"
          style="width: 180px"
        >
          <template #body="s">
            <Button
              icon="pi pi-list"
              class="p-button-rounded p-button-help p-button-sm"
              :title="t('bankalar.hareketler')"
              @click="detayAc(s.data)"
            />
            <Button
              icon="pi pi-pencil"
              class="p-button-rounded p-button-info p-button-sm"
              :title="t('common.edit')"
              @click="editBanka(s.data)"
            />
            <Button
              icon="pi pi-trash"
              class="p-button-rounded p-button-danger p-button-sm"
              :title="t('common.delete')"
              @click="confirmDelete(s.data.id)"
            />
          </template>
        </Column>
        <template #empty>
          <EmptyState
            v-if="bankaStore.bankalar.length === 0"
            :message="t('bankalar.empty')"
            :sub-message="t('bankalar.emptyHint')"
            icon="pi pi-building"
            :action-label="t('bankalar.yeniBanka')"
            action-icon="pi pi-plus"
            @action="openDialog"
          />
        </template>
      </AppDataTable>
    </div>

    <Dialog
      v-model:visible="showDialog"
      :header="editingId ? t('bankalar.duzenle') : t('bankalar.yeniBanka')"
      :modal="true"
      style="width: 500px"
    >
      <div class="form-group">
        <label>{{ t('bankalar.bankaAdi') }}</label>
        <InputText
          v-model="form.ad"
          :placeholder="t('bankalar.bankaAdiPlaceholder')"
          class="w-full"
        />
      </div>
      <div class="form-group">
        <label>{{ t('bankalar.hesapNo') }}</label>
        <InputText
          v-model="form.hesapNo"
          :placeholder="t('bankalar.hesapNoPlaceholder')"
          class="w-full"
        />
      </div>
      <div class="form-group">
        <label>{{ t('bankalar.iban') }}</label>
        <InputText
          v-model="form.iban"
          :placeholder="t('bankalar.ibanPlaceholder')"
          class="w-full"
        />
      </div>
      <div
        v-if="!editingId"
        class="form-group"
      >
        <label>{{ t('bankalar.acilisBakiyesi') }}</label>
        <InputNumber
          v-model="form.bakiye"
          :min="0"
          :min-fraction-digits="2"
          :max-fraction-digits="2"
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
          :label="editingId ? t('bankalar.guncelle') : t('common.save')"
          icon="pi pi-check"
          :loading="saving"
          @click="saveBanka"
        />
      </template>
    </Dialog>

    <Message
      v-if="bankaStore.error"
      severity="error"
      :text="bankaStore.error"
    />

    <!-- Banka detayı: bakiye kartı ve hareket (mutabakat) geçmişi -->
    <Drawer
      v-model:visible="detayDialog"
      position="right"
      :header="detayBanka?.ad || t('bankalar.title')"
      :style="{ width: '640px', maxWidth: '96vw' }"
    >
      <div
        v-if="detayBanka"
        class="detay-bilgi"
      >
        <div class="detay-kutu">
          <span>{{ t('bankalar.colBakiye') }}</span>
          <strong :class="detayBanka.bakiye >= 0 ? 'positive' : 'negative'">
            {{ formatCurrency(detayBanka.bakiye) }}
          </strong>
        </div>
        <div class="detay-kutu">
          <span>{{ t('bankalar.colHesapNo') }}</span>
          <strong>{{ detayBanka.hesapNo || '-' }}</strong>
        </div>
        <div class="detay-kutu genis">
          <span>IBAN</span>
          <strong
            class="kopyalanabilir"
            @click="detayBanka.iban && kopyala(detayBanka.iban, t('bankalar.ibanKopyalandi'))"
          >
            {{ detayBanka.iban || '-' }} <i
              v-if="detayBanka.iban"
              class="pi pi-copy kopyala-ikon"
            />
          </strong>
        </div>
      </div>

      <div class="detay-baslik">
        <span><i class="pi pi-list" /> {{ t('bankalar.hareketler') }}</span>
        <Button
          :label="t('bankalar.mutabakataGit')"
          icon="pi pi-link"
          class="p-button-sm p-button-outlined"
          @click="mutabakataGit"
        />
      </div>

      <DataTable
        :value="hareketler"
        :loading="hareketYukleniyor"
        striped-rows
        size="small"
        scrollable
        scroll-height="52vh"
        :paginator="hareketler.length > 20"
        :rows="20"
      >
        <template #empty>
          <EmptyState :message="t('bankalar.hareketYok')" />
        </template>
        <Column
          field="tarih"
          :header="t('common.date')"
          style="width: 110px"
        />
        <Column
          field="aciklama"
          :header="t('common.description')"
        />
        <Column
          field="borc"
          :header="t('bankalar.borc')"
          style="width: 110px"
        >
          <template #body="s">
            <span v-if="s.data.borc">{{ formatCurrency(s.data.borc) }}</span>
          </template>
        </Column>
        <Column
          field="alacak"
          :header="t('bankalar.alacak')"
          style="width: 110px"
        >
          <template #body="s">
            <span v-if="s.data.alacak">{{ formatCurrency(s.data.alacak) }}</span>
          </template>
        </Column>
        <Column
          field="bakiye"
          :header="t('bankalar.colBakiye')"
          style="width: 120px"
        >
          <template #body="s">
            <span class="gizli-veri">{{ formatCurrency(s.data.bakiye) }}</span>
          </template>
        </Column>
        <Column
          field="eslestirildi"
          :header="t('bankalar.durum')"
          style="width: 120px"
        >
          <template #body="s">
            <Tag
              v-if="s.data.eslestirildi"
              severity="success"
              :value="t('bankalar.eslesti')"
            />
            <Tag
              v-else
              severity="warn"
              :value="t('bankalar.bekliyor')"
            />
          </template>
        </Column>
      </DataTable>
    </Drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { useBankaStore } from '../stores/bankaStore.js'
import { usePanoyaKopyala } from '../composables/usePanoyaKopyala.js'
import { useFormKorumasi } from '../composables/useFormKorumasi.js'
import { excelAPI, bankaMutabakatAPI } from '../api/index.js'
import EmptyState from '../components/EmptyState.vue'
import { formatCurrency } from '../utils/format.js'
import { useI18n } from 'vue-i18n'

const toastBildirim = useToastBildirim()
const { t } = useI18n()
const confirm = useConfirm()
const router = useRouter()
const bankaStore = useBankaStore()
const { kopyala } = usePanoyaKopyala()

const showDialog = ref(false)
const loading = ref(false)
const saving = ref(false)
const editingId = ref(null)

// Banka detayı: hareket (mutabakat) geçmişi
const detayDialog = ref(false)
const detayBanka = ref(null)
const hareketler = ref([])
const hareketYukleniyor = ref(false)

const detayAc = async (banka) => {
  detayBanka.value = banka
  detayDialog.value = true
  hareketYukleniyor.value = true
  hareketler.value = []
  try {
    const r = await bankaMutabakatAPI.listele(banka.id)
    hareketler.value = Array.isArray(r.data) ? r.data : []
  } catch {
    hareketler.value = []
  } finally {
    hareketYukleniyor.value = false
  }
}

const mutabakataGit = () => {
  detayDialog.value = false
  router.push({ name: 'BankaMutabakat' })
}

const form = ref({ ad: '', hesapNo: '', iban: '', bakiye: 0 })
const { temizle: formTemizle } = useFormKorumasi(form)

onMounted(async () => {
  loading.value = true
  try {
    await bankaStore.getAllBankalar()
  } catch {
    toastBildirim.hata(t('bankalar.hataYukleme'))
  } finally {
    loading.value = false
  }
})

const openDialog = () => {
  editingId.value = null
  form.value = { ad: '', hesapNo: '', iban: '', bakiye: 0 }
  formTemizle()
  showDialog.value = true
}

const closeDialog = () => {
  showDialog.value = false
}

const editBanka = (banka) => {
  editingId.value = banka.id
  form.value = { ad: banka.ad, hesapNo: banka.hesapNo || '', iban: banka.iban || '', bakiye: 0 }
  formTemizle()
  showDialog.value = true
}

const saveBanka = async () => {
  if (!form.value.ad.trim()) {
    toastBildirim.uyari(t('bankalar.adBosOlamaz'))
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await bankaStore.updateBanka(editingId.value, {
        ad: form.value.ad,
        hesapNo: form.value.hesapNo,
        iban: form.value.iban
      })
      toastBildirim.basarili(t('bankalar.guncellendi'))
    } else {
      await bankaStore.addBanka(form.value)
      toastBildirim.basarili(t('bankalar.olusturuldu'))
    }
    formTemizle()
    closeDialog()
  } catch {
    toastBildirim.hata(t('bankalar.islemBasarisiz'))
  } finally {
    saving.value = false
  }
}

const confirmDelete = (id) => {
  confirm.require({
    message: t('bankalar.silOnayMesaj'),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    rejectProps: { label: t('common.vazgec'), severity: 'secondary', outlined: true, size: 'small' },
    acceptProps: { label: t('common.evetSil'), severity: 'danger', size: 'small' },
    accept: async () => {
      try {
        await bankaStore.deleteBanka(id)
        toastBildirim.basarili(t('bankalar.silindi'))
      } catch {
        toastBildirim.hata(t('bankalar.silmeBasarisiz'))
      }
    }
  })
}

const excelIndir = async () => {
  try {
    const res = await excelAPI.bankalar()
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', 'Bankalar.xlsx')
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch {
    /* silent */
  }
}

</script>

<style scoped>
.bankalar-container {
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
.loading {
  text-align: center;
  padding: 40px;
  color: var(--text-muted);
}
.form-group {
  margin-bottom: 20px;
}
.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: bold;
  color: var(--text-secondary);
}
.positive {
  color: var(--success);
  font-weight: bold;
}
.negative {
  color: var(--danger);
  font-weight: bold;
}
.kopyalanabilir {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.detay-bilgi {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 16px;
}
.detay-kutu {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-card);
}
.detay-kutu span {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: var(--text-muted);
}
.detay-kutu.genis {
  grid-column: 1 / -1;
}
.detay-baslik {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
  font-weight: 600;
  color: var(--text-primary);
}
.kopyalanabilir:hover {
  color: var(--accent);
}
.kopyala-ikon {
  font-size: 11px;
  opacity: 0.5;
}
.kopyalanabilir:hover .kopyala-ikon {
  opacity: 1;
}
.w-full {
  width: 100% !important;
}
</style>
