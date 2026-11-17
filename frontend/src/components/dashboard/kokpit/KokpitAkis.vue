<template>
  <section
    class="kokpit-akis kart"
    data-test="kokpit-akis"
    :aria-label="t('dashboard.sonIslemler')"
  >
    <header class="akis-baslik">
      <h2 class="akis-baslik-metin">
        {{ t('dashboard.sonIslemler') }}
      </h2>
      <span
        v-if="canli"
        class="akis-canli"
        :title="t('dashboard.kokpitCanli')"
        aria-hidden="true"
      />
    </header>

    <div
      v-if="yukleniyor && olaylar.length === 0"
      class="akis-iskeletler"
      aria-hidden="true"
    >
      <span
        v-for="n in 4"
        :key="`akis-iskelet-${n}`"
        class="akis-iskelet"
      />
    </div>

    <p
      v-else-if="hata"
      class="akis-hata"
      role="alert"
      data-test="kokpit-akis-hata"
    >
      <i
        class="pi pi-exclamation-triangle"
        aria-hidden="true"
      />
      <span>{{ t('dashboard.akisHata') }}</span>
      <button
        type="button"
        class="akis-yenile"
        @click="emit('yenile')"
      >
        {{ t('dashboard.yenidenDene') }}
      </button>
    </p>

    <p
      v-else-if="olaylar.length === 0"
      class="akis-bos"
      data-test="kokpit-akis-bos"
    >
      <i
        class="pi pi-inbox"
        aria-hidden="true"
      />
      {{ t('dashboard.akisBos') }}
    </p>

    <ul
      v-else
      class="akis-liste"
    >
      <li
        v-for="(olay, index) in olaylar"
        :key="`${olay.tur || 'olay'}-${olay.tarih || index}-${index}`"
      >
        <button
          type="button"
          class="akis-satir"
          :data-test="`akis-oge-${index}`"
          @click="emit('sec', olay)"
        >
          <span
            class="akis-ikon"
            :style="ikonStili(olay)"
            aria-hidden="true"
          >
            <i :class="olay.ikon || 'pi pi-circle'" />
          </span>
          <span class="akis-metin">
            <strong class="akis-metin-baslik">{{ olay.baslik || olay.tur }}</strong>
            <small
              v-if="olay.aciklama"
              class="akis-metin-aciklama"
            >{{ olay.aciklama }}</small>
          </span>
          <span class="akis-sag">
            <span
              v-if="olay.tutar !== null && olay.tutar !== undefined"
              class="akis-tutar"
            >{{ formatPara(olay.tutar, paraBirimi) }}</span>
            <small class="akis-zaman">{{ zamanMetni(olay.tarih) }}</small>
          </span>
        </button>
      </li>
    </ul>
  </section>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { formatPara } from '../../../utils/format.js'

defineProps({
  olaylar: { type: Array, default: () => [] },
  yukleniyor: { type: Boolean, default: false },
  hata: { type: [String, Boolean, null], default: null },
  paraBirimi: { type: String, default: 'TRY' },
  canli: { type: Boolean, default: false }
})

const emit = defineEmits(['sec', 'yenile'])
const { t } = useI18n()

const ikonStili = (olay) => {
  const renk = olay?.renk || 'var(--accent)'
  return {
    background: `color-mix(in srgb, ${renk} 16%, transparent)`,
    color: renk
  }
}

/** Göreli zaman: az önce / N dk / N sa / N gün; eski kayıtlarda tarih. */
const zamanMetni = (tarih) => {
  if (!tarih) return ''
  const zaman = new Date(tarih).getTime()
  if (!Number.isFinite(zaman)) return ''
  const farkDk = Math.max(0, Math.round((Date.now() - zaman) / 60000))
  if (farkDk < 1) return t('dashboard.akisSimdi')
  if (farkDk < 60) return t('dashboard.akisDakikaOnce', { n: farkDk })
  const farkSa = Math.round(farkDk / 60)
  if (farkSa < 24) return t('dashboard.akisSaatOnce', { n: farkSa })
  const farkGun = Math.round(farkSa / 24)
  if (farkGun < 8) return t('dashboard.akisGunOnce', { n: farkGun })
  return new Date(zaman).toLocaleDateString('tr-TR')
}
</script>

<style scoped>
.kokpit-akis {
  display: flex;
  flex-direction: column;
  min-width: 0;
  gap: 8px;
}
.akis-baslik {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 2px;
}
.akis-baslik-metin {
  margin: 0;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: var(--text-secondary);
  text-transform: uppercase;
}
.akis-canli {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: var(--success, #10b981);
  box-shadow: 0 0 0 3px var(--success-soft, rgba(16, 185, 129, 0.16));
}
.akis-liste {
  display: flex;
  flex-direction: column;
  margin: 0;
  padding: 0;
  list-style: none;
}
.akis-satir {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 8px 6px;
  border: none;
  border-bottom: 1px solid var(--border);
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
  border-radius: 8px;
  transition: background var(--dur-fast) var(--ease-standard);
}
.akis-liste li:last-child .akis-satir {
  border-bottom: none;
}
.akis-satir:hover,
.akis-satir:focus-visible {
  background: var(--bg-secondary);
}
.akis-satir:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 1px;
}
.akis-ikon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  border-radius: 9px;
  font-size: 13px;
}
.akis-metin {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
  flex: 1;
}
.akis-metin-baslik {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.akis-metin-aciklama {
  font-size: 11px;
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.akis-sag {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 1px;
  flex-shrink: 0;
}
.akis-tutar {
  font-size: 12.5px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--text-primary);
}
.akis-zaman {
  font-size: 10.5px;
  color: var(--text-muted);
}
.akis-bos,
.akis-hata {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 6px 2px;
  color: var(--text-muted);
  font-size: 12px;
}
.akis-hata {
  color: var(--danger);
}
.akis-yenile {
  margin-left: auto;
  padding: 2px 8px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: transparent;
  color: var(--text-secondary);
  font-size: 11px;
  cursor: pointer;
}
.akis-iskeletler {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.akis-iskelet {
  height: 40px;
  border-radius: 9px;
  background: var(--bg-secondary);
  animation: akis-nabiz 1.4s ease-in-out infinite;
}
@keyframes akis-nabiz {
  0%,
  100% {
    opacity: 0.55;
  }
  50% {
    opacity: 0.9;
  }
}
@media (prefers-reduced-motion: reduce) {
  .akis-iskelet {
    animation: none;
  }
  .akis-satir {
    transition: none;
  }
}
</style>
