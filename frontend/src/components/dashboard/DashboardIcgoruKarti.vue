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
              :title="i.mesaj"
            >{{ i.mesaj }}</span>
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
</script>

<style scoped>
.icgoru-karti {
  min-width: 0;
}
.icgoru-liste {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}
.icgoru-liste li + li {
  border-top: 1px solid var(--border);
}
.icgoru-satir {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 4px;
  border-radius: 8px;
  color: inherit;
  text-decoration: none;
  transition: background var(--dur-base, 0.2s) var(--ease-standard, ease);
}
a.icgoru-satir:hover {
  background: var(--bg-muted);
}
a.icgoru-satir:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.icgoru-ikon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 9px;
  flex-shrink: 0;
  font-size: 12.5px;
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
  gap: 1px;
  min-width: 0;
  flex: 1;
}
.icgoru-baslik {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.icgoru-mesaj {
  font-size: 11.5px;
  color: var(--text-muted);
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.icgoru-git {
  font-size: 9px;
  color: var(--text-muted);
  flex-shrink: 0;
}
.icgoru-bos {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  padding: 8px 0;
  color: var(--text-muted);
  font-size: 12.5px;
}
.icgoru-bos i {
  color: var(--success);
}
.icgoru-iskelet {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.iskelet {
  display: block;
  height: 40px;
  border-radius: 10px;
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
  .icgoru-satir,
  .iskelet {
    transition: none;
    animation: none;
  }
}
</style>
