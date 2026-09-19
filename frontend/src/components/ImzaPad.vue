<template>
  <div class="imza-pad">
    <div class="imza-pad-ust">
      <span class="imza-pad-etiket">{{ etiket }}</span>
      <button
        type="button"
        class="imza-temizle"
        @click="temizle"
      >
        {{ $t('imzaPad.temizle') }}
      </button>
    </div>
    <canvas
      ref="canvas"
      class="imza-canvas"
      :class="{ cizili: cizili }"
      @mousedown="baslat"
      @mousemove="ciz"
      @mouseup="bitir"
      @mouseleave="bitir"
      @touchstart="baslatTouch"
      @touchmove="cizTouch"
      @touchend="bitir"
    />
    <small class="imza-ipucu">{{ $t('imzaPad.ipucu') }}</small>
  </div>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  etiket: { type: String, default: '' }
})

const canvas = ref(null)
const cizili = ref(false)
let ctx = null
let ciziyor = false

/** Canvas'ı görünür boyuta göre hazırlar; modal açıldıktan sonra çağrılmalı. */
const hazirla = () => {
  if (!canvas.value) return
  const oran = window.devicePixelRatio || 1
  const genislik = canvas.value.offsetWidth || 320
  const yukseklik = canvas.value.offsetHeight || 140
  canvas.value.width = genislik * oran
  canvas.value.height = yukseklik * oran
  ctx = canvas.value.getContext('2d')
  ctx.scale(oran, oran)
  ctx.lineWidth = 2
  ctx.lineCap = 'round'
  ctx.lineJoin = 'round'
  ctx.strokeStyle = '#1e293b'
  cizili.value = false
}

const temizle = () => {
  if (ctx && canvas.value) {
    ctx.clearRect(0, 0, canvas.value.width, canvas.value.height)
  }
  cizili.value = false
}

const nokta = (e) => {
  const r = canvas.value.getBoundingClientRect()
  return { x: e.clientX - r.left, y: e.clientY - r.top }
}

const baslat = (e) => {
  if (!ctx) return
  ciziyor = true
  ctx.beginPath()
  ctx.moveTo(e.offsetX, e.offsetY)
}

const ciz = (e) => {
  if (!ciziyor || !ctx) return
  ctx.lineTo(e.offsetX, e.offsetY)
  ctx.stroke()
  cizili.value = true
}

const baslatTouch = (e) => {
  if (!ctx) return
  e.preventDefault()
  ciziyor = true
  const p = nokta(e.touches[0])
  ctx.beginPath()
  ctx.moveTo(p.x, p.y)
}

const cizTouch = (e) => {
  if (!ctx || !ciziyor) return
  e.preventDefault()
  const p = nokta(e.touches[0])
  ctx.lineTo(p.x, p.y)
  ctx.stroke()
  cizili.value = true
}

const bitir = () => {
  ciziyor = false
}

const bosMu = () => !cizili.value

const toBlob = () =>
  new Promise((resolve) => {
    if (!canvas.value || typeof canvas.value.toBlob !== 'function') return resolve(null)
    canvas.value.toBlob((b) => resolve(b), 'image/png')
  })

defineExpose({ hazirla, temizle, bosMu, toBlob })
</script>

<style scoped>
.imza-pad {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.imza-pad-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.imza-pad-etiket {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
}
.imza-temizle {
  border: none;
  background: transparent;
  color: var(--danger, #ef4444);
  font-size: 12px;
  cursor: pointer;
  padding: 0;
}
.imza-temizle:hover {
  text-decoration: underline;
}
.imza-canvas {
  width: 100%;
  height: 160px;
  border: 2px dashed var(--border);
  border-radius: 10px;
  background: var(--bg-primary);
  touch-action: none;
  cursor: crosshair;
}
.imza-canvas.cizili {
  border-style: solid;
  border-color: var(--accent);
}
.imza-ipucu {
  font-size: 11px;
  color: var(--text-muted);
}
</style>
