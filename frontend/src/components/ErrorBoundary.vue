<template>
  <div v-if="error" class="error-boundary">
    <div class="error-card">
      <i class="pi pi-exclamation-triangle error-icon"></i>
      <h2>Bir Hata Oluştu</h2>
      <p class="error-message">{{ error.message || 'Beklenmeyen bir sayfa hatası meydana geldi.' }}</p>
      <div class="error-actions">
        <button class="retry-btn" @click="resetError">
          <i class="pi pi-refresh"></i> Tekrar Dene
        </button>
        <button class="home-btn" @click="goHome">
          <i class="pi pi-home"></i> Ana Sayfaya Dön
        </button>
      </div>
    </div>
  </div>
  <slot v-else></slot>
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
  background: var(--surface-card, #ffffff);
  border: 1px solid var(--surface-border, #e2e8f0);
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
  color: var(--text-color, #1e293b);
}

.error-message {
  color: var(--text-color-secondary, #64748b);
  margin-bottom: 1.5rem;
  font-size: 0.95rem;
  word-break: break-word;
}

.error-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: center;
}

.retry-btn, .home-btn {
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
  background: var(--primary-color, #3b82f6);
  color: #ffffff;
}

.retry-btn:hover {
  background: #2563eb;
}

.home-btn {
  background: var(--surface-ground, #f1f5f9);
  color: var(--text-color, #334155);
}

.home-btn:hover {
  background: #e2e8f0;
}
</style>
