<template>
  <section
    class="fin-kart"
    :class="{ 'fin-kart--sakin': azaltHareket, 'fin-kart--hata': !!hata }"
    :data-test="dataTest"
  >
    <header class="fin-kart-ust">
      <div class="fin-kart-baslik-grubu">
        <span
          v-if="ikon"
          class="fin-kart-ikon"
          aria-hidden="true"
        >
          <i :class="ikon" />
        </span>
        <div class="fin-kart-metin">
          <h2 class="fin-kart-baslik">
            {{ baslik }}
          </h2>
          <p
            v-if="altBaslik"
            class="fin-kart-alt-baslik"
          >
            {{ altBaslik }}
          </p>
        </div>
      </div>
      <div class="fin-kart-sag">
        <slot name="ust" />
      </div>
    </header>

    <div
      v-if="hata"
      class="fin-kart-hata"
      role="alert"
    >
      <i
        class="pi pi-exclamation-triangle"
        aria-hidden="true"
      />
      <span>{{ hata }}</span>
    </div>

    <div
      v-else-if="yukleniyor"
      class="fin-kart-iskelet"
      aria-hidden="true"
    >
      <span class="fin-iskelet-satir fin-iskelet-satir--genis" />
      <span class="fin-iskelet-satir" />
      <span class="fin-iskelet-satir fin-iskelet-satir--kisa" />
    </div>

    <div
      v-else
      class="fin-kart-govde"
    >
      <slot />
    </div>

    <footer
      v-if="$slots.alt && !yukleniyor && !hata"
      class="fin-kart-alt"
    >
      <slot name="alt" />
    </footer>
  </section>
</template>

<script setup>
import { useHareketAzaltma } from '../../../composables/useHareketAzaltma.js'

defineProps({
  baslik: { type: String, required: true },
  altBaslik: { type: String, default: '' },
  ikon: { type: String, default: '' },
  yukleniyor: { type: Boolean, default: false },
  hata: { type: String, default: null },
  dataTest: { type: String, default: 'finans-karti' }
})

const { azaltHareket } = useHareketAzaltma()
</script>

<style scoped>
.fin-kart {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
  padding: 13px 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--bg-card);
  box-shadow: var(--elev-1);
  transition:
    border-color var(--dur-base) var(--ease-standard),
    box-shadow var(--dur-base) var(--ease-standard);
}
.fin-kart::before {
  content: '';
  position: absolute;
  inset: 0 0 auto 0;
  height: 2px;
  background: linear-gradient(90deg, var(--accent), transparent 74%);
  opacity: 0.55;
  pointer-events: none;
}
.fin-kart:hover {
  border-color: var(--accent-border);
  box-shadow: var(--elev-2);
}
.fin-kart--hata {
  border-color: var(--danger-border);
}
.fin-kart-ust {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  min-height: 24px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border);
}
.fin-kart-baslik-grubu {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.fin-kart-ikon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  border-radius: 9px;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 12.5px;
}
.fin-kart-metin {
  min-width: 0;
}
.fin-kart-baslik {
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 15px;
  font-weight: 700;
  line-height: 1.2;
  color: var(--text-primary);
}
.fin-kart-alt-baslik {
  margin: 2px 0 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11px;
  line-height: 1.3;
  color: var(--text-muted);
}
.fin-kart-sag {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
  flex-shrink: 0;
}
.fin-kart-govde,
.fin-kart-alt {
  min-width: 0;
}
.fin-kart-hata {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 8px 10px;
  border: 1px solid var(--danger-border);
  border-radius: var(--radius-sm);
  background: var(--danger-soft);
  color: var(--danger);
  font-size: 12px;
}
.fin-kart-iskelet {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 4px 0;
}
.fin-iskelet-satir {
  display: block;
  width: 72%;
  height: 12px;
  border-radius: var(--radius-pill);
  background: var(--bg-muted);
  animation: fin-pulse 1.4s ease-in-out infinite;
}
.fin-iskelet-satir--genis {
  width: 100%;
  height: 56px;
  border-radius: var(--radius-sm);
}
.fin-iskelet-satir--kisa {
  width: 44%;
}
@keyframes fin-pulse {
  0%,
  100% {
    opacity: 0.45;
  }
  50% {
    opacity: 0.9;
  }
}
.fin-kart--sakin .fin-iskelet-satir {
  animation: none;
}
@media (prefers-reduced-motion: reduce) {
  .fin-iskelet-satir {
    animation: none;
  }
}
@media (max-width: 600px) {
  .fin-kart {
    padding: 11px 12px;
  }
}
</style>
