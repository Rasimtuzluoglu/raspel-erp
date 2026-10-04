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
          <template #empty>
            <EmptyState />
          </template>
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
                v-if="data.durum === 'ONAYLANDI'"
                icon="pi pi-cart-plus"
                class="p-button-rounded p-button-text p-button-info"
                :title="t('satinalma.sipariseCevir')"
                @click="talebiSipariseCevir(data)"
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
          <template #empty>
            <EmptyState />
          </template>
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
          >
            <template #body="{ data }">
              <span class="gizli-veri">{{ data.cariHesapAdi || '-' }}</span>
            </template>
          </Column>
          <Column
            field="genelToplam"
            :header="t('satinalma.toplam')"
          >
            <template #body="{ data }">
              <span class="gizli-veri">{{ formatCurrency(data.genelToplam) }}</span>
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
      :style="{ width: '720px' }"
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
            date-format="dd.mm.yy"
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
            rows="2"
            class="w-full"
          />
        </div>
      </div>

      <div class="kalem-baslik">
        <strong>{{ t('satinalma.kalemler') }}</strong>
        <Button
          :label="t('satinalma.kalemEkle')"
          icon="pi pi-plus"
          size="small"
          class="p-button-outlined"
          @click="kalemEkle(talepForm)"
        />
      </div>
      <DataTable
        :value="talepForm.kalemler"
        size="small"
      >
        <template #empty>
          <small class="bos-kalem">{{ t('satinalma.kalemYok') }}</small>
        </template>
        <Column
          :header="t('satinalma.stok')"
          style="min-width: 180px"
        >
          <template #body="{ data }">
            <Dropdown
              v-model="data.stokId"
              :options="stokSecenekleri"
              option-label="label"
              option-value="value"
              filter
              :placeholder="t('satinalma.stokSecin')"
              class="w-full"
              @filter="stokAra"
              @change="stokSecildi(talepForm, data)"
            />
          </template>
        </Column>
        <Column
          :header="t('satinalma.aciklama')"
          style="min-width: 140px"
        >
          <template #body="{ data }">
            <InputText
              v-model="data.aciklama"
              class="w-full"
            />
          </template>
        </Column>
        <Column
          :header="t('satinalma.miktar')"
          style="width: 100px"
        >
          <template #body="{ data }">
            <InputNumber
              v-model="data.miktar"
              :min="0"
              class="w-full"
            />
          </template>
        </Column>
        <Column
          :header="t('satinalma.birimFiyat')"
          style="width: 130px"
        >
          <template #body="{ data }">
            <InputNumber
              v-model="data.birimFiyat"
              :min="0"
              :min-fraction-digits="2"
              class="w-full"
            />
          </template>
        </Column>
        <Column style="width: 50px">
          <template #body="{ index }">
            <Button
              icon="pi pi-trash"
              class="p-button-rounded p-button-text p-button-danger p-button-sm"
              :title="t('common.delete')"
              :aria-label="t('common.delete')"
              @click="kalemSil(talepForm, index)"
            />
          </template>
        </Column>
      </DataTable>
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
      :style="{ width: '720px' }"
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
            date-format="dd.mm.yy"
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
            rows="2"
            class="w-full"
          />
        </div>
      </div>

      <div class="kalem-baslik">
        <strong>{{ t('satinalma.kalemler') }}</strong>
        <Button
          :label="t('satinalma.kalemEkle')"
          icon="pi pi-plus"
          size="small"
          class="p-button-outlined"
          @click="kalemEkle(siparisForm)"
        />
      </div>
      <DataTable
        :value="siparisForm.kalemler"
        size="small"
      >
        <template #empty>
          <small class="bos-kalem">{{ t('satinalma.kalemYok') }}</small>
        </template>
        <Column
          :header="t('satinalma.stok')"
          style="min-width: 180px"
        >
          <template #body="{ data }">
            <Dropdown
              v-model="data.stokId"
              :options="stokSecenekleri"
              option-label="label"
              option-value="value"
              filter
              :placeholder="t('satinalma.stokSecin')"
              class="w-full"
              @filter="stokAra"
              @change="stokSecildi(siparisForm, data)"
            />
          </template>
        </Column>
        <Column
          :header="t('satinalma.aciklama')"
          style="min-width: 140px"
        >
          <template #body="{ data }">
            <InputText
              v-model="data.aciklama"
              class="w-full"
            />
          </template>
        </Column>
        <Column
          :header="t('satinalma.miktar')"
          style="width: 100px"
        >
          <template #body="{ data }">
            <InputNumber
              v-model="data.miktar"
              :min="0"
              class="w-full"
            />
          </template>
        </Column>
        <Column
          :header="t('satinalma.birimFiyat')"
          style="width: 130px"
        >
          <template #body="{ data }">
            <InputNumber
              v-model="data.birimFiyat"
              :min="0"
              :min-fraction-digits="2"
              class="w-full"
            />
          </template>
        </Column>
        <Column style="width: 50px">
          <template #body="{ index }">
            <Button
              icon="pi pi-trash"
              class="p-button-rounded p-button-text p-button-danger p-button-sm"
              :title="t('common.delete')"
              :aria-label="t('common.delete')"
              @click="kalemSil(siparisForm, index)"
            />
          </template>
        </Column>
      </DataTable>
      <div class="siparis-toplam">
        <span>{{ t('satinalma.toplam') }}</span>
        <strong>{{ formatCurrency(formToplam(siparisForm.kalemler)) }}</strong>
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
import { ref, computed, onMounted } from 'vue'
import { unwrapList } from '../api/utils/unwrap.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { satinalmaTalepAPI, satinalmaSiparisAPI, cariHesapAPI } from '../api/index.js'
import { useStokOnerileri } from '../composables/useStokOnerileri.js'
import { formatCurrency, getLocalDateString } from '../utils/format.js'
import { kalemNetTutar, kalemKdv } from '../utils/faturaHesapla.js'
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
const bosTalepForm = () => ({ talepNo: 'TAL-' + Date.now(), tarih: new Date(), talepEden: '', departman: '', aciklama: '', kalemler: [] })
const bosSiparisForm = () => ({ siparisNo: 'SIP-' + Date.now(), tarih: new Date(), cariHesapId: null, talepId: null, aciklama: '', kalemler: [] })
const talepForm = ref(bosTalepForm())
const siparisForm = ref(bosSiparisForm())

// Talep ve sipariş kalemlerindeki stok dropdown'ları sunucu aramalı. Önceden
// `stokAPI.getAll({ size: 1000 })` ile 1000 kayıt çekiliyordu; 1000. stoktan
// sonrası satın alma kalemine eklenemiyordu.
const { oneriler: stoklar, ara: stokAra, hemenAra: stokOnerileriYukle } = useStokOnerileri()

const stokSecenekleri = computed(() => stoklar.value.map((s) => ({ label: s.ad, value: s.id })))

// İskontosuz KDV-dahil satır toplamı (backend ile uyumlu basit önizleme).
const kalemTutar = (k) => Number(k.miktar || 0) * Number(k.birimFiyat || 0)
const formToplam = (kalemler) => kalemler.reduce((t, k) => t + kalemTutar(k), 0)

const kalemEkle = (form) => {
  form.kalemler.push({ stokId: null, aciklama: '', miktar: 1, birim: 'Adet', birimFiyat: 0 })
}
const kalemSil = (form, idx) => {
  form.kalemler.splice(idx, 1)
}
const stokSecildi = (form, kalem) => {
  const s = stoklar.value.find((x) => x.id === kalem.stokId)
  if (s) {
    kalem.aciklama = s.ad
    kalem.birim = s.birim || 'Adet'
    if (!kalem.birimFiyat) kalem.birimFiyat = s.fiyat || 0
  }
}

onMounted(async () => {
  await Promise.all([talepleriYukle(), siparisleriYukle(), carieleriYukle(), stoklariYukle()])
})

const stoklariYukle = async () => {
  // Dropdown ilk açılışta boş görünmesin diye ilk sayfa bir kez çekilir.
  await stokOnerileriYukle()
}

const talepleriYukle = async () => {
  taleplerYukleniyor.value = true
  try {
    const r = await satinalmaTalepAPI.getAll()
    talepler.value = unwrapList(r)
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('satinalma.hataTalepYukleme'))
  }
  taleplerYukleniyor.value = false
}

const siparisleriYukle = async () => {
  siparislerYukleniyor.value = true
  try {
    const r = await satinalmaSiparisAPI.getAll()
    siparisler.value = unwrapList(r)
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('satinalma.hataSiparisYukleme'))
  }
  siparislerYukleniyor.value = false
}

const carieleriYukle = async () => {
  try {
    const r = await cariHesapAPI.getAll()
    cariler.value = unwrapList(r)
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('satinalma.hataCariYukleme'))
  }
}

const talepDialogAc = () => {
  talepForm.value = bosTalepForm()
  talepDialog.value = true
}

const talepKaydet = async () => {
  kaydediliyor.value = true
  try {
    await satinalmaTalepAPI.create({
      ...talepForm.value,
      tarih: getLocalDateString(talepForm.value.tarih),
      kalemler: talepForm.value.kalemler.filter((k) => k.aciklama && k.miktar > 0)
    })
    talepDialog.value = false
    await talepleriYukle()
    toastBildirim.basarili(t('satinalma.talepKaydedildi'))
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
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('common.evetSil'),
    rejectLabel: t('common.vazgec'),
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
  siparisForm.value = bosSiparisForm()
  siparisDialog.value = true
}

// Onaylı talebi siparişe dönüştür: tedarikçi seçilir, kalemler devralınır.
const talebiSipariseCevir = (talep) => {
  siparisForm.value = {
    siparisNo: 'SIP-' + Date.now(),
    tarih: new Date(),
    cariHesapId: null,
    talepId: talep.id,
    aciklama: t('satinalma.talepAciklama', { no: talep.talepNo }),
    kalemler: (talep.kalemler || []).map((k) => ({
      stokId: k.stokId || null,
      aciklama: k.aciklama || '',
      miktar: k.miktar || 1,
      birim: k.birim || 'Adet',
      birimFiyat: k.tahminiBirimFiyat || 0
    }))
  }
  siparisDialog.value = true
}

const siparisKaydet = async () => {
  kaydediliyor.value = true
  try {
    const kalemler = siparisForm.value.kalemler
      .filter((k) => k.aciklama && k.miktar > 0)
      .map((k) => ({
        ...k,
        // Birim fiyat KDV dahildir; satır KDV oranı gönderilir (varsayılan %20).
        kdvOrani: k.kdvOrani ?? 20,
        tutar: kalemTutar(k)
      }))
    // Toplamlar KDV-dahil kanonik modelle hesaplanır (alış faturası ile birebir uyum).
    const araToplam = kalemler.reduce((t, k) => t + kalemNetTutar(k), 0)
    const kdv = kalemler.reduce((t, k) => t + kalemKdv(k), 0)
    const genelToplam = araToplam + kdv
    await satinalmaSiparisAPI.create({
      ...siparisForm.value,
      tarih: getLocalDateString(siparisForm.value.tarih),
      araToplam: Number(araToplam.toFixed(2)),
      kdv: Number(kdv.toFixed(2)),
      genelToplam: Number(genelToplam.toFixed(2)),
      kalemler
    })
    siparisDialog.value = false
    await siparisleriYukle()
    toastBildirim.basarili(t('satinalma.siparisKaydedildi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('satinalma.hataSiparisKaydet'))
  }
  kaydediliyor.value = false
}

const siparisDurumGuncelle = async (data, durum) => {
  // "Teslim Al" stoğa gerçek giriş yapar; yanlış tık kalıcı stok değiştirmesin diye onay ister.
  if (durum === 'TESLIM_ALINDI' && data.durum !== 'TESLIM_ALINDI') {
    confirm.require({
      message: t('satinalma.teslimAlOnay', { no: data.siparisNo }),
      header: t('satinalma.teslimAl'),
      icon: 'pi pi-box',
      acceptLabel: t('common.evet'),
      rejectLabel: t('common.vazgec'),
      accept: () => siparisDurumUygula(data, durum)
    })
    return
  }
  await siparisDurumUygula(data, durum)
}

const siparisDurumUygula = async (data, durum) => {
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
    rejectLabel: t('common.vazgec'),
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
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('common.evetSil'),
    rejectLabel: t('common.vazgec'),
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
.kalem-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 16px 0 8px;
  color: var(--text-primary);
}
.bos-kalem {
  color: var(--text-muted);
}
.siparis-toplam {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 12px;
  font-size: 15px;
  color: var(--text-secondary);
}
.siparis-toplam strong {
  color: var(--text-primary);
}
</style>
