<template>
  <div class="yonetici-kokpiti-sayfasi">
    <!-- Üst Başlık ve Filtreleme -->
    <div class="kokpit-header flex justify-between items-center flex-wrap gap-4 mb-4">
      <div>
        <h1 class="page-title text-xl font-bold">
          <i class="pi pi-bolt text-amber-500 mr-2" />
          Yönetici & Finansal Nabız Kokpiti
        </h1>
        <p class="text-xs text-muted">
          Aylık ciro ve kâr hedefleri, likidite durumu, en değerli müşteriler ve acil tahsilat sinyalleri.
        </p>
      </div>

      <div class="flex items-center gap-2">
        <Dropdown
          v-model="seciliAy"
          :options="aySecenekleri"
          option-label="ad"
          option-value="deger"
          class="w-32 p-inputtext-sm"
          @change="verileriYukle"
        />
        <Dropdown
          v-model="seciliYil"
          :options="yilSecenekleri"
          class="w-24 p-inputtext-sm"
          @change="verileriYukle"
        />
        <Button
          label="Hedef Belirle"
          icon="pi pi-bullseye"
          class="p-button-warning p-button-sm"
          @click="hedefModalAc"
        />
        <Button
          icon="pi pi-refresh"
          class="p-button-outlined p-button-sm"
          :loading="yukleniyor"
          @click="verileriYukle"
        />
      </div>
    </div>

    <!-- 1. TEMEL 4 FİNANSAL KPI KARTI -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-5">
      <!-- Ciro & Hedef -->
      <div class="kpi-kart kart-ciro">
        <div class="flex justify-between items-start mb-2">
          <span class="kpi-baslik">Aylık Ciro Gerçekleşme</span>
          <span
            class="kpi-rozet"
            :style="{ background: progressRenk + '20', color: progressRenk }"
          >
            %{{ (kokpit?.ciroIlerlemeYuzdesi || 0).toFixed(1) }}
          </span>
        </div>
        <div class="kpi-deger text-primary">
          {{ formatCurrency(kokpit?.gerceklesenCiro || 0) }}
        </div>
        <div class="w-full bg-gray-200 dark:bg-gray-700 h-1.5 rounded-full overflow-hidden my-2">
          <div
            class="h-full rounded-full transition-all duration-500"
            :style="{ width: Math.min(100, kokpit?.ciroIlerlemeYuzdesi || 0) + '%', backgroundColor: progressRenk }"
          />
        </div>
        <div class="flex justify-between text-xs text-muted">
          <span>Hedef: {{ formatCurrency(kokpit?.hedefCiro || 0) }}</span>
          <span>{{ kokpit?.kalanGun || 0 }} gün kaldı</span>
        </div>
      </div>

      <!-- Net Kâr -->
      <div class="kpi-kart kart-kar">
        <div class="flex justify-between items-start mb-2">
          <span class="kpi-baslik">Dönem Net Kârı</span>
          <span class="kpi-rozet bg-emerald-100 text-emerald-700 dark:bg-emerald-950/60 dark:text-emerald-300">
            %{{ (kokpit?.netKarMarji || 0).toFixed(1) }} Marj
          </span>
        </div>
        <div class="kpi-deger text-emerald-600 dark:text-emerald-400">
          {{ formatCurrency(kokpit?.gerceklesenKar || 0) }}
        </div>
        <div class="kpi-alt-bilgi mt-3 text-xs text-muted flex justify-between">
          <span>Alış: {{ formatCurrency(kokpit?.toplamAlisMaliyeti || 0) }}</span>
          <span>Gider: {{ formatCurrency(kokpit?.toplamMasraflar || 0) }}</span>
        </div>
      </div>

      <!-- Likidite -->
      <div class="kpi-kart kart-nakit">
        <div class="flex justify-between items-start mb-2">
          <span class="kpi-baslik">Kasa & Banka Likidite</span>
          <i class="pi pi-wallet text-blue-500 text-lg" />
        </div>
        <div class="kpi-deger text-gray-900 dark:text-gray-100">
          {{ formatCurrency(kokpit?.kasaBankaToplam || 0) }}
        </div>
        <div class="kpi-alt-bilgi mt-3 text-xs text-muted flex justify-between">
          <span>Piyasa Alacağı:</span>
          <strong class="text-blue-600">{{ formatCurrency(kokpit?.toplamAlacak || 0) }}</strong>
        </div>
      </div>

      <!-- Riskli Alacaklar -->
      <div class="kpi-kart kart-risk">
        <div class="flex justify-between items-start mb-2">
          <span class="kpi-baslik text-red-600 dark:text-red-400 font-semibold">Geciken Riskli Alacak</span>
          <span class="kpi-rozet bg-red-100 text-red-700 dark:bg-red-950/60 dark:text-red-300">
            {{ kokpit?.kritikAlacaklar?.length || 0 }} Fatura
          </span>
        </div>
        <div class="kpi-deger text-red-600">
          {{ formatCurrency(kokpit?.vadesiGecenAlacak || 0) }}
        </div>
        <div class="kpi-alt-bilgi mt-3 text-xs text-muted flex justify-between">
          <span>Tedarikçi Borcu:</span>
          <span>{{ formatCurrency(kokpit?.toplamBorc || 0) }}</span>
        </div>
      </div>
    </div>

    <!-- 2. GÜNLÜK SATIŞ TRENDİ GRAFİĞİ -->
    <div class="kart-kutu p-4 mb-5">
      <div class="flex justify-between items-center mb-3">
        <h3 class="text-sm font-bold text-gray-800 dark:text-gray-100 flex items-center gap-2">
          <i class="pi pi-chart-bar text-primary" /> Günlük Ciro Gerçekleşme Trendi (Aylık Dağılım)
        </h3>
        <span class="text-xs text-muted">Aylık Toplam: {{ formatCurrency(kokpit?.gerceklesenCiro || 0) }}</span>
      </div>
      <div
        v-if="trendVerisi.labels && trendVerisi.labels.length > 0"
        style="height: 240px;"
      >
        <Bar
          :data="trendVerisi"
          :options="chartOptions"
        />
      </div>
      <div
        v-else
        class="h-40 flex items-center justify-center text-muted text-xs"
      >
        Bu ay için satış hareketi bulunamadı.
      </div>
    </div>

    <!-- 3. ALT 2 DETAY PANELİ: TOP MÜŞTERİLER & VADESİ GEÇENLER -->
    <!-- 3. ALT 2 DETAY PANELİ: TOP MÜŞTERİLER & VADESİ GEÇENLER -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-5 mb-5">
      <!-- En Çok Kazandıran 5 Müşteri -->
      <div class="kart-kutu p-4">
        <div class="flex justify-between items-center mb-3 pb-2 border-b">
          <h3 class="text-sm font-bold text-gray-800 dark:text-gray-100 flex items-center gap-2">
            <i class="pi pi-trophy text-amber-500" /> En İyi 5 Müşteri
          </h3>
          <span class="text-xs text-muted">Ciro Payı</span>
        </div>
        <div
          v-if="kokpit?.topMusteriler?.length > 0"
          class="space-y-2.5"
        >
          <div
            v-for="(m, idx) in kokpit.topMusteriler"
            :key="idx"
            class="flex items-center justify-between p-2.5 rounded-lg bg-gray-50 dark:bg-gray-800/60 border border-gray-100 dark:border-gray-700/50"
          >
            <div class="flex items-center gap-3">
              <span class="musteri-sira font-black text-xs w-5 text-center text-muted">{{ idx + 1 }}</span>
              <div>
                <span class="font-bold text-xs text-gray-800 dark:text-gray-100 block">{{ m.cariAdi || m.unvan }}</span>
                <span class="text-xs text-muted">{{ m.faturaSayisi }} Fatura Kesildi</span>
              </div>
            </div>
            <div class="text-right font-bold text-sm text-primary">
              {{ formatCurrency(m.toplamCiro) }}
            </div>
          </div>
        </div>
        <div
          v-else
          class="py-8 text-center text-muted text-xs"
        >
          Henüz müşteri satış verisi bulunmuyor.
        </div>
      </div>

      <!-- Vadesi Geçen Kritik Alacaklar -->
      <div class="kart-kutu p-4">
        <div class="flex justify-between items-center mb-3 pb-2 border-b">
          <h3 class="text-sm font-bold text-red-600 dark:text-red-400 flex items-center gap-2">
            <i class="pi pi-exclamation-circle" /> Vadesi Geçen Kritik Alacaklar
          </h3>
          <span class="text-xs text-muted">{{ kokpit?.kritikAlacaklar?.length || 0 }} Geciken Fatura</span>
        </div>
        <div
          v-if="kokpit?.kritikAlacaklar?.length > 0"
          class="space-y-2.5"
        >
          <div
            v-for="(a, idx) in kokpit.kritikAlacaklar"
            :key="idx"
            class="flex items-center justify-between p-2.5 rounded-lg bg-red-50/50 dark:bg-red-950/20 border border-red-100 dark:border-red-900/40"
          >
            <div>
              <span class="font-bold text-xs text-gray-900 dark:text-gray-100 block">{{ a.cariAdi || a.unvan }}</span>
              <span class="text-xs text-muted">
                Fatura: #{{ a.faturaNo || a.faturaNumarasi || a.id }} · Vade: {{ a.vadeTarihi }}
                <span class="text-red-600 font-bold ml-1">({{ a.gecikmeGunu }} gün gecikti)</span>
              </span>
            </div>
            <div class="flex items-center gap-2">
              <span class="font-bold text-sm text-red-600">{{ formatCurrency(a.kalanTutar || a.bakiye) }}</span>
              <Button
                v-if="a.telefon"
                icon="pi pi-whatsapp"
                class="p-button-rounded p-button-success p-button-text p-button-sm"
                title="WhatsApp Hatırlatması Gönder"
                @click="hatirlatWhatsApp(a)"
              />
            </div>
          </div>
        </div>
        <div
          v-else
          class="py-8 text-center text-emerald-600 text-xs"
        >
          <i class="pi pi-check-circle mr-1" /> Vadesi geçmiş riskli alacak bulunmuyor.
        </div>
      </div>
    </div>

    <!-- HEDEF BELİRLEME DIALOG -->
    <Dialog
      v-model:visible="hedefModal"
      :modal="true"
      header="Aylık Şirket Hedeflerini Belirle"
      :style="{ width: '450px', maxWidth: '95vw' }"
    >
      <div class="p-fluid space-y-3">
        <div>
          <label class="form-label font-semibold">Hedef Dönemi</label>
          <div class="text-sm font-bold text-primary">
            {{ seciliYil }} - {{ ayAdi(seciliAy) }}
          </div>
        </div>
        <div>
          <label class="form-label font-semibold">Aylık Ciro Hedefi (₺) *</label>
          <input
            v-model.number="hedefForm.hedefCiro"
            type="number"
            min="0"
            step="1000"
            class="p-inputtext w-full"
            placeholder="Örn: 1000000"
          >
        </div>
        <div>
          <label class="form-label font-semibold">Aylık Net Kâr Hedefi (₺)</label>
          <input
            v-model.number="hedefForm.hedefKar"
            type="number"
            min="0"
            step="1000"
            class="p-inputtext w-full"
            placeholder="Örn: 250000"
          >
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="form-label font-semibold">Yeni Müşteri Hedefi</label>
            <input
              v-model.number="hedefForm.hedefYeniMusteri"
              type="number"
              min="0"
              class="p-inputtext w-full"
              placeholder="Örn: 10"
            >
          </div>
          <div>
            <label class="form-label font-semibold">Satış Adedi Hedefi</label>
            <input
              v-model.number="hedefForm.hedefSatisAdedi"
              type="number"
              min="0"
              class="p-inputtext w-full"
              placeholder="Örn: 150"
            >
          </div>
        </div>
        <div>
          <label class="form-label font-semibold">Aylık Strateji / Notlar</label>
          <Textarea
            v-model="hedefForm.notlar"
            rows="2"
            placeholder="Bu ayki odak noktaları ve hedefler..."
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          label="İptal"
          icon="pi pi-times"
          class="p-button-text"
          @click="hedefModal = false"
        />
        <Button
          label="Hedefleri Kaydet"
          icon="pi pi-check"
          class="p-button-primary"
          :loading="hedefKaydediliyor"
          @click="hedefKaydet"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { yoneticiKokpitAPI } from '../api/index.js'
import { formatCurrency } from '../utils/format.js'
import { useToast } from 'primevue/usetoast'
import { Bar } from 'vue-chartjs'
import {
  Chart as ChartJS,
  Title,
  Tooltip,
  Legend,
  BarElement,
  CategoryScale,
  LinearScale
} from 'chart.js'

ChartJS.register(Title, Tooltip, Legend, BarElement, CategoryScale, LinearScale)

const toast = useToast()

const kokpit = ref(null)
const yukleniyor = ref(false)
const hedefModal = ref(false)
const hedefKaydediliyor = ref(false)

const bugun = new Date()
const seciliYil = ref(bugun.getFullYear())
const seciliAy = ref(bugun.getMonth() + 1)

const yilSecenekleri = [2024, 2025, 2026, 2027]
const aySecenekleri = [
  { ad: 'Ocak', deger: 1 }, { ad: 'Şubat', deger: 2 }, { ad: 'Mart', deger: 3 },
  { ad: 'Nisan', deger: 4 }, { ad: 'Mayıs', deger: 5 }, { ad: 'Haziran', deger: 6 },
  { ad: 'Temmuz', deger: 7 }, { ad: 'Ağustos', deger: 8 }, { ad: 'Eylül', deger: 9 },
  { ad: 'Ekim', deger: 10 }, { ad: 'Kasım', deger: 11 }, { ad: 'Aralık', deger: 12 }
]

const hedefForm = ref({
  hedefCiro: 0,
  hedefKar: 0,
  hedefYeniMusteri: 0,
  hedefSatisAdedi: 0,
  notlar: ''
})

onMounted(async () => {
  await verileriYukle()
})

const verileriYukle = async () => {
  yukleniyor.value = true
  try {
    const res = await yoneticiKokpitAPI.getVeriler({
      yil: seciliYil.value,
      ay: seciliAy.value
    })
    kokpit.value = res.data || null
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: 'Kokpit verileri alınamadı: ' + err.message, life: 3000 })
  } finally {
    yukleniyor.value = false
  }
}

const ayAdi = (ay) => {
  return aySecenekleri.find(a => a.deger === ay)?.ad || ay
}

const progressRenk = computed(() => {
  const y = kokpit.value?.ciroIlerlemeYuzdesi || 0
  if (y >= 100) return '#10b981' // Green
  if (y >= 70) return '#3b82f6'  // Blue
  if (y >= 40) return '#f59e0b'  // Amber
  return '#ef4444'              // Red
})

// Bar Chart Verisi
const trendVerisi = computed(() => {
  const trend = kokpit.value?.gunlukCiroTrendi || []
  return {
    labels: trend.map(t => t.tarih),
    datasets: [
      {
        label: 'Günlük Ciro (₺)',
        backgroundColor: '#3b82f6',
        borderRadius: 4,
        data: trend.map(t => t.ciro)
      }
    ]
  }
})

const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false }
  },
  scales: {
    y: {
      beginAtZero: true,
      grid: { color: 'rgba(0,0,0,0.05)' }
    },
    x: {
      grid: { display: false }
    }
  }
}

const hedefModalAc = () => {
  hedefForm.value = {
    hedefCiro: kokpit.value?.hedefCiro || 0,
    hedefKar: kokpit.value?.hedefKar || 0,
    hedefYeniMusteri: kokpit.value?.hedefYeniMusteri || 0,
    hedefSatisAdedi: kokpit.value?.hedefSatisAdedi || 0,
    notlar: ''
  }
  hedefModal.value = true
}

const hedefKaydet = async () => {
  hedefKaydediliyor.value = true
  try {
    await yoneticiKokpitAPI.hedefKaydet({
      yil: seciliYil.value,
      ay: seciliAy.value,
      ...hedefForm.value
    })
    toast.add({ severity: 'success', summary: 'Hedef Kaydedildi', detail: 'Aylık şirket hedefleri güncellendi.', life: 3000 })
    hedefModal.value = false
    await verileriYukle()
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err.message, life: 3000 })
  } finally {
    hedefKaydediliyor.value = false
  }
}

const hatirlatWhatsApp = (cari) => {
  const metin = `Sayın ${cari.unvan},\n\nVadesi geçen ${formatCurrency(cari.bakiye)} tutarındaki faturanızın tahsilatını rica eder, iyi çalışmalar dileriz.`
  window.open(`https://wa.me/${cari.telefon?.replace(/[^0-9]/g, '')}?text=${encodeURIComponent(metin)}`, '_blank')
}
</script>

<style scoped>
.kart-kutu {
  background: var(--bg-card);
  border-radius: 12px;
  border: 1px solid var(--border);
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
}

.kpi-kart {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px 18px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
}

.kpi-baslik {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
}

.kpi-rozet {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 10px;
}

.kpi-deger {
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -0.5px;
}

.musteri-sira {
  color: var(--text-secondary);
}

.badge-gecikme {
  background: #fee2e2;
  color: #b91c1c;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 700;
}
</style>
