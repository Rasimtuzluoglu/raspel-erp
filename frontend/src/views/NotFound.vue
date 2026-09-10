<template>
  <div class="not-found">
    <div class="not-found-content">
      <h1>404</h1>
      <p>{{ t('notFound.bulunamadi') }}</p>
      <router-link to="/">
        <Button
          :label="t('notFound.anaSayfa')"
          icon="pi pi-home"
        />
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

<style scoped>
.not-found {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
}

.not-found-content {
  text-align: center;
}

.not-found-content h1 {
  font-size: 96px;
  margin: 0;
  color: var(--p-primary-color);
}

.not-found-content p {
  font-size: 18px;
  margin: 16px 0 24px;
  color: #94a3b8;
}
</style>
