<template>
  <div class="error-page">
    <div class="error-card">
      <i class="pi pi-lock error-icon" />
      <h1>403</h1>
      <h2>Erisim Engellendi</h2>
      <p>Bu sayfaya erismek icin yetkiniz bulunmuyor.</p>
      <div class="error-actions">
        <router-link to="/" class="p-button p-button-primary">
          <i class="pi pi-home" /> Ana Sayfa
        </router-link>
        <Button label="Geri Don" icon="pi pi-arrow-left" class="p-button-outlined" @click="$router.back()" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.error-page { display: flex; align-items: center; justify-content: center; min-height: 60vh; }
.error-card { text-align: center; padding: 3rem; }
.error-icon { font-size: 4rem; color: var(--orange-500); margin-bottom: 1rem; }
h1 { font-size: 5rem; margin: 0; color: var(--text-color); font-weight: 700; }
h2 { font-size: 1.5rem; margin: 0.5rem 0; color: var(--text-color-secondary); }
p { color: var(--text-color-secondary); margin-bottom: 2rem; }
.error-actions { display: flex; gap: 1rem; justify-content: center; }
</style>
