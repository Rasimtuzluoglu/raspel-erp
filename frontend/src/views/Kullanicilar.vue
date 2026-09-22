<template>
  <div class="kullanicilar-container">
    <h1 class="page-title">
      {{ t('kullanicilar.title') }}
    </h1>

    <Toolbar class="toolbar">
      <template #start>
        <Button
          :label="t('kullanicilar.yeniKullanici')"
          icon="pi pi-plus"
          class="p-button-success"
          @click="openDialog"
        />
      </template>
    </Toolbar>

    <div
      v-if="loading"
      class="loading"
    >
      <p><i class="pi pi-spin pi-spinner" /> {{ t('common.loading') }}</p>
    </div>

    <div
      v-if="!loading"
      class="kullanici-grid"
    >
      <div
        v-for="u in kullanicilar"
        :key="u.id"
        class="kullanici-kart"
      >
        <div class="kart-ust">
          <div class="avatar">
            <img
              v-if="u.avatarUrl"
              :src="u.avatarUrl"
              :alt="u.displayName"
              loading="lazy"
            >
            <span
              v-else
              class="avatar-yedek"
            >{{ u.displayName?.charAt(0) }}</span>
          </div>
          <div class="kart-bilgi">
            <h3>{{ u.displayName }}</h3>
            <span class="kullanici-ad">@{{ u.username }}</span>
          </div>
          <span :class="['rol-badge', u.role?.toLowerCase()]">{{ u.role }}</span>
        </div>
        <div class="kart-alt">
          <span
            v-if="u.companyName"
            class="sirket"
          ><i class="pi pi-building" /> {{ u.companyName }}</span>
          <span
            class="durum"
            :class="u.active ? 'aktif' : 'pasif'"
          >
            {{ u.active ? t('kullanicilar.aktif') : t('kullanicilar.pasif') }}
          </span>
        </div>
        <div class="kart-islem">
          <Button
            v-tooltip.top="t('kullanicilar.sifreSifirlaBaslik')"
            icon="pi pi-key"
            class="p-button-rounded p-button-sm islem-btn sifirla"
            @click="sifreSifirlaAc(u)"
          />
          <Button
            v-tooltip.top="t('common.edit')"
            icon="pi pi-pencil"
            class="p-button-rounded p-button-sm islem-btn duzenle"
            @click="editKullanici(u)"
          />
          <Button
            v-tooltip.top="t('common.delete')"
            icon="pi pi-trash"
            class="p-button-rounded p-button-sm islem-btn sil"
            :disabled="u.id === authStore?.kullanici?.id"
            @click="confirmDel(u.id)"
          />
        </div>
      </div>
      <EmptyState
        v-if="kullanicilar && kullanicilar.length === 0"
        :message="t('kullanicilar.bosDurum')"
        :sub-message="t('kullanicilar.bosDurumIpucu')"
        icon="pi pi-user-plus"
        :action-label="t('kullanicilar.yeniKullanici')"
        action-icon="pi pi-plus"
        class="full-width"
        @action="openDialog"
      />
    </div>

    <Dialog
      v-model:visible="showDialog"
      :header="editingId ? t('kullanicilar.duzenle') : t('kullanicilar.yeniKullanici')"
      :modal="true"
      style="width: 500px"
    >
      <div class="form-grup">
        <label>{{ t('kullanicilar.kullaniciAdi') }}</label>
        <InputText
          v-model="form.username"
          :placeholder="t('kullanicilar.kullaniciAdiPlaceholder')"
          class="w-full"
          :disabled="!!editingId"
        />
      </div>
      <div class="form-grup">
        <label>{{ t('kullanicilar.gorunenAd') }}</label>
        <InputText
          v-model="form.displayName"
          :placeholder="t('kullanicilar.gorunenAdPlaceholder')"
          class="w-full"
        />
      </div>
      <div class="form-grup">
        <label>{{ t('kullanicilar.email') }}</label>
        <InputText
          v-model="form.email"
          type="email"
          :placeholder="t('kullanicilar.emailPlaceholder')"
          class="w-full"
        />
      </div>
      <div class="form-grup">
        <label>{{ editingId ? t('kullanicilar.yeniSifre') : t('kullanicilar.sifre') }}</label>
        <InputText
          v-model="form.password"
          type="password"
          placeholder="••••••"
          class="w-full"
        />
      </div>
      <div class="form-grup">
        <label>{{ t('kullanicilar.avatar') }}</label>
        <div class="avatar-upload">
          <div class="avatar-upload-preview">
            <img
              v-if="avatarPreview"
              :src="avatarPreview"
              class="avatar-preview-img"
              loading="lazy"
            >
            <span
              v-else
              class="avatar-preview-yedek"
            >{{ (form.displayName || '?').charAt(0) }}</span>
          </div>
          <div class="avatar-upload-inputs">
            <input
              ref="avatarInput"
              type="file"
              accept="image/*"
              style="display: none"
              @change="avatarDosyaSec"
            >
            <Button
              :label="t('kullanicilar.dosyaSec')"
              icon="pi pi-upload"
              size="small"
              class="p-button-outlined"
              @click="$refs.avatarInput.click()"
            />
            <span
              v-if="avatarDosyaAdi"
              class="avatar-dosya-adi"
            >{{ avatarDosyaAdi }}</span>
            <span
              v-else
              class="avatar-veya"
            >{{ t('kullanicilar.veyaUrlGirin') }}</span>
            <InputText
              v-model="form.avatarUrl"
              placeholder="https://..."
              class="w-full"
            />
          </div>
        </div>
      </div>
      <div class="form-grup">
        <label>{{ t('kullanicilar.sirketAdi') }}</label>
        <Dropdown
          v-model="form.companyName"
          :options="sirketListesi"
          option-label="ad"
          option-value="ad"
          :placeholder="t('kullanicilar.sirketAdi')"
          editable
          class="w-full"
        />
      </div>
      <div class="form-grup">
        <label>{{ t('kullanicilar.yetkiliFirmalar') }}</label>
        <MultiSelect
          v-model="form.sirketIds"
          :options="sirketListesi"
          option-label="ad"
          option-value="id"
          :placeholder="t('kullanicilar.firmaSeciniz')"
          class="w-full"
          display="chip"
        />
      </div>
      <div class="form-row">
        <div class="form-grup">
          <label>{{ t('kullanicilar.rol') }}</label>
          <Dropdown
            v-model="form.role"
            :options="[
              { label: t('kullanicilar.rolAdmin'), value: 'ADMIN' },
              { label: t('kullanicilar.rolKullanici'), value: 'USER' },
              { label: t('kullanicilar.rolMuhasebe'), value: 'MUHASEBE' },
              { label: t('kullanicilar.rolSofor'), value: 'DRIVER' }
            ]"
            option-label="label"
            option-value="value"
            class="w-full"
          />
        </div>
        <div
          v-if="editingId"
          class="form-grup"
        >
          <label>{{ t('common.status') }}</label>
          <Dropdown
            v-model="form.active"
            :options="[
              { label: t('kullanicilar.aktif'), value: true },
              { label: t('kullanicilar.pasif'), value: false }
            ]"
            option-label="label"
            option-value="value"
            class="w-full"
          />
        </div>
        <div class="form-grup saha-grup">
          <label>{{ t('kullanicilar.sahaKullanicisi') }}</label>
          <ToggleSwitch v-model="form.sahaKullanici" />
          <small class="saha-ipucu">{{ t('kullanicilar.sahaIpucu') }}</small>
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="closeDialog"
        />
        <Button
          :label="editingId ? t('kullanicilar.guncelle') : t('common.save')"
          icon="pi pi-check"
          :loading="saving"
          @click="save"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="showSifirlaDialog"
      :header="t('kullanicilar.sifreSifirlaBaslik')"
      :modal="true"
      style="width: 440px"
    >
      <p class="sifirla-aciklama">
        <strong>{{ sifirlaKullanici?.displayName }}</strong> (@{{ sifirlaKullanici?.username }})
      </p>
      <div class="form-grup">
        <label>{{ t('kullanicilar.yeniSifre') }}</label>
        <Password
          v-model="sifirlaForm.yeniSifre"
          :feedback="true"
          toggle-mask
          class="w-full"
          :disabled="sifirlaSaving"
        />
      </div>
      <div class="form-grup">
        <label>{{ t('kullanicilar.yeniSifreTekrar') }}</label>
        <Password
          v-model="sifirlaForm.tekrar"
          :feedback="false"
          toggle-mask
          class="w-full"
          :disabled="sifirlaSaving"
        />
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="showSifirlaDialog = false"
        />
        <Button
          :label="t('kullanicilar.sifreSifirlaBaslik')"
          icon="pi pi-key"
          :loading="sifirlaSaving"
          @click="sifreSifirlaKaydet"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { unwrapList } from '../api/utils/unwrap.js'
import { useI18n } from 'vue-i18n'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { useAuthStore } from '../stores/authStore.js'
import apiClient, { kullaniciAPI, sirketAPI } from '../api/index.js'
import EmptyState from '../components/EmptyState.vue'

const { t } = useI18n()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const authStore = useAuthStore()

const kullanicilar = ref([])
const sirketListesi = ref([])
const loading = ref(false)
const saving = ref(false)
const showDialog = ref(false)
const editingId = ref(null)

const form = ref({
  username: '',
  displayName: '',
  email: '',
  password: '',
  avatarUrl: '',
  companyName: '',
  sirketIds: [],
  role: 'USER',
  active: true,
  sahaKullanici: false
})

const avatarInput = ref(null)
const avatarDosyaAdi = ref('')
const avatarDosya = ref(null)
const avatarYukleniyor = ref(false)

const avatarPreview = computed(() => {
  if (avatarDosya.value) return URL.createObjectURL(avatarDosya.value)
  return form.value.avatarUrl || null
})

const avatarDosyaSec = (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  avatarDosya.value = file
  avatarDosyaAdi.value = file.name
}

onMounted(async () => {
  loading.value = true
  try {
    const [r, sR] = await Promise.all([kullaniciAPI.getAll({ size: 500 }), sirketAPI.getAktif()])
    kullanicilar.value = unwrapList(r)
    sirketListesi.value = sR.data || []
  } catch {
    toastBildirim.hata(t('kullanicilar.yuklemeHatasi'))
  } finally {
    loading.value = false
  }
})

const openDialog = () => {
  editingId.value = null
  form.value = {
    username: '',
    displayName: '',
    email: '',
    password: '',
    avatarUrl: '',
    companyName: '',
    sirketIds: [],
    role: 'USER',
    active: true,
    sahaKullanici: false
  }
  showDialog.value = true
}

const editKullanici = (u) => {
  editingId.value = u.id
  form.value = {
    username: u.username,
    displayName: u.displayName,
    email: u.email || '',
    password: '',
    avatarUrl: u.avatarUrl || '',
    companyName: u.companyName || '',
    sirketIds: u.sirketIds || (u.sirketId ? [u.sirketId] : []),
    role: u.role || 'USER',
    active: u.active !== false,
    sahaKullanici: u.sahaKullanici === true
  }
  showDialog.value = true
}

const closeDialog = () => {
  showDialog.value = false
}

const avatarYukle = async () => {
  if (!avatarDosya.value) return
  avatarYukleniyor.value = true
  try {
    const fd = new FormData()
    fd.append('file', avatarDosya.value)
    const r = await apiClient.post('/upload/avatar', fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    form.value.avatarUrl = r.data.url
    avatarDosya.value = null
    avatarDosyaAdi.value = ''
  } catch (e) {
    toastBildirim.hata(t('kullanicilar.avatarYuklemeHatasi'))
    throw e
  } finally {
    avatarYukleniyor.value = false
  }
}

const save = async () => {
  if (!form.value.displayName.trim()) {
    toastBildirim.uyari(t('kullanicilar.gorunenAdGiriniz'))
    return
  }
  if (!editingId.value && !form.value.username.trim()) {
    toastBildirim.uyari(t('kullanicilar.kullaniciAdiGiriniz'))
    return
  }
  saving.value = true
  try {
    if (avatarDosya.value) await avatarYukle()
    if (editingId.value) {
      await kullaniciAPI.update(editingId.value, form.value)
      if (editingId.value === authStore?.kullanici?.id) await authStore?.kullaniciGuncelle()
      toastBildirim.basarili(t('kullanicilar.guncellendi'))
    } else {
      if (!form.value.password) {
        toastBildirim.uyari(t('kullanicilar.sifreGiriniz'))
        return
      }
      await kullaniciAPI.create(form.value)
      toastBildirim.basarili(t('kullanicilar.olusturuldu'))
    }
    closeDialog()
    const r = await kullaniciAPI.getAll({ size: 500 })
    kullanicilar.value = unwrapList(r)
  } catch (err) {
    toastBildirim.hata(err.response?.data?.message || t('kullanicilar.islemBasarisiz'))
  } finally {
    saving.value = false
  }
}

const showSifirlaDialog = ref(false)
const sifirlaKullanici = ref(null)
const sifirlaSaving = ref(false)
const sifirlaForm = ref({ yeniSifre: '', tekrar: '' })

const sifreSifirlaAc = (u) => {
  sifirlaKullanici.value = u
  sifirlaForm.value = { yeniSifre: '', tekrar: '' }
  showSifirlaDialog.value = true
}

const sifreSifirlaKaydet = async () => {
  if (!sifirlaForm.value.yeniSifre || sifirlaForm.value.yeniSifre.length < 8) {
    toastBildirim.uyari(t('kullanicilar.sifreEnAz8'))
    return
  }
  if (sifirlaForm.value.yeniSifre !== sifirlaForm.value.tekrar) {
    toastBildirim.uyari(t('kullanicilar.sifrelerEslesmiyor'))
    return
  }
  sifirlaSaving.value = true
  try {
    await kullaniciAPI.update(sifirlaKullanici.value.id, {
      username: sifirlaKullanici.value.username,
      displayName: sifirlaKullanici.value.displayName,
      password: sifirlaForm.value.yeniSifre
    })
    toastBildirim.basarili(t('kullanicilar.sifreSifirlandi'))
    showSifirlaDialog.value = false
  } catch (err) {
    toastBildirim.hata(err.response?.data?.message || t('kullanicilar.islemBasarisiz'))
  } finally {
    sifirlaSaving.value = false
  }
}

const confirmDel = (id) => {
  confirm.require({
    message: t('kullanicilar.silmeOnayMesaji'),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    rejectProps: { label: t('common.vazgec'), severity: 'secondary', outlined: true, size: 'small' },
    acceptProps: { label: t('common.evetSil'), severity: 'danger', size: 'small' },
    accept: async () => {
      try {
        await kullaniciAPI.delete(id)
        kullanicilar.value = kullanicilar.value.filter((u) => u.id !== id)
        toastBildirim.basarili(t('kullanicilar.silindi'))
      } catch {
        toastBildirim.hata(t('kullanicilar.silmeBasarisiz'))
      }
    }
  })
}
</script>

<style scoped>
.kullanicilar-container {
  padding: 0;
  max-width: 100%;
}
h1 {
  color: var(--text-primary);
  margin-bottom: 20px;
  font-size: 28px;
}
.toolbar {
  margin-bottom: 20px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 15px;
}
.loading {
  text-align: center;
  padding: 40px;
  color: #94a3b8;
}
.kullanici-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(340px, 100%), 1fr));
  gap: 16px;
}
.kullanici-kart {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  transition: all 0.3s ease;
}
.kullanici-kart:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.3);
  border-color: var(--accent-border);
}
.kart-ust {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;
}
.avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  border: 2px solid var(--accent-border);
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
  background: linear-gradient(135deg, var(--accent), var(--accent-hover));
  color: white;
  font-weight: 700;
  font-size: 18px;
}
.kart-bilgi {
  flex: 1;
  min-width: 0;
}
.kart-bilgi h3 {
  margin: 0;
  font-size: 16px;
  color: var(--text-primary);
}
.kullanici-ad {
  font-size: 12px;
  color: var(--text-muted);
}
.rol-badge {
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  flex-shrink: 0;
}
.rol-badge.admin {
  background: var(--accent-soft-strong);
  color: var(--accent);
}
.rol-badge.user {
  background: rgba(34, 197, 94, 0.15);
  color: #4ade80;
}
.rol-badge.driver {
  background: rgba(245, 158, 11, 0.15);
  color: #fbbf24;
}
.rol-badge.muhasebe {
  background: rgba(168, 85, 247, 0.15);
  color: #c084fc;
}
.kart-alt {
  display: flex;
  justify-content: space-between;
  margin-bottom: 14px;
  font-size: 13px;
  flex-shrink: 0;
}
.sirket {
  color: #94a3b8;
  display: flex;
  align-items: center;
  gap: 5px;
}
.durum {
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
}
.durum.aktif {
  background: rgba(34, 197, 94, 0.15);
  color: #4ade80;
}
.durum.pasif {
  background: rgba(239, 68, 68, 0.15);
  color: #f87171;
}
.kart-islem {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  padding-top: 14px;
  margin-top: auto;
  border-top: 1px solid rgba(148, 163, 184, 0.1);
  flex-shrink: 0;
}
.islem-btn {
  width: 32px !important;
  height: 32px !important;
  border-radius: 8px !important;
  border: none !important;
}
.islem-btn.duzenle {
  background: var(--accent-soft) !important;
  color: var(--accent) !important;
}
.islem-btn.duzenle:hover {
  background: var(--accent-soft-strong) !important;
}
.islem-btn.sil {
  background: rgba(239, 68, 68, 0.12) !important;
  color: #f87171 !important;
}
.islem-btn.sil:hover {
  background: rgba(239, 68, 68, 0.25) !important;
}
.islem-btn.sifirla {
  background: rgba(245, 158, 11, 0.12) !important;
  color: #fbbf24 !important;
}
.islem-btn.sifirla:hover {
  background: rgba(245, 158, 11, 0.25) !important;
}
.sifirla-aciklama {
  margin-bottom: 16px;
  color: var(--text-muted);
  font-size: 14px;
}
.form-grup {
  margin-bottom: 18px;
}
.form-grup label {
  display: block;
  margin-bottom: 6px;
  font-weight: 600;
  color: #94a3b8;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.form-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 15px;
}
.w-full {
  width: 100% !important;
}
.full-width {
  grid-column: 1/-1;
}
.avatar-upload {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}
.avatar-upload-preview {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  border: 2px solid var(--accent-border);
}
.avatar-preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.avatar-preview-yedek {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--accent), var(--accent-hover));
  color: white;
  font-weight: 700;
  font-size: 20px;
}
.avatar-upload-inputs {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.avatar-dosya-adi {
  font-size: 12px;
  color: var(--text-muted);
}
.avatar-veya {
  font-size: 11px;
  color: var(--text-muted);
  text-align: center;
}
</style>
