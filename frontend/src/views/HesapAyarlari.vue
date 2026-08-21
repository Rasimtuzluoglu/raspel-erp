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
            style="margin-right: 8px"
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
            style="margin-right: 8px"
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
            style="margin-right: 8px"
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
                style="color: #10b981"
              />
              <span>2FA <strong>aktif</strong>. Hesabınız güvende.</span>
            </div>
            <div
              class="field"
              style="margin-top: 14px"
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
                <p>
                  <strong>1.</strong> Aşağıdaki gizli anahtarı (veya otpauth URI'sini) kimlik doğrulayıcı uygulamanıza
                  ekleyin:
                </p>
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
            style="margin-right: 8px"
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
                  :class="{ aktif: accentColor === c.value }"
                  :style="{ background: c.value }"
                  :title="c.name"
                  @click="applyColor(c.value)"
                />
              </div>
            </div>
          </div>
        </template>
      </Card>

      <!-- YAPAY ZEKA (AI) AYARLARI -->
      <Card class="ayar-kart ai-ayar-kart">
        <template #title>
          <div class="ai-baslik-satir">
            <div>
              <i
                class="pi pi-sparkles"
                style="margin-right: 8px; color: #8b5cf6"
              />Yapay Zeka (AI) Entegrasyonu
            </div>
            <Tag
              :value="aiDurum === 'AKTIF' ? 'AI Aktif' : 'Yapılandırılmadı'"
              :severity="aiDurum === 'AKTIF' ? 'success' : 'warn'"
            />
          </div>
        </template>
        <template #content>
          <div class="form-grid">
            <p class="ai-aciklama">
              Kendi AI API anahtarınızı girerek ERP içi akıllı sohbet asistanını ve veri sorgulama motorunu etkinleştirin.
            </p>

            <div class="field">
              <label>AI Sağlayıcı</label>
              <Dropdown
                v-model="aiForm.provider"
                :options="aiSaglayicilar"
                option-label="name"
                option-value="value"
                placeholder="Sağlayıcı seçin"
                class="w-full"
                @change="onProviderChange"
              />
            </div>

            <div class="field">
              <label>API Key</label>
              <div class="p-inputgroup w-full">
                <InputText
                  v-model="aiForm.apiKey"
                  :type="aiKeyGoster ? 'text' : 'password'"
                  placeholder="sk-... veya API Anahtarınız"
                  class="w-full"
                />
                <Button
                  :icon="aiKeyGoster ? 'pi pi-eye-slash' : 'pi pi-eye'"
                  severity="secondary"
                  outlined
                  @click="aiKeyGoster = !aiKeyGoster"
                />
              </div>
              <small
                v-if="aiMevcutMaskeliKey && !aiForm.apiKey"
                class="text-muted"
              >
                Mevcut Anahtar: <code>{{ aiMevcutMaskeliKey }}</code>
              </small>
            </div>

            <div class="field">
              <label>Model</label>
              <Dropdown
                v-model="aiForm.model"
                :options="aktifModelListesi"
                option-label="name"
                option-value="value"
                placeholder="Model seçin"
                class="w-full"
              />
            </div>

            <div class="ai-aksiyonlar">
              <Button
                label="Bağlantıyı Test Et"
                icon="pi pi-bolt"
                severity="info"
                outlined
                :loading="aiTestEdiliyor"
                :disabled="aiDurum !== 'AKTIF' && !aiForm.apiKey"
                @click="aiBaglantiTestEt"
              />
              <Button
                label="AI Ayarlarını Kaydet"
                icon="pi pi-check"
                :loading="aiKaydediliyor"
                @click="aiConfigKaydet"
              />
              <Button
                v-if="aiDurum === 'AKTIF'"
                label="Kaldır"
                icon="pi pi-trash"
                severity="danger"
                outlined
                :loading="aiKaydediliyor"
                @click="aiConfigSil"
              />
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
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { kullaniciAPI, aiConfigAPI } from '../api/index.js'
import { useAuthStore } from '../stores/authStore.js'
import { useTheme } from '../composables/useTheme.js'
import IlkZiyaretIpuclari from '../components/IlkZiyaretIpuclari.vue'

const toast = useToast()
const toastBildirim = useToastBildirim()
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

// AI Yapılandırma State
const aiSaglayicilar = [
  { name: 'OpenAI (ChatGPT)', value: 'OPENAI' },
  { name: 'Google Gemini', value: 'GOOGLE' },
  { name: 'Anthropic Claude', value: 'ANTHROPIC' }
]

const aiModeller = {
  OPENAI: [
    { name: 'GPT-4o (En Yetenekli)', value: 'gpt-4o' },
    { name: 'GPT-4o Mini (Hızlı & Ekonomik)', value: 'gpt-4o-mini' },
    { name: 'GPT-4 Turbo', value: 'gpt-4-turbo' }
  ],
  GOOGLE: [
    { name: 'Gemini 2.5 Flash (Önerilen)', value: 'gemini-2.5-flash' },
    { name: 'Gemini 1.5 Pro', value: 'gemini-1.5-pro' },
    { name: 'Gemini 1.5 Flash', value: 'gemini-1.5-flash' }
  ],
  ANTHROPIC: [
    { name: 'Claude 3.5 Sonnet (Gelişmiş Analiz)', value: 'claude-3-5-sonnet-20241022' },
    { name: 'Claude 3.5 Haiku (Süper Hızlı)', value: 'claude-3-5-haiku-20241022' },
    { name: 'Claude 3 Opus', value: 'claude-3-opus-20240229' }
  ]
}

const aiForm = ref({ provider: 'OPENAI', apiKey: '', model: 'gpt-4o' })
const aiDurum = ref('YAPILANDIRILMADI') // AKTIF / YAPILANDIRILMADI
const aiMevcutMaskeliKey = ref('')
const aiKeyGoster = ref(false)
const aiKaydediliyor = ref(false)
const aiTestEdiliyor = ref(false)

const aktifModelListesi = computed(() => {
  return aiModeller[aiForm.value.provider] || aiModeller.OPENAI
})

const onProviderChange = () => {
  const modeller = aktifModelListesi.value
  if (modeller && modeller.length > 0) {
    aiForm.value.model = modeller[0].value
  }
}

const aiConfigGetir = async () => {
  try {
    const res = await aiConfigAPI.getConfig()
    if (res.data) {
      aiDurum.value = res.data.durum || 'YAPILANDIRILMADI'
      if (res.data.provider) aiForm.value.provider = res.data.provider
      if (res.data.model) aiForm.value.model = res.data.model
      aiMevcutMaskeliKey.value = res.data.apiKey || ''
    }
  } catch {
    /* empty */
  }
}

const aiConfigKaydet = async () => {
  if (!aiForm.value.apiKey && !aiMevcutMaskeliKey.value) {
    toastBildirim.uyari('Lütfen geçerli bir API anahtarı girin')
    return
  }
  aiKaydediliyor.value = true
  try {
    const payload = {
      provider: aiForm.value.provider,
      apiKey: aiForm.value.apiKey || undefined,
      model: aiForm.value.model,
      aktif: true
    }
    const res = await aiConfigAPI.saveConfig(payload)
    if (res.data) {
      aiDurum.value = res.data.durum || 'AKTIF'
      aiMevcutMaskeliKey.value = res.data.apiKey || ''
      aiForm.value.apiKey = ''
    }
    toastBildirim.basarili('Yapay Zeka API ayarları kaydedildi')
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Ayarlar kaydedilemedi')
  } finally {
    aiKaydediliyor.value = false
  }
}

const aiBaglantiTestEt = async () => {
  aiTestEdiliyor.value = true
  try {
    const res = await aiConfigAPI.testConnection()
    if (res.data && res.data.status === 'SUCCESS') {
      toast.add({
        severity: 'success',
        summary: 'Bağlantı Başarılı',
        detail: res.data.message || 'AI API bağlantısı başarıyla doğrulandı.',
        life: 3000
      })
    } else {
      toast.add({
        severity: 'warn',
        summary: 'Bağlantı Uyarısı',
        detail: res.data?.message || 'Bağlantı kurulamadı',
        life: 4000
      })
    }
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'API anahtarı doğrulanamadı')
  } finally {
    aiTestEdiliyor.value = false
  }
}

const aiConfigSil = async () => {
  aiKaydediliyor.value = true
  try {
    await aiConfigAPI.deleteConfig()
    aiDurum.value = 'YAPILANDIRILMADI'
    aiMevcutMaskeliKey.value = ''
    aiForm.value = { provider: 'OPENAI', apiKey: '', model: 'gpt-4o' }
    toastBildirim.basarili('AI yapılandırması kaldırıldı')
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Yapılandırma silinemedi')
  } finally {
    aiKaydediliyor.value = false
  }
}

onMounted(async () => {
  initTheme()
  const k = authStore.kullanici
  if (k) {
    profilForm.value = {
      displayName: k.displayName || '',
      companyName: k.companyName || '',
      avatarUrl: k.avatarUrl || ''
    }
  }
  try {
    const r = await kullaniciAPI.ben()
    const ben = r.data
    twoFactorDurum.value = ben.twoFactorEnabled ? 'ACIK' : 'KAPALI'
  } catch {
    /* empty */
  }
  await aiConfigGetir()
})

const profilKaydet = async () => {
  kaydediliyor.value = true
  try {
    await kullaniciAPI.beniGuncelle(profilForm.value)
    await authStore.kullaniciGuncelle()
    toastBildirim.basarili('Profil güncellendi')
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Profil güncellenemedi')
  }
  kaydediliyor.value = false
}

const sifreKaydet = async () => {
  if (!sifreForm.value.mevcutSifre || !sifreForm.value.yeniSifre) {
    toastBildirim.uyari('Tüm alanları doldurun')
    return
  }
  if (sifreForm.value.yeniSifre !== sifreForm.value.yeniSifreTekrar) {
    toastBildirim.hata('Yeni şifreler eşleşmiyor')
    return
  }
  kaydediliyor.value = true
  try {
    await kullaniciAPI.sifreDegistir({ mevcutSifre: sifreForm.value.mevcutSifre, yeniSifre: sifreForm.value.yeniSifre })
    toastBildirim.basarili('Şifre güncellendi')
    sifreForm.value = { mevcutSifre: '', yeniSifre: '', yeniSifreTekrar: '' }
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Şifre değiştirilemedi')
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
    toastBildirim.hata(err?.response?.data?.message || '2FA kurulumu başlatılamadı')
  }
  kaydediliyor.value = false
}

const ikiFakAktifEt = async () => {
  if (!dogrulamaKodu.value) {
    toastBildirim.uyari('Doğrulama kodunu girin')
    return
  }
  kaydediliyor.value = true
  try {
    await kullaniciAPI.enable2fa({ code: dogrulamaKodu.value })
    twoFactorDurum.value = 'ACIK'
    kurulumData.value = null
    dogrulamaKodu.value = ''
    toastBildirim.basarili('2FA aktif edildi')
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Kod geçersiz')
  }
  kaydediliyor.value = false
}

const ikiFakapat = async () => {
  if (!kapatmaKodu.value) {
    toastBildirim.uyari('Doğrulama kodunu girin')
    return
  }
  kaydediliyor.value = true
  try {
    await kullaniciAPI.disable2fa({ code: kapatmaKodu.value })
    twoFactorDurum.value = 'KAPALI'
    kapatmaKodu.value = ''
    toastBildirim.basarili('2FA kapatıldı')
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Kod geçersiz')
  }
  kaydediliyor.value = false
}

const kopyala = async (text) => {
  try {
    await navigator.clipboard.writeText(text)
    toast.add({ severity: 'success', summary: 'Kopyalandı', detail: 'Gizli anahtar panoya kopyalandı', life: 2000 })
  } catch {
    /* empty */
  }
}
</script>

<style scoped>
.hesap-ayarlari {
  padding: 0;
}
.sayfa-baslik {
  margin-bottom: 24px;
}
.ayarlar-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
}
.ayar-kart :deep(.p-card-content) {
  padding-top: 8px;
}
.form-grid {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}
.w-full {
  width: 100%;
}
.kod-satir {
  display: flex;
  gap: 8px;
  align-items: center;
}
.iki-fa-acik .iki-fa-baslik {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}
.iki-fa-aciklama, .ai-aciklama {
  color: var(--text-secondary);
  margin-bottom: 8px;
  font-size: 13px;
  line-height: 1.5;
}
.ai-baslik-satir {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.ai-aksiyonlar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 8px;
}
.secret-kutu {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(0, 0, 0, 0.2);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 8px 12px;
  margin: 8px 0;
}
.secret-kutu code {
  font-family: monospace;
  font-size: 15px;
  letter-spacing: 2px;
  flex: 1;
}
.otpauth-satir {
  color: var(--text-muted);
  word-break: break-all;
}
.tema-butonlari {
  display: flex;
  gap: 8px;
}
.renk-secenekleri {
  display: flex;
  gap: 12px;
  padding-top: 4px;
}
.renk-dot {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid transparent;
  cursor: pointer;
  transition: all 0.15s;
}
.renk-dot:hover {
  transform: scale(1.12);
}
.renk-dot.aktif {
  border-color: #fff;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.4);
}
</style>

