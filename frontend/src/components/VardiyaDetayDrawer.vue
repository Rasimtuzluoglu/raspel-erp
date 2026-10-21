<template>
  <Drawer
    :visible="visible"
    position="right"
    :header="t('vardiyalar.detay')"
    :style="{ width: '440px', maxWidth: '96vw' }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div
      v-if="kayit"
      class="detay"
    >
      <div class="detay-ust">
        <div class="detay-baslik">
          <i class="pi pi-calendar-plus" />
          <span>{{ kayit.personelAdi || '-' }}</span>
        </div>
        <Tag
          :value="turEtiket(kayit.tur)"
          :severity="kayit.tur === 'SABAH' ? 'info' : kayit.tur === 'AKSAM' ? 'warn' : 'contrast'"
        />
      </div>

      <div class="detay-grid">
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('common.date') }}</span>
          <span>{{ tarih(kayit.tarih) }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('vardiyalar.baslangic') }}</span>
          <span>{{ kayit.baslangic }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('vardiyalar.bitis') }}</span>
          <span>{{ kayit.bitis }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('vardiyalar.olusturma') }}</span>
          <span>{{ tarihSaat(kayit.olusturmaTarihi) }}</span>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="detay-aksiyonlar">
        <Button
          v-if="kayit"
          :label="t('common.edit')"
          icon="pi pi-pencil"
          @click="$emit('duzenle', kayit)"
        />
        <Button
          v-if="kayit && silinebilir"
          :label="t('common.delete')"
          icon="pi pi-trash"
          severity="danger"
          outlined
          @click="$emit('sil', kayit)"
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
  kayit: { type: Object, default: null },
  silinebilir: { type: Boolean, default: false }
})

defineEmits(['update:visible', 'duzenle', 'sil'])

const { t } = useI18n()

const turEtiket = (tur) => ({
  SABAH: t('vardiyalar.turSabah'),
  AKSAM: t('vardiyalar.turAksam'),
  GECE: t('vardiyalar.turGece')
}[tur] || tur)
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
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
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
