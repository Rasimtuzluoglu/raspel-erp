<template>
  <Dialog
    v-model:visible="acik"
    header="Klavye Kısayolları"
    :modal="false"
    :style="{ width: '520px' }"
  >
    <div class="kisayol-grid">
      <div class="kisayol-satir">
        <kbd>Ctrl</kbd> + <kbd>K</kbd><span>Hızlı Arama</span>
      </div>
      <div class="kisayol-satir">
        <kbd>Ctrl</kbd> + <kbd>S</kbd><span>Kaydet</span>
      </div>
      <div class="kisayol-satir">
        <kbd>Ctrl</kbd> + <kbd>P</kbd><span>Yazdır</span>
      </div>
      <div class="kisayol-satir">
        <kbd>Esc</kbd><span>Kapat / İptal</span>
      </div>
      <div class="kisayol-satir">
        <kbd>F2</kbd><span>Hızlı Satış</span>
      </div>
      <div class="kisayol-satir">
        <kbd>F4</kbd><span>Stoklar</span>
      </div>
      <div class="kisayol-satir">
        <kbd>F9</kbd><span>POS'ta Satışı Tamamla</span>
      </div>
      <div class="kisayol-satir">
        <kbd>G</kbd> + harf<span>Hızlı gezinme (g+c cari, g+f fatura, g+h POS)</span>
      </div>
      <div class="kisayol-satir">
        <kbd>?</kbd><span>Bu rehberi aç</span>
      </div>
    </div>
  </Dialog>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const acik = ref(false)

const acListener = () => {
  acik.value = true
}

onMounted(() => window.addEventListener('kisayol-rehberi-ac', acListener))
onUnmounted(() => window.removeEventListener('kisayol-rehberi-ac', acListener))
</script>

<style scoped>
.kisayol-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.kisayol-satir {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}
.kisayol-satir span {
  margin-left: auto;
  color: var(--text-muted);
}
kbd {
  background: var(--bg-primary);
  border: 1px solid var(--border);
  border-bottom-width: 2px;
  border-radius: 5px;
  padding: 2px 7px;
  font-family: monospace;
  font-size: 12px;
  color: var(--text-primary);
}
</style>
