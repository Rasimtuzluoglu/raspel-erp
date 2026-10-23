<template>
  <Drawer
    :visible="visible"
    position="right"
    :header="t('taksitTakvimi.detay')"
    :style="{ width: '460px', maxWidth: '96vw' }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div
      v-if="kalem"
      class="detay"
    >
      <div class="detay-ust">
        <div class="detay-baslik">
          <i class="pi pi-calendar" />
          <span>{{ kalem.cariAd || '-' }}</span>
        </div>
        <Tag
          :value="durumEtiket(kalem)"
          :severity="durumSeverity(kalem)"
        />
      </div>

      <div class="detay-tutar">
        {{ formatCurrency(kalem.tutar) }}
      </div>

      <div class="detay-grid">
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('taksitTakvimi.vadeTarihi') }}</span>
          <span>{{ tarih(kalem.vadeTarihi) }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('taksitTakvimi.taksitNo') }}</span>
          <span>{{ kalem.taksitNo }} / {{ kalem.taksitSayisi }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('taksitTakvimi.kurum') }}</span>
          <span>{{ kalem.kurum || '-' }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('taksitTakvimi.planNo') }}</span>
          <span class="detay-mono">{{ kalem.planNo || '-' }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('taksitTakvimi.odemeTarihi') }}</span>
          <span>{{ tarih(kalem.odemeTarihi) }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('taksitTakvimi.gecikme') }}</span>
          <span>{{ kalem.gecikmeGunu > 0 ? `${kalem.gecikmeGunu} ${t('taksitTakvimi.gun')}` : '-' }}</span>
        </div>
        <div class="detay-alan genis">
          <span class="detay-etiket">{{ t('common.description') }}</span>
          <span>{{ kalem.aciklama || '-' }}</span>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="detay-aksiyonlar">
        <Button
          v-if="kalem && kalem.odemeDurumu !== 'ODENDI' && odeYetkisi"
          :label="t('taksitTakvimi.ode')"
          icon="pi pi-check"
          @click="$emit('ode', kalem)"
        />
        <Button
          v-if="kalem && silYetkisi && kalem.odemeDurumu !== 'ODENDI'"
          :label="t('common.delete')"
          icon="pi pi-trash"
          severity="danger"
          outlined
          @click="$emit('sil', kalem)"
        />
      </div>
    </template>
  </Drawer>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { formatCurrency, formatTarih as tarih } from '../utils/format.js'

defineProps({
  visible: { type: Boolean, default: false },
  kalem: { type: Object, default: null },
  odeYetkisi: { type: Boolean, default: false },
  silYetkisi: { type: Boolean, default: false }
})

defineEmits(['update:visible', 'ode', 'sil'])

const { t } = useI18n()

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
.detay-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}
.detay-baslik {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--text-primary);
}
.detay-baslik i {
  color: var(--accent);
}
.detay-tutar {
  font-size: 1.35rem;
  font-weight: 800;
  color: var(--accent);
  font-variant-numeric: tabular-nums;
}
.detay-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}
.detay-alan {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.detay-alan.genis {
  grid-column: 1 / -1;
}
.detay-etiket {
  font-size: 11.5px;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: var(--text-muted);
}
.detay-mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
}
.detay-aksiyonlar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
