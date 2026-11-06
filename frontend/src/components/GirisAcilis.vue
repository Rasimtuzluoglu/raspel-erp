<template>
  <div
    class="giris-acilis"
    data-test="giris-acilis"
    role="status"
    aria-live="polite"
    :aria-label="t('giris.acilisDurumu')"
  >
    <div
      class="ga-sahne"
      aria-hidden="true"
    >
      <span class="ga-mesh" />
      <span class="ga-grid" />
    </div>

    <div class="ga-icerik">
      <img
        :src="markaLogo"
        alt=""
        class="ga-logo"
        width="56"
        height="56"
      >
      <span class="ga-marka">RasPel ERP</span>
      <p class="ga-vaat">
        {{ t('giris.acilisVaat') }}
      </p>
      <div
        class="ga-cubuk"
        aria-hidden="true"
      >
        <span
          class="ga-dolgu"
          :style="{ animationDuration: sureMs + 'ms' }"
        />
      </div>
    </div>

    <button
      type="button"
      class="ga-atla"
      data-test="giris-acilis-atla"
      @click="atla"
    >
      <span>{{ t('giris.atla') }}</span>
      <i
        class="pi pi-angle-right"
        aria-hidden="true"
      />
    </button>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps({
  /** Test/özel durumlar için sabit süre (ms). 0 ise tercihe göre hesaplanır. */
  sure: { type: Number, default: 0 },
  /** Bitişte odaklanılacak kullanıcı adı alanı. Boşsa odaklanır. */
  odakSecici: { type: String, default: '[data-test="giris-username"]' }
})

const emit = defineEmits(['bitti'])
const { t } = useI18n()

const markaLogo = '/logo-icon.png'

const hareketAzalt = () => {
  try {
    return window.matchMedia('(prefers-reduced-motion: reduce)').matches
  } catch {
    return false
  }
}

const sureMs = ref(props.sure > 0 ? props.sure : (hareketAzalt() ? 250 : 1200))
let zamanlayici = null
let tamamlandi = false

const odaklan = () => {
  try {
    const el = document.querySelector(props.odakSecici)
    if (!el || typeof el.focus !== 'function') return
    // Kullanıcı adı zaten doluysa odağı çalmayız.
    if ((el.value ?? '') === '') el.focus()
  } catch {
    /* yoksay */
  }
}

const bitir = () => {
  if (tamamlandi) return
  tamamlandi = true
  if (zamanlayici) {
    clearTimeout(zamanlayici)
    zamanlayici = null
  }
  emit('bitti')
  // Parent'ın `inert` katmanını kaldırmasını bekle; aksi halde odak engellenir.
  nextTick(odaklan)
}

const atla = () => bitir()

onMounted(() => {
  zamanlayici = setTimeout(bitir, sureMs.value)
})

onBeforeUnmount(() => {
  if (zamanlayici) clearTimeout(zamanlayici)
  zamanlayici = null
})
</script>

<style scoped>
.giris-acilis {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(180deg, #070b10 0%, #0b1219 100%);
  color: #e8eef5;
  overflow: hidden;
}

.ga-sahne {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.ga-mesh {
  position: absolute;
  inset: -20%;
  background:
    radial-gradient(36% 42% at 24% 22%, rgba(20, 184, 166, 0.16), transparent 70%),
    radial-gradient(40% 44% at 78% 28%, rgba(45, 212, 191, 0.1), transparent 72%),
    radial-gradient(44% 48% at 62% 82%, rgba(13, 148, 136, 0.12), transparent 74%);
}

.ga-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.05) 1px, transparent 1px);
  background-size: 48px 48px;
  -webkit-mask-image: radial-gradient(circle at 50% 42%, #000 0%, transparent 76%);
  mask-image: radial-gradient(circle at 50% 42%, #000 0%, transparent 76%);
}

.ga-icerik {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  width: 100%;
  max-width: 420px;
  padding: 24px;
  text-align: center;
  animation: ga-belir 320ms ease-out both;
}

.ga-logo {
  width: 56px;
  height: 56px;
  object-fit: contain;
  margin-bottom: 4px;
}

.ga-marka {
  font-size: 21px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.ga-vaat {
  margin: 0;
  width: 100%;
  max-width: 340px;
  font-size: 13.5px;
  line-height: 1.55;
  color: #9fb0c0;
}

.ga-cubuk {
  width: 168px;
  height: 3px;
  margin-top: 12px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.18);
  overflow: hidden;
}

.ga-dolgu {
  display: block;
  width: 100%;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #0d9488, #2dd4bf);
  transform-origin: left center;
  animation-name: ga-ilerleme;
  animation-timing-function: linear;
  animation-fill-mode: forwards;
}

.ga-atla {
  position: absolute;
  top: calc(18px + env(safe-area-inset-top));
  right: calc(18px + env(safe-area-inset-right));
  z-index: 2;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 44px;
  min-width: 44px;
  padding: 0 14px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 999px;
  background: rgba(16, 24, 33, 0.72);
  color: #9fb0c0;
  font-family: inherit;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: color 160ms ease-out, border-color 160ms ease-out, background 160ms ease-out;
}

.ga-atla:hover {
  color: #e8eef5;
  border-color: rgba(45, 212, 191, 0.5);
  background: rgba(20, 184, 166, 0.12);
}

.ga-atla:focus-visible {
  outline: 2px solid #14b8a6;
  outline-offset: 2px;
}

@keyframes ga-ilerleme {
  from { transform: scaleX(0); }
  to { transform: scaleX(1); }
}

@keyframes ga-belir {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: none; }
}

@media (prefers-reduced-motion: reduce) {
  .ga-icerik {
    animation: none;
  }
  .ga-dolgu {
    animation: none;
    transform: scaleX(1);
  }
  .ga-atla {
    transition: none;
  }
}
</style>
