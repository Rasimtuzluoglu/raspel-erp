import { ref, watch } from 'vue'

// Oturum bazli sunum (musteri) modu. localStorage KULLANILMAZ: sayfa yenilenince
// otomatik kapanir (yanlislikla acik kalma riskini azaltir).
const aktif = ref(false)

// Maske stili tercihi kalici olabilir (bulanik | gizle).
const maske = ref(localStorage.getItem('raspel_erp_sunum_maske') || 'bulanik')

watch(
  aktif,
  (v) => {
    if (typeof document !== 'undefined') {
      document.documentElement.setAttribute('data-sunum', v ? 'on' : 'off')
    }
  },
  { immediate: true }
)

watch(
  maske,
  (v) => {
    if (typeof document !== 'undefined') {
      document.documentElement.setAttribute('data-sunum-maske', v === 'gizle' ? 'gizle' : 'bulanik')
    }
    try {
      localStorage.setItem('raspel_erp_sunum_maske', v)
    } catch {
      /* sessiz */
    }
  },
  { immediate: true }
)

export function useSunumModu() {
  const ac = () => {
    aktif.value = true
  }
  const kapat = () => {
    aktif.value = false
  }
  const degistir = () => {
    aktif.value = !aktif.value
  }
  const maskeAyarla = (v) => {
    maske.value = v === 'gizle' ? 'gizle' : 'bulanik'
  }
  return { aktif, maske, ac, kapat, degistir, maskeAyarla }
}
