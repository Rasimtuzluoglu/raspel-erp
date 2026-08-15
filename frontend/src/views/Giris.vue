<template>
  <div class="giris-sayfasi">
    <div class="giris-kutu">
      <div class="giris-logo">
        <div class="logo-icon">
          <img v-if="sirketLogo" :src="sirketLogo" class="sirket-logo" alt="logo">
          <i v-else class="pi pi-calculator" />
        </div>
        <h1>RasPel</h1>
        <p class="alt-baslik">{{ $t('giris.subtitle') }}</p>
      </div>

      <div class="giris-form">
        <div v-if="hata" class="hata-kutu"><i class="pi pi-exclamation-circle" /> {{ hata }}</div>

        <!-- Adim 0: Ilk Kurulum -->
        <div v-if="kurulumAdimi">
          <div class="kurulum-ikon"><i class="pi pi-rocket" /></div>
          <h2 class="iki-fa-baslik">{{ $t('kurulum.welcome') }}</h2>
          <p class="iki-fa-alt">{{ $t('kurulum.hint') }}</p>

          <div class="form-grup">
            <label>{{ $t('kurulum.companyName') }}</label>
            <div class="input-wrapper">
              <i class="pi pi-building" />
              <InputText v-model="kurulumForm.ad" :placeholder="$t('kurulum.companyName')" @keyup.enter="kurulumBaslat" />
            </div>
          </div>

          <div class="kurulum-iki-kolon">
            <div class="form-grup">
              <label>{{ $t('kurulum.taxNumber') }}</label>
              <div class="input-wrapper">
                <i class="pi pi-hashtag" />
                <InputText v-model="kurulumForm.vergiNo" :placeholder="$t('kurulum.taxNumber')" />
              </div>
            </div>
            <div class="form-grup">
              <label>{{ $t('kurulum.taxOffice') }}</label>
              <div class="input-wrapper">
                <i class="pi pi-map-marker" />
                <InputText v-model="kurulumForm.vergiDairesi" :placeholder="$t('kurulum.taxOffice')" />
              </div>
            </div>
          </div>

          <div class="form-grup">
            <label>{{ $t('kurulum.phone') }}</label>
            <div class="input-wrapper">
              <i class="pi pi-phone" />
              <InputText v-model="kurulumForm.telefon" :placeholder="$t('kurulum.phone')" />
            </div>
          </div>

          <div class="form-grup">
            <label>{{ $t('kurulum.email') }}</label>
            <div class="input-wrapper">
              <i class="pi pi-envelope" />
              <InputText v-model="kurulumForm.email" :placeholder="$t('kurulum.email')" />
            </div>
          </div>

          <div class="kurulum-ayrac" />

          <div class="form-grup">
            <label>{{ $t('kurulum.adminUsername') }}</label>
            <div class="input-wrapper">
              <i class="pi pi-user" />
              <InputText v-model="kurulumForm.adminUsername" :placeholder="$t('kurulum.adminUsername')" />
            </div>
          </div>

          <div class="form-grup">
            <label>{{ $t('kurulum.fullName') }}</label>
            <div class="input-wrapper">
              <i class="pi pi-id-card" />
              <InputText v-model="kurulumForm.adminDisplayName" :placeholder="$t('kurulum.fullName')" />
            </div>
          </div>

          <div class="form-grup">
            <label>{{ $t('kurulum.password') }}</label>
            <div class="input-wrapper">
              <i class="pi pi-lock" />
              <InputText v-model="kurulumForm.adminPassword" :type="sifreGorunur ? 'text' : 'password'" placeholder="••••••" @keyup.enter="kurulumBaslat" />
              <button type="button" class="sifre-toggle" @click="sifreGorunur = !sifreGorunur" tabindex="-1">
                <i :class="sifreGorunur ? 'pi pi-eye-slash' : 'pi pi-eye'" />
              </button>
            </div>
            <small class="sifre-ipucu">{{ $t('kurulum.passwordHint') }}</small>
          </div>

          <Button :label="$t('kurulum.submit')" icon="pi pi-check" :loading="kurulumYukleniyor" class="giris-buton" @click="kurulumBaslat" />
        </div>

        <!-- Adim 2: 2FA -->
        <div v-else-if="ikiFaktorAdimi">
          <div class="iki-fa-ikon"><i class="pi pi-shield" /></div>
          <h2 class="iki-fa-baslik">{{ $t('giris.twoFactorTitle') }}</h2>
          <p class="iki-fa-alt">{{ $t('giris.twoFactorHint') }}</p>
          <div class="form-grup">
            <div class="input-wrapper">
              <i class="pi pi-key" />
              <InputText v-model="ikiFaktorKod" placeholder="••••••" inputmode="numeric" maxlength="6"
                style="text-align:center;letter-spacing:6px;font-size:20px" @keyup.enter="ikiFaktorDogrula" />
            </div>
          </div>
          <Button :label="$t('giris.verify')" icon="pi pi-shield" :loading="authStore.loading" class="giris-buton" @click="ikiFaktorDogrula" />
          <div class="geri-satir"><a @click="geriDon">&larr; {{ $t('giris.back') }}</a></div>
        </div>

        <!-- Adim 3: Sirket Secimi -->
        <div v-else-if="sirketSecimAdimi">
          <div class="sirket-secim-ikon"><i class="pi pi-building" /></div>
          <h2 class="iki-fa-baslik">{{ $t('giris.selectCompany') }}</h2>
          <p class="iki-fa-alt">{{ $t('giris.selectCompanyHint') }}</p>
          <div v-if="sirketler.length > 0" class="sirket-listesi">
            <button
              v-for="sirket in sirketler"
              :key="sirket.id"
              class="sirket-secim-buton"
              @click="sirketSecVeGirisYap(sirket)"
            >
              <img v-if="sirket.logoUrl" :src="sirket.logoUrl" class="sirket-mini-logo">
              <i v-else class="pi pi-building sirket-mini-icon" />
              <span class="sirket-ad">{{ sirket.ad }}</span>
              <i class="pi pi-chevron-right sirket-ok" />
            </button>
          </div>
          <p v-else class="sirket-yok">{{ $t('giris.noCompany') }}</p>
          <div class="geri-satir"><a @click="tumAdimlariSifirla">&larr; {{ $t('giris.loginAgain') }}</a></div>
        </div>

        <!-- Adim 1: Kullanici adi ve sifre -->
        <div v-else>
          <div class="form-grup">
            <label>{{ $t('auth.username') }}</label>
            <div class="input-wrapper">
              <i class="pi pi-user" />
              <InputText ref="kullaniciInput" v-model="username" :placeholder="$t('auth.username')" @keyup.enter="odaklanSifre" />
            </div>
          </div>

          <div class="form-grup">
            <label>{{ $t('auth.password') }}</label>
            <div class="input-wrapper">
              <i class="pi pi-lock" />
              <InputText ref="sifreInput" v-model="password" :type="sifreGorunur ? 'text' : 'password'" placeholder="••••••" @keyup.enter="girisYap" />
              <button type="button" class="sifre-toggle" @click="sifreGorunur = !sifreGorunur" tabindex="-1">
                <i :class="sifreGorunur ? 'pi pi-eye-slash' : 'pi pi-eye'" />
              </button>
            </div>
          </div>

          <div class="beni-hatirla">
            <Checkbox v-model="beniHatirla" :binary="true" input-id="beniHatirla" />
            <label for="beniHatirla">{{ $t('giris.rememberMe') }}</label>
          </div>

          <Button :label="$t('auth.login')" icon="pi pi-sign-in" :loading="authStore.loading" class="giris-buton" @click="girisYap" />

          <div class="giris-alt-linkler">
            <a @click="sifremiUnuttumAdimi = true">{{ $t('giris.forgotPassword') }}</a>
          </div>
        </div>
      </div>

      <!-- Sifremi Unuttum -->
      <div v-if="sifremiUnuttumAdimi && !ikiFaktorAdimi && !sirketSecimAdimi && !kurulumAdimi" class="sifre-sifirla-panel">
        <div class="sifirla-ust">
          <i class="pi pi-envelope" />
          <h3>{{ $t('giris.forgotTitle') }}</h3>
          <a @click="sifremiUnuttumAdimi = false">&larr; {{ $t('giris.backToLogin') }}</a>
        </div>
        <p>{{ $t('giris.forgotHint') }}</p>
      </div>

      <div class="giris-footer">
        <ThemeSwitcher />
        <span>&copy; 2026 RasPel Co.</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '../stores/authStore.js'
import { kurulumAPI } from '../api/index.js'
import ThemeSwitcher from '../components/ThemeSwitcher.vue'

const router = useRouter()
const authStore = useAuthStore()
const { t } = useI18n()

const kullaniciInput = ref(null)
const sifreInput = ref(null)

const username = ref('')
const password = ref('')
const hata = ref('')
const sirketLogo = ref('')
const sirketler = ref([])
const sifreGorunur = ref(false)
const beniHatirla = ref(false)

const ikiFaktorAdimi = ref(false)
const sirketSecimAdimi = ref(false)
const ikiFaktorKod = ref('')
const girisToken = ref('')

const kurulumAdimi = ref(false)
const kurulumYukleniyor = ref(false)
const kurulumForm = ref({ ad: '', vergiNo: '', vergiDairesi: '', telefon: '', email: '', adminUsername: '', adminDisplayName: '', adminPassword: '' })

const sifremiUnuttumAdimi = ref(false)

onMounted(async () => {
  if (authStore.isLoggedIn) { router.push('/'); return }
  beniHatirla.value = localStorage.getItem('raspel_erp_beni_hatirla') === 'true'
  await kurulumDurumKontrol()
})

const kurulumDurumKontrol = async () => {
  try {
    const res = await kurulumAPI.durum()
    kurulumAdimi.value = !!res.data?.kurulumGerekli
  } catch { /* backend erisilemiyorsa giris formu gosterilir */ }
}

const kurulumBaslat = async () => {
  hata.value = ''
  const f = kurulumForm.value
  if (!f.ad.trim() || !f.vergiNo.trim()) { hata.value = t('kurulum.companyRequired'); return }
  if (!f.adminUsername.trim() || !f.adminPassword) { hata.value = t('kurulum.credentialsRequired'); return }
  kurulumYukleniyor.value = true
  try {
    const sonuc = await kurulumAPI.baslat({ ...f })
    girisToken.value = sonuc.data?.girisToken
    sirketler.value = sonuc.data?.sirketler || []
    if (sirketler.value.length === 1) {
      await sirketSecVeGirisYap(sirketler.value[0])
      return
    }
    kurulumAdimi.value = false
    sirketSecimAdimi.value = true
  } catch (err) {
    hata.value = err.response?.data?.message || t('kurulum.failed')
  } finally {
    kurulumYukleniyor.value = false
  }
}

const odaklanSifre = () => sifreInput.value?.$el?.querySelector('input')?.focus()

const girisYap = async () => {
  hata.value = ''
  if (!username.value.trim() || !password.value.trim()) { hata.value = t('giris.emptyCredentials'); return }
  try {
    localStorage.setItem('raspel_erp_beni_hatirla', beniHatirla.value ? 'true' : 'false')
    const sonuc = await authStore.girisYap(username.value, password.value)
    if (sonuc?.twoFactorGerekli) {
      girisToken.value = sonuc.girisToken
      ikiFaktorAdimi.value = true
      return
    }
    girisToken.value = sonuc.girisToken
    sirketler.value = sonuc.sirketler || []
    if (sirketler.value.length === 1) {
      await sirketSecVeGirisYap(sirketler.value[0])
      return
    }
    sirketSecimAdimi.value = true
  } catch (err) { hata.value = err.response?.data?.message || t('giris.loginFailed') }
}

const sirketSecVeGirisYap = async (sirket) => {
  hata.value = ''
  sirketLogo.value = sirket.logoUrl || ''
  try {
    await authStore.girisSirket(girisToken.value, sirket.id)
    router.push('/')
  } catch (err) { hata.value = err.response?.data?.message || t('giris.companySelectFailed') }
}

const ikiFaktorDogrula = async () => {
  hata.value = ''
  if (!ikiFaktorKod.value.trim()) { hata.value = t('giris.invalidCode'); return }
  try {
    const sonuc = await authStore.giris2fa(girisToken.value, ikiFaktorKod.value.trim())
    girisToken.value = sonuc.girisToken
    sirketler.value = sonuc.sirketler || []
    ikiFaktorAdimi.value = false
    if (sirketler.value.length === 1) {
      await sirketSecVeGirisYap(sirketler.value[0])
      return
    }
    sirketSecimAdimi.value = true
  } catch (err) { hata.value = err.response?.data?.message || t('giris.verifyFailed') }
}

const geriDon = () => { ikiFaktorAdimi.value = false; ikiFaktorKod.value = ''; girisToken.value = '' }
const tumAdimlariSifirla = () => {
  ikiFaktorAdimi.value = false
  sirketSecimAdimi.value = false
  ikiFaktorKod.value = ''
  girisToken.value = ''
  sirketler.value = []
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
.giris-form { background: var(--bg-card); border: 1px solid var(--border); border-radius: 16px; padding: 28px; box-shadow: var(--shadow); }
.form-grup { margin-bottom: 16px; }
.form-grup label { display: block; color: var(--text-secondary); font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: .5px; margin-bottom: 6px; }
.input-wrapper { position: relative; }
.input-wrapper > i { position: absolute; left: 14px; top: 50%; transform: translateY(-50%); color: var(--text-muted); font-size: 16px; z-index: 2; pointer-events: none; }
.input-wrapper :deep(.p-inputtext) { width: 100%; padding: 13px 14px 13px 40px !important; background: var(--bg-primary); border: 1px solid var(--border); border-radius: 10px; color: var(--text-primary); font-size: 15px; min-height: 48px; }
.input-wrapper :deep(.p-inputtext:focus) { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59,130,246,.2); outline: none; }
.sifre-toggle { position: absolute; right: 12px; top: 50%; transform: translateY(-50%); background: none; border: none; color: var(--text-muted); cursor: pointer; font-size: 16px; z-index: 2; padding: 4px; }
.sifre-toggle:hover { color: var(--text-primary); }
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
.iki-fa-ikon, .sirket-secim-ikon, .kurulum-ikon { width: 60px; height: 60px; margin: 0 auto 12px; border-radius: 16px; display: flex; align-items: center; justify-content: center; box-shadow: 0 8px 24px rgba(16,185,129,.3); }
.iki-fa-ikon { background: linear-gradient(135deg, #10b981, #059669); }
.sirket-secim-ikon { background: linear-gradient(135deg, #3b82f6, #1d4ed8); }
.kurulum-ikon { background: linear-gradient(135deg, #8b5cf6, #6d28d9); box-shadow: 0 8px 24px rgba(139,92,246,.3); }
.iki-fa-ikon i, .sirket-secim-ikon i, .kurulum-ikon i { font-size: 26px; color: white; }
.kurulum-iki-kolon { display: flex; gap: 10px; }
.kurulum-iki-kolon .form-grup { flex: 1; }
.kurulum-ayrac { border-top: 1px dashed var(--border); margin: 16px 0; }
.sifre-ipucu { display: block; margin-top: 6px; color: var(--text-muted); font-size: 11px; }
.iki-fa-baslik { text-align: center; color: var(--text-primary); font-size: 17px; margin: 0 0 4px; }
.iki-fa-alt { text-align: center; color: var(--text-secondary); font-size: 13px; margin: 0 0 18px; }
.geri-satir { text-align: center; margin-top: 14px; }
.geri-satir a { color: var(--text-muted); font-size: 13px; cursor: pointer; }
.geri-satir a:hover { color: var(--text-primary); }
.sirket-listesi { display: flex; flex-direction: column; gap: 8px; }
.sirket-secim-buton { display: flex; align-items: center; gap: 12px; width: 100%; padding: 14px 16px; background: var(--bg-primary); border: 2px solid var(--border); border-radius: 10px; color: var(--text-primary); font-size: 15px; cursor: pointer; transition: all .2s; }
.sirket-secim-buton:hover { border-color: #3b82f6; background: rgba(59,130,246,.05); transform: translateY(-1px); }
.sirket-mini-logo { width: 32px; height: 32px; border-radius: 8px; object-fit: cover; flex-shrink: 0; }
.sirket-mini-icon { font-size: 20px; color: var(--text-muted); flex-shrink: 0; }
.sirket-ad { flex: 1; text-align: left; font-weight: 500; }
.sirket-ok { color: var(--text-muted); font-size: 14px; }
.sirket-yok { text-align: center; color: var(--text-secondary); font-size: 14px; padding: 20px 0; }
.sifre-sifirla-panel { background: var(--bg-card); border: 1px solid var(--border); border-radius: 16px; padding: 24px; box-shadow: var(--shadow); margin-top: 16px; }
.sifirla-ust { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.sifirla-ust i { font-size: 22px; color: var(--primary-color); }
.sifirla-ust h3 { margin: 0; font-size: 16px; color: var(--text-primary); flex: 1; }
.sifirla-ust a { color: var(--text-muted); font-size: 13px; cursor: pointer; }
.sifirla-ust a:hover { color: var(--text-primary); }
.sifre-sifirla-panel p { font-size: 13px; color: var(--text-secondary); margin: 0 0 14px; }
.sifirla-input-row { display: flex; gap: 8px; }
.sifirla-input-row .p-inputtext { flex: 1; }
.sifirla-mesaj { display: block; margin-top: 10px; font-size: 12px; color: var(--green-500); }
@media (max-width: 480px) { .giris-form { padding: 20px; } .sifirla-input-row { flex-direction: column; } .kurulum-iki-kolon { flex-direction: column; gap: 0; } }
</style>
