<template>
  <Drawer
    :visible="visible"
    position="right"
    :header="donem?.ad || t('donemler.title')"
    :style="{ width: '480px', maxWidth: '96vw' }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div
      v-if="donem"
      class="detay"
    >
      <div class="detay-ust">
        <div class="detay-baslik">
          <i class="pi pi-calendar" />
          <span>{{ donem.ad }}</span>
        </div>
        <div class="detay-rozetler">
          <Tag
            v-if="guncel"
            :value="t('donemler.guncel')"
            severity="info"
            icon="pi pi-flag"
          />
          <Tag
            :value="donem.aktif ? t('donemler.aktif') : t('donemler.pasif')"
            :severity="donem.aktif ? 'success' : 'danger'"
          />
          <Tag
            v-if="donem.kilitli"
            :value="t('donemler.kilitli')"
            severity="warn"
            icon="pi pi-lock"
          />
        </div>
      </div>

      <div class="detay-grid">
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('donemler.baslangic') }}</span>
          <span>{{ tarih(donem.baslangic) }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('donemler.bitis') }}</span>
          <span>{{ tarih(donem.bitis) }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('donemler.sure') }}</span>
          <span>{{ gunSayisi }} {{ t('donemler.gun') }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('donemler.olusturma') }}</span>
          <span>{{ tarihSaat(donem.olusturmaTarihi) }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('donemler.kilitTarihi') }}</span>
          <span>{{ donem.kilitli ? tarihSaat(donem.kilitTarihi) : '-' }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('donemler.kilitKullanici') }}</span>
          <span>{{ donem.kilitKullaniciId != null ? '#' + donem.kilitKullaniciId : '-' }}</span>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="detay-aksiyonlar">
        <Button
          v-if="donem && !donem.aktif"
          :label="t('donemler.aktifYap')"
          icon="pi pi-check-circle"
          @click="$emit('aktif', donem)"
        />
        <Button
          v-if="donem && !donem.kilitli"
          :label="t('donemler.kilitle')"
          icon="pi pi-lock"
          severity="warning"
          @click="$emit('kilitle', donem)"
        />
        <Button
          v-else-if="donem"
          :label="t('donemler.kilidiAc')"
          icon="pi pi-lock-open"
          outlined
          @click="$emit('kilidi-ac', donem)"
        />
        <Button
          v-if="donem"
          :label="t('common.edit')"
          icon="pi pi-pencil"
          text
          :disabled="donem.kilitli"
          @click="$emit('duzenle', donem)"
        />
      </div>
    </template>
  </Drawer>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatTarih as tarih, formatTarihSaat as tarihSaat } from '../utils/format.js'

const props = defineProps({
  visible: { type: Boolean, default: false },
  donem: { type: Object, default: null },
  guncel: { type: Boolean, default: false }
})

defineEmits(['update:visible', 'aktif', 'kilitle', 'kilidi-ac', 'duzenle'])

const { t } = useI18n()

const gunSayisi = computed(() => {
  const d = props.donem
  if (!d) return 0
  const b = new Date(d.baslangic)
  const s = new Date(d.bitis)
  if (Number.isNaN(b.getTime()) || Number.isNaN(s.getTime()) || s < b) return 0
  return Math.round((s - b) / 86400000) + 1
})
</script>

<style scoped>
.detay {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.detay-ust {
  display: flex;
  flex-direction: column;
  gap: 10px;
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
.detay-rozetler {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
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
