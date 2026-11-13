<template>
  <div class="kpi-grid depo-kpi">
    <button
      v-for="k in kartlar"
      :key="k.anahtar"
      type="button"
      class="kpi-tikla"
      :aria-label="k.baslik"
      @click="$emit('kpi-sec', { anahtar: k.anahtar })"
    >
      <KpiKart
        :baslik="k.baslik"
        :deger="k.deger"
        :ikon="k.ikon"
        :para-birimi="false"
        :renk="k.renk"
      />
    </button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import KpiKart from '../KpiKart.vue'

const props = defineProps({
  ozet: { type: Object, default: () => ({}) },
  gecikenTalep: { type: Number, default: 0 }
})
defineEmits(['kpi-sec'])

const { t } = useI18n()
const kartlar = computed(() => [
  { anahtar: 'depolar', baslik: t('depolar.kpiToplamDepo'), deger: props.ozet.toplamDepo || 0, ikon: 'pi pi-warehouse', renk: '#3b82f6' },
  { anahtar: 'aktif', baslik: t('depolar.kpiAktifDepo'), deger: props.ozet.aktifDepo || 0, ikon: 'pi pi-check-circle', renk: '#10b981' },
  { anahtar: 'kalem', baslik: t('depolar.kpiStokKalemi'), deger: props.ozet.stokKalemiSayisi || 0, ikon: 'pi pi-box', renk: '#8b5cf6' },
  { anahtar: 'sifir', baslik: t('depolar.kpiSifirStok'), deger: props.ozet.sifirStokluKalem || 0, ikon: 'pi pi-exclamation-triangle', renk: '#ef4444' },
  { anahtar: 'talep', baslik: t('depolar.kpiBekleyenTalep'), deger: props.ozet.bekleyenTalep || 0, ikon: 'pi pi-list', renk: '#f59e0b' },
  { anahtar: 'geciken', baslik: t('depolar.kpiGecikenTalep'), deger: props.gecikenTalep, ikon: 'pi pi-clock', renk: '#dc2626' }
])
</script>

<style scoped>
/* 6 KPI karti: esit yukseklik, nefes alan bosluk. Basliklar tasmasin. */
.depo-kpi {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(210px, 100%), 1fr));
  gap: 16px;
  margin-bottom: 16px;
}
.kpi-tikla {
  display: flex;
  width: 100%;
  border: none;
  background: transparent;
  padding: 0;
  text-align: left;
  cursor: pointer;
  border-radius: 12px;
}
.kpi-tikla :deep(.kpi-kart) {
  width: 100%;
  height: 100%;
}
.depo-kpi :deep(.kpi-baslik) {
  white-space: normal;
  overflow: visible;
  text-overflow: clip;
  line-height: 1.25;
}
.depo-kpi :deep(.kpi-deger) {
  margin-top: 2px;
  line-height: 1.15;
}
.kpi-tikla:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
@media (max-width: 640px) {
  .depo-kpi {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
  }
}
</style>
