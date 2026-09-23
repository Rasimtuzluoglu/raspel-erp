const VARSAYILAN_KDV_ORANI = 20

export const kdvOrani = (kalem) => kalem?.kdvOrani ?? VARSAYILAN_KDV_ORANI

export const kalemMiktar = (kalem) => kalem?.miktar ?? kalem?.adet ?? 1

// FATURA/SATIŞ: birim fiyat KDV DAHİL kabul edilir (perakende etiket fiyatı).
// Böylece ekranda/fişte gösterilen ve tahsil edilen tutar, kayıtlı genel toplama eşittir.
export const kalemBrut = (kalem) => kalemMiktar(kalem) * (kalem?.birimFiyat || 0)

export const kalemIskontoluBrut = (kalem) => {
  const iskontoOran = (kalem?.iskontoOrani || 0) / 100
  return kalemBrut(kalem) * (1 - iskontoOran)
}

// KDV, iskontolu brüt tutardan ayrıştırılır (iç yüzde).
export const kalemNetTutar = (kalem) => {
  const brut = kalemIskontoluBrut(kalem)
  return brut / (1 + kdvOrani(kalem) / 100)
}

export const kalemKdv = (kalem) => kalemIskontoluBrut(kalem) - kalemNetTutar(kalem)

// Satır toplamı KDV dahildir (ekranda/fişte gösterilen tutar).
export const kalemTutar = (kalem) => kalemIskontoluBrut(kalem)

// TEKLİF konvansiyonu: satır iskontosu yalnız netten düşer, KDV brüt üzerinden alınır.
// (TeklifService ile birebir uyumlu; satış faturalarını etkilemez.)
export const kalemBrutKdv = (kalem) => kalemBrut(kalem) * (kdvOrani(kalem) / 100)

export const teklifOzet = (kalemler = [], iskontoOrani = 0) => {
  const araToplam = kalemler.reduce((t, k) => {
    const iskontoOran = (k?.iskontoOrani || 0) / 100
    return t + kalemBrut(k) * (1 - iskontoOran)
  }, 0)
  const kdv = kalemler.reduce((t, k) => t + (kalemBrutKdv(k) || 0), 0)
  const iskontoTutari = (araToplam * (iskontoOrani || 0)) / 100
  return {
    araToplam,
    kdv,
    iskontoTutari,
    genelToplam: araToplam - iskontoTutari + kdv
  }
}
