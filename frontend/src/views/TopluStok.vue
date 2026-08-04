<template>
  <div class="toplu-stok-container">
    <h1>Toplu Stok İşlemleri</h1>

    <div class="islem-grid">
      <Card>
        <template #title>
          <i
            class="pi pi-file-import"
            style="margin-right:8px;color:#60a5fa"
          />CSV ile Stok Aktar
        </template>
        <template #content>
          <div class="csv-aciklama">
            <p>CSV dosyasından toplu stok girişi yapın. CSV formatı:</p>
            <pre>ad;stokKodu;barkod;birim;fiyat;miktar;minMiktar;stokGrubu;rafNo</pre>
            <a
              href="#"
              style="color:#60a5fa;font-size:13px"
              @click.prevent="ornekCsv"
            >Örnek CSV indir</a>
          </div>
          <div class="csv-upload">
            <input
              ref="fileInput"
              type="file"
              accept=".csv"
              style="display:none"
              @change="csvSec"
            >
            <Button
              label="CSV Seç"
              icon="pi pi-upload"
              class="p-button-outlined"
              @click="$refs.fileInput.click()"
            />
            <span
              v-if="seciliDosya"
              class="dosya-adi"
            >{{ seciliDosya }}</span>
          </div>
          <div
            v-if="csvVeri.length"
            class="csv-preview"
          >
            <h3>Önizleme ({{ csvVeri.length }} kayıt)</h3>
            <DataTable
              :value="csvVeri.slice(0, 5)"
              size="small"
              striped-rows
            >
              <Column header="Satır #">
                <template #body="s">
                  {{ s.index + 1 }}
                </template>
              </Column>
              <Column
                field="stokKodu"
                header="Kod"
              />
              <Column
                field="ad"
                header="Ad"
              />
              <Column
                field="barkod"
                header="Barkod"
              />
              <Column
                field="birim"
                header="Birim"
              />
              <Column
                field="fiyat"
                header="Fiyat"
              />
              <Column
                field="miktar"
                header="Miktar"
              />
            </DataTable>
            <small v-if="csvVeri.length > 5">...ve {{ csvVeri.length - 5 }} kayıt daha</small>
            <div class="csv-actions">
              <Button
                label="Tümünü Aktar"
                icon="pi pi-check"
                class="p-button-success"
                :loading="aktariyor"
                @click="csvAktar"
              />
              <Button
                label="İptal"
                icon="pi pi-times"
                class="p-button-text"
                @click="csvVeri = []; seciliDosya = ''"
              />
            </div>
          </div>
          <div
            v-if="sonuc"
            class="csv-sonuc"
          >
            <Message
              :severity="sonuc.hata === 0 ? 'success' : 'warn'"
              :closable="false"
            >
              <strong>{{ sonuc.basari }} başarılı</strong>
              <span v-if="sonuc.hata > 0">, {{ sonuc.hata }} hatalı</span>
            </Message>
          </div>
        </template>
      </Card>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useToast } from 'primevue/usetoast'
import { stokAPI } from '../api/index.js'

const toast = useToast()
const fileInput = ref(null)
const seciliDosya = ref('')
const csvVeri = ref([])
const aktariyor = ref(false)
const sonuc = ref(null)

const parseCSV = (text) => {
  const lines = text.trim().split('\n').map(l => l.trim()).filter(l => l)
  const header = lines[0].toLowerCase().split(';').map(h => h.trim())
  const satirlar = []
  for (let i = 1; i < lines.length; i++) {
    const cols = lines[i].split(';').map(c => c.trim())
    const obj = {}
    header.forEach((h, idx) => { obj[h] = cols[idx] || '' })
    obj.fiyat = parseFloat(obj.fiyat) || 0
    obj.miktar = parseFloat(obj.miktar) || 0
    obj.minMiktar = obj.minMiktar ? parseFloat(obj.minMiktar) : null
    satirlar.push(obj)
  }
  return satirlar
}

const csvSec = (e) => {
  const file = e.target.files[0]
  if (!file) return
  seciliDosya.value = file.name
  sonuc.value = null
  const reader = new FileReader()
  reader.onload = (ev) => {
    try { csvVeri.value = parseCSV(ev.target.result) } catch { toast.add({ severity: 'error', summary: 'Hata', detail: 'Dosya okunamadı', life: 5000 }) }
  }
  reader.readAsText(file, 'UTF-8')
}

const csvAktar = async () => {
  aktariyor.value = true
  let basari = 0, hata = 0
  for (const veri of csvVeri.value) {
    try {
      await stokAPI.create({
        stokKodu: veri.stokkodu || veri.stokKodu || '',
        barkod: veri.barkod || '',
        ad: veri.ad || veri.isim || '',
        birim: veri.birim || 'Adet',
        fiyat: veri.fiyat || 0,
        miktar: veri.miktar || 0,
        minMiktar: veri.minmiktar || veri.minMiktar || null,
        stokGrubu: veri.stokgrubu || veri.stokGrubu || '',
        rafNo: veri.rafno || veri.rafNo || '',
        aciklama: ''
      })
      basari++
    } catch { hata++ }
  }
  sonuc.value = { basari, hata }
  csvVeri.value = []
  seciliDosya.value = ''
  aktariyor.value = false
}

const ornekCsv = () => {
  const icerik = 'ad;stokKodu;barkod;birim;fiyat;miktar;minMiktar\nTest Urun;URN-001;8691234567890;Adet;100;50;10\nTest Urun 2;URN-002;;Kg;200;30;5'
  const blob = new Blob(['\uFEFF' + icerik], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = 'ornek_stok.csv'; a.click()
  URL.revokeObjectURL(url)
}
</script>

<style scoped>
.toplu-stok-container { padding: 0; }
h1 { font-size: 24px; margin-bottom: 24px; }
.islem-grid { max-width: 800px; }
.csv-aciklama { margin-bottom: 16px; }
.csv-aciklama pre { background: var(--bg-secondary); padding: 12px; border-radius: 8px; font-size: 12px; overflow-x: auto; }
.csv-upload { display: flex; align-items: center; gap: 12px; margin: 16px 0; }
.dosya-adi { font-size: 13px; color: var(--text-secondary); }
.csv-preview { margin-top: 16px; }
.csv-preview h3 { font-size: 15px; margin: 0 0 12px; }
.csv-preview small { display: block; margin-top: 8px; color: var(--text-muted); font-size: 12px; }
.csv-actions { margin-top: 16px; display: flex; gap: 8px; }
.csv-sonuc { margin-top: 16px; }
</style>
