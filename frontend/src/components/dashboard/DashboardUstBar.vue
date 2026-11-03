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
      <!-- Donem secici: KPI'lar secilen doneme gore yeniden yuklenir. -->
      <div class="ust-donem">
        <select
          class="ust-donem-secim"
          :value="donem"
          :aria-label="t('dashboard.donem')"
          :title="t('dashboard.donem')"
          data-test="donem-secici"
          @change="donemSec"
        >
          <option
            v-for="secenek in DONEM_SECENEKLERI"
            :key="secenek.kod"
            :value="secenek.kod"
          >
            {{ t(secenek.anahtar) }}
          </option>
        </select>
        <i
          class="pi pi-angle-down ust-donem-ikon"
          aria-hidden="true"
        />
      </div>

      <!-- Bildirim zili: gorev sayaclarinin toplami + ilgili ekran baglantilari. -->
      <div
        ref="zilKok"
        class="ust-zil-alan"
      >
        <button
          ref="zilButon"
          type="button"
          class="ust-buton ust-buton--ikon ust-zil"
          :class="{ 'ust-buton--secili': zilAcik }"
          :aria-label="zilEtiketi"
          :title="zilEtiketi"
          aria-haspopup="true"
          :aria-expanded="zilAcik ? 'true' : 'false'"
          aria-controls="db-bildirim-menusu"
          data-test="bildirim-zili"
          @click="zilDegistir"
        >
          <i
            class="pi pi-bell"
            aria-hidden="true"
          />
          <span
            v-if="toplamBildirim > 0"
            class="ust-zil-rozet"
            data-test="bildirim-rozet"
            aria-hidden="true"
          >{{ rozetMetni }}</span>
        </button>

        <div
          v-if="zilAcik"
          id="db-bildirim-menusu"
          class="ust-zil-menu"
          role="menu"
          :aria-label="t('dashboard.bildirimler')"
          data-test="bildirim-menusu"
          @keydown.esc.stop.prevent="zilKapat(true)"
        >
          <p class="ust-zil-baslik">
            {{ t('dashboard.bildirimler') }}
          </p>
          <router-link
            v-for="oge in bildirimOgeleri"
            :key="oge.kod"
            :to="oge.to"
            class="ust-zil-oge"
            role="menuitem"
            :data-test="`bildirim-${oge.kod}`"
            @click="zilKapat(false)"
          >
            <i
              :class="oge.ikon"
              aria-hidden="true"
            />
            <span class="ust-zil-oge-metin">{{ t(oge.anahtar) }}</span>
            <strong class="ust-zil-oge-adet">{{ oge.adet }}</strong>
          </router-link>
        </div>
      </div>

      <!-- Vade takvimi -->
      <button
        type="button"
        class="ust-buton ust-buton--ikon"
        :aria-label="t('dashboard.vadeTakvimiAc')"
        :title="t('dashboard.vadeTakvimiAc')"
        data-test="vade-takvimi-buton"
        @click="emit('takvim')"
      >
        <i
          class="pi pi-calendar"
          aria-hidden="true"
        />
      </button>

      <!-- Otomatik yenileme anahtari (60 sn, yalniz sekme gorunurken). -->
      <button
        type="button"
        class="ust-buton ust-buton--ikon ust-oto"
        :class="{ 'ust-buton--secili': otoYenile }"
        role="switch"
        :aria-checked="otoYenile ? 'true' : 'false'"
        :aria-label="otoYenile ? t('dashboard.otoYenilemeKapat') : t('dashboard.otoYenilemeAc')"
        :title="otoYenile ? t('dashboard.otoYenilemeKapat') : t('dashboard.otoYenilemeAc')"
        data-test="oto-yenileme"
        @click="emit('oto-degistir', !otoYenile)"
      >
        <i
          :class="otoYenile ? 'pi pi-sync' : 'pi pi-history'"
          aria-hidden="true"
        />
      </button>

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
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps({
  kullaniciAd: { type: String, default: '' },
  sirketAd: { type: String, default: '' },
  canli: { type: Boolean, default: true },
  sonGuncelleme: { type: String, default: '' },
  yenileniyor: { type: Boolean, default: false },
  duzenModu: { type: Boolean, default: false },
  donem: { type: String, default: 'BU_AY' },
  gorevSayilari: { type: Object, default: () => ({}) },
  otoYenile: { type: Boolean, default: false }
})

const emit = defineEmits(['yenile', 'ara', 'duzenle', 'donem-degistir', 'takvim', 'oto-degistir'])

const { t } = useI18n()

/** Backend donem kodlari; sirasi secicide gorunen siradir. */
const DONEM_SECENEKLERI = [
  { kod: 'BU_AY', anahtar: 'dashboard.donemBuAy' },
  { kod: 'GECEN_AY', anahtar: 'dashboard.donemGecenAy' },
  { kod: 'BU_CEYREK', anahtar: 'dashboard.donemBuCeyrek' },
  { kod: 'BU_YIL', anahtar: 'dashboard.donemBuYil' }
]

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

const donemSec = (olay) => emit('donem-degistir', olay.target.value)

// --- Bildirim zili ---------------------------------------------------------
const sayi = (deger) => {
  const n = Number(deger)
  return Number.isFinite(n) && n > 0 ? Math.floor(n) : 0
}

const bildirimOgeleri = computed(() => [
  {
    kod: 'onayBekleyen',
    anahtar: 'dashboard.bildirimOnayBekleyen',
    to: '/onaylar',
    ikon: 'pi pi-check-square',
    adet: sayi(props.gorevSayilari?.onayBekleyen)
  },
  {
    kod: 'vadesiGecen',
    anahtar: 'dashboard.bildirimVadesiGecen',
    to: '/tahsilat',
    ikon: 'pi pi-clock',
    adet: sayi(props.gorevSayilari?.vadesiGecen)
  },
  {
    kod: 'kritikStok',
    anahtar: 'dashboard.bildirimKritikStok',
    to: '/stoklar?kritik=1',
    ikon: 'pi pi-exclamation-triangle',
    adet: sayi(props.gorevSayilari?.kritikStok)
  }
])

const toplamBildirim = computed(() =>
  bildirimOgeleri.value.reduce((toplam, oge) => toplam + oge.adet, 0)
)

const rozetMetni = computed(() => (toplamBildirim.value > 99 ? '99+' : String(toplamBildirim.value)))

const zilEtiketi = computed(() => `${t('dashboard.bildirimler')} (${toplamBildirim.value})`)

const zilAcik = ref(false)
const zilKok = ref(null)
const zilButon = ref(null)

const zilKapat = (odakla = false) => {
  zilAcik.value = false
  if (odakla) zilButon.value?.focus?.()
}

const zilDegistir = () => {
  zilAcik.value = !zilAcik.value
}

const disariTikla = (olay) => {
  const kok = zilKok.value
  if (!kok || kok.contains?.(olay.target)) return
  zilKapat(false)
}

const tusKacisi = (olay) => {
  if (olay.key === 'Escape') zilKapat(true)
}

// Dinleyiciler yalnizca menu acikken baglanir; kapaninca/kaldirilinca temizlenir.
watch(zilAcik, (acik) => {
  if (typeof document === 'undefined') return
  if (acik) {
    document.addEventListener('click', disariTikla, true)
    document.addEventListener('keydown', tusKacisi)
  } else {
    document.removeEventListener('click', disariTikla, true)
    document.removeEventListener('keydown', tusKacisi)
  }
})

onBeforeUnmount(() => {
  if (typeof document === 'undefined') return
  document.removeEventListener('click', disariTikla, true)
  document.removeEventListener('keydown', tusKacisi)
})
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
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
  min-width: 0;
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
.ust-buton--secili {
  color: var(--accent);
  background: var(--accent-soft);
  border-color: var(--accent-border);
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

/* --- Donem secici -------------------------------------------------------- */
.ust-donem {
  position: relative;
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
}
.ust-donem-secim {
  appearance: none;
  -webkit-appearance: none;
  min-height: 34px;
  padding: 0 26px 0 10px;
  border-radius: 9px;
  border: 1px solid var(--border);
  background: var(--bg-muted);
  color: var(--text-secondary);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition:
    border-color var(--dur-base, 0.2s) var(--ease-standard, ease),
    color var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.ust-donem-secim:hover {
  color: var(--text-primary);
  border-color: var(--accent-border);
}
.ust-donem-secim:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.ust-donem-secim option {
  background: var(--bg-card);
  color: var(--text-primary);
}
.ust-donem-ikon {
  position: absolute;
  right: 9px;
  font-size: 10px;
  color: var(--text-muted);
  pointer-events: none;
}

/* --- Bildirim zili ------------------------------------------------------- */
.ust-zil-alan {
  position: relative;
  display: inline-flex;
  flex-shrink: 0;
}
.ust-zil-rozet {
  position: absolute;
  top: -3px;
  right: -3px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 999px;
  border: 1px solid var(--bg-card);
  background: var(--danger);
  color: #fff;
  font-size: 9.5px;
  font-weight: 800;
  line-height: 14px;
  text-align: center;
  font-variant-numeric: tabular-nums;
}
.ust-zil-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  z-index: 90;
  width: 248px;
  padding: 6px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-card);
  box-shadow: var(--elev-3, var(--elev-2));
}
.ust-zil-baslik {
  margin: 0;
  padding: 4px 8px 6px;
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 0.03em;
  text-transform: uppercase;
  color: var(--text-muted);
}
.ust-zil-oge {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 34px;
  padding: 0 8px;
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 12.5px;
  font-weight: 600;
  text-decoration: none;
}
.ust-zil-oge:hover {
  background: var(--bg-muted);
  color: var(--text-primary);
}
.ust-zil-oge:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: -2px;
}
.ust-zil-oge i {
  font-size: 12px;
  color: var(--accent);
}
.ust-zil-oge-metin {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ust-zil-oge-adet {
  flex-shrink: 0;
  min-width: 22px;
  padding: 1px 6px;
  border-radius: 999px;
  background: var(--bg-muted);
  color: var(--text-primary);
  font-size: 11px;
  font-weight: 800;
  text-align: center;
  font-variant-numeric: tabular-nums;
}

@media (prefers-reduced-motion: reduce) {
  .canli-nokta {
    animation: none;
  }
  .ust-buton,
  .ust-donem-secim {
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
  .ust-donem-secim {
    max-width: 128px;
  }
}
</style>
