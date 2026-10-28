<template>
  <div
    class="bugun-serit"
    role="list"
    :aria-label="t('dashboard.bugunSeridi')"
    data-test="bugun-seridi"
  >
    <div
      v-for="k in kalemler"
      :key="k.key"
      class="bugun-hucre"
      :class="`bugun-hucre--${k.key}`"
      role="listitem"
      :data-test="`bugun-${k.key}`"
    >
      <span class="bugun-etiket">{{ k.etiket }}</span>
      <strong
        v-if="!yukleniyor"
        class="bugun-deger gizli-veri"
      >{{ k.deger }}</strong>
      <span
        v-else
        class="bugun-iskelet"
        aria-hidden="true"
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatCurrency } from '../../utils/format.js'

const props = defineProps({
  bugun: { type: Object, default: null },
  yukleniyor: { type: Boolean, default: false }
})

const { t } = useI18n()

const sayi = (deger) => {
  const n = Number(deger)
  if (!Number.isFinite(n)) return '—'
  return n.toLocaleString('tr-TR')
}

const para = (deger) => {
  if (deger === null || deger === undefined || deger === '') return '—'
  return formatCurrency(Number(deger))
}

const kalemler = computed(() => {
  const b = props.bugun || {}
  return [
    { key: 'tahsilat', etiket: t('dashboard.bugunTahsilat'), deger: para(b.tahsilat) },
    { key: 'odeme', etiket: t('dashboard.bugunOdeme'), deger: para(b.odeme) },
    { key: 'siparis', etiket: t('dashboard.bugunSiparis'), deger: sayi(b.siparis) },
    { key: 'teslimat', etiket: t('dashboard.bugunTeslimat'), deger: sayi(b.teslimat) }
  ]
})
</script>

<style scoped>
/*
 * "Bugün" seridi KPI'larin hemen altinda tek satir, ince bir ozet serittir:
 * kart degil; 4 esit hucre, aralarinda dikey ayirici.
 */
.bugun-serit {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0;
  padding: 2px 0 4px;
  min-width: 0;
}
.bugun-hucre {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
  padding: 2px 16px;
  border-left: 1px solid var(--border);
}
.bugun-hucre:first-child {
  padding-left: 0;
  border-left: 0;
}
.bugun-etiket {
  font-size: 11px;
  color: var(--text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.bugun-deger {
  font-size: 14px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--text-primary);
  white-space: nowrap;
}
.bugun-hucre--tahsilat .bugun-deger {
  color: var(--success);
}
.bugun-hucre--odeme .bugun-deger {
  color: var(--danger);
}
.bugun-iskelet {
  display: block;
  width: 54px;
  height: 12px;
  border-radius: 6px;
  background: var(--bg-muted);
  animation: bugunParlama 1.4s ease infinite;
}
@keyframes bugunParlama {
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
  .bugun-iskelet {
    animation: none;
  }
}
@media (max-width: 700px) {
  .bugun-serit {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    row-gap: 6px;
  }
  .bugun-hucre:nth-child(3) {
    padding-left: 0;
    border-left: 0;
  }
}
@media (max-width: 420px) {
  .bugun-serit {
    grid-template-columns: minmax(0, 1fr);
  }
  .bugun-hucre,
  .bugun-hucre:nth-child(3) {
    padding-left: 0;
    border-left: 0;
  }
}
</style>
