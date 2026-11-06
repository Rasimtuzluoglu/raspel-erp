/**
 * PrimeVue overlay konumlarini html zoom'una gore duzeltir.
 *
 * Uygulama `html { zoom: var(--uyg-olcek) }` (0.9) kullaniyor. PrimeVue
 * overlay'i `body`'ye ekleyip getBoundingClientRect ile fiziksel koordinat
 * yazdiginda, body de zoom'lu oldugu icin panel 0.9 ile tekrar olceklenip
 * sola/yukari kayiyor. Bu modul, portal edilen panellerin inline left/top
 * degerlerini 1/zoom ile carparak tetikleyiciyle hizalar. Paneller body'de
 * kaldigi icin dialog/scroll alanlarinda kirpilma olmaz.
 */
const OVERLAY_SECICI = [
  '.p-select-overlay',
  '.p-dropdown-panel',
  '.p-autocomplete-overlay',
  '.p-datepicker-panel',
  '.p-multiselect-overlay',
  '.p-treeselect-overlay',
  '.p-cascadeselect-overlay',
  '.p-menu',
  '.p-tieredmenu',
  '.p-contextmenu',
  '.p-menubar-submenu',
  '.p-popover',
  '.p-password-overlay',
  '.p-mention-overlay'
].join(',')

const sonDegerler = new WeakMap()
let planlandi = false

const olcekOku = () => {
  try {
    const kok = getComputedStyle(document.documentElement)
    const ham = parseFloat(kok.getPropertyValue('--uyg-olcek'))
    return Number.isFinite(ham) && ham > 0 ? ham : 1
  } catch {
    return 1
  }
}

const pxDuzelt = (el, ozellik, carpan) => {
  const ham = el.style[ozellik]
  if (!ham || !ham.endsWith('px')) return
  const deger = parseFloat(ham)
  if (!Number.isFinite(deger)) return
  const onceki = sonDegerler.get(el) || {}
  // CSSOM px degerlerini yuvarlayabildigi icin string esitligi guvenilir
  // degil; beklenen sayisal degerle toleransli karsilastir.
  if (Number.isFinite(onceki[ozellik]) && Math.abs(deger - onceki[ozellik]) < 0.5) {
    return
  }
  const yeniDeger = deger * carpan
  el.style[ozellik] = `${yeniDeger}px`
  onceki[ozellik] = yeniDeger
  sonDegerler.set(el, onceki)
}

const duzelt = (el) => {
  if (!(el instanceof HTMLElement)) return
  const olcek = olcekOku()
  if (olcek === 1) return
  const carpan = 1 / olcek
  // PrimeVue LTR'de insetInlineStart, bazi surumlerde left kullanir.
  if (el.style.insetInlineStart) {
    pxDuzelt(el, 'insetInlineStart', carpan)
  } else {
    pxDuzelt(el, 'left', carpan)
  }
  pxDuzelt(el, 'top', carpan)
}

const tumunuDuzelt = () => {
  document.querySelectorAll(OVERLAY_SECICI).forEach((el) => {
    if (el.getClientRects().length) duzelt(el)
  })
}

const planla = () => {
  if (planlandi) return
  planlandi = true
  // PrimeVue konumu iki karede bir baglar; ardindan duzelt.
  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      planlandi = false
      tumunuDuzelt()
    })
  })
}

export function overlayZoomFixBaslat() {
  if (typeof window === 'undefined' || typeof MutationObserver === 'undefined') return
  // Teshis/gecici kapatma: ?nooverlayfix
  try {
    if (new URLSearchParams(window.location.search).has('nooverlayfix')) return
  } catch {
    /* yoksay */
  }

  const gozcu = new MutationObserver((kayitlar) => {
    let gerekli = false
    for (const kayit of kayitlar) {
      if (kayit.type === 'attributes') {
        if (kayit.target instanceof HTMLElement && kayit.target.matches(OVERLAY_SECICI)) {
          gerekli = true
        }
        continue
      }
      for (const eklenen of kayit.addedNodes) {
        if (!(eklenen instanceof HTMLElement)) continue
        if (eklenen.matches(OVERLAY_SECICI) || eklenen.querySelector(OVERLAY_SECICI)) {
          gerekli = true
        }
      }
    }
    if (gerekli) planla()
  })

  gozcu.observe(document.body, {
    childList: true,
    subtree: true,
    attributes: true,
    attributeFilter: ['style']
  })
  window.addEventListener('scroll', planla, true)
  window.addEventListener('resize', planla)
}
