<template>
  <header class="ust-bar">
    <div class="ust-sol">
      <p class="ust-selam">
        {{ selamlama }},
        <strong>{{ kullaniciAd || '—' }}</strong>
      </p>
      <p class="ust-alt">
        <span
          v-if="sirketAd"
          class="ust-sirket"
        >{{ sirketAd }} · </span>
        <span>{{ bugunMetni }}</span>
        <span
          v-if="sonGuncelleme"
          class="ust-guncelleme"
        > · {{ t('dashboard.sonGuncellemeKisa', { zaman: sonGuncelleme }) }}</span>
        <span
          class="ust-canli"
          :class="{ 'ust-canli--kapali': !canli }"
        >
          <span class="canli-nokta" />
          {{ canli ? t('dashboard.canli') : t('dashboard.cevrimdisiVeri') }}
        </span>
      </p>
    </div>

    <div class="ust-sag">
      <button
        type="button"
        class="ust-buton ust-buton--ara"
        :aria-label="t('dashboard.heroAra')"
        @click="emit('ara')"
      >
        <i
          class="pi pi-search"
          aria-hidden="true"
        />
        <span class="ust-ara-metin">{{ t('dashboard.heroAra') }}</span>
        <kbd aria-hidden="true">Ctrl K</kbd>
      </button>
      <button
        type="button"
        class="ust-buton ust-buton--ikon"
        :aria-label="t('dashboard.yenile')"
        :title="t('dashboard.yenile')"
        :disabled="yenileniyor"
        @click="emit('yenile')"
      >
        <i
          class="pi"
          :class="yenileniyor ? 'pi-spin pi-spinner' : 'pi-refresh'"
          aria-hidden="true"
        />
      </button>
      <button
        type="button"
        class="ust-buton ust-buton--ikon"
        :class="{ 'ust-buton--aktif': duzenModu }"
        :aria-label="duzenModu ? t('dashboard.duzenBitir') : t('dashboard.duzenle')"
        :title="duzenModu ? t('dashboard.duzenBitir') : t('dashboard.duzenle')"
        :aria-pressed="duzenModu"
        @click="emit('duzenle')"
      >
        <i
          class="pi pi-sliders-h"
          aria-hidden="true"
        />
      </button>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

defineProps({
  kullaniciAd: { type: String, default: '' },
  sirketAd: { type: String, default: '' },
  canli: { type: Boolean, default: true },
  sonGuncelleme: { type: String, default: '' },
  yenileniyor: { type: Boolean, default: false },
  duzenModu: { type: Boolean, default: false }
})

const emit = defineEmits(['yenile', 'ara', 'duzenle'])

const { t } = useI18n()

const selamlama = computed(() => {
  const saat = new Date().getHours()
  if (saat < 6) return t('dashboard.iyiGeceler')
  if (saat < 12) return t('dashboard.gunaydin')
  if (saat < 18) return t('dashboard.iyiGunler')
  return t('dashboard.iyiAksamlar')
})

// "28 Ekim 2026 Çarşamba"
const bugunMetni = computed(() =>
  new Intl.DateTimeFormat('tr-TR', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    weekday: 'long'
  }).format(new Date())
)
</script>

<style scoped>
/*
 * Ust bar sade bir baslik alanidir: kart degil, cerceve/arka plan/golge yok.
 * Yukseklik ~64px; sol tarafta selamlama + tek satir baglam, sagda eylemler.
 */
.ust-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 64px;
  padding: 6px 0;
  min-width: 0;
}
.ust-sol {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.ust-selam {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  line-height: 1.3;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ust-selam strong {
  font-weight: 700;
}
.ust-alt {
  display: flex;
  align-items: center;
  gap: 0;
  flex-wrap: wrap;
  margin: 0;
  font-size: 12px;
  line-height: 1.4;
  color: var(--text-muted);
  min-width: 0;
}
.ust-canli {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-left: 10px;
  font-size: 11px;
  font-weight: 600;
  color: var(--success);
}
.ust-canli--kapali {
  color: var(--warning);
}
.canli-nokta {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentcolor;
  animation: canliNabiz 1.8s infinite;
}
@keyframes canliNabiz {
  0% {
    box-shadow: 0 0 0 0 var(--success-soft);
  }
  70% {
    box-shadow: 0 0 0 6px rgba(16, 185, 129, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(16, 185, 129, 0);
  }
}
.ust-sag {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.ust-buton {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 34px;
  padding: 0 10px;
  border-radius: 9px;
  border: 1px solid transparent;
  background: transparent;
  color: var(--text-secondary);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition:
    background var(--dur-base, 0.2s) var(--ease-standard, ease),
    color var(--dur-base, 0.2s) var(--ease-standard, ease),
    border-color var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.ust-buton:hover:not(:disabled) {
  color: var(--text-primary);
  border-color: var(--border);
  background: var(--bg-muted);
}
.ust-buton:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.ust-buton:disabled {
  opacity: 0.65;
  cursor: progress;
}
.ust-buton--ikon {
  width: 34px;
  padding: 0;
}
.ust-buton--aktif {
  color: var(--accent-contrast);
  background: var(--accent);
  border-color: var(--accent);
}
.ust-buton--ara {
  border-color: var(--border);
}
.ust-buton kbd {
  padding: 1px 6px;
  border-radius: 6px;
  border: 1px solid var(--border);
  background: var(--bg-muted);
  font-size: 10px;
  font-weight: 700;
  color: var(--text-muted);
}
@media (prefers-reduced-motion: reduce) {
  .canli-nokta {
    animation: none;
  }
  .ust-buton {
    transition: none;
  }
}
@media (max-width: 900px) {
  .ust-bar {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
    padding-bottom: 10px;
  }
  .ust-sag {
    justify-content: flex-start;
  }
}
@media (max-width: 560px) {
  .ust-selam {
    font-size: 16px;
  }
  .ust-ara-metin {
    display: none;
  }
}
</style>
