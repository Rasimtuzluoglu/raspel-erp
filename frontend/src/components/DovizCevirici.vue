<template>
  <Dialog :visible="visible" @update:visible="$emit('update:visible', $event)"
    header="Doviz Cevirici" :modal="false" :style="{ width: '340px' }" :draggable="true" :closable="true">
    <div class="cevirici">
      <div class="cevirici-satir">
        <InputNumber v-model="tutar" placeholder="Tutar" :min="0" class="tutar-input" />
        <Select v-model="kaynak" :options="kurlar" option-label="label" option-value="kod" class="kur-select" />
      </div>
      <div class="cevirici-ok"><i class="pi pi-arrow-down" /></div>
      <div class="cevirici-satir">
        <div class="sonuc">{{ sonuc }}</div>
        <Select v-model="hedef" :options="kurlar" option-label="label" option-value="kod" class="kur-select" />
      </div>
      <Button label="Cevir" icon="pi pi-sync" class="w-full mt-3" @click="cevir" :loading="yukleniyor" />
    </div>
  </Dialog>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { dovizAPI } from '../api/index.js'

defineProps({ visible: Boolean })
defineEmits(['update:visible'])

const tutar = ref(1)
const kaynak = ref('TRY')
const hedef = ref('USD')
const sonuc = ref('...')
const yukleniyor = ref(false)
const kurlar = ref([{ label: 'TRY - Turk Lirasi', kod: 'TRY' }])

onMounted(async () => {
  try {
    const r = await dovizAPI.getKurlar()
    const data = r.data || []
    kurlar.value = [{ label: 'TRY - Turk Lirasi', kod: 'TRY' }, ...data.map(k => ({
      label: `${k.dovizKodu || k.kod} - ${k.dovizAdi || ''}`, kod: k.dovizKodu || k.kod
    }))]
  } catch { /* ignore */ }
})

const cevir = async () => {
  if (!tutar.value || kaynak.value === hedef.value) { sonuc.value = String(tutar.value || 0); return }
  yukleniyor.value = true
  try {
    const r = await dovizAPI.cevir(tutar.value, kaynak.value, hedef.value)
    sonuc.value = r.data?.sonuc ? String(Number(r.data.sonuc).toFixed(2)) : 'Hata'
  } catch { sonuc.value = 'Hata' }
  finally { yukleniyor.value = false }
}
</script>

<style scoped>
.cevirici { display: flex; flex-direction: column; gap: 12px; }
.cevirici-satir { display: flex; gap: 8px; align-items: center; }
.tutar-input { flex: 1; }
.kur-select { width: 130px; }
.cevirici-ok { text-align: center; color: var(--primary-color); font-size: 20px; }
.sonuc { flex: 1; background: var(--surface-ground); border-radius: 8px; padding: 12px; text-align: right; font-size: 20px; font-weight: 700; font-family: monospace; }
</style>
