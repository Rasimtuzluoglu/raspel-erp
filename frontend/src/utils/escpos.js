// ESC/POS termal yazıcı komut üretici.
// 58mm/80mm termal yazıcılara doğrudan gönderilebilecek byte dizisi üretir.

const ESC = 0x1b
const GS = 0x1d

function metin(str) {
  return new TextEncoder().encode(str)
}

function komut(...bytes) {
  return new Uint8Array(bytes)
}

function satirBaslat() {
  return komut(ESC, 0x61, 0x00) // left align
}

function normal(str) {
  return concat(metin(str), komut(0x0a))
}

function ciftGenislik(str) {
  // bold + double width/height başlık
  const ac = komut(ESC, 0x45, 0x01, GS, 0x21, 0x11)
  const kapat = komut(ESC, 0x45, 0x00, GS, 0x21, 0x00)
  return concat(ac, metin(str), kapat, komut(0x0a))
}

function ayrac(genislik) {
  const w = genislik || 48
  const adet = Math.max(1, Math.floor(w / 2))
  return metin('-'.repeat(Math.min(w, adet * 2)) + '\n')
}

/** Kağıt genişliğine (58/80mm) karşılık gelen karakter sayısı. */
function karakterGenisligi(genislik) {
  const g = String(genislik ?? '80')
  return g.startsWith('58') ? 32 : 48
}

/** Metni kelime sınırlarında verilen karakter genişliğine göre sarar. */
function sar(str, genislik) {
  const w = Math.max(8, genislik)
  const kelimeler = String(str ?? '').split(/\s+/).filter(Boolean)
  if (!kelimeler.length) return ['']
  const satirlar = []
  let mevcut = ''
  for (const k of kelimeler) {
    if (!mevcut) {
      mevcut = k
    } else if ((mevcut + ' ' + k).length <= w) {
      mevcut += ' ' + k
    } else {
      satirlar.push(mevcut)
      mevcut = k
    }
  }
  if (mevcut) satirlar.push(mevcut)
  return satirlar
}

/** Bir kalemi (adet + tutar) taşmadan yazılacak satırlara böler. */
function kalemSatirlari(k, w) {
  const tutarStr = k.tutar != null ? Number(k.tutar).toFixed(2) : ''
  const baslik = `${k.ad} x${k.adet}`
  const satirlar = sar(baslik, w)
  if (!tutarStr) return satirlar
  const son = satirlar[satirlar.length - 1]
  if (son.length + tutarStr.length + 1 <= w) {
    satirlar[satirlar.length - 1] = son + ' '.repeat(w - son.length - tutarStr.length) + tutarStr
  } else {
    satirlar.push(' '.repeat(Math.max(0, w - tutarStr.length)) + tutarStr)
  }
  return satirlar
}

/** Ağırlığı (kg) okunabilir biçimde döndürür; 1000kg üzerini tona çevirir. */
function agirlikMetni(kg) {
  const deger = Number(kg) || 0
  if (deger >= 1000) return (deger / 1000).toFixed(3) + ' ton'
  return deger.toFixed(2) + ' kg'
}

function kesVeAc() {
  // feed + partial cut
  return komut(0x0a, 0x0a, 0x0a, GS, 0x56, 0x00)
}

function concat(...arrs) {
  const toplam = arrs.reduce((t, a) => t + a.length, 0)
  const sonuc = new Uint8Array(toplam)
  let ofs = 0
  for (const a of arrs) {
    sonuc.set(a, ofs)
    ofs += a.length
  }
  return sonuc
}

/**
 * Termal fişi ESC/POS byte dizisine dönüştürür.
 * @param {{ baslik: string, tarih: string, fisNo: string, kalemler: Array<{ad:string, adet:number, tutar?:number}>, toplam?:number, toplamAgirlik?:number, genislik?:number|string, altNot?:string }} veri
 */
export function escPosFisiUret(veri) {
  const w = karakterGenisligi(veri.genislik)
  const parcalar = [satirBaslat(), ciftGenislik(veri.baslik || 'RASPEL ERP')]
  if (veri.tarih) parcalar.push(normal(veri.tarih))
  if (veri.fisNo) parcalar.push(normal('Fiş No: ' + veri.fisNo))
  parcalar.push(ayrac(w))

  for (const k of veri.kalemler || []) {
    for (const satir of kalemSatirlari(k, w)) {
      parcalar.push(normal(satir))
    }
  }

  parcalar.push(ayrac(w))
  if (veri.toplamAgirlik != null && Number(veri.toplamAgirlik) > 0) {
    parcalar.push(normal('Toplam Ağırlık: ' + agirlikMetni(veri.toplamAgirlik)))
  }
  if (veri.toplam != null) {
    parcalar.push(ciftGenislik('GENEL TOPLAM: ' + Number(veri.toplam).toFixed(2)))
  }
  if (veri.altNot) {
    for (const satir of sar(veri.altNot, w)) parcalar.push(normal(satir))
  }
  parcalar.push(kesVeAc())

  return concat(...parcalar)
}

/**
 * ESC/POS byte dizisini bağlı bir termal yazıcıya göndermeye çalışır (WebUSB).
 * Başarılıysa yazıcı adını (productName), aksi halde null döner.
 * @returns {Promise<string|null>}
 */
export async function escPosYazdir(bytes) {
  if (!('usb' in navigator)) return null
  try {
    const cihaz = await navigator.usb.requestDevice({ filters: [{ vendorId: 0x0416 }] })
    await cihaz.open()
    if (cihaz.configuration === null) await cihaz.selectConfiguration(1)
    await cihaz.claimInterface(0)
    await cihaz.transferOut(1, bytes)
    await cihaz.close()
    return cihaz.productName || 'Termal Yazıcı'
  } catch {
    return null
  }
}
