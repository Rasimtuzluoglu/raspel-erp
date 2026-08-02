<template>
    <div class="giris-sayfasi">
    <div class="giris-kutu">
      <div class="giris-logo">
        <div class="logo-icon">
          <img v-if="sirketLogo" :src="sirketLogo" class="sirket-logo" alt="logo" />
          <i v-else class="pi pi-calculator"></i>
        </div>
        <h1>RasPel</h1>
        <p class="alt-baslik">RasPel Yeni Nesil ERP <span class="versiyon">v1.0.0</span></p>
      </div>

      <div class="giris-form">
        <div class="form-grup">
          <label>Kullanıcı Adı</label>
          <div class="input-wrapper">
            <i class="pi pi-user"></i>
            <InputText ref="kullaniciInput" v-model="username" placeholder="Kullanıcı adı" @keyup.enter="odaklanSifre" />
          </div>
        </div>

        <div class="form-grup">
          <label>Şifre</label>
          <div class="input-wrapper">
            <i class="pi pi-lock"></i>
            <InputText ref="sifreInput" v-model="password" type="password" placeholder="••••••" @keyup.enter="odaklanSirket" />
          </div>
        </div>

        <div v-if="sirketler.length > 0" class="form-grup">
          <label>Firma Seçin</label>
          <div class="input-wrapper">
            <i class="pi pi-building"></i>
            <Select ref="sirketSelect" v-model="selectedSirket" :options="sirketler" optionLabel="ad" placeholder="Firma seçiniz" class="sirket-select" @keyup.enter="girisYap" scrollHeight="250px" />
          </div>
        </div>

        <Button label="Giriş Yap" icon="pi pi-sign-in" @click="girisYap" :loading="authStore.loading" class="giris-buton" />

        <div v-if="hata" class="hata-kutu">
          <i class="pi pi-exclamation-circle"></i> {{ hata }}
        </div>
      </div>

      <div class="giris-footer">
        <span>&copy; 2026 Rasim Tuzluoğlu</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore.js'
import { sirketAPI } from '../api/index.js'

const router = useRouter()
const authStore = useAuthStore()

const kullaniciInput = ref(null)
const sifreInput = ref(null)
const sirketSelect = ref(null)

const username = ref('')
const password = ref('')
const hata = ref('')
const sirketLogo = ref('')
const sirketler = ref([])
const selectedSirket = ref(null)

onMounted(() => {
  if (authStore.isLoggedIn) { router.push('/') }
  firmalariGetir()
})

const firmalariGetir = async () => {
  try {
    const res = await sirketAPI.getAktif()
    sirketler.value = res.data?.content || res.data || []
  } catch {}
}

watch(selectedSirket, (sirket) => {
  sirketLogo.value = sirket?.logoUrl || ''
})

const odaklanSifre = () => sifreInput.value?.$el?.querySelector('input')?.focus()
const odaklanSirket = () => sirketSelect.value?.$el?.querySelector('input')?.focus()

const girisYap = async () => {
  hata.value = ''
  if (!username.value.trim() || !password.value.trim()) {
    hata.value = 'Kullanıcı adı ve şifre giriniz'
    return
  }
  try {
    await authStore.girisYap(username.value, password.value, '', selectedSirket.value?.id)
    router.push('/')
  } catch (err) {
    hata.value = err.response?.data?.message || 'Giriş başarısız'
  }
}

</script>

<style scoped>
.giris-sayfasi {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--bg-primary) 0%, var(--bg-secondary) 50%, var(--bg-primary) 100%);
  padding: 20px;
}

.giris-kutu {
  width: 100%;
  max-width: 420px;
}

.giris-logo {
  text-align: center;
  margin-bottom: 35px;
}

.logo-icon {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 18px;
  box-shadow: 0 8px 32px rgba(59, 130, 246, 0.35);
  overflow: hidden;
}

.sirket-logo {
  width: 100%; height: 100%;
  object-fit: cover;
}

.logo-icon i { font-size: 36px; color: white; }

.giris-logo h1 {
  color: var(--text-primary);
  font-size: 28px;
  margin: 0 0 6px;
  font-weight: 700;
  letter-spacing: -0.5px;
}

.alt-baslik {
  color: var(--text-secondary);
  font-size: 14px;
  margin: 0;
}

.versiyon {
  display: inline-block;
  font-size: 10px;
  color: var(--text-muted);
  margin-left: 6px;
  padding: 2px 6px;
  border: 1px solid rgba(148,163,184,0.2);
  border-radius: 4px;
}

.giris-form {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 30px;
  box-shadow: var(--shadow);
}

.form-grup {
  margin-bottom: 18px;
}

.form-grup label {
  display: block;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
}

.input-wrapper {
  position: relative;
}
.input-wrapper i {
  position: absolute;
  left: 16px; top: 50%; transform: translateY(-50%);
  color: var(--text-muted);
  font-size: 18px;
  z-index: 2;
  pointer-events: none;
}
.input-wrapper :deep(.p-inputtext) {
  width: 100%;
  padding: 14px 16px 14px 44px !important;
  background: var(--bg-primary);
  border: 1px solid var(--border);
  border-radius: 10px;
  color: var(--text-primary);
  font-size: 16px;
  min-height: 52px;
}

.input-wrapper :deep(.p-inputtext:focus) {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.2);
  outline: none;
}

.sirket-select {
  width: 100%;
  padding: 14px 44px 14px 44px !important;
  background: var(--bg-primary);
  border: 1px solid var(--border);
  border-radius: 10px;
  color: var(--text-primary);
  font-size: 16px;
  min-height: 52px;
  display: flex;
  align-items: center;
}
.sirket-select:hover {
  border-color: #3b82f6;
}
.sirket-select.p-focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.2);
  outline: none;
}
.sirket-select :deep(.p-select-overlay) {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  min-width: 100%;
}
.sirket-select :deep(.p-select-option) {
  color: var(--text-primary);
  padding: 16px 18px;
  font-size: 16px;
}
.sirket-select :deep(.p-select-option:hover) {
  background: rgba(59, 130, 246, 0.1);
}
.sirket-select :deep(.p-select-option.p-select-option-selected) {
  background: rgba(59, 130, 246, 0.15);
  color: #3b82f6;
}
.sirket-select :deep(.p-select-label) {
  font-size: 16px;
  padding: 0 !important;
}
.sirket-select :deep(.p-select-dropdown) {
  width: 40px;
}
.sirket-select :deep(.p-placeholder) {
  color: var(--text-muted);
}

.giris-buton {
  width: 100%;
  padding: 12px;
  margin-top: 5px;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  color: white;
  transition: all 0.3s ease;
}

.giris-buton:hover {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  transform: translateY(-1px);
  box-shadow: 0 4px 20px rgba(59, 130, 246, 0.4);
}

.ayirici {
  display: flex;
  align-items: center;
  gap: 15px;
  margin: 25px 0 18px;
  color: var(--text-muted);
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.ayirici::before,
.ayirici::after {
  content: '';
  flex: 1;
  height: 1px;
  background: rgba(148, 163, 184, 0.15);
}

.kullanici-listesi {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.kullanici-kart {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.kullanici-kart:hover {
  border-color: rgba(59, 130, 246, 0.3);
  transform: translateY(-2px);
  box-shadow: var(--shadow);
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  background: var(--bg-primary);
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-yedek {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
  color: white;
  font-weight: 600;
  font-size: 16px;
}

.kullanici-bilgi {
  min-width: 0;
}

.kullanici-bilgi strong {
  display: block;
  color: #f1f5f9;
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.kullanici-bilgi small {
  display: block;
  color: #64748b;
  font-size: 11px;
}

/* Hata kutusu */
.hata-kutu {
  margin-top: 16px;
  padding: 12px 16px;
  background: rgba(239, 68, 68, 0.15);
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 10px;
  color: #fca5a5;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 500;
  backdrop-filter: blur(4px);
}
.hata-kutu i { font-size: 16px; flex-shrink: 0; }

.giris-footer {
  text-align: center;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid rgba(148,163,184,0.1);
}
.giris-footer span {
  font-size: 11px;
  color: #475569;
  letter-spacing: 0.3px;
}

@media (max-width: 480px) {
  .kullanici-listesi { grid-template-columns: 1fr; }
  .giris-form { padding: 22px; }
}
</style>
