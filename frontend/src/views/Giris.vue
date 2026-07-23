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
            <InputText ref="sifreInput" v-model="password" type="password" placeholder="••••••" @keyup.enter="girisYap" />
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore.js'

const router = useRouter()
const authStore = useAuthStore()

const kullaniciInput = ref(null)
const sifreInput = ref(null)

const username = ref('')
const password = ref('')
const hata = ref('')
const sirketLogo = ref('')

onMounted(() => {
  if (authStore.isLoggedIn) { router.push('/') }
})

const odaklanKullanici = () => kullaniciInput.value?.$el?.querySelector('input')?.focus()
const odaklanSifre = () => sifreInput.value?.$el?.querySelector('input')?.focus()

const girisYap = async () => {
  hata.value = ''
  if (!username.value.trim() || !password.value.trim()) {
    hata.value = 'Kullanıcı adı ve şifre giriniz'
    return
  }
  try {
    await authStore.girisYap(username.value, password.value, '')
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
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0f172a 100%);
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
  color: #f1f5f9;
  font-size: 28px;
  margin: 0 0 6px;
  font-weight: 700;
  letter-spacing: -0.5px;
}

.alt-baslik {
  color: #94a3b8;
  font-size: 14px;
  margin: 0;
}

.versiyon {
  display: inline-block;
  font-size: 10px;
  color: #64748b;
  margin-left: 6px;
  padding: 2px 6px;
  border: 1px solid rgba(148,163,184,0.2);
  border-radius: 4px;
}

.giris-form {
  background: rgba(30, 41, 59, 0.8);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(148, 163, 184, 0.15);
  border-radius: 16px;
  padding: 30px;
}

.form-grup {
  margin-bottom: 18px;
}

.form-grup label {
  display: block;
  color: #94a3b8;
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
  left: 14px; top: 50%; transform: translateY(-50%);
  color: #64748b;
  font-size: 16px;
  z-index: 2;
  pointer-events: none;
}
.input-wrapper :deep(.p-inputtext) {
  width: 100%;
  padding: 12px 14px 12px 42px !important;
  background: #1e293b;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 10px;
  color: #f1f5f9;
  font-size: 14px;
}

.input-wrapper :deep(.p-inputtext:focus) {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.2);
  outline: none;
}

.input-wrapper :deep(.p-inputtext::placeholder) {
  color: #475569;
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
  color: #475569;
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
  background: rgba(30, 41, 59, 0.6);
  border: 1px solid rgba(148, 163, 184, 0.1);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.kullanici-kart:hover {
  background: rgba(30, 41, 59, 0.9);
  border-color: rgba(59, 130, 246, 0.3);
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  background: #1e293b;
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
