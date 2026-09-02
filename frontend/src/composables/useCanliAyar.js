import { ref, onMounted, onUnmounted } from 'vue'

/**
 * localStorage tabanlı, sekmeler arası canlı senkronize edilebilir ayar.
 * storage event'i dinleyerek başka sekmede yapılan değişikliği anında yansıtır.
 */
export function useCanliAyar(anahtar, varsayilan) {
  const deger = ref(localStorage.getItem(anahtar) ?? varsayilan)

  const ayarla = (yeni) => {
    deger.value = yeni
    localStorage.setItem(anahtar, String(yeni))
  }

  const dinleyici = (e) => {
    if (e.key === anahtar && e.newValue !== null && e.newValue !== String(deger.value)) {
      deger.value = e.newValue
    }
  }

  onMounted(() => window.addEventListener('storage', dinleyici))
  onUnmounted(() => window.removeEventListener('storage', dinleyici))

  return { deger, ayarla }
}
