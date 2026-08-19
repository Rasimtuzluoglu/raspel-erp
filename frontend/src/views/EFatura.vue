<template>
  <div class="efatura-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        E-Fatura
      </h1>
      <div class="sag-butonlar">
        <Button
          label="Faturadan Oluştur"
          icon="pi pi-file-plus"
          @click="olusturDialogAc"
        />
      </div>
    </div>

    <IlkZiyaretIpuclari
      anahtar="efatura"
      baslik="E-Fatura"
      metin="Satış faturanızdan UBL-TR 2.1 e-fatura taslağı oluşturun, GİB'e gönderin ve XML belgesini indirin. GİB entegratör uç noktası tanımlı değilse gönderimler yerel onay olarak işaretlenir."
    />

    <div class="bilgi-kutu">
      <i class="pi pi-info-circle" />
      GİB entegratör uç noktası (<code>app.efatura.gib-endpoint</code>) tanımlı değilse gönderimler yerel onay
      (simülasyon) olarak işaretlenir.
    </div>

    <AppDataTable
      :value="list"
      :loading="yukleniyor"
      arama-aktif
      arama-placeholder="E-Faturalarda ara..."
      gorunum-anahtari="efatura_liste"
    >
      <Column
        field="faturaNo"
        header="Fatura No"
        sortable
      />
      <Column
        field="ettn"
        header="ETTN"
      >
        <template #body="{ data }">
          <span class="mono">{{ kisaEttn(data.ettn) }}</span>
        </template>
      </Column>
      <Column
        field="aliciUnvan"
        header="Alıcı"
      />
      <Column
        field="odenecekTutar"
        header="Tutar"
      >
        <template #body="{ data }">
          {{ formatCurrency(data.odenecekTutar) }}
        </template>
      </Column>
      <Column
        field="senaryo"
        header="Senaryo"
      />
      <Column
        field="gibDurumKodu"
        header="GİB Durumu"
      >
        <template #body="{ data }">
          <Tag
            :value="durumEtiketi(data.gibDurumKodu)"
            :severity="durumSeverity(data.gibDurumKodu)"
          />
          <div
            class="durum-aciklama"
            :title="data.gibDurumAciklama"
          >
            {{ data.gibDurumAciklama }}
          </div>
        </template>
      </Column>
      <Column
        field="olusturmaTarihi"
        header="Oluşturulma"
      >
        <template #body="{ data }">
          {{ formatDateTime(data.olusturmaTarihi) }}
        </template>
      </Column>
      <Column
        header="İşlem"
        style="width: 90px"
      >
        <template #body="{ data }">
          <div class="eylem-btns">
            <Button
              v-if="data.gibDurumKodu < 1200"
              icon="pi pi-send"
              class="p-button-rounded p-button-text"
              title="GİB'e Gönder"
              @click="gibGonder(data)"
            />
            <Button
              icon="pi pi-download"
              class="p-button-rounded p-button-text"
              title="XML İndir"
              @click="xmlIndir(data)"
            />
          </div>
        </template>
      </Column>
    </AppDataTable>

    <Dialog
      v-model:visible="olusturDialog"
      header="Faturadan E-Fatura Oluştur"
      modal
      :style="{ width: '520px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>Fatura</label>
          <Select
            v-model="olusturForm.faturaId"
            :options="faturalar"
            option-label="etiket"
            option-value="id"
            class="w-full"
            filter
            placeholder="Fatura seçin"
          />
        </div>
        <div class="field">
          <label>Senaryo</label>
          <Select
            v-model="olusturForm.senaryo"
            :options="['TEMELFATURA', 'TICARIFATURA', 'EARSIVEFATURA']"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Tip</label>
          <Select
            v-model="olusturForm.tip"
            :options="['SATIS', 'IADE', 'TEVKIFAT', 'ISTISNA']"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          label="İptal"
          icon="pi pi-times"
          class="p-button-text"
          @click="olusturDialog = false"
        />
        <Button
          label="Oluştur"
          icon="pi pi-file-plus"
          :loading="kaydediliyor"
          @click="olustur"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { eFaturaAPI, faturaAPI } from '../api/index.js'
import IlkZiyaretIpuclari from '../components/IlkZiyaretIpuclari.vue'

const toast = useToast()
const toastBildirim = useToastBildirim()

const list = ref([])
const faturalar = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const olusturDialog = ref(false)
const olusturForm = ref({ faturaId: null, senaryo: 'TEMELFATURA', tip: 'SATIS' })

const formatCurrency = (v) =>
  v == null ? '0,00 ₺' : new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(v)
const formatDateTime = (d) =>
  d ? new Intl.DateTimeFormat('tr-TR', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(d)) : '-'
const kisaEttn = (e) => (e ? e.slice(0, 8) + '…' : '-')
const durumEtiketi = (k) =>
  ({ 1000: 'Hazırlandı', 1200: "GİB'e Gönderildi", 1300: 'Onaylandı', 1350: 'Reddedildi' })[k] || k
const durumSeverity = (k) => (k >= 1300 ? 'success' : k === 1200 ? 'warning' : k === 1350 ? 'danger' : 'info')

onMounted(() => {
  yukle()
  faturalariYukle()
})

const yukle = async () => {
  yukleniyor.value = true
  try {
    const r = await eFaturaAPI.getTumu()
    list.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'E-Faturalar yüklenemedi')
  }
  yukleniyor.value = false
}

const faturalariYukle = async () => {
  try {
    const r = await faturaAPI.getAll()
    const data = r.data?.content || r.data || []
    faturalar.value = data.map((f) => ({
      ...f,
      etiket: `${f.faturaNumarasi} - ${f.cariHesapAd || ''} (${formatCurrency(f.genelToplam)})`
    }))
  } catch {
    /* empty */
  }
}

const olusturDialogAc = () => {
  olusturForm.value = { faturaId: null, senaryo: 'TEMELFATURA', tip: 'SATIS' }
  olusturDialog.value = true
}

const olustur = async () => {
  if (!olusturForm.value.faturaId) {
    toastBildirim.uyari('Fatura seçiniz')
    return
  }
  kaydediliyor.value = true
  try {
    await eFaturaAPI.olustur(olusturForm.value.faturaId, olusturForm.value.senaryo, olusturForm.value.tip)
    toastBildirim.basarili('E-Fatura taslağı oluşturuldu')
    olusturDialog.value = false
    yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Oluşturma başarısız')
  }
  kaydediliyor.value = false
}

const gibGonder = async (data) => {
  try {
    await eFaturaAPI.gibGonder(data.id)
    toast.add({ severity: 'success', summary: 'Gönderildi', detail: "E-Fatura GİB'e iletildi", life: 3000 })
    yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Gönderim başarısız')
  }
}

const xmlIndir = async (data) => {
  try {
    const r = await eFaturaAPI.xmlIndir(data.id)
    const blob = new Blob([r.data], { type: 'application/xml' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `e-fatura-${data.id}.xml`
    a.click()
    URL.revokeObjectURL(url)
  } catch (err) {
    toastBildirim.hata('XML indirilemedi')
  }
}
</script>

<style scoped>
.efatura-container {
  padding: 0;
}
.sayfa-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.bilgi-kutu {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  margin-bottom: 18px;
  background: rgba(59, 130, 246, 0.08);
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 10px;
  font-size: 13px;
  color: var(--text-secondary);
}
.bilgi-kutu code {
  background: rgba(0, 0, 0, 0.2);
  padding: 1px 5px;
  border-radius: 4px;
}
.mono {
  font-family: monospace;
  font-size: 12px;
}
.eylem-btns {
  display: flex;
  align-items: center;
  gap: 2px;
}
.durum-aciklama {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 4px;
  max-width: 260px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.form-grid {
  display: flex;
  flex-direction: column;
  gap: 14px;
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
