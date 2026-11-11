import { ref, computed, watch, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useConfirm } from 'primevue/useconfirm'
import { useToastBildirim } from './useToastBildirim.js'
import { useCariOnerileri, cariHesapCoz } from './useCariOnerileri.js'
import { useTaslakKayit } from './useTaslakKayit.js'
import { useFaturaStore } from '../stores/faturaStore.js'
import { useCariHesapStore } from '../stores/cariHesapStore.js'
import { faturaAPI } from '../api/index.js'
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

  const cariSecildi = (event) => {
    form.value.cariHesapId = event.value?.id || null
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
      if (!f) throw new Error(t('faturalar.faturaBulunamadi'))
      mevcutFatura.value = f
      await cariyiCoz(f.cariHesapId, f.cariHesapAd)
      form.value = faturadanForm(f)
    } catch (err) {
      yuklemeHatasi.value = err?.response?.data?.message || t('faturalar.faturaBulunamadi')
    } finally {
      yukleniyor.value = false
    }
  }

  const hazirla = async () => {
    cariHemenAra('')
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
        await faturaStore.addFatura(payloadUret({ durum, krediOnayli }))
        toast.basarili(t('faturalar.faturaOlusturuldu'))
      }
      taslakTemizle()
      kaydedildi.value = true
      kirli.value = false
      router.push({ name: 'Faturalar' })
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
      if (hazir.value) kirli.value = true
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
    duzenlemeMi,
    duzenlenebilir,
    kilitNedeni,
    sayfaBasligi,
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
