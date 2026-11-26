<template>
  <div class="cari-filtreler filtre-kart">
    <IconField class="cari-arama">
      <InputIcon class="pi pi-search" />
      <InputText
        ref="aramaGirdiRef"
        :model-value="arama"
        :placeholder="t('cariHesaplar.aramaPlaceholder')"
        @update:model-value="aramaGuncelle"
      />
    </IconField>
    <Dropdown
      :model-value="tur"
      :options="cariTurSecenekleri"
      option-label="label"
      option-value="value"
      :placeholder="t('cariHesaplar.tur')"
      class="filtre-select"
      show-clear
      @update:model-value="turGuncelle"
      @change="filtreDegistiEmit"
    />
    <Dropdown
      :model-value="bakiye"
      :options="bakiyeFiltreleri"
      option-label="label"
      option-value="value"
      :placeholder="t('cariHesaplar.bakiye')"
      class="filtre-select"
      show-clear
      @update:model-value="bakiyeGuncelle"
      @change="filtreDegistiEmit"
    />
    <AutoComplete
      :model-value="etiket"
      :suggestions="etiketOnerileri"
      :placeholder="t('cariHesaplar.etiketFiltre')"
      class="filtre-select"
      input-class="w-full"
      dropdown
      data-test="cari-etiket-filtre"
      @update:model-value="etiketGuncelle"
      @complete="etiketOnerEmit"
      @focus="etiketOnerOdak"
      @item-select="etiketAraEmit"
      @keyup.enter="etiketAraEmit"
      @blur="etiketAraEmit"
    />
    <Button
      v-if="temizleGoster"
      :label="t('cariHesaplar.temizle')"
      icon="pi pi-times"
      size="small"
      class="p-button-outlined"
      @click="temizleEmit"
    />
    <div class="cari-filtre-sag">
      <Button
        v-if="riskGorebilir"
        :label="t('cariHesaplar.riskliFiltre')"
        icon="pi pi-exclamation-triangle"
        size="small"
        class="p-button-outlined p-button-danger cari-riskli-btn"
        data-test="cari-riskli"
        @click="riskliEmit"
      />
      <TabloAyarlari
        tablo-key="cari"
        :kolonlar="kolonlar"
        @update:kolonlar="kolonGuncelleEmit"
        @update:yogunluk="yogunlukGuncelleEmit"
      />
    </div>
  </div>

  <div
    v-if="aktifFiltreler.length > 0"
    class="cari-aktif-filtreler"
    data-test="cari-aktif-filtreler"
  >
    <span class="cari-aktif-etiket">{{ t('cariHesaplar.aktifFiltreler') }}</span>
    <button
      v-for="f in aktifFiltreler"
      :key="f.tip"
      type="button"
      class="cari-filtre-cip"
      :aria-label="`${f.etiket} — ${t('cariHesaplar.filtreKaldir')}`"
      @click="cipKaldirEmit(f.tip)"
    >
      {{ f.etiket }}
      <i
        class="pi pi-times"
        aria-hidden="true"
      />
    </button>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import TabloAyarlari from '../TabloAyarlari.vue'

const props = defineProps({
  arama: { type: String, default: '' },
  tur: { type: String, default: null },
  bakiye: { type: String, default: null },
  etiket: { type: String, default: '' },
  etiketOnerileri: { type: Array, default: () => [] },
  cariTurSecenekleri: { type: Array, default: () => [] },
  bakiyeFiltreleri: { type: Array, default: () => [] },
  aktifFiltreler: { type: Array, default: () => [] },
  riskGorebilir: { type: Boolean, default: false },
  kolonlar: { type: Array, default: () => [] }
})

const emit = defineEmits([
  'update:arama',
  'update:tur',
  'update:bakiye',
  'update:etiket',
  'update:kolonlar',
  'update:yogunluk',
  'ara',
  'filtre-degisti',
  'etiket-oner',
  'etiket-ara',
  'filtreleri-temizle',
  'cip-kaldir',
  'riskli'
])

const { t } = useI18n()
const aramaGirdiRef = ref(null)

const temizleGoster = computed(() => Boolean(props.tur || props.bakiye || props.etiket))

const odakla = () => {
  const el = aramaGirdiRef.value?.$el || aramaGirdiRef.value
  el?.focus?.()
  el?.select?.()
}

const aramaGuncelle = (deger) => {
  emit('update:arama', deger)
  emit('ara')
}

const turGuncelle = (deger) => emit('update:tur', deger)
const bakiyeGuncelle = (deger) => emit('update:bakiye', deger)
const etiketGuncelle = (deger) => emit('update:etiket', deger)
const etiketOnerEmit = (event) => emit('etiket-oner', event)
const etiketOnerOdak = () => emit('etiket-oner', { query: props.etiket })
const etiketAraEmit = () => emit('etiket-ara')
const filtreDegistiEmit = () => emit('filtre-degisti')
const temizleEmit = () => emit('filtreleri-temizle')
const cipKaldirEmit = (tip) => emit('cip-kaldir', tip)
const riskliEmit = () => emit('riskli')
const kolonGuncelleEmit = (kolonlar) => emit('update:kolonlar', kolonlar)
const yogunlukGuncelleEmit = (yogunluk) => emit('update:yogunluk', yogunluk)

defineExpose({ odakla })
</script>

<style scoped>
.cari-filtreler {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
  align-items: center;
}
.cari-arama {
  flex: 1 1 240px;
  min-width: 200px;
  max-width: 360px;
}
.cari-arama :deep(.p-inputtext) {
  width: 100%;
}
.cari-filtre-sag {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
}
.filtre-select {
  width: 200px;
}
.cari-filtreler :deep(.p-autocomplete) {
  width: 200px;
}
.cari-filtreler :deep(.p-autocomplete input) {
  width: 100%;
}
.cari-filtreler :deep(.p-autocomplete-dropdown) {
  width: 2.25rem;
  border: 1px solid var(--border);
  border-left: 0;
  background: var(--bg-card);
  color: var(--text-muted);
}
.cari-filtreler :deep(.p-autocomplete-dropdown:hover) {
  background: var(--bg-muted);
  color: var(--text-primary);
}
.cari-aktif-filtreler {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin: -4px 0 12px;
}
.cari-aktif-etiket {
  font-size: 11.5px;
  color: var(--text-muted);
}
.cari-filtre-cip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 30px;
  padding: 3px 9px;
  border: 1px solid var(--accent-border);
  border-radius: 999px;
  background: var(--accent-soft);
  color: var(--accent);
  font-family: inherit;
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
  transition: background 160ms ease-out, border-color 160ms ease-out;
}
.cari-filtre-cip:hover {
  background: var(--accent-soft-strong);
}
.cari-filtre-cip i {
  font-size: 9px;
}
.cari-filtre-cip:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
@media (max-width: 768px) {
  .cari-arama {
    max-width: none;
    flex: 1 1 100%;
  }
  .cari-filtre-sag {
    margin-left: 0;
  }
}
</style>
