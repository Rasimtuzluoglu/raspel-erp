<template>
  <Dialog :visible="visible" @update:visible="$emit('update:visible', $event)"
    header="Kar Marji Hesaplayici" :modal="false" :style="{ width: '320px' }" :draggable="false">
    <div class="marj-form">
      <div class="form-satir"><label>Alis Fiyati</label><InputNumber v-model="alis" :min="0" class="w-full" /></div>
      <div class="form-satir"><label>Satis Fiyati</label><InputNumber v-model="satis" :min="0" class="w-full" /></div>
      <div class="form-satir"><label>veya Kar Marj (%)</label>
        <InputNumber v-model="marj" :min="0" :max="1000" class="w-full" @update:model-value="marjdanHesapla" />
      </div>
      <div class="marj-sonuc">
        <div class="sonuc-satir"><span>Kar Tutari:</span><strong :class="kar >= 0 ? 'positive' : 'negative'">{{ format(kar) }}</strong></div>
        <div class="sonuc-satir"><span>Kar Marj %:</span><strong>{{ yuzde.toFixed(2) }}%</strong></div>
        <div class="sonuc-satir"><span>Satis (marjli):</span><strong>{{ format(satis || 0) }}</strong></div>
      </div>
    </div>
  </Dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
defineProps({ visible: Boolean })
defineEmits(['update:visible'])

const alis = ref(100)
const satis = ref(150)
const marj = ref(50)

const kar = computed(() => (satis.value || 0) - (alis.value || 0))
const yuzde = computed(() => alis.value ? (kar.value / alis.value) * 100 : 0)

const marjdanHesapla = () => { satis.value = (alis.value || 0) * (1 + (marj.value || 0) / 100) }
const format = (v) => new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(v)
</script>

<style scoped>
.marj-form { display: flex; flex-direction: column; gap: 10px; }
.form-satir label { display: block; font-size: 13px; color: var(--text-secondary); margin-bottom: 4px; }
.marj-sonuc { background: var(--surface-ground); border-radius: 8px; padding: 16px; }
.sonuc-satir { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid var(--surface-border); font-size: 14px; }
.sonuc-satir:last-child { border-bottom: none; }
.positive { color: var(--green-500); }
.negative { color: var(--red-500); }
</style>
