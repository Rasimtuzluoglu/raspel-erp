<template>
  <section
    class="saha-ozet"
    data-test="saha-ozet-kartlari"
    :aria-label="t('sahaPortali.bugun')"
  >
    <h2 class="saha-ozet-baslik">
      {{ t('sahaPortali.bugun') }}
    </h2>

    <div
      v-if="yukleniyor && !ozet"
      class="saha-ozet-izgara"
      aria-hidden="true"
    >
      <span
        v-for="n in 3"
        :key="`saha-ozet-iskelet-${n}`"
        class="saha-ozet-iskelet"
      />
    </div>

    <div
      v-else
      class="saha-ozet-izgara"
    >
      <button
        type="button"
        class="saha-ozet-kart"
        data-test="saha-ozet-teslimat"
        @click="emit('sec', 'teslimat')"
      >
        <span class="saha-ozet-ust">
          <i
            class="pi pi-truck"
            aria-hidden="true"
          />
          {{ t('sahaPortali.bugunTeslimat') }}
        </span>
        <strong class="saha-ozet-deger">{{ teslimatAktif }}</strong>
        <span class="saha-ozet-alt">
          {{ t('sahaPortali.bugunTeslimEdilen', { n: teslimEdilen }) }}
          <template v-if="geciken > 0">
            · <span class="saha-ozet-kritik">{{ t('sahaPortali.bugunGeciken', { n: geciken }) }}</span>
          </template>
        </span>
      </button>

      <button
        type="button"
        class="saha-ozet-kart"
        data-test="saha-ozet-ziyaret"
        @click="emit('sec', 'ziyaret')"
      >
        <span class="saha-ozet-ust">
          <i
            class="pi pi-map-marker"
            aria-hidden="true"
          />
          {{ t('sahaPortali.bugunZiyaret') }}
        </span>
        <strong class="saha-ozet-deger">{{ ziyaret }}</strong>
        <span class="saha-ozet-alt">{{ t('sahaPortali.bugun') }}</span>
      </button>

      <button
        type="button"
        class="saha-ozet-kart"
        data-test="saha-ozet-talepler"
        @click="emit('sec', 'talepler')"
      >
        <span class="saha-ozet-ust">
          <i
            class="pi pi-inbox"
            aria-hidden="true"
          />
          {{ t('sahaPortali.bugunTalepler') }}
        </span>
        <strong class="saha-ozet-deger">{{ talepToplam }}</strong>
        <span class="saha-ozet-alt">
          {{ t('sahaPortali.bugunBekleyenIzin', { n: bekleyenIzin }) }}
          · {{ t('sahaPortali.bugunBekleyenMasraf', { n: bekleyenMasraf }) }}
        </span>
      </button>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps({
  ozet: { type: Object, default: null },
  yukleniyor: { type: Boolean, default: false }
})

const emit = defineEmits(['sec'])
const { t } = useI18n()

const teslimat = computed(() => props.ozet?.teslimat || {})
const teslimatAktif = computed(() =>
  Number(teslimat.value.bekleyen || 0) + Number(teslimat.value.yolda || 0))
const teslimEdilen = computed(() => Number(teslimat.value.bugunTeslimEdilen || 0))
const geciken = computed(() => Number(teslimat.value.geciken || 0))
const ziyaret = computed(() => Number(props.ozet?.ziyaret?.bugun || 0))
const bekleyenIzin = computed(() => Number(props.ozet?.talepler?.bekleyenIzin || 0))
const bekleyenMasraf = computed(() => Number(props.ozet?.talepler?.bekleyenMasraf || 0))
const talepToplam = computed(() => bekleyenIzin.value + bekleyenMasraf.value)
</script>

<style scoped>
.saha-ozet {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}
.saha-ozet-baslik {
  margin: 0;
  padding: 0 2px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.02em;
  text-transform: uppercase;
  color: var(--text-secondary);
}
.saha-ozet-izgara {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  min-width: 0;
}
.saha-ozet-kart {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-height: 84px;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg, 14px);
  background: var(--bg-card);
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition:
    border-color var(--dur-fast) var(--ease-standard),
    background var(--dur-fast) var(--ease-standard);
}
.saha-ozet-kart:hover,
.saha-ozet-kart:focus-visible {
  border-color: var(--accent-border);
  background: var(--bg-secondary);
}
.saha-ozet-kart:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.saha-ozet-ust {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--text-secondary);
}
.saha-ozet-ust i {
  color: var(--accent);
  font-size: 12px;
}
.saha-ozet-deger {
  font-size: 1.45rem;
  font-weight: 800;
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
  color: var(--text-primary);
}
.saha-ozet-alt {
  font-size: 10.5px;
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.saha-ozet-kritik {
  color: var(--danger);
  font-weight: 700;
}
.saha-ozet-iskelet {
  height: 84px;
  border-radius: var(--radius-lg, 14px);
  background: var(--bg-secondary);
  animation: saha-ozet-nabiz 1.4s ease-in-out infinite;
}
@keyframes saha-ozet-nabiz {
  0%,
  100% {
    opacity: 0.55;
  }
  50% {
    opacity: 0.9;
  }
}
@media (max-width: 520px) {
  .saha-ozet-izgara {
    gap: 6px;
  }
  .saha-ozet-kart {
    padding: 8px 9px;
    min-height: 78px;
  }
  .saha-ozet-deger {
    font-size: 1.25rem;
  }
}
@media (prefers-reduced-motion: reduce) {
  .saha-ozet-iskelet {
    animation: none;
  }
  .saha-ozet-kart {
    transition: none;
  }
}
</style>
