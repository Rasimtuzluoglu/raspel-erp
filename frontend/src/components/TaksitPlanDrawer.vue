<template>
  <Drawer
    :visible="visible"
    position="right"
    :header="plan?.cariAd || t('taksitTakvimi.planBaslik')"
    :style="{ width: '560px', maxWidth: '96vw' }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div
      v-if="plan"
      class="detay"
    >
      <div class="ozet-ust">
        <div class="ozet-sol">
          <span class="ozet-cari">{{ plan.cariAd }}</span>
          <span class="ozet-alt">{{ plan.kurum || '-' }} · {{ plan.planNo }}</span>
        </div>
        <Tag
          :value="`${plan.odenenAdet}/${plan.taksitSayisi}`"
          :severity="plan.odenenAdet >= plan.taksitSayisi ? 'success' : 'warning'"
        />
      </div>

      <div class="ilerleme">
        <div
          class="ilerleme-bar"
          :style="{ width: `${yuzde}%` }"
        />
      </div>

      <div class="detay-grid">
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('taksitTakvimi.toplamTutar') }}</span>
          <strong>{{ formatCurrency(plan.toplamTutar) }}</strong>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('taksitTakvimi.odenenTutar') }}</span>
          <strong class="pozitif">{{ formatCurrency(plan.odenenTutar) }}</strong>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('taksitTakvimi.kalan') }}</span>
          <strong>{{ formatCurrency(plan.kalanTutar) }}</strong>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('taksitTakvimi.ilkVade') }}</span>
          <span>{{ tarih(plan.ilkVade) }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('taksitTakvimi.sonVade') }}</span>
          <span>{{ tarih(plan.sonVade) }}</span>
        </div>
      </div>

      <div class="kalemler">
        <div class="kalemler-baslik">
          <span>{{ t('taksitTakvimi.taksitler') }}</span>
          <Tag
            :value="String(kalemler.length)"
            severity="secondary"
          />
        </div>
        <div
          v-if="yukleniyor"
          class="kalem-bos"
          role="status"
          aria-live="polite"
        >
          <i class="pi pi-spin pi-spinner" /> {{ t('common.loading') }}
        </div>
        <ul
          v-else
          class="kalem-liste"
          :aria-label="t('taksitTakvimi.taksitler')"
        >
          <li
            v-for="k in kalemler"
            :key="k.id"
            class="kalem-satir"
          >
            <span class="kalem-no">{{ k.taksitNo }}/{{ k.taksitSayisi }}</span>
            <div class="kalem-orta">
              <span>{{ tarih(k.vadeTarihi) }}</span>
              <span class="kalem-alt">{{ formatCurrency(k.tutar) }}</span>
            </div>
            <Tag
              :value="durumEtiket(k)"
              :severity="durumSeverity(k)"
            />
            <Button
              v-if="k.odemeDurumu !== 'ODENDI' && odeYetkisi"
              icon="pi pi-check"
              text
              rounded
              size="small"
              :aria-label="t('taksitTakvimi.ode')"
              @click="$emit('ode', k)"
            />
          </li>
        </ul>
      </div>
    </div>

    <template #footer>
      <div class="detay-aksiyonlar">
        <Button
          v-if="plan && silYetkisi"
          :label="t('taksitTakvimi.planSil')"
          icon="pi pi-trash"
          severity="danger"
          outlined
          @click="$emit('sil', plan)"
        />
      </div>
    </template>
  </Drawer>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatCurrency, formatTarih as tarih } from '../utils/format.js'

const props = defineProps({
  visible: { type: Boolean, default: false },
  plan: { type: Object, default: null },
  kalemler: { type: Array, default: () => [] },
  yukleniyor: { type: Boolean, default: false },
  odeYetkisi: { type: Boolean, default: false },
  silYetkisi: { type: Boolean, default: false }
})

defineEmits(['update:visible', 'sil', 'ode'])

const { t } = useI18n()

const yuzde = computed(() => {
  const toplam = Number(props.plan?.taksitSayisi || 0)
  const odenen = Number(props.plan?.odenenAdet || 0)
  if (!toplam) return 0
  return Math.min(100, Math.round((odenen / toplam) * 100))
})

const durumEtiket = (k) => {
  if (k.odemeDurumu === 'ODENDI') return t('taksitTakvimi.odendi')
  if (k.gecikmeGunu > 0) return t('taksitTakvimi.gecikmis')
  return t('taksitTakvimi.bekliyor')
}

const durumSeverity = (k) => {
  if (k.odemeDurumu === 'ODENDI') return 'success'
  if (k.gecikmeGunu > 0) return 'danger'
  return 'warning'
}
</script>

<style scoped>
.detay {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.ozet-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.ozet-sol {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.ozet-cari {
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--text-primary);
}
.ozet-alt {
  font-size: 12px;
  color: var(--text-muted);
}
.ilerleme {
  height: 8px;
  border-radius: 999px;
  background: var(--bg-muted);
  overflow: hidden;
}
.ilerleme-bar {
  height: 100%;
  border-radius: 999px;
  background: var(--accent);
  transition: width var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.detay-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
  gap: 12px;
}
.detay-alan {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.detay-etiket {
  font-size: 11.5px;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: var(--text-muted);
}
.pozitif {
  color: var(--success, #22c55e);
}
.kalemler {
  border-top: 1px solid var(--border);
  padding-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.kalemler-baslik {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12.5px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: var(--text-secondary);
}
.kalem-liste {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.kalem-satir {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md, 10px);
  background: var(--bg-muted);
}
.kalem-no {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-secondary);
  min-width: 40px;
}
.kalem-orta {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1 1 auto;
  min-width: 0;
  font-size: 12.5px;
  color: var(--text-primary);
}
.kalem-alt {
  font-size: 11.5px;
  color: var(--text-muted);
}
.kalem-bos {
  padding: 12px;
  text-align: center;
  font-size: 13px;
  color: var(--text-muted);
}
.detay-aksiyonlar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
@media (prefers-reduced-motion: reduce) {
  .ilerleme-bar {
    transition: none;
  }
}
</style>
