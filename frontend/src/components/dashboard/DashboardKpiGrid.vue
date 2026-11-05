<template>
  <div
    class="kpi-grid"
    data-test="kpi-grid"
    data-widget="kpi"
    role="group"
    :aria-label="t('dashboard.widgetKpi')"
  >
    <template v-if="yukleniyor && !kpi">
      <div
        v-for="i in 9"
        :key="`kpi-iskelet-${i}`"
        class="kpi-kart kpi-kart--iskelet"
        aria-hidden="true"
      >
        <span class="kpi-iskelet kpi-iskelet--kisa" />
        <span class="kpi-iskelet kpi-iskelet--buyuk" />
        <span class="kpi-iskelet kpi-iskelet--uzun" />
      </div>
    </template>

    <template v-else>
      <!-- 1. Likidite -->
      <article
        class="kpi-kart kpi-kart--likidite"
        data-test="kpi-likidite"
      >
        <header class="kpi-ust">
          <span class="kpi-ikon"><i
            class="pi pi-wallet"
            aria-hidden="true"
          /></span>
          <span class="kpi-etiket">{{ t('dashboard.kpiLikidite') }}</span>
          <span
            v-if="likiditeSparkVar"
            class="kpi-spark"
            :title="t('dashboard.son7GunNet')"
          >
            <SparkLine
              :veriler="likiditeSparkline"
              renk="#10b981"
              :yukseklik="20"
            />
          </span>
        </header>
        <p
          class="kpi-deger gizli-veri"
          data-test="kpi-deger-likidite"
        >
          {{ para(likidite.deger) }}
        </p>
        <footer class="kpi-alt">
          <span>{{ t('dashboard.kpiKasa') }} <strong>{{ para(likidite.kasa) }}</strong></span>
          <span
            class="kpi-ayirici"
            aria-hidden="true"
          >·</span>
          <span>{{ t('dashboard.kpiBanka') }} <strong>{{ para(likidite.banka) }}</strong></span>
        </footer>
      </article>

      <!-- 2. Alacak -->
      <router-link
        to="/cari-hesaplar"
        class="kpi-kart kpi-kart--alacak kpi-kart--link"
        data-test="kpi-alacak"
      >
        <header class="kpi-ust">
          <span class="kpi-ikon"><i
            class="pi pi-arrow-down-left"
            aria-hidden="true"
          /></span>
          <span class="kpi-etiket">{{ t('dashboard.kpiAlacak') }}</span>
          <i
            class="pi pi-arrow-right kpi-git"
            aria-hidden="true"
          />
        </header>
        <p
          class="kpi-deger gizli-veri"
          data-test="kpi-deger-alacak"
        >
          {{ para(netCari.alacak) }}
        </p>
        <footer class="kpi-alt">
          <span>{{ t('dashboard.kpiAlacakAlt') }}</span>
        </footer>
      </router-link>

      <!-- 3. Borç -->
      <router-link
        to="/cari-hesaplar"
        class="kpi-kart kpi-kart--borc kpi-kart--link"
        data-test="kpi-borc"
      >
        <header class="kpi-ust">
          <span class="kpi-ikon"><i
            class="pi pi-arrow-up-right"
            aria-hidden="true"
          /></span>
          <span class="kpi-etiket">{{ t('dashboard.kpiBorc') }}</span>
          <i
            class="pi pi-arrow-right kpi-git"
            aria-hidden="true"
          />
        </header>
        <p
          class="kpi-deger gizli-veri"
          data-test="kpi-deger-borc"
        >
          {{ para(netCari.borc) }}
        </p>
        <footer class="kpi-alt">
          <span>{{ t('dashboard.kpiBorcAlt') }}</span>
        </footer>
      </router-link>

      <!-- 4. Dönem cirosu -->
      <router-link
        :to="ciroHedefVar ? '/raporlar' : '/yonetici-kokpiti'"
        class="kpi-kart kpi-kart--ciro kpi-kart--link"
        data-test="kpi-ciro"
      >
        <header class="kpi-ust">
          <span class="kpi-ikon"><i
            class="pi pi-chart-bar"
            aria-hidden="true"
          /></span>
          <span class="kpi-etiket">{{ t('dashboard.kpiCiroDonem', { donem: donemEtiketi }) }}</span>
          <i
            class="pi pi-arrow-right kpi-git"
            aria-hidden="true"
          />
        </header>
        <div class="kpi-deger-satir">
          <p
            class="kpi-deger gizli-veri"
            data-test="kpi-deger-ciro"
          >
            {{ para(ciro.deger) }}
          </p>
          <span
            v-if="ciroTrend"
            class="kpi-trend"
            :class="`kpi-trend--${ciroTrend.yon}`"
            :title="t('dashboard.oncekiDonemeGore')"
            :aria-label="trendEtiketi(ciroTrend)"
            role="img"
            data-test="kpi-trend-ciro"
          >
            <span aria-hidden="true">{{ ciroTrend.ok }}</span>
            <span>{{ ciroTrend.metin }}</span>
          </span>
        </div>
        <footer class="kpi-alt kpi-alt--blok">
          <template v-if="ciroHedefVar">
            <div class="kpi-ilerleme">
              <div class="kpi-cubuk">
                <div
                  class="kpi-cubuk-dolu kpi-cubuk-dolu--ciro"
                  :style="{ width: `${ciroYuzde}%` }"
                />
              </div>
              <span class="kpi-yuzde">%{{ ciroYuzde }}</span>
            </div>
          </template>
          <span
            v-else
            class="kpi-hedef-yok kpi-hedef-yok--link"
            data-test="kpi-hedef-baglanti"
          >
            {{ t('dashboard.hedefBelirle') }}
            <i
              class="pi pi-arrow-right"
              aria-hidden="true"
            />
          </span>
        </footer>
      </router-link>

      <!-- 5. Dönem net kârı -->
      <router-link
        :to="karHedefVar ? '/raporlar' : '/yonetici-kokpiti'"
        class="kpi-kart kpi-kart--kar kpi-kart--link"
        data-test="kpi-kar"
      >
        <header class="kpi-ust">
          <span class="kpi-ikon"><i
            class="pi pi-chart-line"
            aria-hidden="true"
          /></span>
          <span class="kpi-etiket">{{ t('dashboard.kpiKarDonem', { donem: donemEtiketi }) }}</span>
          <i
            class="pi pi-arrow-right kpi-git"
            aria-hidden="true"
          />
        </header>
        <div class="kpi-deger-satir">
          <p
            class="kpi-deger gizli-veri"
            data-test="kpi-deger-kar"
            :class="karSinifi"
          >
            {{ karMetni }}
          </p>
          <span
            v-if="karTrend"
            class="kpi-trend"
            :class="`kpi-trend--${karTrend.yon}`"
            :title="t('dashboard.oncekiDonemeGore')"
            :aria-label="trendEtiketi(karTrend)"
            role="img"
            data-test="kpi-trend-kar"
          >
            <span aria-hidden="true">{{ karTrend.ok }}</span>
            <span>{{ karTrend.metin }}</span>
          </span>
        </div>
        <footer class="kpi-alt kpi-alt--blok">
          <div class="kpi-ilerleme">
            <span class="kpi-marj">{{ t('dashboard.kpiMarj') }} %{{ marjMetni }}</span>
            <span
              v-if="karHedefVar"
              class="kpi-yuzde"
            >%{{ karYuzde }}</span>
          </div>
          <template v-if="karHedefVar">
            <div class="kpi-cubuk">
              <div
                class="kpi-cubuk-dolu kpi-cubuk-dolu--kar"
                :style="{ width: `${karYuzde}%` }"
              />
            </div>
          </template>
          <span
            v-else
            class="kpi-hedef-yok kpi-hedef-yok--link"
            data-test="kpi-hedef-baglanti"
          >
            {{ t('dashboard.hedefBelirle') }}
            <i
              class="pi pi-arrow-right"
              aria-hidden="true"
            />
          </span>
        </footer>
      </router-link>

      <!-- 6. Bugün tahsilat -->
      <router-link
        to="/hareketler"
        class="kpi-kart kpi-kart--tahsilat kpi-kart--link"
        data-test="kpi-tahsilat"
      >
        <header class="kpi-ust">
          <span class="kpi-ikon"><i
            class="pi pi-arrow-down-left"
            aria-hidden="true"
          /></span>
          <span class="kpi-etiket">{{ t('dashboard.kpiBugunTahsilat') }}</span>
          <i
            class="pi pi-arrow-right kpi-git"
            aria-hidden="true"
          />
        </header>
        <p
          class="kpi-deger gizli-veri"
          data-test="kpi-deger-tahsilat"
        >
          {{ para(bugun.tahsilat) }}
        </p>
        <footer class="kpi-alt">
          <span>{{ t('dashboard.kpiBugunTahsilatAlt') }}</span>
        </footer>
      </router-link>

      <!-- 7. Bugün ödeme -->
      <router-link
        to="/hareketler"
        class="kpi-kart kpi-kart--odeme kpi-kart--link"
        data-test="kpi-odeme"
      >
        <header class="kpi-ust">
          <span class="kpi-ikon"><i
            class="pi pi-arrow-up-right"
            aria-hidden="true"
          /></span>
          <span class="kpi-etiket">{{ t('dashboard.kpiBugunOdeme') }}</span>
          <i
            class="pi pi-arrow-right kpi-git"
            aria-hidden="true"
          />
        </header>
        <p
          class="kpi-deger gizli-veri"
          data-test="kpi-deger-odeme"
        >
          {{ para(bugun.odeme) }}
        </p>
        <footer class="kpi-alt">
          <span>{{ t('dashboard.kpiBugunOdemeAlt') }}</span>
        </footer>
      </router-link>

      <!-- 8. Bekleyen sipariş -->
      <router-link
        to="/siparisler"
        class="kpi-kart kpi-kart--siparis kpi-kart--link"
        data-test="kpi-bekleyen-siparis"
      >
        <header class="kpi-ust">
          <span class="kpi-ikon"><i
            class="pi pi-shopping-cart"
            aria-hidden="true"
          /></span>
          <span class="kpi-etiket">{{ t('dashboard.kpiBekleyenSiparis') }}</span>
          <i
            class="pi pi-arrow-right kpi-git"
            aria-hidden="true"
          />
        </header>
        <p
          class="kpi-deger gizli-veri"
          data-test="kpi-deger-bekleyen-siparis"
        >
          {{ sayi(bekleyenSiparis) }}
        </p>
        <footer class="kpi-alt">
          <span>{{ t('dashboard.kpiBekleyenSiparisAlt') }}</span>
        </footer>
      </router-link>

      <!-- 9. Stok değeri -->
      <router-link
        to="/stoklar"
        class="kpi-kart kpi-kart--stok kpi-kart--link"
        data-test="kpi-stok-degeri"
      >
        <header class="kpi-ust">
          <span class="kpi-ikon"><i
            class="pi pi-box"
            aria-hidden="true"
          /></span>
          <span class="kpi-etiket">{{ t('dashboard.kpiStokDegeri') }}</span>
          <i
            class="pi pi-arrow-right kpi-git"
            aria-hidden="true"
          />
        </header>
        <p
          class="kpi-deger gizli-veri"
          data-test="kpi-deger-stok-degeri"
        >
          {{ para(stok.deger) }}
        </p>
        <footer class="kpi-alt">
          <span>{{ t('dashboard.kpiStokAlt', { n: sayi(stok.kritikAdet) }) }}</span>
        </footer>
      </router-link>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import SparkLine from '../SparkLine.vue'
import { formatCurrency } from '../../utils/format.js'

const props = defineProps({
  kpi: { type: Object, default: null },
  bugun: { type: Object, default: null },
  stok: { type: Object, default: null },
  bekleyenSiparis: { type: Number, default: 0 },
  yukleniyor: { type: Boolean, default: false },
  /** Secili donem kodu: BU_AY | GECEN_AY | BU_CEYREK | BU_YIL */
  donem: { type: String, default: 'BU_AY' }
})

const { t } = useI18n()

const DONEM_ANAHTARLARI = {
  BU_AY: 'dashboard.donemBuAy',
  GECEN_AY: 'dashboard.donemGecenAy',
  BU_CEYREK: 'dashboard.donemBuCeyrek',
  BU_YIL: 'dashboard.donemBuYil'
}

const donemEtiketi = computed(() => t(DONEM_ANAHTARLARI[props.donem] || DONEM_ANAHTARLARI.BU_AY))

const likidite = computed(() => props.kpi?.likidite || {})
const netCari = computed(() => props.kpi?.netCari || {})
const ciro = computed(() => props.kpi?.ciro || {})
const kar = computed(() => props.kpi?.kar || {})
const bugun = computed(() => props.bugun || {})
const stok = computed(() => props.stok || {})

/** null/eksik veri "—" olarak gosterilir; yapay sifir uretilmez. */
const para = (deger) => {
  if (deger === null || deger === undefined || deger === '') return '—'
  const sayi = Number(deger)
  if (!Number.isFinite(sayi)) return '—'
  return formatCurrency(sayi)
}

const sayi = (deger) => {
  if (deger === null || deger === undefined || deger === '') return '—'
  const n = Number(deger)
  if (!Number.isFinite(n)) return '—'
  return n.toLocaleString('tr-TR')
}

// Likidite kartinda yuzde "trend" GOSTERILMEZ: backend'deki trendYuzde
// gecen aya gore degisim degil, son 7 gunun net nakit akisinin likiditeye
// oranidir. Bunun yerine ayni veriden turetilen 7 gunluk net akis sparkline'i
// gosterilir; yuzde etiketi yaniltici olurdu.
const likiditeSparkline = computed(() => {
  const dizi = props.kpi?.likidite?.sparkline
  if (!Array.isArray(dizi)) return []
  return dizi.map((v) => Number(v) || 0)
})
const likiditeSparkVar = computed(
  () => likiditeSparkline.value.length > 1 && likiditeSparkline.value.some((v) => v !== 0)
)

const yuzdeSinirla = (deger) => Math.min(100, Math.max(0, Math.round(Number(deger) || 0)))

const ciroHedefVar = computed(() => Number(ciro.value.hedef) > 0)

const ciroYuzde = computed(() => {
  const ham = ciro.value.yuzde
  if (ham !== null && ham !== undefined) return yuzdeSinirla(ham)
  const hedef = Number(ciro.value.hedef) || 0
  return hedef > 0 ? yuzdeSinirla((Number(ciro.value.deger) / hedef) * 100) : 0
})

const karHedefVar = computed(() => Number(kar.value.hedef) > 0)

const karYuzde = computed(() => {
  const hedef = Number(kar.value.hedef) || 0
  return hedef > 0 ? yuzdeSinirla((Number(kar.value.deger) / hedef) * 100) : 0
})

const marjMetni = computed(() => {
  const ham = kar.value.marj
  if (ham !== null && ham !== undefined && Number.isFinite(Number(ham))) {
    return Number(ham).toFixed(1)
  }
  const ciroDegeri = Number(ciro.value.deger) || 0
  if (ciroDegeri > 0) return ((Number(kar.value.deger) / ciroDegeri) * 100).toFixed(1)
  return '—'
})

const karMetni = computed(() => {
  const ham = kar.value.deger
  if (ham === null || ham === undefined || ham === '') return '—'
  const sayi = Number(ham)
  if (!Number.isFinite(sayi)) return '—'
  // Metin isareti: pozitifte "+", negatifte Intl'in urettigi "-" korunur.
  return sayi > 0 ? `+${formatCurrency(sayi)}` : formatCurrency(sayi)
})

const karSinifi = computed(() => {
  const sayi = Number(kar.value.deger)
  if (!Number.isFinite(sayi) || sayi === 0) return ''
  return sayi > 0 ? 'kpi-deger--artis' : 'kpi-deger--azalis'
})

/**
 * Onceki esdeger doneme gore degisim rozeti. Backend `degisimYuzde` null
 * dondurebilir (onceki donem 0 ise) veya 0 olabilir; bu durumlarda rozet
 * GOSTERILMEZ. Yon: artis ▲ yesil, azalis ▼ kirmizi.
 */
const trend = (kpiDeger) => {
  const ham = kpiDeger?.degisimYuzde
  if (ham === null || ham === undefined || ham === '') return null
  const sayi = Number(ham)
  if (!Number.isFinite(sayi) || sayi === 0) return null
  return {
    yon: sayi > 0 ? 'artis' : 'azalis',
    ok: sayi > 0 ? '▲' : '▼',
    metin: `%${Math.abs(sayi).toLocaleString('tr-TR', { maximumFractionDigits: 1 })}`
  }
}

const ciroTrend = computed(() => trend(ciro.value))
const karTrend = computed(() => trend(kar.value))

const trendEtiketi = (bilgi) =>
  `${t('dashboard.oncekiDonemeGore')}: ${bilgi.ok} ${bilgi.metin}`
</script>

<style scoped>
/*
 * Finans masasi KPI bandi: 9 kart, 5 + 4. Ikinci satirin son hucresi bos
 * kalabilir; kartlar ayni yukseklikte (~96px) ve esit kolonlardadir.
 */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  min-width: 0;
}
.kpi-kart {
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
  min-height: 100px;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-card);
  box-shadow: var(--elev-1);
  color: inherit;
  text-decoration: none;
  transition:
    border-color var(--dur-base, 0.2s) var(--ease-standard, ease),
    box-shadow var(--dur-base, 0.2s) var(--ease-standard, ease),
    transform var(--dur-base, 0.2s) var(--ease-standard, ease);
}
/* Her KPI'nin ustunde kendi anlam rengiyle ince cizgi. */
.kpi-kart::before {
  content: '';
  position: absolute;
  inset: 0 0 auto 0;
  height: 2px;
  background: var(--kpi-vurgu, var(--accent));
  opacity: 0.8;
  pointer-events: none;
}
.kpi-kart--iskelet::before {
  display: none;
}
.kpi-kart--link:hover {
  border-color: var(--accent-border);
  box-shadow: var(--elev-2);
  transform: translateY(-1px);
}
.kpi-kart--likidite,
.kpi-kart--tahsilat {
  --kpi-vurgu: var(--success);
}
.kpi-kart--alacak {
  --kpi-vurgu: #0ea5e9;
}
.kpi-kart--borc,
.kpi-kart--odeme {
  --kpi-vurgu: var(--danger);
}
.kpi-kart--ciro,
.kpi-kart--siparis {
  --kpi-vurgu: #3b82f6;
}
.kpi-kart--kar {
  --kpi-vurgu: #8b5cf6;
}
.kpi-kart--stok {
  --kpi-vurgu: #f59e0b;
  /* 9 kart 5 kolonda: son kart 2 kolon kaplayarak ikinci satiri tamamlar. */
  grid-column: span 2;
}
@media (max-width: 1199px) {
  .kpi-kart--stok {
    grid-column: span 1;
  }
}
@media (max-width: 699px) {
  .kpi-kart--stok {
    grid-column: span 2;
  }
}
@media (max-width: 419px) {
  .kpi-kart--stok {
    grid-column: span 1;
  }
}
.kpi-kart--link:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.kpi-ust {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.kpi-ikon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 8px;
  font-size: 11.5px;
  flex-shrink: 0;
}
.kpi-kart--likidite .kpi-ikon,
.kpi-kart--alacak .kpi-ikon,
.kpi-kart--tahsilat .kpi-ikon {
  background: var(--success-soft);
  color: var(--success);
}
.kpi-kart--borc .kpi-ikon,
.kpi-kart--odeme .kpi-ikon {
  background: var(--danger-soft);
  color: var(--danger);
}
.kpi-kart--ciro .kpi-ikon,
.kpi-kart--siparis .kpi-ikon {
  background: rgba(59, 130, 246, 0.16);
  color: #60a5fa;
}
.kpi-kart--kar .kpi-ikon {
  background: rgba(139, 92, 246, 0.16);
  color: #a78bfa;
}
.kpi-kart--stok .kpi-ikon {
  background: rgba(14, 165, 233, 0.16);
  color: #0ea5e9;
}
.kpi-etiket {
  flex: 1;
  min-width: 0;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.kpi-git {
  font-size: 10px;
  color: var(--text-muted);
  flex-shrink: 0;
}
.kpi-spark {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  width: 56px;
  height: 20px;
  margin-left: auto;
  flex-shrink: 0;
  opacity: 0.9;
}
.kpi-deger {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -0.02em;
  line-height: 1.15;
  font-variant-numeric: tabular-nums;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.kpi-deger--artis {
  color: var(--success);
}
.kpi-deger--azalis {
  color: var(--danger);
}
/* Deger + trend rozeti ayni satirda; rozet degeri sikistirmaz. */
.kpi-deger-satir {
  display: flex;
  align-items: baseline;
  gap: 6px;
  min-width: 0;
}
.kpi-deger-satir .kpi-deger {
  min-width: 0;
}
.kpi-trend {
  display: inline-flex;
  align-items: baseline;
  gap: 1px;
  flex-shrink: 0;
  padding: 1px 5px;
  border-radius: 6px;
  font-size: 10px;
  font-weight: 800;
  line-height: 1.4;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
.kpi-trend--artis {
  color: var(--success);
  background: var(--success-soft);
}
.kpi-trend--azalis {
  color: var(--danger);
  background: var(--danger-soft);
}
.kpi-alt {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
  margin-top: auto;
  font-size: 10.5px;
  line-height: 1.3;
  color: var(--text-muted);
  min-width: 0;
  overflow: hidden;
}
.kpi-alt strong {
  color: var(--text-secondary);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.kpi-alt--blok {
  flex-direction: column;
  align-items: stretch;
  gap: 5px;
}
.kpi-ayirici {
  color: var(--border);
}
.kpi-ilerleme {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
}
.kpi-cubuk {
  flex: 1;
  height: 5px;
  border-radius: 999px;
  background: var(--bg-muted);
  overflow: hidden;
  min-width: 0;
}
.kpi-cubuk-dolu {
  height: 100%;
  border-radius: 999px;
  transition: width 0.4s ease;
}
.kpi-cubuk-dolu--ciro {
  background: linear-gradient(90deg, #2563eb, #60a5fa);
}
.kpi-cubuk-dolu--kar {
  background: linear-gradient(90deg, #7c3aed, #a78bfa);
}
.kpi-yuzde {
  flex-shrink: 0;
  font-size: 10.5px;
  font-weight: 700;
  color: var(--text-secondary);
  font-variant-numeric: tabular-nums;
}
.kpi-marj {
  font-size: 10.5px;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
}
.kpi-hedef-yok {
  font-size: 10.5px;
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
/* Hedef yoksa kart "Hedef belirle" hedefine (/yonetici-kokpiti) baglanir.
   Aksan rengi kucuk metinde 4.5:1 altinda kaldigi icin ikincil metin
   rengi + alt cizgi kullanilir. */
.kpi-hedef-yok--link {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: var(--text-secondary);
  font-weight: 700;
  text-decoration: underline;
  text-underline-offset: 2px;
}
.kpi-hedef-yok--link i {
  font-size: 8.5px;
}
.kpi-kart--iskelet {
  justify-content: center;
  gap: 9px;
}
.kpi-iskelet {
  display: block;
  border-radius: 7px;
  background: var(--bg-muted);
  animation: iskeletParlama 1.4s ease infinite;
}
.kpi-iskelet--kisa {
  width: 42%;
  height: 11px;
}
.kpi-iskelet--buyuk {
  width: 68%;
  height: 20px;
}
.kpi-iskelet--uzun {
  width: 100%;
  height: 10px;
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
  .kpi-kart,
  .kpi-cubuk-dolu,
  .kpi-iskelet {
    transition: none;
    animation: none;
  }
}
/* 1200–1439: 5 kolon kalir; kart biraz sikisir. */
@media (max-width: 1439px) {
  .kpi-deger {
    font-size: 18px;
  }
  .kpi-spark {
    width: 44px;
  }
}
@media (max-width: 1199px) {
  .kpi-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
@media (max-width: 699px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .kpi-deger {
    font-size: 19px;
  }
}
@media (max-width: 419px) {
  .kpi-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
