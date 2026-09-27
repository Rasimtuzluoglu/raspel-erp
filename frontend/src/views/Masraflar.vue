<template>
  <div class="masraf-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        {{ t('masraflar.title') }}
      </h1>
      <Button
        :label="t('masraflar.yeniMasraf')"
        icon="pi pi-plus"
        @click="dialogAc()"
      />
    </div>

    <AppDataTable
      :value="list"
      striped-rows
      :loading="yukleniyor"
      :paginator="false"
      :empty-message="t('masraflar.empty')"
    >
      <Column
        field="tarih"
        :header="t('common.date')"
        sortable
      >
        <template #body="{ data }">
          {{ formatDate(data.tarih) }}
        </template>
      </Column>
      <Column
        field="kategori"
        :header="t('masraflar.kategori')"
        sortable
      />
      <Column
        field="aciklama"
        :header="t('common.description')"
      />
      <Column
        field="tutar"
        :header="t('common.amount')"
      >
        <template #body="{ data }">
          <span class="gizli-veri">{{ formatCurrency(data.tutar) }}</span>
        </template>
      </Column>
      <Column
        field="belgeNo"
        :header="t('masraflar.belgeNo')"
      />
      <Column
        :header="t('common.actions')"
        style="width: 120px"
      >
        <template #body="{ data }">
          <Button
            icon="pi pi-pencil"
            :aria-label="$t('common.edit')"
            class="p-button-rounded p-button-text"
            @click="dialogAc(data)"
          />
          <Button
            icon="pi pi-trash"
            :aria-label="$t('common.delete')"
            class="p-button-rounded p-button-text"
            @click="sil(data)"
          />
        </template>
      </Column>
      <template #empty>
        <EmptyState
          v-if="!yukleniyor && list.length === 0"
          :message="t('masraflar.empty')"
          :sub-message="t('masraflar.emptyHint')"
          icon="pi pi-receipt"
          :action-label="t('masraflar.yeniMasraf')"
          action-icon="pi pi-plus"
          @action="dialogAc()"
        />
      </template>
    </AppDataTable>

    <Dialog
      v-model:visible="dialog"
      :header="dialogHeader"
      modal
      :style="{ width: '520px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('masraflar.tarihZorunlu') }}</label><DatePicker
            v-model="form.tarih"
            date-format="dd.mm.yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('masraflar.kategori') }}</label><InputText
            v-model="form.kategori"
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
          <label>{{ t('masraflar.tutar') }}</label><InputNumber
            v-model="form.tutar"
            mode="currency"
            currency="TRY"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('masraflar.kdvOrani') }}</label><InputNumber
            v-model="form.kdvOrani"
            :min="0"
            :max="100"
            :suffix="' %'"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('masraflar.belgeNo') }}</label><InputText
            v-model="form.belgeNo"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('masraflar.odemeKasa') }}</label><Dropdown
            v-model="form.kasaId"
            :options="kasalar"
            option-label="ad"
            option-value="id"
            :placeholder="t('masraflar.opsiyonel')"
            show-clear
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('masraflar.odemeBanka') }}</label><Dropdown
            v-model="form.bankaId"
            :options="bankalar"
            option-label="ad"
            option-value="id"
            :placeholder="t('masraflar.opsiyonel')"
            show-clear
            class="w-full"
          />
        </div>
        <div
          v-if="!duzenleme && form.tutar > 0"
          class="field full-width kdv-ozet"
        >
          <span>{{ t('masraflar.matrah') }}: <strong>{{ formatCurrency(matrah) }}</strong></span>
          <span>{{ t('masraflar.kdvTutar') }}: <strong>{{ formatCurrency(kdvTutar) }}</strong></span>
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
import { ref, onMounted, computed } from 'vue'
import { unwrapList } from '../api/utils/unwrap.js'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { masrafAPI, kasaAPI, bankaAPI } from '../api/index.js'
import EmptyState from '../components/EmptyState.vue'
import { formatCurrency } from '../utils/format.js'
import { useI18n } from 'vue-i18n'

const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { t } = useI18n()
const list = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const kasalar = ref([])
const bankalar = ref([])
const bosForm = () => ({
  tarih: new Date(), kategori: '', aciklama: '', tutar: 0, belgeNo: '',
  kdvOrani: 20, kasaId: null, bankaId: null
})
const form = ref(bosForm())

// Tutar KDV DAHİL kabul edilir; matrah ve KDV kullanıcıya gösterilir.
const kdvTutar = computed(() => {
  const tutar = Number(form.value.tutar || 0)
  const oran = Number(form.value.kdvOrani || 0)
  if (!tutar || !oran) return 0
  const net = tutar / (1 + oran / 100)
  return Math.round((tutar - net) * 100) / 100
})
const matrah = computed(() => Math.round((Number(form.value.tutar || 0) - kdvTutar.value) * 100) / 100)

const dialogHeader = computed(() => (duzenleme.value ? t('masraflar.duzenle') : t('masraflar.yeniMasraf')))

import { formatTarih as formatDate, getLocalDateString } from '../utils/format.js'

onMounted(async () => {
  yukleniyor.value = true
  try {
    const r = await masrafAPI.getAll()
    list.value = unwrapList(r)
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('masraflar.hataYukleme'))
  }
  // Kasa/banka listeleri hata verse bile masraf listesi gösterilir.
  try {
    const [kasaRes, bankaRes] = await Promise.all([kasaAPI.getAllKasalar(), bankaAPI.getAll()])
    kasalar.value = unwrapList(kasaRes)
    bankalar.value = unwrapList(bankaRes)
  } catch {
    kasalar.value = []
    bankalar.value = []
  }
  yukleniyor.value = false
})

const dialogAc = (data) => {
  duzenleme.value = !!data
  form.value = data
    ? { ...data, tarih: data.tarih ? new Date(data.tarih) : new Date() }
    : bosForm()
  dialog.value = true
}

const kaydet = async () => {
  if (form.value.kasaId && form.value.bankaId) {
    toastBildirim.uyari(t('masraflar.tekHesap'))
    return
  }
  kaydediliyor.value = true
  try {
    const payload = {
      ...form.value,
      tarih: form.value.tarih ? getLocalDateString(form.value.tarih) : null,
      // Ödeme hesabı yalnızca oluşturmada işlenir; güncellemede değiştirilmez.
      ...(duzenleme.value ? { kasaId: undefined, bankaId: undefined, odemeYontemi: undefined } : {})
    }
    if (duzenleme.value) {
      await masrafAPI.update(form.value.id, payload)
      toastBildirim.basarili(t('masraflar.guncellendi'))
    } else {
      await masrafAPI.create(payload)
      toastBildirim.basarili(t('masraflar.olusturuldu'))
    }
    dialog.value = false
    const r = await masrafAPI.getAll()
    list.value = unwrapList(r)
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('masraflar.islemBasarisiz'))
  }
  kaydediliyor.value = false
}

const sil = (data) => {
  confirm.require({
    message: t('masraflar.silOnayMesaj', { n: data.kategori || data.id }),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('common.evetSil'),
    rejectLabel: t('common.vazgec'),
    accept: async () => {
      try {
        await masrafAPI.delete(data.id)
        list.value = list.value.filter((x) => x.id !== data.id)
        toast.add({ severity: 'success', summary: t('masraflar.silindi'), detail: t('masraflar.masrafSilindi'), life: 3000 })
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('masraflar.silmeBasarisiz'))
      }
    }
  })
}
</script>

<style scoped>
.masraf-container {
  padding: 0;
}
.sayfa-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.form-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.kdv-ozet {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: var(--text-secondary);
  background: var(--surface-100, rgba(148, 163, 184, 0.08));
  border-radius: 8px;
  padding: 8px 12px;
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
