<template>
  <section
    v-if="gorunur"
    class="nakit-serit"
    data-test="kokpit-nakit-serit"
    :aria-labelledby="baslikId"
    :aria-busy="iskelet ? 'true' : undefined"
  >
    <template v-if="iskelet">
      <div
        class="nakit-serit-metin"
        aria-hidden="true"
      >
        <span class="nakit-serit-iskelet nakit-serit-iskelet--kisa" />
        <span class="nakit-serit-iskelet nakit-serit-iskelet--deger" />
        <span class="nakit-serit-iskelet nakit-serit-iskelet--kisa" />
      </div>
      <div
        class="nakit-serit-grafik"
        aria-hidden="true"
      >
        <span class="nakit-serit-iskelet nakit-serit-iskelet--grafik" />
      </div>
    </template>

    <template v-else>
      <div class="nakit-serit-metin">
        <h2
          :id="baslikId"
          class="nakit-serit-baslik"
        >
          {{ t('dashboard.kokpitNakitSeritBaslik') }}
        </h2>
        <strong class="nakit-serit-deger gizli-veri">{{ formatPara(tahminiBitis, paraBirimi) }}</strong>
        <p
          class="nakit-serit-durum"
          :class="durum.sinif"
        >
          <i
            class="pi nakit-serit-durum-ikon"
            :class="durum.ikon"
            aria-hidden="true"
          />
          <span>{{ durumMetni }}</span>
        </p>
        <router-link
          to="/kasa"
          class="nakit-serit-link"
          data-test="kokpit-nakit-serit-detay"
        >
          {{ t('dashboard.kokpitNakitSeritDetay') }}
          <i
            class="pi pi-arrow-right"
            aria-hidden="true"
          />
        </router-link>
      </div>

      <div class="nakit-serit-grafik">
        <BuyukGrafik
          tip="line"
          :etiketler="etiketler"
          :seriler="seriler"
          :yukseklik="grafikYukseklik"
          :y-tik-sayisi="3"
          :para-birimi="paraBirimi"
          sifir-cizgisi
          :aria-label="t('dashboard.kokpitNakitSeritGrafikAria')"
          data-test="kokpit-nakit-serit-grafik"
        />
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import BuyukGrafik from './BuyukGrafik.vue'
import { formatPara } from '../../../utils/format.js'
import { NAKIT_GUVENLI_BANT } from '../../../utils/nakitSabitleri.js'
import { useMobilEkran } from '../../../composables/useMobilEkran.js'

const props = defineProps({
  projeksiyon: { type: Object, default: null },
  paraBirimi: { type: String, default: 'TRY' },
  yukleniyor: { type: Boolean, default: false }
})

const { t, locale } = useI18n()

const baslikId = 'kokpit-nakit-serit-baslik'
const mobil = useMobilEkran('(max-width: 900px)')

const gunluk = computed(() => (
  Array.isArray(props.projeksiyon?.gunluk) ? props.projeksiyon.gunluk : []
))
const veriVar = computed(() => gunluk.value.length > 1)
const iskelet = computed(() => props.yukleniyor && !veriVar.value)
const gorunur = computed(() => veriVar.value || iskelet.value)
const tahminiBitis = computed(() => Number(props.projeksiyon?.tahminiBitisBakiyesi) || 0)
/**
 * Güvenli bant: detay kartındaki `hedefBand` ile aynı semantik — 0 ile
 * `NAKIT_GUVENLI_BANT` arası "bantta". Altına düşen bakiye risk, bandın üstü
 * de güvenli kabul edilir (risk değil). Eşik tek kaynaktan gelir.
 */
const DURUMLAR = {
  altinda: {
    anahtar: 'dashboard.kokpitNakitSeritAltinda',
    sinif: 'nakit-serit-durum--risk',
    ikon: 'pi-exclamation-triangle'
  },
  bantta: {
    anahtar: 'dashboard.kokpitNakitSeritBantta',
    sinif: 'nakit-serit-durum--iyi',
    ikon: 'pi-check-circle'
  },
  ustunde: {
    anahtar: 'dashboard.kokpitNakitSeritUstunde',
    sinif: 'nakit-serit-durum--iyi',
    ikon: 'pi-check-circle'
  }
}
const durum = computed(() => {
  if (tahminiBitis.value < 0) return DURUMLAR.altinda
  if (tahminiBitis.value > NAKIT_GUVENLI_BANT) return DURUMLAR.ustunde
  return DURUMLAR.bantta
})
const durumMetni = computed(() => t(durum.value.anahtar))
const grafikYukseklik = computed(() => (mobil.value ? 60 : 66))

const gunEtiketi = (tarih) => {
  const zaman = new Date(tarih)
  if (Number.isNaN(zaman.getTime())) return ''
  return zaman.toLocaleDateString(locale.value || 'tr-TR', { day: '2-digit', month: '2-digit' })
}

const etiketler = computed(() => gunluk.value.map((gun) => gunEtiketi(gun?.tarih)))
const seriler = computed(() => [{
  ad: t('dashboard.kokpitNakitProjeksiyon'),
  degerler: gunluk.value.map((gun) => Number(gun?.kumulatif) || 0),
  renk: 'var(--info)',
  dolgu: true
}])
</script>

<style scoped>
.nakit-serit {
  display: grid;
  grid-template-columns: 290px minmax(0, 1fr);
  align-items: center;
  gap: 14px;
  min-width: 0;
  min-height: 108px;
  padding: 10px 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--bg-card);
  box-shadow: var(--kokpit-kart-golge);
}
.nakit-serit-metin {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  gap: 2px;
  min-width: 0;
}
.nakit-serit-baslik {
  margin: 0;
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--text-muted);
}
.nakit-serit-deger {
  max-width: 100%;
  font-size: var(--kokpit-deger);
  font-weight: 700;
  line-height: 1.15;
  letter-spacing: -0.02em;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.nakit-serit-durum {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  max-width: 100%;
  margin: 0;
  font-size: 11px;
  line-height: 1.3;
}
.nakit-serit-durum span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.nakit-serit-durum-ikon {
  flex-shrink: 0;
  font-size: 11px;
}
.nakit-serit-durum--risk {
  color: var(--warning-text);
}
.nakit-serit-durum--iyi {
  color: var(--success-text);
}
.nakit-serit-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 2px;
  color: var(--accent);
  font-size: 11px;
  font-weight: 600;
  text-decoration: none;
}
.nakit-serit-link:hover {
  color: var(--accent-hover);
}
.nakit-serit-link:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.nakit-serit-grafik {
  min-width: 0;
}
.nakit-serit-iskelet {
  display: block;
  border-radius: 6px;
  background: var(--bg-muted);
  animation: nakit-serit-nabiz 1.4s ease-in-out infinite;
}
.nakit-serit-iskelet--kisa {
  width: 120px;
  height: 10px;
}
.nakit-serit-iskelet--deger {
  width: 180px;
  max-width: 100%;
  height: 22px;
}
.nakit-serit-iskelet--grafik {
  width: 100%;
  height: 66px;
  border-radius: 8px;
}
@keyframes nakit-serit-nabiz {
  0%,
  100% {
    opacity: 0.45;
  }
  50% {
    opacity: 0.9;
  }
}
/* Mobil: tek kolon, metin + kompakt grafik alt alta. */
@media (max-width: 900px) {
  .nakit-serit {
    grid-template-columns: minmax(0, 1fr);
    gap: 8px;
    min-height: 0;
    padding: 12px 14px;
  }
}
/* Dokunmatik: detay linki 44px hedefe çıkar. */
@media (hover: none) and (pointer: coarse) {
  .nakit-serit-link {
    min-height: 44px;
    align-items: center;
  }
}
</style>
