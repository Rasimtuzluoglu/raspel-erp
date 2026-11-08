<template>
  <section
    class="nakit-takvim"
    data-test="nakit-takvimi"
    :aria-label="t('dashboard.kokpitNakitTakvimBaslik')"
  >
    <header class="nakit-takvim-ust">
      <div>
        <h2 class="nakit-takvim-baslik">
          {{ t('dashboard.kokpitNakitTakvimBaslik') }}
        </h2>
        <p class="nakit-takvim-alt">
          {{ t('dashboard.kokpitNakitTakvimAlt') }}
        </p>
        <p
          v-if="gunler.length > 0"
          class="nakit-takvim-adet"
        >
          {{ t('dashboard.kokpitNakitTakvimTahsilatAdet', { n: toplamGirisAdet }) }}
          · {{ t('dashboard.kokpitNakitTakvimOdemeAdet', { n: toplamCikisAdet }) }}
        </p>
      </div>

      <div
        v-if="gunler.length > 0"
        class="nakit-takvim-ozet"
        data-test="nakit-takvim-ozet"
      >
        <span class="nakit-ozet-kutu nakit-ozet-kutu--giris">
          <span>{{ t('dashboard.kokpitNakitTakvimGiris') }}</span>
          <strong>{{ formatPara(toplamGiris, paraBirimi) }}</strong>
        </span>
        <span class="nakit-ozet-kutu nakit-ozet-kutu--cikis">
          <span>{{ t('dashboard.kokpitNakitTakvimCikis') }}</span>
          <strong>{{ formatPara(toplamCikis, paraBirimi) }}</strong>
        </span>
        <span class="nakit-ozet-kutu">
          <span>{{ t('dashboard.kokpitNakitTakvimNet') }}</span>
          <strong :class="{ negatif: net < 0 }">{{ formatPara(net, paraBirimi) }}</strong>
        </span>
        <span
          v-if="enYogun"
          class="nakit-ozet-kutu nakit-ozet-kutu--yogun"
        >
          <span>{{ t('dashboard.kokpitNakitTakvimEnYogun') }}</span>
          <strong>{{ gunTarihi(enYogun.tarih) }} · {{ formatPara(gunHacmi(enYogun), paraBirimi) }}</strong>
        </span>
      </div>
    </header>

    <div
      v-if="gunler.length > 0"
      class="nakit-takvim-lejant"
      aria-hidden="true"
    >
      <span><i class="nakit-lejant-nokta nakit-lejant-nokta--giris" /> {{ t('dashboard.kokpitNakitTakvimGiris') }}</span>
      <span><i class="nakit-lejant-nokta nakit-lejant-nokta--cikis" /> {{ t('dashboard.kokpitNakitTakvimCikis') }}</span>
    </div>

    <div
      v-if="gunler.length > 0"
      class="nakit-takvim-grid"
    >
      <div
        v-for="(gun, index) in gunler"
        :key="`nakit-takvim-${gun.tarih || index}`"
        class="nakit-takvim-gun"
        :class="{ 'nakit-takvim-gun--negatif': gun.net < 0 }"
        :title="gunBasligi(gun)"
        :aria-label="gunBasligi(gun)"
        data-test="nakit-takvim-gun"
      >
        <span class="nakit-takvim-gun-adi">{{ gunAdi(gun.tarih) }}</span>
        <span class="nakit-takvim-gun-tarih">{{ gunTarihi(gun.tarih) }}</span>
        <div class="nakit-takvim-barlar">
          <span
            class="nakit-takvim-bar nakit-takvim-bar--giris"
            :style="{ height: `${barYukseklik(gun.giris)}%` }"
          />
          <span
            class="nakit-takvim-bar nakit-takvim-bar--cikis"
            :style="{ height: `${barYukseklik(gun.cikis)}%` }"
          />
        </div>
        <span
          class="nakit-takvim-gun-net"
          :class="{ negatif: gun.net < 0 }"
        >{{ formatPara(gun.net, paraBirimi) }}</span>
      </div>
    </div>

    <div
      v-else-if="yukleniyor"
      class="nakit-takvim-iskelet"
      aria-hidden="true"
    >
      <span
        v-for="i in 14"
        :key="`nakit-takvim-iskelet-${i}`"
        class="nakit-takvim-iskelet-gun"
      />
    </div>

    <p
      v-else
      class="nakit-takvim-bos"
      data-test="nakit-takvim-bos"
    >
      {{ t('dashboard.kokpitNakitTakvimBos') }}
    </p>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatGunAy, formatPara } from '../../../utils/format.js'

const props = defineProps({
  projeksiyon: { type: Object, default: null },
  takvim: { type: Object, default: null },
  yukleniyor: { type: Boolean, default: false },
  paraBirimi: { type: String, default: 'TRY' }
})

const { t, locale } = useI18n()

const gunler = computed(() => {
  const kaynak = Array.isArray(props.takvim?.gunler)
    ? props.takvim.gunler
    : Array.isArray(props.projeksiyon?.gunluk)
      ? props.projeksiyon.gunluk
      : []
  return kaynak.slice(0, 14)
})

const toplamGirisAdet = computed(() =>
  gunler.value.reduce((toplam, gun) => toplam + (Number(gun.girisAdet) || 0), 0)
)
const toplamCikisAdet = computed(() =>
  gunler.value.reduce((toplam, gun) => toplam + (Number(gun.cikisAdet) || 0), 0)
)

const toplamGiris = computed(() =>
  gunler.value.reduce((toplam, gun) => toplam + (Number(gun.giris) || 0), 0)
)
const toplamCikis = computed(() =>
  gunler.value.reduce((toplam, gun) => toplam + (Number(gun.cikis) || 0), 0)
)
const net = computed(() => toplamGiris.value - toplamCikis.value)

const maxAkim = computed(() =>
  Math.max(
    1,
    ...gunler.value.flatMap((gun) => [Number(gun.giris) || 0, Number(gun.cikis) || 0])
  )
)

const gunHacmi = (gun) => (Number(gun?.giris) || 0) + (Number(gun?.cikis) || 0)

const enYogun = computed(() => {
  if (gunler.value.length === 0) return null
  return gunler.value.reduce((en, gun) => (
    gunHacmi(gun) > gunHacmi(en) ? gun : en
  ))
})

const tarihCoz = (deger) => {
  if (!deger) return null
  if (typeof deger === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(deger)) {
    const [yil, ay, gun] = deger.split('-').map(Number)
    return new Date(yil, ay - 1, gun)
  }
  const tarih = new Date(deger)
  return Number.isNaN(tarih.getTime()) ? null : tarih
}

const gunAdi = (deger) => {
  const tarih = tarihCoz(deger)
  if (!tarih) return '—'
  return tarih.toLocaleDateString(locale.value || 'tr-TR', { weekday: 'short' })
}

const gunTarihi = (deger) => formatGunAy(deger) || '—'

const barYukseklik = (deger) => {
  const sayi = Number(deger) || 0
  if (sayi <= 0) return 3
  return Math.max(6, (sayi / maxAkim.value) * 100)
}

const gunBasligi = (gun) =>
  `${gunTarihi(gun.tarih)} — ${t('dashboard.kokpitNakitTakvimGiris')}: ${formatPara(gun.giris, props.paraBirimi)}, ` +
  `${t('dashboard.kokpitNakitTakvimCikis')}: ${formatPara(gun.cikis, props.paraBirimi)}` +
  (gun.girisAdet ? `, ${t('dashboard.kokpitNakitTakvimTahsilatAdet', { n: gun.girisAdet })}` : '') +
  (gun.cikisAdet ? `, ${t('dashboard.kokpitNakitTakvimOdemeAdet', { n: gun.cikisAdet })}` : '')
</script>

<style scoped>
.nakit-takvim {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 9px;
  min-width: 0;
  padding: 13px 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background:
    radial-gradient(700px 150px at 0% 0%, var(--accent-soft), transparent 72%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.018), transparent 38%),
    var(--bg-card);
  box-shadow: var(--elev-1), inset 0 1px 0 rgba(255, 255, 255, 0.025);
}
.nakit-takvim-ust {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 8px;
}
.nakit-takvim-baslik {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
}
.nakit-takvim-alt {
  margin: 2px 0 0;
  font-size: 10.5px;
  color: var(--text-muted);
}
.nakit-takvim-adet {
  margin: 1px 0 0;
  font-size: 9.5px;
  color: var(--text-muted);
}
.nakit-takvim-ozet {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-left: auto;
}
.nakit-ozet-kutu {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 86px;
  padding: 5px 7px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-muted);
}
.nakit-ozet-kutu > span {
  font-size: 9px;
  color: var(--text-muted);
}
.nakit-ozet-kutu strong {
  font-size: 11px;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
.nakit-ozet-kutu--giris strong {
  color: var(--success);
}
.nakit-ozet-kutu--cikis strong {
  color: var(--danger);
}
.nakit-ozet-kutu--yogun strong {
  color: var(--warning);
}
.nakit-takvim-lejant {
  display: flex;
  gap: 12px;
  font-size: 9.5px;
  color: var(--text-muted);
}
.nakit-lejant-nokta {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-right: 3px;
  border-radius: 50%;
}
.nakit-lejant-nokta--giris {
  background: var(--accent);
}
.nakit-lejant-nokta--cikis {
  background: var(--danger);
}
.nakit-takvim-grid {
  display: grid;
  grid-template-columns: repeat(14, minmax(0, 1fr));
  gap: 4px;
}
.nakit-takvim-gun {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  min-width: 0;
  padding: 5px 3px 4px;
  border: 1px solid var(--border);
  border-radius: var(--radius-xs);
  background: var(--bg-muted);
}
.nakit-takvim-gun--negatif {
  border-color: var(--danger-border);
}
.nakit-takvim-gun-adi {
  font-size: 8.5px;
  font-weight: 700;
  color: var(--text-muted);
  text-transform: uppercase;
}
.nakit-takvim-gun-tarih {
  font-size: 9px;
  color: var(--text-secondary);
  font-variant-numeric: tabular-nums;
}
.nakit-takvim-barlar {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 2px;
  width: 100%;
  height: 40px;
  padding: 2px 0;
}
.nakit-takvim-bar {
  display: block;
  width: 38%;
  max-width: 12px;
  min-height: 2px;
  border-radius: 2px 2px 0 0;
  transition: height var(--dur-base) var(--ease-standard);
}
.nakit-takvim-bar--giris {
  background: var(--accent);
}
.nakit-takvim-bar--cikis {
  background: var(--danger);
}
.nakit-takvim-gun-net {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
  font-size: 8.5px;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
}
.nakit-takvim-gun-net.negatif {
  color: var(--danger);
}
.nakit-takvim-iskelet {
  display: grid;
  grid-template-columns: repeat(14, minmax(0, 1fr));
  gap: 4px;
}
.nakit-takvim-iskelet-gun {
  display: block;
  height: 76px;
  border-radius: var(--radius-xs);
  background: var(--bg-muted);
  animation: nakit-takvim-parla 1.4s ease infinite;
}
.nakit-takvim-bos {
  margin: 0;
  padding: 18px 8px;
  text-align: center;
  font-size: 11.5px;
  color: var(--text-muted);
}
@keyframes nakit-takvim-parla {
  0%,
  100% {
    opacity: 0.45;
  }
  50% {
    opacity: 0.9;
  }
}
@media (prefers-reduced-motion: reduce) {
  .nakit-takvim-bar {
    transition: none;
  }
  .nakit-takvim-iskelet-gun {
    animation: none;
  }
}
@media (max-width: 1200px) {
  .nakit-takvim-grid,
  .nakit-takvim-iskelet {
    grid-template-columns: repeat(7, minmax(0, 1fr));
  }
}
@media (max-width: 600px) {
  .nakit-takvim {
    padding: 11px 10px;
  }
  .nakit-takvim-ozet {
    width: 100%;
    margin-left: 0;
  }
  .nakit-ozet-kutu {
    flex: 1 1 calc(50% - 5px);
    min-width: 0;
  }
  .nakit-takvim-gun-net {
    display: none;
  }
  .nakit-takvim-barlar {
    height: 34px;
  }
}
</style>
