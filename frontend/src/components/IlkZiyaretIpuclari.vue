<template>
  <transition name="ipucu">
    <div
      v-if="gorunur"
      class="ipucu-balon"
    >
      <i class="pi pi-lightbulb ipucu-ikon" />
      <div class="ipucu-icerik">
        <strong>{{ baslik }}</strong>
        <p>{{ metin }}</p>
      </div>
      <button
        class="ipucu-kapat"
        title="Kapat"
        @click="kapat"
      >
        <i class="pi pi-times" />
      </button>
    </div>
  </transition>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const props = defineProps({
  anahtar: { type: String, required: true },
  baslik: { type: String, default: 'İpucu' },
  metin: { type: String, default: '' }
})

const gorunur = ref(false)
const STORE_KEY = 'raspel_gosterilen_ipuclari'

onMounted(() => {
  try {
    const gosterilenler = JSON.parse(localStorage.getItem(STORE_KEY) || '[]')
    if (!gosterilenler.includes(props.anahtar) && !window.location.href.includes('yeni=0')) {
      gorunur.value = true
    }
  } catch { gorunur.value = true }
})

const kapat = () => {
  gorunur.value = false
  try {
    const gosterilenler = JSON.parse(localStorage.getItem(STORE_KEY) || '[]')
    if (!gosterilenler.includes(props.anahtar)) {
      gosterilenler.push(props.anahtar)
      localStorage.setItem(STORE_KEY, JSON.stringify(gosterilenler))
    }
  } catch { /* empty */ }
}
</script>

<style scoped>
.ipucu-balon {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 12px 14px; margin-bottom: 18px;
  background: rgba(139,92,246,0.1); border: 1px solid rgba(139,92,246,0.3);
  border-radius: 10px; color: var(--text-primary);
}
.ipucu-ikon { color: #a78bfa; font-size: 16px; margin-top: 2px; flex-shrink: 0; }
.ipucu-icerik { flex: 1; }
.ipucu-icerik strong { font-size: 13px; display: block; margin-bottom: 2px; }
.ipucu-icerik p { margin: 0; font-size: 12px; color: var(--text-secondary); line-height: 1.5; }
.ipucu-kapat {
  background: none; border: none; color: var(--text-muted); cursor: pointer;
  font-size: 12px; padding: 2px; flex-shrink: 0;
}
.ipucu-kapat:hover { color: var(--text-primary); }
.ipucu-enter-active, .ipucu-leave-active { transition: all 0.25s ease; }
.ipucu-enter-from, .ipucu-leave-to { opacity: 0; transform: translateY(-8px); }
</style>
