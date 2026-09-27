<template>
  <div class="maas-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        {{ t('maasBordro.title') }}
      </h1>
      <Button
        :label="t('maasBordro.yeniBordro')"
        icon="pi pi-plus"
        @click="dialogAc()"
      />
      <Button
        :label="t('maasBordro.bordroAyarlari')"
        icon="pi pi-cog"
        class="p-button-outlined"
        @click="ayarDialogAc"
      />
      <Button
        :label="t('maasBordro.topluUret')"
        icon="pi pi-users"
        class="p-button-outlined"
        @click="topluDialog = true"
      />
    </div>

    <DataTable
      :value="list"
      striped-rows
      :loading="yukleniyor"
    >
      <template #empty>
        <EmptyState />
      </template>
      <Column
        field="personelAdi"
        :header="t('maasBordro.personel')"
        sortable
      />
      <Column
        field="yil"
        :header="t('maasBordro.yil')"
        sortable
      />
      <Column
        field="ay"
        :header="t('maasBordro.ay')"
        sortable
      />
      <Column
        field="brutMaas"
        :header="t('maasBordro.brut')"
      >
        <template #body="{ data }">
          <span class="gizli-veri">{{ formatCurrency(data.brutMaas) }}</span>
        </template>
      </Column>
      <Column
        field="kesintiler"
        :header="t('maasBordro.kesintiler')"
      >
        <template #body="{ data }">
          <span class="gizli-veri">{{ formatCurrency(data.kesintiler) }}</span>
        </template>
      </Column>
      <Column
        field="netMaas"
        :header="t('maasBordro.net')"
      >
        <template #body="{ data }">
          <span class="gizli-veri">{{ formatCurrency(data.netMaas) }}</span>
        </template>
      </Column>
      <Column
        field="odemeTarihi"
        :header="t('maasBordro.odemeTarihi')"
      >
        <template #body="{ data }">
          {{ formatDate(data.odemeTarihi) }}
        </template>
      </Column>
      <Column
        field="durum"
        :header="t('maasBordro.durum')"
        style="width: 130px"
      >
        <template #body="{ data }">
          <span
            class="durum-rozet"
            :class="data.durum === 'ONAYLANDI' ? 'durum-onayli' : 'durum-taslak'"
          >
            {{ data.durum === 'ONAYLANDI' ? t('maasBordro.onaylandi') : t('maasBordro.taslak') }}
          </span>
        </template>
      </Column>
      <Column
        field="odemeDurumu"
        :header="t('maasBordro.odemeDurumu')"
        style="width: 120px"
      >
        <template #body="{ data }">
          <span
            class="durum-rozet"
            :class="data.odemeDurumu === 'ODENDI' ? 'durum-onayli' : 'durum-taslak'"
          >
            {{ data.odemeDurumu === 'ODENDI' ? t('maasBordro.odendi') : t('maasBordro.odenmedi') }}
          </span>
        </template>
      </Column>
      <Column
        :header="t('maasBordro.islem')"
        style="width: 200px"
      >
        <template #body="{ data }">
          <Button
            v-if="data.durum !== 'ONAYLANDI'"
            icon="pi pi-pencil"
            :aria-label="$t('common.edit')"
            class="p-button-rounded p-button-text"
            @click="dialogAc(data)"
          />
          <Button
            v-if="data.durum !== 'ONAYLANDI'"
            icon="pi pi-check"
            :aria-label="t('maasBordro.onayla')"
            class="p-button-rounded p-button-text p-button-success"
            @click="onayla(data)"
          />
          <Button
            v-if="data.durum === 'ONAYLANDI' && data.odemeDurumu !== 'ODENDI'"
            icon="pi pi-wallet"
            :aria-label="t('maasBordro.ode')"
            class="p-button-rounded p-button-text p-button-success"
            @click="odemeDialogAc(data)"
          />
          <Button
            v-if="data.durum === 'ONAYLANDI'"
            icon="pi pi-lock-open"
            :aria-label="t('maasBordro.onayKaldir')"
            class="p-button-rounded p-button-text p-button-warning"
            @click="onayKaldir(data)"
          />
          <Button
            v-if="data.durum !== 'ONAYLANDI'"
            icon="pi pi-trash"
            :aria-label="$t('common.delete')"
            class="p-button-rounded p-button-text"
            @click="sil(data)"
          />
        </template>
      </Column>
    </DataTable>

    <!-- Bordro ödemesi: kasa seçimi -->
    <Dialog
      v-model:visible="odemeDialog"
      :header="t('maasBordro.ode')"
      modal
      :style="{ width: '440px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('maasBordro.odemeKasa') }}</label>
          <Dropdown
            v-model="odemeKasaId"
            :options="kasaListesi"
            option-label="ad"
            option-value="id"
            :placeholder="t('maasBordro.kasaSec')"
            class="w-full"
          />
        </div>
        <p class="odeme-bilgi">
          {{ t('maasBordro.odemeAciklama', { tutar: formatCurrency(odemeBordro?.netMaas || 0) }) }}
        </p>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          class="p-button-text"
          @click="odemeDialog = false"
        />
        <Button
          :label="t('maasBordro.ode')"
          icon="pi pi-wallet"
          :loading="odemeGonderiliyor"
          :disabled="!odemeKasaId"
          @click="odemeYap"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="dialog"
      :header="dialogHeader"
      modal
      :style="{ width: '520px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('maasBordro.personelZorunlu') }}</label>
          <Dropdown
            v-model="form.personelId"
            :options="personelListesi"
            option-label="displayName"
            option-value="id"
            :placeholder="t('maasBordro.personelSec')"
            class="w-full"
            filter
          />
        </div>
        <div class="field-row">
          <div class="field">
            <label>{{ t('maasBordro.yil') }}</label><InputNumber
              v-model="form.yil"
              class="w-full"
              :min="2000"
              :max="2100"
            />
          </div>
          <div class="field">
            <label>{{ t('maasBordro.ay') }}</label><InputNumber
              v-model="form.ay"
              class="w-full"
              :min="1"
              :max="12"
            />
          </div>
        </div>
        <div class="field">
          <label>{{ t('maasBordro.brutMaasZorunlu') }}</label><InputNumber
            v-model="form.brutMaas"
            mode="currency"
            currency="TRY"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('maasBordro.kesintiler') }}</label><InputNumber
            v-model="form.kesintiler"
            mode="currency"
            currency="TRY"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('maasBordro.netMaasHesaplanan') }}</label>
          <span style="font-weight: 700; font-size: 18px; color: #4ade80">{{
            formatCurrency((form.brutMaas || 0) - (form.kesintiler || 0))
          }}</span>
        </div>
        <div class="field">
          <label>{{ t('maasBordro.odemeTarihi') }}</label><DatePicker
            v-model="form.odemeTarihi"
            date-format="dd.mm.yy"
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
      v-model:visible="ayarDialog"
      :header="t('maasBordro.bordroAyarlari')"
      modal
      :style="{ width: '520px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('maasBordro.yil') }}</label><InputNumber
            v-model="ayarForm.yil"
            :use-grouping="false"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('maasBordro.asgariUcret') }}</label><InputNumber
            v-model="ayarForm.asgariUcret"
            mode="currency"
            currency="TRY"
            locale="tr-TR"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('maasBordro.sgkIsciOrani') }}</label><InputNumber
            v-model="ayarForm.sgkIsciOrani"
            :min="0"
            :max="100"
            suffix=" %"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('maasBordro.issizlikIsciOrani') }}</label><InputNumber
            v-model="ayarForm.issizlikIsciOrani"
            :min="0"
            :max="100"
            suffix=" %"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('maasBordro.sgkIsverenOrani') }}</label><InputNumber
            v-model="ayarForm.sgkIsverenOrani"
            :min="0"
            :max="100"
            suffix=" %"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('maasBordro.issizlikIsverenOrani') }}</label><InputNumber
            v-model="ayarForm.issizlikIsverenOrani"
            :min="0"
            :max="100"
            suffix=" %"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('maasBordro.damgaOrani') }}</label><InputNumber
            v-model="ayarForm.damgaOrani"
            :min="0"
            :max="100"
            :max-fraction-digits="3"
            suffix=" %"
            class="w-full"
          />
        </div>
        <div class="field full-width">
          <label>{{ t('maasBordro.vergiDilimleri') }}</label><Textarea
            v-model="ayarForm.gelirVergisiDilimleri"
            rows="4"
            class="w-full"
            :placeholder="dilimOrnek"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          class="p-button-text"
          @click="ayarDialog = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="ayarKaydet"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="topluDialog"
      :header="t('maasBordro.topluUret')"
      modal
      :style="{ width: '380px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('maasBordro.yil') }}</label><InputNumber
            v-model="topluForm.yil"
            :use-grouping="false"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('maasBordro.ay') }}</label><InputNumber
            v-model="topluForm.ay"
            :min="1"
            :max="12"
            show-buttons
            class="w-full"
          />
        </div>
        <small class="toplu-not">{{ t('maasBordro.topluNot') }}</small>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          class="p-button-text"
          @click="topluDialog = false"
        />
        <Button
          :label="t('maasBordro.uret')"
          icon="pi pi-users"
          :loading="kaydediliyor"
          @click="topluUret"
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
import { maasBordroAPI, personelAPI } from '../api/index.js'
import { formatCurrency } from '../utils/format.js'
import { useI18n } from 'vue-i18n'

const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { t } = useI18n()
const list = ref([])
const personelListesi = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const form = ref({
  personelId: null,
  yil: new Date().getFullYear(),
  ay: new Date().getMonth() + 1,
  brutMaas: 0,
  kesintiler: 0,
  odemeTarihi: new Date()
})

const dialogHeader = computed(() => (duzenleme.value ? t('maasBordro.bordroDuzenle') : t('maasBordro.yeniBordro')))

// Bordro hesaplama ayarları (yıl bazlı asgari ücret, oranlar, vergi dilimleri).
const ayarDialog = ref(false)
const topluDialog = ref(false)
const dilimOrnek = '[{"limit":158000,"oran":15},{"limit":null,"oran":20}]'
const buYil = new Date().getFullYear()
const ayarForm = ref({
  yil: buYil, asgariUcret: 0,
  sgkIsciOrani: 14, issizlikIsciOrani: 1,
  sgkIsverenOrani: 20.5, issizlikIsverenOrani: 2,
  damgaOrani: 0.759, gelirVergisiDilimleri: ''
})
const topluForm = ref({ yil: buYil, ay: new Date().getMonth() + 1 })

const listeyiYenile = async () => {
  const r = await maasBordroAPI.getAll()
  list.value = unwrapList(r)
}

const ayarDialogAc = async () => {
  ayarDialog.value = true
  try {
    const r = await maasBordroAPI.ayar(buYil)
    if (r.data) ayarForm.value = { ...ayarForm.value, ...r.data }
  } catch {
    // Ayarlar yüklenemezse varsayılanlar gösterilir.
  }
}

const ayarKaydet = async () => {
  kaydediliyor.value = true
  try {
    await maasBordroAPI.ayarKaydet(ayarForm.value)
    toastBildirim.basarili(t('maasBordro.ayarKaydedildi'))
    ayarDialog.value = false
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('maasBordro.islemBasarisiz'))
  } finally {
    kaydediliyor.value = false
  }
}

const topluUret = async () => {
  kaydediliyor.value = true
  try {
    const r = await maasBordroAPI.topluUret(topluForm.value.yil, topluForm.value.ay)
    toastBildirim.basarili(t('maasBordro.topluSonuc', {
      uretilen: r.data?.uretilen ?? 0,
      atlanan: r.data?.atlanan ?? 0
    }))
    topluDialog.value = false
    await listeyiYenile()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('maasBordro.islemBasarisiz'))
  } finally {
    kaydediliyor.value = false
  }
}

// Bordro ödemesi (kasa seçimi)
const odemeDialog = ref(false)
const odemeBordro = ref(null)
const odemeKasaId = ref(null)
const odemeGonderiliyor = ref(false)
const kasaListesi = ref([])

const odemeDialogAc = (data) => {
  odemeBordro.value = data
  odemeKasaId.value = null
  odemeDialog.value = true
}

const odemeYap = async () => {
  if (!odemeKasaId.value) return
  odemeGonderiliyor.value = true
  try {
    await maasBordroAPI.ode(odemeBordro.value.id, odemeKasaId.value)
    toastBildirim.basarili(t('maasBordro.odemeBasarili'))
    odemeDialog.value = false
    const r = await maasBordroAPI.getAll()
    list.value = unwrapList(r)
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('maasBordro.islemBasarisiz'))
  } finally {
    odemeGonderiliyor.value = false
  }
}

import { formatTarih as formatDate } from '../utils/format.js'
import { getLocalDateString } from '../utils/format.js'
import { kasaAPI } from '../api/index.js'

onMounted(async () => {
  yukleniyor.value = true
  try {
    const [mR, pR, kR] = await Promise.all([
      maasBordroAPI.getAll(),
      personelAPI.getAll(),
      kasaAPI.getAll({ size: 200 })
    ])
    list.value = unwrapList(mR)
    personelListesi.value = pR.data.map((p) => ({
      ...p,
      displayName: p.ad && p.soyad ? `${p.ad} ${p.soyad}` : p.ad || p.id
    }))
    kasaListesi.value = unwrapList(kR)
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('maasBordro.hataYukleme'))
  }
  yukleniyor.value = false
})

const dialogAc = (data) => {
  duzenleme.value = !!data
  form.value = data
    ? { ...data, odemeTarihi: data.odemeTarihi ? new Date(data.odemeTarihi) : new Date() }
    : {
        personelId: null,
        yil: new Date().getFullYear(),
        ay: new Date().getMonth() + 1,
        brutMaas: 0,
        kesintiler: 0,
        odemeTarihi: new Date()
      }
  dialog.value = true
}

const kaydet = async () => {
  kaydediliyor.value = true
  try {
    const netMaas = (form.value.brutMaas || 0) - (form.value.kesintiler || 0)
    const payload = {
      ...form.value,
      netMaas,
      odemeTarihi: form.value.odemeTarihi ? getLocalDateString(form.value.odemeTarihi) : null
    }
    if (duzenleme.value) {
      await maasBordroAPI.update(form.value.id, payload)
      toastBildirim.basarili(t('maasBordro.guncellendi'))
    } else {
      await maasBordroAPI.create(payload)
      toastBildirim.basarili(t('maasBordro.olusturuldu'))
    }
    dialog.value = false
    const r = await maasBordroAPI.getAll()
    list.value = unwrapList(r)
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('maasBordro.islemBasarisiz'))
  }
  kaydediliyor.value = false
}

const sil = (data) => {
  const personelAd = data.personelAdi || data.id
  confirm.require({
    message: t('maasBordro.silOnayMesaj', { ad: personelAd }),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('common.evetSil'),
    rejectLabel: t('common.vazgec'),
    accept: async () => {
      try {
        await maasBordroAPI.delete(data.id)
        list.value = list.value.filter((x) => x.id !== data.id)
        toast.add({ severity: 'success', summary: t('maasBordro.silindi'), detail: t('maasBordro.bordroSilindi'), life: 3000 })
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('maasBordro.silmeBasarisiz'))
      }
    }
  })
}

const onayla = async (data) => {
  try {
    await maasBordroAPI.onayla(data.id)
    toast.add({ severity: 'success', summary: t('maasBordro.onaylandi'), detail: t('maasBordro.onayBasarili'), life: 3000 })
    const r = await maasBordroAPI.getAll()
    list.value = unwrapList(r)
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('maasBordro.islemBasarisiz'))
  }
}

const onayKaldir = async (data) => {
  try {
    await maasBordroAPI.onayKaldir(data.id)
    toast.add({ severity: 'success', summary: t('maasBordro.onayKaldir'), detail: t('maasBordro.onayKaldirildi'), life: 3000 })
    const r = await maasBordroAPI.getAll()
    list.value = unwrapList(r)
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('maasBordro.islemBasarisiz'))
  }
}
</script>

<style scoped>
.maas-container {
  padding: 0;
}
.durum-rozet {
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 0.8rem;
  font-weight: 600;
}
.durum-onayli {
  background: var(--green-100, #dcfce7);
  color: var(--green-700, #15803d);
}
.durum-taslak {
  background: var(--yellow-100, #fef9c3);
  color: var(--yellow-700, #a16207);
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
.toplu-not {
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.5;
}
</style>
