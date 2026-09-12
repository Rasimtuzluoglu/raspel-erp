<template>
  <div class="stoksayim-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        {{ t('stokSayim.title') }}
      </h1>
      <div class="header-actions">
        <div class="tarama-adet">
          <label>{{ t('stokSayim.taramaAdet') }}</label>
          <InputNumber
            v-model="taramaAdet"
            :min="1"
            class="adet-input"
          />
        </div>
        <Button
          :label="t('stokSayim.hizliSayim')"
          icon="pi pi-camera"
          class="p-button-outlined"
          @click="taramaModu = true"
        />
        <Button
          :label="t('stokSayim.yeniSayim')"
          icon="pi pi-plus"
          @click="dialogAc()"
        />
      </div>
    </div>

    <div
      v-if="sonTaramalar.length"
      class="tarama-paneli"
    >
      <div class="tarama-paneli-baslik">
        <i class="pi pi-check-circle" />
        <span>{{ t('stokSayim.tarananEklendi', { stok: sonTaramalar[0].ad, adet: sonTaramalar[0].adet, toplam: sonTaramalar[0].toplam }) }}</span>
      </div>
      <div class="tarama-chips">
        <Tag
          v-for="(tarama, i) in sonTaramalar"
          :key="i"
          :value="`${tarama.ad}: ${tarama.toplam}`"
          severity="success"
          class="tarama-chip"
        />
      </div>
    </div>

    <DataTable
      :value="list"
      striped-rows
      :loading="yukleniyor"
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
        field="stokAdi"
        :header="t('stokSayim.urun')"
        sortable
      />
      <Column
        field="beklenenMiktar"
        :header="t('stokSayim.beklenen')"
      >
        <template #body="{ data }">
          {{ formatNumber(data.beklenenMiktar) }}
        </template>
      </Column>
      <Column
        field="sayilanMiktar"
        :header="t('stokSayim.sayilan')"
      >
        <template #body="{ data }">
          {{ formatNumber(data.sayilanMiktar) }}
        </template>
      </Column>
      <Column
        field="fark"
        :header="t('stokSayim.fark')"
      >
        <template #body="{ data }">
          <span :class="(data.fark ?? data.sayilanMiktar - data.beklenenMiktar) >= 0 ? 'positive' : 'negative'">
            {{ formatNumber(data.fark ?? data.sayilanMiktar - data.beklenenMiktar) }}
          </span>
        </template>
      </Column>
      <Column
        field="durum"
        :header="t('common.status')"
      >
        <template #body="{ data }">
          <Tag
            :value="data.durum"
            :severity="data.durum === 'TAMAMLANDI' ? 'success' : data.durum === 'IPTAL' ? 'danger' : 'warn'"
          />
        </template>
      </Column>
      <Column
        :header="t('stokSayim.islem')"
        style="width: 200px"
      >
        <template #body="{ data }">
          <Button
            v-if="data.durum !== 'TAMAMLANDI'"
            icon="pi pi-check-circle"
            class="p-button-rounded p-button-text p-button-success"
            :title="t('stokSayim.tamamla')"
            @click="durumGuncelle(data, 'TAMAMLANDI')"
          />
          <Button
            v-if="data.durum !== 'IPTAL'"
            icon="pi pi-times-circle"
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

    <Dialog
      v-model:visible="dialog"
      :header="dialogHeader"
      modal
      :style="{ width: '500px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('stokSayim.tarihZorunlu') }}</label><DatePicker
            v-model="form.tarih"
            date-format="dd/mm/yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('stokSayim.urunZorunlu') }}</label>
          <Dropdown
            v-model="form.stokId"
            :options="stokListesi"
            option-label="ad"
            option-value="id"
            :placeholder="t('stokSayim.urunSec')"
            class="w-full"
            filter
            @change="onStokSec"
          />
        </div>
        <div class="field">
          <label>{{ t('stokSayim.beklenenMiktar') }}</label><InputNumber
            v-model="form.beklenenMiktar"
            class="w-full"
            :min="0"
            disabled
          />
        </div>
        <div class="field">
          <label>{{ t('stokSayim.sayilanMiktarZorunlu') }}</label><InputNumber
            v-model="form.sayilanMiktar"
            class="w-full"
            :min="0"
          />
        </div>
        <div class="field">
          <label>{{ t('stokSayim.fark') }}</label>
          <span
            :class="{
              positive: form.sayilanMiktar - form.beklenenMiktar >= 0,
              negative: form.sayilanMiktar - form.beklenenMiktar < 0
            }"
            style="font-weight: 700; font-size: 18px"
          >
            {{ formatNumber(form.sayilanMiktar - form.beklenenMiktar) }}
          </span>
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
      v-model:visible="taramaModu"
      @scan="barkodOkundu"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { stokSayimAPI, stokAPI } from '../api/index.js'
import { useI18n } from 'vue-i18n'
import BarcodeScannerModal from '../components/BarcodeScannerModal.vue'

const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { t } = useI18n()
const list = ref([])
const stokListesi = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const taramaModu = ref(false)
const taramaAdet = ref(1)
const sonTaramalar = ref([])
const form = ref({ tarih: new Date(), stokId: null, beklenenMiktar: 0, sayilanMiktar: 0 })

const dialogHeader = computed(() => (duzenleme.value ? t('stokSayim.sayimDuzenle') : t('stokSayim.yeniSayim')))

import { formatTarih as formatDate } from '../utils/format.js'
const formatNumber = (v) => {
  if (v === null || v === undefined) return '0'
  return new Intl.NumberFormat('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(v)
}

onMounted(async () => {
  yukleniyor.value = true
  try {
    const [sR, stR] = await Promise.all([stokSayimAPI.getAll(), stokAPI.getAll()])
    list.value = sR.data?.content || sR.data || []
    stokListesi.value = stR.data.content || stR.data
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('stokSayim.hataYukleme'))
  }
  yukleniyor.value = false
})

const barkodOkundu = async (kod) => {
  try {
    const res = await stokSayimAPI.tara({ barkod: kod, adet: taramaAdet.value })
    const sayim = res.data
    sonTaramalar.value.unshift({ ad: sayim.stokAdi || kod, adet: taramaAdet.value, toplam: sayim.sayilanMiktar })
    if (sonTaramalar.value.length > 6) sonTaramalar.value.length = 6
    toastBildirim.basarili(t('stokSayim.tarananEklendi', { stok: sayim.stokAdi || kod, adet: taramaAdet.value, toplam: sayim.sayilanMiktar }))
    const r = await stokSayimAPI.getAll()
    list.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('stokSayim.barkodBulunamadi'))
  }
}

const dialogAc = (data) => {
  duzenleme.value = !!data
  form.value = data
    ? { ...data, tarih: data.tarih ? new Date(data.tarih) : new Date() }
    : { tarih: new Date(), stokId: null, beklenenMiktar: 0, sayilanMiktar: 0 }
  dialog.value = true
}

const onStokSec = () => {
  const stok = stokListesi.value.find((s) => s.id === form.value.stokId)
  form.value.beklenenMiktar = stok?.miktar ?? 0
}

const kaydet = async () => {
  kaydediliyor.value = true
  try {
    const fark = form.value.sayilanMiktar - form.value.beklenenMiktar
    const payload = {
      ...form.value,
      tarih: form.value.tarih?.toISOString?.().split('T')[0] ?? form.value.tarih,
      fark
    }
    if (duzenleme.value) {
      await stokSayimAPI.update(form.value.id, payload)
      toastBildirim.basarili(t('stokSayim.guncellendi'))
    } else {
      await stokSayimAPI.create(payload)
      toastBildirim.basarili(t('stokSayim.olusturuldu'))
    }
    dialog.value = false
    const r = await stokSayimAPI.getAll()
    list.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('stokSayim.islemBasarisiz'))
  }
  kaydediliyor.value = false
}

const durumGuncelle = async (data, durum) => {
  try {
    await stokSayimAPI.durumGuncelle(data.id, durum)
    const r = await stokSayimAPI.getAll()
    list.value = r.data?.content || r.data || []
    toastBildirim.basarili(t('stokSayim.durumGuncellendi', { durum }))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('stokSayim.durumHata'))
  }
}

const sil = (data) => {
  confirm.require({
    message: t('stokSayim.silOnayMesaj'),
    header: t('masraflar.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('masraflar.evetSil'),
    rejectLabel: t('common.cancel'),
    accept: async () => {
      try {
        await stokSayimAPI.delete(data.id)
        list.value = list.value.filter((x) => x.id !== data.id)
        toast.add({ severity: 'success', summary: t('stokSayim.silindi'), detail: t('stokSayim.sayimSilindi'), life: 3000 })
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('stokSayim.silmeBasarisiz'))
      }
    }
  })
}
</script>

<style scoped>
.stoksayim-container {
  padding: 0;
}
.sayfa-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 12px;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.tarama-adet {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}
.adet-input {
  width: 90px;
}
.tarama-paneli {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px 14px;
  margin-bottom: 16px;
  background: rgba(74, 222, 128, 0.08);
  border: 1px solid rgba(74, 222, 128, 0.35);
  border-radius: 8px;
}
.tarama-paneli-baslik {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 700;
  color: #4ade80;
}
.tarama-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.tarama-chip {
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
.positive {
  color: #4ade80;
}
.negative {
  color: #f87171;
}
</style>
