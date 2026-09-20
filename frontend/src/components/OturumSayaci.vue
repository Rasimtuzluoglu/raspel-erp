<template>
  <div
    v-if="gorunur"
    class="oturum-sayaci"
    :class="{ kritik }"
    role="timer"
    aria-live="polite"
    :title="$t('auth.sessionTimer')"
  >
    <i class="pi pi-clock" />
    <span class="oturum-sure">{{ kalanMetin }}</span>
    <button
      type="button"
      class="oturum-uzat-btn"
      :disabled="yukleniyor"
      :title="$t('auth.extendTooltip')"
      :aria-label="$t('auth.extendTooltip')"
      @click="uzat"
    >
      <i class="pi pi-plus" />
      {{ $t('auth.extend30') }}
    </button>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '../stores/authStore.js'

const authStore = useAuthStore()

const simdi = ref(Date.now())
const yukleniyor = ref(false)
let interval = null
let cikisYapildi = false

const bitis = computed(() => authStore.tokenExpiresAt || null)
const gorunur = computed(() => authStore.isLoggedIn && !!bitis.value)
const kalanMs = computed(() => (bitis.value ? bitis.value - simdi.value : 0))
// Son 10 dakikada sayac kirmiziya doner.
const kritik = computed(() => kalanMs.value > 0 && kalanMs.value <= 10 * 60 * 1000)

const kalanMetin = computed(() => {
  const toplam = Math.max(0, Math.floor(kalanMs.value / 1000))
  const iki = (n) => String(n).padStart(2, '0')
  const s = toplam % 60
  const d = Math.floor(toplam / 60) % 60
  const sa = Math.floor(toplam / 3600)
  return sa > 0 ? `${sa}:${iki(d)}:${iki(s)}` : `${iki(d)}:${iki(s)}`
})

const uzat = async () => {
  if (yukleniyor.value) return
  yukleniyor.value = true
  try {
    await authStore.oturumUzat()
    simdi.value = Date.now()
    cikisYapildi = false
  } catch {
    /* hata global toast ile bildirilir */
  } finally {
    yukleniyor.value = false
  }
}

const tik = () => {
  simdi.value = Date.now()
  if (authStore.isLoggedIn && bitis.value && kalanMs.value <= 0 && !cikisYapildi) {
    cikisYapildi = true
    authStore.cikisYap()
    if (!window.location.pathname.startsWith('/giris')) window.location.href = '/giris'
  }
}

onMounted(() => {
  interval = setInterval(tik, 1000)
})
onUnmounted(() => {
  if (interval) clearInterval(interval)
})
</script>

<style scoped>
.oturum-sayaci {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  border-radius: 8px;
  background: var(--bg-secondary, rgba(148, 163, 184, 0.12));
  color: var(--text-secondary, #64748b);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
.oturum-sayaci .pi-clock {
  font-size: 12px;
}
.oturum-sure {
  font-weight: 600;
  letter-spacing: 0.3px;
}
.oturum-sayaci.kritik {
  color: #dc2626;
  background: rgba(220, 38, 38, 0.12);
}
.oturum-uzat-btn {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  border: 1px solid currentColor;
  background: transparent;
  color: inherit;
  border-radius: 6px;
  padding: 1px 6px;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  line-height: 1.6;
}
.oturum-uzat-btn:hover:not(:disabled) {
  background: rgba(148, 163, 184, 0.18);
}
.oturum-uzat-btn:disabled {
  opacity: 0.5;
  cursor: default;
}
</style>
