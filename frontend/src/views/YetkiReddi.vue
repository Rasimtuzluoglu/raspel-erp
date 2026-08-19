<template>
  <div class="yetki-reddi">
    <div class="hata-kart">
      <i class="pi pi-lock hata-icon" />
      <h1>Yetkiniz Yok</h1>
      <p>Bu sayfaya erişmek için gerekli yetkiye sahip değilsiniz.</p>
      <p class="ipucu">
        Yöneticinizle iletişime geçerek yetki talep edebilirsiniz.
      </p>
      <div class="hata-aksiyon">
        <router-link
          to="/"
          class="p-button p-button-primary"
        >
          <i class="pi pi-home" /> Ana Sayfaya Dön
        </router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.yetki-reddi {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  padding: 2rem;
}
.hata-kart {
  text-align: center;
  max-width: 480px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 3rem 2rem;
  box-shadow: var(--shadow);
}
.hata-icon {
  font-size: 3.5rem;
  color: #f59e0b;
  margin-bottom: 1rem;
}
.hata-kart h1 {
  margin: 0 0 0.75rem;
  font-size: 1.5rem;
}
.hata-kart p {
  color: var(--text-secondary);
  margin: 0 0 0.5rem;
  font-size: 0.95rem;
}
.ipucu {
  font-size: 0.85rem !important;
  color: var(--text-muted) !important;
}
.hata-aksiyon {
  margin-top: 1.5rem;
}
</style>
