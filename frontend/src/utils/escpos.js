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

function ayrac() {
  return metin('- - - - - - - - - - - - - - - - - -\n')
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
 * @param {{ baslik: string, tarih: string, fisNo: string, kalemler: Array<{ad:string, adet:number, tutar?:number}>, toplam?:number, altNot?:string }} veri
 */
export function escPosFisiUret(veri) {
  const parcalar = [satirBaslat(), ciftGenislik(veri.baslik || 'RASPEL ERP')]
  if (veri.tarih) parcalar.push(normal(veri.tarih))
  if (veri.fisNo) parcalar.push(normal('Fiş No: ' + veri.fisNo))
  parcalar.push(ayrac())

  for (const k of veri.kalemler || []) {
    const satir = `${k.ad} x${k.adet}`
    if (k.tutar != null) {
      const tutar = k.tutar.toFixed(2)
      parcalar.push(normal(satir + '    ' + tutar))
    } else {
      parcalar.push(normal(satir))
    }
  }

  parcalar.push(ayrac())
  if (veri.toplam != null) {
    parcalar.push(ciftGenislik('GENEL TOPLAM: ' + veri.toplam.toFixed(2)))
  }
  if (veri.altNot) parcalar.push(normal(veri.altNot))
  parcalar.push(kesVeAc())

  return concat(...parcalar)
}

/**
 * ESC/POS byte dizisini bağlı bir termal yazıcıya göndermeye çalışır (WebUSB).
 * Desteklenmiyorsa null döner.
 */
export async function escPosYazdir(bytes) {
  if (!('usb' in navigator)) return false
  try {
    const cihaz = await navigator.usb.requestDevice({ filters: [{ vendorId: 0x0416 }] })
    await cihaz.open()
    if (cihaz.configuration === null) await cihaz.selectConfiguration(1)
    await cihaz.claimInterface(0)
    await cihaz.transferOut(1, bytes)
    await cihaz.close()
    return true
  } catch {
    return false
  }
}
