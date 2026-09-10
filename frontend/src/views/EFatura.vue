<template>
  <div class="efatura-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        {{ t('efatura.title') }}
      </h1>
      <div class="sag-butonlar">
        <Button
          :label="t('efatura.faturadanOlustur')"
          icon="pi pi-file-plus"
          @click="olusturDialogAc"
        />
      </div>
    </div>

    <IlkZiyaretIpuclari
      anahtar="efatura"
      :baslik="t('efatura.ipucuBaslik')"
      :metin="t('efatura.ipucuMetin')"
    />

    <div class="bilgi-kutu">
      <i class="pi pi-info-circle" />
      {{ t('efatura.bilgiKutu1') }}<code>app.efatura.gib-endpoint</code>{{ t('efatura.bilgiKutu2') }}
    </div>

    <AppDataTable
      :value="list"
      :loading="yukleniyor"
      arama-aktif
      :arama-placeholder="t('efatura.aramaPlaceholder')"
      gorunum-anahtari="efatura_liste"
    >
      <Column
        field="faturaNo"
        :header="t('efatura.faturaNo')"
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
        :header="t('efatura.alici')"
      />
      <Column
        field="odenecekTutar"
        :header="t('common.amount')"
      >
        <template #body="{ data }">
          {{ formatCurrency(data.odenecekTutar) }}
        </template>
      </Column>
      <Column
        field="senaryo"
        :header="t('efatura.senaryo')"
      />
      <Column
        field="gibDurumKodu"
        :header="t('efatura.gibDurumu')"
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
        :header="t('efatura.olusturulma')"
      >
        <template #body="{ data }">
          {{ formatDateTime(data.olusturmaTarihi) }}
        </template>
      </Column>
      <Column
        :header="t('efatura.islem')"
        style="width: 90px"
      >
        <template #body="{ data }">
          <div class="eylem-btns">
            <Button
              v-if="data.gibDurumKodu < 1200"
              icon="pi pi-send"
              class="p-button-rounded p-button-text"
              :title="t('efatura.gibGonder')"
              @click="gibGonder(data)"
            />
            <Button
              v-if="data.gibDurumKodu === 1200"
              icon="pi pi-refresh"
              class="p-button-rounded p-button-text"
              :title="t('efatura.gibSorgula')"
              @click="durumSorgula(data)"
            />
            <Button
              icon="pi pi-download"
              class="p-button-rounded p-button-text"
              :title="t('efatura.xmlIndir')"
              @click="xmlIndir(data)"
            />
          </div>
        </template>
      </Column>
    </AppDataTable>

    <Dialog
      v-model:visible="olusturDialog"
      :header="t('efatura.olusturBaslik')"
      modal
      :style="{ width: '520px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('efatura.fatura') }}</label>
          <Select
            v-model="olusturForm.faturaId"
            :options="faturalar"
            option-label="etiket"
            option-value="id"
            class="w-full"
            filter
            :placeholder="t('efatura.faturaSecin')"
          />
        </div>
        <div class="field">
          <label>{{ t('efatura.senaryo') }}</label>
          <Select
            v-model="olusturForm.senaryo"
            :options="['TEMELFATURA', 'TICARIFATURA', 'EARSIVEFATURA']"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('efatura.tip') }}</label>
          <Select
            v-model="olusturForm.tip"
            :options="['SATIS', 'IADE', 'TEVKIFAT', 'ISTISNA']"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="olusturDialog = false"
        />
        <Button
          :label="t('efatura.olustur')"
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
import { formatCurrency, formatTarihKisa as formatDateTime } from '../utils/format.js'
import { useI18n } from 'vue-i18n'

const toast = useToast()
const toastBildirim = useToastBildirim()
const { t } = useI18n()

const list = ref([])
const faturalar = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const olusturDialog = ref(false)
const olusturForm = ref({ faturaId: null, senaryo: 'TEMELFATURA', tip: 'SATIS' })

const kisaEttn = (e) => (e ? e.slice(0, 8) + '…' : '-')
const durumEtiketi = (k) =>
  ({ 1000: t('efatura.durumHazirlandi'), 1200: t('efatura.durumGonderildi'), 1300: t('efatura.durumOnaylandi'), 1350: t('efatura.durumReddedildi') })[k] || k
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
    toastBildirim.hata(err?.response?.data?.message || t('efatura.hataYukleme'))
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
    toastBildirim.uyari(t('efatura.faturaSeciniz'))
    return
  }
  kaydediliyor.value = true
  try {
    await eFaturaAPI.olustur(olusturForm.value.faturaId, olusturForm.value.senaryo, olusturForm.value.tip)
    toastBildirim.basarili(t('efatura.taslakOlusturuldu'))
    olusturDialog.value = false
    yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('efatura.olusturmaBasarisiz'))
  }
  kaydediliyor.value = false
}

const gibGonder = async (data) => {
  try {
    await eFaturaAPI.gibGonder(data.id)
    toast.add({ severity: 'success', summary: t('efatura.gonderildi'), detail: t('efatura.gibIletildi'), life: 3000 })
    yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('efatura.gonderimBasarisiz'))
  }
}

const durumSorgula = async (data) => {
  try {
    await eFaturaAPI.durumSorgula(data.id)
    toast.add({ severity: 'success', summary: t('efatura.guncellendi'), detail: t('efatura.gibSorgulandi'), life: 3000 })
    yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('efatura.durumSorgulanamadi'))
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
    toastBildirim.hata(t('efatura.xmlIndirilemedi'))
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
