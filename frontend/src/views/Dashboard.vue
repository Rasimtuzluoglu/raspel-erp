<template>
  <div class="dashboard-container">
    <div class="dashboard-header">
      <h1>Raspel ERP Özeti</h1>
      <Button icon="pi pi-cog" class="p-button-rounded p-button-text" @click="widgetAyarlariGoster = true" title="Widget Ayarları" />
    </div>

    <Card v-if="widgetAyarlariGoster" class="widget-ayarlari">
      <template #title><i class="pi pi-sliders-h" style="margin-right:8px"></i>Gösterilecek Widget'lar</template>
      <template #content>
        <div class="widget-togglar">
          <label v-for="w in widgetListesi" :key="w.key" class="widget-toggle">
            <InputSwitch v-model="w.gorunur" />
            <span>{{ w.etiket }}</span>
          </label>
        </div>
        <div style="margin-top:16px;display:flex;gap:8px;justify-content:flex-end">
          <Button label="Uygula" icon="pi pi-check" class="p-button-sm" @click="kaydetWidget" />
          <Button label="İptal" icon="pi pi-times" class="p-button-sm p-button-text" @click="iptalWidget" />
        </div>
      </template>
    </Card>

    <div class="loading" v-if="loading">
      <i class="pi pi-spin pi-spinner"></i> Yükleniyor...
    </div>

    <template v-if="!loading">
      <!-- İSTATİSTİKLER -->
      <div class="stats-grid" v-if="widgets.istatistikler.gorunur">
        <div class="stat-card">
          <div class="stat-icon cari"><i class="pi pi-users"></i></div>
          <div class="stat-content">
            <p class="stat-label">Toplam Cari</p>
            <p class="stat-value">{{ dashboardStore.toplamCariSayisi }}</p>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon finans"><i class="pi pi-wallet"></i></div>
          <div class="stat-content">
            <p class="stat-label">Toplam Bakiye</p>
            <p class="stat-value" :class="dashboardStore.toplamBakiye >= 0 ? 'positive' : 'negative'">
              {{ formatCurrency(dashboardStore.toplamBakiye) }}
            </p>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon fatura"><i class="pi pi-file"></i></div>
          <div class="stat-content">
            <p class="stat-label">Kesilen / Toplam Fatura</p>
            <p class="stat-value">{{ kesilenFatura }} / {{ toplamFatura }}</p>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon banka"><i class="pi pi-building"></i></div>
          <div class="stat-content">
            <p class="stat-label">Banka Hesapları</p>
            <p class="stat-value">{{ bankaStore.bankalar.length }}</p>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon banka-bakiye"><i class="pi pi-credit-card"></i></div>
          <div class="stat-content">
            <p class="stat-label">Banka Bakiyesi</p>
            <p class="stat-value positive">{{ formatCurrency(toplamBankaBakiye) }}</p>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon kasa"><i class="pi pi-money-bill"></i></div>
          <div class="stat-content">
            <p class="stat-label">Kasa Bakiyesi</p>
            <p class="stat-value positive">{{ formatCurrency(toplamKasaBakiye) }}</p>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon stok"><i class="pi pi-box"></i></div>
          <div class="stat-content">
            <p class="stat-label">Stoktaki Ürünler</p>
            <p class="stat-value">{{ toplamStok }} <small>ürün</small></p>
            <p v-if="dusukStokAdet > 0" class="critical-hint">
              <i class="pi pi-exclamation-triangle"></i> {{ dusukStokAdet }} kritik
            </p>
          </div>
        </div>
      </div>

      <!-- SATIS / SIPARIS -->
      <div class="stats-grid" v-if="widgets.satisSiparis.gorunur">
        <div class="stat-card">
          <div class="stat-icon ticaret"><i class="pi pi-shopping-cart"></i></div>
          <div class="stat-content">
            <p class="stat-label">Bugünkü Siparişler</p>
            <p class="stat-value">{{ dashboardStore.bugunkuSiparis }}</p>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon beklemede"><i class="pi pi-clock"></i></div>
          <div class="stat-content">
            <p class="stat-label">Teslim Edilmeyi Bekleyen</p>
            <p class="stat-value">{{ dashboardStore.bekleyenTeslimat }}</p>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon iade"><i class="pi pi-replay"></i></div>
          <div class="stat-content">
            <p class="stat-label">İade Oranı</p>
            <p class="stat-value">%{{ dashboardStore.iadeOrani || 0 }}</p>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon devir"><i class="pi pi-chart-line"></i></div>
          <div class="stat-content">
            <p class="stat-label">Stok Devir Hızı</p>
            <p class="stat-value">{{ dashboardStore.stokDevirHizi || 0 }}x</p>
          </div>
        </div>
      </div>

      <!-- INSAN KAYNAKLARI -->
      <div class="stats-grid" v-if="widgets.insanKaynaklari.gorunur">
        <div class="stat-card">
          <div class="stat-icon calisan"><i class="pi pi-id-card"></i></div>
          <div class="stat-content">
            <p class="stat-label">Aktif Çalışan</p>
            <p class="stat-value">{{ dashboardStore.aktifCalisan }}</p>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon izinli"><i class="pi pi-calendar-times"></i></div>
          <div class="stat-content">
            <p class="stat-label">Bugün İzinli</p>
            <p class="stat-value">{{ dashboardStore.bugunIzinli }}</p>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon ise-baslayacak"><i class="pi pi-user-plus"></i></div>
          <div class="stat-content">
            <p class="stat-label">Bu Ay İşe Başlayacak</p>
            <p class="stat-value">{{ dashboardStore.buAyIseBaslayacak }}</p>
          </div>
        </div>
      </div>

      <!-- GRAFIkLER -->
      <div class="charts-row" v-if="widgets.grafikler.gorunur">
        <Card>
          <template #title><i class="pi pi-chart-pie" style="margin-right:8px"></i>Bakiye Dağılımı</template>
          <template #content>
            <div class="chart-wrapper" v-if="bakiyeChart.datasets.length">
              <Doughnut :data="bakiyeChart" :options="pieOptions" />
            </div>
            <div class="chart-summary">
              <span class="dot pos"></span> Pozitif: {{ formatCurrency(dashboardStore.pozitifBakiye) }}
              <span class="dot neg"></span> Negatif: {{ formatCurrency(Math.abs(dashboardStore.negatifBakiye)) }}
            </div>
          </template>
        </Card>

        <Card>
          <template #title><i class="pi pi-star-fill" style="margin-right:8px"></i>En Çok Satan 5 Ürün</template>
          <template #content>
            <div class="chart-wrapper" v-if="barChart.datasets.length">
              <Bar :data="barChart" :options="barOptions" />
            </div>
            <div v-else class="chart-empty">Henüz satış verisi yok</div>
          </template>
        </Card>
      </div>

      <!-- SON HAREKETLER & HATIRLATICI -->
      <div class="bottom-grid">
        <div class="recent-transactions" v-if="widgets.sonHareketler.gorunur">
          <h2>Son Hareketler</h2>
          <div class="chart-wrapper line-chart" v-if="hareketChart.datasets.length">
            <Line :data="hareketChart" :options="lineOptions" />
          </div>
          <DataTable :value="dashboardStore.sonHareketler" :rows="5" stripedRows size="small">
            <Column field="cariHesapAd" header="Cari Hesap"><template #body="s"><strong>{{ s.data.cariHesapAd }}</strong></template></Column>
            <Column field="tur" header="Tür" style="width:100px">
              <template #body="s"><span :class="['badge', s.data.tur === 'TAHSILAT' ? 'tahsilat' : 'odeme']">{{ s.data.tur === 'TAHSILAT' ? 'Tahsilat' : 'Ödeme' }}</span></template>
            </Column>
            <Column field="tutar" header="Tutar" style="width:120px">
              <template #body="s"><span :class="s.data.tur === 'TAHSILAT' ? 'positive' : 'negative'">{{ formatCurrency(s.data.tutar) }}</span></template>
            </Column>
            <Column field="hareketTarihi" header="Tarih" style="width:120px"><template #body="s">{{ formatDate(s.data.hareketTarihi) }}</template></Column>
          </DataTable>
          <Message v-if="!dashboardStore.sonHareketler.length" severity="info" text="Henüz hareket yok" />
        </div>

        <div class="reminder-grid" v-if="widgets.hatirlaticilar.gorunur">
          <Card class="reminder-card vadesi-gecen">
            <template #title><i class="pi pi-exclamation-triangle" style="color:#f87171;margin-right:8px"></i>Vadesi Geçen Cari</template>
            <template #content>
              <div v-if="!vadesiGecenCari.length" class="reminder-empty"><i class="pi pi-check-circle"></i> Vadesi geçen cari yok</div>
              <div v-for="c in vadesiGecenCari.slice(0, 5)" :key="c.id" class="reminder-item">
                <span class="reminder-ad">{{ c.ad }}</span>
                <span class="reminder-tutar negative">{{ formatCurrency(Math.abs(c.bakiye)) }}</span>
              </div>
            </template>
          </Card>
          <Card class="reminder-card dusuk-stok">
            <template #title><i class="pi pi-box" style="color:#fbbf24;margin-right:8px"></i>Kritik Stok</template>
            <template #content>
              <div v-if="!dusukStokAdet" class="reminder-empty"><i class="pi pi-check-circle"></i> Kritik stok yok</div>
              <div v-for="s in stokStore.dusukStoklar.slice(0, 5)" :key="s.id" class="reminder-item">
                <span class="reminder-ad">{{ s.ad }}</span>
                <span class="reminder-tutar" style="color:#fbbf24">{{ s.miktar }} {{ s.birim }}</span>
              </div>
            </template>
          </Card>
          <Card class="reminder-card son-faturalar">
            <template #title><i class="pi pi-file" style="color:#60a5fa;margin-right:8px"></i>Son Faturalar</template>
            <template #content>
              <div v-if="!sonFaturalar.length" class="reminder-empty">Henüz fatura yok</div>
              <div v-for="f in sonFaturalar" :key="f.id" class="reminder-item">
                <span class="reminder-ad">#{{ f.faturaNumarasi || f.id }}</span>
                <span class="reminder-tutar">{{ formatCurrency(f.genelToplam) }}</span>
              </div>
            </template>
          </Card>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useDashboardStore } from '../stores/dashboardStore.js'
import { useCariHesapStore } from '../stores/cariHesapStore.js'
import { useFaturaStore } from '../stores/faturaStore.js'
import { useBankaStore } from '../stores/bankaStore.js'
import { useKasaStore } from '../stores/kasaStore.js'
import { useStokStore } from '../stores/stokStore.js'
import { Doughnut, Bar, Line } from 'vue-chartjs'
import { Chart as ChartJS, ArcElement, Tooltip, Legend, CategoryScale, LinearScale, PointElement, LineElement, BarElement } from 'chart.js'

ChartJS.register(ArcElement, Tooltip, Legend, CategoryScale, LinearScale, PointElement, LineElement, BarElement)

const widgetVarsayilan = () => ({
  istatistikler: { gorunur: true, etiket: 'İstatistik Kartları' },
  satisSiparis: { gorunur: true, etiket: 'Satış & Sipariş' },
  insanKaynaklari: { gorunur: true, etiket: 'İnsan Kaynakları' },
  grafikler: { gorunur: true, etiket: 'Grafikler' },
  sonHareketler: { gorunur: true, etiket: 'Son Hareketler' },
  hatirlaticilar: { gorunur: true, etiket: 'Hatırlatıcılar' }
})

const widgetListesi = ref(Object.entries(widgetVarsayilan()).map(([k, v]) => ({ key: k, ...v })))
const widgetAyarlariGoster = ref(false)
const widgets = reactive(widgetVarsayilan())

const kaydetWidget = () => {
  widgetListesi.value.forEach(w => { widgets[w.key].gorunur = w.gorunur })
  widgetAyarlariGoster.value = false
  localStorage.setItem('raspel_erp_widgets', JSON.stringify(Object.fromEntries(Object.entries(widgets).map(([k, v]) => [k, v.gorunur]))))
}
const iptalWidget = () => {
  widgetAyarlariGoster.value = false
  widgetListesi.value = Object.entries(widgets).map(([k, v]) => ({ key: k, ...v }))
}

const dashboardStore = useDashboardStore()
const cariHesapStore = useCariHesapStore()
const faturaStore = useFaturaStore()
const bankaStore = useBankaStore()
const kasaStore = useKasaStore()
const stokStore = useStokStore()
const loading = ref(true)

const bakiyeChart = ref({ labels: [], datasets: [] })
const barChart = ref({ labels: [], datasets: [] })
const hareketChart = ref({ labels: [], datasets: [] })

const pieOptions = { responsive: true, plugins: { legend: { position: 'bottom' } } }
const barOptions = { responsive: true, indexAxis: 'y', plugins: { legend: { display: false } }, scales: { x: { ticks: { color: '#94a3b8' } }, y: { ticks: { color: '#94a3b8' } } } }
const lineOptions = { responsive: true, plugins: { legend: { display: false } }, scales: { x: { ticks: { color: '#94a3b8' } }, y: { ticks: { color: '#94a3b8', callback: v => formatCurrency(v) } } } }

const pozitifCariSayisi = computed(() => cariHesapStore.cariHesaplar.filter(c => c.bakiye >= 0).length)
const negatifCariSayisi = computed(() => cariHesapStore.cariHesaplar.filter(c => c.bakiye < 0).length)
const toplamFatura = computed(() => faturaStore.faturalar.length)
const kesilenFatura = computed(() => faturaStore.faturalar.filter(f => f.durum === 'KESILDI').length)
const toplamBankaBakiye = computed(() => bankaStore.bankalar.reduce((t, b) => t + (b.bakiye || 0), 0))
const toplamKasaBakiye = computed(() => kasaStore.kasalar.reduce((t, k) => t + (k.bakiye || 0), 0))
const toplamStok = computed(() => stokStore.stoklar.length)
const dusukStokAdet = computed(() => stokStore.dusukStoklar.length)
const vadesiGecenCari = computed(() => cariHesapStore.cariHesaplar.filter(c => c.bakiye < 0).sort((a, b) => a.bakiye - b.bakiye))
const sonFaturalar = computed(() => [...faturaStore.faturalar].sort((a, b) => new Date(b.olusturmaTarihi) - new Date(a.olusturmaTarihi)).slice(0, 5))

const grafikleriHesapla = () => {
  bakiyeChart.value = {
    labels: ['Pozitif Bakiye', 'Negatif Bakiye'],
    datasets: [{
      data: [dashboardStore.pozitifBakiye || 0, Math.abs(dashboardStore.negatifBakiye) || 0],
      backgroundColor: ['#4caf50', '#f44336'],
      hoverBackgroundColor: ['#66bb6a', '#ef5350']
    }]
  }

  if (dashboardStore.enCokSatanlar?.length) {
    barChart.value = {
      labels: dashboardStore.enCokSatanlar.map(e => e.stokAd),
      datasets: [{
        data: dashboardStore.enCokSatanlar.map(e => e.satisMiktari),
        backgroundColor: ['#3b82f6', '#8b5cf6', '#ec4899', '#f59e0b', '#14b8a6']
      }]
    }
  }

  if (dashboardStore.sonHareketler?.length) {
    const ters = [...dashboardStore.sonHareketler].reverse()
    hareketChart.value = {
      labels: ters.map(h => formatDate(h.hareketTarihi)),
      datasets: [{
        label: 'Tutar',
        data: ters.map(h => h.tutar),
        borderColor: '#3b82f6',
        backgroundColor: 'rgba(59,130,246,0.1)',
        fill: true,
        tension: 0.3,
        pointRadius: 4,
        pointBackgroundColor: '#3b82f6'
      }]
    }
  }
}

onMounted(async () => {
  try {
    const kayitli = JSON.parse(localStorage.getItem('raspel_erp_widgets'))
    if (kayitli) {
      Object.keys(widgets).forEach(k => { if (kayitli[k] !== undefined) widgets[k].gorunur = kayitli[k] })
      widgetListesi.value = Object.entries(widgets).map(([k, v]) => ({ key: k, ...v }))
    }
  } catch (e) { /* yok */ }

  try {
    await Promise.all([
      dashboardStore.getDashboardData(),
      cariHesapStore.getAllCariHesaplar(),
      faturaStore.getAllFaturalar(),
      bankaStore.getAllBankalar(),
      kasaStore.getAllKasalar(),
      stokStore.getAll()
    ])
    grafikleriHesapla()
  } catch (error) {
    console.error('Dashboard yüklenirken hata:', error)
  }
  loading.value = false
})

const formatCurrency = (v) => {
  if (v === null || v === undefined) return '0,00 ₺'
  return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(v)
}
const formatDate = (d) => {
  if (!d) return '-'
  return new Intl.DateTimeFormat('tr-TR', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(d))
}
</script>

<style scoped>
.dashboard-container { padding: 0; }
.dashboard-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
.dashboard-header h1 { margin: 0; font-size: 28px; font-weight: 700; }
.loading { text-align: center; padding: 60px; color: #94a3b8; font-size: 16px; }
.loading i { margin-right: 8px; }

.widget-ayarlari { margin-bottom: 24px; }
.widget-togglar { display: flex; flex-wrap: wrap; gap: 16px; }
.widget-toggle { display: flex; align-items: center; gap: 10px; font-size: 14px; cursor: pointer; }

.stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(230px, 1fr)); gap: 16px; margin-bottom: 24px; }
.stat-card { background: var(--bg-card); padding: 18px; border-radius: 14px; border: 1px solid var(--border); display: flex; align-items: center; gap: 16px; transition: all 0.3s; box-shadow: 0 2px 12px rgba(0,0,0,0.2); }
.stat-card:hover { transform: translateY(-3px); box-shadow: 0 8px 24px rgba(0,0,0,0.3); border-color: rgba(59,130,246,0.25); }
.stat-icon { width: 52px; height: 52px; background: linear-gradient(135deg, #1976d2, #1565c0); border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 26px; color: white; flex-shrink: 0; }
.stat-icon.cari { background: linear-gradient(135deg, #3b82f6, #2563eb); }
.stat-icon.finans { background: linear-gradient(135deg, #6366f1, #4f46e5); }
.stat-icon.fatura { background: linear-gradient(135deg, #10b981, #059669); }
.stat-icon.banka { background: linear-gradient(135deg, #f59e0b, #d97706); }
.stat-icon.banka-bakiye { background: linear-gradient(135deg, #8b5cf6, #7c3aed); }
.stat-icon.kasa { background: linear-gradient(135deg, #14b8a6, #0d9488); }
.stat-icon.stok { background: linear-gradient(135deg, #f97316, #ea580c); }
.stat-icon.ticaret { background: linear-gradient(135deg, #22c55e, #16a34a); }
.stat-icon.beklemede { background: linear-gradient(135deg, #ef4444, #dc2626); }
.stat-icon.iade { background: linear-gradient(135deg, #f59e0b, #d97706); }
.stat-icon.devir { background: linear-gradient(135deg, #06b6d4, #0891b2); }
.stat-icon.calisan { background: linear-gradient(135deg, #22c55e, #16a34a); }
.stat-icon.izinli { background: linear-gradient(135deg, #eab308, #ca8a04); }
.stat-icon.ise-baslayacak { background: linear-gradient(135deg, #3b82f6, #2563eb); }
.stat-content { flex: 1; min-width: 0; }
.stat-label { font-size: 12px; color: #94a3b8; margin: 0 0 6px; }
.stat-value { font-size: 22px; font-weight: 700; margin: 0; }
.stat-value small { font-size: 12px; color: #94a3b8; font-weight: 400; }
.stat-value.positive { color: #4ade80; }
.stat-value.negative { color: #f87171; }
.critical-hint { margin: 4px 0 0; font-size: 11px; color: #f87171; }

.charts-row { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; margin-bottom: 24px; }
.chart-wrapper { max-width: 350px; margin: 0 auto; }
.chart-wrapper.line-chart { max-width: 100%; }
.chart-summary { text-align: center; margin-top: 12px; font-size: 13px; color: #94a3b8; display: flex; justify-content: center; gap: 20px; }
.chart-empty { text-align: center; padding: 30px; color: #64748b; }
.dot { display: inline-block; width: 10px; height: 10px; border-radius: 50%; margin-right: 6px; vertical-align: middle; }
.dot.pos { background: #4caf50; }
.dot.neg { background: #f44336; }

.bottom-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
.recent-transactions { background: var(--bg-card); padding: 20px; border-radius: 14px; border: 1px solid var(--border); }
.recent-transactions h2 { margin: 0 0 12px; font-size: 18px; }

.reminder-grid { display: flex; flex-direction: column; gap: 16px; }
.reminder-card .p-card-title { font-size: 14px !important; }
.reminder-item { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px solid var(--border); }
.reminder-item:last-child { border-bottom: none; }
.reminder-ad { font-size: 13px; }
.reminder-tutar { font-size: 13px; font-weight: 600; color: #4ade80; }
.reminder-tutar.negative { color: #f87171; }
.reminder-empty { text-align: center; padding: 12px; color: #4ade80; font-size: 13px; }
.reminder-empty i { display: block; font-size: 22px; margin-bottom: 4px; }

.badge { padding: 2px 10px; border-radius: 20px; font-size: 11px; font-weight: 600; }
.badge.tahsilat { background: rgba(76,175,80,0.15); color: #4ade80; }
.badge.odeme { background: rgba(244,67,54,0.15); color: #f87171; }
.positive { color: #4caf50; font-weight: bold; }
.negative { color: #f44336; font-weight: bold; }

@media (max-width: 900px) {
  .charts-row, .bottom-grid { grid-template-columns: 1fr; }
}
</style>
