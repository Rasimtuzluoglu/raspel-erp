<template>
  <Dialog
    v-model:visible="emailDialog"
    :modal="true"
    :draggable="false"
    :style="{ width: '520px', maxWidth: '96vw' }"
  >
    <template #header>
      <div class="dialog-baslik">
        <i class="pi pi-envelope" />
        <span>{{ t('cariHesaplar.topluEposta') }}</span>
      </div>
    </template>
    <p class="toplu-email-aciklama">
      {{ t('cariHesaplar.topluEpostaAciklama', { n: seciliCariHesaplar.length }) }}
    </p>
    <div class="form-group">
      <label>{{ t('cariHesaplar.konu') }}</label>
      <InputText
        v-model="emailKonu"
        class="w-full"
        :placeholder="t('cariHesaplar.epostaKonusuPlaceholder')"
      />
    </div>
    <div class="form-group">
      <label>{{ t('cariHesaplar.mesaj') }}</label>
      <Textarea
        v-model="emailMesaj"
        rows="5"
        class="w-full"
        :placeholder="t('cariHesaplar.epostaIcerigiPlaceholder')"
      />
    </div>
    <template #footer>
      <Button
        :label="t('common.cancel')"
        icon="pi pi-times"
        class="p-button-text"
        @click="emailDialog = false"
      />
      <Button
        :label="t('cariHesaplar.epostaIstemisiniAc')"
        icon="pi pi-envelope"
        @click="emailGonder"
      />
    </template>
  </Dialog>

  <Dialog
    v-model:visible="acilisDialog"
    modal
    :draggable="false"
    :style="{ width: '440px', maxWidth: '96vw' }"
  >
    <template #header>
      <div class="dialog-baslik">
        <i class="pi pi-flag" />
        <span>{{ t('cariHesaplar.acilisFisi') }}</span>
      </div>
    </template>
    <p class="toplu-secim-bilgi">
      {{ acilisCari?.ad }}
    </p>
    <div class="form-alani">
      <label>{{ t('cariHesaplar.acilisTutar') }}</label>
      <InputNumber
        v-model="acilisForm.tutar"
        :min-fraction-digits="2"
        :max-fraction-digits="2"
        class="w-full"
        :placeholder="t('cariHesaplar.acilisTutarYardim')"
      />
    </div>
    <div class="form-alani">
      <label>{{ t('cariHesaplar.acilisTarih') }}</label>
      <DatePicker
        v-model="acilisForm.tarih"
        date-format="dd.mm.yy"
        show-icon
        class="w-full"
      />
    </div>
    <div class="form-alani">
      <label>{{ t('cariHesaplar.acilisAciklama') }}</label>
      <InputText
        v-model="acilisForm.aciklama"
        class="w-full"
      />
    </div>
    <small>{{ t('cariHesaplar.acilisYardim') }}</small>
    <template #footer>
      <Button
        :label="t('common.cancel')"
        class="p-button-text"
        @click="acilisDialog = false"
      />
      <Button
        :label="t('common.save')"
        icon="pi pi-check"
        :loading="acilisKaydediliyor"
        @click="acilisKaydet"
      />
    </template>
  </Dialog>

  <Dialog
    v-model:visible="adresDialog"
    modal
    :draggable="false"
    :style="{ width: '760px', maxWidth: '96vw' }"
  >
    <template #header>
      <div class="dialog-baslik">
        <i class="pi pi-map-marker" />
        <span>{{ t('cariHesaplar.adreslerBaslik') }}</span>
      </div>
    </template>
    <p class="toplu-secim-bilgi">
      {{ adresCari?.ad }}
    </p>
    <DataTable
      :value="adresler"
      data-key="id"
      striped-rows
      responsive-layout="scroll"
      :paginator="adresler.length > 5"
      :rows="5"
    >
      <Column
        field="baslik"
        :header="t('cariHesaplar.adresBaslik')"
      />
      <Column
        field="adres"
        :header="t('cariHesaplar.adres')"
      />
      <Column
        field="il"
        :header="t('cariHesaplar.il')"
      />
      <Column :header="t('cariHesaplar.adresVarsayilan')">
        <template #body="{ data }">
          <i
            v-if="data.varsayilan"
            class="pi pi-check"
          />
        </template>
      </Column>
      <Column
        :header="t('common.actions')"
        style="width: 80px"
      >
        <template #body="{ data }">
          <Button
            :aria-label="t('common.delete')"
            :title="t('common.delete')"
            icon="pi pi-trash"
            class="p-button-rounded p-button-danger p-button-sm"
            @click="adresSil(data)"
          />
        </template>
      </Column>
      <template #empty>
        <p>{{ t('cariHesaplar.adresYok') }}</p>
      </template>
    </DataTable>

    <Divider />

    <div class="adres-form">
      <InputText
        v-model="adresForm.baslik"
        :placeholder="t('cariHesaplar.adresBaslik')"
      />
      <InputText
        v-model="adresForm.adres"
        :placeholder="t('cariHesaplar.adres')"
        class="genis"
      />
      <InputText
        v-model="adresForm.il"
        :placeholder="t('cariHesaplar.il')"
      />
      <InputText
        v-model="adresForm.ilce"
        :placeholder="t('cariHesaplar.ilce')"
      />
      <InputText
        v-model="adresForm.yetkiliKisi"
        :placeholder="t('cariHesaplar.yetkili')"
      />
      <InputText
        v-model="adresForm.telefon"
        :placeholder="t('cariHesaplar.telefon')"
      />
      <div class="varsayilan-alan">
        <Checkbox
          v-model="adresForm.varsayilan"
          :binary="true"
          input-id="adresVarsayilan"
        />
        <label for="adresVarsayilan">{{ t('cariHesaplar.adresVarsayilan') }}</label>
      </div>
      <Button
        :label="t('common.add')"
        icon="pi pi-plus"
        :loading="adresKaydediliyor"
        @click="adresEkleKaydet"
      />
    </div>
  </Dialog>

  <Dialog
    v-model:visible="topluDialog"
    modal
    :draggable="false"
    :style="{ width: '460px', maxWidth: '96vw' }"
  >
    <template #header>
      <div class="dialog-baslik">
        <i class="pi pi-pencil" />
        <span>{{ t('cariHesaplar.topluGuncelleBaslik') }}</span>
      </div>
    </template>
    <p class="toplu-secim-bilgi">
      {{ t('cariHesaplar.topluSecilen', { sayi: seciliCariHesaplar.length }) }}
    </p>
    <div class="form-alani">
      <label>{{ t('cariHesaplar.tur') }}</label>
      <Dropdown
        v-model="topluForm.tur"
        :options="cariTurSecenekleri"
        option-label="label"
        option-value="value"
        show-clear
        :placeholder="t('cariHesaplar.degisiklikYok')"
        class="w-full"
      />
    </div>
    <div class="form-alani">
      <label>{{ t('cariHesaplar.krediLimiti') }}</label>
      <InputNumber
        v-model="topluForm.krediLimiti"
        :min="0"
        :min-fraction-digits="2"
        :max-fraction-digits="2"
        :placeholder="t('cariHesaplar.degisiklikYok')"
        class="w-full"
      />
    </div>
    <div class="form-alani">
      <label>{{ t('cariHesaplar.vadeGun') }}</label>
      <InputNumber
        v-model="topluForm.odemeVadesi"
        :min="0"
        :placeholder="t('cariHesaplar.degisiklikYok')"
        class="w-full"
      />
    </div>
    <div class="form-alani">
      <label>{{ t('cariHesaplar.durum') }}</label>
      <Dropdown
        v-model="topluForm.aktif"
        :options="aktifSecenekleri"
        option-label="label"
        option-value="value"
        show-clear
        :placeholder="t('cariHesaplar.degisiklikYok')"
        class="w-full"
      />
    </div>
    <template #footer>
      <Button
        :label="t('common.cancel')"
        class="p-button-text"
        @click="topluDialog = false"
      />
      <Button
        :label="t('common.save')"
        icon="pi pi-check"
        :loading="topluKaydediliyor"
        @click="topluGuncelleKaydet"
      />
    </template>
  </Dialog>

  <Dialog
    v-model:visible="riskDialog"
    modal
    :draggable="false"
    :style="{ width: '760px', maxWidth: '96vw' }"
  >
    <template #header>
      <div class="dialog-baslik">
        <i class="pi pi-exclamation-triangle" />
        <span>{{ t('cariHesaplar.riskliCarilerBaslik') }}</span>
      </div>
    </template>
    <div
      v-if="riskYukleniyor"
      class="loading-iskelet"
    >
      <SkeletonLoader
        :count="4"
        height="40px"
      />
    </div>
    <DataTable
      v-else
      :value="riskliCariler"
      data-key="cariId"
      striped-rows
      responsive-layout="scroll"
      :paginator="riskliCariler.length > 10"
      :rows="10"
    >
      <Column
        field="cariAd"
        :header="t('cariHesaplar.ad')"
      />
      <Column :header="t('cariHesaplar.borc')">
        <template #body="{ data }">
          {{ formatCurrency(data.borc) }}
        </template>
      </Column>
      <Column :header="t('cariHesaplar.krediLimiti')">
        <template #body="{ data }">
          {{ formatCurrency(data.krediLimiti) }}
        </template>
      </Column>
      <Column :header="t('cariHesaplar.asimTutari')">
        <template #body="{ data }">
          <span class="negative">{{ formatCurrency(data.asimTutari) }}</span>
        </template>
      </Column>
      <Column :header="t('cariHesaplar.riskOrani')">
        <template #body="{ data }">
          %{{ data.riskOrani }}
        </template>
      </Column>
      <template #empty>
        <p>{{ t('cariHesaplar.riskliYok') }}</p>
      </template>
    </DataTable>
  </Dialog>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { cariHesapAPI, hareketAPI } from '../../api/index.js'
import { unwrapList } from '../../api/utils/unwrap.js'
import { useToastBildirim } from '../../composables/useToastBildirim.js'
import { formatCurrency } from '../../utils/format.js'

const props = defineProps({
  seciliCariHesaplar: { type: Array, default: () => [] },
  cariTurSecenekleri: { type: Array, default: () => [] },
  aktifSecenekleri: { type: Array, default: () => [] }
})

const emit = defineEmits(['yenilendi', 'secili-temizle'])
const { t } = useI18n()
const toastBildirim = useToastBildirim()

const emailDialog = ref(false)
const emailKonu = ref('')
const emailMesaj = ref('')

const acilisDialog = ref(false)
const acilisCari = ref(null)
const acilisKaydediliyor = ref(false)
const acilisForm = ref({ tutar: null, tarih: null, aciklama: '' })

const adresDialog = ref(false)
const adresCari = ref(null)
const adresler = ref([])
const adresKaydediliyor = ref(false)
const adresForm = ref({ baslik: '', adres: '', il: '', ilce: '', yetkiliKisi: '', telefon: '', varsayilan: false })

const topluDialog = ref(false)
const topluKaydediliyor = ref(false)
const topluForm = ref({ tur: null, krediLimiti: null, odemeVadesi: null, aktif: null })

const riskDialog = ref(false)
const riskliCariler = ref([])
const riskYukleniyor = ref(false)

const tarihIso = (d) => {
  const tarih = new Date(d)
  if (Number.isNaN(tarih.getTime())) return null
  const yil = tarih.getFullYear()
  const ay = String(tarih.getMonth() + 1).padStart(2, '0')
  const gun = String(tarih.getDate()).padStart(2, '0')
  return `${yil}-${ay}-${gun}`
}

const bosAdresForm = () => ({ baslik: '', adres: '', il: '', ilce: '', yetkiliKisi: '', telefon: '', varsayilan: false })

const emailAc = () => {
  emailDialog.value = true
}

const emailGonder = () => {
  const alicilar = (props.seciliCariHesaplar || [])
    .filter((c) => c.email)
    .map((c) => c.email)
  if (alicilar.length === 0) {
    toastBildirim.uyari(t('cariHesaplar.epostaAdresiYok'))
    return
  }
  const mailto = `mailto:?bcc=${alicilar.join(',')}&subject=${encodeURIComponent(emailKonu.value || '')}&body=${encodeURIComponent(emailMesaj.value || '')}`
  window.open(mailto, '_blank')
  emailDialog.value = false
  toastBildirim.basarili(t('cariHesaplar.epostaIstemiAcildi', { n: alicilar.length }))
}

const acilisAc = (cari) => {
  acilisCari.value = cari
  acilisForm.value = { tutar: null, tarih: new Date(), aciklama: '' }
  acilisDialog.value = true
}

const acilisKaydet = async () => {
  if (acilisKaydediliyor.value) return
  if (!acilisForm.value.tutar) {
    toastBildirim.uyari(t('cariHesaplar.acilisTutarZorunlu'))
    return
  }
  acilisKaydediliyor.value = true
  try {
    await hareketAPI.acilis({
      cariHesapId: acilisCari.value.id,
      tutar: acilisForm.value.tutar,
      tarih: acilisForm.value.tarih ? tarihIso(acilisForm.value.tarih) : null,
      aciklama: acilisForm.value.aciklama
    })
    toastBildirim.basarili(t('cariHesaplar.acilisKaydedildi'))
    acilisDialog.value = false
    emit('yenilendi')
  } catch (e) {
    toastBildirim.hata(e?.response?.data?.message || t('common.error'))
  } finally {
    acilisKaydediliyor.value = false
  }
}

const adresleriYukle = async () => {
  if (!adresCari.value) return
  try {
    const r = await cariHesapAPI.adresler(adresCari.value.id)
    adresler.value = unwrapList(r.data) || []
  } catch (e) {
    toastBildirim.hata(e?.response?.data?.message || t('common.error'))
  }
}

const adresAc = async (cari) => {
  adresCari.value = cari
  adresForm.value = bosAdresForm()
  adresDialog.value = true
  await adresleriYukle()
}

const adresEkleKaydet = async () => {
  if (adresKaydediliyor.value) return
  if (!adresForm.value.adres || !adresForm.value.adres.trim()) {
    toastBildirim.uyari(t('cariHesaplar.adresZorunlu'))
    return
  }
  adresKaydediliyor.value = true
  try {
    await cariHesapAPI.adresEkle(adresCari.value.id, adresForm.value)
    toastBildirim.basarili(t('cariHesaplar.adresEklendi'))
    adresForm.value = bosAdresForm()
    await adresleriYukle()
  } catch (e) {
    toastBildirim.hata(e?.response?.data?.message || t('common.error'))
  } finally {
    adresKaydediliyor.value = false
  }
}

const adresSil = async (adres) => {
  try {
    await cariHesapAPI.adresSil(adresCari.value.id, adres.id)
    toastBildirim.basarili(t('cariHesaplar.adresSilindi'))
    await adresleriYukle()
  } catch (e) {
    toastBildirim.hata(e?.response?.data?.message || t('common.error'))
  }
}

const topluAc = () => {
  topluForm.value = { tur: null, krediLimiti: null, odemeVadesi: null, aktif: null }
  topluDialog.value = true
}

const topluGuncelleKaydet = async () => {
  const payload = { idler: props.seciliCariHesaplar.map((c) => c.id) }
  const form = topluForm.value
  if (form.tur != null) payload.tur = form.tur
  if (form.krediLimiti != null) payload.krediLimiti = form.krediLimiti
  if (form.odemeVadesi != null) payload.odemeVadesi = form.odemeVadesi
  if (form.aktif != null) payload.aktif = form.aktif
  if (Object.keys(payload).length <= 1) {
    toastBildirim.uyari(t('cariHesaplar.topluAlanSec'))
    return
  }
  topluKaydediliyor.value = true
  try {
    await cariHesapAPI.topluGuncelle(payload)
    toastBildirim.basarili(t('cariHesaplar.topluGuncellendi'))
    topluDialog.value = false
    emit('secili-temizle')
    emit('yenilendi')
  } catch (e) {
    toastBildirim.hata(e?.response?.data?.message || t('common.error'))
  } finally {
    topluKaydediliyor.value = false
  }
}

const riskliAc = async () => {
  riskDialog.value = true
  if (riskliCariler.value.length) return
  riskYukleniyor.value = true
  try {
    const r = await cariHesapAPI.riskliCariler()
    riskliCariler.value = unwrapList(r.data) || []
  } catch (e) {
    toastBildirim.hata(e?.response?.data?.message || t('common.error'))
  } finally {
    riskYukleniyor.value = false
  }
}

defineExpose({ emailAc, acilisAc, adresAc, topluAc, riskliAc })
</script>

<style scoped>
.dialog-baslik {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  font-size: 15px;
}
.dialog-baslik i {
  color: var(--accent);
}
.toplu-email-aciklama {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 14px;
}
.toplu-secim-bilgi {
  margin: 0 0 14px;
  padding: 8px 12px;
  border-radius: 10px;
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}
.form-alani {
  display: flex;
  flex-direction: column;
  gap: 5px;
  margin-bottom: 14px;
}
.form-alani label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.4px;
}
.adres-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  align-items: center;
}
.adres-form .genis {
  grid-column: 1 / -1;
}
.varsayilan-alan {
  display: flex;
  align-items: center;
  gap: 8px;
}
.negative {
  color: var(--danger);
  font-weight: bold;
}
.form-group {
  margin-bottom: 14px;
}
.form-group:last-child {
  margin-bottom: 0;
}
.form-group label {
  display: block;
  margin-bottom: 6px;
  font-weight: 700;
  color: var(--text-primary) !important;
  font-size: 13px;
}
@media (max-width: 640px) {
  .adres-form {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
