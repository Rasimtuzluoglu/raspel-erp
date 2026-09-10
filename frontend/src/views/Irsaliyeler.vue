<template>
  <div class="irsaliye-sayfasi">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        {{ t('irsaliyeler.title') }}
      </h1>
      <Button
        :label="t('irsaliyeler.yeniIrsaliye')"
        icon="pi pi-plus"
        @click="dialogAc()"
      />
    </div>

    <DataTable
      :value="list"
      striped-rows
      :loading="yukleniyor"
    >
      <Column
        field="irsaliyeNo"
        :header="t('irsaliyeler.irsaliyeNo')"
        sortable
      />
      <Column
        field="tarih"
        :header="t('common.date')"
      />
      <Column
        field="cariHesapAdi"
        :header="t('irsaliyeler.cariHesap')"
      />
      <Column
        field="tur"
        :header="t('irsaliyeler.tur')"
      />
      <Column
        field="durum"
        :header="t('common.status')"
      >
        <template #body="{ data }">
          <Tag
            :value="data.durum"
            :severity="data.durum === 'KESILDI' ? 'success' : data.durum === 'IPTAL' ? 'danger' : 'warn'"
          />
        </template>
      </Column>
      <Column
        :header="t('irsaliyeler.islem')"
        style="width: 140px"
      >
        <template #body="{ data }">
          <Button
            v-if="data.durum === 'TASLAK'"
            icon="pi pi-check"
            class="p-button-rounded p-button-text p-button-success"
            :title="t('irsaliyeler.kes')"
            @click="durumGuncelle(data, 'KESILDI')"
          />
          <Button
            v-if="data.durum !== 'IPTAL'"
            icon="pi pi-times"
            class="p-button-rounded p-button-text p-button-danger"
            :title="t('common.cancel')"
            @click="durumGuncelle(data, 'IPTAL')"
          />
          <Button
            icon="pi pi-trash"
            class="p-button-rounded p-button-text"
            @click="sil(data)"
          />
        </template>
      </Column>
    </DataTable>

    <EmptyState
      v-if="!yukleniyor && list.length === 0"
      :message="t('irsaliyeler.empty')"
      :sub-message="t('irsaliyeler.emptyHint')"
      icon="pi pi-truck"
      :action-label="t('irsaliyeler.yeniIrsaliye')"
      action-icon="pi pi-plus"
      @action="dialogAc()"
    />

    <Dialog
      v-model:visible="dialog"
      :header="t('irsaliyeler.yeniIrsaliye')"
      modal
      :style="{ width: '500px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('irsaliyeler.irsaliyeNoZorunlu') }}</label><InputText
            v-model="form.irsaliyeNo"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('common.date') }}</label><DatePicker
            v-model="form.tarih"
            date-format="dd/mm/yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('irsaliyeler.cariHesapZorunlu') }}</label>
          <Dropdown
            v-model="form.cariHesapId"
            :options="cariler"
            option-label="ad"
            option-value="id"
            :placeholder="t('common.select')"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('irsaliyeler.tur') }}</label>
          <Dropdown
            v-model="form.tur"
            :options="['SATIS', 'ALIS']"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('common.description') }}</label><Textarea
            v-model="form.aciklama"
            rows="2"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('irsaliyeler.siparisOpsiyonel') }}</label>
          <Dropdown
            v-model="form.siparisId"
            :options="siparisler"
            option-label="siparisNo"
            option-value="id"
            filter
            :placeholder="t('irsaliyeler.siparisBagla')"
            class="w-full"
          />
        </div>
        <div class="field">
          <div class="kalem-baslik">
            <label>{{ t('irsaliyeler.kalemler') }}</label>
            <Button
              icon="pi pi-camera"
              :label="t('irsaliyeler.barkod')"
              class="p-button-sm p-button-outlined"
              @click="barkodAcik = true"
            />
          </div>
          <div
            v-for="(k, i) in form.kalemler"
            :key="i"
            class="kalem-satir"
          >
            <Dropdown
              v-model="k.stokId"
              :options="stoklar"
              option-label="ad"
              option-value="id"
              filter
              class="w-full"
              :placeholder="t('irsaliyeler.urun')"
            />
            <InputNumber
              v-model="k.miktar"
              :min="0"
              class="kalem-miktar"
              :placeholder="t('irsaliyeler.miktar')"
            />
            <Button
              icon="pi pi-trash"
              class="p-button-text p-button-danger p-button-sm"
              @click="form.kalemler.splice(i, 1)"
            />
          </div>
          <Button
            :label="t('irsaliyeler.kalemEkle')"
            icon="pi pi-plus"
            class="p-button-sm p-button-text"
            @click="form.kalemler.push({ stokId: null, miktar: 1, aciklama: '', birim: 'Adet' })"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="dialog = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="kaydet"
        />
      </template>
    </Dialog>

    <BarcodeScannerModal
      v-model:visible="barkodAcik"
      @scan="barkodOkundu"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { irsaliyeAPI, cariHesapAPI, stokAPI, siparisAPI } from '../api/index.js'
import EmptyState from '../components/EmptyState.vue'
import BarcodeScannerModal from '../components/BarcodeScannerModal.vue'
import { useI18n } from 'vue-i18n'
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { t } = useI18n()

const list = ref([])
const cariler = ref([])
const stoklar = ref([])
const siparisler = ref([])
const barkodAcik = ref(false)
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const dialog = ref(false)
const form = ref({ irsaliyeNo: '', tarih: new Date(), cariHesapId: null, tur: 'SATIS', aciklama: '', siparisId: null, kalemler: [] })

onMounted(async () => {
  yukleniyor.value = true
  try {
    const [r, c, s, sp] = await Promise.all([
      irsaliyeAPI.getAll(),
      cariHesapAPI.getAll(),
      stokAPI.getAll({ size: 500 }),
      siparisAPI.getAll({ size: 500 })
    ])
    list.value = r.data?.content || r.data || []
    cariler.value = c.data?.content || c.data || []
    stoklar.value = s.data?.content || s.data || []
    siparisler.value = sp.data?.content || sp.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('irsaliyeler.hataYukleme'))
  }
  yukleniyor.value = false
})

const dialogAc = () => {
  form.value = { irsaliyeNo: 'IRS-' + Date.now(), tarih: new Date(), cariHesapId: null, tur: 'SATIS', aciklama: '', siparisId: null, kalemler: [] }
  dialog.value = true
}

const barkodOkundu = (kod) => {
  const stok = stoklar.value.find((s) => s.barkod === kod || s.stokKodu === kod)
  if (!stok) {
    toastBildirim.uyari(t('irsaliyeler.barkodBulunamadi', { kod }))
    return
  }
  form.value.kalemler.push({ stokId: stok.id, miktar: 1, aciklama: stok.ad, birim: stok.birim || 'Adet' })
  toastBildirim.basarili(t('irsaliyeler.eklendi', { ad: stok.ad }))
}

const kaydet = async () => {
  kaydediliyor.value = true
  try {
    await irsaliyeAPI.create({
      ...form.value,
      tarih: form.value.tarih?.toISOString().split('T')[0],
      kalemler: form.value.kalemler.filter((k) => k.stokId && k.miktar > 0)
    })
    dialog.value = false
    const r = await irsaliyeAPI.getAll()
    list.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('irsaliyeler.hataKaydet'))
  }
  kaydediliyor.value = false
}
const durumGuncelle = async (data, durum) => {
  try {
    await irsaliyeAPI.durumGuncelle(data.id, durum)
    const r = await irsaliyeAPI.getAll()
    list.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('irsaliyeler.hataDurum'))
  }
}
const sil = (data) => {
  confirm.require({
    message: t('common.confirmDelete'),
    header: t('masraflar.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('masraflar.evetSil'),
    rejectLabel: t('common.cancel'),
    accept: async () => {
      try {
        await irsaliyeAPI.delete(data.id)
        list.value = list.value.filter((x) => x.id !== data.id)
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || err?.message || t('irsaliyeler.hataSil'))
      }
    },
    reject: () => {}
  })
}
</script>

<style scoped>
.irsaliye-sayfasi {
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
.kalem-baslik {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}
.kalem-satir {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
.kalem-miktar {
  width: 110px;
}
</style>
