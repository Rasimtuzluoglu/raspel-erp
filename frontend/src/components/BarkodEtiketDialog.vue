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
        :disabled="!stok?.barkod"
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
      <button
        type="button"
        :class="{ aktif: tip === 'ikisi' }"
        :disabled="!stok?.barkod"
        @click="tipSec('ikisi')"
      >
        {{ t('stoklar.etiketIkisi') }}
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
        v-show="barkodGoster && stok?.barkod"
        ref="barkodSvgRef"
        class="etiket-barkod"
      />
      <div
        v-if="tip !== 'qr' && stok && !stok.barkod"
        class="etiket-uyari"
      >
        {{ t('stoklar.barkodYok') }}
      </div>
      <img
        v-if="qrGoster && qrUrl"
        :src="qrUrl"
        alt="QR"
        class="etiket-qr"
      >
      <div class="etiket-fiyat">
        {{ formatCurrency(stok?.satisFiyati) }}
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
import { stokAPI } from '../api/index.js'

const { t } = useI18n()

const TIP_ANAHTAR = 'raspel_etiket_tipi'
const visible = defineModel('visible', { type: Boolean, default: false })
const props = defineProps({
  stok: { type: Object, default: null }
})

const barkodSvgRef = ref(null)
const qrUrl = ref(null)
const pdfYukleniyor = ref(false)

// Son seçilen etiket türü hatırlanır; ilk kullanımda barkod.
const tip = ref(localStorage.getItem(TIP_ANAHTAR) || 'barkod')
const barkodGoster = computed(() => tip.value === 'barkod' || tip.value === 'ikisi')
const qrGoster = computed(() => tip.value === 'qr' || tip.value === 'ikisi')

const qrTemizle = () => {
  if (qrUrl.value) {
    URL.revokeObjectURL(qrUrl.value)
    qrUrl.value = null
  }
}

const barkodCiz = async () => {
  if (!barkodGoster.value || !props.stok?.barkod) return
  await nextTick()
  try {
    const { default: JsBarcode } = await import('jsbarcode')
    if (barkodSvgRef.value) {
      JsBarcode(barkodSvgRef.value, props.stok.barkod, {
        format: 'CODE128',
        width: 2,
        height: 60,
        displayValue: false,
        margin: 0
      })
    }
  } catch {
    /* jsbarcode yuklenemedi */
  }
}

const qrYukle = async () => {
  if (!qrGoster.value || !props.stok?.id) return
  try {
    const { data } = await stokAPI.etiketQr(props.stok.id)
    qrTemizle()
    qrUrl.value = URL.createObjectURL(data)
  } catch {
    qrTemizle()
  }
}

const hazirla = async () => {
  await barkodCiz()
  await qrYukle()
}

const tipSec = (yeni) => {
  if (yeni !== 'qr' && !props.stok?.barkod) return
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

const tipKodu = () => (tip.value === 'qr' ? 'QR' : tip.value === 'ikisi' ? 'IKISI' : 'BARKOD')

const escapeHtml = (s) =>
  String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c])

const etiketYazdir = () => {
  const win = window.open('', '_blank', 'width=400,height=520')
  if (!win) return
  const barkodSvg = barkodGoster.value && barkodSvgRef.value ? barkodSvgRef.value.outerHTML : ''
  win.document.write(`
    <html><head><title>${escapeHtml(props.stok?.ad || '')}</title></head>
    <body style="font-family: sans-serif; text-align: center; padding: 20px;">
      <div style="font-size: 22px; font-weight: 700;">${escapeHtml(props.stok?.ad || '')}</div>
      <div style="font-size: 16px; color: #555;">${escapeHtml(props.stok?.stokKodu || props.stok?.barkod || '')}</div>
      ${props.stok?.rafNo ? `<div style="font-size: 14px; color: #555;">${escapeHtml(t('stoklar.raf'))}: ${escapeHtml(props.stok.rafNo)}</div>` : ''}
      ${barkodSvg ? `<div style="margin-top: 10px;">${barkodSvg}</div>` : ''}
      ${qrGoster.value && qrUrl.value ? `<img src="${qrUrl.value}" width="180" height="180" style="margin-top:10px;" />` : ''}
      <div style="font-size: 20px; font-weight: 700; margin-top: 10px;">${formatCurrency(props.stok?.satisFiyati)}</div>
    </body></html>
  `)
  win.document.close()
  setTimeout(() => {
    win.focus()
    win.print()
  }, 800)
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
  font-size: 1.2rem;
  font-weight: 700;
}
</style>
