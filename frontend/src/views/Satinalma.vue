<template>
  <div class="satinalma-sayfasi">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        {{ t('satinalma.title') }}
      </h1>
    </div>

    <TabView>
      <TabPanel :header="t('satinalma.talepler')">
        <div class="panel-baslik">
          <Button
            :label="t('satinalma.yeniTalep')"
            icon="pi pi-plus"
            @click="talepDialogAc()"
          />
        </div>
        <DataTable
          :value="talepler"
          striped-rows
          :loading="taleplerYukleniyor"
        >
          <Column
            field="talepNo"
            :header="t('satinalma.talepNo')"
            sortable
          />
          <Column
            field="tarih"
            :header="t('common.date')"
          />
          <Column
            field="talepEden"
            :header="t('satinalma.talepEden')"
          />
          <Column
            field="departman"
            :header="t('satinalma.departman')"
          />
          <Column
            field="durum"
            :header="t('common.status')"
          >
            <template #body="{ data }">
              <Tag
                :value="data.durum"
                :severity="data.durum === 'ONAYLANDI' ? 'success' : data.durum === 'REDDEDILDI' ? 'danger' : 'warn'"
              />
            </template>
          </Column>
          <Column
            :header="t('satinalma.islem')"
            style="width: 150px"
          >
            <template #body="{ data }">
              <Button
                v-if="data.durum === 'TASLAK'"
                icon="pi pi-check"
                class="p-button-rounded p-button-text p-button-success"
                :title="t('satinalma.onayla')"
                @click="talepDurumGuncelle(data, 'ONAYLANDI')"
              />
              <Button
                v-if="data.durum === 'TASLAK'"
                icon="pi pi-times"
                class="p-button-rounded p-button-text p-button-danger"
                :title="t('satinalma.reddet')"
                @click="talepDurumGuncelle(data, 'REDDEDILDI')"
              />
              <Button
                icon="pi pi-trash"
                class="p-button-rounded p-button-text"
                :title="t('common.delete')"
                @click="talepSil(data)"
              />
            </template>
          </Column>
        </DataTable>
      </TabPanel>

      <TabPanel :header="t('satinalma.siparisler')">
        <div class="panel-baslik">
          <Button
            :label="t('satinalma.yeniSiparis')"
            icon="pi pi-plus"
            @click="siparisDialogAc()"
          />
        </div>
        <DataTable
          :value="siparisler"
          striped-rows
          :loading="siparislerYukleniyor"
        >
          <Column
            field="siparisNo"
            :header="t('satinalma.siparisNo')"
            sortable
          />
          <Column
            field="tarih"
            :header="t('common.date')"
          />
          <Column
            field="cariHesapAdi"
            :header="t('satinalma.tedarikci')"
          />
          <Column
            field="genelToplam"
            :header="t('satinalma.toplam')"
          >
            <template #body="{ data }">
              {{ formatCurrency(data.genelToplam) }}
            </template>
          </Column>
          <Column
            field="durum"
            :header="t('common.status')"
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
            :header="t('satinalma.islem')"
            style="width: 150px"
          >
            <template #body="{ data }">
              <Button
                v-if="data.durum === 'TASLAK'"
                icon="pi pi-check-circle"
                class="p-button-rounded p-button-text p-button-info"
                :title="t('satinalma.siparisVer')"
                @click="siparisDurumGuncelle(data, 'SIPARIS_VERILDI')"
              />
              <Button
                v-if="data.durum === 'SIPARIS_VERILDI'"
                icon="pi pi-box"
                class="p-button-rounded p-button-text p-button-success"
                :title="t('satinalma.teslimAl')"
                @click="siparisDurumGuncelle(data, 'TESLIM_ALINDI')"
              />
              <Button
                v-if="data.durum === 'TESLIM_ALINDI'"
                icon="pi pi-file"
                class="p-button-rounded p-button-text p-button-warning"
                :title="t('satinalma.alisFaturasinaCevir')"
                @click="siparisFaturayaCevir(data)"
              />
              <Button
                icon="pi pi-trash"
                class="p-button-rounded p-button-text"
                :title="t('common.delete')"
                @click="siparisSil(data)"
              />
            </template>
          </Column>
        </DataTable>
      </TabPanel>
    </TabView>

    <Dialog
      v-model:visible="talepDialog"
      :header="t('satinalma.yeniTalepBaslik')"
      modal
      :style="{ width: '600px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('satinalma.talepNoZorunlu') }}</label>
          <InputText
            v-model="talepForm.talepNo"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('common.date') }}</label>
          <DatePicker
            v-model="talepForm.tarih"
            date-format="dd/mm/yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('satinalma.talepEden') }}</label>
          <InputText
            v-model="talepForm.talepEden"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('satinalma.departman') }}</label>
          <InputText
            v-model="talepForm.departman"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('common.description') }}</label>
          <Textarea
            v-model="talepForm.aciklama"
            rows="3"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="talepDialog = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="talepKaydet"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="siparisDialog"
      :header="t('satinalma.yeniSiparisBaslik')"
      modal
      :style="{ width: '600px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('satinalma.siparisNoZorunlu') }}</label>
          <InputText
            v-model="siparisForm.siparisNo"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('common.date') }}</label>
          <DatePicker
            v-model="siparisForm.tarih"
            date-format="dd/mm/yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('satinalma.tedarikciZorunlu') }}</label>
          <Dropdown
            v-model="siparisForm.cariHesapId"
            :options="cariler"
            option-label="ad"
            option-value="id"
            :placeholder="t('satinalma.tedarikciSecin')"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('common.description') }}</label>
          <Textarea
            v-model="siparisForm.aciklama"
            rows="3"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="siparisDialog = false"
        />
        <Button
          :label="t('common.save')"
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
import { formatCurrency } from '../utils/format.js'
import { useI18n } from 'vue-i18n'
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { t } = useI18n()

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
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('satinalma.hataTalepYukleme'))
  }
  taleplerYukleniyor.value = false
}

const siparisleriYukle = async () => {
  siparislerYukleniyor.value = true
  try {
    const r = await satinalmaSiparisAPI.getAll()
    siparisler.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('satinalma.hataSiparisYukleme'))
  }
  siparislerYukleniyor.value = false
}

const carieleriYukle = async () => {
  try {
    const r = await cariHesapAPI.getAll()
    cariler.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('satinalma.hataCariYukleme'))
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
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('satinalma.hataTalepKaydet'))
  }
  kaydediliyor.value = false
}

const talepDurumGuncelle = async (data, durum) => {
  try {
    await satinalmaTalepAPI.durumGuncelle(data.id, durum)
    await talepleriYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('satinalma.hataDurumGuncelle'))
  }
}

const talepSil = (data) => {
  confirm.require({
    message: t('common.confirmDelete'),
    header: t('masraflar.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('masraflar.evetSil'),
    rejectLabel: t('common.cancel'),
    accept: async () => {
      try {
        await satinalmaTalepAPI.delete(data.id)
        await talepleriYukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || err?.message || t('satinalma.hataTalepSil'))
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
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('satinalma.hataSiparisKaydet'))
  }
  kaydediliyor.value = false
}

const siparisDurumGuncelle = async (data, durum) => {
  try {
    await satinalmaSiparisAPI.durumGuncelle(data.id, durum)
    await siparisleriYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('satinalma.hataDurumGuncelle'))
  }
}

const siparisFaturayaCevir = (data) => {
  confirm.require({
    message: t('satinalma.faturayaCevirMesaj', { no: data.siparisNo }),
    header: t('satinalma.faturayaCevirBaslik'),
    icon: 'pi pi-file',
    acceptLabel: t('satinalma.evetDonustur'),
    rejectLabel: t('common.cancel'),
    accept: async () => {
      try {
        await satinalmaSiparisAPI.faturayaCevir(data.id)
        toastBildirim.basarili(t('satinalma.donusturuldu'))
        await siparisleriYukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || err?.message || t('satinalma.hataFaturayaCevir'))
      }
    },
    reject: () => {}
  })
}

const siparisSil = (data) => {
  confirm.require({
    message: t('common.confirmDelete'),
    header: t('masraflar.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('masraflar.evetSil'),
    rejectLabel: t('common.cancel'),
    accept: async () => {
      try {
        await satinalmaSiparisAPI.delete(data.id)
        await siparisleriYukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || err?.message || t('satinalma.hataSiparisSil'))
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
