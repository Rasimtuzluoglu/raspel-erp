<template>
  <Dialog
    v-model:visible="visible"
    :header="$t('cmp.topluFiyatHeader')"
    :modal="true"
    style="width: 480px"
  >
    <div class="form-grup">
      <label>{{ $t('stoklar.islemYonu') }}</label>
      <Dropdown
        v-model="yon"
        :options="['ARTIR', 'AZALT']"
        class="w-full"
      />
    </div>
    <div class="form-grup">
      <label>{{ $t('stoklar.oranYuzde') }}</label>
      <InputNumber
        v-model="oran"
        :min="0"
        :max="100"
        :min-fraction-digits="1"
        class="w-full"
      />
    </div>
    <div class="form-grup">
      <label>{{ $t('stoklar.kategoriFiltreOps') }}</label>
      <InputText
        v-model="kategori"
        :placeholder="$t('cmp.tumKategoriler')"
        class="w-full"
      />
    </div>
    <div class="form-grup">
      <label>{{ $t('stoklar.stokGrubuFiltreOps') }}</label>
      <InputText
        v-model="stokGrubu"
        :placeholder="$t('cmp.tumGruplar')"
        class="w-full"
      />
    </div>
    <template #footer>
      <Button
        :label="$t('common.cancel')"
        icon="pi pi-times"
        class="p-button-text"
        @click="visible = false"
      />
      <Button
        :label="$t('common.apply')"
        icon="pi pi-check"
        :loading="loading"
        @click="$emit('uygula')"
      />
    </template>
  </Dialog>
</template>

<script setup>
defineProps({
  loading: { type: Boolean, default: false }
})

defineEmits(['uygula'])

const visible = defineModel('visible', { type: Boolean, default: false })
const yon = defineModel('yon', { type: String, default: 'ARTIR' })
const oran = defineModel('oran', { type: Number, default: 0 })
const kategori = defineModel('kategori', { type: String, default: '' })
const stokGrubu = defineModel('stokGrubu', { type: String, default: '' })
</script>

<style scoped>
.form-grup {
  margin-bottom: 18px;
}
.form-grup label {
  display: block;
  margin-bottom: 6px;
  font-weight: 600;
  color: var(--text-secondary);
  font-size: 12px;
}
</style>
