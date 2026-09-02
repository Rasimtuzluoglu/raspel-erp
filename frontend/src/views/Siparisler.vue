<template>
  <div class="siparisler-sayfasi">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        Siparişler & Teklifler
      </h1>
      <Button
        label="Yeni Teklif"
        icon="pi pi-plus"
        @click="dialogAc()"
      />
    </div>

    <DataTable
      state-storage="session"
      state-key="siparisler-table-state"
      :value="siparisler"
      striped-rows
      :loading="yukleniyor"
    >
      <Column
        field="siparisNo"
        header="No"
        sortable
      />
      <Column
        field="tarih"
        header="Tarih"
      />
      <Column
        field="cariHesapAdi"
        header="Müşteri"
      />
      <Column
        field="tur"
        header="Tür"
      />
      <Column
        field="genelToplam"
        header="Tutar"
      >
        <template #body="{ data }">
          {{ data.genelToplam?.toFixed(2) }} ₺
        </template>
      </Column>
      <Column
        field="durum"
        header="Durum"
      >
        <template #body="{ data }">
          <Tag
            :value="data.durum"
            :severity="
              data.durum === 'SIPARIS'
                ? 'info'
                : data.durum === 'FATURA_KESILDI'
                  ? 'success'
                  : data.durum === 'IPTAL'
                    ? 'danger'
                    : 'warn'
            "
          />
        </template>
      </Column>
      <Column
        header="İşlemler"
        style="width: 220px"
      >
        <template #body="{ data }">
          <Button
            v-if="data.durum === 'TEKLIF'"
            icon="pi pi-check-circle"
            class="p-button-rounded p-button-text p-button-info"
            title="Siparişe Çevir"
            @click="durumGuncelle(data, 'SIPARIS')"
          />
          <Button
            v-if="data.durum === 'SIPARIS'"
            icon="pi pi-file"
            class="p-button-rounded p-button-text p-button-success"
            title="Faturalaştır"
            @click="durumGuncelle(data, 'FATURA_KESILDI')"
          />
          <Button
            v-if="data.durum === 'FATURA_KESILDI' || data.durum === 'IPTAL'"
            icon="pi pi-undo"
            class="p-button-rounded p-button-text p-button-help"
            title="Siparişe Geri Al"
            @click="durumGuncelle(data, 'SIPARIS')"
          />
          <Button
            v-if="data.durum !== 'IPTAL' && data.durum !== 'FATURA_KESILDI'"
            icon="pi pi-times-circle"
            class="p-button-rounded p-button-text p-button-warning"
            title="İptal Et"
            @click="durumGuncelle(data, 'IPTAL')"
          />
          <Button
            icon="pi pi-briefcase"
            class="p-button-rounded p-button-text p-button-info"
            title="İş Emri Oluştur"
            @click="isEmriAc(data)"
          />
          <Button
            icon="pi pi-trash"
            class="p-button-rounded p-button-text p-button-danger"
            @click="sil(data)"
          />
        </template>
      </Column>
    </DataTable>

    <EmptyState
      v-if="!yukleniyor && siparisler.length === 0"
      message="Henüz sipariş bulunamadı"
      sub-message="İlk teklif/siparişinizi eklemek için Yeni Teklif butonuna tıklayın"
      icon="pi pi-shopping-cart"
      action-label="Yeni Teklif"
      action-icon="pi pi-plus"
      @action="dialogAc()"
    />

    <Dialog
      v-model:visible="dialog"
      header="Yeni Teklif / Sipariş"
      modal
      :style="{ width: '550px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>Teklif No *</label><InputText
            v-model="form.siparisNo"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Tarih</label><DatePicker
            v-model="form.tarih"
            date-format="dd/mm/yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Müşteri *</label>
          <Dropdown
            v-model="form.cariHesapId"
            :options="cariler"
            option-label="ad"
            option-value="id"
            placeholder="Seçin"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Açıklama</label><Textarea
            v-model="form.aciklama"
            rows="2"
            class="w-full"
          />
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

    <Dialog
      v-model:visible="isEmriDialog"
      header="İş Emri Oluştur"
      :modal="true"
      style="width: 480px"
    >
      <div
        v-if="isEmriSiparis"
        class="isemri-siparis"
      >
        Sipariş: <strong>{{ isEmriSiparis.siparisNo }}</strong>
      </div>
      <div class="form-group">
        <label>Atanacak Personel</label>
        <Dropdown
          v-model="isEmriPersonelId"
          :options="personeller"
          option-label="label"
          option-value="value"
          placeholder="Personel seçin"
          filter
          class="w-full"
        />
      </div>
      <div class="form-group">
        <label>Açıklama</label>
        <Textarea
          v-model="isEmriAciklama"
          rows="3"
          placeholder="İş emri açıklaması (isteğe bağlı)"
          class="w-full"
        />
      </div>
      <template #footer>
        <Button
          label="İptal"
          icon="pi pi-times"
          class="p-button-text"
          @click="isEmriDialog = false"
        />
        <Button
          label="Oluştur"
          icon="pi pi-briefcase"
          :loading="isEmriKaydediliyor"
          @click="isEmriOlustur"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { siparisAPI, cariHesapAPI, personelAPI } from '../api/index.js'
import EmptyState from '../components/EmptyState.vue'
const toastBildirim = useToastBildirim()
const confirm = useConfirm()

const siparisler = ref([])
const cariler = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const dialog = ref(false)
const form = ref({ siparisNo: '', tarih: new Date(), cariHesapId: null, aciklama: '' })

onMounted(async () => {
  yukleniyor.value = true
  try {
    const [sR, cR] = await Promise.all([siparisAPI.getAll(), cariHesapAPI.getAll()])
    siparisler.value = sR.data?.content || sR.data || []
    cariler.value = cR.data
    personelleriYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || 'Siparişler yüklenirken hata oluştu')
  }
  yukleniyor.value = false
})

const personeller = ref([])
const isEmriDialog = ref(false)
const isEmriSiparis = ref(null)
const isEmriPersonelId = ref(null)
const isEmriAciklama = ref('')
const isEmriKaydediliyor = ref(false)

const personelleriYukle = async () => {
  try {
    const r = await personelAPI.getAll({ size: 500 })
    const list = r.data?.content || r.data || []
    personeller.value = list.map((p) => ({ label: `${p.ad || ''} ${p.soyad || ''}`.trim(), value: p.id }))
  } catch {
    personeller.value = []
  }
}

const isEmriAc = (siparis) => {
  isEmriSiparis.value = siparis
  isEmriPersonelId.value = null
  isEmriAciklama.value = ''
  isEmriDialog.value = true
}

const isEmriOlustur = async () => {
  isEmriKaydediliyor.value = true
  try {
    await siparisAPI.isEmriOlustur(isEmriSiparis.value.id, {
      personelId: isEmriPersonelId.value,
      aciklama: isEmriAciklama.value
    })
    toastBildirim.basarili('İş emri oluşturuldu')
    isEmriDialog.value = false
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'İş emri oluşturulamadı')
  } finally {
    isEmriKaydediliyor.value = false
  }
}

const dialogAc = () => {
  form.value = { siparisNo: 'TKF-' + Date.now(), tarih: new Date(), cariHesapId: null, aciklama: '' }
  dialog.value = true
}

const kaydet = async () => {
  kaydediliyor.value = true
  try {
    await siparisAPI.create({ ...form.value, tarih: form.value.tarih?.toISOString().split('T')[0] })
    dialog.value = false
    const r = await siparisAPI.getAll()
    siparisler.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || 'Sipariş kaydedilirken hata oluştu')
  }
  kaydediliyor.value = false
}

const durumGuncelle = async (data, durum) => {
  try {
    await siparisAPI.durumGuncelle(data.id, durum)
    const r = await siparisAPI.getAll()
    siparisler.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || 'Durum güncellenirken hata oluştu')
  }
}

const sil = (data) => {
  confirm.require({
    message: 'Bu kaydı silmek istediğinize emin misiniz?',
    header: 'Silme Onayı',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Evet, Sil',
    rejectLabel: 'İptal',
    accept: async () => {
      try {
        await siparisAPI.delete(data.id)
        siparisler.value = siparisler.value.filter((s) => s.id !== data.id)
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || err?.message || 'Sipariş silinirken hata oluştu')
      }
    },
    reject: () => {}
  })
}
</script>

<style scoped>
.siparisler-sayfasi {
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
</style>
