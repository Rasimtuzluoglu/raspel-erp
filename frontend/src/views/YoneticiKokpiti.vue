<template>
  <div class="yonetici-kokpiti-sayfasi">
    <!-- Üst Başlık ve Ay/Yıl Seçimi -->
    <div class="kokpit-header flex justify-between items-center flex-wrap gap-4 mb-4">
      <div>
        <h1 class="page-title">
          <i class="pi pi-bolt text-amber-500 mr-2" />
          Yönetici & Patron Canlı Kokpiti
        </h1>
        <p class="text-muted">
          Şirketinizin anlık ciro/kâr hedefleri, nakit durumu, en karlı müşteriler ve acil tahsilat sinyalleri.
        </p>
      </div>

      <div class="flex items-center gap-3">
        <Dropdown
          v-model="seciliAy"
          :options="aySecenekleri"
          option-label="ad"
          option-value="deger"
          class="w-36"
          @change="verileriYukle"
        />
        <Dropdown
          v-model="seciliYil"
          :options="yilSecenekleri"
          class="w-28"
          @change="verileriYukle"
        />
        <Button
          label="Hedef Belirle"
          icon="pi pi-bullseye"
          class="p-button-warning"
          @click="hedefModalAc"
        />
        <Button
          icon="pi pi-refresh"
          class="p-button-outlined"
          :loading="yukleniyor"
          @click="verileriYukle"
        />
      </div>
    </div>

    <!-- 1. BÜYÜK HEDEF & CANLI NABIZ HERO KARTI -->
    <div class="hero-kokpit-kart grid grid-cols-1 lg:grid-cols-3 gap-6 p-6 mb-6">
      <!-- Ciro İlerleme Göstergesi (Progress Ring) -->
      <div class="hedef-progress-kutu flex flex-col items-center justify-center text-center">
        <div class="progress-ring-wrapper relative flex items-center justify-center">
          <svg
            class="progress-ring"
            width="160"
            height="160"
          >
            <circle
              class="progress-ring-circle-bg"
              stroke="#e2e8f0"
              stroke-width="12"
              fill="transparent"
              r="68"
              cx="80"
              cy="80"
            />
            <circle
              class="progress-ring-circle"
              :stroke="progressRenk"
              stroke-width="12"
              stroke-linecap="round"
              fill="transparent"
              r="68"
              cx="80"
              cy="80"
              :style="{
                strokeDasharray: `${circumference} ${circumference}`,
                strokeDashoffset: strokeDashoffset
              }"
            />
          </svg>
          <div class="progress-metin absolute flex flex-col items-center">
            <span class="progress-yuzde text-3xl font-extrabold text-gray-900 dark:text-gray-100">
              %{{ (kokpit?.ciroIlerlemeYuzdesi || 0).toFixed(1) }}
            </span>
            <span class="text-xs text-muted font-medium">Hedef Gerçekleşti</span>
          </div>
        </div>

        <div class="mt-3">
          <span class="text-xs font-semibold uppercase tracking-wider text-muted">Aylık Ciro Hedefi</span>
          <div class="text-xl font-bold text-gray-800 dark:text-gray-100">
            {{ formatCurrency(kokpit?.gerceklesenCiro || 0) }} / {{ formatCurrency(kokpit?.hedefCiro || 0) }}
          </div>
        </div>
      </div>

      <!-- Hedef Kalan / Günlük Hız Metrikleri -->
      <div class="hedef-hiz-kutu flex flex-col justify-around border-l border-r border-gray-200 dark:border-gray-700 px-6">
        <div class="hiz-satir">
          <div class="flex justify-between items-center mb-1">
            <span class="text-sm text-muted">Kalan Ciro Tutarı:</span>
            <span class="text-base font-bold text-gray-800 dark:text-gray-100">{{ formatCurrency(kokpit?.kalanCiro || 0) }}</span>
          </div>
          <ProgressBar
            :value="Math.min(100, kokpit?.ciroIlerlemeYuzdesi || 0)"
            :show-value="false"
            style="height: 6px;"
          />
        </div>

        <div class="hiz-satir">
          <div class="flex justify-between items-center mb-1">
            <span class="text-sm text-muted">Ayın Kalan Süresi:</span>
            <span class="text-sm font-semibold text-amber-600 dark:text-amber-400">
              <i class="pi pi-clock mr-1" /> {{ kokpit?.kalanGun || 0 }} Gün Kaldı
            </span>
          </div>
        </div>

        <div class="hiz-satir bg-amber-50 dark:bg-amber-950/40 p-3 rounded-lg border border-amber-200 dark:border-amber-900/60">
          <span class="text-xs text-amber-800 dark:text-amber-300 font-semibold block mb-0.5">🎯 Günlük Hedef Hızı:</span>
          <p class="text-xs text-amber-900 dark:text-amber-200">
            Hedefe ulaşmak için günde ortalama <strong>{{ formatCurrency(gunlukGerekenCiro) }}</strong> ciro yapılması gerekiyor.
          </p>
        </div>
      </div>

      <!-- Net Kâr & Kâr Marjı Kutusu -->
      <div class="kar-ozet-kutu flex flex-col justify-around">
        <div class="kar-kart bg-emerald-50 dark:bg-emerald-950/40 p-4 rounded-xl border border-emerald-200 dark:border-emerald-900/60">
          <div class="flex justify-between items-center">
            <span class="text-xs font-bold uppercase tracking-wider text-emerald-800 dark:text-emerald-300">Dönem Net Kârı</span>
            <i class="pi pi-chart-line text-emerald-600 text-lg" />
          </div>
          <div class="text-2xl font-extrabold text-emerald-700 dark:text-emerald-300 mt-1">
            {{ formatCurrency(kokpit?.gerceklesenKar || 0) }}
          </div>
          <div class="text-xs text-emerald-600 dark:text-emerald-400 mt-1">
            Net Kâr Marjı: <strong>%{{ (kokpit?.netKarMarji || 0).toFixed(1) }}</strong>
          </div>
        </div>

        <div class="grid grid-cols-2 gap-3 text-xs">
          <div class="bg-gray-50 dark:bg-gray-800 p-2.5 rounded-lg border">
            <span class="text-muted block">Alış Maliyeti</span>
            <span class="font-bold text-gray-800 dark:text-gray-200">{{ formatCurrency(kokpit?.toplamAlisMaliyeti || 0) }}</span>
          </div>
          <div class="bg-gray-50 dark:bg-gray-800 p-2.5 rounded-lg border">
            <span class="text-muted block">Masraflar</span>
            <span class="font-bold text-gray-800 dark:text-gray-200">{{ formatCurrency(kokpit?.toplamMasraflar || 0) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 2. DÖRT TEMEL FİNANSAL NABIZ KARTI -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
      <div class="nabiz-kart kart-kasa">
        <div class="kart-sol">
          <span class="kart-baslik">Kasa & Banka Likidite</span>
          <span class="kart-tutar">{{ formatCurrency(kokpit?.kasaBankaToplam || 0) }}</span>
          <span class="kart-alt text-emerald-600"><i class="pi pi-check-circle" /> Anlık Nakit Varlık</span>
        </div>
        <div class="kart-sag-ikon">
          <i class="pi pi-wallet" />
        </div>
      </div>

      <div class="nabiz-kart kart-alacak">
        <div class="kart-sol">
          <span class="kart-baslik">Piyasa Alacakları</span>
          <span class="kart-tutar">{{ formatCurrency(kokpit?.toplamAlacak || 0) }}</span>
          <span class="kart-alt text-blue-600"><i class="pi pi-arrow-down-left" /> Müşteri Bakiyeleri</span>
        </div>
        <div class="kart-sag-ikon">
          <i class="pi pi-users" />
        </div>
      </div>

      <div class="nabiz-kart kart-risk">
        <div class="kart-sol">
          <span class="kart-baslik">Geciken Riskli Alacak</span>
          <span class="kart-tutar text-red-600">{{ formatCurrency(kokpit?.vadesiGecenAlacak || 0) }}</span>
          <span class="kart-alt text-red-500 font-semibold"><i class="pi pi-exclamation-triangle" /> {{ kokpit?.kritikAlacaklar?.length || 0 }} Faturada Gecikme</span>
        </div>
        <div class="kart-sag-ikon bg-red-100 text-red-600">
          <i class="pi pi-bell" />
        </div>
      </div>

      <div class="nabiz-kart kart-borc">
        <div class="kart-sol">
          <span class="kart-baslik">Tedarikçi Borçları</span>
          <span class="kart-tutar">{{ formatCurrency(kokpit?.toplamBorc || 0) }}</span>
          <span class="kart-alt text-purple-600"><i class="pi pi-arrow-up-right" /> Ödenecek Toplam</span>
        </div>
        <div class="kart-sag-ikon">
          <i class="pi pi-truck" />
        </div>
      </div>
    </div>

    <!-- 3. DETAYLI GRAFİK VE ANALİZ PANELLERİ -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
      <!-- Günlük Satış Trendi Grafiği -->
      <div class="col-span-2 kart-kutu">
        <div class="kart-header-baslik flex justify-between items-center mb-4">
          <h3 class="text-base font-bold text-gray-800 dark:text-gray-100">
            <i class="pi pi-chart-bar text-primary mr-1" /> Günlük Ciro Gerçekleşme Trendi
          </h3>
          <span class="text-xs text-muted">Aylık Dağılım</span>
        </div>
        <div
          v-if="trendVerisi.labels && trendVerisi.labels.length > 0"
          style="height: 280px;"
        >
          <Bar
            :data="trendVerisi"
            :options="chartOptions"
          />
        </div>
        <div
          v-else
          class="h-64 flex items-center justify-center text-muted"
        >
          Bu ay için satış hareketi bulunamadı.
        </div>
      </div>

      <!-- En Çok Kazandıran Top 5 Müşteri -->
      <div class="kart-kutu">
        <div class="kart-header-baslik flex justify-between items-center mb-4">
          <h3 class="text-base font-bold text-gray-800 dark:text-gray-100">
            <i class="pi pi-trophy text-amber-500 mr-1" /> En İyi 5 Müşteri
          </h3>
          <span class="text-xs text-muted">Ciroya Göre</span>
        </div>
        <div
          v-if="kokpit?.topMusteriler?.length > 0"
          class="space-y-3"
        >
          <div
            v-for="(m, idx) in kokpit.topMusteriler"
            :key="m.cariId"
            class="top-item flex justify-between items-center p-2.5 rounded-lg border bg-gray-50 dark:bg-gray-800"
          >
            <div class="flex items-center gap-3">
              <span class="sira-no font-bold text-xs text-muted w-4">#{{ idx + 1 }}</span>
              <div>
                <span class="font-semibold text-xs block text-gray-800 dark:text-gray-200">{{ m.unvan }}</span>
                <small class="text-muted">{{ m.faturaSayisi }} Fatura</small>
              </div>
            </div>
            <span class="font-bold text-xs text-primary">{{ formatCurrency(m.toplamCiro) }}</span>
          </div>
        </div>
        <div
          v-else
          class="py-12 text-center text-muted text-xs"
        >
          Kayıt bulunamadı.
        </div>
      </div>
    </div>

    <!-- 4. EN ÇOK SATAN ÜRÜNLER & ACİL TAHSİLAT LİSTESİ -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- En Çok Satan / Karlı Ürünler -->
      <div class="kart-kutu">
        <div class="kart-header-baslik flex justify-between items-center mb-3">
          <h3 class="text-base font-bold text-gray-800 dark:text-gray-100">
            <i class="pi pi-box text-indigo-500 mr-1" /> En Çok Satan Karlı Ürünler
          </h3>
        </div>
        <DataTable
          :value="kokpit?.topUrunler || []"
          class="p-datatable-sm"
          responsive-layout="scroll"
        >
          <Column
            field="stokKodu"
            header="Kod"
          />
          <Column
            field="stokAdi"
            header="Ürün Adı"
          />
          <Column
            field="satisMiktari"
            header="Satış Adedi"
            class="text-center"
          />
          <Column
            field="toplamCiro"
            header="Toplam Ciro"
            class="text-right font-bold"
          >
            <template #body="{ data }">
              {{ formatCurrency(data.toplamCiro) }}
            </template>
          </Column>
        </DataTable>
      </div>

      <!-- Acil Tahsilat Alarm Listesi (Geciken Alacaklar) -->
      <div class="kart-kutu">
        <div class="kart-header-baslik flex justify-between items-center mb-3">
          <h3 class="text-base font-bold text-red-600 dark:text-red-400">
            <i class="pi pi-bell text-red-500 mr-1" /> Acil Tahsilat Alarmları (Vadesi Geçen)
          </h3>
          <span class="text-xs text-red-500 font-semibold">{{ kokpit?.kritikAlacaklar?.length || 0 }} Cari</span>
        </div>
        <DataTable
          :value="kokpit?.kritikAlacaklar || []"
          class="p-datatable-sm"
          responsive-layout="scroll"
        >
          <Column
            field="unvan"
            header="Müşteri"
          />
          <Column
            field="bakiye"
            header="Kalan Borç"
            class="font-bold text-red-600 text-right"
          >
            <template #body="{ data }">
              {{ formatCurrency(data.bakiye) }}
            </template>
          </Column>
          <Column
            field="gecikmeGunu"
            header="Gecikme"
            class="text-center"
          >
            <template #body="{ data }">
              <span class="badge-gecikme">{{ data.gecikmeGunu }} gün</span>
            </template>
          </Column>
          <Column
            header="İletişim"
            class="text-right"
          >
            <template #body="{ data }">
              <div class="flex justify-end gap-1">
                <Button
                  v-if="data.telefon"
                  icon="pi pi-whatsapp"
                  class="p-button-text p-button-sm p-button-success"
                  title="WhatsApp Hatırlatması Gönder"
                  @click="hatirlatWhatsApp(data)"
                />
              </div>
            </template>
          </Column>
        </DataTable>
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

// Progress Ring Hesaplaması (Radius = 68)
const circumference = 2 * Math.PI * 68
const strokeDashoffset = computed(() => {
  const yuzde = Math.min(100, Math.max(0, kokpit.value?.ciroIlerlemeYuzdesi || 0))
  return circumference - (yuzde / 100) * circumference
})

const progressRenk = computed(() => {
  const y = kokpit.value?.ciroIlerlemeYuzdesi || 0
  if (y >= 100) return '#10b981' // Green
  if (y >= 70) return '#3b82f6'  // Blue
  if (y >= 40) return '#f59e0b'  // Amber
  return '#ef4444'              // Red
})

const gunlukGerekenCiro = computed(() => {
  const kalanCiro = kokpit.value?.kalanCiro || 0
  const kalanGun = Math.max(1, kokpit.value?.kalanGun || 1)
  return kalanCiro / kalanGun
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
  padding: 20px;
}
.hero-kokpit-kart {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.04);
}

.progress-ring {
  transform: rotate(-90deg);
}
.progress-ring-circle-bg {
  stroke: var(--border, #e2e8f0);
}
.progress-ring-circle {
  transition: stroke-dashoffset 0.8s ease-in-out;
}

.nabiz-kart {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
}
.kart-sol {
  display: flex;
  flex-direction: column;
}
.kart-baslik {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 500;
}
.kart-tutar {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 4px 0;
}
.kart-alt {
  font-size: 11px;
}
.kart-sag-ikon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: rgba(59, 130, 246, 0.1);
  color: var(--primary-color, #3b82f6);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
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
