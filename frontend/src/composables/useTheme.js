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
