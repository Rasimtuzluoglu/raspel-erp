import { onMounted, onUnmounted } from 'vue'
import router from '../router'

const tuslar = new Map()

let gTimer = null
let gAktif = false

const gezinme = {
  c: '/cari-hesaplar',
  f: '/faturalar',
  s: '/stoklar',
  b: '/bankalar',
  k: '/kasa',
  p: '/personel',
  h: '/hizli-satis',
  r: '/raporlar',
  n: '/notlar',
  d: '/'
}

function girdiMi() {
  const aktif = document.activeElement
  return aktif && (aktif.tagName === 'INPUT' || aktif.tagName === 'TEXTAREA' || aktif.isContentEditable)
}

function handler(e) {
  const ctrl = e.ctrlKey || e.metaKey
  const key = e.key.toLowerCase()

  if (ctrl && key === 's') {
    if (tuslar.has('kaydet')) {
      e.preventDefault()
      tuslar.get('kaydet')()
    }
    return
  }
  if (e.key === 'Escape') {
    if (gAktif) {
      gAktif = false
      clearTimeout(gTimer)
      return
    }
    if (tuslar.has('iptal')) {
      tuslar.get('iptal')()
    }
    return
  }
  if (e.key === 'F2') {
    if (tuslar.has('yeni')) {
      e.preventDefault()
      tuslar.get('yeni')()
    }
    return
  }
  if (ctrl && key === 'p') {
    if (tuslar.has('yazdir')) {
      e.preventDefault()
      tuslar.get('yazdir')()
    }
    return
  }
  if (ctrl && key === 'k') {
    if (tuslar.has('ara')) {
      e.preventDefault()
      tuslar.get('ara')()
    }
    return
  }

  if (girdiMi()) {
    gAktif = false
    clearTimeout(gTimer)
    return
  }

  if (!ctrl && !e.metaKey && !e.altKey) {
    if (key === 'g') {
      e.preventDefault()
      gAktif = true
      clearTimeout(gTimer)
      gTimer = setTimeout(() => {
        gAktif = false
      }, 1000)
      return
    }
    if (gAktif && gezinme[key]) {
      e.preventDefault()
      gAktif = false
      clearTimeout(gTimer)
      router.push(gezinme[key])
    }
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
