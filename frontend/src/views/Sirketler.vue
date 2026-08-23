<template>
  <div class="sirketler-sayfasi">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        Şirketler
      </h1>
      <Button
        label="Yeni Şirket"
        icon="pi pi-plus"
        @click="dialogAc"
      />
    </div>

    <DataTable
      state-storage="session"
      state-key="sirketler-table-state"
      :value="sirketler"
      striped-rows
      responsive-layout="scroll"
      :loading="yukleniyor"
    >
      <Column
        field="id"
        header="#"
        style="width: 60px"
      />
      <Column
        field="ad"
        header="Şirket Adı"
        sortable
      />
      <Column
        field="vergiNo"
        header="Vergi No"
      />
      <Column
        field="vergiDairesi"
        header="Vergi Dairesi"
      />
      <Column
        field="telefon"
        header="Telefon"
      />
      <Column
        field="email"
        header="E-posta"
      />
      <Column
        field="tur"
        header="Tür"
      >
        <template #body="{ data }">
          <Tag
            :value="data.tur"
            :severity="data.tur === 'RESMI' ? 'success' : data.tur === 'GAYRIRESMI' ? 'warning' : 'info'"
          />
        </template>
      </Column>
      <Column
        field="yil"
        header="Yıl"
      />
      <Column
        field="aktif"
        header="Durum"
      >
        <template #body="{ data }">
          <Tag
            :value="data.aktif ? 'Aktif' : 'Pasif'"
            :severity="data.aktif ? 'success' : 'danger'"
          />
        </template>
      </Column>
      <Column
        header="İşlem"
        style="width: 120px"
      >
        <template #body="{ data }">
          <Button
            icon="pi pi-sitemap"
            class="p-button-rounded p-button-text p-button-info"
            title="Grup Konsolide Özeti"
            @click="konsolideGoster(data)"
          />
          <Button
            icon="pi pi-pencil"
            class="p-button-rounded p-button-text"
            title="Düzenle"
            @click="dialogAc(data)"
          />
          <Button
            icon="pi pi-trash"
            class="p-button-rounded p-button-text p-button-danger"
            title="Sil"
            @click="sil(data)"
          />
        </template>
      </Column>
    </DataTable>

    <Dialog
      v-model:visible="dialog"
      :header="duzenleme ? 'Şirket Düzenle' : 'Yeni Şirket'"
      modal
      :style="{ width: '500px' }"
    >
      <Message
        v-if="uyariMesaji"
        severity="warn"
        :closable="false"
      >
        <i
          class="pi pi-exclamation-triangle"
          style="margin-right: 8px"
        />{{ uyariMesaji }}
      </Message>
      <div class="form-grid">
        <div class="field">
          <label>Şirket Adı *</label>
          <InputText
            v-model="form.ad"
            class="w-full"
          />
        </div>
        
        <div class="field-row" style="display: flex; gap: 16px;">
          <div class="field" style="flex: 1;">
            <label>Şirket Türü</label>
            <Dropdown
              v-model="form.tur"
              :options="[
                { label: 'Diğer', value: 'DIGER' },
                { label: 'Resmi', value: 'RESMI' },
                { label: 'Gayriresmi', value: 'GAYRIRESMI' }
              ]"
              optionLabel="label"
              optionValue="value"
              class="w-full"
            />
          </div>
          <div class="field" style="flex: 1;">
            <label>Mali Yıl</label>
            <InputNumber
              v-model="form.yil"
              :useGrouping="false"
              class="w-full"
            />
          </div>
        </div>

        <div class="field">
          <label>Ana Şirket (Gruplama için)</label>
          <Dropdown
            v-model="form.parentId"
            :options="sirketler.filter(s => s.id !== form.id)"
            optionLabel="ad"
            optionValue="id"
            showClear
            placeholder="Ana şirket seçin"
            class="w-full"
          />
        </div>

        <div class="field">
          <label>Vergi No</label>
          <InputText
            v-model="form.vergiNo"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Vergi Dairesi</label>
          <InputText
            v-model="form.vergiDairesi"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Telefon</label>
          <InputText
            v-model="form.telefon"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>E-posta</label>
          <InputText
            v-model="form.email"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Web Sitesi</label>
          <InputText
            v-model="form.webSite"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Logo</label>
          <div class="logo-upload-row">
            <InputText
              v-model="form.logoUrl"
              class="w-full"
              placeholder="URL girin veya dosya seçin"
            />
            <Button
              icon="pi pi-upload"
              class="p-button-outlined"
              :loading="logoYukleniyor"
              @click="$refs.logoInput.click()"
            />
            <input
              ref="logoInput"
              type="file"
              accept="image/*"
              style="display: none"
              @change="logoSec"
            >
          </div>
          <img
            v-if="form.logoUrl"
            :src="form.logoUrl"
            class="logo-preview"
            alt="Logo Önizleme"
          >
        </div>
        <div class="field">
          <label>Adres</label>
          <Textarea
            v-model="form.adres"
            rows="3"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Aktif</label>
          <InputSwitch v-model="form.aktif" />
        </div>
      </div>
      <template #footer>
        <Button
          label="İptal"
          icon="pi pi-times"
          class="p-button-text"
          @click="dialog = false"
        />
        <Button
          label="Kaydet"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="kaydet"
        />
      </template>
    </Dialog>

    <!-- Grup Konsolidasyon Modal -->
    <Dialog
      v-model:visible="konsolideModal"
      :header="konsolideVeri?.anaSirketAdi + ' - Grup Konsolide Özeti'"
      modal
      :style="{ width: '700px' }"
    >
      <div v-if="konsolideYukleniyor" class="text-center py-6">
        <i class="pi pi-spin pi-spinner text-3xl text-primary" />
      </div>
      <div v-else-if="konsolideVeri" class="space-y-4">
        <div class="grid grid-cols-3 gap-3">
          <div class="p-3 bg-blue-50 dark:bg-blue-950/40 rounded-lg border border-blue-100">
            <span class="text-xs text-blue-600 font-bold block">Toplam Stok Değeri</span>
            <span class="text-lg font-extrabold text-blue-800 dark:text-blue-200">{{ formatPara(konsolideVeri.toplamStokDegeri) }}</span>
          </div>
          <div class="p-3 bg-emerald-50 dark:bg-emerald-950/40 rounded-lg border border-emerald-100">
            <span class="text-xs text-emerald-600 font-bold block">Toplam Alacak</span>
            <span class="text-lg font-extrabold text-emerald-800 dark:text-emerald-200">{{ formatPara(konsolideVeri.toplamAlacakBakiye) }}</span>
          </div>
          <div class="p-3 bg-amber-50 dark:bg-amber-950/40 rounded-lg border border-amber-100">
            <span class="text-xs text-amber-600 font-bold block">Toplam Borç</span>
            <span class="text-lg font-extrabold text-amber-800 dark:text-amber-200">{{ formatPara(konsolideVeri.toplamBorcBakiye) }}</span>
          </div>
        </div>

        <h4 class="font-bold text-sm text-gray-700 dark:text-gray-200 mt-3">Grup ve Yıllık Alt Şirketler ({{ konsolideVeri.sirketler?.length || 0 }})</h4>
        <DataTable :value="konsolideVeri.sirketler" size="small" striped-rows>
          <Column field="sirketAdi" header="Şirket" />
          <Column field="tur" header="Tür" style="width: 100px" />
          <Column field="yil" header="Yıl" style="width: 80px" />
          <Column field="stokSayisi" header="Stok Çeşidi" style="width: 100px" />
          <Column header="Stok Değeri">
            <template #body="{ data }">
              {{ formatPara(data.stokDegeri) }}
            </template>
          </Column>
          <Column header="Cari Bakiye">
            <template #body="{ data }">
              <span :class="data.bakiye >= 0 ? 'text-emerald-600' : 'text-red-600'">
                {{ formatPara(data.bakiye) }}
              </span>
            </template>
          </Column>
        </DataTable>
      </div>
      <template #footer>
        <Button label="Kapat" class="p-button-text" @click="konsolideModal = false" />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useConfirm } from 'primevue/useconfirm'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { sirketAPI, uploadAPI } from '../api/index.js'

const toastBildirim = useToastBildirim()
const confirm = useConfirm()

const sirketler = ref([])
const yukleniyor = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const kaydediliyor = ref(false)
const seciliId = ref(null)
const form = ref({
  ad: '',
  vergiNo: '',
  vergiDairesi: '',
  adres: '',
  telefon: '',
  email: '',
  webSite: '',
  logoUrl: '',
  aktif: true
})
const uyariMesaji = ref('')
const logoYukleniyor = ref(false)

const konsolideModal = ref(false)
const konsolideYukleniyor = ref(false)
const konsolideVeri = ref(null)

const formatPara = (v) => {
  if (v == null || isNaN(v)) return '0,00 ₺'
  return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(v)
}

const konsolideGoster = async (sirket) => {
  konsolideYukleniyor.value = true
  konsolideModal.value = true
  try {
    const res = await sirketAPI.getKonsolideOzet(sirket.id)
    konsolideVeri.value = res.data
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || 'Konsolide veriler alınamadı')
  } finally {
    konsolideYukleniyor.value = false
  }
}

onMounted(async () => {
  yukleniyor.value = true
  try {
    const r = await sirketAPI.getAll()
    sirketler.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || 'Şirketler yüklenirken hata oluştu')
  }
  yukleniyor.value = false
})

const eskiAd = ref('')
const dialogAc = (data) => {
  duzenleme.value = !!data
  seciliId.value = data?.id || null
  form.value = data
    ? { ...data }
    : {
        ad: '',
        tur: 'DIGER',
        yil: new Date().getFullYear(),
        parentId: null,
        vergiNo: '',
        vergiDairesi: '',
        adres: '',
        telefon: '',
        email: '',
        webSite: '',
        logoUrl: '',
        aktif: true
      }
  eskiAd.value = data?.ad || ''
  uyariMesaji.value = ''
  if (data?.sonAdGuncellemeTarihi) {
    const son = new Date(data.sonAdGuncellemeTarihi)
    const simdi = new Date()
    const fark = Math.ceil((son.getTime() + 30 * 24 * 60 * 60 * 1000 - simdi.getTime()) / (24 * 60 * 60 * 1000))
    if (fark > 0) {
      uyariMesaji.value = `Şirket adı ${fark} gün içinde tekrar değiştirilemez. Son değişiklik: ${son.toLocaleDateString('tr-TR')}`
    }
  }
  dialog.value = true
}

const logoSec = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  logoYukleniyor.value = true
  try {
    const res = await uploadAPI.uploadSirketLogo(file)
    form.value.logoUrl = res.data.url
  } catch (err) {
    toastBildirim.hata('Logo yüklenemedi')
  }
  logoYukleniyor.value = false
}

const kaydet = async () => {
  if (duzenleme.value && form.value.ad !== eskiAd.value && uyariMesaji.value) {
    confirm.require({
      message: `${uyariMesaji.value}\n\nDeğişiklik yapmak istediğinize emin misiniz?`,
      header: '30 Gün Kuralı Uyarısı',
      icon: 'pi pi-exclamation-triangle',
      accept: () => kaydetAction()
    })
    return
  }
  await kaydetAction()
}

const kaydetAction = async () => {
  kaydediliyor.value = true
  try {
    if (duzenleme.value) {
      await sirketAPI.update(seciliId.value, form.value)
      toastBildirim.basarili('Şirket güncellendi')
    } else {
      await sirketAPI.create(form.value)
      toastBildirim.basarili('Şirket oluşturuldu')
    }
    dialog.value = false
    const r = await sirketAPI.getAll()
    sirketler.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err.response?.data?.message || 'İşlem başarısız')
  }
  kaydediliyor.value = false
}

const sil = async (data) => {
  try {
    await sirketAPI.delete(data.id)
    sirketler.value = sirketler.value.filter((s) => s.id !== data.id)
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || 'Şirket silinirken hata oluştu')
  }
}
</script>

<style scoped>
.sirketler-sayfasi {
  padding: 0;
}
.sayfa-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.sayfa-baslik h1 {
  margin: 0;
}
.form-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
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
.logo-upload-row {
  display: flex;
  gap: 8px;
  align-items: center;
}
.logo-preview {
  max-width: 120px;
  max-height: 60px;
  margin-top: 8px;
  border-radius: 6px;
  object-fit: contain;
}
</style>
