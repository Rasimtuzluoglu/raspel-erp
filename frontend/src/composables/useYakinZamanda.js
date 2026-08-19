const ANAHTAR = 'raspel_yakin_zamanda'

export function useYakinZamanda() {
  const kaydet = (tur, id, baslik, alt) => {
    try {
      const liste = JSON.parse(localStorage.getItem(ANAHTAR) || '[]')
      const yeni = { tur, id, baslik, alt, tarih: new Date().toISOString() }
      const filtrelenmis = liste.filter((i) => !(i.tur === tur && i.id === id))
      filtrelenmis.unshift(yeni)
      localStorage.setItem(ANAHTAR, JSON.stringify(filtrelenmis.slice(0, 10)))
    } catch {
      /* empty */
    }
  }

  const liste = () => {
    try {
      return JSON.parse(localStorage.getItem(ANAHTAR) || '[]')
    } catch {
      return []
    }
  }

  return { kaydet, liste }
}

export const yakinZamandaTurleri = {
  fatura: { ikon: 'pi pi-file', renk: '#fbbf24', yol: '/faturalar/' },
  cari: { ikon: 'pi pi-users', renk: '#60a5fa', yol: '/cari-hesaplar/' },
  stok: { ikon: 'pi pi-box', renk: '#4ade80', yol: '/stoklar' },
  siparis: { ikon: 'pi pi-receipt', renk: '#a78bfa', yol: '/siparisler/' },
  proje: { ikon: 'pi pi-folder', renk: '#f472b6', yol: '/projeler/' }
}
