<template>
  <Dialog
    :visible="visible"
    header="Kameralı Barkod Okuyucu"
    modal
    :style="{ width: '500px' }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div class="scanner-container">
      <div
        v-if="cameraError"
        class="error-msg"
      >
        <i class="pi pi-exclamation-triangle" />
        <span>{{ cameraError }}</span>
      </div>

      <div
        v-else
        class="video-wrapper"
      >
        <video
          ref="videoRef"
          autoplay
          playsinline
          muted
          class="camera-video"
        />
        <div class="scan-overlay">
          <div class="scan-laser" />
        </div>
      </div>

      <div class="manual-input-box">
        <label>Veya Manuel Barkod Girin:</label>
        <div class="input-group">
          <InputText
            v-model="manualBarcode"
            placeholder="Barkod No..."
            class="w-full"
            @keyup.enter="submitManual"
          />
          <Button
            icon="pi pi-check"
            label="Ekle"
            @click="submitManual"
          />
        </div>
      </div>
    </div>
    <template #footer>
      <Button
        label="Kapat"
        icon="pi pi-times"
        class="p-button-text"
        @click="closeModal"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { ref, watch, onUnmounted } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false }
})

const emit = defineEmits(['update:visible', 'scan'])

const videoRef = ref(null)
const manualBarcode = ref('')
const cameraError = ref(null)
let streamInstance = null
let animationFrameId = null

const startCamera = async () => {
  cameraError.value = null
  try {
    const stream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: 'environment', width: { ideal: 1280 }, height: { ideal: 720 } }
    })
    streamInstance = stream
    if (videoRef.value) {
      videoRef.value.srcObject = stream
      detectBarcode()
    }
  } catch (err) {
    cameraError.value = 'Kameraya erişilemedi veya kamera izni verilmedi. Manuel barkod girişini kullanabilirsiniz.'
  }
}

const stopCamera = () => {
  if (animationFrameId) cancelAnimationFrame(animationFrameId)
  if (streamInstance) {
    streamInstance.getTracks().forEach(track => track.stop())
    streamInstance = null
  }
}

const detectBarcode = async () => {
  if (!('BarcodeDetector' in window)) {
    return // BarcodeDetector not supported in browser, fallback to manual or user scan
  }

  try {
    const barcodeDetector = new window.BarcodeDetector({ formats: ['code_128', 'ean_13', 'ean_8', 'qr_code'] })
    const scanFrame = async () => {
      if (videoRef.value && videoRef.value.readyState === videoRef.value.HAVE_ENOUGH_DATA) {
        try {
          const barcodes = await barcodeDetector.detect(videoRef.value)
          if (barcodes.length > 0) {
            const code = barcodes[0].rawValue
            emit('scan', code)
            closeModal()
            return
          }
        } catch { /* empty */ }
      }
      animationFrameId = requestAnimationFrame(scanFrame)
    }
    scanFrame()
  } catch { /* empty */ }
}

const submitManual = () => {
  if (manualBarcode.value.trim()) {
    emit('scan', manualBarcode.value.trim())
    manualBarcode.value = ''
    closeModal()
  }
}

const closeModal = () => {
  stopCamera()
  emit('update:visible', false)
}

watch(() => props.visible, (val) => {
  if (val) {
    startCamera()
  } else {
    stopCamera()
  }
})

onUnmounted(() => {
  stopCamera()
})
</script>

<style scoped>
.scanner-container {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}
.video-wrapper {
  position: relative;
  width: 100%;
  height: 260px;
  background: #000;
  border-radius: 12px;
  overflow: hidden;
}
.camera-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.scan-overlay {
  position: absolute;
  top: 15%; left: 10%; right: 10%; bottom: 15%;
  border: 2px dashed rgba(59, 130, 246, 0.8);
  border-radius: 10px;
  box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.4);
}
.scan-laser {
  width: 100%;
  height: 2px;
  background: #ef4444;
  box-shadow: 0 0 8px #ef4444;
  animation: scanAnim 2s infinite ease-in-out;
}
@keyframes scanAnim {
  0% { transform: translateY(0); }
  50% { transform: translateY(180px); }
  100% { transform: translateY(0); }
}
.error-msg {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
  padding: 1rem;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.9rem;
}
.input-group {
  display: flex;
  gap: 0.5rem;
  margin-top: 0.35rem;
}
.w-full { width: 100%; }
</style>
