<template>
  <div class="siparisler-sayfasi">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        {{ t('siparisler.title') }}
      </h1>
      <Button
        :label="t('siparisler.newTeklif')"
        icon="pi pi-plus"
        @click="dialogAc()"
      />
    </div>

    <DataTable
      :value="siparisler"
      striped-rows
      :loading="yukleniyor"
    >
      <Column
        field="siparisNo"
        :header="t('siparisler.colNo')"
        sortable
      />
      <Column
        field="tarih"
        :header="t('common.date')"
      />
      <Column
        field="cariHesapAdi"
        :header="t('siparisler.colMusteri')"
      />
      <Column
        field="tur"
        :header="t('siparisler.colTur')"
      />
      <Column
        field="genelToplam"
        :header="t('common.amount')"
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
        :header="t('siparisler.colSofor')"
        style="width: 190px"
      >
        <template #body="{ data }">
          <Dropdown
            :model-value="data.driverId"
            :options="suruculer"
            option-label="ad"
            option-value="id"
            filter
            :placeholder="t('siparisler.soforAta')"
            class="w-full"
            :show-clear="true"
            @update:model-value="(d) => soforAta(data, d)"
          />
        </template>
      </Column>
      <Column
        :header="t('common.actions')"
        style="width: 220px"
      >
        <template #body="{ data }">
          <Button
            v-if="data.durum === 'TEKLIF'"
            icon="pi pi-check-circle"
            class="p-button-rounded p-button-text p-button-info"
            :title="t('siparisler.sipariseCevir')"
            @click="durumGuncelle(data, 'SIPARIS')"
          />
          <Button
            v-if="data.durum === 'SIPARIS'"
            icon="pi pi-file"
            class="p-button-rounded p-button-text p-button-success"
            :title="t('siparisler.faturalastir')"
            @click="durumGuncelle(data, 'FATURA_KESILDI')"
          />
          <Button
            v-if="data.durum === 'FATURA_KESILDI' || data.durum === 'IPTAL'"
            icon="pi pi-undo"
            class="p-button-rounded p-button-text p-button-help"
            :title="t('siparisler.sipariseGeriAl')"
            @click="durumGuncelle(data, 'SIPARIS')"
          />
          <Button
            v-if="data.durum !== 'IPTAL' && data.durum !== 'FATURA_KESILDI'"
            icon="pi pi-times-circle"
            class="p-button-rounded p-button-text p-button-warning"
            :title="t('siparisler.iptalEt')"
            @click="durumGuncelle(data, 'IPTAL')"
          />
          <Button
            icon="pi pi-briefcase"
            class="p-button-rounded p-button-text p-button-info"
            :title="t('siparisler.isEmriOlustur')"
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
      :message="t('siparisler.empty')"
      :sub-message="t('siparisler.emptyHint')"
      icon="pi pi-shopping-cart"
      :action-label="t('siparisler.newTeklif')"
      action-icon="pi pi-plus"
      @action="dialogAc()"
    />

    <Dialog
      v-model:visible="dialog"
      :header="t('siparisler.dialogTitle')"
      modal
      :style="{ width: '550px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('siparisler.teklifNo') }}</label><InputText
            v-model="form.siparisNo"
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
          <label>{{ t('siparisler.musteri') }}</label>
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
          <label>{{ t('common.description') }}</label><Textarea
            v-model="form.aciklama"
            rows="2"
            class="w-full"
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

    <Dialog
      v-model:visible="isEmriDialog"
      :header="t('siparisler.isEmriTitle')"
      :modal="true"
      style="width: 480px"
    >
      <div
        v-if="isEmriSiparis"
        class="isemri-siparis"
      >
        {{ t('siparisler.isEmriSiparis') }} <strong>{{ isEmriSiparis.siparisNo }}</strong>
      </div>
      <div class="form-group">
        <label>{{ t('siparisler.atanacakPersonel') }}</label>
        <Dropdown
          v-model="isEmriPersonelId"
          :options="personeller"
          option-label="label"
          option-value="value"
          :placeholder="t('siparisler.personelSecin')"
          filter
          class="w-full"
        />
      </div>
      <div class="form-group">
        <label>{{ t('common.description') }}</label>
        <Textarea
          v-model="isEmriAciklama"
          rows="3"
          :placeholder="t('siparisler.isEmriAciklama')"
          class="w-full"
        />
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="isEmriDialog = false"
        />
        <Button
          :label="t('siparisler.olustur')"
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
import { useI18n } from 'vue-i18n'
import { siparisAPI, cariHesapAPI, personelAPI, teslimatAPI } from '../api/index.js'
import EmptyState from '../components/EmptyState.vue'
import { formatCurrency } from '../utils/format.js'
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { t } = useI18n()

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
    suruculeriYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('siparisler.hataYukleme'))
  }
  yukleniyor.value = false
})

const personeller = ref([])
const suruculer = ref([])
const isEmriDialog = ref(false)
const isEmriSiparis = ref(null)
const isEmriPersonelId = ref(null)
const isEmriAciklama = ref('')
const isEmriKaydediliyor = ref(false)

const suruculeriYukle = async () => {
  try {
    const r = await teslimatAPI.suruculer()
    suruculer.value = r.data || []
  } catch {
    suruculer.value = []
  }
}

const soforAta = async (siparis, driverId) => {
  try {
    await siparisAPI.soforAta(siparis.id, driverId)
    siparis.driverId = driverId
    const surucu = suruculer.value.find((s) => s.id === driverId)
    siparis.driverAd = surucu?.ad || ''
    toastBildirim.basarili(t('siparisler.soforAtandi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('siparisler.soforAtanamadi'))
  }
}

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
    toastBildirim.basarili(t('siparisler.isEmriOlusturuldu'))
    isEmriDialog.value = false
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('siparisler.isEmriOlusturulamadi'))
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
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('siparisler.hataKaydet'))
  }
  kaydediliyor.value = false
}

const durumGuncelle = async (data, durum) => {
  try {
    await siparisAPI.durumGuncelle(data.id, durum)
    const r = await siparisAPI.getAll()
    siparisler.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('siparisler.hataDurum'))
  }
}

const sil = (data) => {
  confirm.require({
    message: t('common.confirmDelete'),
    header: t('siparisler.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('siparisler.evetSil'),
    rejectLabel: t('common.cancel'),
    accept: async () => {
      try {
        await siparisAPI.delete(data.id)
        siparisler.value = siparisler.value.filter((s) => s.id !== data.id)
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || err?.message || t('siparisler.hataSil'))
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
