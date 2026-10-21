<template>
  <Drawer
    :visible="visible"
    position="right"
    :header="t('izinler.detay')"
    :style="{ width: '460px', maxWidth: '96vw' }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div
      v-if="yukleniyor && !izin"
      class="detay-yukleniyor"
      role="status"
      :aria-label="t('common.loading')"
    >
      <SkeletonLoader
        :count="4"
        height="56px"
      />
    </div>

    <div
      v-else-if="izin"
      class="detay"
    >
      <div class="detay-ust">
        <div class="detay-baslik">
          <i class="pi pi-calendar" />
          <span>{{ izin.personelAdi || '-' }}</span>
        </div>
        <Tag
          :value="durumEtiket(izin.durum)"
          :severity="izin.durum === 'ONAYLANDI' ? 'success' : izin.durum === 'REDDEDILDI' ? 'danger' : 'warn'"
        />
      </div>

      <div class="detay-grid">
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('izinler.izinTuru') }}</span>
          <span>{{ izinTuruEtiket(izin.izinTuru) }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('izinler.gun') }}</span>
          <strong>{{ izin.gunSayisi }}</strong>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('izinler.baslangic') }}</span>
          <span>{{ tarih(izin.baslangic) }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('izinler.bitis') }}</span>
          <span>{{ tarih(izin.bitis) }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('izinler.onaylayan') }}</span>
          <span>{{ izin.onaylayan || '-' }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('izinler.olusturma') }}</span>
          <span>{{ tarihSaat(izin.olusturmaTarihi) }}</span>
        </div>
        <div class="detay-alan genis">
          <span class="detay-etiket">{{ t('common.description') }}</span>
          <span>{{ izin.aciklama || '-' }}</span>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="detay-aksiyonlar">
        <Button
          v-if="onaylanabilir"
          :label="t('izinler.onayla')"
          icon="pi pi-check"
          severity="success"
          @click="$emit('durum', 'ONAYLANDI')"
        />
        <Button
          v-if="onaylanabilir"
          :label="t('izinler.reddet')"
          icon="pi pi-times"
          severity="danger"
          outlined
          @click="$emit('durum', 'REDDEDILDI')"
        />
        <Button
          v-if="silinebilir"
          :label="t('common.delete')"
          icon="pi pi-trash"
          severity="danger"
          text
          @click="$emit('sil')"
        />
      </div>
    </template>
  </Drawer>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import SkeletonLoader from './SkeletonLoader.vue'
import { formatTarih as tarih, formatTarihSaat as tarihSaat } from '../utils/format.js'

defineProps({
  visible: { type: Boolean, default: false },
  izin: { type: Object, default: null },
  yukleniyor: { type: Boolean, default: false },
  onaylanabilir: { type: Boolean, default: false },
  silinebilir: { type: Boolean, default: false }
})

defineEmits(['update:visible', 'durum', 'sil'])

const { t } = useI18n()

const izinTuruEtiket = (kod) => ({
  YILLIK_IZIN: t('izinler.yillikIzin'),
  HASTA_IZNI: t('izinler.hastaIzni'),
  MAZERET_IZNI: t('izinler.mazeretIzni'),
  DOGUM_IZNI: t('izinler.dogumIzni'),
  BABALIK_IZNI: t('izinler.babalikIzni'),
  EVLILIK_IZNI: t('izinler.evlilikIzni'),
  UCRETSIZ_IZIN: t('izinler.ucretsizIzin')
}[kod] || kod)

const durumEtiket = (durum) => ({
  BEKLEMEDE: t('izinler.beklemede'),
  ONAYLANDI: t('izinler.onaylandi'),
  REDDEDILDI: t('izinler.reddedildi')
}[durum] || durum)
</script>

<style scoped>
.detay {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.detay-yukleniyor {
  display: flex;
  flex-direction: column;
  gap: 10px;
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
.detay-alan.genis {
  grid-column: 1 / -1;
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
