<template>
  <div
    v-if="!yukleniyor"
    class="cari-kpi"
  >
    <button
      type="button"
      class="cari-kpi-tikla"
      data-test="kpi-toplam"
      :title="t('cariHesaplar.toplamCari')"
      @click="emit('temizle')"
    >
      <KpiKart
        :baslik="t('cariHesaplar.toplamCari')"
        :deger="toplamDeger"
        :para-birimi="false"
        ikon="pi pi-users"
        renk="#14b8a6"
      />
    </button>
    <button
      type="button"
      class="cari-kpi-tikla"
      :class="{ 'cari-kpi-tikla--aktif': filtreBakiye === 'alacak' }"
      data-test="kpi-alacak"
      :aria-pressed="filtreBakiye === 'alacak' ? 'true' : 'false'"
      :title="t('cariHesaplar.bizeAlacakli')"
      @click="bakiyeSec('alacak')"
    >
      <KpiKart
        :baslik="t('cariHesaplar.bizeAlacakli')"
        :deger="cariOzet.alacakli ?? 0"
        ikon="pi pi-arrow-down-left"
        renk="#22c55e"
      />
    </button>
    <button
      type="button"
      class="cari-kpi-tikla"
      :class="{ 'cari-kpi-tikla--aktif': filtreBakiye === 'borc' }"
      data-test="kpi-borc"
      :aria-pressed="filtreBakiye === 'borc' ? 'true' : 'false'"
      :title="t('cariHesaplar.bizeBorclu')"
      @click="bakiyeSec('borc')"
    >
      <KpiKart
        :baslik="t('cariHesaplar.bizeBorclu')"
        :deger="cariOzet.borclu ?? 0"
        ikon="pi pi-arrow-up-right"
        renk="#ef4444"
      />
    </button>
  </div>

  <p
    v-if="!yukleniyor && aktifFiltreler.length > 0"
    class="cari-kpi-not"
  >
    {{ t('cariHesaplar.kpiTumCariNotu') }}
  </p>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import KpiKart from '../KpiKart.vue'

const props = defineProps({
  cariOzet: { type: Object, default: () => ({}) },
  toplamKayit: { type: [Number, String], default: 0 },
  filtreBakiye: { type: String, default: null },
  yukleniyor: { type: Boolean, default: false },
  aktifFiltreler: { type: Array, default: () => [] }
})

const emit = defineEmits(['temizle', 'bakiye-filtre'])
const { t } = useI18n()

const toplamDeger = computed(() => props.cariOzet.toplamKayit ?? props.toplamKayit)
const bakiyeSec = (yon) => emit('bakiye-filtre', yon)
</script>

<style scoped>
.cari-kpi {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.cari-kpi-tikla {
  display: block;
  width: 100%;
  min-width: 0;
  padding: 0;
  border: 0;
  border-radius: var(--radius-lg, 14px);
  background: transparent;
  font: inherit;
  text-align: left;
  cursor: pointer;
}
.cari-kpi-tikla:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.cari-kpi-tikla--aktif {
  box-shadow: 0 0 0 2px var(--accent);
  border-radius: var(--radius-lg, 14px);
}
.cari-kpi-not {
  margin: -8px 0 10px;
  font-size: 11px;
  color: var(--text-muted);
}
</style>
