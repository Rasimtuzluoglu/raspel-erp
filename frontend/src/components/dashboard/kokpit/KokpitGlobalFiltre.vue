<template>
  <header
    class="kokpit-filtre"
    data-test="kokpit-global-filtre"
  >
    <div class="kokpit-filtre-baslik">
      <span class="kokpit-eyebrow">{{ t('dashboard.kokpitAltBaslik') }}</span>
      <h1 class="kokpit-baslik">
        {{ t('dashboard.kokpitBaslik') }}
      </h1>
    </div>

    <div class="kokpit-kontroller">
      <div
        class="kokpit-donem"
        role="group"
        :aria-label="t('dashboard.kokpitDonem')"
      >
        <button
          v-for="secenek in DONEMLER"
          :key="secenek.kod"
          type="button"
          class="kokpit-donem-buton"
          :class="{ 'kokpit-donem-buton--aktif': donem === secenek.kod }"
          :aria-pressed="donem === secenek.kod"
          :data-test="`donem-${secenek.kod}`"
          @click="emit('update:donem', secenek.kod)"
        >
          {{ t(secenek.anahtar) }}
        </button>
      </div>

      <label class="kokpit-para">
        <span class="kokpit-gizli">{{ t('dashboard.kokpitParaBirimi') }}</span>
        <select
          class="kokpit-para-secim"
          :value="paraBirimi"
          :aria-label="t('dashboard.kokpitParaBirimi')"
          data-test="para-birimi"
          @change="emit('update:paraBirimi', $event.target.value)"
        >
          <option
            v-for="birim in PARA_BIRIMLERI"
            :key="birim"
            :value="birim"
          >
            {{ birim }}
          </option>
        </select>
      </label>

      <button
        type="button"
        class="kokpit-canli"
        :class="{ 'kokpit-canli--kapali': !canli }"
        :aria-pressed="canli"
        :title="canli ? t('dashboard.kokpitCanli') : t('dashboard.kokpitCanliKapali')"
        data-test="canli-durum"
        @click="emit('canli-degistir')"
      >
        <span
          class="kokpit-canli-nokta"
          aria-hidden="true"
        />
        {{ canli ? t('dashboard.kokpitCanli') : t('dashboard.kokpitCanliKapali') }}
      </button>

      <button
        type="button"
        class="kokpit-ikon-buton"
        :aria-label="`${t('dashboard.yogunluk')}: ${yogunluk === 'ferah' ? t('dashboard.yogunlukFerah') : t('dashboard.yogunlukKompakt')}`"
        :title="yogunluk === 'ferah' ? t('dashboard.yogunlukKompakt') : t('dashboard.yogunlukFerah')"
        :aria-pressed="yogunluk === 'ferah'"
        data-test="kokpit-yogunluk"
        @click="emit('update:yogunluk', yogunluk === 'ferah' ? 'kompakt' : 'ferah')"
      >
        <i
          class="pi"
          :class="yogunluk === 'ferah' ? 'pi-align-justify' : 'pi-arrows-alt'"
          aria-hidden="true"
        />
      </button>

      <span
        v-if="sonGuncelleme"
        class="kokpit-guncelleme"
        data-test="son-guncelleme"
      >
        {{ t('dashboard.kokpitSonGuncelleme', { zaman: formatDateTime(sonGuncelleme) }) }}
      </span>

      <div class="kokpit-islemler">
        <button
          type="button"
          class="kokpit-ikon-buton"
          :aria-label="t('dashboard.kokpitYenile')"
          :title="t('dashboard.kokpitYenile')"
          data-test="kokpit-yenile"
          @click="emit('yenile')"
        >
          <i
            class="pi pi-refresh"
            :class="{ 'pi-spin': yukleniyor }"
            aria-hidden="true"
          />
        </button>
        <details class="kokpit-menu">
          <summary
            class="kokpit-ikon-buton kokpit-menu-ozet"
            :aria-label="t('dashboard.kokpitMenu')"
            :title="t('dashboard.kokpitMenu')"
            data-test="kokpit-menu"
          >
            <i
              class="pi pi-ellipsis-h"
              aria-hidden="true"
            />
          </summary>
          <div class="kokpit-menu-panel">
            <button
              type="button"
              class="kokpit-menu-oge"
              :aria-label="t('dashboard.kokpitExcel')"
              data-test="kokpit-excel"
              @click="emit('excel')"
            >
              <i
                class="pi pi-file-excel"
                aria-hidden="true"
              />
              {{ t('dashboard.kokpitExcel') }}
            </button>
            <button
              type="button"
              class="kokpit-menu-oge"
              :aria-label="t('dashboard.kokpitPdf')"
              data-test="kokpit-pdf"
              @click="emit('pdf')"
            >
              <i
                class="pi pi-file-pdf"
                aria-hidden="true"
              />
              {{ t('dashboard.kokpitPdf') }}
            </button>
            <button
              type="button"
              class="kokpit-menu-oge"
              :aria-label="t('dashboard.kokpitOzellestir')"
              data-test="kokpit-ozellestir"
              @click="emit('ozellestir')"
            >
              <i
                class="pi pi-sliders-h"
                aria-hidden="true"
              />
              {{ t('dashboard.kokpitOzellestir') }}
            </button>
          </div>
        </details>
      </div>
    </div>
  </header>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { formatDateTime } from '../../../utils/format.js'

defineProps({
  donem: { type: String, default: 'BU_AY' },
  paraBirimi: { type: String, default: 'TRY' },
  canli: { type: Boolean, default: true },
  yogunluk: { type: String, default: 'kompakt' },
  sonGuncelleme: { type: [Date, String], default: null },
  yukleniyor: { type: Boolean, default: false }
})

const emit = defineEmits([
  'update:donem',
  'update:paraBirimi',
  'update:yogunluk',
  'canli-degistir',
  'yenile',
  'excel',
  'pdf',
  'ozellestir'
])

const { t } = useI18n()

const DONEMLER = [
  { kod: 'BU_AY', anahtar: 'dashboard.donemBuAy' },
  { kod: 'GECEN_AY', anahtar: 'dashboard.donemGecenAy' },
  { kod: 'BU_CEYREK', anahtar: 'dashboard.donemBuCeyrek' },
  { kod: 'BU_YIL', anahtar: 'dashboard.donemBuYil' }
]

const PARA_BIRIMLERI = ['TRY', 'USD', 'EUR']
</script>

<style scoped>
.kokpit-filtre {
  position: sticky;
  top: 0;
  z-index: 5;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 16px;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--bg-header);
  backdrop-filter: blur(14px);
  box-shadow: var(--elev-1);
}
.kokpit-filtre-baslik {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}
.kokpit-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--accent);
}
.kokpit-eyebrow::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent);
}
/* `.main-content h1` genel kuralını (24px, alt boşluk) bastırmak için
   yeterli özgüllük: üst bar başlığı 22px kalmalı. */
.kokpit-filtre .kokpit-filtre-baslik .kokpit-baslik {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  line-height: 1.15;
  letter-spacing: -0.025em;
  color: var(--text-primary);
}
.kokpit-kontroller {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-left: auto;
  min-width: 0;
}
.kokpit-donem {
  display: inline-flex;
  padding: 3px;
  border: 1px solid var(--border);
  border-radius: var(--radius-pill);
  background: var(--bg-muted);
}
.kokpit-donem-buton {
  min-height: 30px;
  padding: 0 12px;
  border: 0;
  border-radius: var(--radius-pill);
  background: transparent;
  color: var(--text-muted);
  font-size: 11.5px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  transition:
    background var(--dur-fast) var(--ease-standard),
    color var(--dur-fast) var(--ease-standard);
}
.kokpit-donem-buton:hover {
  color: var(--text-primary);
}
.kokpit-donem-buton:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 1px;
}
.kokpit-donem-buton--aktif {
  background: var(--accent-soft);
  color: var(--accent-text);
  box-shadow: none;
}
.kokpit-para {
  display: inline-flex;
  align-items: center;
}
.kokpit-para-secim {
  height: 28px;
  padding: 0 7px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
}
.kokpit-para-secim:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 1px;
}
.kokpit-canli {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 8px;
  border: 1px solid var(--success-border);
  border-radius: var(--radius-pill);
  background: var(--success-soft);
  color: var(--success-text);
  font-size: 10.5px;
  font-weight: 700;
  cursor: pointer;
}
.kokpit-canli--kapali {
  border-color: var(--border);
  background: var(--bg-muted);
  color: var(--text-muted);
}
.kokpit-canli-nokta {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  animation: kokpit-nabiz 1.8s ease-in-out infinite;
}
.kokpit-canli--kapali .kokpit-canli-nokta {
  animation: none;
}
.kokpit-guncelleme {
  font-size: 11px;
  color: var(--text-muted);
  white-space: nowrap;
}
.kokpit-islemler {
  display: inline-flex;
  gap: 3px;
}
/* Excel/PDF/özelleştirme tek "⋯" menüsünde: üst bar sade kalır. */
.kokpit-menu {
  position: relative;
}
.kokpit-menu-ozet {
  list-style: none;
}
.kokpit-menu-ozet::-webkit-details-marker {
  display: none;
}
.kokpit-menu-panel {
  position: absolute;
  right: 0;
  top: calc(100% + 6px);
  z-index: var(--z-menu, 1250);
  display: flex;
  flex-direction: column;
  min-width: 190px;
  padding: 6px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg-card);
  box-shadow: var(--elev-2, 0 12px 32px rgba(0, 0, 0, 0.35));
}
.kokpit-menu-oge {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-secondary);
  font-size: 12px;
  text-align: left;
  cursor: pointer;
}
.kokpit-menu-oge:hover,
.kokpit-menu-oge:focus-visible {
  background: var(--bg-secondary);
  color: var(--accent);
}
.kokpit-menu-oge:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: -2px;
}
.kokpit-ikon-buton {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-secondary);
  font-size: 12px;
  cursor: pointer;
}
.kokpit-ikon-buton:hover,
.kokpit-ikon-buton:focus-visible {
  border-color: var(--accent-border);
  color: var(--accent);
}
.kokpit-ikon-buton:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 1px;
}
.kokpit-gizli {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
@keyframes kokpit-nabiz {
  0%,
  100% {
    opacity: 0.5;
  }
  50% {
    opacity: 1;
  }
}
@media (prefers-reduced-motion: reduce) {
  .kokpit-canli-nokta {
    animation: none;
  }
}
@media (max-width: 1200px) {
  .kokpit-guncelleme {
    display: none;
  }
}
/* Kabuk 900 px'te sabit mobil üst bara geçiyor; üst bar da aynı eşikte
   statikleşmeli. 761–900 bandında sticky başlık sabit barın altına kaçıyordu. */
@media (max-width: 900px) {
  .kokpit-filtre {
    position: static;
  }
  .kokpit-kontroller {
    width: 100%;
    margin-left: 0;
  }
  .kokpit-donem {
    width: 100%;
    justify-content: space-between;
    overflow-x: auto;
  }
  .kokpit-donem-buton {
    flex: 1;
    text-align: center;
  }
  .kokpit-ikon-buton {
    width: 44px;
    height: 44px;
  }
}
/* Dokunmatik isaretci: ust bar kontrolleri 44 px hedefe cikar.
   Masaustu/ince isaretci boyutlari degismez. */
@media (pointer: coarse) {
  .kokpit-donem-buton {
    min-height: 44px;
  }
  .kokpit-para-secim {
    height: 44px;
  }
  .kokpit-ikon-buton {
    width: 44px;
    height: 44px;
  }
}
</style>
