import { defineStore } from 'pinia'
import { ref } from 'vue'
import { dovizAPI } from '../api/index.js'

export const useDovizStore = defineStore('doviz', () => {
  const kurlar = ref([
    { kod: 'USD', ad: 'ABD Doları', sembol: '$', alisFiyati: 47.35, satisFiyati: 47.43 },
    { kod: 'EUR', ad: 'Euro', sembol: '€', alisFiyati: 51.2, satisFiyati: 51.3 },
    { kod: 'GBP', ad: 'İngiliz Sterlini', sembol: '£', alisFiyati: 60.4, satisFiyati: 60.55 },
    { kod: 'SAR', ad: 'Suudi Arabistan Riyali', sembol: '﷼', alisFiyati: 12.6, satisFiyati: 12.65 },
    { kod: 'GAU', ad: 'Gram Altın', sembol: 'GAU', alisFiyati: 4200.0, satisFiyati: 4230.0 }
  ])
  const aktifParaBirimi = ref('TRY')
  const loading = ref(false)
  const sonGuncelleme = ref(new Date())

  const getKur = (kod) => {
    if (kod === 'TRY')
      return {
        kod: 'TRY',
        dovizKodu: 'TRY',
        ad: 'Türk Lirası',
        sembol: '₺',
        alisFiyati: 1,
        satisFiyati: 1,
        alisKuru: 1,
        satisKuru: 1
      }
    const bul = kurlar.value.find((k) => (k.kod || k.dovizKodu) === kod)
    if (bul) {
      return {
        ...bul,
        kod: bul.kod || bul.dovizKodu,
        ad: bul.ad || bul.dovizAdi,
        satisFiyati: bul.satisFiyati || bul.satisKuru || 1,
        alisFiyati: bul.alisFiyati || bul.alisKuru || 1
      }
    }
    return { kod, dovizKodu: kod, ad: kod, sembol: kod, alisFiyati: 1, satisFiyati: 1, alisKuru: 1, satisKuru: 1 }
  }

  const convert = (tutar, kaynak = 'TRY', hedef = 'USD') => {
    if (tutar == null || tutar === 0) return 0
    if (kaynak === hedef) return tutar

    const kKurObj = getKur(kaynak)
    const hKurObj = getKur(hedef)

    const kKur = kaynak === 'TRY' ? 1 : kKurObj.satisFiyati || kKurObj.satisKuru || 1
    const hKur = hedef === 'TRY' ? 1 : hKurObj.satisFiyati || hKurObj.satisKuru || 1

    const tryVal = tutar * kKur
    return tryVal / hKur
  }

  const formatPara = (tutar, birim = 'TRY') => {
    const v = tutar ?? 0
    const sembolMap = { TRY: '₺', USD: '$', EUR: '€', GBP: '£', SAR: '﷼', GAU: ' GAU' }
    const sembol = sembolMap[birim] || ` ${birim}`

    const formatted = new Intl.NumberFormat('tr-TR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(v)

    return birim === 'GAU' ? `${formatted}${sembol}` : `${formatted} ${sembol}`
  }

  const kurlariYukle = async () => {
    loading.value = true
    try {
      const res = await dovizAPI.getKurlar()
      if (res.data && res.data.length) {
        kurlar.value = res.data
        sonGuncelleme.value = new Date()
      }
    } catch (e) {
      console.warn('Döviz kurları yüklenirken hata, varsayılan kurlar kullanılıyor:', e)
    } finally {
      loading.value = false
    }
  }

  const kurlariGuncelle = async () => {
    loading.value = true
    try {
      const res = await dovizAPI.guncelle()
      if (res.data && res.data.length) {
        kurlar.value = res.data
        sonGuncelleme.value = new Date()
      }
    } catch (e) {
      console.error('Kurlar güncellenemedi:', e)
    } finally {
      loading.value = false
    }
  }

  return { kurlar, aktifParaBirimi, loading, sonGuncelleme, getKur, convert, formatPara, kurlariYukle, kurlariGuncelle }
})
