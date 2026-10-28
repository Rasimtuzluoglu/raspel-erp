<template>
  <header class="dashboard-hero">
    <div class="hero-ust">
      <div class="hero-sol">
        <h1 class="hero-baslik">
          RasPel ERP
        </h1>
        <p class="hero-karsilama">
          {{ karsilamaMetni }},
          <strong>{{ kullaniciAd }}</strong>
          <span
            v-if="sirketAd"
            class="hero-sirket"
          >
            <i
              class="pi pi-building"
              aria-hidden="true"
            /> {{ sirketAd }}
          </span>
        </p>
        <div class="hero-rozetler">
          <span
            class="canli-rozet"
            :class="{ 'canli-rozet--kapali': !canli }"
          >
            <span class="canli-nokta" />
            {{ canli ? t('dashboard.canli') : t('dashboard.cevrimdisiVeri') }}
          </span>
          <span class="hero-saat"><SaatGostergesi /></span>
          <span
            v-if="sonGuncelleme"
            class="hero-guncelleme"
          >{{ t('dashboard.sonGuncelleme', { zaman: sonGuncelleme }) }}</span>
        </div>
      </div>

      <div class="hero-sag">
        <DovizTickerCompact class="hero-ticker" />
        <div class="hero-aksiyonlar">
          <button
            type="button"
            class="hero-buton hero-buton--ara"
            :aria-label="t('dashboard.heroAra')"
            @click="emit('ara')"
          >
            <i
              class="pi pi-search"
              aria-hidden="true"
            />
            <span class="hero-buton-metin">{{ t('dashboard.heroAra') }}</span>
            <kbd aria-hidden="true">Ctrl K</kbd>
          </button>
          <button
            type="button"
            class="hero-buton hero-buton--ikon"
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
            class="hero-buton hero-buton--ikon"
            :class="{ 'hero-buton--aktif': duzenModu }"
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
      </div>
    </div>

    <div
      class="bugun-seridi"
      role="list"
      :aria-label="t('dashboard.bugunSeridi')"
    >
      <div
        v-for="k in bugunKartlari"
        :key="k.key"
        class="bugun-kart"
        :class="`bugun-kart--${k.key}`"
        role="listitem"
        data-test="hero-bugun"
      >
        <span class="bugun-ikon">
          <i
            :class="k.ikon"
            aria-hidden="true"
          />
        </span>
        <span class="bugun-etiket">{{ k.etiket }}</span>
        <strong
          v-if="!yukleniyor"
          class="bugun-deger"
        >{{ k.deger }}</strong>
        <span
          v-else
          class="bugun-skeleton"
          aria-hidden="true"
        />
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import SaatGostergesi from '../SaatGostergesi.vue'
import DovizTickerCompact from '../DovizTickerCompact.vue'
import { formatCurrency } from '../../utils/format.js'

const props = defineProps({
  ozet: { type: Object, default: null },
  kullaniciAd: { type: String, default: '' },
  sirketAd: { type: String, default: '' },
  yukleniyor: { type: Boolean, default: false },
  yenileniyor: { type: Boolean, default: false },
  sonGuncelleme: { type: String, default: '' },
  duzenModu: { type: Boolean, default: false }
})

const emit = defineEmits(['yenile', 'ara', 'duzenle'])

const { t } = useI18n()

const karsilamaMetni = computed(() => {
  const saat = new Date().getHours()
  if (saat < 6) return t('dashboard.iyiGeceler')
  if (saat < 12) return t('dashboard.gunaydin')
  if (saat < 18) return t('dashboard.iyiGunler')
  return t('dashboard.iyiAksamlar')
})

const canli = computed(() => props.ozet?.hero?.canli !== false)

const bugun = computed(() => props.ozet?.bugun || {})

const bugunKartlari = computed(() => [
  {
    key: 'tahsilat',
    etiket: t('dashboard.bugunTahsilat'),
    ikon: 'pi pi-arrow-down-left',
    deger: formatCurrency(bugun.value.tahsilat || 0)
  },
  {
    key: 'odeme',
    etiket: t('dashboard.bugunOdeme'),
    ikon: 'pi pi-arrow-up-right',
    deger: formatCurrency(bugun.value.odeme || 0)
  },
  {
    key: 'siparis',
    etiket: t('dashboard.bugunSiparis'),
    ikon: 'pi pi-shopping-cart',
    deger: Number(bugun.value.siparis || 0).toLocaleString('tr-TR')
  },
  {
    key: 'teslimat',
    etiket: t('dashboard.bugunTeslimat'),
    ikon: 'pi pi-truck',
    deger: Number(bugun.value.teslimat || 0).toLocaleString('tr-TR')
  }
])
</script>

<style scoped>
/*
 * Hero, panonun ust bandidir: kendi cercevesi/golgesi/yaricapi yoktur.
 * Alt cizgi panonun geri kalanindan ayirir; gradyan hafif tutulur.
 */
.dashboard-hero {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 22px 24px;
  border: 0;
  border-bottom: 1px solid var(--border);
  border-radius: 0;
  background:
    radial-gradient(900px 240px at 0% 0%, var(--accent-soft), transparent 62%),
    radial-gradient(700px 240px at 100% 0%, rgba(139, 92, 246, 0.08), transparent 64%);
  overflow: hidden;
}
.hero-ust {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}
.hero-sol {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}
.hero-baslik {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.02em;
}
.hero-karsilama {
  margin: 0;
  font-size: 14px;
  color: var(--text-secondary);
}
.hero-karsilama strong {
  color: var(--text-primary);
}
.hero-sirket {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-left: 8px;
  padding: 2px 10px;
  border-radius: 20px;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 12px;
  font-weight: 600;
}
.hero-rozetler {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 6px;
}
.canli-rozet {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 10px;
  border-radius: 999px;
  background: var(--success-soft);
  color: var(--success);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.3px;
}
.canli-rozet--kapali {
  background: var(--warning-soft);
  color: var(--warning);
}
.canli-nokta {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: currentcolor;
  animation: canliNabiz 1.8s infinite;
}
@keyframes canliNabiz {
  0% {
    box-shadow: 0 0 0 0 var(--success-soft);
  }
  70% {
    box-shadow: 0 0 0 7px rgba(16, 185, 129, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(16, 185, 129, 0);
  }
}
.hero-saat,
.hero-guncelleme {
  font-size: 12px;
  color: var(--text-muted);
}
.hero-sag {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
  min-width: 0;
}
.hero-ticker {
  max-width: 100%;
}
.hero-aksiyonlar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.hero-buton {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 40px;
  padding: 0 12px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--bg-muted);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition:
    background var(--dur-base, 0.2s) var(--ease-standard, ease),
    color var(--dur-base, 0.2s) var(--ease-standard, ease),
    border-color var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.hero-buton:hover:not(:disabled) {
  color: var(--text-primary);
  border-color: var(--accent-border);
  background: var(--accent-soft);
}
.hero-buton:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.hero-buton:disabled {
  opacity: 0.65;
  cursor: progress;
}
.hero-buton--ikon {
  width: 40px;
  padding: 0;
}
.hero-buton--aktif {
  color: var(--accent-contrast);
  background: var(--accent);
  border-color: var(--accent);
}
.hero-buton--ara {
  color: var(--text-primary);
}
.hero-buton kbd {
  padding: 1px 6px;
  border-radius: 6px;
  border: 1px solid var(--border);
  background: var(--bg-card);
  font-size: 10.5px;
  font-weight: 700;
  color: var(--text-muted);
}
.bugun-seridi {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0;
}
.bugun-kart {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 2px 0 2px 14px;
  border-left: 1px solid var(--border);
  background: transparent;
  min-width: 0;
  flex-wrap: wrap;
}
.bugun-kart:first-child {
  padding-left: 0;
  border-left: 0;
}
.bugun-ikon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 10px;
  font-size: 14px;
  flex-shrink: 0;
}
.bugun-kart--tahsilat .bugun-ikon {
  background: var(--success-soft);
  color: var(--success);
}
.bugun-kart--odeme .bugun-ikon {
  background: var(--danger-soft);
  color: var(--danger);
}
.bugun-kart--siparis .bugun-ikon {
  background: var(--accent-soft);
  color: var(--accent);
}
.bugun-kart--teslimat .bugun-ikon {
  background: rgba(139, 92, 246, 0.16);
  color: #a78bfa;
}
.bugun-etiket {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 600;
}
.bugun-deger {
  margin-left: auto;
  font-size: 15px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  color: var(--text-primary);
}
.bugun-skeleton {
  margin-left: auto;
  width: 62px;
  height: 14px;
  border-radius: 6px;
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
  .canli-nokta,
  .bugun-skeleton {
    animation: none;
  }
  .hero-buton {
    transition: none;
  }
}
@media (max-width: 900px) {
  .hero-ust {
    flex-direction: column;
    align-items: stretch;
  }
  .hero-sag {
    justify-content: flex-start;
  }
  .bugun-seridi {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px 0;
  }
  .bugun-kart {
    padding-left: 14px;
    border-left: 1px solid var(--border);
  }
  .bugun-kart:nth-child(odd) {
    padding-left: 0;
    border-left: 0;
  }
}
@media (max-width: 600px) {
  .dashboard-hero {
    padding: 18px 16px;
  }
}
@media (max-width: 560px) {
  .bugun-seridi {
    grid-template-columns: 1fr;
    gap: 10px;
  }
  .bugun-kart,
  .bugun-kart:nth-child(odd) {
    padding-left: 0;
    border-left: 0;
  }
  .hero-buton--ara .hero-buton-metin {
    display: none;
  }
}
</style>
