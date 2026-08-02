import { onMounted, onUnmounted } from 'vue'

const tuslar = new Map()

function handler(e) {
  const ctrl = e.ctrlKey || e.metaKey
  const key = e.key.toLowerCase()

  if (ctrl && key === 's') {
    if (tuslar.has('kaydet')) { e.preventDefault(); tuslar.get('kaydet')() }
    return
  }
  if (e.key === 'Escape') {
    if (tuslar.has('iptal')) { tuslar.get('iptal')() }
    return
  }
  if (e.key === 'F2') {
    if (tuslar.has('yeni')) { e.preventDefault(); tuslar.get('yeni')() }
    return
  }
  if (ctrl && key === 'p') {
    if (tuslar.has('yazdir')) { e.preventDefault(); tuslar.get('yazdir')() }
    return
  }
  if (ctrl && key === 'k') {
    if (tuslar.has('ara')) { e.preventDefault(); tuslar.get('ara')() }
  }
}

window.addEventListener('keydown', handler)

export function useKisayollar({ kaydet, iptal, yeni, yazdir, ara } = {}) {
  onMounted(() => {
    if (kaydet) tuslar.set('kaydet', kaydet)
    if (iptal) tuslar.set('iptal', iptal)
    if (yeni) tuslar.set('yeni', yeni)
    if (yazdir) tuslar.set('yazdir', yazdir)
    if (ara) tuslar.set('ara', ara)
  })
  onUnmounted(() => {
    if (kaydet) tuslar.delete('kaydet')
    if (iptal) tuslar.delete('iptal')
    if (yeni) tuslar.delete('yeni')
    if (yazdir) tuslar.delete('yazdir')
    if (ara) tuslar.delete('ara')
  })
}
