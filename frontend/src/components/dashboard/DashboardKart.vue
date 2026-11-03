<template>
  <section
    class="db-kart"
    data-test="dashboard-kart"
  >
    <header class="db-kart-ust">
      <h2 class="db-kart-baslik">
        <span class="db-kart-ikon">
          <i
            :class="ikon"
            aria-hidden="true"
          />
        </span>
        <span class="db-kart-metin">{{ baslik }}</span>
      </h2>
      <div class="db-kart-sag">
        <slot name="sag" />
        <router-link
          v-if="linkTo"
          :to="linkTo"
          class="db-kart-link"
          :aria-label="`${baslik} — ${linkEtiketi}`"
        >
          {{ linkEtiketi }}
          <i
            class="pi pi-arrow-right"
            aria-hidden="true"
          />
        </router-link>
      </div>
    </header>

    <div class="db-kart-govde">
      <slot />
    </div>

    <div
      v-if="$slots.alt"
      class="db-kart-alt"
    >
      <slot name="alt" />
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps({
  baslik: { type: String, required: true },
  ikon: { type: String, default: 'pi pi-chart-bar' },
  linkTo: { type: String, default: null },
  linkMetin: { type: String, default: '' }
})

const { t } = useI18n()

const linkEtiketi = computed(() => props.linkMetin || t('dashboard.detayaGit'))
</script>

<style scoped>
/*
 * Kart, 12 kolonlu izgaranin tek gorsel birimidir: kendi cercevesi, arka
 * plani ve yaricapi vardir. Golge yoktur (veya cok hafiftir); bolumler arasi
 * ek cizgi cizilmez.
 */
.db-kart {
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--bg-card);
  box-shadow: var(--elev-1);
  transition:
    border-color var(--dur-base, 0.2s) var(--ease-standard, ease),
    box-shadow var(--dur-base, 0.2s) var(--ease-standard, ease);
}
/* Ustte ince vurgu cizgisi: kartlara ortak premium kimlik. */
.db-kart::before {
  content: '';
  position: absolute;
  inset: 0 0 auto 0;
  height: 2px;
  background: linear-gradient(90deg, var(--accent), transparent 72%);
  opacity: 0.55;
  pointer-events: none;
}
.db-kart:hover {
  border-color: var(--accent-border);
  box-shadow: var(--elev-2);
}
.db-kart-ust {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 22px;
  min-width: 0;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border);
}
.db-kart-baslik {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  min-width: 0;
  font-size: 13.5px;
  font-weight: 700;
  line-height: 1.2;
  color: var(--text-primary);
}
.db-kart-metin {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.db-kart-ikon {
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
.db-kart-sag {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: auto;
  flex-shrink: 0;
  min-width: 0;
}
.db-kart-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 4px;
  border-radius: 6px;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--accent);
  text-decoration: none;
  white-space: nowrap;
}
.db-kart-link:hover {
  color: var(--accent-hover);
}
.db-kart-link:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.db-kart-link i {
  font-size: 9px;
}
.db-kart-govde {
  min-width: 0;
}
.db-kart-alt {
  min-width: 0;
}
@media (max-width: 600px) {
  .db-kart {
    padding: 12px;
  }
}
</style>
