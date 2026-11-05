/**
 * Anasayfa hizli kisayol seridinin katalogu ve cihaz tercihi.
 *
 * Katalog sirasi ayni zamanda seritte gorunen siradir. Kullanici yalnizca
 * secim yapar; siralama her zaman bu katalog tarafindan belirlenir. Bu sayede
 * localStorage'daki key dizisi bozulsa bile serit ongorulebilir kalir.
 */

export const KISAYOL_DEPOLAMA_ANAHTARI = 'raspel_dashboard_kisayollar'
export const MAX_KISAYOL = 8

/**
 * `admin: true` olan kisayollar yalnizca ADMIN rolune gorunur/secilebilir.
 * `renk` alani seritteki renk sinifini belirler (`kisayol--<renk>`).
 */
export const KISAYOL_KATALOGU = [
  { key: 'yeniFatura', to: '/faturalar', anahtar: 'dashboard.kisayolYeniFatura', ikon: 'pi pi-file', renk: 'yeniFatura' },
  { key: 'tahsilat', to: '/hareketler', anahtar: 'dashboard.kisayolTahsilat', ikon: 'pi pi-arrow-down-left', renk: 'tahsilat' },
  { key: 'odeme', to: '/hareketler', anahtar: 'dashboard.kisayolOdeme', ikon: 'pi pi-arrow-up-right', renk: 'odeme' },
  { key: 'hizliSatis', to: '/hizli-satis', anahtar: 'dashboard.kisayolHizliSatis', ikon: 'pi pi-shopping-bag', renk: 'hizliSatis' },
  { key: 'yeniCari', to: '/cari-hesaplar', anahtar: 'dashboard.kisayolYeniCari', ikon: 'pi pi-users', renk: 'yeniCari' },
  { key: 'kasa', to: '/kasa', anahtar: 'dashboard.kisayolKasa', ikon: 'pi pi-wallet', renk: 'kasa' },
  { key: 'stoklar', to: '/stoklar', anahtar: 'dashboard.kisayolStoklar', ikon: 'pi pi-box', renk: 'stoklar' },
  { key: 'teklifler', to: '/teklifler', anahtar: 'dashboard.kisayolTeklifler', ikon: 'pi pi-file-edit', renk: 'teklifler' },
  { key: 'satinalma', to: '/satinalma', anahtar: 'dashboard.satinalma', ikon: 'pi pi-shopping-cart', renk: 'satinalma' },
  { key: 'onaylar', to: '/onaylar', anahtar: 'dashboard.onaylar', ikon: 'pi pi-check-square', renk: 'onaylar' },
  { key: 'raporlar', to: '/raporlar', anahtar: 'dashboard.raporlar', ikon: 'pi pi-chart-bar', renk: 'raporlar' },
  { key: 'kokpit', to: '/yonetici-kokpiti', anahtar: 'dashboard.kokpit', ikon: 'pi pi-chart-line', renk: 'kokpit', admin: true },
  { key: 'ajanda', to: '/ajanda', anahtar: 'dashboard.ajanda', ikon: 'pi pi-calendar', renk: 'ajanda' },
  { key: 'yedekler', to: '/yedekler', anahtar: 'dashboard.yedekler', ikon: 'pi pi-save', renk: 'yedekler', admin: true }
]

/** Varsayilan serit: mevcut 8 islem (katalogun ilk 8 kaydi). */
export const VARSAYILAN_KISAYOLLAR = KISAYOL_KATALOGU.slice(0, 8).map((k) => k.key)

const katalogHaritasi = new Map(KISAYOL_KATALOGU.map((k) => [k.key, k]))

/** Role gore kullanilabilir katalog (ADMIN olmayan kokpit/yedekleri gormez). */
export const kisayolKatalogu = (adminMi = false) =>
  KISAYOL_KATALOGU.filter((k) => !k.admin || adminMi === true)

const guvenliDepo = () => {
  try {
    return typeof localStorage !== 'undefined' ? localStorage : null
  } catch {
    return null
  }
}

/**
 * Key dizisini katalog sirasina ve role gore normalize eder.
 * - Bilinmeyen/tekrarlanan anahtarlar atilir.
 * - ADMIN olmayan kullanicinin yetkisiz secimleri filtrelenir.
 * - En fazla `MAX_KISAYOL` kayit tutulur.
 * - Sonuc bossa varsayilan 8'e donulur.
 */
export const kisayollariNormalize = (keys, adminMi = false) => {
  const izinli = new Set(kisayolKatalogu(adminMi).map((k) => k.key))
  const secilen = new Set()
  if (Array.isArray(keys)) {
    for (const key of keys) {
      if (typeof key === 'string' && izinli.has(key)) secilen.add(key)
    }
  }
  const sirali = KISAYOL_KATALOGU.filter((k) => secilen.has(k.key)).map((k) => k.key)
  const sonuc = sirali.slice(0, MAX_KISAYOL)
  return sonuc.length ? sonuc : [...VARSAYILAN_KISAYOLLAR]
}

/** Secimi tam katalog nesnelerine cevirir (katalog sirasi korunur). */
export const kisayollariCoz = (keys, adminMi = false) =>
  kisayollariNormalize(keys, adminMi)
    .map((key) => katalogHaritasi.get(key))
    .filter(Boolean)

/** localStorage'dan secimi okur; kayit yok/bozuksa varsayilana doner. */
export const kisayollariOku = (adminMi = false, depo = guvenliDepo()) => {
  try {
    const ham = depo?.getItem?.(KISAYOL_DEPOLAMA_ANAHTARI)
    if (!ham) return [...VARSAYILAN_KISAYOLLAR]
    return kisayollariNormalize(JSON.parse(ham), adminMi)
  } catch {
    return [...VARSAYILAN_KISAYOLLAR]
  }
}

/** Secimi cihaza yazar. Depolama kapaliysa sessizce yutar. */
export const kisayollariYaz = (keys, adminMi = false, depo = guvenliDepo()) => {
  const normalize = kisayollariNormalize(keys, adminMi)
  try {
    depo?.setItem?.(KISAYOL_DEPOLAMA_ANAHTARI, JSON.stringify(normalize))
  } catch {
    /* depolama kapaliysa oturum boyunca bellekte kalir */
  }
  return normalize
}
