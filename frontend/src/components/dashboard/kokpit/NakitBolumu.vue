<template>
  <FinansKarti
    :baslik="t('dashboard.kokpitNakitBaslik')"
    :alt-baslik="t('dashboard.kokpitNakitAltBaslik')"
    ikon="pi pi-wallet"
    :yukleniyor="yukleniyor"
    :hata="hata"
    data-test="nakit-bolumu"
  >
    <template #ust>
      <span
        class="nakit-trend"
        :class="trendSinifi"
      >
        <i
          :class="trendIkonu"
          aria-hidden="true"
        />
        {{ trendMetni }}
      </span>
      <router-link
        to="/kasa"
        class="bolum-hizli-link"
        data-test="nakit-kasa-link"
      >
        {{ t('dashboard.kokpitNakitKasa') }}
        <i
          class="pi pi-arrow-up-right"
          aria-hidden="true"
        />
      </router-link>
    </template>

    <div class="nakit-ozet">
      <div class="nakit-stat nakit-stat--ana">
        <span class="nakit-stat-etiket">{{ t('dashboard.kokpitNakitToplam') }}</span>
        <strong class="nakit-stat-deger gizli-veri">{{ formatPara(toplam, paraBirimi) }}</strong>
        <MikroGrafik
          v-if="sparkline.length > 1"
          :seriler="sparkline"
          :yukseklik="30"
          :aria-label="`${t('dashboard.kokpitNakitToplam')} trend`"
          data-test="nakit-sparkline"
        />
      </div>
      <div class="nakit-stat">
        <span class="nakit-stat-etiket">{{ t('dashboard.kokpitNakitKasa') }}</span>
        <strong class="nakit-stat-deger gizli-veri">{{ formatPara(kasa, paraBirimi) }}</strong>
      </div>
      <div class="nakit-stat">
        <span class="nakit-stat-etiket">{{ t('dashboard.kokpitNakitBanka') }}</span>
        <strong class="nakit-stat-deger gizli-veri">{{ formatPara(banka, paraBirimi) }}</strong>
      </div>
    </div>

    <div class="nakit-dagilim">
      <div
        class="nakit-dagilim-bar"
        role="img"
        :aria-label="dagitimEtiketi"
      >
        <span
          class="nakit-dagilim-kasa"
          :style="{ width: `${kasaOrani}%` }"
        />
        <span
          class="nakit-dagilim-banka"
          :style="{ width: `${100 - kasaOrani}%` }"
        />
      </div>
      <div class="nakit-dagilim-etiketler">
        <span>
          <i
            class="pi pi-wallet"
            aria-hidden="true"
          /> {{ t('dashboard.kokpitNakitKasa') }} · {{ yuzdeMetni(kasaOrani) }}
        </span>
        <span>
          <i
            class="pi pi-building"
            aria-hidden="true"
          /> {{ t('dashboard.kokpitNakitBanka') }} · {{ yuzdeMetni(100 - kasaOrani) }}
        </span>
      </div>
    </div>

    <div
      class="nakit-saglik"
      :class="{ 'nakit-saglik--risk': projeksiyonRiskli }"
      data-test="nakit-saglik"
    >
      <span class="nakit-saglik-rozet">
        <i
          :class="projeksiyonRiskli ? 'pi pi-exclamation-triangle' : 'pi pi-check-circle'"
          aria-hidden="true"
        />
        {{ projeksiyonRiskli ? t('dashboard.kokpitNakitRisk') : t('dashboard.kokpitNakitSaglikli') }}
      </span>
      <span class="nakit-saglik-metin">
        {{ t('dashboard.kokpitNakitNet30') }}:
        <strong :class="{ 'negatif': net30 < 0 }">{{ formatPara(net30, paraBirimi) }}</strong>
      </span>
      <span class="nakit-saglik-metin">
        {{ t('dashboard.kokpitNakitTahminiKapanis') }}:
        <strong :class="{ 'negatif': tahminiBitis < 0 }">{{ formatPara(tahminiBitis, paraBirimi) }}</strong>
      </span>
    </div>

    <div class="nakit-grafik-ust">
      <span class="nakit-grafik-baslik">{{ t('dashboard.kokpitNakitGunlukAkis') }}</span>
      <span
        class="nakit-net"
        :class="{ 'nakit-net--negatif': gunlukNet < 0 }"
      >
        {{ t('dashboard.kokpitNakitNet') }}: {{ formatPara(gunlukNet, paraBirimi) }}
      </span>
    </div>
    <MikroGrafik
      v-if="gunlukSeri.length > 0"
      tip="bar"
      :seriler="[{ degerler: gunlukSeri, renk: 'var(--accent)' }]"
      :yukseklik="52"
      :aria-label="t('dashboard.kokpitNakitGunlukAkis')"
      data-test="nakit-gunluk-grafik"
    />
    <p
      v-else
      class="nakit-bos"
    >
      {{ t('dashboard.kokpitNakitBos') }}
    </p>

    <div
      class="nakit-projeksiyon"
      data-test="nakit-projeksiyon"
    >
      <div class="nakit-projeksiyon-ust">
        <span class="nakit-grafik-baslik">{{ t('dashboard.kokpitNakitProjeksiyon') }}</span>
        <strong class="nakit-projeksiyon-bitis gizli-veri">
          {{ formatPara(projeksiyon.tahminiBitisBakiyesi, paraBirimi) }}
        </strong>
      </div>
      <div class="nakit-projeksiyon-detay">
        <span>{{ t('dashboard.kokpitNakitBeklenenGiris') }} <strong class="gizli-veri">{{ formatPara(projeksiyon.toplamBeklenenGiris, paraBirimi) }}</strong></span>
        <span>{{ t('dashboard.kokpitNakitBeklenenCikis') }} <strong class="gizli-veri">{{ formatPara(projeksiyon.toplamBeklenenCikis, paraBirimi) }}</strong></span>
      </div>
      <MikroGrafik
        v-if="projeksiyonSeri.length > 1"
        :seriler="[{ degerler: projeksiyonSeri, renk: 'var(--info)' }]"
        :yukseklik="34"
        :aria-label="t('dashboard.kokpitNakitProjeksiyon')"
        data-test="nakit-projeksiyon-grafik"
      />
      <MiniTablo
        v-if="projeksiyonGunleri.length > 0"
        :kolonlar="projeksiyonKolonlari"
        :satirlar="projeksiyonGunleri"
        :bos-metin="t('dashboard.kokpitNakitBos')"
        :para-birimi="paraBirimi"
        :secilebilir="false"
        :baslik="t('dashboard.kokpitNakitProjeksiyon')"
        data-test="nakit-projeksiyon-tablo"
      />
    </div>

    <template #alt>
      <button
        type="button"
        class="finans-detay-buton"
        data-test="nakit-detay"
        @click="emit('detay', hedef)"
      >
        {{ t('dashboard.kokpitTumunuGor') }}
        <i
          class="pi pi-arrow-right"
          aria-hidden="true"
        />
      </button>
    </template>
  </FinansKarti>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import FinansKarti from './FinansKarti.vue'
import MikroGrafik from './MikroGrafik.vue'
import MiniTablo from './MiniTablo.vue'
import { formatPara } from '../../../utils/format.js'

const props = defineProps({
  veri: { type: Object, default: null },
  yukleniyor: { type: Boolean, default: false },
  hata: { type: String, default: null },
  paraBirimi: { type: String, default: 'TRY' }
})

const emit = defineEmits(['detay'])
const { t } = useI18n()

const nakit = computed(() => props.veri || {})
const kasa = computed(() => Number(nakit.value.kasa) || 0)
const banka = computed(() => Number(nakit.value.banka) || 0)
const toplam = computed(() => {
  const deger = Number(nakit.value.toplam)
  return Number.isFinite(deger) ? deger : kasa.value + banka.value
})
const trendYuzde = computed(() => {
  const deger = Number(nakit.value.trendYuzde)
  return Number.isFinite(deger) ? deger : null
})
const sparkline = computed(() =>
  Array.isArray(nakit.value.sparkline) ? nakit.value.sparkline.map(Number) : []
)
const gunlukAkis = computed(() =>
  Array.isArray(nakit.value.gunlukAkis) ? nakit.value.gunlukAkis : []
)
const gunlukSeri = computed(() => gunlukAkis.value.map((gun) => Number(gun.net) || 0))
const gunlukNet = computed(() => gunlukSeri.value.reduce((toplamDeger, deger) => toplamDeger + deger, 0))
const projeksiyon = computed(() => nakit.value.projeksiyon || {})
const projeksiyonSeri = computed(() => {
  const gunler = Array.isArray(projeksiyon.value.gunluk) ? projeksiyon.value.gunluk : []
  return gunler.map((gun) => Number(gun.kumulatif) || 0)
})
const tahminiBitis = computed(() => Number(projeksiyon.value.tahminiBitisBakiyesi) || 0)
const net30 = computed(() =>
  (Number(projeksiyon.value.toplamBeklenenGiris) || 0) -
  (Number(projeksiyon.value.toplamBeklenenCikis) || 0)
)
const projeksiyonRiskli = computed(() => tahminiBitis.value < 0)
const projeksiyonGunleri = computed(() => {
  const gunler = Array.isArray(projeksiyon.value.gunluk) ? projeksiyon.value.gunluk : []
  return gunler.slice(0, 4)
})
const projeksiyonKolonlari = computed(() => [
  { anahtar: 'tarih', etiket: t('dashboard.kokpitNakitTarih'), bicim: 'tarih' },
  { anahtar: 'net', etiket: t('dashboard.kokpitNakitNet'), hiza: 'sag', bicim: 'para' },
  {
    anahtar: 'kumulatif',
    etiket: t('dashboard.kokpitNakitKumulatif'),
    hiza: 'sag',
    bicim: 'para',
    mobilGizle: true
  }
])
const hedef = computed(() => nakit.value.hedef || { route: '/kasa', query: {} })

const kasaOrani = computed(() => {
  if (toplam.value <= 0) return 0
  return Math.min(100, Math.max(0, (kasa.value / toplam.value) * 100))
})

const yuzdeMetni = (deger) => `${deger.toLocaleString('tr-TR', { maximumFractionDigits: 1 })}%`

const trendMetni = computed(() => {
  if (trendYuzde.value === null) return '—'
  const isaret = trendYuzde.value > 0 ? '+' : ''
  return `${isaret}${trendYuzde.value.toLocaleString('tr-TR', { maximumFractionDigits: 1 })}%`
})
const trendSinifi = computed(() => {
  if (trendYuzde.value === null || trendYuzde.value === 0) return 'nakit-trend--notr'
  return trendYuzde.value > 0 ? 'nakit-trend--iyi' : 'nakit-trend--kotu'
})
const trendIkonu = computed(() => {
  if (trendYuzde.value === null || trendYuzde.value === 0) return 'pi pi-minus'
  return trendYuzde.value > 0 ? 'pi pi-arrow-up' : 'pi pi-arrow-down'
})
const dagitimEtiketi = computed(() =>
  `${t('dashboard.kokpitNakitKasa')} ${yuzdeMetni(kasaOrani.value)}, ${t('dashboard.kokpitNakitBanka')} ${yuzdeMetni(100 - kasaOrani.value)}`
)
</script>

<style scoped>
.nakit-trend {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}
.nakit-trend i {
  font-size: 8px;
}
.nakit-trend--iyi {
  color: var(--success);
}
.nakit-trend--kotu {
  color: var(--danger);
}
.nakit-trend--notr {
  color: var(--text-muted);
}
.bolum-hizli-link {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 10.5px;
  font-weight: 600;
  color: var(--accent);
  text-decoration: none;
  white-space: nowrap;
}
.bolum-hizli-link:hover,
.bolum-hizli-link:focus-visible {
  color: var(--accent-hover);
}
.bolum-hizli-link:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.bolum-hizli-link i {
  font-size: 8px;
}
.nakit-ozet {
  display: grid;
  grid-template-columns: 1.4fr 1fr 1fr;
  gap: 6px;
}
.nakit-stat {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
  padding: 7px 8px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-muted);
}
.nakit-stat--ana {
  background: var(--accent-soft);
  border-color: var(--accent-border);
}
.nakit-stat-etiket {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 10px;
  font-weight: 600;
  color: var(--text-muted);
}
.nakit-stat-deger {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.nakit-stat--ana .nakit-stat-deger {
  font-size: 19px;
}
.nakit-stat-deger + .mikro-grafik {
  margin-top: 3px;
}
.nakit-dagilim {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.nakit-dagilim-bar {
  display: flex;
  width: 100%;
  height: 7px;
  overflow: hidden;
  border-radius: var(--radius-pill);
  background: var(--bg-muted);
}
.nakit-dagilim-kasa {
  background: var(--accent);
}
.nakit-dagilim-banka {
  background: var(--info);
}
.nakit-dagilim-etiketler {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 10.5px;
  color: var(--text-muted);
}
.nakit-dagilim-etiketler i {
  font-size: 9px;
}
.nakit-saglik {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 5px 10px;
  padding: 7px 8px;
  border: 1px solid var(--success-border);
  border-radius: var(--radius-sm);
  background: var(--success-soft);
}
.nakit-saglik--risk {
  border-color: var(--danger-border);
  background: var(--danger-soft);
}
.nakit-saglik-rozet {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  font-size: 10px;
  font-weight: 700;
  color: var(--success);
}
.nakit-saglik--risk .nakit-saglik-rozet {
  color: var(--danger);
}
.nakit-saglik-metin {
  font-size: 10.5px;
  color: var(--text-muted);
}
.nakit-saglik-metin strong {
  color: var(--text-secondary);
  font-variant-numeric: tabular-nums;
}
.nakit-saglik-metin strong.negatif {
  color: var(--danger);
}
.nakit-grafik-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.nakit-grafik-baslik {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-muted);
}
.nakit-net {
  font-size: 10.5px;
  font-weight: 600;
  color: var(--success);
  font-variant-numeric: tabular-nums;
}
.nakit-net--negatif {
  color: var(--danger);
}
.nakit-bos {
  margin: 0;
  padding: 10px 6px;
  text-align: center;
  font-size: 11.5px;
  color: var(--text-muted);
}
.nakit-projeksiyon {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 8px 9px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-muted);
}
.nakit-projeksiyon-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.nakit-projeksiyon-bitis {
  font-size: 14px;
  font-weight: 700;
  color: var(--info);
  font-variant-numeric: tabular-nums;
}
.nakit-projeksiyon-detay {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 12px;
  font-size: 10.5px;
  color: var(--text-muted);
}
.nakit-projeksiyon-detay strong {
  color: var(--text-secondary);
  font-variant-numeric: tabular-nums;
}
@media (max-width: 600px) {
  .nakit-ozet {
    grid-template-columns: 1fr 1fr;
  }
  .nakit-stat--ana {
    grid-column: 1 / -1;
  }
}
</style>
