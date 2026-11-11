import { ref, computed, watch, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useConfirm } from 'primevue/useconfirm'
import { useToastBildirim } from './useToastBildirim.js'
import { useCariOnerileri, cariHesapCoz } from './useCariOnerileri.js'
import { useTaslakKayit } from './useTaslakKayit.js'
import { useFaturaStore } from '../stores/faturaStore.js'
import { useCariHesapStore } from '../stores/cariHesapStore.js'
import { faturaAPI, depoAPI, personelAPI, teslimatAPI, siparisAPI, irsaliyeAPI, teklifAPI, stokAPI, pdfAPI, eFaturaAPI } from '../api/index.js'
import { unwrapList } from '../api/utils/unwrap.js'
import { satisPayloadUret } from '../utils/satisPayload.js'
import { kalemNetTutar, kalemKdv, kdvOrani } from '../utils/faturaHesapla.js'
import { getLocalDateString } from '../utils/format.js'

// Tek ekran fatura formunun durumu ve is akisi. Liste ekranindan bagimsizdir;
// FaturaKalemleri bileseninin props/emit sozlesmesi degismeden kullanilir.

export const bosFaturaFormu = () => ({
  cariHesapId: null,
  tur: '',
  tarih: new Date(),
  teslimEden: '',
  teslimDurumu: 'BEKLIYOR',
  teslimNotu: '',
  depoId: null,
  paraBirimi: 'TRY',
  kur: null,
  siparisId: null,
  irsaliyeId: null,
  aciklama: '',
  driverId: null,
  beklenenTeslimTarihi: null,
  teslimatAdresi: '',
  teslimatNotu: '',
  kalemler: []
})

const faturadanForm = (f) => ({
  cariHesapId: f.cariHesapId,
  tur: f.tur,
  tarih: f.tarih ? new Date(f.tarih) : new Date(),
  teslimEden: f.teslimEden || '',
  teslimDurumu: f.teslimDurumu || 'BEKLIYOR',
  teslimNotu: f.teslimNotu || '',
  depoId: f.depoId || null,
  paraBirimi: f.paraBirimi || 'TRY',
  kur: f.kur ?? null,
  siparisId: f.siparisId ?? null,
  irsaliyeId: f.irsaliyeId ?? null,
  aciklama: f.aciklama || '',
  driverId: null,
  beklenenTeslimTarihi: null,
  teslimatAdresi: '',
  teslimatNotu: '',
  kalemler: (f.kalemler || []).map((k) => ({
    id: k.id,
    aciklama: k.aciklama || '',
    adet: k.adet ?? 1,
    birimFiyat: k.birimFiyat ?? 0,
    // Bos iskonto = sunucu indirim motoru; 0 = elle sifir. Eski 0 varsayilani
    // motoru devre disi birakiyordu.
    iskontoOrani: k.iskontoOrani ?? null,
    kdvOrani: kdvOrani(k),
    stokId: k.stokId ?? null
  }))
})

const kopyalananForm = (f) => {
  const form = faturadanForm(f)
  return {
    ...form,
    // Kopya yeni bir faturadir; kaynak baglari tasinmaz.
    siparisId: null,
    irsaliyeId: null,
    // Kopyada kalem kimlikleri tasinmaz; aksi halde update sanilir.
    kalemler: form.kalemler.map((k) => ({
      aciklama: k.aciklama,
      adet: k.adet,
      birimFiyat: k.birimFiyat,
      iskontoOrani: k.iskontoOrani,
      kdvOrani: k.kdvOrani,
      stokId: k.stokId
    }))
  }
}

export function useFaturaForm() {
  const route = useRoute()
  const router = useRouter()
  const { t } = useI18n()
  const toast = useToastBildirim()
  const confirm = useConfirm()
  const faturaStore = useFaturaStore()
  const cariHesapStore = useCariHesapStore()

  // Faz C: depo / personel / sofor listeleri (tek ekranda secim icin).
  const depolar = ref([])
  const personelListesi = ref([])
  const suruculer = ref([])

  const faturaId = ref(route.params.id ? Number(route.params.id) : null)
  const kopyaId = ref(route.query.kopya ? Number(route.query.kopya) : null)

  const form = ref(bosFaturaFormu())
  const seciliCariNesnesi = ref(null)
  const mevcutFatura = ref(null)
  const yukleniyor = ref(false)
  const yuklemeHatasi = ref('')
  const saving = ref(false)
  const hata = ref('')
  const kaydedildi = ref(false)
  const kirli = ref(false)
  const hazir = ref(false)
  // Kaydetme sonrasi aksiyonlar (PDF/e-fatura) icin son kaydedilen kayit kimligi.
  const sonKaydedilenId = ref(null)
  // C5: kaydedilmis faturadaki e-fatura belgesi (varsa).
  const eFatura = ref(null)
  const efaturaIslem = ref(false)

  const { ara: cariAra, oneriler: cariOnerileri, yukleniyor: cariOnerileriYukleniyor, hemenAra: cariHemenAra } =
    useCariOnerileri()

  const taslakAnahtari = faturaId.value ? `fatura_duzenle_${faturaId.value}` : 'fatura_yeni'
  const taslakGeriYuklendi = ref(false)
  const { temizle: taslakTemizle } = useTaslakKayit(taslakAnahtari, form, {
    // Duzenleme/kopya modunda sunucu verisi esastir; taslak yalnizca yeni faturada.
    restore: !faturaId.value && !kopyaId.value,
    onRestore: () => {
      taslakGeriYuklendi.value = true
      // localStorage tarihleri ISO string olarak doner; DatePicker icin normalize et.
      if (typeof form.value.tarih === 'string') form.value.tarih = new Date(form.value.tarih)
    }
  })

  const araToplam = computed(() => form.value.kalemler.reduce((t, k) => t + kalemNetTutar(k), 0))
  const kdvToplam = computed(() => form.value.kalemler.reduce((t, k) => t + kalemKdv(k), 0))
  const genelToplam = computed(() => araToplam.value + kdvToplam.value)

  const duzenlemeMi = computed(() => !!faturaId.value)

  /**
   * IPTAL ve odemesi olan KESILDI faturalar backend'de guncellenemez; ekran bunu
   * kaydetmeden once kapatir.
   */
  const duzenlenebilir = computed(() => {
    if (!duzenlemeMi.value) return true
    const durum = mevcutFatura.value?.durum
    if (durum === 'IPTAL') return false
    if (durum === 'KESILDI' && Number(mevcutFatura.value?.odenenTutar || 0) > 0) return false
    return true
  })

  const kilitNedeni = computed(() => {
    if (duzenlenebilir.value) return ''
    if (mevcutFatura.value?.durum === 'IPTAL') return t('faturalar.duzenlenemezIptal')
    return t('faturalar.duzenlenemezOdeme')
  })

  const sayfaBasligi = computed(() => {
    if (duzenlemeMi.value) {
      return mevcutFatura.value?.faturaNumarasi
        ? `${t('faturalar.dialogDuzenle')} · ${mevcutFatura.value.faturaNumarasi}`
        : t('faturalar.dialogDuzenle')
    }
    return kopyaId.value ? t('faturalar.faturaCogalt') : t('faturalar.yeniFatura')
  })

  const depoAdi = computed(() => {
    if (mevcutFatura.value?.depoAd) return mevcutFatura.value.depoAd
    const depo = depolar.value.find((d) => d.id === form.value.depoId)
    return depo?.ad || ''
  })

  const personelSecenekleri = computed(() =>
    personelListesi.value
      .filter((p) => p.aktif !== false)
      .map((p) => `${p.ad || ''} ${p.soyad || ''}`.trim())
      .filter(Boolean)
  )

  const teslimatVar = computed(() => !!mevcutFatura.value?.teslimatVar)
  // Revizyonda depo/kur degisimi backend'de guvenli islenmiyor; duzenlemede kilitli.
  const depoKilitli = computed(() => duzenlemeMi.value)
  const kurKilitli = computed(() => duzenlemeMi.value || form.value.paraBirimi === 'TRY')

  const listeleriYukle = async () => {
    const [depoSonuc, personelSonuc, soforSonuc] = await Promise.allSettled([
      depoAPI.getAll({ size: 500 }),
      personelAPI.getAll({ size: 500 }),
      teslimatAPI.suruculer()
    ])
    if (depoSonuc.status === 'fulfilled') {
      depolar.value = unwrapList(depoSonuc.value).filter((d) => d?.aktif !== false)
    }
    if (personelSonuc.status === 'fulfilled') {
      personelListesi.value = unwrapList(personelSonuc.value)
    }
    if (soforSonuc.status === 'fulfilled') {
      suruculer.value = soforSonuc.value?.data || []
    }
  }

  // C4: siparis / irsaliye / teklif kaynagindan kalem doldurma.
  const kaynaklar = ref([])
  const kaynaklarYukleniyor = ref(false)

  const kaynakYukle = async (tur) => {
    kaynaklarYukleniyor.value = true
    kaynaklar.value = []
    try {
      if (tur === 'IRSALIYE') {
        const r = await irsaliyeAPI.filtreli({ durum: 'KESILDI', page: 0, size: 100 })
        kaynaklar.value = unwrapList(r).filter((i) => !i.faturaId)
      } else if (tur === 'TEKLIF') {
        const r = await teklifAPI.getAll({ page: 0, size: 100 })
        kaynaklar.value = unwrapList(r).filter((t) => t.durum !== 'FATURALASTI' && t.durum !== 'IPTAL')
      } else {
        const r = await siparisAPI.getAll({ page: 0, size: 100 })
        kaynaklar.value = unwrapList(r).filter((s) => s.durum !== 'FATURA_KESILDI' && s.durum !== 'IPTAL')
      }
    } catch {
      kaynaklar.value = []
      toast.hata(t('faturalar.kaynakHata'))
    } finally {
      kaynaklarYukleniyor.value = false
    }
  }

  const kaynaktanDoldur = async (tur, id) => {
    if (!id) return false
    try {
      const detay =
        tur === 'IRSALIYE'
          ? (await irsaliyeAPI.getById(id)).data
          : tur === 'TEKLIF'
            ? (await teklifAPI.getById(id)).data
            : (await siparisAPI.getById(id)).data
      if (!detay) throw new Error()

      if (tur === 'IRSALIYE' && detay.faturaId) {
        toast.uyari(t('faturalar.kaynakFaturaVar'))
        return false
      }
      if (form.value.kalemler.length) {
        const devam = window.confirm(t('faturalar.kaynakUzerineYaz'))
        if (!devam) return false
      }

      const cariId = detay.cariHesapId
      form.value.cariHesapId = cariId
      await cariyiCoz(cariId, detay.cariHesapAdi)
      form.value.tur = 'SATIS'
      if (tur === 'IRSALIYE' && detay.depoId) form.value.depoId = detay.depoId
      if (tur === 'TEKLIF' && detay.paraBirimi) form.value.paraBirimi = detay.paraBirimi
      form.value.siparisId = tur === 'SIPARIS' ? detay.id : detay.siparisId || null
      form.value.irsaliyeId = tur === 'IRSALIYE' ? detay.id : null

      let stokHaritasi = new Map()
      if (tur === 'IRSALIYE') {
        const stokIdler = [...new Set((detay.kalemler || []).map((k) => k.stokId).filter(Boolean))]
        const stoklar = await Promise.all(
          stokIdler.map((sid) => stokAPI.getById(sid).then((r) => r.data).catch(() => null))
        )
        stokHaritasi = new Map(stoklar.filter(Boolean).map((s) => [s.id, s]))
      }

      const kalemler = (detay.kalemler || []).map((k) => {
        if (tur === 'IRSALIYE') {
          const stok = stokHaritasi.get(k.stokId)
          return {
            stokId: k.stokId ?? null,
            aciklama: k.aciklama || k.stokAdi || stok?.ad || '',
            adet: Number(k.miktar || 1),
            birimFiyat: Number(stok?.satisFiyati || stok?.fiyat || 0),
            iskontoOrani: null,
            kdvOrani: kdvOrani(stok || {})
          }
        }
        return {
          stokId: k.stokId ?? null,
          aciklama: k.aciklama || k.stokAdi || k.stokKodu || '',
          adet: Number(k.miktar ?? k.adet ?? 1),
          birimFiyat: Number(k.birimFiyat || 0),
          iskontoOrani: k.iskontoOrani ?? null,
          kdvOrani: kdvOrani(k)
        }
      })
      if (!kalemler.length) {
        toast.uyari(t('faturalar.kaynakKalemYok'))
        return false
      }
      form.value.kalemler = kalemler
      toast.basarili(t('faturalar.kaynakDolduruldu'))
      return true
    } catch {
      toast.hata(t('faturalar.kaynakHata'))
      return false
    }
  }

  const cariSecildi = (event) => {
    form.value.cariHesapId = event.value?.id || null
    const adres = event.value?.adres || ''
    if (adres && !String(form.value.teslimatAdresi || '').trim()) {
      form.value.teslimatAdresi = adres
    }
  }

  const kalemEkle = (kalem) => {
    form.value.kalemler.push(kalem)
  }

  const removeKalem = (index) => {
    form.value.kalemler.splice(index, 1)
  }

  const kalemGeriAl = ({ index, kalem }) => {
    const hedef = Math.min(Math.max(index, 0), form.value.kalemler.length)
    form.value.kalemler.splice(hedef, 0, kalem)
  }

  const sonFaturayiKopyala = async () => {
    if (!form.value.cariHesapId) return
    try {
      const r = await faturaAPI.cariSonFatura(form.value.cariHesapId)
      if (!r.data) {
        toast.bilgi(t('faturalar.dahaOnceFaturaYok'))
        return
      }
      const kaynak = r.data
      form.value.aciklama = kaynak.aciklama || ''
      form.value.tur = kaynak.tur || 'SATIS'
      form.value.kalemler = (kaynak.kalemler || []).map((k) => ({
        aciklama: k.aciklama || '',
        adet: k.adet || 1,
        birimFiyat: k.birimFiyat || 0,
        iskontoOrani: k.iskontoOrani ?? null,
        kdvOrani: kdvOrani(k),
        stokId: k.stokId || null
      }))
      toast.basarili(t('faturalar.sonFaturaKopyalandi'))
    } catch {
      toast.hata(t('faturalar.sonFaturaYuklenemedi'))
    }
  }

  const cariyiCoz = async (cariHesapId, ad) => {
    seciliCariNesnesi.value =
      (await cariHesapCoz(cariHesapId, cariHesapStore?.cariHesaplar)) ||
      (cariHesapId ? { id: cariHesapId, ad } : null)
  }

  const faturaYukle = async (id) => {
    yukleniyor.value = true
    yuklemeHatasi.value = ''
    try {
      const f = await faturaStore.getFaturaById(id)
      if (!f || Array.isArray(f) || f.id == null) throw new Error(t('faturalar.faturaBulunamadi'))
      mevcutFatura.value = f
      await cariyiCoz(f.cariHesapId, f.cariHesapAd)
      form.value = faturadanForm(f)
      await efaturaYukle()
    } catch (err) {
      yuklemeHatasi.value = err?.response?.data?.message || t('faturalar.faturaBulunamadi')
    } finally {
      yukleniyor.value = false
    }
  }

  const hazirla = async () => {
    cariHemenAra('')
    // Listeler form yuklemesini bloklamaz; seciciler doldukca kullanilabilir.
    listeleriYukle()
    if (route.query.cariId) {
      const id = Number(route.query.cariId)
      await cariyiCoz(id)
      form.value.cariHesapId = id
    } else if (!faturaId.value && !kopyaId.value && form.value.cariHesapId) {
      // localStorage taslagi geri yuklendiyse cari nesnesini de coz.
      await cariyiCoz(form.value.cariHesapId)
    }
    if (faturaId.value) {
      await faturaYukle(faturaId.value)
    } else if (kopyaId.value) {
      yukleniyor.value = true
      try {
        const f = await faturaStore.getFaturaById(kopyaId.value)
        await cariyiCoz(f.cariHesapId, f.cariHesapAd)
        form.value = kopyalananForm(f)
        toast.basarili(t('faturalar.faturaCogaltildi'))
      } catch (err) {
        yuklemeHatasi.value = err?.response?.data?.message || t('faturalar.faturaBulunamadi')
      } finally {
        yukleniyor.value = false
      }
    }
    hazir.value = true
    // Geri yuklenen taslak kullanici degisikligi sayilir; sayfadan cikarken sorulur.
    kirli.value = taslakGeriYuklendi.value
    if (taslakGeriYuklendi.value) toast.bilgi(t('faturalar.taslakGeriYuklendi'))
  }

  const dogrula = () => {
    if (!form.value.tur) return t('faturalar.faturaTuruSeciniz')
    if (!form.value.cariHesapId) return t('faturalar.cariSeciniz')
    if (!form.value.kalemler.length) return t('faturalar.kalemleriDoldurun')
    const gecersiz = form.value.kalemler.some((k) => !String(k.aciklama || '').trim() || !k.adet || !k.birimFiyat)
    if (gecersiz) return t('faturalar.kalemleriDoldurun')
    return ''
  }

  const payloadUret = ({ durum, krediOnayli }) => {
    const cekirdek = {
      cariHesapId: form.value.cariHesapId,
      tur: form.value.tur,
      tarih: form.value.tarih ? getLocalDateString(form.value.tarih) : null,
      aciklama: form.value.aciklama,
      genelIskontoTutari: 0,
      kalemler: form.value.kalemler
    }
    // Odeme alanlari yalnizca yeni kayitta sifir olarak gonderilir; guncellemede
    // mevcut odeme/kalan bilgisini ezmemek icin hic yazilmaz.
    if (!duzenlemeMi.value) {
      cekirdek.odenenTutar = 0
      cekirdek.odemeDurumu = 'ODENMEDI'
      // Manuel kur yalnizca yeni kayitta ve TRY disi para biriminde gonderilir.
      // Bos/0 ise backend TCMB satis kurunu kullanir.
      const kur = Number(form.value.kur)
      if (form.value.paraBirimi && form.value.paraBirimi !== 'TRY' && Number.isFinite(kur) && kur > 0) {
        cekirdek.kur = kur
      }
      if (form.value.siparisId) cekirdek.siparisId = form.value.siparisId
      if (form.value.irsaliyeId) cekirdek.irsaliyeId = form.value.irsaliyeId
    }
    const payload = satisPayloadUret({
      ...cekirdek,
      ekstra: {
        teslimEden: form.value.teslimEden || null,
        teslimDurumu: form.value.teslimDurumu || 'BEKLIYOR',
        teslimNotu: form.value.teslimNotu || null,
        depoId: form.value.depoId || null,
        paraBirimi: form.value.paraBirimi || 'TRY',
        krediLimitiGormezdenGel: !!krediOnayli
      }
    })
    if (!duzenlemeMi.value && durum) payload.durum = durum
    return payload
  }

  const teslimatKaydet = async (id) => {
    const adres = String(form.value.teslimatAdresi || '').trim()
    if (!id || !form.value.driverId || !adres) return false
    try {
      await teslimatAPI.olustur({
        faturaId: id,
        driverId: form.value.driverId,
        teslimatAdresi: adres,
        beklenenTeslimTarihi: form.value.beklenenTeslimTarihi
          ? getLocalDateString(form.value.beklenenTeslimTarihi)
          : null,
        notlar: form.value.teslimatNotu || null
      })
      return true
    } catch (err) {
      // Fatura kaydi geri alinmaz; kullaniciya ayri bildirim verilir.
      toast.hata(
        t('faturalar.teslimatOlusturulamadi') +
          (err?.response?.data?.message || t('faturalar.bilinmeyenHata'))
      )
      return false
    }
  }

  // C5: kaydedilmis fatura aksiyonlari (PDF / e-posta / e-fatura).
  const aktifFaturaId = computed(() => mevcutFatura.value?.id || sonKaydedilenId.value)

  const efaturaYukle = async () => {
    eFatura.value = null
    if (mevcutFatura.value?.durum !== 'KESILDI' || !faturaId.value) return
    try {
      const r = await eFaturaAPI.faturaIleGetir(faturaId.value)
      eFatura.value = r?.data || null
    } catch {
      eFatura.value = null
    }
  }

  const dosyaIndir = (blob, ad) => {
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = ad
    document.body.appendChild(a)
    a.click()
    a.remove()
    setTimeout(() => window.URL.revokeObjectURL(url), 30000)
  }

  const pdfIndir = async () => {
    if (!aktifFaturaId.value) return
    try {
      const res = await pdfAPI.fatura(aktifFaturaId.value)
      dosyaIndir(new Blob([res.data], { type: 'application/pdf' }), `fatura-${aktifFaturaId.value}.pdf`)
    } catch (err) {
      toast.hata(err?.response?.data?.message || t('faturalar.pdfIndirilemedi'))
    }
  }

  const onizleFatura = async () => {
    if (!aktifFaturaId.value) return
    try {
      const res = await pdfAPI.faturaOnizleme(aktifFaturaId.value)
      const url = window.URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }))
      window.open(url, '_blank')
      setTimeout(() => window.URL.revokeObjectURL(url), 60000)
    } catch (err) {
      toast.hata(err?.response?.data?.message || t('faturalar.pdfIndirilemedi'))
    }
  }

  const epostaGonder = async () => {
    if (!aktifFaturaId.value) return
    try {
      await faturaAPI.gonderEmail(aktifFaturaId.value)
      toast.basarili(t('faturalar.epostaGonderildi'))
    } catch (err) {
      toast.hata(err?.response?.data?.message || t('faturalar.epostaHata'))
    }
  }

  const efaturaIslemCalistir = async (islem) => {
    if (!aktifFaturaId.value || efaturaIslem.value) return
    efaturaIslem.value = true
    try {
      const r = await islem()
      if (r?.data) eFatura.value = r.data
      return true
    } catch (err) {
      toast.hata(err?.response?.data?.message || t('faturalar.efaturaHata'))
      return false
    } finally {
      efaturaIslem.value = false
    }
  }

  const efaturaOlustur = async () => {
    const oldu = await efaturaIslemCalistir(() => eFaturaAPI.olustur(aktifFaturaId.value))
    if (oldu) toast.basarili(t('faturalar.efaturaOlusturuldu'))
  }

  const efaturaGonder = () => efaturaIslemCalistir(() => eFaturaAPI.gibGonder(eFatura.value.id))
  const efaturaDurum = () => efaturaIslemCalistir(() => eFaturaAPI.durumSorgula(eFatura.value.id))

  const efaturaXml = async () => {
    if (!eFatura.value?.id) return
    try {
      const res = await eFaturaAPI.xmlIndir(eFatura.value.id)
      dosyaIndir(new Blob([res.data], { type: 'application/xml' }), `e-fatura-${eFatura.value.id}.xml`)
    } catch (err) {
      toast.hata(err?.response?.data?.message || t('faturalar.efaturaHata'))
    }
  }

  const kaydet = async ({ durum = 'TASLAK', krediOnayli = false } = {}) => {
    if (saving.value) return false
    if (!duzenlenebilir.value) {
      hata.value = kilitNedeni.value
      return false
    }
    const dogrulamaHatasi = dogrula()
    if (dogrulamaHatasi) {
      toast.uyari(dogrulamaHatasi)
      return false
    }

    saving.value = true
    hata.value = ''
    try {
      if (duzenlemeMi.value) {
        await faturaStore.updateFatura(faturaId.value, payloadUret({ durum: null, krediOnayli }))
        if (durum === 'KESILDI' && mevcutFatura.value?.durum !== 'KESILDI') {
          await faturaStore.updateDurum(faturaId.value, 'KESILDI')
        }
        toast.basarili(t('faturalar.faturaGuncellendi'))
      } else {
        const yeni = await faturaStore.addFatura(payloadUret({ durum, krediOnayli }))
        sonKaydedilenId.value = yeni?.id ?? null
        await teslimatKaydet(yeni?.id)
        toast.basarili(t('faturalar.faturaOlusturuldu'))
      }
      taslakTemizle()
      kaydedildi.value = true
      kirli.value = false
      // Kaydettikten sonra ekranda kal: aksiyonlar (PDF/e-fatura) icin id gerekir.
      const kayitId = duzenlemeMi.value ? faturaId.value : sonKaydedilenId.value
      if (duzenlemeMi.value) {
        hazir.value = false
        await faturaYukle(kayitId)
        hazir.value = true
        kirli.value = false
      } else if (kayitId) {
        router.replace({ name: 'FaturaDuzenle', params: { id: kayitId } })
      } else {
        router.push({ name: 'Faturalar' })
      }
      return true
    } catch (err) {
      const msg = err?.response?.data?.message || t('faturalar.islemBasarisiz')
      // Kredi limiti asimi: yalnizca KESILDI kaydetmede olusur. Kullanici acik
      // onay verirse bayrakla tekrar denenir.
      if (!krediOnayli && durum === 'KESILDI' && msg.includes('Kredi limiti') && confirm?.require) {
        confirm.require({
          message: msg + ' ' + t('faturalar.krediLimitiDevam'),
          header: t('faturalar.krediLimitiBaslik'),
          icon: 'pi pi-exclamation-triangle',
          acceptLabel: t('faturalar.krediLimitiOnayla'),
          rejectLabel: t('common.vazgec'),
          accept: () => kaydet({ durum, krediOnayli: true })
        })
      } else {
        hata.value = msg
        toast.hata(msg)
      }
      return false
    } finally {
      saving.value = false
    }
  }

  const taslakKaydet = () => kaydet({ durum: 'TASLAK' })
  const kaydetVeKes = () => kaydet({ durum: 'KESILDI' })

  // Rota parametresi ayni bilesen icinde degisirse (1 -> 2) formu yeniden yukle.
  watch(
    () => route.params.id,
    async (yeniId) => {
      faturaId.value = yeniId ? Number(yeniId) : null
      hazir.value = false
      await hazirla()
    }
  )

  watch(
    form,
    () => {
      if (hazir.value) {
        kirli.value = true
        // Yeni bir degisiklik, onceki basarili kaydin "kaydedildi" bayragini gecersiz kilar.
        kaydedildi.value = false
      }
    },
    { deep: true }
  )

  onUnmounted(() => {
    /* zamanlayicilar alt composable'larda temizleniyor */
  })

  return {
    form,
    seciliCariNesnesi,
    mevcutFatura,
    yukleniyor,
    yuklemeHatasi,
    saving,
    hata,
    kirli,
    kaydedildi,
    sonKaydedilenId,
    duzenlemeMi,
    duzenlenebilir,
    kilitNedeni,
    sayfaBasligi,
    depolar,
    depoAdi,
    depoKilitli,
    kurKilitli,
    personelSecenekleri,
    suruculer,
    teslimatVar,
    kaynaklar,
    kaynaklarYukleniyor,
    kaynakYukle,
    kaynaktanDoldur,
    aktifFaturaId,
    eFatura,
    efaturaIslem,
    pdfIndir,
    onizleFatura,
    epostaGonder,
    efaturaOlustur,
    efaturaGonder,
    efaturaDurum,
    efaturaXml,
    araToplam,
    kdvToplam,
    genelToplam,
    cariOnerileri,
    cariOnerileriYukleniyor,
    cariAra,
    cariSecildi,
    kalemEkle,
    removeKalem,
    kalemGeriAl,
    sonFaturayiKopyala,
    hazirla,
    kaydet,
    taslakKaydet,
    kaydetVeKes
  }
}
