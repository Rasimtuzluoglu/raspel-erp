<template>
  <Card class="settings-card">
    <template #title>
      <div class="sc-baslik">
        <span
          v-if="ikon"
          class="sc-ikon"
        ><i :class="ikon" /></span>
        <div class="sc-metin">
          <span class="sc-title">{{ baslik }}</span>
          <small
            v-if="aciklama"
            class="sc-aciklama"
          >{{ aciklama }}</small>
        </div>
        <div
          v-if="$slots.aksiyon"
          class="sc-aksiyon"
        >
          <slot name="aksiyon" />
        </div>
      </div>
    </template>
    <template #content>
      <slot />
    </template>
  </Card>
</template>

<script setup>
defineProps({
  baslik: { type: String, default: '' },
  aciklama: { type: String, default: '' },
  ikon: { type: String, default: '' }
})
</script>

<style scoped>
.settings-card {
  height: 100%;
}
.settings-card :deep(.p-card-body) {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.settings-card :deep(.p-card-content) {
  flex: 1 1 auto;
}
.sc-baslik {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}
.sc-ikon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 16px;
  flex-shrink: 0;
}
.sc-metin {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.sc-title {
  font-weight: 700;
  color: var(--text-primary);
}
.sc-aciklama {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 400;
}
.sc-aksiyon {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>
