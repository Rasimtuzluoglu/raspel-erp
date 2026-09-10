<template>
  <div class="uretim-sayfasi">
    <div class="sayfa-baslik">
      <h1><i class="pi pi-cog" /> {{ t('uretim.title') }}</h1>
    </div>

    <!-- Reçeteler -->
    <div class="bolum">
      <div class="bolum-baslik">
        <h2>{{ t('uretim.receteler') }}</h2>
        <Button
          :label="t('uretim.yeniRecete')"
          icon="pi pi-plus"
          class="p-button-sm"
          @click="receteDialogAc(null)"
        />
      </div>
      <div
        v-if="!receteler.length"
        class="bos"
      >
        {{ t('uretim.receteYok') }}
      </div>
      <div
        v-for="r in receteler"
        :key="r.id"
        class="satir"
      >
        <div class="satir-bilgi">
          <strong>{{ r.ad }}</strong>
          <span class="muted">{{ t('uretim.mamul') }}: {{ r.urunAd || '#' + r.urunId }}</span>
          <span class="muted">{{ t('uretim.kalem') }}: {{ (r.kalemler || []).length }}</span>
        </div>
        <Button
          icon="pi pi-trash"
          class="p-button-rounded p-button-text p-button-danger p-button-sm"
          @click="receteSil(r)"
        />
      </div>
    </div>

    <!-- Üretim Emirleri -->
    <div class="bolum">
      <div class="bolum-baslik">
        <h2>{{ t('uretim.uretimEmirleri') }}</h2>
        <Button
          :label="t('uretim.yeniEmir')"
          icon="pi pi-plus"
          class="p-button-sm"
          @click="emirDialog = true"
        />
      </div>
      <div
        v-if="!emirler.length"
        class="bos"
      >
        {{ t('uretim.emirYok') }}
      </div>
      <div
        v-for="e in emirler"
        :key="e.id"
        class="satir"
      >
        <div class="satir-bilgi">
          <strong>{{ e.urunAd || '#' + e.urunId }}</strong>
          <span class="muted">{{ t('uretim.miktar') }}: {{ e.miktar }}</span>
          <Tag
            :value="durumAdi(e.durum)"
            :severity="durumSeverity(e.durum)"
          />
        </div>
        <Button
          v-if="e.durum !== 'TAMAMLANDI' && e.durum !== 'IPTAL'"
          :label="t('uretim.tamamla')"
          icon="pi pi-check"
          class="p-button-sm p-button-success"
          @click="emirTamamla(e)"
        />
      </div>
    </div>

    <!-- Reçete Dialog -->
    <Dialog
      v-model:visible="receteDialog"
      :header="t('uretim.recete')"
      :modal="true"
      :style="{ width: '520px' }"
    >
      <div class="form">
        <div class="field">
          <label>{{ t('uretim.receteAdiZorunlu') }}</label>
          <InputText
            v-model="receteForm.ad"
            class="w-full"
            :placeholder="t('uretim.receteAdiPlaceholder')"
          />
        </div>
        <div class="field">
          <label>{{ t('uretim.mamulZorunlu') }}</label>
          <Dropdown
            v-model="receteForm.urunId"
            :options="stoklar"
            option-label="ad"
            option-value="id"
            filter
            class="w-full"
            :placeholder="t('uretim.mamulSecin')"
          />
        </div>
        <div class="field">
          <label>{{ t('uretim.hammaddeler') }}</label>
          <div
            v-for="(k, i) in receteForm.kalemler"
            :key="i"
            class="kalem-satir"
          >
            <Dropdown
              v-model="k.hammaddeId"
              :options="stoklar"
              option-label="ad"
              option-value="id"
              filter
              class="w-full"
              :placeholder="t('uretim.hammadde')"
            />
            <InputNumber
              v-model="k.miktar"
              :min="0"
              class="kalem-miktar"
              :placeholder="t('uretim.miktar')"
            />
            <Button
              icon="pi pi-trash"
              class="p-button-text p-button-danger p-button-sm"
              @click="receteForm.kalemler.splice(i, 1)"
            />
          </div>
          <Button
            :label="t('uretim.kalemEkle')"
            icon="pi pi-plus"
            class="p-button-sm p-button-text"
            @click="receteForm.kalemler.push({ hammaddeId: null, miktar: 1 })"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          class="p-button-text"
          @click="receteDialog = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="receteKaydet"
        />
      </template>
    </Dialog>

    <!-- Emir Dialog -->
    <Dialog
      v-model:visible="emirDialog"
      :header="t('uretim.yeniUretimEmri')"
      :modal="true"
      :style="{ width: '420px' }"
    >
      <div class="form">
        <div class="field">
          <label>{{ t('uretim.urunZorunlu') }}</label>
          <Dropdown
            v-model="emirForm.urunId"
            :options="stoklar"
            option-label="ad"
            option-value="id"
            filter
            class="w-full"
            :placeholder="t('uretim.urunSecin')"
          />
        </div>
        <div class="field">
          <label>{{ t('uretim.miktarZorunlu') }}</label>
          <InputNumber
            v-model="emirForm.miktar"
            :min="0"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          class="p-button-text"
          @click="emirDialog = false"
        />
        <Button
          :label="t('uretim.olustur')"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="emirKaydet"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { uretimAPI, stokAPI } from '../api/index.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useI18n } from 'vue-i18n'

const toastBildirim = useToastBildirim()
const { t } = useI18n()

const receteler = ref([])
const emirler = ref([])
const stoklar = ref([])
const receteDialog = ref(false)
const emirDialog = ref(false)
const kaydediliyor = ref(false)

const receteForm = ref({ ad: '', urunId: null, kalemler: [] })
const emirForm = ref({ urunId: null, miktar: null })

const durumAdi = (d) => ({ TASLAK: t('faturalar.durumTaslak'), URETIMDE: t('uretim.uretimde'), TAMAMLANDI: t('uretim.tamamlandi'), IPTAL: t('faturalar.durumIptal') })[d] || d
const durumSeverity = (d) => ({ TASLAK: 'secondary', URETIMDE: 'info', TAMAMLANDI: 'success', IPTAL: 'danger' })[d] || 'secondary'

const yukle = async () => {
  try {
    const [r, e, s] = await Promise.all([uretimAPI.receteler(), uretimAPI.emirler(), stokAPI.getAll({ size: 500 })])
    receteler.value = r.data || []
    emirler.value = e.data || []
    stoklar.value = s.data?.content || s.data || []
  } catch (err) {
    toastBildirim.hata(t('uretim.hataYukleme'))
  }
}

const receteDialogAc = () => {
  receteForm.value = { ad: '', urunId: null, kalemler: [] }
  receteDialog.value = true
}

const receteKaydet = async () => {
  if (!receteForm.value.ad?.trim() || !receteForm.value.urunId) {
    toastBildirim.uyari(t('uretim.receteZorunlu'))
    return
  }
  kaydediliyor.value = true
  try {
    await uretimAPI.receteOlustur({
      ad: receteForm.value.ad,
      urunId: receteForm.value.urunId,
      kalemler: receteForm.value.kalemler.filter((k) => k.hammaddeId && k.miktar > 0)
    })
    toastBildirim.basarili(t('uretim.receteKaydedildi'))
    receteDialog.value = false
    yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('uretim.receteKaydedilemedi'))
  } finally {
    kaydediliyor.value = false
  }
}

const receteSil = async (r) => {
  try {
    await uretimAPI.receteSil(r.id)
    toastBildirim.basarili(t('uretim.receteSilindi'))
    yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('uretim.silinemedi'))
  }
}

const emirKaydet = async () => {
  if (!emirForm.value.urunId || !emirForm.value.miktar) {
    toastBildirim.uyari(t('uretim.urunMiktarZorunlu'))
    return
  }
  kaydediliyor.value = true
  try {
    await uretimAPI.emirOlustur(emirForm.value)
    toastBildirim.basarili(t('uretim.emirOlusturuldu'))
    emirDialog.value = false
    emirForm.value = { urunId: null, miktar: null }
    yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('uretim.emirOlusturulamadi'))
  } finally {
    kaydediliyor.value = false
  }
}

const emirTamamla = async (e) => {
  try {
    await uretimAPI.emirTamamla(e.id)
    toastBildirim.basarili(t('uretim.uretimTamamlandi'))
    yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('uretim.uretimTamamlanamadi'))
  }
}

onMounted(yukle)
</script>

<style scoped>
.uretim-sayfasi {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.sayfa-baslik h1 {
  margin: 0;
  font-size: 20px;
  display: flex;
  align-items: center;
}
.bolum {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px;
}
.bolum-baslik {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.bolum-baslik h2 {
  margin: 0;
  font-size: 15px;
}
.satir {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid var(--border);
}
.satir:last-child {
  border-bottom: none;
}
.satir-bilgi {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.muted {
  font-size: 12px;
  color: var(--text-muted);
}
.bos {
  text-align: center;
  color: var(--text-muted);
  padding: 24px;
}
.form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
}
.w-full {
  width: 100%;
}
.kalem-satir {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
.kalem-miktar {
  width: 120px;
}
</style>
