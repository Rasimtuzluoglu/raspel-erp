<template>
  <div class="dashboard-container">
    <div class="dashboard-header">
      <h1>Raspel ERP Özeti</h1>
      <div class="header-sag">
        <div class="dashboard-datetime">
          <i class="pi pi-calendar"></i> {{ simdikiTarih }}
        </div>
        <Button icon="pi pi-refresh" class="p-button-rounded p-button-text" @click="refresh" :loading="loading" title="Yenile" />
        <Button icon="pi pi-cog" class="p-button-rounded p-button-text" @click="widgetAyarlariGoster = true" title="Widget Ayarları" />
      </div>
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

    <div class="skeleton-grid" v-if="loading">
      <div v-for="i in 7" :key="i" class="skeleton-card"><Skeleton width="100%" height="90px" /></div>
      <div style="grid-column:1/-1"><Skeleton width="100%" height="200px" /></div>
      <div style="grid-column:1/-1"><Skeleton width="100%" height="120px" /></div>
      <div style="grid-column:1/-1;display:grid;grid-template-columns:1fr 1fr;gap:18px">
        <Skeleton width="100%" height="220px" /><Skeleton width="100%" height="220px" />
      </div>
    </div>

    <Onboarding v-if="!loading && bosSistem" />

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
        <div class="stat-card">
          <div class="stat-icon tahsilat"><i class="pi pi-arrow-down"></i></div>
          <div class="stat-content">
            <p class="stat-label">Bugünkü Tahsilat</p>
            <p class="stat-value positive">{{ formatCurrency(dashboardStore.bugunkuTahsilat) }}</p>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon odeme"><i class="pi pi-arrow-up"></i></div>
          <div class="stat-content">
            <p class="stat-label">Bugünkü Ödeme</p>
            <p class="stat-value negative">{{ formatCurrency(dashboardStore.bugunkuOdeme) }}</p>
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
        <div class="stat-card">
          <div class="stat-icon bekleyen-izin"><i class="pi pi-clock"></i></div>
          <div class="stat-content">
            <p class="stat-label">Onay Bekleyen İzin</p>
            <p class="stat-value" :class="dashboardStore.bekleyenIzinSayisi > 0 ? 'negative' : 'positive'">{{ dashboardStore.bekleyenIzinSayisi }}</p>
          </div>
        </div>
      </div>

      <!-- HIZLI ISLEMLER -->
      <div class="hizli-islemler" v-if="widgets.hizliIslemler.gorunur">
        <h2 class="section-title"><i class="pi pi-bolt"></i> Hizli Islemler</h2>
        <div class="islem-grid">
          <Button label="Hizli Satis (POS)" icon="pi pi-shopping-cart" class="p-button-success p-button-lg" @click="$router.push('/hizli-satis')" />
          <Button label="Yeni Stok" icon="pi pi-box" class="p-button-info p-button-lg" @click="$router.push('/stoklar')" />
          <Button label="Yeni Cari" icon="pi pi-user-plus" class="p-button-help p-button-lg" @click="$router.push('/cari-hesaplar')" />
          <Button label="Yeni Fatura" icon="pi pi-file" class="p-button-warning p-button-lg" @click="$router.push('/faturalar')" />
        </div>
      </div>

      <!-- NAKİT AKIŞI -->
      <div class="nakit-akisi" v-if="widgets.nakitAkisi.gorunur">
        <h2 class="section-title"><i class="pi pi-money-bill"></i> Nakit Akışı</h2>
        <div class="nakit-grid">
          <Card class="nakit-kart">
            <template #title><i class="pi pi-building" style="margin-right:8px;color:#60a5fa"></i>Banka</template>
            <template #content><p class="nakit-deger positive">{{ formatCurrency(toplamBankaBakiye) }}</p></template>
          </Card>
          <Card class="nakit-kart">
            <template #title><i class="pi pi-money-bill" style="margin-right:8px;color:#34d399"></i>Kasa</template>
            <template #content><p class="nakit-deger positive">{{ formatCurrency(toplamKasaBakiye) }}</p></template>
          </Card>
          <Card class="nakit-kart">
            <template #title><i class="pi pi-arrow-down" style="margin-right:8px;color:#4ade80"></i>Bugünkü Tahsilat</template>
            <template #content><p class="nakit-deger positive">{{ formatCurrency(dashboardStore.bugunkuTahsilat) }}</p></template>
          </Card>
          <Card class="nakit-kart">
            <template #title><i class="pi pi-arrow-up" style="margin-right:8px;color:#f87171"></i>Bugünkü Ödeme</template>
            <template #content><p class="nakit-deger negative">{{ formatCurrency(dashboardStore.bugunkuOdeme) }}</p></template>
          </Card>
          <Card class="nakit-kart nakit-toplam">
            <template #title><i class="pi pi-wallet" style="margin-right:8px;color:#fbbf24"></i>Toplam Likidite</template>
            <template #content>
              <p class="nakit-deger" :class="toplamLikidite >= 0 ? 'positive' : 'negative'">{{ formatCurrency(toplamLikidite) }}</p>
              <p class="nakit-alt">Banka + Kasa</p>
            </template>
          </Card>
        </div>
      </div>

      <!-- SON GÖRÜNTÜLENENLER -->
      <div class="son-goruntulenenler" v-if="widgets.sonGoruntulenenler.gorunur && sonGoruntulenenler.length">
        <h2 class="section-title"><i class="pi pi-history"></i> Son Görüntülenenler</h2>
        <div class="sg-grid">
          <div v-for="(kayit, i) in sonGoruntulenenler" :key="i" class="sg-item" @click="sgGit(kayit)">
            <i :class="sgIkon(kayit.tur)" :style="{ color: sgRenk(kayit.tur) }"></i>
            <div class="sg-bilgi">
              <strong>{{ kayit.baslik }}</strong>
              <small>{{ kayit.alt }}</small>
            </div>
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

        <Card>
          <template #title><i class="pi pi-chart-bar" style="margin-right:8px"></i>Aylık Gelir / Gider (Son 6 Ay)</template>
          <template #content>
            <div class="chart-wrapper" v-if="gelirGiderChart.datasets.length" style="max-width:100%">
              <Bar :data="gelirGiderChart" :options="gelirGiderOptions" />
            </div>
            <div v-else class="chart-empty">Henüz hareket verisi yok</div>
          </template>
        </Card>
      </div>

      <!-- NOTLAR -->
      <div class="notlar-widget" v-if="widgets.notlar.gorunur">
        <div class="notlar-header">
          <h2><i class="pi pi-pencil"></i> Notlar</h2>
          <Button icon="pi pi-save" class="p-button-sm p-button-text" @click="notlariKaydet" :disabled="notKaydediliyor" :label="notKaydedildi ? 'Kaydedildi' : ''" />
        </div>
        <Textarea v-model="notMetni" :autoResize="true" rows="4" placeholder="Hizli notlarinizi buraya yazin..." class="not-textarea" @keydown.ctrl.enter="notlariKaydet" />
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
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import Skeleton from 'primevue/skeleton'
import { useDashboardStore } from '../stores/dashboardStore.js'
import { useCariHesapStore } from '../stores/cariHesapStore.js'
import { useFaturaStore } from '../stores/faturaStore.js'
import { useBankaStore } from '../stores/bankaStore.js'
import { useKasaStore } from '../stores/kasaStore.js'
import { useStokStore } from '../stores/stokStore.js'
import { Doughnut, Bar, Line } from 'vue-chartjs'
import Onboarding from '../components/Onboarding.vue'
import { useYakinZamanda, yakinZamandaTurleri } from '../composables/useYakinZamanda.js'
import { Chart as ChartJS, ArcElement, Tooltip, Legend, CategoryScale, LinearScale, PointElement, LineElement, BarElement, Filler } from 'chart.js'

ChartJS.register(ArcElement, Tooltip, Legend, CategoryScale, LinearScale, PointElement, LineElement, BarElement, Filler)

const router = useRouter()
const widgetVarsayilan = () => ({
  istatistikler: { gorunur: true, etiket: 'İstatistik Kartları' },
  satisSiparis: { gorunur: true, etiket: 'Satış & Sipariş' },
  insanKaynaklari: { gorunur: true, etiket: 'İnsan Kaynakları' },
  nakitAkisi: { gorunur: true, etiket: 'Nakit Akışı' },
  sonGoruntulenenler: { gorunur: true, etiket: 'Son Görüntülenenler' },
  grafikler: { gorunur: true, etiket: 'Grafikler' },
  sonHareketler: { gorunur: true, etiket: 'Son Hareketler' },
  hatirlaticilar: { gorunur: true, etiket: 'Hatırlatıcılar' },
  hizliIslemler: { gorunur: true, etiket: 'Hizli Islemler' },
  notlar: { gorunur: true, etiket: 'Notlar' }
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

const simdikiTarih = ref('')
const notMetni = ref(localStorage.getItem('raspel_erp_notlar') || '')
const notKaydediliyor = ref(false)
const notKaydedildi = ref(false)
const notlariKaydet = () => {
  localStorage.setItem('raspel_erp_notlar', notMetni.value)
  notKaydedildi.value = true
  setTimeout(() => { notKaydedildi.value = false }, 2000)
}
const refresh = async () => {
  loading.value = true
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
    console.error('Dashboard yenilenirken hata:', error)
  }
  loading.value = false
}
const tarihSaat = () => {
  const now = new Date()
  simdikiTarih.value = now.toLocaleDateString('tr-TR', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' }) + ' ' + now.toLocaleTimeString('tr-TR')
}
let tarihInterval = null

const bakiyeChart = ref({ labels: [], datasets: [] })
const barChart = ref({ labels: [], datasets: [] })
const hareketChart = ref({ labels: [], datasets: [] })
const gelirGiderChart = ref({ labels: [], datasets: [] })

const pieOptions = { responsive: true, plugins: { legend: { position: 'bottom' } } }
const barOptions = { responsive: true, indexAxis: 'y', plugins: { legend: { display: false } }, scales: { x: { ticks: { color: '#94a3b8' } }, y: { ticks: { color: '#94a3b8' } } } }
const lineOptions = { responsive: true, plugins: { legend: { display: false } }, scales: { x: { ticks: { color: '#94a3b8' } }, y: { ticks: { color: '#94a3b8', callback: v => formatCurrency(v) } } } }
const gelirGiderOptions = { responsive: true, plugins: { legend: { position: 'bottom' } }, scales: { x: { ticks: { color: '#94a3b8' } }, y: { ticks: { color: '#94a3b8', callback: v => formatCurrency(v) } } } }

const pozitifCariSayisi = computed(() => cariHesapStore.cariHesaplar.filter(c => c.bakiye >= 0).length)
const negatifCariSayisi = computed(() => cariHesapStore.cariHesaplar.filter(c => c.bakiye < 0).length)
const bosSistem = computed(() =>
  cariHesapStore.cariHesaplar.length === 0 &&
  stokStore.stoklar.length === 0 &&
  faturaStore.faturalar.length === 0
)

const sonGoruntulenenler = ref([])
const sgIkon = (tur) => yakinZamandaTurleri[tur]?.ikon || 'pi pi-history'
const sgRenk = (tur) => yakinZamandaTurleri[tur]?.renk || '#94a3b8'
const sgGit = (kayit) => {
  const yol = yakinZamandaTurleri[kayit.tur]?.yol
  if (yol) router.push(`${yol}${kayit.id}`)
}
const toplamFatura = computed(() => faturaStore.faturalar.length)
const kesilenFatura = computed(() => faturaStore.faturalar.filter(f => f.durum === 'KESILDI').length)
const toplamBankaBakiye = computed(() => bankaStore.bankalar.reduce((t, b) => t + (b.bakiye || 0), 0))
const toplamKasaBakiye = computed(() => kasaStore.kasalar.reduce((t, k) => t + (k.bakiye || 0), 0))
const toplamLikidite = computed(() => toplamBankaBakiye.value + toplamKasaBakiye.value)
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

  if (dashboardStore.aylikGelirGider?.length) {
    gelirGiderChart.value = {
      labels: dashboardStore.aylikGelirGider.map(a => a.ay),
      datasets: [
        {
          label: 'Gelir',
          data: dashboardStore.aylikGelirGider.map(a => a.gelir),
          backgroundColor: '#4caf50',
          borderRadius: 4
        },
        {
          label: 'Gider',
          data: dashboardStore.aylikGelirGider.map(a => a.gider),
          backgroundColor: '#f44336',
          borderRadius: 4
        }
      ]
    }
  }
}

onMounted(async () => {
  tarihSaat(); tarihInterval = setInterval(tarihSaat, 1000)
  sonGoruntulenenler.value = useYakinZamanda().liste()
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

onUnmounted(() => { if (tarihInterval) clearInterval(tarihInterval) })

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
.dashboard-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; gap: 16px; flex-wrap: wrap; }
.dashboard-header h1 { margin: 0; font-size: 28px; font-weight: 700; }
.header-sag { display: flex; align-items: center; gap: 12px; }
.dashboard-datetime { font-size: 13px; color: #94a3b8; white-space: nowrap; }
.dashboard-datetime i { margin-right: 6px; }
.skeleton-grid { display:grid; grid-template-columns:repeat(auto-fit,minmax(230px,1fr)); gap:16px; margin-bottom:24px; }
.skeleton-card { border-radius:14px; overflow:hidden; }

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
.stat-icon.tahsilat { background: linear-gradient(135deg, #22c55e, #16a34a); }
.stat-icon.odeme { background: linear-gradient(135deg, #ef4444, #dc2626); }
.stat-icon.bekleyen-izin { background: linear-gradient(135deg, #f59e0b, #d97706); }
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

.charts-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 18px; margin-bottom: 24px; }
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

.notlar-widget { background: var(--bg-card); padding: 18px 20px; border-radius: 14px; border: 1px solid var(--border); margin-bottom: 24px; }
.notlar-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.notlar-header h2 { margin: 0; font-size: 16px; display: flex; align-items: center; gap: 8px; }
.not-textarea { width: 100%; background: var(--bg-primary); border: 1px solid var(--border); border-radius: 10px; color: var(--text-primary); font-size: 14px; padding: 12px; resize: vertical; }
.not-textarea:focus { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59,130,246,0.2); outline: none; }

.section-title { font-size: 16px; margin: 24px 0 12px; color: var(--text-primary); display: flex; align-items: center; gap: 8px; }
.nakit-akisi { margin-bottom: 24px; }
.nakit-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 14px; }
.nakit-kart .p-card-title { font-size: 13px !important; }
.nakit-kart .p-card-content { padding-top: 0 !important; }
.nakit-deger { font-size: 20px; font-weight: 700; margin: 0; }
.nakit-alt { font-size: 11px; color: var(--text-muted); margin: 4px 0 0; }
.nakit-toplam { border-color: rgba(245,158,11,0.3) !important; }
.son-goruntulenenler { margin-bottom: 24px; }
.sg-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 10px; }
.sg-item {
  display: flex; align-items: center; gap: 10px;
  background: var(--bg-card); border: 1px solid var(--border);
  border-radius: 10px; padding: 10px 14px; cursor: pointer;
  transition: all 0.15s;
}
.sg-item:hover { border-color: rgba(59,130,246,0.3); transform: translateY(-2px); }
.sg-item i { font-size: 18px; }
.sg-bilgi { flex: 1; min-width: 0; }
.sg-bilgi strong { display: block; font-size: 13px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.sg-bilgi small { display: block; font-size: 11px; color: var(--text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.hizli-islemler { margin-bottom: 24px; }
.islem-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; }
.islem-grid .p-button { height: 56px; font-size: 13px; justify-content: center; }

@media (max-width: 900px) {
  .charts-row, .bottom-grid { grid-template-columns: 1fr; }
}
</style>
