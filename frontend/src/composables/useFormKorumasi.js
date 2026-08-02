import { watch, onMounted, onUnmounted } from 'vue'

export function useFormKorumasi(form, { aktif = true, mesaj = 'Kaydedilmemiş değişiklikler var. Sayfadan ayrılmak istediğinize emin misiniz?' } = {}) {
  let kirli = false

  const isaretle = () => { kirli = true }
  const temizle = () => { kirli = false }

  const beforeUnloadHandler = (e) => {
    if (!kirli || !aktif) return
    e.preventDefault()
    e.returnValue = mesaj
    return mesaj
  }

  watch(form, () => { kirli = true }, { deep: true })

  onMounted(() => window.addEventListener('beforeunload', beforeUnloadHandler))
  onUnmounted(() => window.removeEventListener('beforeunload', beforeUnloadHandler))

  return { isaretle, temizle }
}
