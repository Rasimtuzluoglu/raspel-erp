const VARSAYILAN_KDV_ORANI = 20

export const kdvOrani = (kalem) => kalem?.kdvOrani ?? VARSAYILAN_KDV_ORANI

export const kalemMiktar = (kalem) => kalem?.miktar ?? kalem?.adet ?? 1

export const kalemBrut = (kalem) => {
  const brf = kalem?.birimFiyat || 0
  return kalemMiktar(kalem) * brf
}

export const kalemNetTutar = (kalem) => {
  const iskontoOran = (kalem?.iskontoOrani || 0) / 100
  return kalemBrut(kalem) * (1 - iskontoOran)
}

// Fatura ve satış belgelerinde KDV matrahtan (iskonto sonrası net) hesaplanır.
export const kalemKdv = (kalem) => {
  return kalemNetTutar(kalem) * (kdvOrani(kalem) / 100)
}

export const kalemTutar = (kalem) => {
  return kalemNetTutar(kalem) + kalemKdv(kalem)
}

// Teklifte satır iskontosu yalnız net düşer; KDV brüt üzerinden alınır.
export const kalemBrutKdv = (kalem) => {
  return kalemBrut(kalem) * (kdvOrani(kalem) / 100)
}

export const teklifOzet = (kalemler = [], iskontoOrani = 0) => {
  const araToplam = kalemler.reduce((t, k) => t + (kalemNetTutar(k) || 0), 0)
  const kdv = kalemler.reduce((t, k) => t + (kalemBrutKdv(k) || 0), 0)
  const iskontoTutari = (araToplam * (iskontoOrani || 0)) / 100
  return {
    araToplam,
    kdv,
    iskontoTutari,
    genelToplam: (araToplam - iskontoTutari) + kdv
  }
}