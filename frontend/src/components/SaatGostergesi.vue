<template>
  <span class="saat-gostergesi"><i class="pi pi-calendar" /> {{ saat }}</span>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const saat = ref('')
let interval = null

const guncelle = () => {
  const now = new Date()
  saat.value = now.toLocaleDateString('tr-TR', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' }) + ' ' + now.toLocaleTimeString('tr-TR')
}

onMounted(() => {
  guncelle()
  interval = setInterval(guncelle, 1000)
})

onUnmounted(() => {
  if (interval) clearInterval(interval)
})
</script>
