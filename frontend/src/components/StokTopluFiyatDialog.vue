<template>
  <Dialog
    v-model:visible="visible"
    header="Toplu Fiyat Güncelleme"
    :modal="true"
    style="width: 480px"
  >
    <div class="form-grup">
      <label>İşlem Yönü</label>
      <Dropdown
        v-model="yon"
        :options="['ARTIR', 'AZALT']"
        class="w-full"
      />
    </div>
    <div class="form-grup">
      <label>Oran (%)</label>
      <InputNumber
        v-model="oran"
        :min="0"
        :max="100"
        :min-fraction-digits="1"
        class="w-full"
      />
    </div>
    <div class="form-grup">
      <label>Kategori Filtre (opsiyonel)</label>
      <InputText
        v-model="kategori"
        placeholder="Tüm kategoriler"
        class="w-full"
      />
    </div>
    <div class="form-grup">
      <label>Stok Grubu Filtre (opsiyonel)</label>
      <InputText
        v-model="stokGrubu"
        placeholder="Tüm gruplar"
        class="w-full"
      />
    </div>
    <template #footer>
      <Button
        label="İptal"
        icon="pi pi-times"
        class="p-button-text"
        @click="visible = false"
      />
      <Button
        label="Uygula"
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
