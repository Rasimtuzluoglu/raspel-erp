import { ref, computed } from 'vue'

const MODE_KEY = 'raspel_erp_theme'
const COLOR_KEY = 'raspel_primary_color'
const VARSAYILAN_RENK = '#0f766e'
const ESKI_VARSAYILAN = '#10b981'

const mode = ref(localStorage.getItem(MODE_KEY) || 'dark')

// Eski varsayılan renk kayıtlıysa (kullanıcı seçimi yoksa) yeni varsayılana taşı.
let kayitliRenk = localStorage.getItem(COLOR_KEY)
if (kayitliRenk && kayitliRenk.toLowerCase() === ESKI_VARSAYILAN) {
  kayitliRenk = VARSAYILAN_RENK
  localStorage.setItem(COLOR_KEY, kayitliRenk)
}
const accentColor = ref(kayitliRenk || VARSAYILAN_RENK)

let systemMedia = null
let mediaHandler = null

function systemTercihiKaranlik() {
  try {
    return !!(window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches)
  } catch {
    return false
  }
}

function applyMode(m) {
  let etkin = m
  if (m === 'system') {
    etkin = systemTercihiKaranlik() ? 'dark' : 'light'
  } else {
    etkin = m === 'light' ? 'light' : 'dark'
  }
  mode.value = m === 'system' ? 'system' : etkin
  const root = document.documentElement
  root.setAttribute('data-theme', etkin)
  root.classList.toggle('p-dark', etkin === 'dark')
  localStorage.setItem(MODE_KEY, mode.value)
  temaRengiSenkron(etkin)
  // Tema değişince aksan metni yeniden hesaplanır (inline token güncel kalır).
  root.style.setProperty('--accent-text', aksanMetinRengi(accentColor.value, etkin === 'dark'))
}

/**
 * PWA/mobil tarayici chrome rengini (adres cubugu, durum cubugu) uygulama temasiyla
 * senkronlar. Boylece PWA'da yukleniyor ekrani/splash ve cerceve, masaustundeki
 * uygulama gorunumuyle ayni zemini kullanir.
 */
function temaRengiSenkron(etkinTema) {
  try {
    const renk = etkinTema === 'light' ? '#f1f5f9' : '#0a0e14'
    let meta = document.querySelector('meta[name="theme-color"]:not([media])')
    if (!meta) {
      // media'li eski meta etiketlerini kaldir; tek otorite runtime deger olsun.
      document.querySelectorAll('meta[name="theme-color"]').forEach((m) => m.remove())
      meta = document.createElement('meta')
      meta.setAttribute('name', 'theme-color')
      document.head.appendChild(meta)
    }
    meta.setAttribute('content', renk)
  } catch {
    /* yoksay */
  }
}

/** Aksan renginin parlaklığına göre okunabilir metin/ikon rengi döndürür. */
function kontrastRengi(hex) {
  try {
    const h = hex.replace('#', '')
    const r = parseInt(h.substring(0, 2), 16)
    const g = parseInt(h.substring(2, 4), 16)
    const b = parseInt(h.substring(4, 6), 16)
    const parlaklik = (0.299 * r + 0.587 * g + 0.114 * b) / 255
    return parlaklik > 0.6 ? '#0f172a' : '#ffffff'
  } catch {
    return '#ffffff'
  }
}

/** `#rrggbb` rengini "r, g, b" biçimine çevirir; geçersizse null döner. */
function hexToRgb(hex) {
  try {
    const h = String(hex).replace('#', '')
    if (h.length !== 6) return null
    const r = parseInt(h.substring(0, 2), 16)
    const g = parseInt(h.substring(2, 4), 16)
    const b = parseInt(h.substring(4, 6), 16)
    if ([r, g, b].some((n) => Number.isNaN(n))) return null
    return `${r}, ${g}, ${b}`
  } catch {
    return null
  }
}

/** `#rrggbb` -> [r, g, b]; geçersizse null. */
function hexDizi(hex) {
  try {
    const h = String(hex).replace('#', '')
    if (h.length !== 6) return null
    const dizi = [0, 2, 4].map((i) => parseInt(h.substring(i, i + 2), 16))
    return dizi.some((n) => Number.isNaN(n)) ? null : dizi
  } catch {
    return null
  }
}

function rgbHex(dizi) {
  return '#' + dizi.map((v) => Math.round(v).toString(16).padStart(2, '0')).join('')
}

function karistir(a, b, oran) {
  return a.map((v, i) => v + (b[i] - v) * oran)
}

/** WCAG bağıl parlaklık (0-1). */
function parlaklik(rgb) {
  const kanal = (v) => {
    const s = v / 255
    return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4)
  }
  return 0.2126 * kanal(rgb[0]) + 0.7152 * kanal(rgb[1]) + 0.0722 * kanal(rgb[2])
}

/** İki rengin WCAG kontrast oranı (1-21); geçersiz renkte 0. */
export function kontrastOrani(hexA, hexB) {
  const a = hexDizi(hexA)
  const b = hexDizi(hexB)
  if (!a || !b) return 0
  const [l1, l2] = [parlaklik(a), parlaklik(b)].sort((x, y) => y - x)
  return (l1 + 0.05) / (l2 + 0.05)
}

/**
 * Marka renginden küçük metin için WCAG-güvenli bir ton üretir.
 *
 * Hedef zemin `%18 aksan + kart` karışımıdır (rozet / aktif çip). Kart
 * zemininden biraz daha zorlayıcı olduğu için sonuç her iki yüzeyde de geçer.
 * Koyu temada beyaza, açık temada siyaha doğru ikili aramayla >= 5:1 aranır;
 * 4.6 yerine 5 hedeflenir çünkü aktif çiplerde zemin `--bg-muted` üzerine
 * bindiğinde varsayılan karışımdan birkaç puan daha koyu olabiliyor.
 */
export function aksanMetinRengi(hex, koyu) {
  const taban = hexDizi(hex)
  if (!taban) return koyu ? '#2dd4bf' : '#115e59'
  const kart = koyu ? [20, 27, 38] : [255, 255, 255]
  const zemin = rgbHex(karistir(kart, taban, 0.18))
  if (kontrastOrani(hex, zemin) >= 5) return hex
  const hedef = koyu ? [255, 255, 255] : [0, 0, 0]
  let alt = 0
  let ust = 1
  for (let i = 0; i < 12; i += 1) {
    const oran = (alt + ust) / 2
    const deneme = rgbHex(karistir(taban, hedef, oran))
    if (kontrastOrani(deneme, zemin) >= 5) ust = oran
    else alt = oran
  }
  return rgbHex(karistir(taban, hedef, ust))
}

function applyColor(hex) {
  if (!hex) return
  accentColor.value = hex
  const style = document.documentElement.style
  style.setProperty('--accent', hex)
  style.setProperty('--accent-hover', hex + 'dd')
  style.setProperty('--accent-contrast', kontrastRengi(hex))
  style.setProperty('--primary-color', hex)
  style.setProperty('--primary-color-hover', hex + 'dd')
  const rgb = hexToRgb(hex)
  if (rgb) {
    style.setProperty('--accent-soft', `rgba(${rgb}, 0.18)`)
    style.setProperty('--accent-soft-strong', `rgba(${rgb}, 0.28)`)
    style.setProperty('--accent-border', `rgba(${rgb}, 0.42)`)
  }
  // Küçük aksan metinleri için okunabilir ton (koyu: beyaza, açık: siyaha).
  const koyu = document.documentElement.getAttribute('data-theme') !== 'light'
  style.setProperty('--accent-text', aksanMetinRengi(hex, koyu))
  localStorage.setItem(COLOR_KEY, hex)
}

function initTheme() {
  try {
    if (window.matchMedia) {
      systemMedia = window.matchMedia('(prefers-color-scheme: dark)')
      if (systemMedia && systemMedia.addEventListener && !mediaHandler) {
        mediaHandler = () => {
          if (mode.value === 'system') applyMode('system')
        }
        systemMedia.addEventListener('change', mediaHandler)
      }
    }
  } catch {
    /* matchMedia desteklenmiyor */
  }
  applyMode(mode.value)
  applyColor(accentColor.value)
}

/**
 * matchMedia 'change' listener'ini kaldirir (component unmount temizligi).
 * Birden fazla component initTheme cagirsa da tek listener eklenir; dispose
 * sonrasi yeniden initTheme cagrilirsa listener tekrar eklenir.
 */
function disposeTheme() {
  try {
    if (systemMedia && mediaHandler && systemMedia.removeEventListener) {
      systemMedia.removeEventListener('change', mediaHandler)
    }
  } catch {
    /* matchMedia desteklenmiyor */
  }
  mediaHandler = null
}

const isDark = computed(() => {
  if (mode.value === 'system') return systemTercihiKaranlik()
  return mode.value === 'dark'
})

export function useTheme() {
  return { mode, isDark, accentColor, applyMode, applyColor, initTheme, disposeTheme }
}
