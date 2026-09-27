// DataTable'lar icin mobil kart gorunumu etiketleri.
// <=600px'te tablo satirlari karta donusur; her hucrenin basina kolon basligi
// (data-label) yazilir. Secim/expander gibi basligi olmayan kolonlar atlanir.
// Etiketler yalnizca mobil CSS'te kullanildigi icin masaustunde hic calisilmaz;
// gozlemci de tum dokumani degil, yalnizca eklenen tablolari isler (performans).
const ATTR = 'data-label'
const ILK_ATTR = 'data-label-ilk'
const MOBIL_SORGU = '(max-width: 600px)'

function mobilMi() {
  try {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return true
    return window.matchMedia(MOBIL_SORGU).matches
  } catch {
    return true
  }
}

function satirlariEtiketle(tablo) {
  const basliklar = [...tablo.querySelectorAll('.p-datatable-thead > tr > th')].map((th) =>
    (th.textContent || '').replace(/\s+/g, ' ').trim()
  )
  for (const tr of tablo.querySelectorAll('.p-datatable-tbody > tr')) {
    // onceki isaretleri temizle (attr degisimi gozlemciyi tetiklemez)
    for (const eski of tr.querySelectorAll(`[${ATTR}], [${ILK_ATTR}]`)) {
      eski.removeAttribute(ATTR)
      eski.removeAttribute(ILK_ATTR)
    }
    const tdler = [...tr.children].filter((c) => c.tagName === 'TD')
    let ilkIsaretlendi = false
    tdler.forEach((td, i) => {
      const baslik = basliklar[i] || ''
      if (!baslik) return
      td.setAttribute(ATTR, baslik)
      if (!ilkIsaretlendi) {
        td.setAttribute(ILK_ATTR, '1')
        ilkIsaretlendi = true
      }
    })
  }
}

export function tabloEtiketle(kok = document) {
  if (!mobilMi()) return
  const tablolar = []
  if (kok && kok.matches && kok.matches('.p-datatable')) tablolar.push(kok)
  if (kok && kok.querySelectorAll) tablolar.push(...kok.querySelectorAll('.p-datatable'))
  for (const tablo of tablolar) satirlariEtiketle(tablo)
}

/** Eklenen dugumlerden yalnizca ilgili tablolari toplar. */
function eklenenTablolar(dugumler) {
  const tablolar = new Set()
  for (const dugum of dugumler) {
    if (!dugum || dugum.nodeType !== 1) continue
    if (dugum.matches && dugum.matches('.p-datatable')) tablolar.add(dugum)
    if (dugum.querySelectorAll) dugum.querySelectorAll('.p-datatable').forEach((t) => tablolar.add(t))
    const ust = dugum.closest ? dugum.closest('.p-datatable') : null
    if (ust) tablolar.add(ust)
  }
  return tablolar
}

let planlandi = false
let bekleyenDugumler = []

function planla(dugumler) {
  if (dugumler && dugumler.length) bekleyenDugumler.push(...dugumler)
  if (planlandi) return
  planlandi = true
  const calistir = () => {
    planlandi = false
    const dugumler = bekleyenDugumler
    bekleyenDugumler = []
    try {
      if (!mobilMi()) return
      if (!dugumler.length) {
        tabloEtiketle(document)
        return
      }
      for (const tablo of eklenenTablolar(dugumler)) satirlariEtiketle(tablo)
    } catch {
      /* sessiz */
    }
  }
  // Toplu DOM guncellemelerinde tek seferde calis (kisa debounce).
  setTimeout(calistir, 200)
}

/** Uygulama baslangicinda cagrilir: tablolar olustukca/guncellendikce etiketleri yazar. */
export function initTabloEtiketleri() {
  if (typeof document === 'undefined') return
  planla([])
  if (typeof MutationObserver === 'undefined') return
  const gozlemci = new MutationObserver((kayitlar) => {
    if (!mobilMi()) return
    const eklenenler = []
    for (const kayit of kayitlar) kayit.addedNodes.forEach((n) => eklenenler.push(n))
    if (eklenenler.length) planla(eklenenler)
  })
  gozlemci.observe(document.body, { childList: true, subtree: true })
  // Masaustunden mobil genislige gecilirse mevcut tablolar bir kez etiketlenir.
  if (typeof window !== 'undefined' && typeof window.matchMedia === 'function') {
    try {
      const sorgu = window.matchMedia(MOBIL_SORGU)
      if (sorgu.addEventListener) sorgu.addEventListener('change', (e) => { if (e.matches) planla([]) })
    } catch {
      /* yoksay */
    }
  }
}

export default {
  mounted: (el) => tabloEtiketle(el),
  updated: (el) => tabloEtiketle(el)
}
