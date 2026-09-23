<template>
  <div class="iskonto-kurallari-sayfasi">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        <i class="pi pi-percentage" /> {{ t('iskontoKurallari.title') }}
      </h1>
      <div class="baslik-aksiyon">
        <Button
          :label="t('iskontoKurallari.yeniKural')"
          icon="pi pi-plus"
          @click="dialogAc"
        />
      </div>
    </div>

    <Message
      severity="info"
      :closable="false"
      class="bilgi-kutu"
    >
      <i class="pi pi-info-circle mr-2" />{{ t('iskontoKurallari.bilgi') }}
    </Message>

    <DataTable
      :value="kurallar"
      striped-rows
      responsive-layout="scroll"
      :loading="yukleniyor"
    >
      <template #empty>
        <EmptyState />
      </template>
      <Column
        field="ad"
        :header="t('iskontoKurallari.kuralAdi')"
        sortable
      />
      <Column
        field="kapsam"
        :header="t('iskontoKurallari.kapsam')"
      >
        <template #body="{ data }">
          <div class="kapsam-etiketleri">
            <Tag
              v-if="data.stokId"
              :value="stokAdi(data.stokId)"
              severity="info"
            />
            <Tag
              v-if="data.cariHesapId"
              :value="cariAdi(data.cariHesapId)"
              severity="secondary"
            />
            <Tag
              v-if="data.kategori"
              :value="data.kategori"
              severity="contrast"
            />
            <Tag
              v-if="!data.stokId && !data.cariHesapId && !data.kategori"
              :value="t('iskontoKurallari.genel')"
              severity="success"
            />
          </div>
        </template>
      </Column>
      <Column
        :header="t('iskontoKurallari.miktarAraligi')"
      >
        <template #body="{ data }">
          <span class="miktar-aralik">{{ aralikMetni(data) }}</span>
        </template>
      </Column>
      <Column
        field="iskontoOrani"
        :header="t('iskontoKurallari.oran')"
        style="width: 100px"
      >
        <template #body="{ data }">
          <span class="oran">%{{ data.iskontoOrani }}</span>
        </template>
      </Column>
      <Column
        field="oncelik"
        :header="t('iskontoKurallari.oncelik')"
        style="width: 100px"
      />
      <Column
        :header="t('common.status')"
        style="width: 100px"
      >
        <template #body="{ data }">
          <Tag
            :value="data.aktif ? t('iskontoKurallari.aktif') : t('iskontoKurallari.pasif')"
            :severity="data.aktif ? 'success' : 'danger'"
          />
        </template>
      </Column>
      <Column
        :header="t('common.actions')"
        style="width: 110px"
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
            class="p-button-rounded p-button-text p-button-danger"
            @click="sil(data)"
          />
        </template>
      </Column>
    </DataTable>

    <Dialog
      v-model:visible="dialog"
      :header="duzenleme ? t('iskontoKurallari.duzenle') : t('iskontoKurallari.yeniKural')"
      modal
      :style="{ width: '520px' }"
    >
      <div class="form-grid">
        <div class="field full-width">
          <label>{{ t('iskontoKurallari.kuralAdiZorunlu') }}</label>
          <InputText
            v-model="form.ad"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('iskontoKurallari.stok') }}</label>
          <Dropdown
            v-model="form.stokId"
            :options="stoklar"
            option-label="ad"
            option-value="id"
            show-clear
            filter
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('iskontoKurallari.cari') }}</label>
          <Dropdown
            v-model="form.cariHesapId"
            :options="cariler"
            option-label="ad"
            option-value="id"
            show-clear
            filter
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('iskontoKurallari.kategori') }}</label>
          <InputText
            v-model="form.kategori"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('iskontoKurallari.iskontoOraniZorunlu') }}</label>
          <InputNumber
            v-model="form.iskontoOrani"
            :min="0"
            :max="100"
            :min-fraction-digits="0"
            :max-fraction-digits="2"
            suffix=" %"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('iskontoKurallari.minAdet') }}</label>
          <InputNumber
            v-model="form.minAdet"
            :min="0"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('iskontoKurallari.maxAdet') }}</label>
          <InputNumber
            v-model="form.maxAdet"
            :min="0"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('iskontoKurallari.oncelik') }}</label>
          <InputNumber
            v-model="form.oncelik"
            :min="0"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('iskontoKurallari.gecerliBaslangic') }}</label>
          <DatePicker
            v-model="form.gecerliBaslangic"
            date-format="dd.mm.yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('iskontoKurallari.gecerliBitis') }}</label>
          <DatePicker
            v-model="form.gecerliBitis"
            date-format="dd.mm.yy"
            class="w-full"
          />
        </div>
        <div class="field full-width">
          <label>{{ t('iskontoKurallari.aciklama') }}</label>
          <Textarea
            v-model="form.aciklama"
            rows="2"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('iskontoKurallari.aktif') }}</label>
          <InputSwitch v-model="form.aktif" />
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
import { useConfirm } from 'primevue/useconfirm'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { iskontoKuraliAPI, stokAPI, cariHesapAPI } from '../api/index.js'
import { unwrapList } from '../api/utils/unwrap.js'
import { useI18n } from 'vue-i18n'
import { getLocalDateString } from '../utils/format.js'

const { t } = useI18n()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()

const kurallar = ref([])
const stoklar = ref([])
const cariler = ref([])
const yukleniyor = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const kaydediliyor = ref(false)
const seciliId = ref(null)
const form = ref(bosForm())

function bosForm() {
  return {
    ad: '',
    stokId: null,
    cariHesapId: null,
    kategori: '',
    iskontoOrani: 0,
    minAdet: null,
    maxAdet: null,
    oncelik: 100,
    gecerliBaslangic: null,
    gecerliBitis: null,
    aktif: true,
    aciklama: ''
  }
}

const stokAdi = (id) => stoklar.value.find((s) => s.id === id)?.ad || `#${id}`
const cariAdi = (id) => cariler.value.find((c) => c.id === id)?.ad || `#${id}`

const aralikMetni = (k) => {
  const min = k.minAdet
  const max = k.maxAdet
  if (min == null && max == null) return t('iskontoKurallari.tumMiktarlar')
  if (min != null && max != null) return `${min} - ${max}`
  if (min != null) return `${min} +`
  return `0 - ${max}`
}

const yukle = async () => {
  yukleniyor.value = true
  try {
    // Not: ilk 500 iskonto kurali gosterilir; ust sinir icin sunucu sayfalamasi gerekir.
    const r = await iskontoKuraliAPI.getAll({ size: 500 })
    kurallar.value = unwrapList(r)
  } catch {
    toastBildirim.hata(t('iskontoKurallari.hataYukleme'))
  } finally {
    yukleniyor.value = false
  }
}

onMounted(async () => {
  await yukle()
  try {
    const [s, c] = await Promise.all([stokAPI.getAll({ size: 500 }), cariHesapAPI.getAll({ size: 500 })])
    stoklar.value = unwrapList(s)
    cariler.value = unwrapList(c)
  } catch {
    /* opsiyonel yardımcı veriler */
  }
})

const dialogAc = (data) => {
  duzenleme.value = !!data
  seciliId.value = data?.id || null
  form.value = data
    ? {
        ...bosForm(),
        ...data,
        gecerliBaslangic: data.gecerliBaslangic ? new Date(data.gecerliBaslangic) : null,
        gecerliBitis: data.gecerliBitis ? new Date(data.gecerliBitis) : null
      }
    : bosForm()
  dialog.value = true
}

const kaydet = async () => {
  if (!form.value.ad || !form.value.ad.trim()) {
    toastBildirim.uyari(t('iskontoKurallari.kuralAdiGiriniz'))
    return
  }
  kaydediliyor.value = true
  try {
    const payload = {
      ...form.value,
      gecerliBaslangic: getLocalDateString(form.value.gecerliBaslangic),
      gecerliBitis: getLocalDateString(form.value.gecerliBitis)
    }
    if (duzenleme.value) {
      await iskontoKuraliAPI.update(seciliId.value, payload)
    } else {
      await iskontoKuraliAPI.create(payload)
    }
    dialog.value = false
    await yukle()
    toastBildirim.basarili(t('iskontoKurallari.kaydedildi'))
  } catch (err) {
    toastBildirim.hata(err.response?.data?.message || t('iskontoKurallari.kaydetHatasi'))
  } finally {
    kaydediliyor.value = false
  }
}

const sil = (data) => {
  confirm.require({
    message: t('common.confirmDelete'),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('common.evetSil'),
    rejectLabel: t('common.vazgec'),
    accept: async () => {
      try {
        await iskontoKuraliAPI.delete(data.id)
        await yukle()
        toastBildirim.basarili(t('iskontoKurallari.silindi'))
      } catch (err) {
        toastBildirim.hata(err.response?.data?.message || t('iskontoKurallari.silmeHatasi'))
      }
    },
    reject: () => {}
  })
}
</script>

<style scoped>
.iskonto-kurallari-sayfasi {
  padding: 0;
}
.sayfa-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 16px;
  flex-wrap: wrap;
}
.page-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary);
}
.page-title i {
  color: var(--accent);
}
.bilgi-kutu {
  margin-bottom: 16px;
}
.kapsam-etiketleri {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.miktar-aralik {
  font-size: 13px;
  color: var(--text-secondary);
}
.oran {
  font-weight: 700;
  color: var(--accent);
}
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field.full-width {
  grid-column: 1 / -1;
}
.field label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.4px;
}
.w-full {
  width: 100%;
}
@media (max-width: 700px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
  .field.full-width {
    grid-column: auto;
  }
}
</style>
