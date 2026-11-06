<template>
  <FinansKarti
    :baslik="t('dashboard.kokpitFaturaSiparisBaslik')"
    :alt-baslik="t('dashboard.kokpitFaturaSiparisAltBaslik')"
    ikon="pi pi-file-invoice"
    :yukleniyor="yukleniyor"
    :hata="hata"
    data-test="fatura-siparis-bolumu"
  >
    <template #ust>
      <button
        type="button"
        class="fatura-hizli-islem"
        data-test="fatura-tahsilat"
        @click="emit('detay', { route: '/tahsilat', query: {} })"
      >
        <i
          class="pi pi-wallet"
          aria-hidden="true"
        />
        {{ t('dashboard.kokpitTahsilat') }}
      </button>
    </template>

    <div class="fatura-ozet">
      <div class="fatura-stat fatura-stat--kritik">
        <span class="fatura-stat-etiket">{{ t('dashboard.kokpitFaturaSiparisVadesiGecen') }}</span>
        <strong class="fatura-stat-deger gizli-veri">{{ formatPara(vadesiGecenTutar, paraBirimi) }}</strong>
        <span class="fatura-stat-alt">{{ vadesiGecenAdet }} {{ t('dashboard.kokpitFaturaAdet') }}</span>
      </div>
      <div class="fatura-stat">
        <span class="fatura-stat-etiket">{{ t('dashboard.kokpitFaturaSiparisTahsilatOrani') }}</span>
        <strong class="fatura-stat-deger">{{ tahsilatOraniMetni }}</strong>
        <span class="fatura-stat-alt">{{ t('dashboard.kokpitFaturaSiparisDurumlar') }}</span>
      </div>
      <div class="fatura-stat">
        <span class="fatura-stat-etiket">{{ t('dashboard.kokpitFaturaSiparisSiparisDurumlari') }}</span>
        <strong class="fatura-stat-deger">{{ acikSiparis }}</strong>
        <span class="fatura-stat-alt">{{ t('dashboard.kokpitAdet') }}</span>
      </div>
    </div>

    <div
      class="fatura-pipeline"
      data-test="fatura-pipeline"
    >
      <span class="fatura-bolum-baslik">{{ t('dashboard.kokpitFaturaSiparisDurumlar') }}</span>
      <PipelineKovalari
        :kovalar="durumlar"
        :para-birimi="paraBirimi"
        :bos-metin="t('dashboard.kokpitFaturaSiparisBos')"
        @kova-sec="durumDetay"
      />
    </div>

    <div class="fatura-pipeline fatura-pipeline--siparis">
      <span class="fatura-bolum-baslik">{{ t('dashboard.kokpitFaturaSiparisSiparisDurumlari') }}</span>
      <PipelineKovalari
        :kovalar="siparisDurumlari"
        :para-birimi="paraBirimi"
        :bos-metin="t('dashboard.kokpitFaturaSiparisBos')"
        data-test="siparis-pipeline"
        @kova-sec="siparisDetay"
      />
    </div>

    <div
      class="fatura-vadesi-gecen"
      data-test="fatura-vadesi-gecen"
    >
      <span class="fatura-bolum-baslik">{{ t('dashboard.kokpitFaturaSiparisVadesiGecen') }}</span>
      <MiniTablo
        :kolonlar="vadeKolonlari"
        :satirlar="vadesiGecenFaturalar"
        :bos-metin="t('dashboard.kokpitFaturaSiparisBos')"
        :para-birimi="paraBirimi"
        :baslik="t('dashboard.kokpitFaturaSiparisVadesiGecen')"
        data-test="vadesi-gecen-tablo"
        @satir-sec="faturaDetay"
      />
    </div>

    <template #alt>
      <button
        type="button"
        class="finans-detay-buton"
        data-test="fatura-detay"
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
import MiniTablo from './MiniTablo.vue'
import PipelineKovalari from './PipelineKovalari.vue'
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
const vadesiGecenTutar = computed(() => Number(veriDegeri.value.vadesiGecenTutar) || 0)
const vadesiGecenAdet = computed(() => Number(veriDegeri.value.vadesiGecenAdet) || 0)
const hedef = computed(() => veriDegeri.value.hedef || { route: '/tahsilat', query: {} })

const tahsilatOraniMetni = computed(() => {
  if (veriDegeri.value.tahsilatOrani === null || veriDegeri.value.tahsilatOrani === undefined) {
    return '—'
  }
  const deger = Number(veriDegeri.value.tahsilatOrani)
  if (!Number.isFinite(deger)) return '—'
  return `${deger.toLocaleString('tr-TR', { maximumFractionDigits: 1 })}%`
})

const siparisDurumAnahtarlari = {
  TEKLIF: 'dashboard.siparisDurumTeklif',
  SIPARIS: 'dashboard.siparisDurumSiparis',
  BEKLIYOR: 'dashboard.siparisDurumBekliyor',
  HAZIRLANIYOR: 'dashboard.siparisDurumHazirlaniyor',
  YOLDA: 'dashboard.siparisDurumYolda',
  TESLIM_EDILDI: 'dashboard.siparisDurumTeslimEdildi',
  FATURA_KESILDI: 'dashboard.siparisDurumFaturaKesildi',
  IPTAL: 'dashboard.siparisDurumIptal'
}

const durumlar = computed(() =>
  (Array.isArray(veriDegeri.value.durumlar) ? veriDegeri.value.durumlar : []).map((kova) => {
    const anahtar = `dashboard.kokpitFaturaDurum${kova.anahtar}`
    return {
      ...kova,
      etiket: te(anahtar) ? t(anahtar) : kova.etiket
    }
  })
)

const siparisDurumlari = computed(() =>
  (Array.isArray(veriDegeri.value.siparisDurumlari) ? veriDegeri.value.siparisDurumlari : []).map((kova) => {
    const anahtar = siparisDurumAnahtarlari[kova.anahtar]
    return {
      ...kova,
      etiket: anahtar && te(anahtar) ? t(anahtar) : kova.etiket
    }
  })
)

const acikSiparis = computed(() =>
  siparisDurumlari.value
    .filter((kova) => kova.anahtar !== 'TESLIM_EDILDI' && kova.anahtar !== 'IPTAL')
    .reduce((toplam, kova) => toplam + (Number(kova.adet) || 0), 0)
)

const vadesiGecenFaturalar = computed(() =>
  (Array.isArray(veriDegeri.value.vadesiGecenFaturalar) ? veriDegeri.value.vadesiGecenFaturalar : []).slice(0, 8)
)

const vadeKolonlari = [
  { anahtar: 'faturaNumarasi', etiket: t('dashboard.kokpitFaturaSiparisFaturaNo') },
  { anahtar: 'cariAd', etiket: t('dashboard.kokpitFaturaSiparisCari'), mobilGizle: true },
  { anahtar: 'vadeTarihi', etiket: t('dashboard.kokpitFaturaSiparisVade'), bicim: 'tarih', mobilGizle: true },
  { anahtar: 'kalanTutar', etiket: t('dashboard.kokpitFaturaSiparisKalan'), hiza: 'sag', bicim: 'para' }
]

const durumDetay = (kova) => {
  if (kova?.anahtar === 'VADESI_GECEN') {
    emit('detay', { route: '/tahsilat', query: {} })
    return
  }
  if (kova?.anahtar === 'TAHSIL_EDILDI') {
    emit('detay', { route: '/faturalar', query: { odemeDurumu: 'ODENDI' } })
    return
  }
  emit('detay', { route: '/faturalar', query: { durum: kova?.anahtar || '' } })
}

const siparisDetay = (kova) => {
  emit('detay', { route: '/siparisler', query: { durum: kova?.anahtar || '' } })
}

const faturaDetay = (satir) => {
  if (!satir?.faturaNumarasi) return
  emit('detay', { route: '/faturalar', query: { no: satir.faturaNumarasi } })
}
</script>

<style scoped>
.fatura-ozet {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
}
.fatura-hizli-islem {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 3px 6px;
  border: 1px solid var(--accent-border);
  border-radius: var(--radius-xs);
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 10px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
}
.fatura-hizli-islem:hover,
.fatura-hizli-islem:focus-visible {
  background: var(--accent-soft-strong);
}
.fatura-hizli-islem:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 1px;
}
.fatura-stat {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
  padding: 7px 8px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-muted);
}
.fatura-stat--kritik {
  border-color: var(--danger-border);
  background: var(--danger-soft);
}
.fatura-stat-etiket {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 10px;
  font-weight: 600;
  color: var(--text-muted);
}
.fatura-stat-deger {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.fatura-stat-alt {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 10px;
  color: var(--text-muted);
}
.fatura-pipeline,
.fatura-vadesi-gecen {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
}
.fatura-bolum-baslik {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-muted);
}
@media (max-width: 600px) {
  .fatura-ozet {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
