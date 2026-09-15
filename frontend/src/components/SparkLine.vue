<template>
  <div
    class="sparkline"
    :style="{ height: `${yukseklik}px` }"
  >
    <Line
      :data="veri"
      :options="secenekler"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Line } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Filler
} from 'chart.js'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Filler)

const props = defineProps({
  veriler: { type: Array, default: () => [] },
  renk: { type: String, default: '#3b82f6' },
  yukseklik: { type: Number, default: 36 }
})

const veri = computed(() => ({
  labels: props.veriler.map((_, i) => i),
  datasets: [
    {
      data: props.veriler,
      borderColor: props.renk,
      backgroundColor: `${props.renk}26`,
      borderWidth: 2,
      fill: true,
      tension: 0.4,
      pointRadius: 0,
      pointHoverRadius: 0
    }
  ]
}))

const secenekler = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { display: false }, tooltip: { enabled: false } },
  scales: { x: { display: false }, y: { display: false } },
  animation: { duration: 400 },
  elements: { line: { borderCapStyle: 'round' } }
}
</script>

<style scoped>
.sparkline {
  width: 100%;
  min-width: 0;
}
</style>
