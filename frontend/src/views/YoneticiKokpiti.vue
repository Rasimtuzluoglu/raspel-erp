<template>
  <div class="yonetici-kokpiti-sayfasi">
    <!-- Üst Başlık ve Filtreleme -->
    <div class="kokpit-header-card">
      <div class="kokpit-header-content">
        <div>
          <h1 class="page-title">
            <i class="pi pi-bolt text-amber-400 mr-2" />
            Yönetici & Finansal Nabız Kokpiti
          </h1>
          <p class="page-subtitle">
            Aylık ciro & kâr hedefleri, nakit likiditesi, gelir-gider dağılımı ve riskli alacak analizi.
          </p>
        </div>

        <div class="filter-controls">
          <Dropdown
            v-model="seciliAy"
            :options="aySecenekleri"
            option-label="ad"
            option-value="deger"
            class="filter-dropdown ay-select"
            @change="verileriYukle"
          />
          <Dropdown
            v-model="seciliYil"
            :options="yilSecenekleri"
            class="filter-dropdown yil-select"
            @change="verileriYukle"
          />
          <Button
            label="Hedef Belirle"
            icon="pi pi-bullseye"
            class="p-button-warning p-button-sm font-semibold"
            @click="hedefModalAc"
          />
          <Button
            icon="pi pi-refresh"
            class="p-button-outlined p-button-sm text-white"
            :loading="yukleniyor"
            @click="verileriYukle"
          />
        </div>
      </div>
    </div>

    <!-- 1. TEMEL 4 FİNANSAL KPI KARTI -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-5">
      <!-- Ciro & Hedef -->
      <div class="kpi-card">
        <div class="kpi-card-header">
          <span class="kpi-card-title">Aylık Ciro Gerçekleşme</span>
          <span class="badge-pill" :style="{ backgroundColor: progressRenk + '20', color: progressRenk }">
            %{{ (kokpit?.ciroIlerlemeYuzdesi || 0).toFixed(1) }}
          </span>
        </div>
        <div class="kpi-card-value text-primary">
          {{ formatPara(kokpit?.gerceklesenCiro || 0) }}
        </div>
        <div class="progress-track my-2">
          <div
            class="progress-fill"
            :style="{ width: Math.min(100, kokpit?.ciroIlerlemeYuzdesi || 0) + '%', backgroundColor: progressRenk }"
          />
        </div>
        <div class="kpi-card-footer">
          <span>Hedef: {{ formatPara(kokpit?.hedefCiro || 0) }}</span>
          <span class="font-medium text-gray-500">{{ kokpit?.kalanGun || 0 }} gün kaldı</span>
        </div>
      </div>

      <!-- Net Kâr -->
      <div class="kpi-card">
        <div class="kpi-card-header">
          <span class="kpi-card-title">Dönem Net Kârı</span>
          <span class="badge-pill bg-emerald-50 text-emerald-600 dark:bg-emerald-950/40">
            %{{ (kokpit?.netKarMarji || 0).toFixed(1) }} Marj
          </span>
        </div>
        <div class="kpi-card-value text-emerald-600 dark:text-emerald-400">
          {{ formatPara(kokpit?.gerceklesenKar || 0) }}
        </div>
        <div class="kpi-card-footer mt-4 pt-2 border-t">
          <span>Maliyet: {{ formatPara(kokpit?.toplamAlisMaliyeti || 0) }}</span>
          <span>Gider: {{ formatPara(kokpit?.toplamMasraflar || 0) }}</span>
        </div>
      </div>

      <!-- Likidite -->
      <div class="kpi-card">
        <div class="kpi-card-header">
          <span class="kpi-card-title">Kasa & Banka Likidite</span>
          <div class="w-8 h-8 rounded-lg bg-blue-50 dark:bg-blue-900/30 flex items-center justify-center text-blue-600">
            <i class="pi pi-wallet" />
          </div>
        </div>
        <div class="kpi-card-value text-gray-900 dark:text-gray-100">
          {{ formatPara(kokpit?.kasaBankaToplam || 0) }}
        </div>
        <div class="kpi-card-footer mt-4 pt-2 border-t">
          <span>Piyasa Alacağı:</span>
          <strong class="text-blue-600">{{ formatPara(kokpit?.toplamAlacak || 0) }}</strong>
        </div>
      </div>

      <!-- Riskli Alacaklar -->
      <div class="kpi-card">
        <div class="kpi-card-header">
          <span class="kpi-card-title text-red-600 dark:text-red-400">Geciken Riskli Alacak</span>
          <span class="badge-pill bg-red-50 text-red-600 dark:bg-red-950/40">
            {{ kokpit?.kritikAlacaklar?.length || 0 }} Fatura
          </span>
        </div>
        <div class="kpi-card-value text-red-600">
          {{ formatPara(kokpit?.vadesiGecenAlacak || 0) }}
        </div>
        <div class="kpi-card-footer mt-4 pt-2 border-t">
          <span>Tedarikçi Borcu:</span>
          <span>{{ formatPara(kokpit?.toplamBorc || 0) }}</span>
        </div>
      </div>
    </div>

    <!-- 2. GRAFİK BÖLÜMÜ: 2 KOLON (SATIŞ TRENDİ + GELİR/GİDER DAĞILIMI) -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-5 mb-5">
      <!-- Günlük Satış Trendi (Bar Chart - 2 Kolon) -->
      <div class="section-card lg:col-span-2">
        <div class="section-card-header">
          <div>
            <h3 class="section-card-title">
              <i class="pi pi-chart-bar text-primary mr-2" />
              Günlük Ciro Gerçekleşme Trendi
            </h3>
            <p class="text-xs text-muted">Seçilen ay içerisindeki günlük satış dağılımı</p>
          </div>
          <span class="badge-pill bg-primary/10 text-primary font-bold">
            Toplam: {{ formatPara(kokpit?.gerceklesenCiro || 0) }}
          </span>
        </div>
        <div class="chart-container" style="height: 260px;">
          <Bar
            v-if="trendVerisi?.labels?.length"
            :data="trendVerisi"
            :options="barChartOptions"
          />
          <div v-else class="empty-chart">
            <i class="pi pi-chart-bar text-3xl text-gray-300 mb-2 block" />
            <span>Bu ay için henüz satış hareketi bulunamadı.</span>
          </div>
        </div>
      </div>

      <!-- Gelir & Gider Dağılımı (Doughnut Chart - 1 Kolon) -->
      <div class="section-card">
        <div class="section-card-header">
          <div>
            <h3 class="section-card-title">
              <i class="pi pi-chart-pie text-emerald-500 mr-2" />
              Finansal Dağılım
            </h3>
            <p class="text-xs text-muted">Ciro, Maliyet ve Gider Oranları</p>
          </div>
        </div>
        <div class="chart-container flex items-center justify-center" style="height: 260px;">
          <Doughnut
            v-if="dagilimVerisi?.datasets?.[0]?.data?.some(v => v > 0)"
            :data="dagilimVerisi"
            :options="doughnutOptions"
          />
          <div v-else class="empty-chart">
            <i class="pi pi-chart-pie text-3xl text-gray-300 mb-2 block" />
            <span>Finansal dağılım verisi henüz oluşmadı.</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 3. ALT 2 DETAY PANELİ: EN İYİ MÜŞTERİLER & VADESİ GEÇENLER -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-5 mb-5">
      <!-- En Çok Kazandıran 5 Müşteri -->
      <div class="section-card">
        <div class="section-card-header">
          <div>
            <h3 class="section-card-title">
              <i class="pi pi-trophy text-amber-500 mr-2" />
              En Çok Ciro Sağlayan 5 Müşteri
            </h3>
            <p class="text-xs text-muted">Bu ay en yüksek hacimli satış yapılan cari hesaplar</p>
          </div>
        </div>
        <div v-if="kokpit?.topMusteriler?.length" class="space-y-2.5">
          <div
            v-for="(m, idx) in kokpit.topMusteriler"
            :key="idx"
            class="customer-row"
          >
            <div class="flex items-center gap-3">
              <span class="rank-badge">{{ idx + 1 }}</span>
              <div>
                <strong class="text-sm text-gray-800 dark:text-gray-100 block">{{ m?.cariAdi || m?.unvan || 'Müşteri' }}</strong>
                <span class="text-xs text-muted">{{ m?.faturaSayisi || 0 }} Fatura</span>
              </div>
            </div>
            <div class="text-right font-bold text-sm text-primary">
              {{ formatPara(m?.toplamCiro || 0) }}
            </div>
          </div>
        </div>
        <div v-else class="empty-list">
          <i class="pi pi-users text-3xl text-gray-300 mb-2 block" />
          <span>Henüz müşteri satış verisi bulunmuyor.</span>
        </div>
      </div>

      <!-- Vadesi Geçen Kritik Alacaklar -->
      <div class="section-card">
        <div class="section-card-header">
          <div>
            <h3 class="section-card-title text-red-600 dark:text-red-400">
              <i class="pi pi-exclamation-circle mr-2" />
              Vadesi Geçen Kritik Alacaklar
            </h3>
            <p class="text-xs text-muted">Ödemesi geciken ve tahsilat takibi gereken müşteriler</p>
          </div>
          <span class="badge-pill bg-red-50 text-red-600 font-bold">
            {{ kokpit?.kritikAlacaklar?.length || 0 }} Fatura
          </span>
        </div>
        <div v-if="kokpit?.kritikAlacaklar?.length" class="space-y-2.5">
          <div
            v-for="(a, idx) in kokpit.kritikAlacaklar"
            :key="idx"
            class="risk-row"
          >
            <div>
              <strong class="text-sm text-gray-900 dark:text-gray-100 block">{{ a?.cariAdi || a?.unvan || 'Cari Hesap' }}</strong>
              <div class="text-xs text-muted flex items-center gap-2 mt-0.5">
                <span>Vade: {{ a?.vadeTarihi || '-' }}</span>
                <span class="text-red-600 font-bold">({{ a?.gecikmeGunu || 0 }} gün gecikti)</span>
              </div>
            </div>
            <div class="flex items-center gap-3">
              <span class="font-bold text-sm text-red-600">{{ formatPara(a?.kalanTutar || a?.bakiye || 0) }}</span>
              <Button
                v-if="a?.telefon"
                icon="pi pi-whatsapp"
                class="p-button-rounded p-button-success p-button-sm"
                title="WhatsApp Hatırlatması Gönder"
                @click="hatirlatWhatsApp(a)"
              />
            </div>
          </div>
        </div>
        <div v-else class="empty-list text-emerald-600">
          <i class="pi pi-check-circle text-3xl text-emerald-400 mb-2 block" />
          <span>Vadesi geçmiş riskli alacak kaydı bulunmuyor.</span>
        </div>
      </div>
    </div>

    <!-- HEDEF BELİRLEME MODALI -->
    <Dialog
      v-model:visible="hedefModal"
      :modal="true"
      header="Aylık Şirket Hedeflerini Belirle"
      :style="{ width: '90%', maxWidth: '480px' }"
    >
      <div class="space-y-4 pt-2">
        <div class="period-indicator">
          <span class="text-xs text-muted block">Hedef Dönemi</span>
          <strong class="text-base text-primary">{{ seciliYil }} - {{ ayAdi(seciliAy) }}</strong>
        </div>

        <div>
          <label class="block text-sm font-semibold mb-1 text-gray-700 dark:text-gray-300">Aylık Ciro Hedefi (₺) *</label>
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
          <label class="block text-sm font-semibold mb-1 text-gray-700 dark:text-gray-300">Aylık Net Kâr Hedefi (₺)</label>
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
            <label class="block text-sm font-semibold mb-1 text-gray-700 dark:text-gray-300">Yeni Müşteri Hedefi</label>
            <input
              v-model.number="hedefForm.hedefYeniMusteri"
              type="number"
              min="0"
              class="p-inputtext w-full"
              placeholder="Örn: 10"
            >
          </div>
          <div>
            <label class="block text-sm font-semibold mb-1 text-gray-700 dark:text-gray-300">Satış Adedi Hedefi</label>
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
          <label class="block text-sm font-semibold mb-1 text-gray-700 dark:text-gray-300">Aylık Strateji / Odak Notu</label>
          <Textarea
            v-model="hedefForm.notlar"
            rows="3"
            placeholder="Bu ayki stratejik hedefler ve odak noktaları..."
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          label="İptal"
          class="p-button-text"
          @click="hedefModal = false"
        />
        <Button
          label="Hedefleri Kaydet"
          icon="pi pi-check"
          class="p-button-primary font-bold"
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
import { useToast } from 'primevue/usetoast'
import { Bar, Doughnut } from 'vue-chartjs'
import {
  Chart as ChartJS,
  Title,
  Tooltip,
  Legend,
  BarElement,
  CategoryScale,
  LinearScale,
  ArcElement
} from 'chart.js'

ChartJS.register(Title, Tooltip, Legend, BarElement, CategoryScale, LinearScale, ArcElement)

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

// Güvenli formatlayıcılar
const formatPara = (v) => {
  if (v == null || isNaN(v)) return '0,00 ₺'
  return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(v)
}

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
    kokpit.value = res?.data || null
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: 'Kokpit verileri alınamadı: ' + (err?.message || ''), life: 3000 })
  } finally {
    yukleniyor.value = false
  }
}

const ayAdi = (ay) => {
  return aySecenekleri.find(a => a.deger === ay)?.ad || ay
}

const progressRenk = computed(() => {
  const y = kokpit.value?.ciroIlerlemeYuzdesi || 0
  if (y >= 100) return '#10b981'
  if (y >= 70) return '#3b82f6'
  if (y >= 40) return '#f59e0b'
  return '#ef4444'
})

// Bar Chart Verisi (Günlük Trend)
const trendVerisi = computed(() => {
  const trend = kokpit.value?.gunlukCiroTrendi || []
  return {
    labels: trend.map(t => t?.tarih || ''),
    datasets: [
      {
        label: 'Günlük Ciro (₺)',
        backgroundColor: '#3b82f6',
        borderRadius: 4,
        data: trend.map(t => t?.ciro || 0)
      }
    ]
  }
})

const barChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
    tooltip: {
      callbacks: {
        label: (ctx) => formatPara(ctx.raw || 0)
      }
    }
  },
  scales: {
    y: {
      beginAtZero: true,
      grid: { color: 'rgba(0,0,0,0.05)' },
      ticks: {
        callback: (v) => formatPara(v)
      }
    },
    x: {
      grid: { display: false }
    }
  }
}

// Doughnut Chart Verisi (Finansal Dağılım)
const dagilimVerisi = computed(() => {
  const maliyet = kokpit.value?.toplamAlisMaliyeti || 0
  const masraflar = kokpit.value?.toplamMasraflar || 0
  const netKar = kokpit.value?.gerceklesenKar || 0

  return {
    labels: ['Net Kâr', 'Alış Maliyeti', 'Genel Giderler'],
    datasets: [
      {
        data: [Math.max(0, netKar), maliyet, masraflar],
        backgroundColor: ['#10b981', '#f59e0b', '#ef4444'],
        borderWidth: 0
      }
    ]
  }
})

const doughnutOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      position: 'bottom',
      labels: { boxWidth: 12, font: { size: 11 } }
    },
    tooltip: {
      callbacks: {
        label: (ctx) => ` ${ctx.label}: ${formatPara(ctx.raw || 0)}`
      }
    }
  }
}

const hedefModalAc = () => {
  hedefForm.value = {
    hedefCiro: kokpit.value?.hedefCiro || 0,
    hedefKar: kokpit.value?.hedefKar || 0,
    hedefYeniMusteri: kokpit.value?.hedefYeniMusteri || 0,
    hedefSatisAdedi: kokpit.value?.hedefSatisAdedi || 0,
    notlar: kokpit.value?.notlar || ''
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
    toast.add({ severity: 'success', summary: 'Hedef Kaydedildi', detail: 'Aylık hedefler başarıyla güncellendi.', life: 3000 })
    hedefModal.value = false
    await verileriYukle()
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err?.message || 'Hedef kaydedilemedi.', life: 3000 })
  } finally {
    hedefKaydediliyor.value = false
  }
}

const hatirlatWhatsApp = (cari) => {
  if (!cari) return
  const metin = `Sayın ${cari.unvan || cari.cariAdi},\n\nVadesi geçen ${formatPara(cari.bakiye || cari.kalanTutar)} tutarındaki ödemenizi hatırlatır, iyi çalışmalar dileriz.`
  window.open(`https://wa.me/${(cari.telefon || '').replace(/[^0-9]/g, '')}?text=${encodeURIComponent(metin)}`, '_blank')
}
</script>

<style scoped>
.yonetici-kokpiti-sayfasi {
  padding-bottom: 40px;
}

.kokpit-header-card {
  background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
  color: white;
  padding: 1.25rem 1.5rem;
  border-radius: 1rem;
  margin-bottom: 1.25rem;
  box-shadow: 0 8px 20px -4px rgba(15, 23, 42, 0.4);
}

.kokpit-header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.page-title {
  font-size: 1.25rem;
  font-weight: 800;
  margin: 0 0 0.2rem 0;
  letter-spacing: -0.02em;
}

.page-subtitle {
  font-size: 0.8rem;
  color: #94a3b8;
  margin: 0;
}

.filter-controls {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.ay-select {
  width: 130px;
}

.yil-select {
  width: 100px;
}

.kpi-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 1rem;
  padding: 1.25rem;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
}

.kpi-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.kpi-card-title {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--text-secondary);
}

.badge-pill {
  font-size: 0.75rem;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 9999px;
}

.kpi-card-value {
  font-size: 1.5rem;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.progress-track {
  width: 100%;
  height: 6px;
  border-radius: 9999px;
  background: var(--bg-muted, rgba(0,0,0,0.06));
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 9999px;
  transition: width 0.4s ease;
}

.kpi-card-footer {
  display: flex;
  justify-content: space-between;
  font-size: 0.75rem;
  color: var(--text-secondary);
}

.section-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 1rem;
  padding: 1.25rem;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
}

.section-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid var(--border);
}

.section-card-title {
  font-size: 0.95rem;
  font-weight: 700;
  margin: 0;
  color: var(--text-primary);
}

.empty-chart, .empty-list {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 2rem;
  color: var(--text-secondary);
  font-size: 0.8rem;
  text-align: center;
}

.customer-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  background: var(--bg-muted, rgba(0,0,0,0.02));
  border: 1px solid var(--border);
  border-radius: 0.75rem;
}

.rank-badge {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
  font-size: 0.75rem;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
}

.risk-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  background: rgba(239, 68, 68, 0.04);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: 0.75rem;
}

.period-indicator {
  background: var(--bg-muted, rgba(0,0,0,0.03));
  padding: 0.75rem;
  border-radius: 0.5rem;
}
</style>
