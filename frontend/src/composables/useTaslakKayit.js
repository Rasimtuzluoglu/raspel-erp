import { watch, onUnmounted } from 'vue'

const ANAHTAR_ON = 'raspel_taslak_'

export function useTaslakKayit(anahtar, form, opts = {}) {
  const { timeout = 500, restore = true, onRestore } = opts
  const storageAnahtari = ANAHTAR_ON + anahtar
  let timer = null

  watch(
    form,
    () => {
      if (timer) clearTimeout(timer)
      timer = setTimeout(() => {
        try {
          localStorage.setItem(storageAnahtari, JSON.stringify(form.value))
        } catch {
          /* empty */
        }
      }, timeout)
    },
    { deep: true }
  )

  const geriYukle = () => {
    try {
      const kayitli = localStorage.getItem(storageAnahtari)
      if (kayitli) {
        const veri = JSON.parse(kayitli)
        Object.assign(form.value, veri)
        if (onRestore) onRestore(veri)
        return true
      }
    } catch {
      /* empty */
    }
    return false
  }

  const temizle = () => {
    try {
      localStorage.removeItem(storageAnahtari)
    } catch {
      /* empty */
    }
  }

  if (restore) geriYukle()

  onUnmounted(() => {
    if (timer) clearTimeout(timer)
  })

  return { geriYukle, temizle }
}
