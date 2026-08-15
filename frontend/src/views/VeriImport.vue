<template>
  <div class="import-page">
    <PageHeader
      title="Veri İçe Aktar"
      subtitle="CSV dosyaları ile toplu stok ve cari hesap aktarımı yapın."
    />

    <div class="import-grid">
      <Card>
        <template #title>
          <i
            class="pi pi-box"
            style="margin-right:8px"
          />Stok Aktar
        </template>
        <template #content>
          <p class="import-desc">
            CSV dosyası ile toplu stok girişi. Kolonlar: <code>ad;stokKodu;barkod;birim;fiyat;miktar;minMiktar</code>
          </p>
          <div
            class="import-dropzone"
            @dragover.prevent
            @drop.prevent="dosyaSec($event, 'stok')"
            @click="$refs.stokInput.click()"
          >
            <input
              ref="stokInput"
              type="file"
              accept=".csv"
              hidden
              @change="dosyaDegisti($event, 'stok')"
            >
            <i class="pi pi-upload" />
            <span>{{ stokDosya ? stokDosya.name : 'CSV dosyasını seçin veya sürükleyin' }}</span>
          </div>
          <Button
            v-if="stokDosya"
            label="Aktar"
            icon="pi pi-upload"
            class="p-button-success w-full"
            :loading="stokYukleniyor"
            @click="aktar('stok')"
          />
          <div
            v-if="stokSonuc"
            class="import-sonuc"
          >
            <Message
              :severity="stokSonuc.hatalar?.length ? 'warn' : 'success'"
              :closable="true"
            >
              <strong>{{ stokSonuc.basarili }} stok aktarıldı.</strong>
              <span v-if="stokSonuc.hatalar?.length"> {{ stokSonuc.hatalar.length }} hata.</span>
            </Message>
            <ul
              v-if="stokSonuc.hatalar?.length"
              class="hata-listesi"
            >
              <li
                v-for="h in stokSonuc.hatalar"
                :key="h"
              >
                {{ h }}
              </li>
            </ul>
          </div>
        </template>
      </Card>

      <Card>
        <template #title>
          <i
            class="pi pi-users"
            style="margin-right:8px"
          />Cari Hesap Aktar
        </template>
        <template #content>
          <p class="import-desc">
            CSV dosyası ile toplu cari hesap girişi. Kolonlar: <code>ad;vergiNo;telefon;eposta;il;ilce;adres</code>
          </p>
          <div
            class="import-dropzone"
            @dragover.prevent
            @drop.prevent="dosyaSec($event, 'cari')"
            @click="$refs.cariInput.click()"
          >
            <input
              ref="cariInput"
              type="file"
              accept=".csv"
              hidden
              @change="dosyaDegisti($event, 'cari')"
            >
            <i class="pi pi-upload" />
            <span>{{ cariDosya ? cariDosya.name : 'CSV dosyasını seçin veya sürükleyin' }}</span>
          </div>
          <Button
            v-if="cariDosya"
            label="Aktar"
            icon="pi pi-upload"
            class="p-button-success w-full"
            :loading="cariYukleniyor"
            @click="aktar('cari')"
          />
          <div
            v-if="cariSonuc"
            class="import-sonuc"
          >
            <Message
              :severity="cariSonuc.hatalar?.length ? 'warn' : 'success'"
              :closable="true"
            >
              <strong>{{ cariSonuc.basarili }} cari hesap aktarıldı.</strong>
              <span v-if="cariSonuc.hatalar?.length"> {{ cariSonuc.hatalar.length }} hata.</span>
            </Message>
            <ul
              v-if="cariSonuc.hatalar?.length"
              class="hata-listesi"
            >
              <li
                v-for="h in cariSonuc.hatalar"
                :key="h"
              >
                {{ h }}
              </li>
            </ul>
          </div>
        </template>
      </Card>

      <Card>
        <template #title>
          <i
            class="pi pi-file-import"
            style="margin-right:8px"
          />Alış Faturası Aktar
        </template>
        <template #content>
          <p class="import-desc">
            CSV dosyası ile toplu alış faturası girişi. Kolonlar: <code>faturaNo;tarih;cariId;stokKodu;aciklama;adet;birimFiyat;kdvOrani</code> (aynı faturaNo'ya sahip satırlar tek faturada birleştirilir, stoklar otomatik eklenir)
          </p>
          <div
            class="import-dropzone"
            @dragover.prevent
            @drop.prevent="dosyaSec($event, 'alisFatura')"
            @click="$refs.alisFaturaInput.click()"
          >
            <input
              ref="alisFaturaInput"
              type="file"
              accept=".csv"
              hidden
              @change="dosyaDegisti($event, 'alisFatura')"
            >
            <i class="pi pi-upload" />
            <span>{{ alisFaturaDosya ? alisFaturaDosya.name : 'CSV dosyasını seçin veya sürükleyin' }}</span>
          </div>
          <Button
            v-if="alisFaturaDosya"
            label="Aktar"
            icon="pi pi-upload"
            class="p-button-success w-full"
            :loading="alisFaturaYukleniyor"
            @click="aktar('alisFatura')"
          />
          <div
            v-if="alisFaturaSonuc"
            class="import-sonuc"
          >
            <Message
              :severity="alisFaturaSonuc.hatalar?.length ? 'warn' : 'success'"
              :closable="true"
            >
              <strong>{{ alisFaturaSonuc.basarili }} alış faturası aktarıldı.</strong>
              <span v-if="alisFaturaSonuc.hatalar?.length"> {{ alisFaturaSonuc.hatalar.length }} hata.</span>
            </Message>
            <ul
              v-if="alisFaturaSonuc.hatalar?.length"
              class="hata-listesi"
            >
              <li
                v-for="h in alisFaturaSonuc.hatalar"
                :key="h"
              >
                {{ h }}
              </li>
            </ul>
          </div>
        </template>
      </Card>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { importAPI } from '../api/index.js'

const toast = useToast()
const toastBildirim = useToastBildirim()
const stokDosya = ref(null)
const cariDosya = ref(null)
const alisFaturaDosya = ref(null)
const stokYukleniyor = ref(false)
const cariYukleniyor = ref(false)
const alisFaturaYukleniyor = ref(false)
const stokSonuc = ref(null)
const cariSonuc = ref(null)
const alisFaturaSonuc = ref(null)

const dosyaDegisti = (e, tur) => {
  const file = e.target.files[0]
  if (file) { if (tur === 'stok') stokDosya.value = file; else if (tur === 'cari') cariDosya.value = file; else alisFaturaDosya.value = file }
}

const dosyaSec = (e, tur) => {
  const file = e.dataTransfer.files[0]
  if (file) { if (tur === 'stok') stokDosya.value = file; else if (tur === 'cari') cariDosya.value = file; else alisFaturaDosya.value = file }
}

const aktar = async (tur) => {
  const file = tur === 'stok' ? stokDosya.value : tur === 'cari' ? cariDosya.value : alisFaturaDosya.value
  if (!file) return
  const loading = tur === 'stok' ? stokYukleniyor : tur === 'cari' ? cariYukleniyor : alisFaturaYukleniyor
  const sonuc = tur === 'stok' ? stokSonuc : tur === 'cari' ? cariSonuc : alisFaturaSonuc
  loading.value = true
  sonuc.value = null
  try {
    const api = tur === 'stok' ? importAPI.stok : tur === 'cari' ? importAPI.cari : importAPI.alisFatura
    const res = await api(file)
    sonuc.value = res.data
    toast.add({ severity: res.data.hatalar?.length ? 'warn' : 'success', summary: 'İşlem Tamam', detail: res.data.mesaj, life: 5000 })
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Aktarma başarısız')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.import-page { padding: 1.5rem; }
.import-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; margin-top: 1.5rem; }
.import-desc { font-size: 0.85rem; color: var(--text-secondary); margin-bottom: 1rem; }
.import-desc code { background: rgba(148,163,184,0.1); padding: 2px 6px; border-radius: 4px; font-size: 0.8rem; }
.import-dropzone {
  border: 2px dashed var(--border); border-radius: 10px;
  padding: 2rem; text-align: center; cursor: pointer;
  display: flex; flex-direction: column; align-items: center; gap: 8px;
  color: var(--text-muted); font-size: 0.85rem; transition: all 0.2s;
  margin-bottom: 1rem;
}
.import-dropzone:hover { border-color: var(--accent); color: var(--text-secondary); }
.import-dropzone i { font-size: 2rem; }
.w-full { width: 100% !important; }
.import-sonuc { margin-top: 1rem; }
.hata-listesi { margin: 0.5rem 0 0; padding-left: 1.2rem; font-size: 0.8rem; color: #f87171; max-height: 150px; overflow-y: auto; }
.hata-listesi li { margin-bottom: 2px; }
@media (max-width: 900px) { .import-grid { grid-template-columns: 1fr; } }
</style>
