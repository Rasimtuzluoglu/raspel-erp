import { onMounted, onUnmounted } from 'vue'
import router from '../router'

const tuslar = new Map()

// Ayni kisayol birden fazla bilesen tarafindan kaydedilebilir; her biri icin
// ayri handler tutulur. Bir bilesen unmount oldugunda yalnizca kendi handler'i
// kaldirilir (diger bilesenlerin kisayolu bozulmaz).
function kaydet(eylem, fn) {
  if (!tuslar.has(eylem)) tuslar.set(eylem, new Set())
  tuslar.get(eylem).add(fn)
}

function kaldir(eylem, fn) {
  const set = tuslar.get(eylem)
  if (!set) return
  set.delete(fn)
  if (set.size === 0) tuslar.delete(eylem)
}

function calistir(eylem) {
  const set = tuslar.get(eylem)
  if (!set || set.size === 0) return false
  set.forEach((fn) => fn())
  return true
}

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

// `g` + harf ile gezinme ozelligini gecici olarak kapatmak icin sayisal saya.
// POS (HizliSatis) `n/k/h/t/p/g` harflerini ODEME YONTEMI ve GERI AL icin
// kullaniyordu; `g` ile baslayan gezinme bu harfleri yutuyor, yani POS'tan
// cikis kisayolu (`g h`) sessizce OLMEYDI. Saya 0'dan buyukse gezinme kapali.
let gezinmeAskida = 0

function handler(e) {
  const ctrl = e.ctrlKey || e.metaKey
  // e.key bazi sentetik/edge-case olaylarda tanimsiz olabilir; cokmesin.
  const key = (e.key || '').toLowerCase()

  if (ctrl && key === 's') {
    if (calistir('kaydet')) {
      e.preventDefault()
    }
    return
  }
  if (e.key === 'Escape') {
    if (gAktif) {
      gAktif = false
      clearTimeout(gTimer)
      return
    }
    calistir('iptal')
    return
  }
  if (e.key === 'F2') {
    if (calistir('yeni')) {
      e.preventDefault()
    }
    return
  }
  if (ctrl && key === 'p') {
    if (calistir('yazdir')) {
      e.preventDefault()
    }
    return
  }
  if (ctrl && key === 'k') {
    if (calistir('ara')) {
      e.preventDefault()
    }
    return
  }
  // "?" ile kısayol rehberini aç
  if (e.key === '?' && !e.shiftKey && !e.ctrlKey && !e.metaKey && !girdiMi()) {
    // Sayfa kendi ipucunu acmak istiyorsa `ipucu` eylemi calisir ve global
    // rehber DEVREYE GIRMEZ. Boylece POS'ta `?` yerel kısayol şeridini açar.
    if (!calistir('ipucu')) {
      window.dispatchEvent(new CustomEvent('kisayol-rehberi-ac'))
    }
    e.preventDefault()
    return
  }

  if (girdiMi()) {
    gAktif = false
    clearTimeout(gTimer)
    return
  }

  if (!ctrl && !e.metaKey && !e.altKey && gezinmeAskida === 0) {
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

// Capture fazinda dinle: sayfa ozel kisayollari (orn. F2 = yeni kayit)
// App.vue'nun global fallback kisayollarindan once calisir ve isledigi tuslarda
// preventDefault() cagirir (App.vue defaultPrevented kontrolu ile atlar).
window.addEventListener('keydown', handler, true)

/**
 * Sayfaya ozgu kisayol kaydeder.
 *
 * @param {object} secenek
 * @param {Function} [secenek.kaydet]  Ctrl/Cmd+S
 * @param {Function} [secenek.iptal]   Esc (once sayfa dialoglarini kapatir)
 * @param {Function} [secenek.yeni]    F2 (yeni kayit)
 * @param {Function} [secenek.yazdir]  Ctrl/Cmd+P
 * @param {Function} [secenek.ara]     Ctrl/Cmd+K
 * @param {Function} [secenek.ipucu]   `?` — sayfaya ozgu ipucu. Verilirse global
 *   KisayolRehberi acilmaz; bu sayfada o komut yerel ipucuna gider.
 * @param {boolean}  [secenek.gezinmeKapat] `g`+harf gezinmesini kapatir. Tek
 *   harfli kisayollari olan ekranlarda (POS: n/k/h/t/p/g) `g` gezinmesi bu
 *   harfleri yuttugu icin kapatilmalidir.
 */
export function useKisayollar({
  kaydet: kaydetFn,
  iptal,
  yeni,
  yazdir,
  ara,
  ipucu,
  gezinmeKapat = false
} = {}) {
  if (gezinmeKapat) gezinmeAskida++

  onMounted(() => {
    if (kaydetFn) kaydet('kaydet', kaydetFn)
    if (iptal) kaydet('iptal', iptal)
    if (yeni) kaydet('yeni', yeni)
    if (yazdir) kaydet('yazdir', yazdir)
    if (ara) kaydet('ara', ara)
    if (ipucu) kaydet('ipucu', ipucu)
  })
  onUnmounted(() => {
    if (kaydetFn) kaldir('kaydet', kaydetFn)
    if (iptal) kaldir('iptal', iptal)
    if (yeni) kaldir('yeni', yeni)
    if (yazdir) kaldir('yazdir', yazdir)
    if (ara) kaldir('ara', ara)
    if (ipucu) kaldir('ipucu', ipucu)
    // Saya KESIN azaltilir: ayni sayfa iki kez mount olup iki kez unmount
    // olsa da (HMR, keep-alive) sayac kalici olarak sifirlanmaz, aksine
    // asagi duserse gecici kisayollar sayfadan cikinca gezinme KAPANIR kalir.
    gezinmeAskida = Math.max(0, gezinmeAskida - 1)
  })
}
