<template>
  <div class="tf-page">
    <PageHeader
      :title="t('tekrarlayanFaturalar.title')"
      :subtitle="t('tekrarlayanFaturalar.subtitle')"
    >
      <template #actions>
        <Button
          :label="t('tekrarlayanFaturalar.yeniFatura')"
          icon="pi pi-plus"
          class="p-button-success"
          @click="yeniKayit"
        />
      </template>
    </PageHeader>

    <Card>
      <template #content>
        <DataTable
          :value="kayitlar"
          :loading="yukleniyor"
          striped-rows
          size="small"
        >
          <template #empty>
            <EmptyState />
          </template>
          <Column
            field="cariHesapAd"
            :header="t('tekrarlayanFaturalar.cari')"
          >
            <template #body="s">
              {{ s.data.cariHesapAd || '-' }}
            </template>
          </Column>
          <Column
            field="tur"
            :header="t('tekrarlayanFaturalar.tur')"
            style="width: 90px"
          >
            <template #body="s">
              <Tag
                :value="s.data.tur === 'ALIS' ? t('tekrarlayanFaturalar.alis') : t('tekrarlayanFaturalar.satis')"
                :severity="s.data.tur === 'ALIS' ? 'warning' : 'success'"
              />
            </template>
          </Column>
          <Column
            field="periyot"
            :header="t('tekrarlayanFaturalar.periyot')"
            style="width: 100px"
          />
          <Column :header="t('tekrarlayanFaturalar.baslangic')">
            <template #body="s">
              {{ formatDate(s.data.baslangicTarihi) }}
            </template>
          </Column>
          <Column :header="t('tekrarlayanFaturalar.bitis')">
            <template #body="s">
              {{ s.data.bitisTarihi ? formatDate(s.data.bitisTarihi) : '-' }}
            </template>
          </Column>
          <Column :header="t('tekrarlayanFaturalar.sonrakiCalistirma')">
            <template #body="s">
              {{ s.data.sonrakiCalistirma ? formatDate(s.data.sonrakiCalistirma) : '-' }}
            </template>
          </Column>
          <Column
            field="aktif"
            :header="t('common.status')"
            style="width: 90px"
          >
            <template #body="s">
              <Tag
                :value="s.data.aktif ? t('status.active') : t('status.passive')"
                :severity="s.data.aktif ? 'success' : 'secondary'"
              />
            </template>
          </Column>
          <Column
            :header="t('common.actions')"
            style="width: 180px"
          >
            <template #body="s">
              <Button
                icon="pi pi-pencil"
                :aria-label="$t('common.edit')"
                class="p-button-sm p-button-text"
                @click="duzenle(s.data)"
              />
              <Button
                icon="pi pi-file"
                class="p-button-sm p-button-text"
                :title="t('tekrarlayanFaturalar.simdiUret')"
                @click="suretiUret(s.data)"
              />
              <Button
                icon="pi pi-trash"
                :aria-label="$t('common.delete')"
                class="p-button-sm p-button-text p-button-danger"
                @click="sil(s.data)"
              />
            </template>
          </Column>
        </DataTable>
        <div
          v-if="(!kayitlar || !kayitlar.length) && !yukleniyor"
          class="empty-state"
        >
          {{ t('tekrarlayanFaturalar.empty') }}
        </div>
      </template>
    </Card>

    <Dialog
      v-model:visible="dialogAcik"
      :header="duzenlemeId ? t('tekrarlayanFaturalar.duzenle') : t('tekrarlayanFaturalar.yeniFatura')"
      :modal="true"
      style="width: 680px"
      :maximizable="true"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('tekrarlayanFaturalar.cariHesap') }}</label>
          <Select
            v-model="form.cariHesapId"
            :options="cariSecenekleri"
            option-label="ad"
            option-value="id"
            :placeholder="t('tekrarlayanFaturalar.cariSecin')"
            class="w-full"
            show-clear
            filter
          />
        </div>
        <div class="field">
          <label>{{ t('tekrarlayanFaturalar.tur') }}</label>
          <Select
            v-model="form.tur"
            :options="[{ label: t('tekrarlayanFaturalar.satis'), value: 'SATIS' }, { label: t('tekrarlayanFaturalar.alis'), value: 'ALIS' }]"
            option-label="label"
            option-value="value"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('tekrarlayanFaturalar.periyot') }}</label>
          <Select
            v-model="form.periyot"
            :options="periyotlar"
            option-label="label"
            option-value="value"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('tekrarlayanFaturalar.baslangicTarihi') }}</label>
          <DatePicker
            v-model="form.baslangicTarihi"
            date-format="dd/mm/yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('tekrarlayanFaturalar.bitisTarihiOpsiyonel') }}</label>
          <DatePicker
            v-model="form.bitisTarihi"
            date-format="dd/mm/yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('tekrarlayanFaturalar.aciklama') }}</label>
          <InputText
            v-model="form.aciklama"
            class="w-full"
            :placeholder="t('tekrarlayanFaturalar.aciklamaPlaceholder')"
          />
        </div>
        <div class="field">
          <label>{{ t('status.active') }}</label>
          <ToggleSwitch v-model="form.aktif" />
        </div>
      </div>

      <div class="kalem-baslik">
        <span>{{ t('tekrarlayanFaturalar.kalemler') }}</span>
        <Button
          icon="pi pi-plus"
          class="p-button-sm p-button-outlined"
          :label="t('tekrarlayanFaturalar.kalemEkle')"
          @click="kalemEkle"
        />
      </div>
      <div
        v-for="(k, i) in form.kalemler"
        :key="i"
        class="kalem-satir"
      >
        <InputText
          v-model="k.aciklama"
          :placeholder="t('tekrarlayanFaturalar.aciklama')"
          class="kalem-aciklama"
        />
        <InputNumber
          v-model="k.adet"
          :placeholder="t('tekrarlayanFaturalar.adet')"
          class="kalem-adet"
          :min="1"
        />
        <InputNumber
          v-model="k.birimFiyat"
          :placeholder="t('tekrarlayanFaturalar.birimFiyat')"
          class="kalem-fiyat"
          mode="currency"
          currency="TRY"
        />
        <InputNumber
          v-model="k.kdvOrani"
          :placeholder="t('tekrarlayanFaturalar.kdvYuzde')"
          class="kalem-kdv"
          :min="0"
          :max="100"
        />
        <Button
          icon="pi pi-times"
          :aria-label="$t('common.close')"
          class="p-button-sm p-button-text p-button-danger"
          @click="form.kalemler.splice(i, 1)"
        />
      </div>

      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="dialogAcik = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          class="p-button-primary"
          :loading="kaydediliyor"
          @click="kaydet"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { unwrapList } from '../api/utils/unwrap.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { tekrarlayanFaturaAPI, cariHesapAPI } from '../api/index.js'
import PageHeader from '../components/PageHeader.vue'

const { t } = useI18n()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()

const kayitlar = ref([])
const yukleniyor = ref(false)
const dialogAcik = ref(false)
const kaydediliyor = ref(false)
const duzenlemeId = ref(null)

const cariSecenekleri = ref([])

const periyotlar = [
  { label: t('tekrarlayanFaturalar.gunluk'), value: 'GUNLUK' },
  { label: t('tekrarlayanFaturalar.haftalik'), value: 'HAFTALIK' },
  { label: t('tekrarlayanFaturalar.aylik'), value: 'AYLIK' },
  { label: t('tekrarlayanFaturalar.yillik'), value: 'YILLIK' }
]

const bosForm = () => ({
  cariHesapId: null,
  tur: 'SATIS',
  periyot: 'AYLIK',
  baslangicTarihi: new Date(),
  bitisTarihi: null,
  aciklama: '',
  aktif: true,
  kalemler: [{ aciklama: '', adet: 1, birimFiyat: 0, kdvOrani: 20, iskontoOrani: 0, stokId: null }]
})

const form = ref(bosForm())

const yukle = async () => {
  yukleniyor.value = true
  try {
    const r = await tekrarlayanFaturaAPI.getAll()
    kayitlar.value = r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('tekrarlayanFaturalar.hataYukleme'))
  } finally {
    yukleniyor.value = false
  }
}

const carileriYukle = async () => {
  try {
    const r = await cariHesapAPI.getAll()
    kayitlar.value
    cariSecenekleri.value = unwrapList(r)
  } catch {
    cariSecenekleri.value = []
  }
}

const yeniKayit = () => {
  duzenlemeId.value = null
  form.value = bosForm()
  dialogAcik.value = true
}

const duzenle = (k) => {
  duzenlemeId.value = k.id
  form.value = {
    cariHesapId: k.cariHesapId,
    tur: k.tur,
    periyot: k.periyot,
    baslangicTarihi: k.baslangicTarihi ? new Date(k.baslangicTarihi) : new Date(),
    bitisTarihi: k.bitisTarihi ? new Date(k.bitisTarihi) : null,
    aciklama: k.aciklama || '',
    aktif: k.aktif !== false,
    kalemler: (k.kalemler || []).map((c) => ({ ...c }))
  }
  if (!form.value.kalemler.length) form.value.kalemler = bosForm().kalemler
  dialogAcik.value = true
}

const kalemEkle = () => {
  form.value.kalemler.push({ aciklama: '', adet: 1, birimFiyat: 0, kdvOrani: 20, iskontoOrani: 0, stokId: null })
}

const tarihParam = (d) => (d ? (d.toISOString?.().split('T')[0] ?? d) : null)

const kaydet = async () => {
  if (!form.value.cariHesapId) {
    toastBildirim.uyari(t('tekrarlayanFaturalar.cariSecinUyari'))
    return
  }
  if (!form.value.kalemler.length || form.value.kalemler.some((k) => !k.aciklama)) {
    toastBildirim.uyari(t('tekrarlayanFaturalar.kalemlerEksik'))
    return
  }
  kaydediliyor.value = true
  const gonderilecek = {
    cariHesapId: form.value.cariHesapId,
    tur: form.value.tur,
    periyot: form.value.periyot,
    baslangicTarihi: tarihParam(form.value.baslangicTarihi),
    bitisTarihi: tarihParam(form.value.bitisTarihi),
    aciklama: form.value.aciklama,
    aktif: form.value.aktif,
    kalemler: form.value.kalemler
  }
  try {
    if (duzenlemeId.value) {
      await tekrarlayanFaturaAPI.update(duzenlemeId.value, gonderilecek)
      toastBildirim.basarili(t('tekrarlayanFaturalar.guncellendi'))
    } else {
      await tekrarlayanFaturaAPI.create(gonderilecek)
      toastBildirim.basarili(t('tekrarlayanFaturalar.olusturuldu'))
    }
    dialogAcik.value = false
    yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('tekrarlayanFaturalar.kaydedilemedi'))
  } finally {
    kaydediliyor.value = false
  }
}

const sil = (k) => {
  confirm.require({
    message: t('tekrarlayanFaturalar.silOnayMesaj'),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    accept: async () => {
      try {
        await tekrarlayanFaturaAPI.delete(k.id)
        toastBildirim.basarili(t('tekrarlayanFaturalar.silindi'))
        yukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('tekrarlayanFaturalar.silinemedi'))
      }
    }
  })
}

const suretiUret = (k) => {
  confirm.require({
    message: t('tekrarlayanFaturalar.uretOnayMesaj'),
    header: t('tekrarlayanFaturalar.faturaUret'),
    icon: 'pi pi-file',
    accept: async () => {
      try {
        await tekrarlayanFaturaAPI.uret(k.id)
        toastBildirim.basarili(t('tekrarlayanFaturalar.faturaUretildi'))
        yukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('tekrarlayanFaturalar.faturaUretilemedi'))
      }
    }
  })
}

import { formatTarih as formatDate } from '../utils/format.js'

onMounted(() => {
  yukle()
  carileriYukle()
})
</script>

<style scoped>
.tf-page {
  padding: 0;
  max-width: 100%;
}
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.field label {
  display: block;
  margin-bottom: 4px;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--text-secondary);
}
.w-full {
  width: 100% !important;
}
.kalem-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 700;
  margin-bottom: 8px;
}
.kalem-satir {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.kalem-aciklama {
  flex: 1;
  min-width: 140px;
}
.kalem-adet {
  width: 80px;
}
.kalem-fiyat {
  width: 130px;
}
.kalem-kdv {
  width: 90px;
}
.empty-state {
  text-align: center;
  padding: 2rem;
  color: var(--text-muted);
}
</style>
