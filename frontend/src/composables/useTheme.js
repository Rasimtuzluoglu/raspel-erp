import { ref, computed } from 'vue'

const MODE_KEY = 'raspel_erp_theme'
const COLOR_KEY = 'raspel_primary_color'

const mode = ref(localStorage.getItem(MODE_KEY) || 'dark')
const accentColor = ref(localStorage.getItem(COLOR_KEY) || '#3b82f6')

let systemMedia = null

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

function applyColor(hex) {
  if (!hex) return
  accentColor.value = hex
  const style = document.documentElement.style
  style.setProperty('--accent', hex)
  style.setProperty('--accent-hover', hex + 'dd')
  style.setProperty('--primary-color', hex)
  style.setProperty('--primary-color-hover', hex + 'dd')
  localStorage.setItem(COLOR_KEY, hex)
}

function initTheme() {
  try {
    if (window.matchMedia) {
      systemMedia = window.matchMedia('(prefers-color-scheme: dark)')
      if (systemMedia && systemMedia.addEventListener) {
        systemMedia.addEventListener('change', () => {
          if (mode.value === 'system') applyMode('system')
        })
      }
    }
  } catch {
    /* matchMedia desteklenmiyor */
  }
  applyMode(mode.value)
  applyColor(accentColor.value)
}

const isDark = computed(() => {
  if (mode.value === 'system') return systemTercihiKaranlik()
  return mode.value === 'dark'
})

export function useTheme() {
  return { mode, isDark, accentColor, applyMode, applyColor, initTheme }
}
