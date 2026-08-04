<template>
  <div class="tarih-hizli">
    <Button
      v-for="p in presetler"
      :key="p.id"
      :label="p.label"
      size="small"
      class="p-button-sm"
      :class="{ 'p-button-outlined': secili !== p.id }"
      @click="sec(p)"
    />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({ modelValue: { type: [Array, Object], default: () => [] } })
const emit = defineEmits(['update:modelValue'])

const secili = ref(null)

const presetler = [
  { id: 'bugun', label: 'Bugün', hesapla: () => { const b = new Date(); return [b, b] } },
  { id: 'hafta', label: 'Bu Hafta', hesapla: () => { const b = new Date(); const gun = b.getDay() || 7; const bas = new Date(b); bas.setDate(b.getDate() - gun + 1); return [bas, b] } },
  { id: 'ay', label: 'Bu Ay', hesapla: () => { const b = new Date(); const bas = new Date(b.getFullYear(), b.getMonth(), 1); return [bas, b] } },
  { id: '3ay', label: 'Son 3 Ay', hesapla: () => { const b = new Date(); const bas = new Date(b); bas.setMonth(b.getMonth() - 3); return [bas, b] } },
  { id: 'yil', label: 'Bu Yıl', hesapla: () => { const b = new Date(); const bas = new Date(b.getFullYear(), 0, 1); return [bas, b] } }
]

const sec = (p) => {
  secili.value = secili.value === p.id ? null : p.id
  if (secili.value) {
    emit('update:modelValue', p.hesapla())
  } else {
    emit('update:modelValue', [])
  }
}

watch(() => props.modelValue, (v) => {
  if (!v || v.length !== 2 || !v[0]) secili.value = null
})
</script>

<style scoped>
.tarih-hizli { display: flex; flex-wrap: wrap; gap: 4px; }
</style>
