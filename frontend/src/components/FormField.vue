<template>
  <div
    class="form-field"
    :class="{ 'has-error': error }"
  >
    <label
      v-if="label"
      :for="inputId"
      class="form-label"
    >
      {{ label }} <span
        v-if="required"
        class="required-star"
      >*</span>
    </label>
    <slot />
    <small
      v-if="error"
      class="form-error"
    >{{ error }}</small>
    <small
      v-else-if="hint"
      class="form-hint"
    >{{ hint }}</small>
  </div>
</template>

<script setup>
defineProps({
  label: { type: String, default: '' },
  required: { type: Boolean, default: false },
  error: { type: String, default: '' },
  hint: { type: String, default: '' },
  inputId: { type: String, default: '' }
})
</script>

<style scoped>
.form-field {
  margin-bottom: 1.25rem;
}
.form-label {
  display: block;
  margin-bottom: 0.35rem;
  font-weight: 600;
  font-size: 0.85rem;
  color: var(--text-secondary);
}
.required-star {
  color: #ef4444;
  margin-left: 2px;
}
.form-error {
  display: block;
  margin-top: 0.3rem;
  font-size: 0.78rem;
  color: #ef4444;
}
.form-hint {
  display: block;
  margin-top: 0.3rem;
  font-size: 0.78rem;
  color: var(--text-muted);
}
.has-error :deep(.p-inputtext),
.has-error :deep(.p-dropdown),
.has-error :deep(.p-textarea),
.has-error :deep(.p-multiselect),
.has-error :deep(.p-inputnumber) {
  border-color: #ef4444 !important;
}
.has-error :deep(.p-inputtext:focus),
.has-error :deep(.p-dropdown:focus),
.has-error :deep(.p-textarea:focus) {
  box-shadow: 0 0 0 2px rgba(239, 68, 68, 0.2) !important;
}
</style>
