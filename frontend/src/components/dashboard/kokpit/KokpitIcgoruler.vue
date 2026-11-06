<template>
  <section
    class="kokpit-icgoruler"
    data-test="kokpit-icgoruler"
    :aria-label="t('dashboard.kokpitIcgoruBaslik')"
  >
    <header class="icgoru-ust">
      <div>
        <h2 class="icgoru-baslik">
          {{ t('dashboard.kokpitIcgoruBaslik') }}
        </h2>
        <p class="icgoru-alt">
          {{ t('dashboard.kokpitIcgoruAlt') }}
        </p>
      </div>
      <span class="icgoru-rozet">AI / KURAL</span>
    </header>

    <div
      v-if="yukleniyor && icgoruler.length === 0"
      class="icgoru-iskelet"
      aria-hidden="true"
    >
      <span
        v-for="i in 3"
        :key="`icgoru-iskelet-${i}`"
        class="icgoru-iskelet-kart"
      />
    </div>

    <p
      v-else-if="icgoruler.length === 0"
      class="icgoru-bos"
      data-test="kokpit-icgoru-bos"
    >
      <i
        class="pi pi-check-circle"
        aria-hidden="true"
      />
      {{ t('dashboard.kokpitIcgoruYok') }}
    </p>

    <div
      v-else
      class="icgoru-grid"
    >
      <router-link
        v-for="(icgoru, index) in icgoruler.slice(0, 3)"
        :key="`${icgoru.baslik}-${index}`"
        :to="hedef(icgoru)"
        class="icgoru-kart"
        :class="seviyeSinifi(icgoru.seviye)"
        :data-test="`kokpit-icgoru-${index}`"
      >
        <span class="icgoru-ikon">
          <i
            :class="icgoru.ikon || seviyeIkonu(icgoru.seviye)"
            aria-hidden="true"
          />
        </span>
        <span class="icgoru-metin">
          <strong class="icgoru-kart-baslik">{{ icgoru.baslik }}</strong>
          <span class="icgoru-mesaj">{{ icgoru.mesaj }}</span>
        </span>
        <span class="icgoru-ok">
          <i
            class="pi pi-arrow-up-right"
            aria-hidden="true"
          />
        </span>
      </router-link>
    </div>
  </section>
</template>

<script setup>
import { useI18n } from 'vue-i18n'

defineProps({
  icgoruler: { type: Array, default: () => [] },
  yukleniyor: { type: Boolean, default: false }
})

const { t } = useI18n()

const seviyeSinifi = (seviye) => {
  if (seviye === 'KRITIK') return 'icgoru-kart--kritik'
  if (seviye === 'WARN') return 'icgoru-kart--uyari'
  return 'icgoru-kart--bilgi'
}

const seviyeIkonu = (seviye) => {
  if (seviye === 'KRITIK') return 'pi pi-exclamation-circle'
  if (seviye === 'WARN') return 'pi pi-exclamation-triangle'
  return 'pi pi-info-circle'
}

const hedef = (icgoru) => {
  const route = icgoru?.route
  if (!route) return '/'
  const [path, queryString] = String(route).split('?')
  const query = {}
  if (queryString) {
    new URLSearchParams(queryString).forEach((value, key) => {
      query[key] = value
    })
  }
  return { path, query }
}
</script>

<style scoped>
.kokpit-icgoruler {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 9px;
  min-width: 0;
  padding: 13px 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background:
    radial-gradient(600px 140px at 100% 0%, var(--accent-soft), transparent 70%),
    var(--bg-card);
  box-shadow: var(--elev-1);
}
.icgoru-ust {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}
.icgoru-baslik {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
}
.icgoru-alt {
  margin: 2px 0 0;
  font-size: 10.5px;
  color: var(--text-muted);
}
.icgoru-rozet {
  margin-left: auto;
  padding: 2px 6px;
  border: 1px solid var(--accent-border);
  border-radius: var(--radius-pill);
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 8.5px;
  font-weight: 700;
  letter-spacing: 0.05em;
  white-space: nowrap;
}
.icgoru-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 7px;
}
.icgoru-kart {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 8px;
  min-width: 0;
  padding: 9px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.025), transparent 50%),
    var(--bg-muted);
  color: inherit;
  text-decoration: none;
  overflow: hidden;
  transition:
    border-color var(--dur-fast) var(--ease-standard),
    background var(--dur-fast) var(--ease-standard);
}
.icgoru-kart::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  width: 2px;
  background: var(--info);
}
.icgoru-kart--kritik::before {
  background: var(--danger);
}
.icgoru-kart--uyari::before {
  background: var(--warning);
}
.icgoru-kart:hover,
.icgoru-kart:focus-visible {
  border-color: var(--accent-border);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.04), transparent 50%),
    var(--bg-secondary);
}
.icgoru-kart:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.icgoru-ikon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background: var(--bg-card);
  color: var(--info);
  font-size: 12px;
}
.icgoru-kart--kritik .icgoru-ikon {
  color: var(--danger);
}
.icgoru-kart--uyari .icgoru-ikon {
  color: var(--warning);
}
.icgoru-metin {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}
.icgoru-kart-baslik {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11.5px;
  font-weight: 700;
  color: var(--text-primary);
}
.icgoru-mesaj {
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  font-size: 10.5px;
  line-height: 1.35;
  color: var(--text-muted);
}
.icgoru-ok {
  flex-shrink: 0;
  margin-left: auto;
  color: var(--text-muted);
  font-size: 8px;
}
.icgoru-bos {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin: 0;
  padding: 14px 8px;
  border: 1px dashed var(--border);
  border-radius: var(--radius-sm);
  font-size: 11.5px;
  color: var(--text-muted);
}
.icgoru-iskelet {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 7px;
}
.icgoru-iskelet-kart {
  display: block;
  height: 58px;
  border-radius: var(--radius-sm);
  background: var(--bg-muted);
  animation: icgoru-parla 1.4s ease infinite;
}
@keyframes icgoru-parla {
  0%,
  100% {
    opacity: 0.45;
  }
  50% {
    opacity: 0.9;
  }
}
@media (prefers-reduced-motion: reduce) {
  .icgoru-iskelet-kart {
    animation: none;
  }
  .icgoru-kart {
    transition: none;
  }
}
@media (max-width: 900px) {
  .icgoru-grid,
  .icgoru-iskelet {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
