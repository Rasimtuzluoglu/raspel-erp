// DataTable'lar icin mobil kart gorunumu etiketleri.
// <=600px'te tablo satirlari karta donusur; her hucrenin basina kolon basligi
// (data-label) yazilir. Secim/expander gibi basligi olmayan kolonlar atlanir.
// Global MutationObserver ile tum tablolara otomatik uygulanir.
const ATTR = 'data-label'
const ILK_ATTR = 'data-label-ilk'

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
  const tablolar = kok.querySelectorAll ? kok.querySelectorAll('.p-datatable') : []
  for (const tablo of tablolar) satirlariEtiketle(tablo)
}

let planlandi = false
function planla() {
  if (planlandi) return
  planlandi = true
  const calistir = () => {
    planlandi = false
    try {
      tabloEtiketle(document)
    } catch {
      /* sessiz */
    }
  }
  if (typeof requestAnimationFrame === 'function') requestAnimationFrame(calistir)
  else setTimeout(calistir, 0)
}

/** Uygulama baslangicinda cagrilir: tablolar olustukca/guncellendikce etiketleri yazar. */
export function initTabloEtiketleri() {
  if (typeof document === 'undefined') return
  planla()
  if (typeof MutationObserver === 'undefined') return
  const gozlemci = new MutationObserver(planla)
  gozlemci.observe(document.body, { childList: true, subtree: true })
}

export default {
  mounted: (el) => tabloEtiketle(el),
  updated: (el) => tabloEtiketle(el)
}
