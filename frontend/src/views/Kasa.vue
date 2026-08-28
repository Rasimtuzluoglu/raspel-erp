<template>
  <div class="kasa-container">
    <h1>Kasa Yönetimi</h1>

    <Toolbar class="toolbar">
      <template #start>
        <Button
          label="Yeni Kasa"
          icon="pi pi-plus"
          class="p-button-success"
          @click="openKasaDialog"
        />
      </template>
      <template #end>
        <Button
          label="Excel"
          icon="pi pi-file-excel"
          class="p-button-sm p-button-outlined"
          @click="excelIndir"
        />
      </template>
    </Toolbar>

    <div
      v-if="kasaStore.loading"
      class="loading"
    >
      <p><i class="pi pi-spin pi-spinner" /> Yükleniyor...</p>
    </div>

    <div
      v-if="!kasaStore.loading"
      class="kasa-cards"
    >
      <div
        v-for="kasa in kasaStore.kasalar"
        :key="kasa.id"
        class="kasa-card"
        :class="{ active: seciliKasaId === kasa.id }"
        @click="kasaSec(kasa)"
      >
        <div class="kasa-ust">
          <i class="pi pi-wallet" />
          <h3>{{ kasa.ad }}</h3>
        </div>
        <p
          class="kasa-bakiye"
          :class="kasa.bakiye >= 0 ? 'positive' : 'negative'"
        >
          {{ formatCurrency(kasa.bakiye) }}
        </p>
        <div class="kasa-islem">
          <Button
            icon="pi pi-pencil"
            class="p-button-rounded p-button-info p-button-sm"
            @click.stop="editKasa(kasa)"
          />
          <Button
            icon="pi pi-trash"
            class="p-button-rounded p-button-danger p-button-sm"
            @click.stop="confirmDel(kasa.id)"
          />
        </div>
      </div>
      <EmptyState
        v-if="kasaStore.kasalar.length === 0"
        message="Henüz kasa hesabı bulunamadı"
        sub-message="İlk kasa hesabınızı eklemek için Yeni Kasa butonuna tıklayın"
        icon="pi pi-wallet"
        action-label="Yeni Kasa"
        action-icon="pi pi-plus"
        class="full-width"
        @action="openKasaDialog"
      />
    </div>

    <Dialog
      v-model:visible="showKasaDialog"
      :header="editingKasaId ? 'Kasa Düzenle' : 'Yeni Kasa'"
      :modal="true"
      style="width: 400px"
    >
      <div class="form-group">
        <label>Kasa Adı *</label>
        <InputText
          v-model="kasaForm.ad"
          placeholder="Kasa adı"
          class="w-full"
        />
      </div>
      <div
        v-if="!editingKasaId"
        class="form-group"
      >
        <label>Açılış Bakiyesi</label>
        <InputNumber
          v-model="kasaForm.bakiye"
          :min="0"
          :min-fraction-digits="2"
          :max-fraction-digits="2"
          class="w-full"
        />
      </div>
      <template #footer>
        <Button
          label="İptal"
          icon="pi pi-times"
          class="p-button-text"
          @click="showKasaDialog = false"
        />
        <Button
          :label="editingKasaId ? 'Güncelle' : 'Kaydet'"
          icon="pi pi-check"
          :loading="saving"
          @click="saveKasa"
        />
      </template>
    </Dialog>

    <div
      v-if="seciliKasa"
      class="hareket-bolumu"
    >
      <div class="hareket-header">
        <h2>{{ seciliKasa.ad }} - Hareketler</h2>
        <Button
          label="+ Gelir Ekle"
          icon="pi pi-plus-circle"
          class="p-button-success p-button-sm"
          @click="openHareketDialog('GELIR')"
        />
        <Button
          label="+ Gider Ekle"
          icon="pi pi-minus-circle"
          class="p-button-danger p-button-sm"
          @click="openHareketDialog('GIDER')"
        />
      </div>

      <div class="table-container">
        <DataTable
          state-storage="session"
          state-key="kasa-table-state"
          :value="kasaHareketler"
          striped-rows
          :rows="10"
          :paginator="true"
          paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport"
          current-page-report-template="{first} - {last} ({totalRecords} kayıt)"
        >
          <Column
            field="tarih"
            header="Tarih"
            style="width: 100px"
          >
            <template #body="s">
              {{ formatDate(s.data.hareketTarihi) }}
            </template>
          </Column>
          <Column
            field="tur"
            header="Tür"
            style="width: 80px"
          >
            <template #body="s">
              <span :class="['badge', s.data.tur === 'GELIR' ? 'gelir' : 'gider']">
                {{ s.data.tur === 'GELIR' ? 'Gelir' : 'Gider' }}
              </span>
            </template>
          </Column>
          <Column
            field="tutar"
            header="Tutar"
            style="width: 120px"
          >
            <template #body="s">
              <span :class="s.data.tur === 'GELIR' ? 'positive' : 'negative'">
                {{ formatCurrency(s.data.tutar) }}
              </span>
            </template>
          </Column>
          <Column
            field="kategoriAd"
            header="Kategori"
            style="width: 140px"
          >
            <template #body="s">
              {{ s.data.kategoriAd || '-' }}
            </template>
          </Column>
          <Column
            field="aciklama"
            header="Açıklama"
          />
          <Column
            header=""
            style="width: 60px"
          >
            <template #body="s">
              <Button
                icon="pi pi-trash"
                class="p-button-rounded p-button-danger p-button-sm"
                @click="delHareket(s.data.id)"
              />
            </template>
          </Column>
        </DataTable>
        <Message
          v-if="kasaHareketler && kasaHareketler.length === 0"
          severity="info"
          text="Hareket bulunmamaktadır."
        />
      </div>
    </div>

    <Dialog
      v-model:visible="showHareketDialog"
      :header="hareketBaslik"
      :modal="true"
      style="width: 500px"
    >
      <div class="form-group">
        <label>Tutar *</label>
        <InputNumber
          v-model="hareketForm.tutar"
          :min="0.01"
          :min-fraction-digits="2"
          :max-fraction-digits="2"
          class="w-full"
        />
      </div>
      <div class="form-group">
        <label>Tarih *</label>
        <DatePicker
          v-model="hareketForm.hareketTarihi"
          date-format="dd.mm.yy"
          class="w-full"
        />
      </div>
      <div class="form-group">
        <label>Kategori</label>
        <Dropdown
          v-model="hareketForm.kategoriId"
          :options="kategoriSecenekler"
          option-label="ad"
          option-value="id"
          placeholder="Seçiniz"
          class="w-full"
        />
      </div>
      <div class="form-group">
        <label>Açıklama</label>
        <Textarea
          v-model="hareketForm.aciklama"
          rows="2"
          class="w-full"
        />
      </div>
      <template #footer>
        <Button
          label="İptal"
          icon="pi pi-times"
          class="p-button-text"
          @click="showHareketDialog = false"
        />
        <Button
          label="Kaydet"
          icon="pi pi-check"
          :loading="saving"
          @click="saveHareket"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { useKasaStore } from '../stores/kasaStore.js'
import { useKategoriStore } from '../stores/kategoriStore.js'
import { kasaAPI, excelAPI } from '../api/index.js'
import EmptyState from '../components/EmptyState.vue'
import { formatCurrency } from '../utils/format.js'

const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const kasaStore = useKasaStore()
const kategoriStore = useKategoriStore()

const seciliKasaId = ref(null)
const seciliKasa = ref(null)
const kasaHareketler = ref([])
const saving = ref(false)

const showKasaDialog = ref(false)
const editingKasaId = ref(null)
const kasaForm = ref({ ad: '', bakiye: 0 })

const showHareketDialog = ref(false)
const hareketTur = ref('GELIR')
const hareketForm = ref({ tutar: null, hareketTarihi: new Date(), kategoriId: null, aciklama: '' })

const hareketBaslik = computed(() => (hareketTur.value === 'GELIR' ? 'Gelir Ekle' : 'Gider Ekle'))

const kategoriSecenekler = computed(() => kategoriStore.kategoriler.filter((k) => k.tur === hareketTur.value))

onMounted(async () => {
  await Promise.all([kasaStore.getAllKasalar(), kategoriStore.getAllKategoriler()])
})

const kasaSec = async (kasa) => {
  seciliKasaId.value = kasa.id
  seciliKasa.value = kasa
  try {
    const r = await kasaAPI.getHareketler(kasa.id)
    kasaHareketler.value = r.data
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || 'Hareketler yüklenemedi')
  }
}

const openKasaDialog = () => {
  editingKasaId.value = null
  kasaForm.value = { ad: '', bakiye: 0 }
  showKasaDialog.value = true
}

const editKasa = (kasa) => {
  editingKasaId.value = kasa.id
  kasaForm.value = { ad: kasa.ad, bakiye: 0 }
  showKasaDialog.value = true
}

const saveKasa = async () => {
  if (!kasaForm.value.ad.trim()) {
    toastBildirim.uyari('Kasa adı giriniz')
    return
  }
  saving.value = true
  try {
    if (editingKasaId.value) {
      await kasaStore.updateKasa(editingKasaId.value, kasaForm.value)
      toastBildirim.basarili('Kasa güncellendi')
    } else {
      await kasaStore.addKasa(kasaForm.value)
      toastBildirim.basarili('Kasa oluşturuldu')
    }
    showKasaDialog.value = false
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || 'İşlem başarısız')
  } finally {
    saving.value = false
  }
}

const confirmDel = (id) => {
  confirm.require({
    message: 'Bu kasayı silmek istediğinizden emin misiniz?',
    header: 'Onay',
    icon: 'pi pi-exclamation-triangle',
    accept: async () => {
      try {
        await kasaStore.deleteKasa(id)
        if (seciliKasaId.value === id) {
          seciliKasaId.value = null
          seciliKasa.value = null
          kasaHareketler.value = []
        }
        toastBildirim.basarili('Kasa silindi')
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || err?.message || 'Silme başarısız')
      }
    }
  })
}

const openHareketDialog = (tur) => {
  hareketTur.value = tur
  hareketForm.value = { tutar: null, hareketTarihi: new Date(), kategoriId: null, aciklama: '' }
  showHareketDialog.value = true
}

const saveHareket = async () => {
  if (!hareketForm.value.tutar || hareketForm.value.tutar <= 0) {
    toastBildirim.uyari('Geçerli tutar giriniz')
    return
  }
  saving.value = true
  try {
    await kasaAPI.addHareket(seciliKasaId.value, {
      tur: hareketTur.value,
      tutar: hareketForm.value.tutar,
      hareketTarihi: hareketForm.value.hareketTarihi.toISOString().split('T')[0],
      kategoriId: hareketForm.value.kategoriId,
      aciklama: hareketForm.value.aciklama
    })
    const r = await kasaAPI.getHareketler(seciliKasaId.value)
    kasaHareketler.value = r.data
    await kasaStore.getAllKasalar()
    const guncel = kasaStore.kasalar.find((k) => k.id === seciliKasaId.value)
    if (guncel) seciliKasa.value = guncel
    showHareketDialog.value = false
    toastBildirim.basarili('Hareket eklendi')
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || 'İşlem başarısız')
  } finally {
    saving.value = false
  }
}

const delHareket = async (id) => {
  try {
    await kasaAPI.deleteHareket(id)
    kasaHareketler.value = kasaHareketler.value.filter((h) => h.id !== id)
    await kasaStore.getAllKasalar()
    const guncel = kasaStore.kasalar.find((k) => k.id === seciliKasaId.value)
    if (guncel) seciliKasa.value = guncel
    toastBildirim.basarili('Hareket silindi')
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || 'Silme başarısız')
  }
}

const excelIndir = async () => {
  try {
    const res = await excelAPI.kasalar()
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', 'Kasalar.xlsx')
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch {
    /* silent */
  }
}


const formatDate = (d) =>
  d ? new Intl.DateTimeFormat('tr-TR', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(d)) : '-'
</script>

<style scoped>
.kasa-container {
  padding: 20px;
}
h1 {
  color: var(--text-primary);
  margin-bottom: 20px;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.5px;
}
h2 {
  color: var(--text-primary);
  font-size: 20px;
  margin: 0;
}
.toolbar {
  margin-bottom: 20px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px 18px;
}
.loading {
  text-align: center;
  padding: 40px;
  color: #666;
}
.kasa-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 15px;
  margin-bottom: 30px;
}
.kasa-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
}
.kasa-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
  border-color: rgba(59, 130, 246, 0.25);
}
.kasa-card.active {
  border-color: #3b82f6;
  background: rgba(59, 130, 246, 0.08);
}
.kasa-ust {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 15px;
}
.kasa-ust i {
  font-size: 28px;
  color: #1976d2;
}
.kasa-ust h3 {
  margin: 0;
  font-size: 18px;
}
.kasa-bakiye {
  font-size: 24px;
  font-weight: bold;
  margin: 0 0 15px 0;
}
.kasa-islem {
  display: flex;
  gap: 8px;
}
.positive {
  color: #4caf50;
}
.negative {
  color: #f44336;
}
.hareket-bolumu {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 20px;
}
.hareket-header {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
.table-container {
  overflow-x: auto;
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
.full-width {
  grid-column: 1 / -1;
}
</style>
