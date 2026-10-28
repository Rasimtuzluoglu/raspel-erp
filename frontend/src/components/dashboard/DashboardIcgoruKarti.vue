<template>
  <div
    class="icgoru-karti"
    data-test="icgoru-karti"
  >
    <div
      v-if="yukleniyor && !icgoruler.length"
      class="icgoru-iskelet"
      aria-hidden="true"
    >
      <span
        v-for="i in 2"
        :key="`icgoru-iskelet-${i}`"
        class="iskelet"
      />
    </div>

    <p
      v-else-if="!liste.length"
      class="icgoru-bos"
    >
      <i
        class="pi pi-sparkles"
        aria-hidden="true"
      />
      {{ t('dashboard.icgoruBos') }}
    </p>

    <ul
      v-else
      class="icgoru-liste"
    >
      <li
        v-for="(i, index) in liste"
        :key="`${i.seviye}-${index}`"
      >
        <component
          :is="i.route ? 'router-link' : 'div'"
          :to="i.route || undefined"
          class="icgoru-satir"
          :class="`icgoru-satir--${seviyeSinifi(i.seviye)}`"
          :data-test="`icgoru-${index}`"
        >
          <span class="icgoru-ikon">
            <i
              :class="i.ikon || seviyeIkonu(i.seviye)"
              aria-hidden="true"
            />
          </span>
          <span class="icgoru-icerik">
            <span class="icgoru-baslik">{{ i.baslik }}</span>
            <span
              v-if="i.mesaj"
              class="icgoru-mesaj"
            >{{ i.mesaj }}</span>
            <span class="icgoru-seviye">{{ seviyeEtiketi(i.seviye) }}</span>
          </span>
          <i
            v-if="i.route"
            class="pi pi-arrow-right icgoru-git"
            aria-hidden="true"
          />
        </component>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps({
  icgoruler: { type: Array, default: () => [] },
  yukleniyor: { type: Boolean, default: false }
})

const { t } = useI18n()

// En fazla 3 icgoru gosterilir (spec).
const liste = computed(() => (Array.isArray(props.icgoruler) ? props.icgoruler.slice(0, 3) : []))

const seviyeSinifi = (seviye) => {
  if (seviye === 'KRITIK') return 'kritik'
  if (seviye === 'WARN') return 'uyari'
  return 'bilgi'
}

const seviyeIkonu = (seviye) => {
  if (seviye === 'KRITIK') return 'pi pi-exclamation-circle'
  if (seviye === 'WARN') return 'pi pi-exclamation-triangle'
  return 'pi pi-info-circle'
}

const seviyeEtiketi = (seviye) => {
  if (seviye === 'KRITIK') return t('dashboard.icgoruKritik')
  if (seviye === 'WARN') return t('dashboard.icgoruUyari')
  return t('dashboard.icgoruBilgi')
}
</script>

<style scoped>
.icgoru-karti {
  min-height: 120px;
}
.icgoru-liste {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.icgoru-satir {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid var(--border);
  border-left-width: 3px;
  background: var(--bg-card);
  color: inherit;
  text-decoration: none;
  transition: border-color var(--dur-base, 0.2s) var(--ease-standard, ease);
}
a.icgoru-satir:hover {
  border-color: var(--accent-border);
}
a.icgoru-satir:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.icgoru-satir--bilgi {
  border-left-color: var(--accent);
}
.icgoru-satir--uyari {
  border-left-color: var(--warning);
}
.icgoru-satir--kritik {
  border-left-color: var(--danger);
}
.icgoru-ikon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 8px;
  flex-shrink: 0;
  font-size: 13px;
}
.icgoru-satir--bilgi .icgoru-ikon {
  background: var(--accent-soft);
  color: var(--accent);
}
.icgoru-satir--uyari .icgoru-ikon {
  background: var(--warning-soft);
  color: var(--warning);
}
.icgoru-satir--kritik .icgoru-ikon {
  background: var(--danger-soft);
  color: var(--danger);
}
.icgoru-icerik {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1;
}
.icgoru-baslik {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
}
.icgoru-mesaj {
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.45;
}
.icgoru-seviye {
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 0.4px;
  text-transform: uppercase;
  color: var(--text-muted);
}
.icgoru-git {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 4px;
}
.icgoru-bos {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  padding: 18px 12px;
  border-radius: 12px;
  border: 1px dashed var(--border);
  color: var(--text-muted);
  font-size: 13px;
}
.icgoru-iskelet {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.iskelet {
  display: block;
  height: 46px;
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
