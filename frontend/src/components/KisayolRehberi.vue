<template>
  <Dialog
    v-model:visible="acik"
    :header="$t('common.keyboardShortcuts')"
    :modal="false"
    :style="{ width: '520px' }"
  >
    <div class="kisayol-grid">
      <div class="kisayol-baslik">
        {{ $t('shortcuts.general') }}
      </div>
      <div class="kisayol-satir">
        <kbd>Ctrl</kbd> + <kbd>K</kbd><span>{{ $t('shortcuts.search') }}</span>
      </div>
      <div class="kisayol-satir">
        <kbd>Ctrl</kbd> + <kbd>S</kbd><span>{{ $t('shortcuts.save') }}</span>
      </div>
      <div class="kisayol-satir">
        <kbd>Ctrl</kbd> + <kbd>P</kbd><span>{{ $t('shortcuts.print') }}</span>
      </div>
      <div class="kisayol-satir">
        <kbd>Esc</kbd><span>{{ $t('shortcuts.close') }}</span>
      </div>
      <div class="kisayol-satir">
        <kbd>F2</kbd><span>{{ $t('shortcuts.gotoHizliSatis') }}</span>
      </div>
      <div class="kisayol-satir">
        <kbd>F4</kbd><span>{{ $t('shortcuts.gotoStok') }}</span>
      </div>
      <div class="kisayol-satir">
        <kbd>G</kbd> + <kbd>{{ $t('shortcuts.letter') }}</kbd><span>{{ $t('shortcuts.quickNav') }}</span>
      </div>
      <div class="kisayol-satir">
        <kbd>Ctrl</kbd> + <kbd>Shift</kbd> + <kbd>H</kbd><span>{{ $t('shortcuts.presentationMode') }}</span>
      </div>
      <div class="kisayol-satir">
        <kbd>?</kbd><span>{{ $t('shortcuts.guide') }}</span>
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
