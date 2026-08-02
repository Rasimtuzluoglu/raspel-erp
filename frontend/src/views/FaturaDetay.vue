<template>
  <div class="fatura-detay" :class="{ 'print-mode': printMode }">
    <div class="detay-header">
      <Button label="Geri" icon="pi pi-arrow-left" @click="$router.push('/faturalar')" class="p-button-text no-print" />
      <div class="detay-ayarlar no-print">
        <SelectButton v-model="faturaFiyatli" :options="fiyatSecenekleri" optionLabel="label" optionValue="value" size="small" />
        <Button label="Yazdır" icon="pi pi-print" @click="win.print()" />
      </div>
    </div>

    <div v-if="loading" class="loading"><p><i class="pi pi-spin pi-spinner"></i> Yükleniyor...</p></div>

    <div v-if="fatura && !loading" class="fatura-kagit">
      <div class="fatura-baslik">
        <div class="firma-bilgi">
          <h2>ÖN MUHASEBE</h2>
          <p>Örnek Şirket Ltd. Şti.</p>
          <p>Vergi Dairesi: Kadıköy V.D.</p>
          <p>Vergi No: 1234567890</p>
        </div>
        <div class="fatura-bilgi">
          <h1>{{ fatura.tur === 'SATIS' ? 'SATIŞ FATURASI' : 'ALIŞ FATURASI' }}</h1>
          <p><strong>Fatura No:</strong> {{ fatura.faturaNumarasi }}</p>
          <p><strong>Tarih:</strong> {{ formatDate(fatura.tarih) }}</p>
          <p><strong>Durum:</strong> <span :class="['durum-badge', fatura.durum.toLowerCase()]">{{ durumLabel(fatura.durum) }}</span></p>
        </div>
      </div>

      <div class="cari-bilgi" v-if="fatura.cariHesapAd">
        <h3>Müşteri / Tedarikçi Bilgisi</h3>
        <p><strong>{{ fatura.cariHesapAd }}</strong></p>
      </div>

      <table class="fatura-tablo">
        <thead>
          <tr>
            <th>#</th>
            <th>Açıklama</th>
            <th>Adet</th>
            <th v-if="faturaFiyatli">Birim Fiyat</th>
            <th v-if="faturaFiyatli">İskonto %</th>
            <th v-if="faturaFiyatli">KDV %</th>
            <th v-if="faturaFiyatli">Toplam</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(k, i) in fatura.kalemler" :key="k.id">
            <td>{{ i + 1 }}</td>
            <td>{{ k.aciklama }}</td>
            <td>{{ k.adet }}</td>
            <td v-if="faturaFiyatli">{{ formatCurrency(k.birimFiyat) }}</td>
            <td v-if="faturaFiyatli">{{ k.iskontoOrani || 0 }}%</td>
            <td v-if="faturaFiyatli">{{ k.kdvOrani }}%</td>
            <td v-if="faturaFiyatli" class="text-right">{{ formatCurrency(k.tutar) }}</td>
          </tr>
        </tbody>
      </table>

      <div class="fatura-ozet" v-if="faturaFiyatli">
        <div class="ozet-row"><span>Ara Toplam:</span><span>{{ formatCurrency(fatura.araToplam) }}</span></div>
        <div class="ozet-row"><span>KDV Toplam:</span><span>{{ formatCurrency(fatura.kdv) }}</span></div>
        <div class="ozet-row" v-if="fatura.genelIskontoTutari > 0"><span>Genel İskonto:</span><span class="negative">-{{ formatCurrency(fatura.genelIskontoTutari) }}</span></div>
        <div class="ozet-row total"><span>Genel Toplam:</span><span>{{ formatCurrency(fatura.genelToplam) }}</span></div>
        <div class="ozet-row odeme" v-if="fatura.odemeDurumu"><span>Ödeme Durumu:</span><span :class="fatura.odemeDurumu === 'ODENDI' ? 'positive' : 'negative'">{{ odemeDurumLabel(fatura.odemeDurumu) }}</span></div>
        <div class="ozet-row odeme" v-if="fatura.odenenTutar > 0"><span>Ödenen:</span><span>{{ formatCurrency(fatura.odenenTutar) }}</span></div>
        <div class="ozet-row odeme" v-if="fatura.kalanTutar > 0"><span>Kalan:</span><span class="negative">{{ formatCurrency(fatura.kalanTutar) }}</span></div>
      </div>

      <div class="fatura-yazi" v-if="fatura.aciklama">
        <p><strong>Açıklama:</strong> {{ fatura.aciklama }}</p>
      </div>

      <div class="fatura-alt">
        <p>Oluşturma: {{ formatDateTime(fatura.olusturmaTarihi) }}</p>
      </div>
    </div>

    <div v-if="error && !loading" class="error-card">
      <Message severity="error" :text="error" />
      <Button label="Faturalara Dön" icon="pi pi-arrow-left" @click="$router.push('/faturalar')" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useFaturaStore } from '../stores/faturaStore.js'
import SelectButton from 'primevue/selectbutton'
import { useYakinZamanda } from '../composables/useYakinZamanda.js'

const route = useRoute()
const faturaStore = useFaturaStore()
const win = window

const fatura = ref(null)
const loading = ref(true)
const error = ref(null)
const printMode = ref(route.query.print === 'true')

const faturaFiyatli = ref(true)
const fiyatSecenekleri = ref([
  { label: 'Fiyatlı', value: true },
  { label: 'Fiyatsız', value: false }
])

onMounted(async () => {
  try {
    fatura.value = await faturaStore.getFaturaById(route.params.id)
    useYakinZamanda().kaydet('fatura', fatura.value.id, `#${fatura.value.faturaNumarasi || fatura.value.id}`, fatura.value.cariHesapAd)
  } catch (err) {
    error.value = err.response?.data?.message || 'Fatura bulunamadı'
  } finally {
    loading.value = false
  }
  if (printMode.value) setTimeout(() => { try { win.focus(); win.print() } catch (e) { console.error('Yazdırma hatası:', e) } }, 300)
})

const kdvTutari = (k) => {
  const brf = k.birimFiyat || 0
  const adt = k.adet || 0
  return (brf * adt) * ((k.kdvOrani || 0) / 100)
}

const durumLabel = (d) => ({ TASLAK: 'Taslak', TEKLIF: 'Teklif', KESILDI: 'Kesildi', IPTAL: 'İptal' })[d] || d
const odemeDurumLabel = (d) => ({ ODENMEDI: 'Ödenmedi', KISMI_ODENDI: 'Kısmi Ödendi', ODENDI: 'Ödendi' })[d] || d

const formatCurrency = (v) => {
  if (v === null || v === undefined) return '0,00 ₺'
  return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(v)
}

const formatDate = (d) => {
  if (!d) return '-'
  return new Intl.DateTimeFormat('tr-TR', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(d))
}

const formatDateTime = (d) => {
  if (!d) return '-'
  return new Intl.DateTimeFormat('tr-TR', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(d))
}
</script>

<style scoped>
.fatura-detay { padding: 20px; }
.detay-header { display: flex; justify-content: space-between; margin-bottom: 20px; align-items: center; gap: 8px; flex-wrap: wrap; }
.detay-ayarlar { display: flex; align-items: center; gap: 8px; }
.loading { text-align: center; padding: 60px; color: #666; }
.fatura-kagit { background: var(--bg-card); padding: 40px; border: 1px solid var(--border); border-radius: 14px; max-width: 800px; margin: 0 auto; }
.fatura-baslik { display: flex; justify-content: space-between; margin-bottom: 30px; border-bottom: 2px solid #1976d2; padding-bottom: 20px; }
.fatura-baslik h1 { color: #1976d2; font-size: 22px; margin: 0 0 10px 0; }
.fatura-baslik h2 { color: #333; margin: 0 0 10px 0; }
.firma-bilgi p, .fatura-bilgi p { margin: 3px 0; font-size: 13px; }
.cari-bilgi { background: #f5f5f5; padding: 15px; border-radius: 4px; margin-bottom: 20px; }
.cari-bilgi h3 { margin: 0 0 8px 0; font-size: 14px; color: #666; }
.cari-bilgi p { margin: 0; font-size: 16px; }
.fatura-tablo { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
.fatura-tablo th { background: #1976d2; color: white; padding: 10px; text-align: left; font-size: 13px; }
.fatura-tablo td { padding: 10px; border-bottom: 1px solid #eee; font-size: 13px; }
.fatura-tablo tr:nth-child(even) { background: #f9f9f9; }
.text-right { text-align: right; }
.fatura-ozet { margin-left: auto; width: 300px; background: #f8f9fa; padding: 15px; border-radius: 8px; }
.ozet-row { display: flex; justify-content: space-between; padding: 5px 0; font-size: 14px; }
.ozet-row.total { font-weight: bold; font-size: 18px; border-top: 2px solid #1976d2; margin-top: 5px; padding-top: 10px; }
.ozet-row.odeme { font-size: 13px; color: #666; }
.ozet-row .negative { color: #f44336; }
.ozet-row .positive { color: #4caf50; }
.fatura-yazi { margin-top: 20px; padding: 15px; background: #fff8e1; border-radius: 4px; }
.fatura-alt { margin-top: 30px; text-align: center; color: #999; font-size: 12px; border-top: 1px solid #eee; padding-top: 15px; }
.durum-badge { padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: bold; }
.durum-badge.taslak { background: #fff3e0; color: #e65100; }
.durum-badge.teklif { background: #e3f2fd; color: #1565c0; }
.durum-badge.kesildi { background: #e8f5e9; color: #2e7d32; }
.durum-badge.iptal { background: #f5f5f5; color: #9e9e9e; }
.error-card { text-align: center; padding: 60px; }

@media print {
  .no-print { display: none !important; }
  body * { visibility: hidden; }
  .fatura-kagit, .fatura-kagit * { visibility: visible; }
  .fatura-kagit {
    position: absolute; left: 0; top: 0;
    box-shadow: none; padding: 20px; max-width: 100%;
    border: none; border-radius: 0;
  }
  .fatura-detay { padding: 0; background: white; }
  .durum-badge.kesildi { border: 1px solid #2e7d32; }
}
</style>
