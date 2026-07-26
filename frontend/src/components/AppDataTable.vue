<template>
  <DataTable v-bind="$attrs" :loading="loading" paginator :rows="rows" :rowsPerPageOptions="[10,25,50,100]">
    <template #empty>
      <EmptyState v-if="!loading" :message="emptyMessage" />
    </template>
    <slot />
  </DataTable>
</template>

<script setup>
import EmptyState from './EmptyState.vue'

defineProps({
  loading: { type: Boolean, default: false },
  emptyMessage: { type: String, default: 'Kayıt bulunamadı' },
  rows: { type: Number, default: 25 }
})
</script>
