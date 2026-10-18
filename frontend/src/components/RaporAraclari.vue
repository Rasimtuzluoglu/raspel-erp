<template>
  <div class="rapor-araclari">
    <span class="rapor-araclari-baslik">
      <i :class="ikon" />{{ baslik }}
    </span>
    <Button
      :icon="favori ? 'pi pi-star-fill' : 'pi pi-star'"
      :label="favori ? t('raporlar.favoridenCikar') : t('raporlar.favorilereEkle')"
      size="small"
      :severity="favori ? 'warn' : 'secondary'"
      :outlined="!favori"
      @click="$emit('favori-degistir', favoriKey, favoriIndex, baslik)"
    />
    <span class="arac-bosluk" />
    <Button
      v-if="pdf"
      icon="pi pi-file-pdf"
      :label="t('raporlar.pdf')"
      size="small"
      outlined
      @click="pdf()"
    />
    <Button
      v-if="excel"
      icon="pi pi-file-excel"
      :label="t('raporlar.excelIndir')"
      size="small"
      outlined
      @click="excel()"
    />
    <Button
      v-if="eposta"
      icon="pi pi-envelope"
      :label="t('raporlar.epostaGonder')"
      size="small"
      outlined
      @click="eposta()"
    />
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

defineProps({
  baslik: { type: String, default: '' },
  ikon: { type: String, default: 'pi pi-chart-bar' },
  favoriKey: { type: String, default: '' },
  favoriIndex: { type: Number, default: null },
  favori: { type: Boolean, default: false },
  pdf: { type: Function, default: null },
  excel: { type: Function, default: null },
  eposta: { type: Function, default: null }
})

defineEmits(['favori-degistir'])
</script>

<style scoped>
.rapor-araclari {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 10px 12px;
  margin-bottom: 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-card);
}
.rapor-araclari-baslik {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 1rem;
  font-weight: 700;
  color: var(--text-primary);
}
.rapor-araclari-baslik i {
  color: var(--accent);
}
.arac-bosluk {
  flex: 1 1 auto;
}
</style>
