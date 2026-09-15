<template>
  <Dialog
    v-model:visible="acik"
    header="Klavye Kısayolları"
    :modal="false"
    :style="{ width: '520px' }"
  >
    <div class="kisayol-grid">
      <div class="kisayol-baslik">
        Genel
      </div>
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
        <kbd>F2</kbd><span>Hızlı Satış'a git</span>
      </div>
      <div class="kisayol-satir">
        <kbd>F4</kbd><span>Stoklar'a git</span>
      </div>
      <div class="kisayol-satir">
        <kbd>G</kbd> + harf<span>Hızlı gezinme (g+c cari, g+f fatura, g+h POS)</span>
      </div>
      <div class="kisayol-satir">
        <kbd>Ctrl</kbd> + <kbd>Shift</kbd> + <kbd>H</kbd><span>Sunum (Müşteri) modu</span>
      </div>
      <div class="kisayol-satir">
        <kbd>?</kbd><span>Bu rehberi aç</span>
      </div>

      <div class="kisayol-baslik">
        Hızlı Satış (POS) sayfasında
      </div>
      <div class="kisayol-satir">
        <kbd>F1</kbd><span>Barkod alanına odaklan</span>
      </div>
      <div class="kisayol-satir">
        <kbd>F2</kbd><span>Sepeti temizle</span>
      </div>
      <div class="kisayol-satir">
        <kbd>F3</kbd><span>Ürün arama alanına odaklan</span>
      </div>
      <div class="kisayol-satir">
        <kbd>F4</kbd><span>Müşteri seçimi</span>
      </div>
      <div class="kisayol-satir">
        <kbd>F5</kbd><span>Yeni müşteri ekle</span>
      </div>
      <div class="kisayol-satir">
        <kbd>F6</kbd><span>Kamera tarayıcıyı aç</span>
      </div>
      <div class="kisayol-satir">
        <kbd>F9</kbd><span>Tam ödeme + satışı tamamla</span>
      </div>
      <div class="kisayol-satir">
        <kbd>F10</kbd><span>Kısmi ödeme + satışı tamamla</span>
      </div>
      <div class="kisayol-satir">
        <kbd>N</kbd>/<kbd>K</kbd>/<kbd>H</kbd><span>Ödeme yöntemi: Nakit / Kart / Havale</span>
      </div>
      <div class="kisayol-satir">
        <kbd>Alt</kbd> + <kbd>↑</kbd>/<kbd>↓</kbd><span>Aktif satır miktarını artır/azalt</span>
      </div>
      <div class="kisayol-satir">
        <kbd>Del</kbd><span>Aktif satırı sil</span>
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
.kisayol-baslik {
  margin-top: 10px;
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: var(--text-secondary);
}
.kisayol-baslik:first-child {
  margin-top: 0;
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
