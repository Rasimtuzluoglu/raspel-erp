<template>
  <Drawer
    :visible="visible"
    position="right"
    :header="sayim?.stokAdi || t('stokSayim.title')"
    :style="{ width: '480px', maxWidth: '96vw' }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div
      v-if="sayim"
      class="detay"
    >
      <div class="detay-ust">
        <div class="detay-baslik">
          <i class="pi pi-clipboard" />
          <span>{{ sayim.stokAdi || '-' }}</span>
        </div>
        <Tag
          :value="durumLabel(sayim.durum, t)"
          :severity="durumSeverity(sayim.durum)"
        />
      </div>

      <div class="detay-grid">
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('common.date') }}</span>
          <span>{{ tarih(sayim.tarih) }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('stokSayim.beklenen') }}</span>
          <span>{{ formatNumber(sayim.beklenenMiktar) }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('stokSayim.sayilan') }}</span>
          <span>{{ formatNumber(sayim.sayilanMiktar) }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('stokSayim.fark') }}</span>
          <strong :class="farkSinif">{{ formatNumber(fark) }}</strong>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('stokSayim.olusturma') }}</span>
          <span>{{ tarihSaat(sayim.olusturmaTarihi) }}</span>
        </div>
      </div>

      <div
        v-if="sayim.aciklama"
        class="detay-aciklama"
      >
        {{ sayim.aciklama }}
      </div>
    </div>

    <template #footer>
      <div class="detay-aksiyonlar">
        <Button
          v-if="sayim && sayim.durum === 'TASLAK'"
          :label="t('common.edit')"
          icon="pi pi-pencil"
          @click="$emit('duzenle', sayim)"
        />
        <Button
          v-if="sayim && sayim.durum !== 'TAMAMLANDI'"
          :label="t('stokSayim.tamamla')"
          icon="pi pi-check-circle"
          class="p-button-success"
          @click="$emit('tamamla', sayim)"
        />
        <Button
          v-if="sayim && sayim.durum !== 'IPTAL'"
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text p-button-danger"
          @click="$emit('iptal', sayim)"
        />
      </div>
    </template>
  </Drawer>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatTarih as tarih, formatTarihSaat as tarihSaat, durumLabel } from '../utils/format.js'

const props = defineProps({
  visible: { type: Boolean, default: false },
  sayim: { type: Object, default: null }
})

defineEmits(['update:visible', 'duzenle', 'tamamla', 'iptal'])

const { t } = useI18n()

const formatNumber = (v) => {
  if (v === null || v === undefined) return '0'
  return Number(v).toLocaleString('tr-TR', { maximumFractionDigits: 4 })
}

const fark = computed(() => {
  const s = props.sayim
  if (!s) return 0
  return Number(s.fark ?? Number(s.sayilanMiktar || 0) - Number(s.beklenenMiktar || 0))
})

const farkSinif = computed(() => (fark.value >= 0 ? 'positive' : 'negative'))

const durumSeverity = (d) => (d === 'TAMAMLANDI' ? 'success' : d === 'IPTAL' ? 'danger' : 'warn')
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
.detay-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
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
.detay-aciklama {
  padding: 10px;
  border-radius: 8px;
  background: var(--bg-hover, rgba(148, 163, 184, 0.1));
  font-size: 13px;
}
.positive {
  color: #4ade80;
}
.negative {
  color: #f87171;
}
.detay-aksiyonlar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
