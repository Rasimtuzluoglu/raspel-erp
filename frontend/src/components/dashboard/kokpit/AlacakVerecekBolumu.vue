<template>
  <FinansKarti
    :baslik="t('dashboard.kokpitAlacakVerecekBaslik')"
    :alt-baslik="t('dashboard.kokpitAlacakVerecekAltBaslik')"
    ikon="pi pi-users"
    :yukleniyor="yukleniyor"
    :hata="hata"
    data-test="alacak-verecek-bolumu"
  >
    <template #ust>
      <span
        class="alacak-net"
        :class="{ 'alacak-net--negatif': net < 0 }"
      >
        {{ t('dashboard.kokpitAlacakVerecekNet') }}:
        <strong class="gizli-veri">{{ formatPara(net, paraBirimi) }}</strong>
      </span>
    </template>

    <div class="alacak-ozet">
      <div class="alacak-stat alacak-stat--alacak">
        <span class="alacak-stat-etiket">{{ t('dashboard.kokpitAlacakVerecekAlacak') }}</span>
        <strong class="alacak-stat-deger gizli-veri">{{ formatPara(alacakToplam, paraBirimi) }}</strong>
      </div>
      <div class="alacak-stat alacak-stat--verecek">
        <span class="alacak-stat-etiket">{{ t('dashboard.kokpitAlacakVerecekVerecek') }}</span>
        <strong class="alacak-stat-deger gizli-veri">{{ formatPara(verecekToplam, paraBirimi) }}</strong>
      </div>
      <div class="alacak-stat">
        <span class="alacak-stat-etiket">{{ t('dashboard.kokpitAlacakVerecekTahsilatOrani') }}</span>
        <strong class="alacak-stat-deger">{{ tahsilatOraniMetni }}</strong>
      </div>
    </div>

    <div
      class="alacak-bolum"
      data-test="alacak-yaslanma"
    >
      <span class="alacak-bolum-baslik">{{ t('dashboard.kokpitAlacakVerecekYaslanma') }}</span>
      <YaslanmaKovalari
        :kovalar="yaslanma"
        :para-birimi="paraBirimi"
        :bos-metin="t('dashboard.kokpitAlacakVerecekBos')"
        :adet-etiketi="t('dashboard.kokpitAdet')"
        @kova-sec="kappaSec"
      />
    </div>

    <div class="alacak-tablolar">
      <div class="alacak-tablo">
        <span class="alacak-bolum-baslik">{{ t('dashboard.kokpitAlacakVerecekBorclular') }}</span>
        <MiniTablo
          :kolonlar="borcluKolonlari"
          :satirlar="borclular"
          :bos-metin="t('dashboard.kokpitAlacakVerecekBos')"
          :para-birimi="paraBirimi"
          :baslik="t('dashboard.kokpitAlacakVerecekBorclular')"
          data-test="borclular-tablo"
          @satir-sec="borcluDetay"
        >
          <template #hucre-islem="{ satir }">
            <button
              type="button"
              class="alacak-hizli-islem"
              data-test="borclu-tahsilat"
              @click.stop="borcluDetay(satir)"
            >
              <i
                class="pi pi-wallet"
                aria-hidden="true"
              />
              {{ t('dashboard.kokpitTahsilat') }}
            </button>
          </template>
        </MiniTablo>
      </div>
      <div class="alacak-tablo">
        <span class="alacak-bolum-baslik">{{ t('dashboard.kokpitAlacakVerecekTedarikciler') }}</span>
        <MiniTablo
          :kolonlar="cariKolonlari"
          :satirlar="tedarikciler"
          :bos-metin="t('dashboard.kokpitAlacakVerecekBos')"
          :para-birimi="paraBirimi"
          :baslik="t('dashboard.kokpitAlacakVerecekTedarikciler')"
          data-test="tedarikciler-tablo"
          @satir-sec="cariDetay"
        />
      </div>
    </div>

    <template #alt>
      <button
        type="button"
        class="finans-detay-buton"
        data-test="alacak-detay"
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
import YaslanmaKovalari from './YaslanmaKovalari.vue'
import MiniTablo from './MiniTablo.vue'
import { formatPara } from '../../../utils/format.js'

const props = defineProps({
  veri: { type: Object, default: null },
  yukleniyor: { type: Boolean, default: false },
  hata: { type: String, default: null },
  paraBirimi: { type: String, default: 'TRY' }
})

const emit = defineEmits(['detay'])
const { t, te } = useI18n()

const veriDegeri = computed(() => props.veri || {})
const alacakToplam = computed(() => Number(veriDegeri.value.alacakToplam) || 0)
const verecekToplam = computed(() => Number(veriDegeri.value.verecekToplam) || 0)
const net = computed(() => {
  const deger = Number(veriDegeri.value.net)
  return Number.isFinite(deger) ? deger : alacakToplam.value - verecekToplam.value
})
const borclular = computed(() => Array.isArray(veriDegeri.value.borclular) ? veriDegeri.value.borclular : [])
const tedarikciler = computed(() => Array.isArray(veriDegeri.value.tedarikciler) ? veriDegeri.value.tedarikciler : [])
const hedef = computed(() => veriDegeri.value.hedef || { route: '/cari-hesaplar', query: {} })

const tahsilatOraniMetni = computed(() => {
  if (veriDegeri.value.tahsilatOrani === null || veriDegeri.value.tahsilatOrani === undefined) {
    return '—'
  }
  const deger = Number(veriDegeri.value.tahsilatOrani)
  if (!Number.isFinite(deger)) return '—'
  return `${deger.toLocaleString('tr-TR', { maximumFractionDigits: 1 })}%`
})

const yaslanma = computed(() =>
  (Array.isArray(veriDegeri.value.yaslanma) ? veriDegeri.value.yaslanma : []).map((kova) => {
    const anahtar = `dashboard.kokpitYaslanmaKova${kova.anahtar}`
    return {
      ...kova,
      etiket: te(anahtar) ? t(anahtar) : kova.etiket
    }
  })
)

const cariKolonlari = [
  { anahtar: 'ad', etiket: t('dashboard.kokpitAlacakVerecekCari') },
  { anahtar: 'tutar', etiket: t('dashboard.kokpitAlacakVerecekTutar'), hiza: 'sag', bicim: 'para' }
]

const borcluKolonlari = [
  ...cariKolonlari,
  { anahtar: 'islem', etiket: '', hiza: 'sag' }
]

const kappaSec = (kova) => {
  emit('detay', {
    route: '/cari-hesaplar',
    query: { yaslanma: kova.anahtar }
  })
}

const borcluDetay = (satir) => {
  if (!satir?.cariId) return
  // Tahsilat ekranı cariId ile açıldığında tahsilat giriş diyaloğunu açar.
  emit('detay', {
    route: '/tahsilat',
    query: { cariId: String(satir.cariId) }
  })
}

const cariDetay = (satir) => {
  if (!satir?.cariId) return
  emit('detay', {
    route: '/cari-hesaplar',
    query: { cariId: String(satir.cariId) }
  })
}
</script>

<style scoped>
.alacak-net {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-muted);
}
.alacak-net strong {
  color: var(--success);
  font-variant-numeric: tabular-nums;
}
.alacak-net--negatif strong {
  color: var(--danger);
}
.alacak-ozet {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
}
.alacak-stat {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
  padding: 7px 8px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-muted);
}
.alacak-stat--alacak {
  border-color: var(--success-border);
  background: var(--success-soft);
}
.alacak-stat--verecek {
  border-color: var(--danger-border);
  background: var(--danger-soft);
}
.alacak-stat-etiket {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 10px;
  font-weight: 600;
  color: var(--text-muted);
}
.alacak-stat-deger {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.alacak-bolum {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
}
.alacak-bolum-baslik {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-muted);
}
.alacak-tablolar {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  min-width: 0;
}
.alacak-tablo {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}
.alacak-hizli-islem {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 2px 5px;
  border: 1px solid var(--accent-border);
  border-radius: var(--radius-xs);
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 9.5px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
}
.alacak-hizli-islem:hover,
.alacak-hizli-islem:focus-visible {
  background: var(--accent-soft-strong);
}
.alacak-hizli-islem:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 1px;
}
@media (max-width: 760px) {
  .alacak-tablolar {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
