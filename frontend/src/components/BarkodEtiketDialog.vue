<template>
  <Dialog
    v-model:visible="visible"
    :header="t('stoklar.barkodEtiket')"
    :modal="true"
    style="width: 340px"
  >
    <div class="etiket-tip-secim">
      <button
        type="button"
        :class="{ aktif: tip === 'barkod' }"
        @click="tipSec('barkod')"
      >
        {{ t('stoklar.etiketBarkod') }}
      </button>
      <button
        type="button"
        :class="{ aktif: tip === 'qr' }"
        @click="tipSec('qr')"
      >
        {{ t('stoklar.etiketQr') }}
      </button>
    </div>

    <div class="etiket-kart">
      <div class="etiket-ad">
        {{ stok?.ad }}
      </div>
      <div class="etiket-kod">
        {{ stok?.stokKodu || stok?.barkod }}
      </div>
      <div
        v-if="stok?.rafNo"
        class="etiket-raf"
      >
        {{ t('stoklar.raf') }}: {{ stok.rafNo }}
      </div>
      <svg
        v-show="barkodGoster && icerik"
        ref="barkodSvgRef"
        class="etiket-barkod"
      />
      <!-- Barkod alani bos urunlerde de etiket uretilir; cubuklar stok kodunu kodlar. -->
      <div
        v-if="stok && !stok.barkod"
        class="etiket-uyari"
      >
        {{ t('stoklar.barkodYerineKod') }}
      </div>
      <div
        v-else-if="barkodHata && barkodGoster"
        class="etiket-uyari"
      >
        {{ t('stoklar.barkodCizilemedi') }}
      </div>
      <img
        v-if="qrGoster && (qrDataUrl || qrUrl)"
        :src="qrDataUrl || qrUrl"
        alt="QR"
        class="etiket-qr"
      >
      <div class="etiket-fiyat">
        {{ formatCurrency(stok?.satisFiyati ?? stok?.fiyat) }}
      </div>
    </div>
    <template #footer>
      <Button
        :label="t('stoklar.kapat')"
        icon="pi pi-times"
        class="p-button-text"
        @click="visible = false"
      />
      <Button
        :label="t('stoklar.etiketPdf')"
        icon="pi pi-file-pdf"
        class="p-button-secondary"
        :loading="pdfYukleniyor"
        @click="etiketPdfIndir"
      />
      <Button
        :label="t('stoklar.yazdir')"
        icon="pi pi-print"
        class="p-button-primary"
        @click="etiketYazdir"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatCurrency } from '../utils/format.js'
import { barkodIcerik } from '../utils/barkodIcerik.js'
import { stokAPI } from '../api/index.js'

const { t } = useI18n()

const TIP_ANAHTAR = 'raspel_etiket_tipi'
const visible = defineModel('visible', { type: Boolean, default: false })
const props = defineProps({
  stok: { type: Object, default: null }
})

const barkodSvgRef = ref(null)
const qrUrl = ref(null)
const qrDataUrl = ref(null)
const pdfYukleniyor = ref(false)
const barkodHata = ref(false)

// Etikette kodlanacak icerik: barkod varsa barkod, yoksa stok kodu (backend ile ayni).
const icerik = computed(() => (props.stok ? barkodIcerik(props.stok) : ''))

// Son seçilen etiket türü hatırlanır; ilk kullanımda barkod. Eski sürümlerdeki
// "ikisi" seçimi artık desteklenmiyor; barkoda düşer.
const kayitliTip = localStorage.getItem(TIP_ANAHTAR)
const tip = ref(kayitliTip === 'qr' ? 'qr' : 'barkod')
const barkodGoster = computed(() => tip.value === 'barkod')
const qrGoster = computed(() => tip.value === 'qr')

const qrTemizle = () => {
  if (qrUrl.value) {
    URL.revokeObjectURL(qrUrl.value)
    qrUrl.value = null
  }
  qrDataUrl.value = null
}

const blobToDataUrl = (blob) =>
  new Promise((resolve) => {
    try {
      const okuyucu = new FileReader()
      okuyucu.onload = () => resolve(okuyucu.result)
      okuyucu.onerror = () => resolve(null)
      okuyucu.readAsDataURL(blob)
    } catch {
      resolve(null)
    }
  })

const barkodCiz = async () => {
  if (!barkodGoster.value || !icerik.value) return
  await nextTick()
  barkodHata.value = false
  try {
    const { default: JsBarcode } = await import('jsbarcode')
    if (barkodSvgRef.value) {
      JsBarcode(barkodSvgRef.value, icerik.value, {
        format: 'CODE128',
        width: 2,
        height: 60,
        displayValue: false,
        margin: 0
      })
    }
  } catch {
    // Cizim basarisizsa kullaniciya bilgi verilir (sessiz kalmaz).
    barkodHata.value = true
  }
}

const qrYukle = async () => {
  if (!qrGoster.value || !props.stok?.id) return
  try {
    const { data } = await stokAPI.etiketQr(props.stok.id)
    qrTemizle()
    qrUrl.value = URL.createObjectURL(data)
    // Yazdirma penceresi blob URL'e bagimli kalmasin; data URL'e de cevrilir.
    qrDataUrl.value = await blobToDataUrl(data)
  } catch {
    qrTemizle()
  }
}

const hazirla = async () => {
  await barkodCiz()
  await qrYukle()
}

const tipSec = (yeni) => {
  tip.value = yeni
  localStorage.setItem(TIP_ANAHTAR, yeni)
  nextTick(() => hazirla())
}

watch(visible, async (acik) => {
  if (!acik) {
    qrTemizle()
    return
  }
  await nextTick()
  hazirla()
})

const tipKodu = () => (tip.value === 'qr' ? 'QR' : 'BARKOD')

const escapeHtml = (s) =>
  String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c])

const etiketYazdir = () => {
  const win = window.open('', '_blank', 'width=400,height=520')
  if (!win) return
  const barkodSvg = barkodGoster.value && barkodSvgRef.value ? barkodSvgRef.value.outerHTML : ''
  const qrKaynak = qrDataUrl.value || qrUrl.value || ''
  win.document.write(`
    <html><head><title>${escapeHtml(props.stok?.ad || '')}</title>
    <style>
      body { font-family: sans-serif; text-align: center; padding: 20px; }
      svg { max-width: 100%; height: auto; }
      img { display: block; margin: 10px auto 0; }
    </style>
    </head>
    <body>
      <div style="font-size: 22px; font-weight: 700;">${escapeHtml(props.stok?.ad || '')}</div>
      <div style="font-size: 16px; color: #555;">${escapeHtml(props.stok?.stokKodu || props.stok?.barkod || '')}</div>
      ${props.stok?.rafNo ? `<div style="font-size: 14px; color: #555;">${escapeHtml(t('stoklar.raf'))}: ${escapeHtml(props.stok.rafNo)}</div>` : ''}
      ${barkodSvg ? `<div style="margin-top: 10px;">${barkodSvg}</div>` : ''}
      ${qrGoster.value && qrKaynak ? `<img src="${qrKaynak}" width="180" height="180" />` : ''}
      <div style="font-size: 22px; font-weight: 700; margin-top: 10px;">${formatCurrency(props.stok?.satisFiyati ?? props.stok?.fiyat)}</div>
    </body></html>
  `)
  win.document.close()
  // Gorsellerin yuklenmesini bekleyip yazdir; kacak load olayina karsi emniyet
  // zamanlayicisi da kurulur (cift yazdirmayi bayrak engeller).
  let yazildi = false
  const yazdir = () => {
    if (yazildi) return
    yazildi = true
    try {
      win.focus()
      win.print()
    } catch {
      /* yoksay */
    }
  }
  try {
    if (win.document && win.document.readyState === 'complete') {
      setTimeout(yazdir, 250)
    } else if (typeof win.addEventListener === 'function') {
      win.addEventListener('load', () => setTimeout(yazdir, 150), { once: true })
      setTimeout(yazdir, 2500)
    } else {
      setTimeout(yazdir, 800)
    }
  } catch {
    setTimeout(yazdir, 800)
  }
}

const etiketPdfIndir = async () => {
  if (!props.stok?.id) return
  pdfYukleniyor.value = true
  try {
    const { data } = await stokAPI.etiketPdf(props.stok.id, tipKodu())
    const url = URL.createObjectURL(data)
    const win = window.open(url, '_blank')
    if (!win) {
      const a = document.createElement('a')
      a.href = url
      a.download = `etiket-${props.stok.id}.pdf`
      a.click()
    }
    setTimeout(() => URL.revokeObjectURL(url), 60000)
  } finally {
    pdfYukleniyor.value = false
  }
}
</script>

<style scoped>
.etiket-tip-secim {
  display: flex;
  gap: 6px;
  margin-bottom: 12px;
}
.etiket-tip-secim button {
  flex: 1;
  padding: 7px 6px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--bg-primary);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}
.etiket-tip-secim button:hover:not(:disabled) {
  border-color: var(--accent, var(--accent));
  color: var(--accent, var(--accent));
}
.etiket-tip-secim button.aktif {
  background: var(--accent-soft);
  border-color: var(--accent, var(--accent));
  color: var(--accent, var(--accent));
}
.etiket-tip-secim button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.etiket-kart {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px;
  border: 1px dashed var(--border);
  border-radius: 8px;
}
.etiket-ad {
  font-size: 1.1rem;
  font-weight: 700;
  text-align: center;
}
.etiket-kod {
  font-size: 0.9rem;
  color: var(--text-secondary);
  font-family: monospace;
}
.etiket-raf {
  font-size: 0.85rem;
  color: var(--text-secondary);
  font-weight: 600;
}
.etiket-uyari {
  font-size: 0.8rem;
  color: var(--text-muted);
  font-style: italic;
  text-align: center;
}
.etiket-qr {
  width: 160px;
  height: 160px;
}
.etiket-barkod {
  width: 220px;
  height: 70px;
}
.etiket-fiyat {
  font-size: 1.35rem;
  font-weight: 800;
  letter-spacing: 0.3px;
}
</style>
