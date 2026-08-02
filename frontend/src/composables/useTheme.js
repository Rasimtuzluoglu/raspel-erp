import { ref, computed } from 'vue'

const MODE_KEY = 'raspel_erp_theme'
const COLOR_KEY = 'raspel_primary_color'

const mode = ref(localStorage.getItem(MODE_KEY) || 'dark')
const accentColor = ref(localStorage.getItem(COLOR_KEY) || '#3b82f6')

function applyMode(m) {
  mode.value = m === 'light' ? 'light' : 'dark'
  const root = document.documentElement
  root.setAttribute('data-theme', mode.value)
  root.classList.toggle('p-dark', mode.value === 'dark')
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
  applyMode(mode.value)
  applyColor(accentColor.value)
}

const isDark = computed(() => mode.value === 'dark')

export function useTheme() {
  return { mode, isDark, accentColor, applyMode, applyColor, initTheme }
}
