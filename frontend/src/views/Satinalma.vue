<template>
  <div class="satinalma-sayfasi">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        Satın Alma
      </h1>
    </div>

    <TabView>
      <TabPanel header="Talepler">
        <div class="panel-baslik">
          <Button
            label="Yeni Talep"
            icon="pi pi-plus"
            @click="talepDialogAc()"
          />
        </div>
        <DataTable
          state-storage="session"
          state-key="satinalma-table-state"
          :value="talepler"
          striped-rows
          :loading="taleplerYukleniyor"
        >
          <Column
            field="talepNo"
            header="Talep No"
            sortable
          />
          <Column
            field="tarih"
            header="Tarih"
          />
          <Column
            field="talepEden"
            header="Talep Eden"
          />
          <Column
            field="departman"
            header="Departman"
          />
          <Column
            field="durum"
            header="Durum"
          >
            <template #body="{ data }">
              <Tag
                :value="data.durum"
                :severity="data.durum === 'ONAYLANDI' ? 'success' : data.durum === 'REDDEDILDI' ? 'danger' : 'warn'"
              />
            </template>
          </Column>
          <Column
            header="İşlem"
            style="width: 150px"
          >
            <template #body="{ data }">
              <Button
                v-if="data.durum === 'TASLAK'"
                icon="pi pi-check"
                class="p-button-rounded p-button-text p-button-success"
                title="Onayla"
                @click="talepDurumGuncelle(data, 'ONAYLANDI')"
              />
              <Button
                v-if="data.durum === 'TASLAK'"
                icon="pi pi-times"
                class="p-button-rounded p-button-text p-button-danger"
                title="Reddet"
                @click="talepDurumGuncelle(data, 'REDDEDILDI')"
              />
              <Button
                icon="pi pi-trash"
                class="p-button-rounded p-button-text"
                title="Sil"
                @click="talepSil(data)"
              />
            </template>
          </Column>
        </DataTable>
      </TabPanel>

      <TabPanel header="Siparişler">
        <div class="panel-baslik">
          <Button
            label="Yeni Sipariş"
            icon="pi pi-plus"
            @click="siparisDialogAc()"
          />
        </div>
        <DataTable
          state-storage="session"
          state-key="satinalma-table-state"
          :value="siparisler"
          striped-rows
          :loading="siparislerYukleniyor"
        >
          <Column
            field="siparisNo"
            header="Sipariş No"
            sortable
          />
          <Column
            field="tarih"
            header="Tarih"
          />
          <Column
            field="cariHesapAdi"
            header="Tedarikçi"
          />
          <Column
            field="genelToplam"
            header="Toplam"
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
                  data.durum === 'SIPARIS_VERILDI'
                    ? 'info'
                    : data.durum === 'TESLIM_ALINDI'
                      ? 'success'
                      : data.durum === 'IPTAL'
                        ? 'danger'
                        : 'warn'
                "
              />
            </template>
          </Column>
          <Column
            header="İşlem"
            style="width: 150px"
          >
            <template #body="{ data }">
              <Button
                v-if="data.durum === 'TASLAK'"
                icon="pi pi-check-circle"
                class="p-button-rounded p-button-text p-button-info"
                title="Sipariş Ver"
                @click="siparisDurumGuncelle(data, 'SIPARIS_VERILDI')"
              />
              <Button
                v-if="data.durum === 'SIPARIS_VERILDI'"
                icon="pi pi-box"
                class="p-button-rounded p-button-text p-button-success"
                title="Teslim Al"
                @click="siparisDurumGuncelle(data, 'TESLIM_ALINDI')"
              />
              <Button
                v-if="data.durum === 'TESLIM_ALINDI'"
                icon="pi pi-file"
                class="p-button-rounded p-button-text p-button-warning"
                title="Alış Faturasına Çevir"
                @click="siparisFaturayaCevir(data)"
              />
              <Button
                icon="pi pi-trash"
                class="p-button-rounded p-button-text"
                title="Sil"
                @click="siparisSil(data)"
              />
            </template>
          </Column>
        </DataTable>
      </TabPanel>
    </TabView>

    <Dialog
      v-model:visible="talepDialog"
      header="Yeni Satın Alma Talebi"
      modal
      :style="{ width: '600px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>Talep No *</label>
          <InputText
            v-model="talepForm.talepNo"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Tarih</label>
          <DatePicker
            v-model="talepForm.tarih"
            date-format="dd/mm/yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Talep Eden</label>
          <InputText
            v-model="talepForm.talepEden"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Departman</label>
          <InputText
            v-model="talepForm.departman"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Açıklama</label>
          <Textarea
            v-model="talepForm.aciklama"
            rows="3"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          label="İptal"
          icon="pi pi-times"
          class="p-button-text"
          @click="talepDialog = false"
        />
        <Button
          label="Kaydet"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="talepKaydet"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="siparisDialog"
      header="Yeni Satın Alma Siparişi"
      modal
      :style="{ width: '600px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>Sipariş No *</label>
          <InputText
            v-model="siparisForm.siparisNo"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Tarih</label>
          <DatePicker
            v-model="siparisForm.tarih"
            date-format="dd/mm/yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Tedarikçi *</label>
          <Dropdown
            v-model="siparisForm.cariHesapId"
            :options="cariler"
            option-label="ad"
            option-value="id"
            placeholder="Tedarikçi Seçin"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Açıklama</label>
          <Textarea
            v-model="siparisForm.aciklama"
            rows="3"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          label="İptal"
          icon="pi pi-times"
          class="p-button-text"
          @click="siparisDialog = false"
        />
        <Button
          label="Kaydet"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="siparisKaydet"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { satinalmaTalepAPI, satinalmaSiparisAPI, cariHesapAPI } from '../api/index.js'
const toastBildirim = useToastBildirim()
const confirm = useConfirm()

const talepler = ref([])
const siparisler = ref([])
const cariler = ref([])
const taleplerYukleniyor = ref(false)
const siparislerYukleniyor = ref(false)
const kaydediliyor = ref(false)
const talepDialog = ref(false)
const siparisDialog = ref(false)
const talepForm = ref({ talepNo: '', tarih: new Date(), talepEden: '', departman: '', aciklama: '' })
const siparisForm = ref({ siparisNo: '', tarih: new Date(), cariHesapId: null, aciklama: '' })

onMounted(async () => {
  await Promise.all([talepleriYukle(), siparisleriYukle(), carieleriYukle()])
})

const talepleriYukle = async () => {
  taleplerYukleniyor.value = true
  try {
    const r = await satinalmaTalepAPI.getAll()
    talepler.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || 'Talepler yüklenirken hata oluştu')
  }
  taleplerYukleniyor.value = false
}

const siparisleriYukle = async () => {
  siparislerYukleniyor.value = true
  try {
    const r = await satinalmaSiparisAPI.getAll()
    siparisler.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || 'Siparişler yüklenirken hata oluştu')
  }
  siparislerYukleniyor.value = false
}

const carieleriYukle = async () => {
  try {
    const r = await cariHesapAPI.getAll()
    cariler.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || 'Cariler yüklenirken hata oluştu')
  }
}

const talepDialogAc = () => {
  talepForm.value = { talepNo: 'TAL-' + Date.now(), tarih: new Date(), talepEden: '', departman: '', aciklama: '' }
  talepDialog.value = true
}

const talepKaydet = async () => {
  kaydediliyor.value = true
  try {
    await satinalmaTalepAPI.create({ ...talepForm.value, tarih: talepForm.value.tarih?.toISOString().split('T')[0] })
    talepDialog.value = false
    await talepleriYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || 'Talep kaydedilirken hata oluştu')
  }
  kaydediliyor.value = false
}

const talepDurumGuncelle = async (data, durum) => {
  try {
    await satinalmaTalepAPI.durumGuncelle(data.id, durum)
    await talepleriYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || 'Durum güncellenirken hata oluştu')
  }
}

const talepSil = (data) => {
  confirm.require({
    message: 'Bu kaydı silmek istediğinize emin misiniz?',
    header: 'Silme Onayı',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Evet, Sil',
    rejectLabel: 'İptal',
    accept: async () => {
      try {
        await satinalmaTalepAPI.delete(data.id)
        await talepleriYukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || err?.message || 'Talep silinirken hata oluştu')
      }
    },
    reject: () => {}
  })
}

const siparisDialogAc = () => {
  siparisForm.value = { siparisNo: 'SIP-' + Date.now(), tarih: new Date(), cariHesapId: null, aciklama: '' }
  siparisDialog.value = true
}

const siparisKaydet = async () => {
  kaydediliyor.value = true
  try {
    await satinalmaSiparisAPI.create({
      ...siparisForm.value,
      tarih: siparisForm.value.tarih?.toISOString().split('T')[0]
    })
    siparisDialog.value = false
    await siparisleriYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || 'Sipariş kaydedilirken hata oluştu')
  }
  kaydediliyor.value = false
}

const siparisDurumGuncelle = async (data, durum) => {
  try {
    await satinalmaSiparisAPI.durumGuncelle(data.id, durum)
    await siparisleriYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || 'Durum güncellenirken hata oluştu')
  }
}

const siparisFaturayaCevir = (data) => {
  confirm.require({
    message: `"${data.siparisNo}" siparişi alış faturasına dönüştürülecek ve stoklar güncellenecek. Devam edilsin mi?`,
    header: 'Alış Faturasına Dönüştür',
    icon: 'pi pi-file',
    acceptLabel: 'Evet, Dönüştür',
    rejectLabel: 'İptal',
    accept: async () => {
      try {
        await satinalmaSiparisAPI.faturayaCevir(data.id)
        toastBildirim.basarili('Sipariş alış faturasına dönüştürüldü, stoklar güncellendi.')
        await siparisleriYukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || err?.message || 'Faturaya dönüştürülürken hata oluştu')
      }
    },
    reject: () => {}
  })
}

const siparisSil = (data) => {
  confirm.require({
    message: 'Bu kaydı silmek istediğinize emin misiniz?',
    header: 'Silme Onayı',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Evet, Sil',
    rejectLabel: 'İptal',
    accept: async () => {
      try {
        await satinalmaSiparisAPI.delete(data.id)
        await siparisleriYukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || err?.message || 'Sipariş silinirken hata oluştu')
      }
    },
    reject: () => {}
  })
}
</script>

<style scoped>
.satinalma-sayfasi {
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
.panel-baslik {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
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
