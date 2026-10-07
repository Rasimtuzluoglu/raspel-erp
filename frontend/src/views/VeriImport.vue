<template>
  <div class="import-page">
    <PageHeader
      :title="t('veriImport.title')"
      :subtitle="t('veriImport.subtitle')"
    />

    <div class="import-grid">
      <Card>
        <template #title>
          <i
            class="pi pi-box"
            style="margin-right: 8px"
          />{{ t('veriImport.stokAktar') }}
        </template>
        <template #content>
          <p class="import-desc">
            {{ t('veriImport.stokDesc') }} <code>ad;stokKodu;barkod;birim;fiyat;miktar;minMiktar</code>
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
            <span>{{ stokDosya ? stokDosya.name : t('veriImport.csvSecin') }}</span>
          </div>
          <Button
            v-if="stokDosya"
            :label="t('veriImport.aktar')"
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
              <strong>{{ t('veriImport.stokAktarildi', { n: stokSonuc.basarili }) }}</strong>
              <span v-if="stokSonuc.hatalar?.length"> {{ t('veriImport.hataSayisi', { n: stokSonuc.hatalar.length }) }}</span>
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
            style="margin-right: 8px"
          />{{ t('veriImport.cariAktar') }}
        </template>
        <template #content>
          <p class="import-desc">
            {{ t('veriImport.cariDesc') }} <code>ad;vergiNo;telefon;eposta;il;ilce;adres</code>
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
            <span>{{ cariDosya ? cariDosya.name : t('veriImport.csvSecin') }}</span>
          </div>
          <Button
            v-if="cariDosya"
            :label="t('veriImport.aktar')"
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
              <strong>{{ t('veriImport.cariAktarildi', { n: cariSonuc.basarili }) }}</strong>
              <span v-if="cariSonuc.hatalar?.length"> {{ t('veriImport.hataSayisi', { n: cariSonuc.hatalar.length }) }}</span>
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
            style="margin-right: 8px"
          />{{ t('veriImport.alisFaturaAktar') }}
        </template>
        <template #content>
          <p class="import-desc">
            {{ t('veriImport.alisFaturaDesc') }}
            <code>faturaNo;tarih;cariId;stokKodu;aciklama;adet;birimFiyat;kdvOrani</code> {{ t('veriImport.alisFaturaNot') }}
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
            <span>{{ alisFaturaDosya ? alisFaturaDosya.name : t('veriImport.csvSecin') }}</span>
          </div>
          <Button
            v-if="alisFaturaDosya"
            :label="t('veriImport.aktar')"
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
              <strong>{{ t('veriImport.alisFaturaAktarildi', { n: alisFaturaSonuc.basarili }) }}</strong>
              <span v-if="alisFaturaSonuc.hatalar?.length"> {{ t('veriImport.hataSayisi', { n: alisFaturaSonuc.hatalar.length }) }}</span>
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

      <Card>
        <template #title>
          <i
            class="pi pi-money-bill"
            style="margin-right: 8px"
          />{{ t('veriImport.hareketAktar') }}
        </template>
        <template #content>
          <p class="import-desc">
            {{ t('veriImport.hareketDesc') }}
            <code>cariId;tarih;tur;tutar;aciklama</code> {{ t('veriImport.hareketNot') }}
          </p>
          <div
            class="import-dropzone"
            @dragover.prevent
            @drop.prevent="dosyaSec($event, 'hareket')"
            @click="$refs.hareketInput.click()"
          >
            <input
              ref="hareketInput"
              type="file"
              accept=".csv"
              hidden
              @change="dosyaDegisti($event, 'hareket')"
            >
            <i class="pi pi-upload" />
            <span>{{ hareketDosya ? hareketDosya.name : t('veriImport.csvSecin') }}</span>
          </div>
          <Button
            v-if="hareketDosya"
            :label="t('veriImport.aktar')"
            icon="pi pi-upload"
            class="p-button-success w-full"
            :loading="hareketYukleniyor"
            @click="aktar('hareket')"
          />
          <div
            v-if="hareketSonuc"
            class="import-sonuc"
          >
            <Message
              :severity="hareketSonuc.hatalar?.length ? 'warn' : 'success'"
              :closable="true"
            >
              <strong>{{ t('veriImport.hareketAktarildi', { n: hareketSonuc.basarili }) }}</strong>
              <span v-if="hareketSonuc.hatalar?.length"> {{ t('veriImport.hataSayisi', { n: hareketSonuc.hatalar.length }) }}</span>
            </Message>
            <ul
              v-if="hareketSonuc.hatalar?.length"
              class="hata-listesi"
            >
              <li
                v-for="h in hareketSonuc.hatalar"
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
import { useI18n } from 'vue-i18n'

const toast = useToast()
const toastBildirim = useToastBildirim()
const { t } = useI18n()
const stokDosya = ref(null)
const cariDosya = ref(null)
const alisFaturaDosya = ref(null)
const hareketDosya = ref(null)
const stokYukleniyor = ref(false)
const cariYukleniyor = ref(false)
const alisFaturaYukleniyor = ref(false)
const hareketYukleniyor = ref(false)
const stokSonuc = ref(null)
const cariSonuc = ref(null)
const alisFaturaSonuc = ref(null)
const hareketSonuc = ref(null)

// Faz 2.7: import türleri tek haritada toplanır (yeni tür eklemek kolay).
const dosyaRef = { stok: stokDosya, cari: cariDosya, alisFatura: alisFaturaDosya, hareket: hareketDosya }
const yukleniyorRef = { stok: stokYukleniyor, cari: cariYukleniyor, alisFatura: alisFaturaYukleniyor, hareket: hareketYukleniyor }
const sonucRef = { stok: stokSonuc, cari: cariSonuc, alisFatura: alisFaturaSonuc, hareket: hareketSonuc }
const apiRef = { stok: importAPI.stok, cari: importAPI.cari, alisFatura: importAPI.alisFatura, hareket: importAPI.hareket }

const dosyaDegisti = (e, tur) => {
  const file = e.target.files[0]
  if (file) dosyaRef[tur].value = file
}

const dosyaSec = (e, tur) => {
  const file = e.dataTransfer.files[0]
  if (file) dosyaRef[tur].value = file
}

const aktar = async (tur) => {
  const file = dosyaRef[tur].value
  if (!file) return
  const loading = yukleniyorRef[tur]
  const sonuc = sonucRef[tur]
  loading.value = true
  sonuc.value = null
  try {
    const res = await apiRef[tur](file)
    sonuc.value = res.data
    toast.add({
      severity: res.data.hatalar?.length ? 'warn' : 'success',
      summary: t('veriImport.islemTamam'),
      detail: res.data.mesaj,
      life: 5000
    })
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('veriImport.aktarmaBasarisiz'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.import-page {
  padding: 0;
  max-width: 100%;
}
.import-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1.5rem;
  margin-top: 1.5rem;
}
.import-desc {
  font-size: 0.85rem;
  color: var(--text-secondary);
  margin-bottom: 1rem;
}
.import-desc code {
  background: rgba(148, 163, 184, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.8rem;
}
.import-dropzone {
  border: 2px dashed var(--border);
  border-radius: 10px;
  padding: 2rem;
  text-align: center;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--text-muted);
  font-size: 0.85rem;
  transition: all 0.2s;
  margin-bottom: 1rem;
}
.import-dropzone:hover {
  border-color: var(--accent);
  color: var(--text-secondary);
}
.import-dropzone i {
  font-size: 2rem;
}
.w-full {
  width: 100% !important;
}
.import-sonuc {
  margin-top: 1rem;
}
.hata-listesi {
  margin: 0.5rem 0 0;
  padding-left: 1.2rem;
  font-size: 0.8rem;
  color: #f87171;
  max-height: 150px;
  overflow-y: auto;
}
.hata-listesi li {
  margin-bottom: 2px;
}
@media (max-width: 900px) {
  .import-grid {
    grid-template-columns: 1fr;
  }
}
</style>
