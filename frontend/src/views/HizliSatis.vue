<template>
  <div class="pos-container">
    <div class="pos-header">
      <h1>Hızlı Satış</h1>
      <div class="pos-bilgi">
        <span class="pos-kayit">{{ dashboardStore.bugunkuSiparis || 0 }} bugün</span>
        <span class="pos-sepet-adet">{{ sepet.length }} ürün</span>
      </div>
    </div>

    <div class="pos-body">
      <div class="pos-sol">
        <div class="pos-taramakutusu">
          <i class="pi pi-qrcode"></i>
          <input ref="barkodInput" v-model="barkod" type="text" class="pos-input" placeholder="Barkod okut veya ürün ara..."
            @keydown.enter.prevent="barkodIleEkle" autofocus />
          <Button icon="pi pi-plus" class="p-button-sm p-button-success" @click="barkodIleEkle" />
        </div>

        <div class="pos-urunara">
          <Dropdown v-model="seciliUrun" :options="stokListesi" optionLabel="ad" placeholder="Ürün adı ile ara..."
            class="w-full" filter :filterFields="['ad','stokKodu','barkod']" @change="urunSec" />
        </div>

        <div class="pos-sepet" v-if="sepet.length > 0">
          <div v-for="(item, idx) in sepet" :key="idx" class="pos-sepet-item">
            <div class="pos-sepet-bilgi">
              <strong>{{ item.ad }}</strong>
              <small>{{ item.stokKodu }} / {{ item.birim }}</small>
            </div>
            <div class="pos-sepet-miktar">
              <Button icon="pi pi-minus" class="p-button-rounded p-button-text p-button-sm" @click="miktarAzalt(idx)" />
              <InputNumber v-model="item.miktar" :min="1" class="pos-adet-input" />
              <Button icon="pi pi-plus" class="p-button-rounded p-button-text p-button-sm" @click="miktarArtir(idx)" />
            </div>
            <div class="pos-sepet-fiyat">
              <InputNumber v-model="item.fiyat" :min-fraction-digits="2" class="pos-fiyat-input" />
            </div>
            <div class="pos-sepet-tutar">{{ formatCurrency(item.miktar * item.fiyat) }}</div>
            <Button icon="pi pi-trash" class="p-button-rounded p-button-text p-button-danger p-button-sm" @click="sepetSil(idx)" />
          </div>
        </div>
        <div v-else class="pos-bos">
          <i class="pi pi-shopping-cart"></i>
          <p>Barkod okutun veya ürün seçin</p>
        </div>
      </div>

      <div class="pos-sag">
        <div class="pos-toplam">
          <div class="pos-toplam-label">Toplam</div>
          <div class="pos-toplam-deger">{{ formatCurrency(genelToplam) }}</div>
        </div>
        <div class="pos-odeme">
          <div class="pos-odeme-grup">
            <label>Tahsilat Türü</label>
            <Dropdown v-model="odemeTuru" :options="odemeTurleri" placeholder="Ödeme türü" class="w-full" />
          </div>
          <div class="pos-odeme-grup" v-if="odemeTuru === 'NAKIT'">
            <label>Alınan Para</label>
            <InputNumber v-model="alinanPara" :min-fraction-digits="2" class="w-full" />
          </div>
          <div class="pos-ustu" v-if="paraUstu > 0">
            <span>Para Üstü</span>
            <strong class="positive">{{ formatCurrency(paraUstu) }}</strong>
          </div>
          <Button label="Satışı Tamamla" icon="pi pi-check" class="p-button-success pos-buton" @click="satisiTamamla"
            :loading="kaydediliyor" :disabled="sepet.length === 0" />
        </div>
      </div>
    </div>

    <Message v-if="hata" severity="error" :text="hata" closable @close="hata = ''" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useDashboardStore } from '../stores/dashboardStore.js'
import { stokAPI, faturaAPI } from '../api/index.js'
import { useRouter } from 'vue-router'

const toast = useToast()
const router = useRouter()
const dashboardStore = useDashboardStore()
const barkodInput = ref(null)
const barkod = ref('')
const sepet = ref([])
const kaydediliyor = ref(false)
const hata = ref('')
const odemeTuru = ref('NAKIT')
const alinanPara = ref(0)
const odemeTurleri = ['NAKIT', 'KREDI_KARTI', 'HAVALE', 'CEK']
const stokListesi = ref([])
const seciliUrun = ref(null)

const genelToplam = computed(() => sepet.value.reduce((t, i) => t + (i.miktar * i.fiyat), 0))
const paraUstu = computed(() => Math.max(0, (alinanPara.value || 0) - genelToplam.value))
const formatCurrency = (v) => { if (v == null) return '0,00 ₺'; return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(v) }

onMounted(async () => {
  try {
    const stokRes = await stokAPI.getAll(); stokListesi.value = stokRes.data.content || stokRes.data
    dashboardStore.getDashboardData()
  } catch (e) { hata.value = 'Stoklar yüklenemedi' }
  nextTick(() => barkodInput.value?.focus())
})

const barkodIleEkle = async () => {
  if (!barkod.value.trim()) return
  const q = barkod.value.trim()
  barkod.value = ''
  try {
    let urunler = (await stokAPI.ara(q)).data
    if (urunler.length === 0) { hata.value = `"${q}" eşleşmedi`; return }
    const urun = urunler[0]
    sepeteEkle(urun)
  } catch (e) { hata.value = 'Stok sorgulanamadı' }
}

const urunSec = () => {
  if (seciliUrun.value) { sepeteEkle(seciliUrun.value); seciliUrun.value = null }
  nextTick(() => barkodInput.value?.focus())
}

const sepeteEkle = (urun) => {
  hata.value = ''
  const varOlan = sepet.value.find(i => i.id === urun.id)
  if (varOlan) { varOlan.miktar++ } else {
    sepet.value.push({ id: urun.id, ad: urun.ad, stokKodu: urun.stokKodu, birim: urun.birim, miktar: 1, fiyat: urun.fiyat })
  }
}

const miktarArtir = (idx) => { sepet.value[idx].miktar++ }
const miktarAzalt = (idx) => {
  if (sepet.value[idx].miktar > 1) sepet.value[idx].miktar--
  else sepetSil(idx)
}
const sepetSil = (idx) => { sepet.value.splice(idx, 1) }

const satisiTamamla = async () => {
  if (sepet.value.length === 0) return
  kaydediliyor.value = true
  hata.value = ''
  try {
    const kalemler = sepet.value.map(i => ({
      stokId: i.id, aciklama: i.ad,
      adet: i.miktar, birimFiyat: i.fiyat,
      kdvOrani: 20, tutar: i.miktar * i.fiyat
    }))
    await faturaAPI.create({
      cariHesapId: null, tur: 'SATIS', durum: 'KESILDI',
      tarih: new Date().toISOString().split('T')[0],
      aciklama: `Hızlı Satış - ${odemeTuru.value}`,
      kalemler: kalemler
    })
    toast.add({ severity: 'success', summary: 'Başarılı', detail: `Satış tamamlandı - ${formatCurrency(genelToplam.value)}`, life: 5000 })
    sepet.value = []
    dashboardStore.getDashboardData()
  } catch (e) { hata.value = e?.response?.data?.message || 'Satış başarısız' }
  kaydediliyor.value = false
  nextTick(() => barkodInput.value?.focus())
}
</script>

<style scoped>
.pos-container { padding: 0; display: flex; flex-direction: column; height: calc(100vh - 120px); }
.pos-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.pos-header h1 { margin: 0; font-size: 24px; }
.pos-bilgi { display: flex; gap: 16px; font-size: 13px; color: var(--text-secondary); }
.pos-body { display: flex; gap: 20px; flex: 1; min-height: 0; }
.pos-sol { flex: 1; display: flex; flex-direction: column; gap: 12px; }
.pos-sag { width: 300px; display: flex; flex-direction: column; gap: 16px; }

.pos-taramakutusu { display: flex; gap: 8px; align-items: center; background: var(--bg-card); border: 2px solid var(--accent); border-radius: 12px; padding: 8px 16px; }
.pos-taramakutusu i { font-size: 24px; color: var(--accent); }
.pos-input { flex: 1; border: none; outline: none; background: transparent; color: var(--text-primary); font-size: 20px; font-family: 'Courier New', monospace; letter-spacing: 2px; }
.pos-input::placeholder { font-size: 14px; font-family: inherit; letter-spacing: 0; color: #475569; }

.pos-sepet { flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 4px; }
.pos-sepet-item { display: flex; align-items: center; gap: 8px; background: var(--bg-card); padding: 8px 12px; border-radius: 10px; border: 1px solid var(--border); }
.pos-sepet-bilgi { flex: 1; min-width: 0; }
.pos-sepet-bilgi strong { display: block; font-size: 14px; }
.pos-sepet-bilgi small { font-size: 11px; color: var(--text-muted); }
.pos-sepet-miktar { display: flex; align-items: center; gap: 4px; }
.pos-adet-input { width: 60px; }
.pos-adet-input :deep(.p-inputnumber-input) { text-align: center; font-size: 14px; width: 60px; }
.pos-fiyat-input { width: 100px; }
.pos-fiyat-input :deep(.p-inputnumber-input) { text-align: right; font-size: 14px; }
.pos-sepet-tutar { width: 100px; text-align: right; font-weight: 700; font-size: 15px; }

.pos-toplam { text-align: center; background: var(--bg-card); padding: 24px; border-radius: 14px; border: 2px solid var(--accent); }
.pos-toplam-label { font-size: 14px; color: var(--text-secondary); text-transform: uppercase; }
.pos-toplam-deger { font-size: 36px; font-weight: 800; color: var(--accent); margin-top: 4px; }

.pos-odeme { background: var(--bg-card); padding: 20px; border-radius: 14px; border: 1px solid var(--border); display: flex; flex-direction: column; gap: 12px; }
.pos-odeme-grup label { display: block; font-size: 12px; color: var(--text-secondary); margin-bottom: 4px; }
.pos-ustu { display: flex; justify-content: space-between; align-items: center; background: rgba(76,175,80,0.1); padding: 10px; border-radius: 8px; }
.pos-ustu span { font-size: 13px; color: var(--text-secondary); }
.pos-ustu strong { font-size: 20px; }
.pos-buton { margin-top: 8px; width: 100%; }
.pos-bos { text-align: center; padding: 60px; color: var(--text-muted); }
.pos-bos i { font-size: 48px; display: block; margin-bottom: 12px; }
.pos-urunara { margin-top: -4px; }
.pos-urunara :deep(.p-dropdown) { width: 100%; }
</style>
