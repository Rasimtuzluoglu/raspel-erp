<template>
  <div class="tablo-ayarlari">
    <Button
      icon="pi pi-filter"
      class="p-button-sm p-button-text"
      :badge="gizliKolonSayisi"
      badge-class="p-badge-warning"
      title="Sütun Ayarları"
      @click="panelAcik = !panelAcik"
    />
    <transition name="panel">
      <div
        v-if="panelAcik"
        class="ayar-panel"
      >
        <div class="ayar-baslik">
          Sütunlar
        </div>
        <label
          v-for="(k, i) in localKolonlar"
          :key="i"
          class="ayar-satir"
        >
          <Checkbox
            :model-value="k.visible"
            :binary="true"
            @update:model-value="degistir(i, $event)"
          />
          <span>{{ k.header }}</span>
        </label>
        <div class="ayar-ayrac" />
        <div class="ayar-baslik">
          Yoğunluk
        </div>
        <div class="ayar-yogunluk">
          <Button
            :label="'Kompakt'"
            :class="{ 'p-button-sm': true, 'p-button-outlined': yogunluk !== 'compact' }"
            size="small"
            @click="yogunlukSec('compact')"
          />
          <Button
            :label="'Rahat'"
            :class="{ 'p-button-sm': true, 'p-button-outlined': yogunluk !== 'comfortable' }"
            size="small"
            @click="yogunlukSec('comfortable')"
          />
        </div>
        <div class="ayar-ayrac" />
        <Button
          label="Hepsini Göster"
          icon="pi pi-eye"
          size="small"
          class="p-button-sm w-full"
          @click="hepsiniGoster"
        />
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  tabloKey: { type: String, required: true },
  kolonlar: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:kolonlar', 'update:yogunluk'])

const panelAcik = ref(false)
const yogunluk = ref('comfortable')

const localKolonlar = ref(props.kolonlar.map(k => ({ ...k })))

watch(() => props.kolonlar, (yeni) => {
  localKolonlar.value = yeni.map(k => ({ ...k }))
})

const gizliKolonSayisi = computed(() => localKolonlar.value.filter(k => k.visible === false).length)

const localStorageAnahtari = () => `raspel_tablo_${props.tabloKey}`

watch(localKolonlar, () => {
  localStorage.setItem(localStorageAnahtari(), JSON.stringify({
    kolonlar: localKolonlar.value.map(k => ({ field: k.field, visible: k.visible !== false })),
    yogunluk: yogunluk.value
  }))
}, { deep: true })

const init = () => {
  try {
    const kayitli = JSON.parse(localStorage.getItem(localStorageAnahtari()))
    if (kayitli?.kolonlar) {
      const yeniKolonlar = localKolonlar.value.map(k => ({ ...k }))
      kayitli.kolonlar.forEach(k => {
        const kolon = yeniKolonlar.find(c => c.field === k.field)
        if (kolon) kolon.visible = k.visible
      })
      localKolonlar.value = yeniKolonlar
      emit('update:kolonlar', yeniKolonlar)
    }
    if (kayitli?.yogunluk) yogunluk.value = kayitli.yogunluk
  } catch { /* empty */ }
  emit('update:yogunluk', yogunluk.value)
}

const degistir = (idx, val) => {
  const yeniKolonlar = localKolonlar.value.map(k => ({ ...k }))
  yeniKolonlar[idx].visible = val
  localKolonlar.value = yeniKolonlar
  emit('update:kolonlar', yeniKolonlar)
}

const yogunlukSec = (y) => {
  yogunluk.value = y
  emit('update:yogunluk', y)
}

const hepsiniGoster = () => {
  const yeniKolonlar = localKolonlar.value.map(k => ({ ...k, visible: true }))
  localKolonlar.value = yeniKolonlar
  emit('update:kolonlar', yeniKolonlar)
}

init()
</script>

<style scoped>
.tablo-ayarlari { position: relative; }
.ayar-panel {
  position: absolute; top: 36px; right: 0; z-index: 100;
  background: var(--bg-card); border: 1px solid var(--border);
  border-radius: 10px; box-shadow: var(--shadow);
  padding: 12px; min-width: 210px;
}
.ayar-baslik { font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; color: var(--text-muted); margin: 6px 0; }
.ayar-satir { display: flex; align-items: center; gap: 8px; padding: 4px 0; font-size: 13px; cursor: pointer; }
.ayar-ayrac { border-top: 1px solid var(--border); margin: 8px 0; }
.ayar-yogunluk { display: flex; gap: 6px; }
.w-full { width: 100% !important; }
.panel-enter-active, .panel-leave-active { transition: all 0.15s ease; }
.panel-enter-from, .panel-leave-to { opacity: 0; transform: translateY(-6px); }
</style>
