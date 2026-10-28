<template>
  <div
    class="piyasa-serit"
    data-test="piyasa-seridi"
    :aria-label="t('dashboard.piyasa')"
  >
    <span class="piyasa-etiket">{{ t('dashboard.piyasa') }}</span>
    <div class="piyasa-kaydir">
      <span
        v-for="k in gosterilecek"
        :key="k.kod"
        class="piyasa-kalem"
        :data-test="`piyasa-${k.kod}`"
      >
        <span class="piyasa-kod">{{ k.kod }}</span>
        <span class="piyasa-deger gizli-veri">{{ k.deger }}</span>
      </span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDovizStore } from '../../stores/dovizStore.js'

const { t } = useI18n()
const dovizStore = useDovizStore()

// Istek uzerine en fazla 4 para birimi; SAR/GAU gibi ek kalemler gosterilmez.
const GOSTERILECEK = ['USD', 'EUR', 'GBP', 'BTC']

const bicimle = (deger) => {
  const sayi = Number(deger)
  if (!Number.isFinite(sayi) || sayi <= 0) return '—'
  return new Intl.NumberFormat('tr-TR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(sayi)
}

const gosterilecek = computed(() => {
  const kurlar = Array.isArray(dovizStore?.kurlar) ? dovizStore.kurlar : []
  return GOSTERILECEK.map((kod) => {
    const kayit = kurlar.find((k) => (k.kod || k.dovizKodu) === kod)
    return { kod, deger: bicimle(kayit?.satisFiyati ?? kayit?.satisKuru) }
  })
})
</script>

<style scoped>
/*
 * Piyasa seridi ince tek satirdir: kart degil, cerceve/arka plan yok.
 * Yatay kaydirilir; satir asla kirilmaz.
 */
.piyasa-serit {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  padding: 0 0 4px;
  font-size: 12px;
  line-height: 1.6;
}
.piyasa-etiket {
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.4px;
  text-transform: uppercase;
  color: var(--text-muted);
}
.piyasa-kaydir {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: nowrap;
  overflow-x: auto;
  min-width: 0;
  scrollbar-width: none;
}
.piyasa-kaydir::-webkit-scrollbar {
  display: none;
}
.piyasa-kalem {
  display: inline-flex;
  align-items: baseline;
  gap: 5px;
  flex-shrink: 0;
  white-space: nowrap;
}
.piyasa-kod {
  font-size: 11px;
  font-weight: 700;
  color: var(--text-secondary);
}
.piyasa-deger {
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  color: var(--text-primary);
}
</style>
