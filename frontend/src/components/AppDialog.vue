<template>
  <Dialog
    :visible="visible"
    :header="header"
    :modal="modal"
    :closable="closable"
    class="app-dialog"
    :style="{ width: width, maxWidth: maxWidth, '--app-dialog-ch': contentMaxHeight }"
    @update:visible="emit('update:visible', $event)"
  >
    <div class="app-dialog-icerik">
      <slot />
    </div>
    <template
      v-if="$slots.footer"
      #footer
    >
      <slot name="footer" />
    </template>
  </Dialog>
</template>

<script setup>
defineProps({
  visible: { type: Boolean, default: false },
  header: { type: String, default: '' },
  /** Genişlik (px değeri veya CSS değeri). */
  width: { type: String, default: '860px' },
  maxWidth: { type: String, default: '96vw' },
  /** İçerik alanı üst yüksekliği (scroll bu alanda). */
  contentMaxHeight: { type: String, default: 'min(70vh, 660px)' },
  modal: { type: Boolean, default: true },
  closable: { type: Boolean, default: true }
})
const emit = defineEmits(['update:visible'])
</script>

<style scoped>
.app-dialog :deep(.p-dialog-content) {
  max-height: var(--app-dialog-ch, min(70vh, 660px));
}
.app-dialog-icerik {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.app-dialog :deep(.p-dialog-footer) {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.app-dialog :deep(.p-dialog-footer) button {
  min-width: 120px;
}
</style>
