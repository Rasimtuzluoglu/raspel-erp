<template>
  <div class="hesap-ayarlari">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        Hesap Ayarları
      </h1>
    </div>

    <IlkZiyaretIpuclari
      anahtar="hesap-ayarlari"
      baslik="Hesap Ayarları"
      metin="Profil bilgilerinizi güncelleyin, şifrenizi değiştirin ve iki faktörlü doğrulama (2FA) ile hesabınızı güvenceye alın."
    />

    <div class="ayarlar-grid">
      <!-- PROFİL -->
      <Card class="ayar-kart">
        <template #title>
          <i
            class="pi pi-user"
            style="margin-right:8px"
          />Profil
        </template>
        <template #content>
          <div class="form-grid">
            <div class="field">
              <label>Kullanıcı Adı</label><InputText
                :model-value="kullanici?.username"
                disabled
                class="w-full"
              />
            </div>
            <div class="field">
              <label>Görünen Ad</label><InputText
                v-model="profilForm.displayName"
                class="w-full"
              />
            </div>
            <div class="field">
              <label>Firma Adı</label><InputText
                v-model="profilForm.companyName"
                class="w-full"
              />
            </div>
            <div class="field">
              <label>Avatar URL</label><InputText
                v-model="profilForm.avatarUrl"
                class="w-full"
              />
            </div>
            <Button
              label="Profili Kaydet"
              icon="pi pi-check"
              :loading="kaydediliyor"
              @click="profilKaydet"
            />
          </div>
        </template>
      </Card>

      <!-- ŞİFRE -->
      <Card class="ayar-kart">
        <template #title>
          <i
            class="pi pi-lock"
            style="margin-right:8px"
          />Şifre Değiştir
        </template>
        <template #content>
          <div class="form-grid">
            <div class="field">
              <label>Mevcut Şifre</label><InputText
                v-model="sifreForm.mevcutSifre"
                type="password"
                class="w-full"
              />
            </div>
            <div class="field">
              <label>Yeni Şifre</label><InputText
                v-model="sifreForm.yeniSifre"
                type="password"
                class="w-full"
              />
            </div>
            <div class="field">
              <label>Yeni Şifre (Tekrar)</label><InputText
                v-model="sifreForm.yeniSifreTekrar"
                type="password"
                class="w-full"
              />
            </div>
            <Button
              label="Şifreyi Güncelle"
              icon="pi pi-key"
              :loading="kaydediliyor"
              @click="sifreKaydet"
            />
          </div>
        </template>
      </Card>

      <!-- 2FA -->
      <Card class="ayar-kart">
        <template #title>
          <i
            class="pi pi-shield"
            style="margin-right:8px"
          />İki Faktörlü Doğrulama (2FA)
        </template>
        <template #content>
          <div
            v-if="twoFactorDurum === 'ACIK'"
            class="iki-fa-acik"
          >
            <div class="iki-fa-baslik">
              <i
                class="pi pi-check-circle"
                style="color:#10b981"
              />
              <span>2FA <strong>aktif</strong>. Hesabınız güvende.</span>
            </div>
            <div
              class="field"
              style="margin-top:14px"
            >
              <label>Kapatmak için doğrulama kodu</label>
              <div class="kod-satir">
                <InputText
                  v-model="kapatmaKodu"
                  class="w-full"
                  placeholder="6 haneli kod"
                />
                <Button
                  label="2FA'yı Kapat"
                  icon="pi pi-shield"
                  severity="danger"
                  outlined
                  :loading="kaydediliyor"
                  @click="ikiFakapat"
                />
              </div>
            </div>
          </div>

          <div v-else>
            <template v-if="!kurulumData">
              <p class="iki-fa-aciklama">
                Google Authenticator / Authy gibi bir uygulama ile girişlerinize ikinci bir güvenlik katmanı ekleyin.
              </p>
              <Button
                label="2FA Kur"
                icon="pi pi-qrcode"
                :loading="kaydediliyor"
                @click="kurulumBaslat"
              />
            </template>
            <template v-else>
              <div class="iki-fa-kurulum">
                <p><strong>1.</strong> Aşağıdaki gizli anahtarı (veya otpauth URI'sini) kimlik doğrulayıcı uygulamanıza ekleyin:</p>
                <div class="secret-kutu">
                  <code>{{ kurulumData.secret }}</code>
                  <Button
                    icon="pi pi-copy"
                    class="p-button-rounded p-button-text"
                    @click="kopyala(kurulumData.secret)"
                  />
                </div>
                <p class="otpauth-satir">
                  <small>{{ kurulumData.qrCodeUri }}</small>
                </p>
                <p><strong>2.</strong> Uygulamanın ürettiği 6 haneli kodu girin:</p>
                <div class="kod-satir">
                  <InputText
                    v-model="dogrulamaKodu"
                    class="w-full"
                    placeholder="6 haneli kod"
                  />
                  <Button
                    label="Doğrula ve Aktif Et"
                    icon="pi pi-check"
                    :loading="kaydediliyor"
                    @click="ikiFakAktifEt"
                  />
                </div>
              </div>
            </template>
          </div>
        </template>
      </Card>

      <!-- GÖRÜNÜM -->
      <Card class="ayar-kart">
        <template #title>
          <i
            class="pi pi-palette"
            style="margin-right:8px"
          />Görünüm
        </template>
        <template #content>
          <div class="form-grid">
            <div class="field">
              <label>Tema Modu</label>
              <div class="tema-butonlari">
                <Button
                  label="Açık"
                  icon="pi pi-sun"
                  :severity="!isDark ? 'primary' : 'secondary'"
                  :outlined="isDark"
                  @click="applyMode('light')"
                />
                <Button
                  label="Koyu"
                  icon="pi pi-moon"
                  :severity="isDark ? 'primary' : 'secondary'"
                  :outlined="!isDark"
                  @click="applyMode('dark')"
                />
              </div>
            </div>
            <div class="field">
              <label>Vurgu Rengi</label>
              <div class="renk-secenekleri">
                <button
                  v-for="c in renkler"
                  :key="c.value"
                  class="renk-dot"
                  :class="{ 'aktif': accentColor === c.value }"
                  :style="{ background: c.value }"
                  :title="c.name"
                  @click="applyColor(c.value)"
                />
              </div>
            </div>
          </div>
        </template>
      </Card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { kullaniciAPI } from '../api/index.js'
import { useAuthStore } from '../stores/authStore.js'
import { useTheme } from '../composables/useTheme.js'
import IlkZiyaretIpuclari from '../components/IlkZiyaretIpuclari.vue'

const toast = useToast()
const authStore = useAuthStore()
const { isDark, accentColor, applyMode, applyColor, initTheme } = useTheme()

const renkler = [
  { name: 'Okyanus Mavisi', value: '#3b82f6' },
  { name: 'Zümrüt Yeşil', value: '#10b981' },
  { name: 'Asil Mor', value: '#8b5cf6' },
  { name: 'Sıcak Amber', value: '#f59e0b' }
]

const kullanici = computed(() => authStore.kullanici)
const kaydediliyor = ref(false)

const profilForm = ref({ displayName: '', companyName: '', avatarUrl: '' })
const sifreForm = ref({ mevcutSifre: '', yeniSifre: '', yeniSifreTekrar: '' })

const twoFactorDurum = ref('KAPALI') // ACIK / KAPALI / KURULUM
const kurulumData = ref(null)
const dogrulamaKodu = ref('')
const kapatmaKodu = ref('')

onMounted(async () => {
  initTheme()
  const k = authStore.kullanici
  if (k) {
    profilForm.value = { displayName: k.displayName || '', companyName: k.companyName || '', avatarUrl: k.avatarUrl || '' }
  }
  try {
    const r = await kullaniciAPI.ben()
    const ben = r.data
    twoFactorDurum.value = ben.twoFactorEnabled ? 'ACIK' : 'KAPALI'
  } catch { /* empty */ }
})

const profilKaydet = async () => {
  kaydediliyor.value = true
  try {
    await kullaniciAPI.beniGuncelle(profilForm.value)
    await authStore.kullaniciGuncelle()
    toast.add({ severity: 'success', summary: 'Başarılı', detail: 'Profil güncellendi', life: 3000 })
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || 'Profil güncellenemedi', life: 5000 })
  }
  kaydediliyor.value = false
}

const sifreKaydet = async () => {
  if (!sifreForm.value.mevcutSifre || !sifreForm.value.yeniSifre) {
    toast.add({ severity: 'warn', summary: 'Uyarı', detail: 'Tüm alanları doldurun', life: 3000 }); return
  }
  if (sifreForm.value.yeniSifre !== sifreForm.value.yeniSifreTekrar) {
    toast.add({ severity: 'error', summary: 'Hata', detail: 'Yeni şifreler eşleşmiyor', life: 5000 }); return
  }
  kaydediliyor.value = true
  try {
    await kullaniciAPI.sifreDegistir({ mevcutSifre: sifreForm.value.mevcutSifre, yeniSifre: sifreForm.value.yeniSifre })
    toast.add({ severity: 'success', summary: 'Başarılı', detail: 'Şifre güncellendi', life: 3000 })
    sifreForm.value = { mevcutSifre: '', yeniSifre: '', yeniSifreTekrar: '' }
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || 'Şifre değiştirilemedi', life: 5000 })
  }
  kaydediliyor.value = false
}

const kurulumBaslat = async () => {
  kaydediliyor.value = true
  try {
    const r = await kullaniciAPI.setup2fa()
    kurulumData.value = r.data
    twoFactorDurum.value = 'KURULUM'
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || '2FA kurulumu başlatılamadı', life: 5000 })
  }
  kaydediliyor.value = false
}

const ikiFakAktifEt = async () => {
  if (!dogrulamaKodu.value) {
    toast.add({ severity: 'warn', summary: 'Uyarı', detail: 'Doğrulama kodunu girin', life: 3000 }); return
  }
  kaydediliyor.value = true
  try {
    await kullaniciAPI.enable2fa({ code: dogrulamaKodu.value })
    twoFactorDurum.value = 'ACIK'
    kurulumData.value = null
    dogrulamaKodu.value = ''
    toast.add({ severity: 'success', summary: 'Başarılı', detail: '2FA aktif edildi', life: 3000 })
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || 'Kod geçersiz', life: 5000 })
  }
  kaydediliyor.value = false
}

const ikiFakapat = async () => {
  if (!kapatmaKodu.value) {
    toast.add({ severity: 'warn', summary: 'Uyarı', detail: 'Doğrulama kodunu girin', life: 3000 }); return
  }
  kaydediliyor.value = true
  try {
    await kullaniciAPI.disable2fa({ code: kapatmaKodu.value })
    twoFactorDurum.value = 'KAPALI'
    kapatmaKodu.value = ''
    toast.add({ severity: 'success', summary: 'Başarılı', detail: '2FA kapatıldı', life: 3000 })
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || 'Kod geçersiz', life: 5000 })
  }
  kaydediliyor.value = false
}

const kopyala = async (text) => {
  try {
    await navigator.clipboard.writeText(text)
    toast.add({ severity: 'success', summary: 'Kopyalandı', detail: 'Gizli anahtar panoya kopyalandı', life: 2000 })
  } catch { /* empty */ }
}
</script>

<style scoped>
.hesap-ayarlari { padding: 0; }
.sayfa-baslik { margin-bottom: 24px; }
.ayarlar-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(340px, 1fr)); gap: 20px; }
.ayar-kart :deep(.p-card-content) { padding-top: 8px; }
.form-grid { display: flex; flex-direction: column; gap: 14px; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field label { font-size: 13px; font-weight: 600; color: var(--text-secondary); }
.w-full { width: 100%; }
.kod-satir { display: flex; gap: 8px; align-items: center; }
.iki-fa-acik .iki-fa-baslik { display: flex; align-items: center; gap: 8px; font-size: 14px; }
.iki-fa-aciklama { color: var(--text-secondary); margin-bottom: 14px; }
.secret-kutu {
  display: flex; align-items: center; gap: 8px; background: rgba(0,0,0,0.2);
  border: 1px solid var(--border); border-radius: 8px; padding: 8px 12px; margin: 8px 0;
}
.secret-kutu code { font-family: monospace; font-size: 15px; letter-spacing: 2px; flex: 1; }
.otpauth-satir { color: var(--text-muted); word-break: break-all; }
.tema-butonlari { display: flex; gap: 8px; }
.renk-secenekleri { display: flex; gap: 12px; padding-top: 4px; }
.renk-dot {
  width: 32px; height: 32px; border-radius: 50%; border: 2px solid transparent;
  cursor: pointer; transition: all 0.15s;
}
.renk-dot:hover { transform: scale(1.12); }
.renk-dot.aktif { border-color: #fff; box-shadow: 0 0 0 3px rgba(59,130,246,0.4); }
</style>
