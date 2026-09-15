<template>
  <Dialog
    v-model:visible="visible"
    :header="baslik"
    :modal="true"
    style="width: 500px"
  >
    <div class="form-grup">
      <label>{{ $t('common.quantity') }} *</label>
      <InputNumber
        v-model="miktar"
        :min="0.01"
        :min-fraction-digits="1"
        class="w-full"
      />
    </div>
    <div class="form-grup">
      <label>{{ $t('common.date') }} *</label>
      <DatePicker
        v-model="hareketTarihi"
        date-format="dd.mm.yy"
        class="w-full"
      />
    </div>
    <div class="form-grup">
      <label>{{ $t('nav.cari') }}</label>
      <Dropdown
        v-model="cariHesapId"
        :options="cariHesaplar"
        option-label="ad"
        option-value="id"
        :placeholder="$t('common.optional')"
        class="w-full"
      />
    </div>
    <div class="form-grup">
      <label>{{ $t('common.description') }}</label>
      <Textarea
        v-model="aciklama"
        rows="2"
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
        :label="$t('common.save')"
        icon="pi pi-check"
        :loading="loading"
        @click="$emit('kaydet')"
      />
    </template>
  </Dialog>
</template>

<script setup>
defineProps({
  baslik: { type: String, default: '' },
  cariHesaplar: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

defineEmits(['kaydet'])

const visible = defineModel('visible', { type: Boolean, default: false })
const miktar = defineModel('miktar', { type: Number, default: null })
const hareketTarihi = defineModel('hareketTarihi', { type: [Date, String], default: null })
const cariHesapId = defineModel('cariHesapId', { type: [Number, String], default: null })
const aciklama = defineModel('aciklama', { type: String, default: '' })
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
