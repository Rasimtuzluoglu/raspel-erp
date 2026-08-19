import { defineStore } from 'pinia'
import { ref } from 'vue'
import { dashboardAPI } from '../api/index.js'

export const useDashboardStore = defineStore('dashboard', () => {
  const toplamCariSayisi = ref(0)
  const toplamBakiye = ref(0)
  const sonHareketler = ref([])
  const aktifCalisan = ref(0)
  const bugunIzinli = ref(0)
  const buAyIseBaslayacak = ref(0)
  const bugunkuSiparis = ref(0)
  const bekleyenTeslimat = ref(0)
  const iadeOrani = ref(0)
  const stokDevirHizi = ref(0)
  const enCokSatanlar = ref([])
  const pozitifBakiye = ref(0)
  const negatifBakiye = ref(0)
  const bugunkuTahsilat = ref(0)
  const bugunkuOdeme = ref(0)
  const bekleyenIzinSayisi = ref(0)
  const aylikGelirGider = ref([])
  const vadesiGecenFaturalar = ref([])
  const vadesiYaklasanFaturalar = ref([])
  const loading = ref(false)
  const error = ref(null)

  const getDashboardData = async () => {
    loading.value = true
    error.value = null
    try {
      const response = await dashboardAPI.getData()
      const d = response.data
      toplamCariSayisi.value = d.toplamCariSayisi || 0
      toplamBakiye.value = d.toplamBakiye || 0
      sonHareketler.value = d.sonHareketler || []
      aktifCalisan.value = d.aktifCalisan || 0
      bugunIzinli.value = d.bugunIzinli || 0
      buAyIseBaslayacak.value = d.buAyIseBaslayacak || 0
      bugunkuSiparis.value = d.bugunkuSiparis || 0
      bekleyenTeslimat.value = d.bekleyenTeslimat || 0
      iadeOrani.value = d.iadeOrani || 0
      stokDevirHizi.value = d.stokDevirHizi || 0
      enCokSatanlar.value = d.enCokSatanlar || []
      pozitifBakiye.value = d.pozitifBakiye || 0
      negatifBakiye.value = d.negatifBakiye || 0
      bugunkuTahsilat.value = d.bugunkuTahsilat || 0
      bugunkuOdeme.value = d.bugunkuOdeme || 0
      bekleyenIzinSayisi.value = d.bekleyenIzinSayisi || 0
      aylikGelirGider.value = d.aylikGelirGider || []
      vadesiGecenFaturalar.value = d.vadesiGecenFaturalar || []
      vadesiYaklasanFaturalar.value = d.vadesiYaklasanFaturalar || []
      return d
    } catch (err) {
      error.value = err.message
      console.error('Dashboard verileri yüklenirken hata:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  return {
    toplamCariSayisi,
    toplamBakiye,
    sonHareketler,
    aktifCalisan,
    bugunIzinli,
    buAyIseBaslayacak,
    bugunkuSiparis,
    bekleyenTeslimat,
    iadeOrani,
    stokDevirHizi,
    enCokSatanlar,
    pozitifBakiye,
    negatifBakiye,
    bugunkuTahsilat,
    bugunkuOdeme,
    bekleyenIzinSayisi,
    aylikGelirGider,
    vadesiGecenFaturalar,
    vadesiYaklasanFaturalar,
    loading,
    error,
    getDashboardData
  }
})
