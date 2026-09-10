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
      <svg
        v-if="stok?.barkod"
        ref="barkodSvgRef"
        class="etiket-barkod"
      />
      <img
        v-if="stok?.barkod"
        :src="etiketQr(stok.barkod)"
        alt="Barkod"
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

const { t } = useI18n()

const visible = defineModel('visible', { type: Boolean, default: false })
const props = defineProps({
  stok: { type: Object, default: null }
})

const barkodSvgRef = ref(null)

watch(visible, async (acik) => {
  if (acik && props.stok?.barkod) {
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
})

const etiketQr = (deger) => `https://api.qrserver.com/v1/create-qr-code/?data=${encodeURIComponent(deger)}&size=160x160`

const etiketYazdir = () => {
  const win = window.open('', '_blank', 'width=400,height=500')
  if (!win) return
  win.document.write(`
    <html><head><title>Barkod Etiket</title></head>
    <body style="font-family: sans-serif; text-align: center; padding: 20px;">
      <div style="font-size: 22px; font-weight: 700;">${props.stok?.ad || ''}</div>
      <div style="font-size: 16px; color: #555;">${props.stok?.stokKodu || props.stok?.barkod || ''}</div>
      ${props.stok?.barkod ? `<img src="${etiketQr(props.stok.barkod)}" width="200" height="200" />` : ''}
      <div style="font-size: 20px; font-weight: 700; margin-top: 10px;">${formatCurrency(props.stok?.satisFiyati)}</div>
    </body></html>
  `)
  win.document.close()
  setTimeout(() => {
    win.focus()
    win.print()
  }, 800)
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