<template>
  <div class="ceksenet-sayfasi">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        {{ t('cekSenet.title') }}
      </h1>
      <Button
        :label="t('cekSenet.yeniCekSenet')"
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
        field="tur"
        :header="t('cekSenet.tur')"
      >
        <template #body="{ data }">
          <Tag
            :value="data.tur"
            :severity="data.tur === 'CEK' ? 'info' : 'warn'"
          />
        </template>
      </Column>
      <Column
        field="cariHesapAdi"
        :header="t('cekSenet.cariHesap')"
      />
      <Column
        field="cekNo"
        :header="t('cekSenet.cekNo')"
      />
      <Column
        field="bankaAdi"
        :header="t('cekSenet.banka')"
      />
      <Column
        field="vadeTarihi"
        :header="t('cekSenet.vade')"
      />
      <Column
        field="tutar"
        :header="t('common.amount')"
      >
        <template #body="{ data }">
          {{ formatCurrency(data.tutar) }}
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
              data.durum === 'PORTFOY'
                ? 'info'
                : data.durum === 'TAHSIL_EDILDI'
                  ? 'success'
                  : data.durum === 'PROTESTO'
                    ? 'danger'
                    : 'warn'
            "
          />
        </template>
      </Column>
      <Column
        :header="t('cekSenet.islem')"
        style="width: 160px"
      >
        <template #body="{ data }">
          <Button
            v-if="data.durum === 'PORTFOY'"
            icon="pi pi-check"
            class="p-button-rounded p-button-text p-button-success"
            :title="t('cekSenet.tahsilEt')"
            @click="durumGuncelle(data, 'TAHSIL_EDILDI')"
          />
          <Button
            v-if="data.durum === 'PORTFOY'"
            icon="pi pi-sync"
            class="p-button-rounded p-button-text p-button-info"
            :title="t('cekSenet.ciroEt')"
            @click="durumGuncelle(data, 'CIRO_EDILDI')"
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
      :message="t('cekSenet.empty')"
      :sub-message="t('cekSenet.emptyHint')"
      icon="pi pi-credit-card"
      :action-label="t('cekSenet.yeniCekSenet')"
      action-icon="pi pi-plus"
      @action="dialogAc()"
    />

    <Dialog
      v-model:visible="dialog"
      :header="t('cekSenet.yeniCekSenet')"
      modal
      :style="{ width: '500px' }"
    >
      <div class="form-grid">
        <div class="field-row">
          <div class="field">
            <label>{{ t('cekSenet.turZorunlu') }}</label>
            <Dropdown
              v-model="form.tur"
              :options="['CEK', 'SENET']"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('cekSenet.cariHesapZorunlu') }}</label>
            <Dropdown
              v-model="form.cariHesapId"
              :options="cariler"
              option-label="ad"
              option-value="id"
              :placeholder="t('common.select')"
              class="w-full"
            />
          </div>
        </div>
        <div class="field-row">
          <div class="field">
            <label>{{ t('cekSenet.banka') }}</label><InputText
              v-model="form.bankaAdi"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('cekSenet.cekNo') }}</label><InputText
              v-model="form.cekNo"
              class="w-full"
            />
          </div>
        </div>
        <div class="field-row">
          <div class="field">
            <label>{{ t('cekSenet.vadeTarihiZorunlu') }}</label><DatePicker
              v-model="form.vadeTarihi"
              date-format="dd/mm/yy"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('cekSenet.tutarZorunlu') }}</label><InputNumber
              v-model="form.tutar"
              mode="currency"
              currency="TRY"
              class="w-full"
            />
          </div>
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { cekSenetAPI, cariHesapAPI } from '../api/index.js'
import EmptyState from '../components/EmptyState.vue'
import { formatCurrency } from '../utils/format.js'
import { useI18n } from 'vue-i18n'
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { t } = useI18n()

const list = ref([])
const cariler = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const dialog = ref(false)
const form = ref({
  tur: 'CEK',
  cariHesapId: null,
  bankaAdi: '',
  cekNo: '',
  vadeTarihi: new Date(),
  tutar: null,
  aciklama: ''
})

onMounted(async () => {
  yukleniyor.value = true
  try {
    const [r, c] = await Promise.all([cekSenetAPI.getAll(), cariHesapAPI.getAll()])
    list.value = r.data?.content || r.data || []
    cariler.value = c.data?.content || c.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('cekSenet.hataYukleme'))
  }
  yukleniyor.value = false
})

const dialogAc = () => {
  form.value = {
    tur: 'CEK',
    cariHesapId: null,
    bankaAdi: '',
    cekNo: '',
    vadeTarihi: new Date(),
    tutar: null,
    aciklama: ''
  }
  dialog.value = true
}

const kaydet = async () => {
  kaydediliyor.value = true
  try {
    await cekSenetAPI.create({ ...form.value, vadeTarihi: form.value.vadeTarihi?.toISOString().split('T')[0] })
    dialog.value = false
    const r = await cekSenetAPI.getAll()
    list.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('cekSenet.hataKaydet'))
  }
  kaydediliyor.value = false
}

const durumGuncelle = async (data, durum) => {
  try {
    await cekSenetAPI.durumGuncelle(data.id, durum)
    const r = await cekSenetAPI.getAll()
    list.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('cekSenet.hataDurum'))
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
        await cekSenetAPI.delete(data.id)
        list.value = list.value.filter((x) => x.id !== data.id)
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || err?.message || t('cekSenet.hataSil'))
      }
    },
    reject: () => {}
  })
}
</script>

<style scoped>
.ceksenet-sayfasi {
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
.field-row {
  display: flex;
  gap: 16px;
}
.field-row .field {
  flex: 1;
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
