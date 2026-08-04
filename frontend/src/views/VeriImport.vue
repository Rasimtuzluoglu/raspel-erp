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
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useToast } from 'primevue/usetoast'
import { importAPI } from '../api/index.js'

const toast = useToast()
const stokDosya = ref(null)
const cariDosya = ref(null)
const stokYukleniyor = ref(false)
const cariYukleniyor = ref(false)
const stokSonuc = ref(null)
const cariSonuc = ref(null)

const dosyaDegisti = (e, tur) => {
  const file = e.target.files[0]
  if (file) { if (tur === 'stok') stokDosya.value = file; else cariDosya.value = file }
}

const dosyaSec = (e, tur) => {
  const file = e.dataTransfer.files[0]
  if (file) { if (tur === 'stok') stokDosya.value = file; else cariDosya.value = file }
}

const aktar = async (tur) => {
  const file = tur === 'stok' ? stokDosya.value : cariDosya.value
  if (!file) return
  const loading = tur === 'stok' ? stokYukleniyor : cariYukleniyor
  const sonuc = tur === 'stok' ? stokSonuc : cariSonuc
  loading.value = true
  sonuc.value = null
  try {
    const api = tur === 'stok' ? importAPI.stok : importAPI.cari
    const res = await api(file)
    sonuc.value = res.data
    toast.add({ severity: res.data.hatalar?.length ? 'warn' : 'success', summary: 'İşlem Tamam', detail: res.data.mesaj, life: 5000 })
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || 'Aktarma başarısız', life: 5000 })
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
