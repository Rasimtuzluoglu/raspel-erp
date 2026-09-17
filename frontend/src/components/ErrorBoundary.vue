<template>
  <div
    v-if="error"
    class="error-boundary"
  >
    <div class="error-card">
      <i class="pi pi-exclamation-triangle error-icon" />
      <h2>Bir Hata Oluştu</h2>
      <p class="error-message">
        {{ error.message || $t('common.unexpectedError') }}
      </p>
      <div class="error-actions">
        <button
          class="retry-btn"
          @click="resetError"
        >
          <i class="pi pi-refresh" /> Tekrar Dene
        </button>
        <button
          class="home-btn"
          @click="goHome"
        >
          <i class="pi pi-home" /> Ana Sayfaya Dön
        </button>
      </div>
    </div>
  </div>
  <slot v-else />
</template>

<script setup>
import { ref, onErrorCaptured } from 'vue'
import { useRouter } from 'vue-router'

const error = ref(null)
const router = useRouter()

onErrorCaptured((err, instance, info) => {
  console.error('ErrorBoundary captured error:', err, info)
  error.value = err
  return false
})

const resetError = () => {
  error.value = null
}

const goHome = () => {
  error.value = null
  router.push('/')
}
</script>

<style scoped>
.error-boundary {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  padding: 2rem;
}

.error-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 2.5rem;
  text-align: center;
  max-width: 480px;
  width: 100%;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.error-icon {
  font-size: 3rem;
  color: #ef4444;
  margin-bottom: 1rem;
}

.error-card h2 {
  margin: 0 0 0.5rem 0;
  font-size: 1.5rem;
  color: var(--text-primary);
}

.error-message {
  color: var(--text-secondary);
  margin-bottom: 1.5rem;
  font-size: 0.95rem;
  word-break: break-word;
}

.error-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: center;
}

.retry-btn,
.home-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 1.2rem;
  border-radius: 8px;
  font-weight: 500;
  font-size: 0.9rem;
  cursor: pointer;
  border: none;
  transition: background 0.2s;
}

.retry-btn {
  background: var(--primary-color, var(--accent));
  color: #ffffff;
}

.retry-btn:hover {
  background: var(--accent-hover);
}

.home-btn {
  background: var(--bg-secondary);
  color: var(--text-primary);
}

.home-btn:hover {
  background: rgba(148, 163, 184, 0.18);
}
</style>
