<template>
  <Dialog
    v-model:visible="visible"
    :header="t('stoklar.barkodEtiket')"
    :modal="true"
    style="width: 320px"
  >
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
        v-if="stok?.barkod"
        ref="barkodSvgRef"
        class="etiket-barkod"
      />
      <img
        v-if="qrUrl"
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
import { ref, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatCurrency } from '../utils/format.js'
import { stokAPI } from '../api/index.js'

const { t } = useI18n()

const visible = defineModel('visible', { type: Boolean, default: false })
const props = defineProps({
  stok: { type: Object, default: null }
})

const barkodSvgRef = ref(null)
const qrUrl = ref(null)
const pdfYukleniyor = ref(false)

const qrTemizle = () => {
  if (qrUrl.value) {
    URL.revokeObjectURL(qrUrl.value)
    qrUrl.value = null
  }
}

watch(visible, async (acik) => {
  if (!acik) {
    qrTemizle()
    return
  }
  if (props.stok?.barkod) {
    await nextTick()
    try {
      const { default: JsBarcode } = await import('jsbarcode')
      JsBarcode(barkodSvgRef.value, props.stok.barkod, {
        format: 'CODE128',
        width: 2,
        height: 60,
        displayValue: false,
        margin: 0
      })
    } catch {
      /* jsbarcode yuklenemedi */
    }
  }
  if (props.stok?.id) {
    try {
      const { data } = await stokAPI.etiketQr(props.stok.id)
      qrTemizle()
      qrUrl.value = URL.createObjectURL(data)
    } catch {
      qrTemizle()
    }
  }
})

const etiketYazdir = () => {
  const win = window.open('', '_blank', 'width=400,height=500')
  if (!win) return
  win.document.write(`
    <html><head><title>Barkod Etiket</title></head>
    <body style="font-family: sans-serif; text-align: center; padding: 20px;">
      <div style="font-size: 22px; font-weight: 700;">${props.stok?.ad || ''}</div>
      <div style="font-size: 16px; color: #555;">${props.stok?.stokKodu || props.stok?.barkod || ''}</div>
      ${props.stok?.rafNo ? `<div style="font-size: 14px; color: #555;">Raf: ${props.stok.rafNo}</div>` : ''}
      ${qrUrl.value ? `<img src="${qrUrl.value}" width="200" height="200" />` : ''}
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
    const { data } = await stokAPI.etiketPdf(props.stok.id)
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