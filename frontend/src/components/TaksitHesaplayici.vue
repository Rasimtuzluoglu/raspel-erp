<template>
  <Dialog :visible="visible" @update:visible="$emit('update:visible', $event)"
    header="Taksit Hesaplayici" :modal="false" :style="{ width: '380px' }" :draggable="false">
    <div class="taksit-form">
      <div class="form-satir"><label>Tutar</label><InputNumber v-model="tutar" :min="0" class="w-full" /></div>
      <div class="form-satir"><label>Taksit Sayisi</label><Select v-model="taksit" :options="taksitler" class="w-full" /></div>
      <div class="form-satir"><label>Faiz Orani (% aylik)</label><InputNumber v-model="faiz" :min="0" :max="100" class="w-full" /></div>
      <DataTable :value="plan" size="small" class="mt-3">
        <Column field="ay" header="Ay" style="width:50px" />
        <Column field="taksit" header="Taksit"><template #body="s">{{ format(s.data.taksit) }}</template></Column>
        <Column field="kalan" header="Kalan"><template #body="s">{{ format(s.data.kalan) }}</template></Column>
      </DataTable>
      <div class="taksit-ozet">Toplam Odeme: <strong>{{ format(toplam) }}</strong></div>
    </div>
  </Dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
defineProps({ visible: Boolean })
defineEmits(['update:visible'])

const tutar = ref(10000)
const taksit = ref(6)
const faiz = ref(2)
const taksitler = [2,3,4,5,6,9,12,18,24]

const plan = computed(() => {
  const t = tutar.value || 0; const n = taksit.value; const r = (faiz.value || 0) / 100
  const aylik = r > 0 ? t * r / (1 - Math.pow(1 + r, -n)) : t / n
  const p = []; let kalan = t
  for (let i = 1; i <= n; i++) { kalan -= (aylik - kalan * r); p.push({ ay: i, taksit: aylik, kalan: Math.max(0, kalan) }) }
  return p
})
const toplam = computed(() => plan.value.reduce((s, p) => s + p.taksit, 0))
const format = (v) => new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(v)
</script>

<style scoped>
.taksit-form { display: flex; flex-direction: column; gap: 10px; }
.form-satir label { display: block; font-size: 13px; color: var(--text-secondary); margin-bottom: 4px; }
.taksit-ozet { text-align: right; font-size: 16px; padding: 8px; background: var(--surface-ground); border-radius: 8px; }
</style>
