export const kalemNetTutar = (kalem) => {
  const brf = kalem?.birimFiyat || 0
  const adt = kalem?.adet || 0
  const iskontoOran = (kalem?.iskontoOrani || 0) / 100
  return brf * adt * (1 - iskontoOran)
}

export const kalemKdv = (kalem) => {
  return kalemNetTutar(kalem) * ((kalem?.kdvOrani || 0) / 100)
}

export const kalemTutar = (kalem) => {
  return kalemNetTutar(kalem) + kalemKdv(kalem)
}
