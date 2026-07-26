<template>
  <div class="app-datatable-wrapper">
    <DataTable
      v-bind="$attrs"
      :loading="loading"
      :paginator="paginator"
      :rows="rows"
      :lazy="lazy"
      :totalRecords="totalRecords"
      :rowsPerPageOptions="rowsPerPageOptions"
      responsiveLayout="scroll"
      @page="$emit('page', $event)"
      @sort="$emit('sort', $event)"
    >
      <template #empty>
        <EmptyState v-if="!loading" :message="emptyMessage" />
      </template>
      <slot />
    </DataTable>
  </div>
</template>

<script setup>
import EmptyState from './EmptyState.vue'

defineProps({
  loading: { type: Boolean, default: false },
  paginator: { type: Boolean, default: true },
  emptyMessage: { type: String, default: 'Kayıt bulunamadı' },
  rows: { type: Number, default: 25 },
  lazy: { type: Boolean, default: false },
  totalRecords: { type: Number, default: 0 },
  rowsPerPageOptions: { type: Array, default: () => [10, 25, 50, 100] }
})

defineEmits(['page', 'sort'])
</script>

<style scoped>
.app-datatable-wrapper {
  width: 100%;
  overflow-x: auto;
}
</style>
