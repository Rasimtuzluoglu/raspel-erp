<template>
  <Dialog
    :visible="visible"
    header="Döviz Çevirici"
    :modal="false"
    :style="{ width: '360px' }"
    :draggable="true"
    :closable="true"
    @update:visible="$emit('update:visible', $event)"
  >
    <div class="cevirici">
      <div class="satir">
        <InputNumber
          v-model="tutar"
          placeholder="Tutar"
          :min="0"
          class="tutar-input"
          :input-id="'doviz-tutar'"
        />
        <Select
          v-model="kaynak"
          :options="kurlar"
          option-label="label"
          option-value="kod"
          class="kur-select"
        />
      </div>

      <div class="takas-satir">
        <div class="takas-cizgi" />
        <Button
          icon="pi pi-arrow-up-arrow-down"
          class="p-button-rounded p-button-text p-button-sm takas-btn"
          title="Para birimlerini değiştir"
          @click="takas"
        />
      </div>

      <div class="satir">
        <div class="sonuc">
          {{ sonuc }}
        </div>
        <Select
          v-model="hedef"
          :options="kurlar"
          option-label="label"
          option-value="kod"
          class="kur-select"
        />
      </div>

      <div
        v-if="kurBilgisi"
        class="kur-bilgisi"
      >
        <i class="pi pi-info-circle" />
        <span>1 {{ hedef }} = {{ kurBilgisi }}</span>
        <span
          v-if="kurTarihi"
          class="kur-tarih"
        >• {{ kurTarihi }}</span>
      </div>
    </div>
  </Dialog>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { dovizAPI } from '../api/index.js'

defineProps({ visible: Boolean })
defineEmits(['update:visible'])

const tutar = ref(1)
const kaynak = ref('TRY')
const hedef = ref('USD')
const sonuc = ref('0')
const kurlar = ref([{ label: 'TRY - Türk Lirası', kod: 'TRY' }])
const kurMap = ref({})
const kurTarihi = ref('')

onMounted(async () => {
  try {
    const r = await dovizAPI.getKurlar()
    const data = r.data || []
    const map = { TRY: 1 }
    const liste = [{ label: 'TRY - Türk Lirası', kod: 'TRY' }]
    data.forEach((k) => {
      const kod = k.dovizKodu || k.kod
      const kur = Number(k.satisKuru) || 0
      if (!kod) return
      map[kod] = kur
      liste.push({ label: `${kod} - ${k.dovizAdi || ''}`, kod })
      if (k.tarih) kurTarihi.value = String(k.tarih)
    })
    kurMap.value = map
    kurlar.value = liste
    cevir()
  } catch {
    /* ignore */
  }
})

const cevir = () => {
  const t = Number(tutar.value) || 0
  if (!t) {
    sonuc.value = '0'
    return
  }
  if (kaynak.value === hedef.value) {
    sonuc.value = formatla(t)
    return
  }
  const kaynakKur = kurMap.value[kaynak.value]
  const hedefKur = kurMap.value[hedef.value]
  if (!kaynakKur || !hedefKur) {
    sonuc.value = '—'
    return
  }
  const sonucVal = (t * kaynakKur) / hedefKur
  sonuc.value = formatla(sonucVal)
}

const formatla = (v) => new Intl.NumberFormat('tr-TR', { maximumFractionDigits: 2 }).format(v)

const takas = () => {
  const gecici = kaynak.value
  kaynak.value = hedef.value
  hedef.value = gecici
}

const kurBilgisi = computed(() => {
  if (hedef.value === 'TRY' || !kurMap.value[hedef.value]) return ''
  return Number(kurMap.value[hedef.value]).toFixed(4) + ' ₺'
})

watch([tutar, kaynak, hedef], cevir)
</script>

<style scoped>
.cevirici {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.satir {
  display: flex;
  gap: 8px;
  align-items: center;
}
.tutar-input {
  flex: 1;
}
.kur-select {
  width: 150px;
}
.takas-satir {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 2px 0;
}
.takas-cizgi {
  flex: 1;
  height: 1px;
  background: var(--border);
}
.takas-btn {
  color: var(--accent);
}
.takas-btn:hover {
  background: rgba(59, 130, 246, 0.12);
}
.sonuc {
  flex: 1;
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 14px 16px;
  text-align: right;
  font-size: 24px;
  font-weight: 700;
  font-family: monospace;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.kur-bilgisi {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-muted);
  padding: 6px 2px 0;
}
.kur-bilgisi i {
  color: var(--accent);
}
.kur-tarih {
  opacity: 0.7;
}
</style>
