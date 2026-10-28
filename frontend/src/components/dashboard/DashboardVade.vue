<template>
  <div
    class="vade-takibi"
    data-test="vade-takibi"
  >
    <div
      v-if="yukleniyor && !vadeler.length"
      class="vade-iskelet"
      aria-hidden="true"
    >
      <span
        v-for="i in 2"
        :key="`vade-iskelet-${i}`"
        class="iskelet"
      />
    </div>

    <template v-else>
      <router-link
        v-for="v in vadeler"
        :key="v.tur"
        to="/tahsilat"
        class="vade-kart"
        :class="`vade-kart--${v.tur === 'VADESI_GECEN' ? 'gecen' : 'yaklasan'}`"
        :data-test="`vade-${v.tur}`"
      >
        <span class="vade-ikon">
          <i
            :class="v.tur === 'VADESI_GECEN' ? 'pi pi-exclamation-triangle' : 'pi pi-clock'"
            aria-hidden="true"
          />
        </span>
        <span class="vade-icerik">
          <span class="vade-baslik">{{ baslik(v.tur) }}</span>
          <span class="vade-alt">{{ t('dashboard.gorevAdet', { n: v.adet || 0 }) }}</span>
        </span>
        <span
          v-if="Number(v.tutar) > 0"
          class="vade-tutar gizli-veri"
        >{{ formatCurrency(v.tutar) }}</span>
      </router-link>

      <p
        v-if="!vadeler.length"
        class="vade-bos"
      >
        <i
          class="pi pi-check-circle"
          aria-hidden="true"
        />
        {{ t('dashboard.vadeBos') }}
      </p>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatCurrency } from '../../utils/format.js'

const props = defineProps({
  gorevler: { type: Array, default: () => [] },
  yukleniyor: { type: Boolean, default: false }
})

const { t } = useI18n()

const VADE_TURLERI = ['VADESI_GECEN', 'VADESI_YAKLASAN']

const vadeler = computed(() =>
  props.gorevler.filter((g) => g && VADE_TURLERI.includes(g.tur) && (Number(g.adet) || 0) > 0)
)

const baslik = (tur) =>
  tur === 'VADESI_GECEN' ? t('dashboard.gorevTuruVadesiGecen') : t('dashboard.gorevTuruVadesiYaklasan')
</script>

<style scoped>
.vade-takibi {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.vade-kart {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: var(--bg-card);
  color: inherit;
  text-decoration: none;
  transition: border-color var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.vade-kart:hover {
  border-color: var(--accent-border);
}
.vade-kart:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.vade-ikon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 9px;
  font-size: 14px;
  flex-shrink: 0;
}
.vade-kart--gecen .vade-ikon {
  background: var(--danger-soft);
  color: var(--danger);
}
.vade-kart--yaklasan .vade-ikon {
  background: var(--warning-soft);
  color: var(--warning);
}
.vade-icerik {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
  flex: 1;
}
.vade-baslik {
  font-size: 12.5px;
  font-weight: 700;
  color: var(--text-primary);
}
.vade-alt {
  font-size: 11.5px;
  color: var(--text-muted);
}
.vade-tutar {
  font-size: 13px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
.vade-kart--gecen .vade-tutar {
  color: var(--danger);
}
.vade-kart--yaklasan .vade-tutar {
  color: var(--warning);
}
.vade-bos {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  padding: 16px 12px;
  border-radius: 12px;
  border: 1px dashed var(--border);
  color: var(--text-muted);
  font-size: 12.5px;
}
.vade-bos i {
  color: var(--success);
}
.vade-iskelet {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.iskelet {
  display: block;
  height: 54px;
  border-radius: 12px;
  background: var(--bg-muted);
  animation: iskeletParlama 1.4s ease infinite;
}
@keyframes iskeletParlama {
  0% {
    opacity: 0.45;
  }
  50% {
    opacity: 0.85;
  }
  100% {
    opacity: 0.45;
  }
}
@media (prefers-reduced-motion: reduce) {
  .iskelet {
    animation: none;
  }
}
</style>
