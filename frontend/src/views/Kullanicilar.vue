<template>
  <div class="kullanicilar-container">
    <h1>Kullanıcı Yönetimi</h1>

    <Toolbar class="toolbar">
      <template #start>
        <Button label="Yeni Kullanıcı" icon="pi pi-plus" @click="openDialog" class="p-button-success" />
      </template>
    </Toolbar>

    <div class="loading" v-if="loading"><p><i class="pi pi-spin pi-spinner"></i> Yükleniyor...</p></div>

    <div class="kullanici-grid" v-if="!loading">
      <div v-for="u in kullanicilar" :key="u.id" class="kullanici-kart">
        <div class="kart-ust">
          <div class="avatar">
            <img v-if="u.avatarUrl" :src="u.avatarUrl" :alt="u.displayName" />
            <span v-else class="avatar-yedek">{{ u.displayName?.charAt(0) }}</span>
          </div>
          <div class="kart-bilgi">
            <h3>{{ u.displayName }}</h3>
            <span class="kullanici-ad">@{{ u.username }}</span>
          </div>
          <span :class="['rol-badge', u.role?.toLowerCase()]">{{ u.role }}</span>
        </div>
        <div class="kart-alt">
          <span class="sirket" v-if="u.companyName"><i class="pi pi-building"></i> {{ u.companyName }}</span>
          <span class="durum" :class="u.active ? 'aktif' : 'pasif'">
            {{ u.active ? 'Aktif' : 'Pasif' }}
          </span>
        </div>
        <div class="kart-islem">
          <Button icon="pi pi-pencil" class="p-button-rounded p-button-sm islem-btn duzenle" @click="editKullanici(u)" v-tooltip.top="'Düzenle'" />
          <Button icon="pi pi-trash" class="p-button-rounded p-button-sm islem-btn sil" @click="confirmDel(u.id)" v-tooltip.top="'Sil'" :disabled="u.id === authStore.kullanici?.id" />
        </div>
      </div>
      <Message v-if="kullanicilar.length === 0" severity="info" text="Kullanıcı bulunmamaktadır." class="full-width" />
    </div>

    <Dialog v-model:visible="showDialog" :header="editingId ? 'Kullanıcı Düzenle' : 'Yeni Kullanıcı'" :modal="true" style="width:500px">
      <div class="form-grup">
        <label>Kullanıcı Adı *</label>
        <InputText v-model="form.username" placeholder="Kullanıcı adı" class="w-full" :disabled="!!editingId" />
      </div>
      <div class="form-grup">
        <label>Görünen Ad *</label>
        <InputText v-model="form.displayName" placeholder="Ad soyad" class="w-full" />
      </div>
      <div class="form-grup">
        <label>{{ editingId ? 'Yeni Şifre (boş bırakılırsa değişmez)' : 'Şifre' }}</label>
        <InputText v-model="form.password" type="password" placeholder="••••••" class="w-full" />
      </div>
      <div class="form-grup">
        <label>Avatar</label>
        <div class="avatar-upload">
          <div class="avatar-upload-preview">
            <img v-if="avatarPreview" :src="avatarPreview" class="avatar-preview-img" />
            <span v-else class="avatar-preview-yedek">{{ (form.displayName || '?').charAt(0) }}</span>
          </div>
          <div class="avatar-upload-inputs">
            <input type="file" accept="image/*" @change="avatarDosyaSec" ref="avatarInput" style="display:none" />
            <Button label="Dosya Seç" icon="pi pi-upload" @click="$refs.avatarInput.click()" size="small" class="p-button-outlined" />
            <span v-if="avatarDosyaAdi" class="avatar-dosya-adi">{{ avatarDosyaAdi }}</span>
            <span v-else class="avatar-veya">veya URL girin</span>
            <InputText v-model="form.avatarUrl" placeholder="https://..." class="w-full" />
          </div>
        </div>
      </div>
      <div class="form-grup">
        <label>Şirket Adı</label>
        <Dropdown v-model="form.companyName" :options="sirketListesi" option-label="ad" option-value="ad" placeholder="Şirket Seçiniz" editable class="w-full" />
      </div>
      <div class="form-row">
        <div class="form-grup">
          <label>Rol</label>
          <Dropdown v-model="form.role" :options="[{label:'Admin',value:'ADMIN'},{label:'Kullanıcı',value:'USER'}]" option-label="label" option-value="value" class="w-full" />
        </div>
        <div class="form-grup" v-if="editingId">
          <label>Durum</label>
          <Dropdown v-model="form.active" :options="[{label:'Aktif',value:true},{label:'Pasif',value:false}]" option-label="label" option-value="value" class="w-full" />
        </div>
      </div>
      <template #footer>
        <Button label="İptal" icon="pi pi-times" @click="closeDialog" class="p-button-text" />
        <Button :label="editingId ? 'Güncelle' : 'Kaydet'" icon="pi pi-check" @click="save" :loading="saving" />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import { useAuthStore } from '../stores/authStore.js'
import apiClient, { kullaniciAPI } from '../api/index.js'

const toast = useToast()
const confirm = useConfirm()
const authStore = useAuthStore()

const kullanicilar = ref([])
const loading = ref(false)
const saving = ref(false)
const showDialog = ref(false)
const editingId = ref(null)

const form = ref({
  username: '', displayName: '', password: '',
  avatarUrl: '', companyName: '', role: 'USER', active: true
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
    const [r, sR] = await Promise.all([
      kullaniciAPI.getAll(),
      sirketAPI.getAktifSirketler()
    ])
    kullanicilar.value = r.data?.content || r.data || []
    sirketListesi.value = sR.data || []
  }
  catch { toast.add({ severity: 'error', summary: 'Hata', detail: 'Kullanıcılar veya şirketler yüklenemedi', life: 5000 }) }
  finally { loading.value = false }
})

const openDialog = () => {
  editingId.value = null
  form.value = { username: '', displayName: '', password: '', avatarUrl: '', companyName: '', role: 'USER', active: true }
  showDialog.value = true
}

const editKullanici = (u) => {
  editingId.value = u.id
  form.value = {
    username: u.username, displayName: u.displayName, password: '',
    avatarUrl: u.avatarUrl || '', companyName: u.companyName || '',
    role: u.role || 'USER', active: u.active !== false
  }
  showDialog.value = true
}

const closeDialog = () => { showDialog.value = false }

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
    toast.add({ severity: 'error', summary: 'Hata', detail: 'Avatar yüklenemedi', life: 3000 })
    throw e
  } finally { avatarYukleniyor.value = false }
}

const save = async () => {
  if (!form.value.displayName.trim()) { toast.add({ severity: 'warn', summary: 'Uyarı', detail: 'Görünen ad giriniz', life: 5000 }); return }
  if (!editingId && !form.value.username.trim()) { toast.add({ severity: 'warn', summary: 'Uyarı', detail: 'Kullanıcı adı giriniz', life: 5000 }); return }
  saving.value = true
  try {
    if (avatarDosya.value) await avatarYukle()
    if (editingId.value) {
      await kullaniciAPI.update(editingId.value, form.value)
      if (editingId.value === authStore.kullanici?.id) await authStore.kullaniciGuncelle()
      toast.add({ severity: 'success', summary: 'Başarılı', detail: 'Kullanıcı güncellendi', life: 5000 })
    } else {
      if (!form.value.password) { toast.add({ severity: 'warn', summary: 'Uyarı', detail: 'Şifre giriniz', life: 5000 }); return }
      await kullaniciAPI.create(form.value)
      toast.add({ severity: 'success', summary: 'Başarılı', detail: 'Kullanıcı oluşturuldu', life: 5000 })
    }
    closeDialog()
    const r = await kullaniciAPI.getAll()
    kullanicilar.value = r.data?.content || r.data || []
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err.response?.data?.message || 'İşlem başarısız', life: 5000 })
  } finally { saving.value = false }
}

const confirmDel = (id) => {
  confirm.require({
    message: 'Bu kullanıcıyı silmek istediğinizden emin misiniz?', header: 'Onay',
    icon: 'pi pi-exclamation-triangle',
    accept: async () => {
      try { await kullaniciAPI.delete(id); kullanicilar.value = kullanicilar.value.filter(u => u.id !== id); toast.add({ severity: 'success', summary: 'Başarılı', detail: 'Kullanıcı silindi', life: 5000 }) }
      catch { toast.add({ severity: 'error', summary: 'Hata', detail: 'Silme başarısız', life: 5000 }) }
    }
  })
}
</script>

<style scoped>
.kullanicilar-container { padding: 20px; }
h1 { color: var(--text-primary); margin-bottom: 20px; font-size: 28px; }
.toolbar { margin-bottom: 20px; background: var(--bg-card); border: 1px solid var(--border); border-radius: 12px; padding: 15px; }
.loading { text-align: center; padding: 40px; color: #94a3b8; }
.kullanici-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(340px, 1fr)); gap: 16px; }
.kullanici-kart {
  background: var(--bg-card); border: 1px solid var(--border);
  border-radius: 14px; padding: 20px;
  display: flex; flex-direction: column;
  transition: all 0.3s ease;
}
.kullanici-kart:hover { transform: translateY(-2px); box-shadow: 0 8px 30px rgba(0,0,0,0.3); border-color: rgba(59,130,246,0.3); }
.kart-ust { display: flex; align-items: center; gap: 14px; margin-bottom: 14px; }
.avatar { width: 48px; height: 48px; border-radius: 50%; overflow: hidden; flex-shrink: 0; border: 2px solid rgba(59,130,246,0.3); }
.avatar img { width: 100%; height: 100%; object-fit: cover; }
.avatar-yedek { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg,#3b82f6,#1d4ed8); color: white; font-weight: 700; font-size: 18px; }
.kart-bilgi { flex: 1; min-width: 0; }
.kart-bilgi h3 { margin: 0; font-size: 16px; color: var(--text-primary); }
.kullanici-ad { font-size: 12px; color: var(--text-muted); }
.rol-badge { padding: 3px 10px; border-radius: 20px; font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; flex-shrink: 0; }
.rol-badge.admin { background: rgba(59,130,246,0.15); color: #60a5fa; }
.rol-badge.user { background: rgba(34,197,94,0.15); color: #4ade80; }
.kart-alt { display: flex; justify-content: space-between; margin-bottom: 14px; font-size: 13px; flex-shrink: 0; }
.sirket { color: #94a3b8; display: flex; align-items: center; gap: 5px; }
.durum { padding: 2px 8px; border-radius: 10px; font-size: 11px; }
.durum.aktif { background: rgba(34,197,94,0.15); color: #4ade80; }
.durum.pasif { background: rgba(239,68,68,0.15); color: #f87171; }
.kart-islem { display: flex; gap: 8px; justify-content: flex-end; padding-top: 14px; margin-top: auto; border-top: 1px solid rgba(148,163,184,0.1); flex-shrink: 0; }
.islem-btn { width: 32px !important; height: 32px !important; border-radius: 8px !important; border: none !important; }
.islem-btn.duzenle { background: rgba(59,130,246,0.12) !important; color: #60a5fa !important; }
.islem-btn.duzenle:hover { background: rgba(59,130,246,0.25) !important; }
.islem-btn.sil { background: rgba(239,68,68,0.12) !important; color: #f87171 !important; }
.islem-btn.sil:hover { background: rgba(239,68,68,0.25) !important; }
.form-grup { margin-bottom: 18px; }
.form-grup label { display: block; margin-bottom: 6px; font-weight: 600; color: #94a3b8; font-size: 12px; text-transform: uppercase; letter-spacing: 0.5px; }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; }
.w-full { width: 100% !important; }
.full-width { grid-column: 1/-1; }
.avatar-upload { display: flex; gap: 12px; align-items: flex-start; }
.avatar-upload-preview { width: 64px; height: 64px; border-radius: 50%; overflow: hidden; flex-shrink: 0; border: 2px solid rgba(59,130,246,0.3); }
.avatar-preview-img { width: 100%; height: 100%; object-fit: cover; }
.avatar-preview-yedek { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg,#3b82f6,#1d4ed8); color: white; font-weight: 700; font-size: 20px; }
.avatar-upload-inputs { flex: 1; display: flex; flex-direction: column; gap: 6px; }
.avatar-dosya-adi { font-size: 12px; color: var(--text-muted); }
.avatar-veya { font-size: 11px; color: var(--text-muted); text-align: center; }
</style>
