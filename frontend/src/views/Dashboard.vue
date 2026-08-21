<template>
  <div class="dashboard-container">
    <div class="dashboard-header">
      <div class="dashboard-baslik-blok">
        <h1>RasPel ERP</h1>
        <p class="karsilama-mesaji">
          {{ karsilamaMetni }},
          <strong>{{ authStore?.kullanici?.displayName || authStore?.kullanici?.username || '' }}</strong>
          <span
            v-if="authStore?.sirketAdi"
            class="karsilama-sirket"
          >
            <i class="pi pi-building" /> {{ authStore?.sirketAdi }}
          </span>
        </p>
      </div>
      <div class="header-sag">
        <div class="doviz-ticker-compact">
          <div
            v-for="k in (dovizStore?.kurlar || [])"
            :key="k.kod || k.dovizKodu"
            class="ticker-chip"
          >
            <span class="chip-kod">{{ k.kod || k.dovizKodu }}:</span>
            <span class="chip-fiyat">{{ dovizStore?.formatPara ? dovizStore.formatPara(k.satisFiyati || k.satisKuru, 'TRY') : '' }}</span>
          </div>
          <button
            class="chip-refresh-btn"
            :disabled="dovizStore?.loading || false"
            title="Kurları Yenile"
            @click="dovizStore?.kurlariGuncelle"
          >
            <i :class="dovizStore?.loading ? 'pi pi-spin pi-spinner' : 'pi pi-sync'" />
          </button>
        </div>
        <div class="dashboard-datetime">
          <SaatGostergesi />
        </div>
        <Button
          icon="pi pi-refresh"
          class="p-button-rounded p-button-text"
          :loading="loading"
          title="Yenile"
          @click="refresh"
        />
        <Button
          icon="pi pi-cog"
          class="p-button-rounded p-button-text"
          title="Widget Ayarları"
          @click="widgetAyarlariGoster = true"
        />
      </div>
    </div>

    <Card
      v-if="widgetAyarlariGoster"
      class="widget-ayarlari"
    >
      <template #title>
        <i
          class="pi pi-sliders-h"
          style="margin-right: 8px"
        />Gösterilecek Widget'lar
      </template>
      <template #content>
        <div class="widget-togglar">
          <label
            v-for="w in widgetListesi"
            :key="w.key"
            class="widget-toggle"
          >
            <InputSwitch v-model="w.gorunur" />
            <span>{{ w.etiket }}</span>
          </label>
        </div>
        <div style="margin-top: 16px; display: flex; gap: 8px; justify-content: flex-end">
          <Button
            label="Uygula"
            icon="pi pi-check"
            class="p-button-sm"
            @click="kaydetWidget"
          />
          <Button
            label="İptal"
            icon="pi pi-times"
            class="p-button-sm p-button-text"
            @click="iptalWidget"
          />
        </div>
      </template>
    </Card>

    <div
      v-if="loading"
      class="skeleton-grid"
    >
      <div
        v-for="i in 7"
        :key="i"
        class="skeleton-card"
      >
        <Skeleton
          width="100%"
          height="90px"
        />
      </div>
      <div style="grid-column: 1/-1">
        <Skeleton
          width="100%"
          height="200px"
        />
      </div>
      <div style="grid-column: 1/-1">
        <Skeleton
          width="100%"
          height="120px"
        />
      </div>
      <div style="grid-column: 1/-1; display: grid; grid-template-columns: 1fr 1fr; gap: 18px">
        <Skeleton
          width="100%"
          height="220px"
        /><Skeleton
          width="100%"
          height="220px"
        />
      </div>
    </div>

    <Onboarding
      v-if="!loading && bosSistem"
      @demo-loaded="demoYuklendi"
    />

    <template v-if="!loading && !bosSistem">
      <!-- 1. TEMEL 4 KPI KARTI -->
      <div
        v-if="widgets.istatistikler.gorunur"
        class="stats-grid"
      >
        <div class="stat-card cari">
          <div class="stat-icon cari">
            <i class="pi pi-users" />
          </div>
          <div class="stat-content">
            <p class="stat-label">
              Toplam Cari
            </p>
            <p class="stat-value">
              {{ dashboardStore?.toplamCariSayisi || 0 }}
            </p>
            <p class="stat-sub">
              Bakiye: <strong>{{ formatCurrency(dashboardStore?.toplamBakiye || 0) }}</strong>
            </p>
          </div>
        </div>
        <div class="stat-card finans">
          <div class="stat-icon finans">
            <i class="pi pi-wallet" />
          </div>
          <div class="stat-content">
            <p class="stat-label">
              Toplam Likidite
            </p>
            <p
              class="stat-value"
              :class="toplamLikidite >= 0 ? 'positive' : 'negative'"
            >
              {{ formatCurrency(toplamLikidite) }}
            </p>
            <p class="stat-sub">
              Kasa: {{ formatCurrency(toplamKasaBakiye) }} · Banka: {{ formatCurrency(toplamBankaBakiye) }}
            </p>
          </div>
        </div>
        <div class="stat-card fatura">
          <div class="stat-icon fatura">
            <i class="pi pi-file" />
          </div>
          <div class="stat-content">
            <p class="stat-label">
              Fatura Durumu
            </p>
            <p class="stat-value">
              {{ kesilenFatura }} / {{ toplamFatura }}
            </p>
            <p class="stat-sub">
              Bugünkü Tahsilat: <strong>{{ formatCurrency(dashboardStore?.bugunkuTahsilat || 0) }}</strong>
            </p>
          </div>
        </div>
        <div class="stat-card stok">
          <div class="stat-icon stok">
            <i class="pi pi-box" />
          </div>
          <div class="stat-content">
            <p class="stat-label">
              Stok Çeşidi
            </p>
            <p class="stat-value">
              {{ toplamStok }} <small>ürün</small>
            </p>
            <p
              v-if="dusukStokAdet > 0"
              class="critical-hint"
            >
              <i class="pi pi-exclamation-triangle" /> {{ dusukStokAdet }} kritik stok
            </p>
            <p
              v-else
              class="stat-sub text-emerald-600"
            >
              Stok seviyeleri yeterli
            </p>
          </div>
        </div>
      </div>

      <!-- 2. HIZLI İŞLEMLER ÇUBUĞU -->
      <div
        v-if="widgets.istatistikler.gorunur"
        class="quick-actions"
      >
        <router-link
          v-if="authStore.isAdmin"
          to="/yonetici-kokpiti"
          class="action-card kokpit"
        >
          <i class="pi pi-bolt" /><span>Yönetici Kokpiti</span>
        </router-link>
        <router-link
          to="/teklifler"
          class="action-card teklif"
        >
          <i class="pi pi-file-edit" /><span>Satış Teklifleri</span>
        </router-link>
        <router-link
          to="/saha-portali"
          class="action-card saha"
        >
          <i class="pi pi-compass" /><span>Saha Portalı</span>
        </router-link>
        <router-link
          to="/faturalar"
          class="action-card fatura"
        >
          <i class="pi pi-file" /><span>Yeni Fatura</span>
        </router-link>
        <router-link
          to="/hizli-satis"
          class="action-card satis"
        >
          <i class="pi pi-shopping-bag" /><span>Hızlı Satış</span>
        </router-link>
        <router-link
          to="/cari-hesaplar"
          class="action-card cari"
        >
          <i class="pi pi-user-plus" /><span>Yeni Cari</span>
        </router-link>
        <router-link
          to="/hareketler"
          class="action-card tahsilat"
        >
          <i class="pi pi-money-bill" /><span>Tahsilat/Ödeme</span>
        </router-link>
      </div>

      <div
        v-if="authStore.isAdmin && yedekUyarisiGoster"
        class="backup-reminder"
      >
        <i class="pi pi-save" />
        <span>Son yedekleme 7 günden eski. Verilerinizi güvence altına almak için
          <router-link to="/yedekler">yedek alın</router-link></span>
        <button
          class="reminder-close"
          @click="yedekUyarisiGoster = false"
        >
          &times;
        </button>
      </div>

      <!-- 3. GRAFİK VE ANALİZ PANELLERİ -->
      <div
        v-if="widgets.grafikler.gorunur"
        class="charts-row"
      >
        <Card>
          <template #title>
            <i
              class="pi pi-chart-pie"
              style="margin-right: 8px"
            />Cari Bakiye Dağılımı
          </template>
          <template #content>
            <div
              v-if="bakiyeChart.datasets.length"
              class="chart-wrapper"
            >
              <Doughnut
                :data="bakiyeChart"
                :options="pieOptions"
              />
            </div>
            <div class="chart-summary">
              <span class="dot pos" /> Alacak: {{ formatCurrency(dashboardStore?.pozitifBakiye || 0) }}
              <span class="dot neg" /> Borç: {{ formatCurrency(Math.abs(dashboardStore?.negatifBakiye || 0)) }}
            </div>
          </template>
        </Card>

        <Card>
          <template #title>
            <i
              class="pi pi-chart-bar"
              style="margin-right: 8px"
            />Aylık Gelir / Gider Trendi (Son 6 Ay)
          </template>
          <template #content>
            <div
              v-if="aylikKarsilastirmaChart.datasets.length"
              class="chart-wrapper"
            >
              <Bar
                :data="aylikKarsilastirmaChart"
                :options="aylikKarsilastirmaOptions"
              />
            </div>
            <div
              v-else
              class="chart-empty"
            >
              Henüz gelir/gider verisi bulunmuyor
            </div>
          </template>
        </Card>
      </div>

      <!-- 4. ALT BÖLÜM: SON HAREKETLER VE ÖDEME VADELERİ -->
      <div class="bottom-grid">
        <div
          v-if="widgets.sonHareketler.gorunur"
          class="recent-transactions"
        >
          <h2>Son Finansal Hareketler</h2>
          <DataTable
            state-storage="session"
            state-key="dashboard-table-state"
            :value="dashboardStore?.sonHareketler || []"
            :rows="5"
            striped-rows
            size="small"
          >
            <Column
              field="cariHesapAd"
              header="Cari Hesap"
            >
              <template #body="s">
                <strong>{{ s.data.cariHesapAd }}</strong>
              </template>
            </Column>
            <Column
              field="tur"
              header="Tür"
              style="width: 100px"
            >
              <template #body="s">
                <span :class="['badge', s.data.tur === 'TAHSILAT' ? 'tahsilat' : 'odeme']">{{
                  s.data.tur
                }}</span>
              </template>
            </Column>
            <Column
              field="tutar"
              header="Tutar"
              style="width: 130px; text-align: right"
            >
              <template #body="s">
                <span :class="s.data.tur === 'TAHSILAT' ? 'positive' : 'negative'">
                  {{ formatCurrency(s.data.tutar) }}
                </span>
              </template>
            </Column>
            <Column
              field="hareketTarihi"
              header="Tarih"
              style="width: 110px"
            >
              <template #body="s">
                {{ formatDate(s.data.hareketTarihi) }}
              </template>
            </Column>
          </DataTable>
        </div>

        <div
          v-if="widgets.odemeVadeleri.gorunur"
          class="vade-uyarilari"
        >
          <Card class="vade-card vadesi-gecen mb-3">
            <template #title>
              <i
                class="pi pi-exclamation-triangle"
                style="color: #f87171; margin-right: 8px"
              />Vadesi Geçen Faturalar
            </template>
            <template #content>
              <div
                v-if="!dashboardStore?.vadesiGecenFaturalar?.length"
                class="reminder-empty text-xs text-muted"
              >
                <i class="pi pi-check-circle text-emerald-500 mr-1" /> Vadesi geçen fatura yok
              </div>
              <div
                v-for="f in (dashboardStore?.vadesiGecenFaturalar || []).slice(0, 4)"
                :key="f.faturaId"
                class="reminder-item"
              >
                <span class="reminder-ad">#{{ f.faturaNumarasi }} <small>{{ f.cariHesapAd }}</small></span>
                <span class="reminder-tutar negative">{{ formatCurrency(f.kalanTutar) }}</span>
              </div>
            </template>
          </Card>

          <Card class="vade-card vadesi-yaklasan">
            <template #title>
              <i
                class="pi pi-clock"
                style="color: #fbbf24; margin-right: 8px"
              />Vadesi Yaklaşan (7 gün)
            </template>
            <template #content>
              <div
                v-if="!dashboardStore?.vadesiYaklasanFaturalar?.length"
                class="reminder-empty text-xs text-muted"
              >
                <i class="pi pi-check-circle text-emerald-500 mr-1" /> Yaklaşan vade yok
              </div>
              <div
                v-for="f in (dashboardStore?.vadesiYaklasanFaturalar || []).slice(0, 4)"
                :key="f.faturaId"
                class="reminder-item"
              >
                <span class="reminder-ad">#{{ f.faturaNumarasi }} <small>{{ f.cariHesapAd }}</small></span>
                <div class="reminder-aksiyon">
                  <span class="reminder-tutar">{{ formatCurrency(f.kalanTutar) }}</span>
                  <a
                    v-if="f.cariTelefon"
                    class="whatsapp-buton"
                    :href="whatsappLink(f)"
                    target="_blank"
                    rel="noopener"
                    title="WhatsApp ile hatırlat"
                  >
                    <i class="pi pi-whatsapp" />
                  </a>
                </div>
              </div>
            </template>
          </Card>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import Skeleton from 'primevue/skeleton'
import { useDashboardStore } from '../stores/dashboardStore.js'
import { useDovizStore } from '../stores/dovizStore.js'
import { useAuthStore } from '../stores/authStore.js'
import { Doughnut, Bar } from 'vue-chartjs'
import Onboarding from '../components/Onboarding.vue'
import SaatGostergesi from '../components/SaatGostergesi.vue'
import {
  Chart as ChartJS,
  ArcElement,
  Tooltip,
  Legend,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Filler
} from 'chart.js'

ChartJS.register(ArcElement, Tooltip, Legend, CategoryScale, LinearScale, PointElement, LineElement, BarElement, Filler)

const dovizStore = useDovizStore()
const karsilamaMetni = computed(() => {
  const saat = new Date().getHours()
  if (saat < 6) return 'İyi geceler'
  if (saat < 12) return 'Günaydın'
  if (saat < 18) return 'İyi günler'
  return 'İyi akşamlar'
})
const widgetVarsayilan = () => ({
  istatistikler: { gorunur: true, etiket: 'İstatistik Kartları' },
  grafikler: { gorunur: true, etiket: 'Grafikler' },
  sonHareketler: { gorunur: true, etiket: 'Son Hareketler' },
  odemeVadeleri: { gorunur: true, etiket: 'Ödeme Vadeleri' }
})

const widgetListesi = ref(Object.entries(widgetVarsayilan()).map(([k, v]) => ({ key: k, ...v })))
const widgetAyarlariGoster = ref(false)
const widgets = reactive(widgetVarsayilan())

const kaydetWidget = () => {
  widgetListesi.value.forEach((w) => {
    widgets[w.key].gorunur = w.gorunur
  })
  widgetAyarlariGoster.value = false
  localStorage.setItem(
    'raspel_erp_widgets',
    JSON.stringify(Object.fromEntries(Object.entries(widgets).map(([k, v]) => [k, v.gorunur])))
  )
}
const iptalWidget = () => {
  widgetAyarlariGoster.value = false
  widgetListesi.value = Object.entries(widgets).map(([k, v]) => ({ key: k, ...v }))
}

const dashboardStore = useDashboardStore()
const authStore = useAuthStore()
const loading = ref(true)

const refresh = async () => {
  loading.value = true
  try {
    await Promise.all([
      dashboardStore.getDashboardData(),
      dovizStore?.kurlariYukle ? dovizStore.kurlariYukle() : Promise.resolve()
    ])
    grafikleriHesapla()
  } catch (error) {
    console.error('Dashboard yenilenirken hata:', error)
  }
  loading.value = false
}

const bakiyeChart = ref({ labels: [], datasets: [] })
const aylikKarsilastirmaChart = ref({ labels: [], datasets: [] })

const pieOptions = { responsive: true, plugins: { legend: { position: 'bottom' } } }
const aylikKarsilastirmaOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { position: 'bottom' } },
  scales: { x: { ticks: { color: '#94a3b8' } }, y: { ticks: { color: '#94a3b8', callback: (v) => formatCurrency(v) } } }
}

const bosSistem = computed(
  () =>
    (dashboardStore.toplamCariSayisi || 0) === 0 &&
    (dashboardStore.toplamStok || 0) === 0 &&
    (dashboardStore.toplamFatura || 0) === 0
)

const yedekUyarisiGoster = ref(true)

const toplamFatura = computed(() => dashboardStore.toplamFatura || 0)
const kesilenFatura = computed(() => dashboardStore.kesilenFatura || 0)
const toplamBankaBakiye = computed(() => dashboardStore.toplamBankaBakiye || 0)
const toplamKasaBakiye = computed(() => dashboardStore.toplamKasaBakiye || 0)
const toplamLikidite = computed(() => (dashboardStore.toplamBankaBakiye || 0) + (dashboardStore.toplamKasaBakiye || 0))
const toplamStok = computed(() => dashboardStore.toplamStok || 0)
const dusukStokAdet = computed(() => dashboardStore.kritikStokSayisi || 0)

const grafikleriHesapla = () => {
  bakiyeChart.value = {
    labels: ['Alacak', 'Borç'],
    datasets: [
      {
        data: [dashboardStore.pozitifBakiye || 0, Math.abs(dashboardStore.negatifBakiye) || 0],
        backgroundColor: ['#4caf50', '#f44336'],
        hoverBackgroundColor: ['#66bb6a', '#ef5350']
      }
    ]
  }

  aylikKarsilastirmayiHesapla()
}

const aylikKarsilastirmayiHesapla = () => {
  const aylikVeri = dashboardStore.aylikGelirGider || []
  if (!aylikVeri.length) {
    aylikKarsilastirmaChart.value = { labels: [], datasets: [] }
    return
  }

  aylikKarsilastirmaChart.value = {
    labels: aylikVeri.map((v) => v.ay),
    datasets: [
      {
        label: 'Gelir',
        data: aylikVeri.map((v) => v.gelir),
        backgroundColor: '#4caf50',
        borderRadius: 4
      },
      {
        label: 'Gider',
        data: aylikVeri.map((v) => v.gider),
        backgroundColor: '#f44336',
        borderRadius: 4
      }
    ]
  }
}

onMounted(async () => {
  try {
    const kayitli = JSON.parse(localStorage.getItem('raspel_erp_widgets'))
    if (kayitli) {
      Object.keys(widgets).forEach((k) => {
        if (kayitli[k] !== undefined) widgets[k].gorunur = kayitli[k]
      })
      widgetListesi.value = Object.entries(widgets).map(([k, v]) => ({ key: k, ...v }))
    }
  } catch (e) {
    /* yok */
  }

  try {
    await Promise.all([
      dashboardStore.getDashboardData(),
      dovizStore?.kurlariYukle ? dovizStore.kurlariYukle() : Promise.resolve()
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

const whatsappLink = (f) => {
  const tel = (f.cariTelefon || '').replace(/\D/g, '')
  const mesaj = `Sayın ${f.cariHesapAd || ''}, ${f.faturaNumarasi || ''} numaralı faturanızın ${formatCurrency(f.kalanTutar)} tutarında kalan ödemesi bulunmaktadır. Bilginize sunarız.`
  return `https://wa.me/${tel}?text=${encodeURIComponent(mesaj)}`
}
</script>

<style scoped>
.dashboard-container {
  padding: 0;
}
.dashboard-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  gap: 16px;
  flex-wrap: wrap;
}
.dashboard-header h1 {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
}
.dashboard-baslik-blok {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.karsilama-mesaji {
  margin: 0;
  font-size: 14px;
  color: var(--text-secondary);
}
.karsilama-mesaji strong {
  color: var(--text-primary);
}
.karsilama-sirket {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-left: 8px;
  padding: 2px 10px;
  border-radius: 20px;
  background: rgba(59, 130, 246, 0.12);
  color: #60a5fa;
  font-size: 12px;
  font-weight: 600;
}
.header-sag {
  display: flex;
  align-items: center;
  gap: 12px;
}
.dashboard-datetime {
  font-size: 13px;
  color: #94a3b8;
  white-space: nowrap;
}
.dashboard-datetime i {
  margin-right: 6px;
}
.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}
.skeleton-card {
  border-radius: 14px;
  overflow: hidden;
}

.widget-ayarlari {
  margin-bottom: 24px;
}
.widget-togglar {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}
.widget-toggle {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  cursor: pointer;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}
.stat-card {
  background: var(--bg-card);
  padding: 18px;
  border-radius: 14px;
  border: 1px solid var(--border);
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.3s;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
  position: relative;
  overflow: hidden;
}
.stat-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(180deg, #3b82f6, #2563eb);
}
.stat-card.cari::before {
  background: linear-gradient(180deg, #3b82f6, #2563eb);
}
.stat-card.finans::before {
  background: linear-gradient(180deg, #6366f1, #4f46e5);
}
.stat-card.fatura::before {
  background: linear-gradient(180deg, #10b981, #059669);
}
.stat-card.banka::before {
  background: linear-gradient(180deg, #f59e0b, #d97706);
}
.stat-card.kasa::before {
  background: linear-gradient(180deg, #14b8a6, #0d9488);
}
.stat-card.stok::before {
  background: linear-gradient(180deg, #f97316, #ea580c);
}
.stat-card.beklemede::before {
  background: linear-gradient(180deg, #ef4444, #dc2626);
}
.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
  border-color: rgba(59, 130, 246, 0.25);
}
.stat-icon {
  width: 52px;
  height: 52px;
  background: linear-gradient(135deg, #1976d2, #1565c0);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  color: white;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25);
}
.stat-icon.cari {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
}
.stat-icon.finans {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
}
.stat-icon.fatura {
  background: linear-gradient(135deg, #10b981, #059669);
}
.stat-icon.banka {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}
.stat-icon.banka-bakiye {
  background: linear-gradient(135deg, #8b5cf6, #7c3aed);
}
.stat-icon.kasa {
  background: linear-gradient(135deg, #14b8a6, #0d9488);
}
.stat-icon.stok {
  background: linear-gradient(135deg, #f97316, #ea580c);
}
.stat-icon.ticaret {
  background: linear-gradient(135deg, #22c55e, #16a34a);
}
.stat-icon.beklemede {
  background: linear-gradient(135deg, #ef4444, #dc2626);
}
.stat-icon.iade {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}
.stat-icon.devir {
  background: linear-gradient(135deg, #06b6d4, #0891b2);
}
.stat-icon.tahsilat {
  background: linear-gradient(135deg, #22c55e, #16a34a);
}
.stat-icon.odeme {
  background: linear-gradient(135deg, #ef4444, #dc2626);
}
.stat-icon.bekleyen-izin {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}
.stat-icon.calisan {
  background: linear-gradient(135deg, #22c55e, #16a34a);
}
.stat-icon.izinli {
  background: linear-gradient(135deg, #eab308, #ca8a04);
}
.stat-icon.ise-baslayacak {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
}
.stat-content {
  flex: 1;
  min-width: 0;
}
.stat-label {
  font-size: 12px;
  color: #94a3b8;
  margin: 0 0 6px;
}
.stat-value {
  font-size: 22px;
  font-weight: 700;
  margin: 0;
}
.stat-value small {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 400;
}
.stat-value.positive {
  color: #4ade80;
}
.stat-value.negative {
  color: #f87171;
}
.critical-hint {
  margin: 4px 0 0;
  font-size: 11px;
  color: #f87171;
}
.stat-sub {
  margin: 4px 0 0;
  font-size: 11px;
  color: var(--text-secondary);
}

.charts-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 18px;
  margin-bottom: 24px;
}
.chart-wrapper {
  max-width: 350px;
  margin: 0 auto;
}
.aylik-chart {
  max-width: 100%;
  height: 260px;
}
.chart-wrapper.line-chart {
  max-width: 100%;
}
.chart-summary {
  text-align: center;
  margin-top: 12px;
  font-size: 13px;
  color: #94a3b8;
  display: flex;
  justify-content: center;
  gap: 20px;
}
.chart-empty {
  text-align: center;
  padding: 30px;
  color: #64748b;
}
.dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
}
.dot.pos {
  background: #4caf50;
}
.dot.neg {
  background: #f44336;
}

.bottom-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}
.recent-transactions {
  background: var(--bg-card);
  padding: 20px;
  border-radius: 14px;
  border: 1px solid var(--border);
}
.recent-transactions h2 {
  margin: 0 0 12px;
  font-size: 18px;
}

.reminder-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.vade-uyarilari {
  margin-bottom: 24px;
}
.vade-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 18px;
}
.vade-card .p-card-title {
  font-size: 14px !important;
}
.reminder-item small {
  color: var(--text-muted);
  margin-left: 6px;
}
.reminder-card .p-card-title {
  font-size: 14px !important;
}
.reminder-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid var(--border);
}
.reminder-aksiyon {
  display: flex;
  align-items: center;
  gap: 10px;
}
.whatsapp-buton {
  color: #25d366;
  font-size: 18px;
  display: inline-flex;
}
.whatsapp-buton:hover {
  transform: scale(1.15);
}
.reminder-item:last-child {
  border-bottom: none;
}
.reminder-ad {
  font-size: 13px;
}
.reminder-tutar {
  font-size: 13px;
  font-weight: 600;
  color: #4ade80;
}
.reminder-tutar.negative {
  color: #f87171;
}
.reminder-empty {
  text-align: center;
  padding: 12px;
  color: #4ade80;
  font-size: 13px;
}
.reminder-empty i {
  display: block;
  font-size: 22px;
  margin-bottom: 4px;
}

.badge {
  padding: 2px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
}
.badge.tahsilat {
  background: rgba(76, 175, 80, 0.15);
  color: #4ade80;
}
.badge.odeme {
  background: rgba(244, 67, 54, 0.15);
  color: #f87171;
}
.positive {
  color: #4caf50;
  font-weight: bold;
}
.negative {
  color: #f44336;
  font-weight: bold;
}

.section-title {
  font-size: 16px;
  margin: 24px 0 12px;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}
.nakit-akisi {
  margin-bottom: 24px;
}
.nakit-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 14px;
}
.nakit-kart .p-card-title {
  font-size: 13px !important;
}
.nakit-kart .p-card-content {
  padding-top: 0 !important;
}
.nakit-deger {
  font-size: 20px;
  font-weight: 700;
  margin: 0;
}
.nakit-alt {
  font-size: 11px;
  color: var(--text-muted);
  margin: 4px 0 0;
}
.nakit-toplam {
  border-color: rgba(245, 158, 11, 0.3) !important;
}
.son-goruntulenenler {
  margin-bottom: 24px;
}
.sg-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 10px;
}
.sg-item {
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 10px 14px;
  cursor: pointer;
  transition: all 0.15s;
}
.sg-item:hover {
  border-color: rgba(59, 130, 246, 0.3);
  transform: translateY(-2px);
}
.sg-item i {
  font-size: 18px;
}
.sg-bilgi {
  flex: 1;
  min-width: 0;
}
.sg-bilgi strong {
  display: block;
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.sg-bilgi small {
  display: block;
  font-size: 11px;
  color: var(--text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.hizli-islemler {
  margin-bottom: 24px;
}
.islem-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 12px;
}
.islem-grid .p-button {
  height: 56px;
  font-size: 13px;
  justify-content: center;
}

@media (max-width: 900px) {
  .charts-row,
  .bottom-grid {
    grid-template-columns: 1fr;
  }
}

.doviz-ticker-compact {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--bg-card, rgba(255, 255, 255, 0.05));
  border: 1px solid var(--border, rgba(255, 255, 255, 0.12));
  border-radius: 20px;
  padding: 4px 10px;
  font-size: 11px;
  flex-wrap: wrap;
}

.ticker-chip {
  display: flex;
  align-items: center;
  gap: 4px;
}

.chip-kod {
  font-weight: 700;
  color: var(--text-secondary, #94a3b8);
}

.chip-fiyat {
  font-weight: 600;
  color: #10b981;
}

.chip-refresh-btn {
  background: transparent;
  border: none;
  color: var(--text-muted, #64748b);
  cursor: pointer;
  padding: 2px 4px;
  display: flex;
  align-items: center;
  transition: color 0.15s;
}

.chip-refresh-btn:hover {
  color: #3b82f6;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
  margin-bottom: 24px;
}
.action-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 18px;
  border-radius: 12px;
  text-decoration: none;
  color: white;
  font-weight: 600;
  font-size: 13.5px;
  transition:
    transform 0.15s,
    box-shadow 0.15s;
}
.action-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}
.action-card i {
  font-size: 1.3rem;
}
.action-card.kokpit {
  background: linear-gradient(135deg, #d97706, #b45309);
}
.action-card.teklif {
  background: linear-gradient(135deg, #2563eb, #3b82f6);
}
.action-card.saha {
  background: linear-gradient(135deg, #7c3aed, #8b5cf6);
}
.action-card.fatura {
  background: linear-gradient(135deg, #4f46e5, #6366f1);
}
.action-card.satis {
  background: linear-gradient(135deg, #059669, #10b981);
}
.action-card.cari {
  background: linear-gradient(135deg, #d97706, #f59e0b);
}
.action-card.tahsilat {
  background: linear-gradient(135deg, #dc2626, #ef4444);
}

.backup-reminder {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  margin-bottom: 20px;
  background: var(--yellow-50, #fefce8);
  border: 1px solid var(--yellow-200, #fef08a);
  border-radius: 10px;
  font-size: 14px;
  color: var(--yellow-800, #854d0e);
}
.backup-reminder a {
  color: var(--blue-600, #2563eb);
  font-weight: 600;
}
.reminder-close {
  margin-left: auto;
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: var(--yellow-600);
}

@media (max-width: 768px) {
  .quick-actions {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
