<template>
  <Dialog
    :visible="visible"
    header="KDV Hesaplayıcı"
    :modal="false"
    :style="{ width: '340px' }"
    :draggable="true"
    :closable="true"
    @update:visible="$emit('update:visible', $event)"
  >
    <div class="kdv-form">
      <div class="yon-secim">
        <button
          class="yon-btn"
          :class="{ aktif: yon === 'haric' }"
          @click="yon = 'haric'"
        >
          KDV Hariçten
        </button>
        <button
          class="yon-btn"
          :class="{ aktif: yon === 'dahil' }"
          @click="yon = 'dahil'"
        >
          KDV Dahilden
        </button>
      </div>

      <div class="form-satir">
        <label>{{ yon === 'haric' ? 'KDV Hariç Tutar' : 'KDV Dahil Tutar' }}</label>
        <InputNumber
          v-model="tutar"
          :min="0"
          class="w-full"
        />
      </div>

      <div class="form-satir">
        <label>KDV Oranı (%)</label>
        <Select
          v-model="oran"
          :options="oranlar"
          class="w-full"
        />
      </div>

      <div class="kdv-sonuc">
        <div class="sonuc-satir ana">
          <span>{{ yon === 'haric' ? 'KDV Dahil Toplam' : 'KDV Hariç Tutar' }}</span>
          <strong>{{ format(anaSonuc) }}</strong>
        </div>
        <div class="sonuc-satir">
          <span>KDV Tutarı</span>
          <strong>{{ format(kdvTutari) }}</strong>
        </div>
        <div class="sonuc-satir">
          <span>{{ yon === 'haric' ? 'KDV Hariç' : 'KDV Dahil' }}</span>
          <strong>{{ format(tutar || 0) }}</strong>
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
const yon = ref('haric')
const oranlar = [1, 8, 10, 18, 20]

const kdvTutari = computed(() => {
  const t = tutar.value || 0
  const r = oran.value / 100
  return yon.value === 'haric' ? t * r : t - t / (1 + r)
})

const anaSonuc = computed(() => {
  const t = tutar.value || 0
  return yon.value === 'haric' ? t + kdvTutari.value : t - kdvTutari.value
})

const format = (v) => new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(v)
</script>

<style scoped>
.kdv-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.yon-secim {
  display: flex;
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 3px;
  gap: 3px;
}
.yon-btn {
  flex: 1;
  padding: 8px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}
.yon-btn.aktif {
  background: var(--accent);
  color: #fff;
}
.form-satir label {
  display: block;
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}
.kdv-sonuc {
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 14px 16px;
}
.sonuc-satir {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid var(--border);
  font-size: 14px;
  color: var(--text-secondary);
}
.sonuc-satir:last-child {
  border-bottom: none;
}
.sonuc-satir strong {
  color: var(--text-primary);
  font-family: monospace;
}
.sonuc-satir.ana strong {
  color: var(--accent);
  font-size: 16px;
}
</style>
