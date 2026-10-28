import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useChartTema } from './useChartTema.js'
import { formatCurrency } from '../utils/format.js'

/**
 * Anasayfa grafiklerinin Chart.js veri/opsiyonlarini uretir.
 *
 * Kaynak: `GET /dashboard/grafikler` payload'i (reactive ref ya da computed).
 * Renkler `useChartTema` paletinden gelir; koyu/acik tema degisince eksen,
 * izgara ve lejant renkleri de uyum saglar.
 */
export function useDashboardGrafikleri(kaynak, { nakitGun } = {}) {
  const { t } = useI18n()
  const { palet, lejant } = useChartTema()

  const veri = computed(() => {
    const ham = kaynak && 'value' in kaynak ? kaynak.value : kaynak
    return ham || {}
  })

  const paraTick = (v) => formatCurrency(v)

  // Bar/line veri setleri icin dikey/yatay degrade yardimcilari. Chart alani
  // henuz olusmadiysa duz renk doner (Chart.js ilk cizimde tekrar cagirir).
  const dikeyGradyan = (ctx, ust, alt) => {
    const { chart } = ctx
    const { ctx: c, chartArea } = chart
    if (!chartArea) return ust
    const g = c.createLinearGradient(0, chartArea.bottom, 0, chartArea.top)
    g.addColorStop(0, alt)
    g.addColorStop(1, ust)
    return g
  }

  const yatayGradyan = (ctx, sol, sag) => {
    const { chart } = ctx
    const { ctx: c, chartArea } = chart
    if (!chartArea) return sol
    const g = c.createLinearGradient(chartArea.left, 0, chartArea.right, 0)
    g.addColorStop(0, sol)
    g.addColorStop(1, sag)
    return g
  }

  const eksenler = computed(() => ({
    x: { ticks: { color: palet.value.metin }, grid: { display: false } },
    y: { ticks: { color: palet.value.metin, callback: paraTick }, grid: { color: palet.value.izgara } }
  }))

  const yatayEksenler = computed(() => ({
    x: { ticks: { color: palet.value.metin, callback: paraTick }, grid: { color: palet.value.izgara } },
    y: { ticks: { color: palet.value.metin }, grid: { display: false } }
  }))

  const temelOpsiyonlar = computed(() => ({
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: lejant() }
  }))

  const donutOpsiyonlar = computed(() => ({
    responsive: true,
    maintainAspectRatio: false,
    cutout: '62%',
    plugins: { legend: lejant() }
  }))

  const gunAnahtari = (tarih) =>
    `${tarih.getFullYear()}-${String(tarih.getMonth() + 1).padStart(2, '0')}-${String(tarih.getDate()).padStart(2, '0')}`
  const gunEtiketi = (tarih) =>
    `${String(tarih.getDate()).padStart(2, '0')}.${String(tarih.getMonth() + 1).padStart(2, '0')}`

  /**
   * 30 gunluk nakit akisi (alan grafik). Backend yalnizca hareket olan
   * gunleri dondurur; grafikteki zaman ekseni kopuk olmasin diye son N gun
   * sifir degerleriyle tamamlanir. Hic hareket yoksa bos doner.
   */
  const gunlukNakitAkisi = computed(() => {
    const tumGunler = Array.isArray(veri.value.gunlukNakitAkisi) ? veri.value.gunlukNakitAkisi : []
    const istenen = nakitGun && 'value' in nakitGun ? Number(nakitGun.value) : Number(nakitGun)
    const gunSayisi = Number.isFinite(istenen) && istenen > 0 ? Math.floor(istenen) : 30
    if (!tumGunler.length) return { labels: [], datasets: [] }

    const harita = new Map(tumGunler.map((g) => [String(g.gun || ''), g]))
    const gunler = []
    const bugun = new Date()
    for (let i = gunSayisi - 1; i >= 0; i -= 1) {
      const tarih = new Date(bugun.getFullYear(), bugun.getMonth(), bugun.getDate() - i)
      const kayit = harita.get(gunAnahtari(tarih)) || {}
      gunler.push({
        etiket: gunEtiketi(tarih),
        gelir: Number(kayit.gelir) || 0,
        gider: Number(kayit.gider) || 0
      })
    }
    return {
      labels: gunler.map((g) => g.etiket),
      datasets: [
        {
          label: t('dashboard.etiketGelir'),
          data: gunler.map((g) => Number(g.gelir) || 0),
          borderColor: '#10b981',
          backgroundColor: (ctx) => dikeyGradyan(ctx, 'rgba(16, 185, 129, 0.35)', 'rgba(16, 185, 129, 0)'),
          fill: true,
          tension: 0.35,
          borderWidth: 2.5,
          pointRadius: 0,
          pointHoverRadius: 4
        },
        {
          label: t('dashboard.etiketGider'),
          data: gunler.map((g) => Number(g.gider) || 0),
          borderColor: '#f43f5e',
          backgroundColor: (ctx) => dikeyGradyan(ctx, 'rgba(244, 63, 94, 0.3)', 'rgba(244, 63, 94, 0)'),
          fill: true,
          tension: 0.35,
          borderWidth: 2.5,
          pointRadius: 0,
          pointHoverRadius: 4
        }
      ]
    }
  })

  const gunlukNakitAkisiOpsiyonlari = computed(() => ({
    ...temelOpsiyonlar.value,
    plugins: {
      ...temelOpsiyonlar.value.plugins,
      tooltip: { callbacks: { label: (ctx) => `${ctx.dataset.label}: ${formatCurrency(ctx.parsed.y)}` } }
    },
    scales: eksenler.value
  }))

  const ayAnahtari = (tarih) =>
    `${tarih.getFullYear()}-${String(tarih.getMonth() + 1).padStart(2, '0')}`
  const ayEtiketi = (anahtar) => {
    const [yil, ay] = String(anahtar).split('-')
    return `${ay}/${yil.slice(2)}`
  }

  /**
   * 12 aylik gelir/gider (bar) + net (line). Backend yalnizca hareket olan
   * aylari dondurur; seri son 12 aya tamamlanir ki tek aylik veri bile
   * zaman ekseninde dogru yerde gorunsun.
   */
  const aylikGelirGider = computed(() => {
    const aylar = Array.isArray(veri.value.aylikGelirGider) ? veri.value.aylikGelirGider : []
    if (!aylar.length) return { labels: [], datasets: [] }

    const harita = new Map(aylar.map((a) => [String(a.ay || ''), a]))
    const seri = []
    const bugun = new Date()
    for (let i = 11; i >= 0; i -= 1) {
      const tarih = new Date(bugun.getFullYear(), bugun.getMonth() - i, 1)
      const anahtar = ayAnahtari(tarih)
      const kayit = harita.get(anahtar) || {}
      seri.push({
        ay: anahtar,
        gelir: Number(kayit.gelir) || 0,
        gider: Number(kayit.gider) || 0
      })
    }
    return {
      labels: seri.map((a) => ayEtiketi(a.ay)),
      datasets: [
        {
          label: t('dashboard.etiketGelir'),
          data: seri.map((a) => a.gelir),
          backgroundColor: (ctx) => dikeyGradyan(ctx, '#34d399', 'rgba(16, 185, 129, 0.25)'),
          hoverBackgroundColor: '#6ee7b7',
          borderRadius: 8,
          maxBarThickness: 26
        },
        {
          label: t('dashboard.etiketGider'),
          data: seri.map((a) => a.gider),
          backgroundColor: (ctx) => dikeyGradyan(ctx, '#fb7185', 'rgba(244, 63, 94, 0.25)'),
          hoverBackgroundColor: '#fda4af',
          borderRadius: 8,
          maxBarThickness: 26
        },
        {
          type: 'line',
          label: t('dashboard.net'),
          data: seri.map((a) => a.gelir - a.gider),
          borderColor: '#a78bfa',
          backgroundColor: '#a78bfa',
          borderWidth: 2.5,
          pointRadius: 3,
          pointHoverRadius: 5,
          tension: 0.35
        }
      ]
    }
  })

  const aylikGelirGiderOpsiyonlari = computed(() => ({
    ...temelOpsiyonlar.value,
    plugins: {
      ...temelOpsiyonlar.value.plugins,
      tooltip: { callbacks: { label: (ctx) => `${ctx.dataset.label}: ${formatCurrency(ctx.parsed.y)}` } }
    },
    scales: eksenler.value
  }))

  const RENKLER = ['#3b82f6', '#10b981', '#f59e0b', '#8b5cf6', '#ec4899', '#06b6d4', '#f97316', '#84cc16']

  // Kategori satislari (donut)
  const kategoriSatislari = computed(() => {
    const kategoriler = Array.isArray(veri.value.kategoriSatislari) ? veri.value.kategoriSatislari : []
    if (!kategoriler.length) return { labels: [], datasets: [] }
    return {
      labels: kategoriler.map((k) => k.kategori),
      datasets: [
        {
          data: kategoriler.map((k) => Number(k.tutar) || 0),
          backgroundColor: kategoriler.map((_, i) => RENKLER[i % RENKLER.length]),
          borderWidth: 0,
          hoverOffset: 8
        }
      ]
    }
  })

  // En cok satan urunler (yatay bar)
  const enCokSatanlar = computed(() => {
    const urunler = Array.isArray(veri.value.enCokSatanlar) ? veri.value.enCokSatanlar.slice(0, 5) : []
    if (!urunler.length) return { labels: [], datasets: [] }
    return {
      labels: urunler.map((u) => u.stokAd || u.stokKodu || ''),
      datasets: [
        {
          label: t('dashboard.satisMiktari'),
          data: urunler.map((u) => Number(u.satisMiktari) || 0),
          backgroundColor: (ctx) => {
            const renk = RENKLER[ctx.dataIndex % RENKLER.length]
            return yatayGradyan(ctx, renk, `${renk}55`)
          },
          borderRadius: 8,
          maxBarThickness: 20
        }
      ]
    }
  })

  const enCokSatanlarOpsiyonlari = computed(() => ({
    indexAxis: 'y',
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      x: { ticks: { color: palet.value.metin }, grid: { color: palet.value.izgara } },
      y: { ticks: { color: palet.value.metin }, grid: { display: false } }
    }
  }))

  // Alacak yaslandirma
  const alacakYaslandirma = computed(() => {
    const yaslar = Array.isArray(veri.value.alacakYaslandirma) ? veri.value.alacakYaslandirma : []
    if (!yaslar.length) return { labels: [], datasets: [] }
    return {
      labels: yaslar.map((y) => y.aralik),
      datasets: [
        {
          label: t('dashboard.kalanTutarTL'),
          data: yaslar.map((y) => Number(y.tutar) || 0),
          backgroundColor: ['#fb7185', '#fbbf24', '#60a5fa', '#a78bfa'],
          borderRadius: 8,
          maxBarThickness: 46
        }
      ]
    }
  })

  const alacakYaslandirmaOpsiyonlari = computed(() => ({
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: eksenler.value
  }))

  const carileriHazirla = (liste, etiket, renk) => {
    const cariler = Array.isArray(liste) ? liste.slice(0, 7) : []
    if (!cariler.length) return { labels: [], datasets: [] }
    return {
      labels: cariler.map((c) => c.cariAd),
      datasets: [
        {
          label: etiket,
          data: cariler.map((c) => Number(c.tutar) || 0),
          backgroundColor: (ctx) => yatayGradyan(ctx, renk, `${renk}4d`),
          borderRadius: 8,
          maxBarThickness: 20
        }
      ]
    }
  }

  const enCokBorcCariler = computed(() =>
    carileriHazirla(veri.value.enCokBorcCariler, t('dashboard.borcTL'), '#fb7185')
  )
  const enCokAlacakCariler = computed(() =>
    carileriHazirla(veri.value.enCokAlacakCariler, t('dashboard.alacakTL'), '#34d399')
  )

  const cariOpsiyonlari = computed(() => ({
    indexAxis: 'y',
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: yatayEksenler.value
  }))

  // Kasa / banka (donut)
  const kasaBanka = computed(() => {
    const kasa = Number(veri.value.kasaBanka?.kasa) || 0
    const banka = Number(veri.value.kasaBanka?.banka) || 0
    if (!kasa && !banka) return { labels: [], datasets: [] }
    return {
      labels: [t('dashboard.kasa'), t('dashboard.etiketBanka')],
      datasets: [
        {
          data: [kasa, banka],
          backgroundColor: ['#f59e0b', '#3b82f6'],
          hoverBackgroundColor: ['#fbbf24', '#60a5fa'],
          borderWidth: 0,
          hoverOffset: 8
        }
      ]
    }
  })

  // Kategori Pareto (bar + kumulatif %)
  const kategoriPareto = computed(() => {
    const liste = (Array.isArray(veri.value.kategoriSatislari) ? [...veri.value.kategoriSatislari] : [])
      .map((k) => ({ kategori: k.kategori, tutar: Number(k.tutar) || 0 }))
      .sort((a, b) => b.tutar - a.tutar)
    if (!liste.length) return { labels: [], datasets: [] }
    const toplam = liste.reduce((s, k) => s + k.tutar, 0)
    let kumulatif = 0
    const yuzdeler = liste.map((k) => {
      kumulatif += k.tutar
      return toplam > 0 ? Math.round((kumulatif / toplam) * 1000) / 10 : 0
    })
    return {
      labels: liste.map((k) => k.kategori),
      datasets: [
        {
          label: t('dashboard.toplam'),
          data: liste.map((k) => k.tutar),
          backgroundColor: (ctx) => dikeyGradyan(ctx, '#a78bfa', 'rgba(139, 92, 246, 0.25)'),
          hoverBackgroundColor: '#c4b5fd',
          borderRadius: 8,
          maxBarThickness: 34,
          yAxisID: 'y'
        },
        {
          type: 'line',
          label: t('dashboard.kumulatif'),
          data: yuzdeler,
          borderColor: '#f59e0b',
          backgroundColor: '#f59e0b',
          pointRadius: 3,
          tension: 0.3,
          yAxisID: 'y1'
        }
      ]
    }
  })

  const kategoriParetoOpsiyonlari = computed(() => ({
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: lejant() },
    scales: {
      x: { ticks: { color: palet.value.metin }, grid: { display: false } },
      y: {
        position: 'left',
        ticks: { color: palet.value.metin, callback: paraTick },
        grid: { color: palet.value.izgara }
      },
      y1: {
        position: 'right',
        min: 0,
        max: 100,
        grid: { drawOnChartArea: false },
        ticks: { color: '#f59e0b', callback: (v) => `${v}%` }
      }
    }
  }))

  // Ciro hedef trendi
  const ciroHedef = computed(() => {
    const aylar = Array.isArray(veri.value.aylikGelirGider) ? veri.value.aylikGelirGider : []
    if (!aylar.length) return { labels: [], datasets: [] }
    const hedef = Number(veri.value.hedef?.ciro) || 0
    return {
      labels: aylar.map((a) => a.ay),
      datasets: [
        {
          label: t('dashboard.etiketGelir'),
          data: aylar.map((a) => Number(a.gelir) || 0),
          backgroundColor: (ctx) => dikeyGradyan(ctx, '#60a5fa', 'rgba(59, 130, 246, 0.25)'),
          hoverBackgroundColor: '#93c5fd',
          borderRadius: 8,
          maxBarThickness: 34,
          order: 2
        },
        {
          type: 'line',
          label: t('dashboard.hedefCizgi'),
          data: aylar.map(() => hedef),
          borderColor: '#f59e0b',
          borderDash: [6, 4],
          borderWidth: 2,
          pointRadius: 0,
          fill: false,
          order: 1
        }
      ]
    }
  })

  const ciroHedefOpsiyonlari = computed(() => ({
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: lejant() },
    scales: eksenler.value
  }))

  return {
    gunlukNakitAkisi,
    gunlukNakitAkisiOpsiyonlari,
    aylikGelirGider,
    aylikGelirGiderOpsiyonlari,
    kategoriSatislari,
    donutOpsiyonlar,
    enCokSatanlar,
    enCokSatanlarOpsiyonlari,
    alacakYaslandirma,
    alacakYaslandirmaOpsiyonlari,
    enCokBorcCariler,
    enCokAlacakCariler,
    cariOpsiyonlari,
    kasaBanka,
    kategoriPareto,
    kategoriParetoOpsiyonlari,
    ciroHedef,
    ciroHedefOpsiyonlari
  }
}
