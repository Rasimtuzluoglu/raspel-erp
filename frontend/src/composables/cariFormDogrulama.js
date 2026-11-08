/**
 * Cari hesap formu alan doğrulama yardımcıları.
 * Hem görünüm hem birim testleri aynı kuralları kullanır.
 */

export const epostaGecerli = (deger) => {
  if (!deger || !deger.trim()) return true
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(deger.trim())
}

export const telefonGecerli = (deger) => {
  if (!deger || !deger.trim()) return true
  const rakamlar = String(deger).replace(/\D/g, '')
  return rakamlar.length >= 10 && rakamlar.length <= 11
}

export const vergiNumarasiGecerli = (deger) => {
  if (!deger || !deger.trim()) return true
  return /^\d{10,11}$/.test(String(deger).replace(/\D/g, ''))
}

/**
 * Formu doğrular ve alan adı → hata mesajı haritası döndürür.
 * @param {{ form: object, ibanGecerli: boolean|null, t: Function }} girdi
 */
export const cariFormDogrula = ({ form, ibanGecerli, t }) => {
  const hatalar = {}
  if (!form?.ad || !form.ad.trim()) {
    hatalar.ad = t('cariHesaplar.adBosOlamaz')
  }
  if (!epostaGecerli(form?.email)) {
    hatalar.email = t('cariHesaplar.gecersizEposta')
  }
  if (!telefonGecerli(form?.telefon)) {
    hatalar.telefon = t('cariHesaplar.gecersizTelefon')
  }
  if (!vergiNumarasiGecerli(form?.vergiNumarasi)) {
    hatalar.vergiNumarasi = t('cariHesaplar.gecersizVergiNo')
  }
  if (form?.iban && ibanGecerli === false) {
    hatalar.iban = t('cariHesaplar.ibanGecersiz')
  }
  return hatalar
}
