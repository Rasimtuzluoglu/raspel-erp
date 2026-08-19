<template>
  <div class="belgeler-sayfasi">
    <div class="sayfa-baslik">
      <h1>
        <i
          class="pi pi-folder-open"
          style="margin-right: 8px"
        />Belgeler
      </h1>
      <Button
        label="Belge Yükle"
        icon="pi pi-upload"
        @click="yukleDialog = true"
      />
    </div>

    <DataTable
      state-storage="session"
      state-key="belgeler-table-state"
      :value="liste"
      striped-rows
      :loading="yukleniyor"
    >
      <Column
        field="dosyaAdi"
        header="Dosya"
      >
        <template #body="{ data }">
          <div class="dosya-ad">
            <i :class="dosyaIkon(data.dosyaAdi)" />
            <span>{{ data.dosyaAdi }}</span>
          </div>
        </template>
      </Column>
      <Column
        field="entityAdi"
        header="Bağlı Kayıt"
      />
      <Column
        field="olusturmaTarihi"
        header="Tarih"
      >
        <template #body="{ data }">
          {{ data.olusturmaTarihi ? new Date(data.olusturmaTarihi).toLocaleString('tr-TR') : '-' }}
        </template>
      </Column>
      <Column
        header="İşlem"
        style="width: 180px"
      >
        <template #body="{ data }">
          <Button
            icon="pi pi-eye"
            class="p-button-rounded p-button-text"
            title="Önizle"
            @click="onizle(data)"
          />
          <Button
            icon="pi pi-download"
            class="p-button-rounded p-button-text"
            title="İndir"
            @click="indir(data)"
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
    <div
      v-if="!yukleniyor && !liste.length"
      class="bos"
    >
      Henüz belge yok.
    </div>

    <Dialog
      v-model:visible="yukleDialog"
      header="Belge Yükle"
      modal
      :style="{ width: '480px' }"
    >
      <div class="form-grup">
        <label>Bağlı Kayıt Türü</label>
        <InputText
          v-model="yukleForm.entityAdi"
          placeholder="örn. Fatura, Sipariş, Cari..."
          class="w-full"
        />
      </div>
      <div class="form-grup">
        <label>Kayıt ID</label>
        <InputNumber
          v-model="yukleForm.entityId"
          placeholder="örn. 123"
          class="w-full"
        />
      </div>
      <div class="form-grup">
        <label>Dosya</label>
        <input
          type="file"
          class="w-full"
          @change="dosyaSec"
        >
      </div>
      <template #footer>
        <Button
          label="İptal"
          icon="pi pi-times"
          class="p-button-text"
          @click="yukleDialog = false"
        />
        <Button
          label="Yükle"
          icon="pi pi-upload"
          :loading="yukleniyor"
          @click="yukle"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="onizleDialog"
      :header="onizleBelge?.dosyaAdi"
      modal
      :style="{ width: '700px' }"
    >
      <div
        v-if="resimMi(onizleBelge?.dosyaAdi)"
        class="onizle-resim"
      >
        <img
          :src="onizleUrl"
          alt="önizle"
        >
      </div>
      <div
        v-else
        class="onizle-yok"
      >
        <i
          class="pi pi-file"
          style="font-size: 40px"
        />
        <p>Bu dosya türü için önizleme yok. İndirerek görüntüleyebilirsiniz.</p>
        <Button
          label="İndir"
          icon="pi pi-download"
          @click="onizleBelge && indir(onizleBelge)"
        />
      </div>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { belgeAPI } from '../api/index.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'

const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const liste = ref([])
const yukleniyor = ref(false)
const yukleDialog = ref(false)
const onizleDialog = ref(false)
const onizleBelge = ref(null)
const onizleUrl = ref('')
const yukleForm = ref({ entityAdi: '', entityId: null, file: null })

const resimUzantilar = ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.bmp']

const resimMi = (ad) => !!ad && resimUzantilar.some((u) => ad.toLowerCase().endsWith(u))

const dosyaIkon = (ad) => {
  if (resimMi(ad)) return 'pi pi-image'
  if (ad?.toLowerCase().endsWith('.pdf')) return 'pi pi-file-pdf'
  return 'pi pi-file'
}

const yukle = async () => {
  if (!yukleForm.value.file) {
    toastBildirim.uyari('Lütfen dosya seçin')
    return
  }
  yukleniyor.value = true
  try {
    await belgeAPI.yukle(yukleForm.value.entityAdi || 'Genel', yukleForm.value.entityId || 0, yukleForm.value.file)
    toastBildirim.basarili('Belge yüklendi')
    yukleDialog.value = false
    yukleForm.value = { entityAdi: '', entityId: null, file: null }
    await yukleListe()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Belge yüklenemedi')
  }
  yukleniyor.value = false
}

const dosyaSec = (e) => {
  yukleForm.value.file = e.target.files[0]
}

const yukleListe = async () => {
  yukleniyor.value = true
  try {
    const r = await belgeAPI.tumBelgeler()
    liste.value = r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Belgeler yüklenemedi')
  }
  yukleniyor.value = false
}

const indir = async (belge) => {
  const dosyaAdi = belge.url ? belge.url.split('/').pop() : null
  if (!dosyaAdi) return
  try {
    const r = await belgeAPI.indir(dosyaAdi)
    const url = window.URL.createObjectURL(new Blob([r.data]))
    const a = document.createElement('a')
    a.href = url
    a.download = belge.dosyaAdi || dosyaAdi
    a.click()
    window.URL.revokeObjectURL(url)
  } catch (err) {
    toastBildirim.hata('İndirme başarısız')
  }
}

const onizle = async (belge) => {
  onizleBelge.value = belge
  const dosyaAdi = belge.url ? belge.url.split('/').pop() : null
  if (dosyaAdi && resimMi(belge.dosyaAdi)) {
    const r = await belgeAPI.indir(dosyaAdi)
    onizleUrl.value = window.URL.createObjectURL(new Blob([r.data]))
  }
  onizleDialog.value = true
}

const sil = (belge) => {
  confirm.require({
    message: `"${belge.dosyaAdi}" silinsin mi?`,
    header: 'Silme Onayı',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Evet',
    rejectLabel: 'İptal',
    accept: async () => {
      try {
        await belgeAPI.sil(belge.id)
        toastBildirim.basarili('Belge silindi')
        await yukleListe()
      } catch (err) {
        toastBildirim.hata('Silme başarısız')
      }
    },
    reject: () => {}
  })
}

onMounted(yukleListe)
</script>

<style scoped>
.belgeler-sayfasi {
  padding: 0;
}
.sayfa-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.sayfa-baslik h1 {
  margin: 0;
}
.dosya-ad {
  display: flex;
  align-items: center;
  gap: 8px;
}
.bos {
  text-align: center;
  color: var(--text-muted);
  padding: 40px 0;
}
.form-grup {
  margin-bottom: 14px;
}
.form-grup label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 6px;
}
.w-full {
  width: 100%;
}
.onizle-resim {
  text-align: center;
}
.onizle-resim img {
  max-width: 100%;
  max-height: 500px;
  border-radius: 8px;
}
.onizle-yok {
  text-align: center;
  padding: 40px;
  color: var(--text-secondary);
}
</style>
