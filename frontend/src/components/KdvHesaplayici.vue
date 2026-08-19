<template>
  <Dialog
    :visible="visible"
    header="KDV Hesaplayici"
    :modal="false"
    :style="{ width: '320px' }"
    :draggable="true"
    :closable="true"
    @update:visible="$emit('update:visible', $event)"
  >
    <div class="kdv-form">
      <div class="form-satir">
        <label>Tutar</label>
        <InputNumber
          v-model="tutar"
          :min="0"
          class="w-full"
        />
      </div>
      <div class="form-satir">
        <label>KDV Orani (%)</label>
        <Select
          v-model="oran"
          :options="oranlar"
          class="w-full"
        />
      </div>
      <div class="kdv-sonuc">
        <div class="sonuc-satir">
          <span>KDV Tutari:</span><strong>{{ format(kdvTutari) }}</strong>
        </div>
        <div class="sonuc-satir">
          <span>KDV Dahil Toplam:</span><strong>{{ format(kdvDahil) }}</strong>
        </div>
        <div class="sonuc-satir">
          <span>KDV Haric:</span><strong>{{ format(tutar || 0) }}</strong>
        </div>
      </div>
    </div>
  </Dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
defineProps({ visible: Boolean })
defineEmits(['update:visible'])

const tutar = ref(1000)
const oran = ref(20)
const oranlar = [1, 8, 10, 18, 20]

const kdvTutari = computed(() => (tutar.value || 0) * (oran.value / 100))
const kdvDahil = computed(() => (tutar.value || 0) + kdvTutari.value)
const format = (v) => new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(v)
</script>

<style scoped>
.kdv-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.form-satir label {
  display: block;
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}
.kdv-sonuc {
  background: var(--surface-ground);
  border-radius: 8px;
  padding: 16px;
}
.sonuc-satir {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid var(--surface-border);
  font-size: 14px;
}
.sonuc-satir:last-child {
  border-bottom: none;
}
</style>
