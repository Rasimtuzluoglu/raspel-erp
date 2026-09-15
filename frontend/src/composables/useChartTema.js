import { computed } from 'vue'
import { useTheme } from './useTheme.js'

/**
 * Grafik (Chart.js) renklerini uygulama temasından (CSS değişkenleri) türetir.
 * Açık/koyu tema değişince grafik eksen/lejant/ızgara renkleri de uyum sağlar.
 */
function cssDegisken(ad, varsayilan) {
  if (typeof document === 'undefined') return varsayilan
  const v = getComputedStyle(document.documentElement).getPropertyValue(ad)
  return (v && v.trim()) || varsayilan
}

export function useChartTema() {
  const { isDark, accentColor } = useTheme()

  const palet = computed(() => {
    // isDark'a bağımlılık: tema değişince yeniden hesaplanır
    void isDark.value
    return {
      metin: cssDegisken('--text-secondary', '#94a3b8'),
      izgara: cssDegisken('--border', 'rgba(148, 163, 184, 0.2)'),
      vurgu: accentColor.value || '#3b82f6'
    }
  })

  const lejant = () => ({
    position: 'bottom',
    labels: { color: palet.value.metin, usePointStyle: true, boxWidth: 8, padding: 14 }
  })

  return { palet, lejant }
}
