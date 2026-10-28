<template>
  <div
    class="gorev-merkezi"
    data-test="gorev-merkezi"
  >
    <div
      v-if="yukleniyor && !gorevler.length"
      class="gorev-iskelet"
      aria-hidden="true"
    >
      <div
        v-for="i in 3"
        :key="`gorev-iskelet-${i}`"
        class="gorev-iskelet-satir"
      >
        <span class="iskelet iskelet--daire" />
        <span class="iskelet iskelet--metin" />
      </div>
    </div>

    <p
      v-else-if="hata"
      class="gorev-durum gorev-durum--hata"
    >
      <i
        class="pi pi-exclamation-circle"
        aria-hidden="true"
      />
      {{ t('dashboard.gorevHata') }}
    </p>

    <p
      v-else-if="!siraliGorevler.length"
      class="gorev-durum gorev-durum--bos"
    >
      <i
        class="pi pi-check-circle"
        aria-hidden="true"
      />
      {{ t('dashboard.gorevBos') }}
    </p>

    <ul
      v-else
      class="gorev-liste"
    >
      <li
        v-for="g in siraliGorevler"
        :key="g.tur"
        class="gorev-satir"
      >
        <component
          :is="gorevRoute(g) ? 'router-link' : 'div'"
          :to="gorevRoute(g) || undefined"
          class="gorev-kart"
          :class="oncelikSinifi(g.oncelik)"
          :data-test="`gorev-${g.tur}`"
        >
          <span
            class="gorev-ikon"
            :style="{ background: `${g.renk || '#3b82f6'}22`, color: g.renk || '#3b82f6' }"
          >
            <i
              :class="g.ikon || 'pi pi-flag'"
              aria-hidden="true"
            />
          </span>
          <span class="gorev-icerik">
            <span class="gorev-baslik">{{ gorevBasligi(g) }}</span>
            <span
              v-if="g.aciklama"
              class="gorev-aciklama"
            >{{ g.aciklama }}</span>
          </span>
          <span class="gorev-sag">
            <strong class="gorev-adet">{{ g.adet || 0 }}</strong>
            <span
              v-if="Number(g.tutar) > 0"
              class="gorev-tutar gizli-veri"
            >{{ formatCurrency(g.tutar) }}</span>
          </span>
          <span
            v-if="vadeMi(g)"
            class="gorev-link"
          >
            {{ t('dashboard.gorevTahsilatLink') }}
            <i
              class="pi pi-arrow-right"
              aria-hidden="true"
            />
          </span>
        </component>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatCurrency } from '../../utils/format.js'

const props = defineProps({
  gorevler: { type: Array, default: () => [] },
  yukleniyor: { type: Boolean, default: false },
  hata: { type: String, default: null }
})

const { t } = useI18n()

const GOREV_TUR_ANAHTARLARI = {
  ONAY_IZIN: 'dashboard.gorevTuruOnayIzin',
  ONAY_MASRAF: 'dashboard.gorevTuruOnayMasraf',
  ONAY_SATINALMA: 'dashboard.gorevTuruOnaySatinalma',
  ONAY_SIPARIS: 'dashboard.gorevTuruOnaySiparis',
  VADESI_GECEN: 'dashboard.gorevTuruVadesiGecen',
  VADESI_YAKLASAN: 'dashboard.gorevTuruVadesiYaklasan',
  KRITIK_STOK: 'dashboard.gorevTuruKritikStok',
  BEKLEYEN_TESLIMAT: 'dashboard.gorevTuruBekleyenTeslimat'
}

const VADE_TURLERI = ['VADESI_GECEN', 'VADESI_YAKLASAN']

const vadeMi = (g) => VADE_TURLERI.includes(g?.tur)

// Vade kayitlari her zaman tahsilat ekranina gider (adet + tutar + link).
const gorevRoute = (g) => (vadeMi(g) ? '/tahsilat' : g.route || null)

const gorevBasligi = (g) => {
  const anahtar = GOREV_TUR_ANAHTARLARI[g.tur]
  if (g.baslik) return g.baslik
  return anahtar ? t(anahtar) : g.tur
}

const oncelikSinifi = (oncelik) => {
  const deger = Number(oncelik) || 0
  if (deger >= 70) return 'gorev-kart--kritik'
  if (deger >= 40) return 'gorev-kart--uyari'
  return 'gorev-kart--bilgi'
}

// Backend oncelik sirali dondurur; istemci de ayni sozlesmeyi garanti eder.
const siraliGorevler = computed(() =>
  [...props.gorevler]
    .filter((g) => g && (Number(g.adet) || 0) > 0)
    .sort((a, b) => (Number(b.oncelik) || 0) - (Number(a.oncelik) || 0))
)
</script>

<style scoped>
/*
 * Gorev satirlari kart degil, panonun icinde duz satirlardir: cerceve/golge
 * yok; satirlar arasinda 1px ayirici, hover'da hafif zemin.
 */
.gorev-merkezi {
  min-width: 0;
}
.gorev-liste {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}
.gorev-satir {
  min-width: 0;
}
.gorev-satir + .gorev-satir {
  border-top: 1px solid var(--border);
}
.gorev-kart {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0 -8px;
  padding: 10px 8px;
  border-radius: 10px;
  color: inherit;
  text-decoration: none;
  min-height: 56px;
  transition: background var(--dur-base, 0.2s) var(--ease-standard, ease);
}
a.gorev-kart:hover {
  background: var(--bg-muted);
}
a.gorev-kart:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: -2px;
}
.gorev-ikon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 10px;
  font-size: 14px;
  flex-shrink: 0;
}
.gorev-icerik {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1;
}
.gorev-baslik {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.gorev-aciklama {
  font-size: 11.5px;
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.gorev-sag {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  flex-shrink: 0;
}
.gorev-adet {
  font-size: 18px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  line-height: 1;
}
.gorev-tutar {
  font-size: 11px;
  color: var(--text-secondary);
  font-variant-numeric: tabular-nums;
}
.gorev-kart--kritik .gorev-adet {
  color: var(--danger);
}
.gorev-kart--uyari .gorev-adet {
  color: var(--warning);
}
.gorev-kart--bilgi .gorev-adet {
  color: var(--accent);
}
.gorev-link {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  flex-shrink: 0;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--accent);
}
.gorev-link i {
  font-size: 9px;
}
.gorev-durum {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  padding: 10px 0;
  color: var(--text-muted);
  font-size: 13px;
}
.gorev-durum--bos i {
  color: var(--success);
}
.gorev-durum--hata {
  color: var(--danger);
}
.gorev-iskelet {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.gorev-iskelet-satir {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
}
.gorev-iskelet-satir + .gorev-iskelet-satir {
  border-top: 1px solid var(--border);
}
.iskelet {
  display: block;
  border-radius: 8px;
  background: var(--bg-muted);
  animation: iskeletParlama 1.4s ease infinite;
}
.iskelet--daire {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  flex-shrink: 0;
}
.iskelet--metin {
  flex: 1;
  height: 13px;
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
  .gorev-kart,
  .iskelet {
    transition: none;
    animation: none;
  }
}
@media (max-width: 600px) {
  .gorev-kart {
    gap: 10px;
    margin: 0;
    padding: 10px 0;
  }
  .gorev-link {
    display: none;
  }
}
</style>
