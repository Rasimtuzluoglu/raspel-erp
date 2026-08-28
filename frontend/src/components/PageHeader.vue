<template>
  <div class="page-header">
    <div class="page-header-main">
      <div class="page-header-content">
        <div class="page-header-title">
          <i
            v-if="icon"
            :class="icon"
            class="page-header-icon"
          />
          <h1 class="page-header-heading">
            {{ title }}
          </h1>
        </div>
        <p
          v-if="headerDesc"
          class="page-header-desc"
        >
          {{ headerDesc }}
        </p>
      </div>
      <div
        v-if="$slots.actions || $slots.default"
        class="page-header-actions"
      >
        <slot name="actions" />
        <slot />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, required: true },
  icon: { type: String, default: null },
  description: { type: String, default: null },
  subtitle: { type: String, default: null }
})

const headerDesc = computed(() => props.description || props.subtitle || null)
</script>

<style scoped>
.page-header {
  margin-bottom: 24px;
}
.page-header-main {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  flex-wrap: wrap;
}
.page-header-content {
  flex: 1;
  min-width: 250px;
}
.page-header-title {
  display: flex;
  align-items: center;
  gap: 10px;
}
.page-header-icon {
  font-size: 24px;
  color: var(--primary-color, #3b82f6);
}
.page-header-heading {
  font-size: 24px;
  font-weight: 600;
  margin: 0;
  color: var(--text-primary, #f1f5f9);
}
.page-header-desc {
  margin: 6px 0 0 0;
  font-size: 14px;
  color: var(--text-secondary, #94a3b8);
}
.page-header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
</style>
