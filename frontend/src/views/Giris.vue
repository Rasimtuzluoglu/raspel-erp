<template>
  <div class="giris-sayfasi">
    <div class="giris-kutu">
      <div class="giris-logo">
        <div class="logo-icon">
          <img v-if="sirketLogo" :src="sirketLogo" class="sirket-logo" alt="logo">
          <i v-else class="pi pi-calculator" />
        </div>
        <h1>RasPel</h1>
        <p class="alt-baslik">KOBİ'nin Tek Panosu</p>
      </div>

      <div class="giris-form">
        <!-- Hata mesaji - form ustunde -->
        <div v-if="hata" class="hata-kutu"><i class="pi pi-exclamation-circle" /> {{ hata }}</div>

        <div v-if="ikiFaktorAdimi">
          <div class="iki-fa-ikon"><i class="pi pi-shield" /></div>
          <h2 class="iki-fa-baslik">Iki Faktorlu Dogrulama</h2>
          <p class="iki-fa-alt">Kimlik dogrulayici uygulamanizdaki 6 haneli kodu girin.</p>
          <div class="form-grup">
            <div class="input-wrapper">
              <i class="pi pi-key" />
              <InputText v-model="ikiFaktorKod" placeholder="••••••" inputmode="numeric" maxlength="6"
                style="text-align:center;letter-spacing:6px;font-size:20px" @keyup.enter="ikiFaktorDogrula" />
            </div>
          </div>
          <Button label="Dogrula ve Giris Yap" icon="pi pi-shield" :loading="authStore.loading" class="giris-buton" @click="ikiFaktorDogrula" />
          <div class="geri-satir"><a @click="geriDon">&larr; Geri don</a></div>
        </div>

        <div v-else>
          <div class="form-grup">
            <label>Kullanici Adi</label>
            <div class="input-wrapper">
              <i class="pi pi-user" />
              <InputText ref="kullaniciInput" v-model="username" placeholder="Kullanici adi" @keyup.enter="odaklanSifre" />
            </div>
          </div>

          <div class="form-grup">
            <label>Sifre</label>
            <div class="input-wrapper">
              <i class="pi pi-lock" />
              <InputText ref="sifreInput" v-model="password" :type="sifreGorunur ? 'text' : 'password'" placeholder="••••••" @keyup.enter="odaklanSirket" />
              <button type="button" class="sifre-toggle" @click="sifreGorunur = !sifreGorunur" tabindex="-1">
                <i :class="sifreGorunur ? 'pi pi-eye-slash' : 'pi pi-eye'" />
              </button>
            </div>
          </div>

          <div v-if="sirketler.length > 0" class="form-grup">
            <label>Firma Secin</label>
            <div class="input-wrapper">
              <i class="pi pi-building" />
              <Select ref="sirketSelect" v-model="selectedSirket" :options="filtreliSirketler" option-label="ad"
                placeholder="Firma seciniz" class="sirket-select" scroll-height="250px" filter
                @keyup.enter="girisYap" />
            </div>
          </div>

          <div class="beni-hatirla">
            <Checkbox v-model="beniHatirla" :binary="true" input-id="beniHatirla" />
            <label for="beniHatirla">Beni Hatirla</label>
          </div>

          <Button label="Giris Yap" icon="pi pi-sign-in" :loading="authStore.loading" class="giris-buton" @click="girisYap" />

          <div class="giris-alt-linkler">
            <a @click="sifremiUnuttumAdimi = true">Sifremi Unuttum</a>
          </div>
        </div>
      </div>

      <!-- Sifremi Unuttum - Formun disinda ayri bolum -->
      <div v-if="sifremiUnuttumAdimi && !ikiFaktorAdimi" class="sifre-sifirla-panel">
        <div class="sifirla-ust">
          <i class="pi pi-envelope" />
          <h3>Sifremi Unuttum</h3>
          <a @click="sifremiUnuttumAdimi = false">&larr; Girise don</a>
        </div>
        <p>E-posta adresinizi girin, sifre sifirlama baglantisi gonderelim.</p>
        <div class="sifirla-input-row">
          <InputText v-model="sifirlaEmail" placeholder="E-posta adresi" />
          <Button label="Gonder" icon="pi pi-send" :loading="sifirlaYukleniyor" @click="sifreSifirla" />
        </div>
        <small v-if="sifirlaMesaj" class="sifirla-mesaj">{{ sifirlaMesaj }}</small>
      </div>

      <div class="giris-footer">
        <ThemeSwitcher />
        <span>&copy; 2026 RasPel Co.</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore.js'
import { sirketAPI, kullaniciAPI } from '../api/index.js'
import ThemeSwitcher from '../components/ThemeSwitcher.vue'

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
const sifreGorunur = ref(false)
const beniHatirla = ref(false)

const ikiFaktorAdimi = ref(false)
const ikiFaktorKod = ref('')
const girisToken = ref('')
const girilenFirma = ref('')

const sifremiUnuttumAdimi = ref(false)
const sifirlaEmail = ref('')
const sifirlaYukleniyor = ref(false)
const sifirlaMesaj = ref('')

const filtreliSirketler = computed(() => sirketler.value)

onMounted(() => {
  if (authStore.isLoggedIn) { router.push('/') }
  firmalariGetir()
  beniHatirla.value = localStorage.getItem('raspel_erp_beni_hatirla') === 'true'
})

const firmalariGetir = async () => {
  try {
    const res = await sirketAPI.getAktif()
    sirketler.value = res.data?.content || res.data || []
  } catch { /* empty */ }
}

watch(selectedSirket, (sirket) => { sirketLogo.value = sirket?.logoUrl || '' })

const odaklanSifre = () => sifreInput.value?.$el?.querySelector('input')?.focus()
const odaklanSirket = () => sirketSelect.value?.$el?.querySelector('input')?.focus()

const girisYap = async () => {
  hata.value = ''
  if (!username.value.trim() || !password.value.trim()) { hata.value = 'Kullanici adi ve sifre giriniz'; return }
  try {
    localStorage.setItem('raspel_erp_beni_hatirla', beniHatirla.value ? 'true' : 'false')
    girilenFirma.value = selectedSirket.value?.ad || ''
    const sonuc = await authStore.girisYap(username.value, password.value, girilenFirma.value, selectedSirket.value?.id)
    if (sonuc?.twoFactorGerekli) { girisToken.value = sonuc.girisToken; ikiFaktorAdimi.value = true; return }
    router.push('/')
  } catch (err) { hata.value = err.response?.data?.message || 'Giris basarisiz' }
}

const ikiFaktorDogrula = async () => {
  hata.value = ''
  if (!ikiFaktorKod.value.trim()) { hata.value = 'Dogrulama kodunu girin'; return }
  try {
    await authStore.giris2fa(girisToken.value, ikiFaktorKod.value.trim(), girilenFirma.value, selectedSirket.value?.id)
    router.push('/')
  } catch (err) { hata.value = err.response?.data?.message || 'Dogrulama basarisiz' }
}

const geriDon = () => { ikiFaktorAdimi.value = false; ikiFaktorKod.value = ''; girisToken.value = '' }

const sifreSifirla = async () => {
  if (!sifirlaEmail.value.trim()) { sifirlaMesaj.value = 'E-posta adresi giriniz'; return }
  sifirlaYukleniyor.value = true; sifirlaMesaj.value = ''
  try {
    await kullaniciAPI.sifreSifirla?.({ email: sifirlaEmail.value.trim() }) || Promise.resolve()
    sifirlaMesaj.value = 'Sifre sifirlama baglantisi e-posta adresinize gonderildi.'
  } catch { sifirlaMesaj.value = 'Sifre sifirlama islemi basarisiz. Lutfen yoneticinize basin.' }
  finally { sifirlaYukleniyor.value = false }
}
</script>

<style scoped>
.giris-sayfasi { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, var(--bg-primary) 0%, var(--bg-secondary) 50%, var(--bg-primary) 100%); padding: 20px; }
.giris-kutu { width: 100%; max-width: 420px; }
.giris-logo { text-align: center; margin-bottom: 30px; }
.logo-icon { width: 72px; height: 72px; background: linear-gradient(135deg, #3b82f6, #1d4ed8); border-radius: 20px; display: flex; align-items: center; justify-content: center; margin: 0 auto 16px; box-shadow: 0 8px 32px rgba(59,130,246,.35); overflow: hidden; }
.sirket-logo { width: 100%; height: 100%; object-fit: cover; }
.logo-icon i { font-size: 32px; color: white; }
.giris-logo h1 { color: var(--text-primary); font-size: 26px; margin: 0 0 4px; font-weight: 700; }
.alt-baslik { color: var(--text-secondary); font-size: 13px; margin: 0; }
.versiyon { font-size: 10px; color: var(--text-muted); margin-left: 6px; padding: 2px 6px; border: 1px solid rgba(148,163,184,.2); border-radius: 4px; }
.giris-form { background: var(--bg-card); border: 1px solid var(--border); border-radius: 16px; padding: 28px; box-shadow: var(--shadow); }
.form-grup { margin-bottom: 16px; }
.form-grup label { display: block; color: var(--text-secondary); font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: .5px; margin-bottom: 6px; }
.input-wrapper { position: relative; }
.input-wrapper > i { position: absolute; left: 14px; top: 50%; transform: translateY(-50%); color: var(--text-muted); font-size: 16px; z-index: 2; pointer-events: none; }
.input-wrapper :deep(.p-inputtext) { width: 100%; padding: 13px 14px 13px 40px !important; background: var(--bg-primary); border: 1px solid var(--border); border-radius: 10px; color: var(--text-primary); font-size: 15px; min-height: 48px; }
.input-wrapper :deep(.p-inputtext:focus) { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59,130,246,.2); outline: none; }
.sifre-toggle { position: absolute; right: 12px; top: 50%; transform: translateY(-50%); background: none; border: none; color: var(--text-muted); cursor: pointer; font-size: 16px; z-index: 2; padding: 4px; }
.sifre-toggle:hover { color: var(--text-primary); }
.sirket-select { width: 100%; }
.beni-hatirla { display: flex; align-items: center; gap: 8px; margin-bottom: 14px; font-size: 13px; color: var(--text-secondary); }
.giris-buton { width: 100%; padding: 12px; margin-top: 2px; background: linear-gradient(135deg, #3b82f6, #2563eb); border: none; border-radius: 10px; font-size: 15px; font-weight: 600; color: white; }
.giris-buton:hover { background: linear-gradient(135deg, #2563eb, #1d4ed8); transform: translateY(-1px); box-shadow: 0 4px 20px rgba(59,130,246,.4); }
.giris-alt-linkler { text-align: center; margin-top: 14px; }
.giris-alt-linkler a { color: var(--text-muted); font-size: 13px; cursor: pointer; }
.giris-alt-linkler a:hover { color: var(--primary-color); }
.hata-kutu { margin-bottom: 16px; padding: 12px 14px; background: rgba(239,68,68,.15); border: 1px solid rgba(239,68,68,.3); border-radius: 10px; color: #fca5a5; font-size: 13px; display: flex; align-items: center; gap: 10px; font-weight: 500; }
.hata-kutu i { font-size: 16px; flex-shrink: 0; }
.giris-footer { text-align: center; margin-top: 24px; padding-top: 18px; border-top: 1px solid rgba(148,163,184,.1); display: flex; align-items: center; justify-content: center; gap: 16px; }
.giris-footer span { font-size: 11px; color: #475569; }
.iki-fa-ikon { width: 60px; height: 60px; margin: 0 auto 12px; background: linear-gradient(135deg, #10b981, #059669); border-radius: 16px; display: flex; align-items: center; justify-content: center; box-shadow: 0 8px 24px rgba(16,185,129,.3); }
.iki-fa-ikon i { font-size: 26px; color: white; }
.iki-fa-baslik { text-align: center; color: var(--text-primary); font-size: 17px; margin: 0 0 4px; }
.iki-fa-alt { text-align: center; color: var(--text-secondary); font-size: 13px; margin: 0 0 18px; }
.geri-satir { text-align: center; margin-top: 14px; }
.geri-satir a { color: var(--text-muted); font-size: 13px; cursor: pointer; }
.geri-satir a:hover { color: var(--text-primary); }
.sifre-sifirla { margin-top: 14px; padding: 14px; background: var(--surface-ground); border-radius: 10px; }
.sifre-sifirla p { font-size: 13px; color: var(--text-secondary); margin: 0 0 10px; }
.sifirla-mesaj { display: block; margin-top: 8px; font-size: 12px; color: var(--green-500); }

.sifre-sifirla-panel {
  background: var(--bg-card); border: 1px solid var(--border);
  border-radius: 16px; padding: 24px; box-shadow: var(--shadow);
  margin-top: 16px;
}
.sifirla-ust { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.sifirla-ust i { font-size: 22px; color: var(--primary-color); }
.sifirla-ust h3 { margin: 0; font-size: 16px; color: var(--text-primary); flex: 1; }
.sifirla-ust a { color: var(--text-muted); font-size: 13px; cursor: pointer; }
.sifirla-ust a:hover { color: var(--text-primary); }
.sifre-sifirla-panel p { font-size: 13px; color: var(--text-secondary); margin: 0 0 14px; }
.sifirla-input-row { display: flex; gap: 8px; }
.sifirla-input-row .p-inputtext { flex: 1; }
.sifirla-mesaj { display: block; margin-top: 10px; font-size: 12px; color: var(--green-500); }

@media (max-width: 480px) { .giris-form { padding: 20px; } .sifirla-input-row { flex-direction: column; } }
</style>
