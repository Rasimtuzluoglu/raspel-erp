import { ref, watch } from 'vue'

// Oturum bazli sunum (musteri) modu. localStorage KULLANILMAZ: sayfa yenilenince
// otomatik kapanir (yanlislikla acik kalma riskini azaltir).
const aktif = ref(false)

watch(
  aktif,
  (v) => {
    if (typeof document !== 'undefined') {
      document.documentElement.setAttribute('data-sunum', v ? 'on' : 'off')
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
  return { aktif, ac, kapat, degistir }
}
