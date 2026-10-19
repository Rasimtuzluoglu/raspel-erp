<template>
  <Drawer
    :visible="visible"
    position="right"
    :header="seri?.seriNo || t('stokSeriler.title')"
    :style="{ width: '480px', maxWidth: '96vw' }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div
      v-if="seri"
      class="detay"
    >
      <div class="detay-ust">
        <div class="detay-baslik">
          <i class="pi pi-tags" />
          <span>{{ seri.stokAdi || '-' }}</span>
        </div>
        <Tag
          :value="seri.durum === 'TUKETILDI' ? t('stokSeriler.durumTuketildi') : t('stokSeriler.durumStokta')"
          :severity="seri.durum === 'TUKETILDI' ? 'secondary' : 'success'"
        />
      </div>

      <div class="detay-grid">
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('stokSeriler.seriNo') }}</span>
          <span>{{ seri.seriNo || '-' }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('stokSeriler.lotNo') }}</span>
          <span>{{ seri.lotNo || '-' }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('stokSeriler.miktar') }}</span>
          <span>{{ formatNumber(seri.miktar) }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('stokSeriler.kalanMiktar') }}</span>
          <strong>{{ formatNumber(seri.kalanMiktar) }}</strong>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('stokSeriler.depo') }}</span>
          <span>{{ depoAdi || '-' }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('stokSeriler.skt') }}</span>
          <span>{{ tarih(seri.sonKullanmaTarihi) }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('stokSeriler.girisTarihi') }}</span>
          <span>{{ tarih(seri.girisTarihi) }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('stokSeriler.olusturma') }}</span>
          <span>{{ tarihSaat(seri.olusturmaTarihi) }}</span>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="detay-aksiyonlar">
        <Button
          v-if="seri && seri.durum === 'STOKTA'"
          :label="t('common.edit')"
          icon="pi pi-pencil"
          @click="$emit('duzenle', seri)"
        />
      </div>
    </template>
  </Drawer>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { formatTarih as tarih, formatTarihSaat as tarihSaat } from '../utils/format.js'

defineProps({
  visible: { type: Boolean, default: false },
  seri: { type: Object, default: null },
  depoAdi: { type: String, default: '' }
})

defineEmits(['update:visible', 'duzenle'])

const { t } = useI18n()

const formatNumber = (v) => {
  if (v === null || v === undefined) return '0'
  return Number(v).toLocaleString('tr-TR', { maximumFractionDigits: 4 })
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
.detay-aksiyonlar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
