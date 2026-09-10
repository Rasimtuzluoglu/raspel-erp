<template>
  <div class="butce-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        {{ t('butceler.title') }}
      </h1>
      <Button
        :label="t('butceler.yeniButce')"
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
        field="ad"
        :header="t('butceler.ad')"
        sortable
      />
      <Column
        field="yil"
        :header="t('butceler.yil')"
        sortable
      />
      <Column
        field="ay"
        :header="t('butceler.ay')"
        sortable
      />
      <Column
        field="tur"
        :header="t('butceler.tur')"
      >
        <template #body="{ data }">
          <Tag
            :value="data.tur === 'GELIR' ? t('butceler.gelir') : t('butceler.gider')"
            :severity="data.tur === 'GELIR' ? 'success' : 'danger'"
          />
        </template>
      </Column>
      <Column
        field="tutar"
        :header="t('common.amount')"
      >
        <template #body="{ data }">
          {{ formatCurrency(data.tutar) }}
        </template>
      </Column>
      <Column
        field="kategori"
        :header="t('butceler.kategori')"
      />
      <Column
        :header="t('butceler.islem')"
        style="width: 120px"
      >
        <template #body="{ data }">
          <Button
            icon="pi pi-pencil"
            class="p-button-rounded p-button-text"
            @click="dialogAc(data)"
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
      :message="t('butceler.empty')"
      :sub-message="t('butceler.emptyHint')"
      icon="pi pi-chart-bar"
      :action-label="t('butceler.yeniButce')"
      action-icon="pi pi-plus"
      @action="dialogAc()"
    />

    <Card class="gerceklesen-kart">
      <template #title>
        <div class="gerceklesen-baslik">
          <span>
            <i class="pi pi-chart-line" /> {{ t('butceler.butceVsGerceklesen') }}
          </span>
          <div class="gerceklesen-filtre">
            <Select
              v-model="raporYil"
              :options="yilSecenekleri"
              class="yil-select"
            />
            <Select
              v-model="raporAy"
              :options="aySecenekleri"
              option-label="label"
              option-value="value"
              :placeholder="t('butceler.tumAylar')"
              class="ay-select"
              show-clear
            />
            <Button
              icon="pi pi-refresh"
              class="p-button-sm"
              @click="raporYukle"
            />
            <Button
              icon="pi pi-file-pdf"
              :label="t('butceler.pdf')"
              class="p-button-sm p-button-secondary"
              @click="raporPdfIndir"
            />
          </div>
        </div>
      </template>
      <template #content>
        <DataTable
          :value="raporList"
          striped-rows
          size="small"
          :loading="raporYukleniyor"
        >
          <Column
            field="kategori"
            :header="t('butceler.kategori')"
          />
          <Column :header="t('butceler.butce')">
            <template #body="{ data }">
              {{ formatCurrency(data.butce) }}
            </template>
          </Column>
          <Column :header="t('butceler.gerceklesen')">
            <template #body="{ data }">
              {{ formatCurrency(data.gerceklesen) }}
            </template>
          </Column>
          <Column :header="t('butceler.sapma')">
            <template #body="{ data }">
              <span :class="(data.sapma || 0) > 0 ? 'negative' : 'positive'">{{ formatCurrency(data.sapma) }}</span>
            </template>
          </Column>
          <Column :header="t('butceler.kullanim')">
            <template #body="{ data }">
              {{ data.kullanimYuzdesi != null ? data.kullanimYuzdesi + '%' : '-' }}
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <Dialog
      v-model:visible="dialog"
      :header="dialogHeader"
      modal
      :style="{ width: '500px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('butceler.adZorunlu') }}</label><InputText
            v-model="form.ad"
            class="w-full"
          />
        </div>
        <div class="field-row">
          <div class="field">
            <label>{{ t('butceler.yil') }}</label><InputNumber
              v-model="form.yil"
              class="w-full"
              :min="2000"
              :max="2100"
              :use-grouping="false"
            />
          </div>
          <div class="field">
            <label>{{ t('butceler.ayAralik') }}</label><InputNumber
              v-model="form.ay"
              class="w-full"
              :min="1"
              :max="12"
              :use-grouping="false"
            />
          </div>
        </div>
        <div class="field">
          <label>{{ t('butceler.turZorunlu') }}</label>
          <Dropdown
            v-model="form.tur"
            :options="turSecenekleri"
            :placeholder="t('common.select')"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('common.amount') }}</label><InputNumber
            v-model="form.tutar"
            mode="currency"
            currency="TRY"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('butceler.kategori') }}</label><InputText
            v-model="form.kategori"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('common.description') }}</label><Textarea
            v-model="form.aciklama"
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
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { butceAPI, raporAPI } from '../api/index.js'
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
const form = ref({
  ad: '',
  yil: new Date().getFullYear(),
  ay: new Date().getMonth() + 1,
  tur: 'GELIR',
  tutar: 0,
  kategori: '',
  aciklama: ''
})
const turSecenekleri = ['GELIR', 'GIDER']

const raporYil = ref(new Date().getFullYear())
const raporAy = ref(null)
const raporList = ref([])
const raporYukleniyor = ref(false)
const yilSecenekleri = computed(() => {
  const yil = new Date().getFullYear()
  return Array.from({ length: 6 }, (_, i) => yil - i)
})
const aySecenekleri = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12].map((a) => ({ label: a, value: a }))

const raporYukle = async () => {
  raporYukleniyor.value = true
  try {
    const params = { yil: raporYil.value }
    if (raporAy.value) params.ay = raporAy.value
    const r = await raporAPI.butceGerceklesen(params)
    raporList.value = r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('butceler.hataRapor'))
  } finally {
    raporYukleniyor.value = false
  }
}

const raporPdfIndir = async () => {
  try {
    const params = { yil: raporYil.value }
    if (raporAy.value) params.ay = raporAy.value
    const r = await raporAPI.butceGerceklesenPdf(params)
    const blob = new Blob([r.data], { type: 'application/pdf' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `butce-gerceklesen-${raporYil.value}${raporAy.value ? '-' + raporAy.value : ''}.pdf`
    a.click()
    URL.revokeObjectURL(url)
  } catch (err) {
    toastBildirim.hata(t('butceler.hataPdf'))
  }
}

const dialogHeader = computed(() => (duzenleme.value ? t('butceler.butceDuzenle') : t('butceler.yeniButce')))


onMounted(async () => {
  yukleniyor.value = true
  try {
    const r = await butceAPI.getAll()
    list.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('butceler.hataYukleme'))
  }
  yukleniyor.value = false
  raporYukle()
})

const dialogAc = (data) => {
  duzenleme.value = !!data
  form.value = data
    ? { ...data }
    : {
        ad: '',
        yil: new Date().getFullYear(),
        ay: new Date().getMonth() + 1,
        tur: 'GELIR',
        tutar: 0,
        kategori: '',
        aciklama: ''
      }
  dialog.value = true
}

const kaydet = async () => {
  kaydediliyor.value = true
  try {
    if (duzenleme.value) {
      await butceAPI.update(form.value.id, form.value)
      toastBildirim.basarili(t('butceler.guncellendi'))
    } else {
      await butceAPI.create(form.value)
      toastBildirim.basarili(t('butceler.olusturuldu'))
    }
    dialog.value = false
    const r = await butceAPI.getAll()
    list.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('butceler.islemBasarisiz'))
  }
  kaydediliyor.value = false
}

const sil = (data) => {
  confirm.require({
    message: t('butceler.silOnayMesaj', { ad: data.ad }),
    header: t('masraflar.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('masraflar.evetSil'),
    rejectLabel: t('common.cancel'),
    accept: async () => {
      try {
        await butceAPI.delete(data.id)
        list.value = list.value.filter((x) => x.id !== data.id)
        toast.add({ severity: 'success', summary: t('butceler.silindi'), detail: t('butceler.butceSilindi'), life: 3000 })
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('butceler.silmeBasarisiz'))
      }
    }
  })
}
</script>

<style scoped>
.butce-container {
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
.field-row {
  display: flex;
  gap: 12px;
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
.gerceklesen-kart {
  margin-top: 24px;
}
.gerceklesen-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  flex-wrap: wrap;
  gap: 8px;
}
.gerceklesen-filtre {
  display: flex;
  gap: 8px;
  align-items: center;
}
.yil-select {
  width: 90px;
}
.ay-select {
  width: 120px;
}
.positive {
  color: #10b981;
  font-weight: 600;
}
.negative {
  color: #ef4444;
  font-weight: 600;
}
</style>
