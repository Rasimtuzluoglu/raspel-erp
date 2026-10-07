<template>
  <div>
    <div class="rapor-filtre">
      <div class="form-group">
        <label>{{ t('raporlar.projeksiyonSuresi') }}</label>
        <Dropdown
          v-model="nakitGun"
          :options="[
            { label: t('raporlar.projeksiyonGun', { n: 30 }), value: 30 },
            { label: t('raporlar.projeksiyonGun', { n: 60 }), value: 60 },
            { label: t('raporlar.projeksiyonGun', { n: 90 }), value: 90 }
          ]"
          option-label="label"
          option-value="value"
          class="w-full"
          @change="getNakitAkisi"
        />
      </div>
      <div class="form-group filtre-btn">
        <label>&nbsp;</label>
        <Button
          :label="t('raporlar.yenile')"
          icon="pi pi-refresh"
          :loading="nakitLoading"
          @click="getNakitAkisi"
        />
      </div>
    </div>

    <div
      v-if="nakitData"
      class="rapor-sonuc"
    >
      <div class="ozet-kartlar">
        <div class="ozet-kart">
          <span>{{ t('raporlar.mevcutLikidite') }}</span>
          <strong>{{ formatCurrency(nakitData.baslangicBakiyesi) }}</strong>
        </div>
        <div class="ozet-kart gelir">
          <span>{{ t('raporlar.beklenenTahsilatlar') }}</span>
          <strong class="positive">+{{ formatCurrency(nakitData.toplamBeklenenGiris) }}</strong>
        </div>
        <div class="ozet-kart gider">
          <span>{{ t('raporlar.beklenenOdemeler') }}</span>
          <strong class="negative">-{{ formatCurrency(nakitData.toplamBeklenenCikis) }}</strong>
        </div>
        <div
          class="ozet-kart"
          :class="nakitData.tahminiBitisBakiyesi >= 0 ? 'kar' : 'zarar'"
        >
          <span>{{ t('raporlar.gunSonrakiTahminiKasa', { n: nakitGun }) }}</span>
          <strong>{{ formatCurrency(nakitData.tahminiBitisBakiyesi) }}</strong>
        </div>
      </div>

      <h3 style="margin-top: 25px">
        {{ t('raporlar.gunlukNakitAkisiDetayi') }}
      </h3>
      <DataTable
        :value="nakitData.gunlukAkis"
        striped-rows
        size="small"
        :rows="15"
        :paginator="true"
        paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport"
      >
        <template #empty>
          <EmptyState />
        </template>
        <Column
          field="tarih"
          :header="t('common.date')"
          style="width: 120px"
        >
          <template #body="s">
            {{ formatDate(s.data.tarih) }}
          </template>
        </Column>
        <Column
          field="beklenenGiris"
          :header="t('raporlar.girisTahsilat')"
          style="width: 140px"
        >
          <template #body="s">
            <span :class="s.data.beklenenGiris > 0 ? 'positive' : ''">{{ formatCurrency(s.data.beklenenGiris) }}</span>
          </template>
        </Column>
        <Column
          field="beklenenCikis"
          :header="t('raporlar.cikisOdeme')"
          style="width: 140px"
        >
          <template #body="s">
            <span :class="s.data.beklenenCikis > 0 ? 'negative' : ''">{{ formatCurrency(s.data.beklenenCikis) }}</span>
          </template>
        </Column>
        <Column
          field="netAkis"
          :header="t('raporlar.netGunlukAkis')"
          style="width: 140px"
        >
          <template #body="s">
            <span :class="s.data.netAkis >= 0 ? 'positive' : 'negative'">{{ formatCurrency(s.data.netAkis) }}</span>
          </template>
        </Column>
        <Column
          field="kumulatifBakiye"
          :header="t('raporlar.tahminiKasaBakiyesi')"
          style="width: 170px"
        >
          <template #body="s">
            <strong :class="s.data.kumulatifBakiye >= 0 ? 'positive' : 'negative'">{{ formatCurrency(s.data.kumulatifBakiye) }}</strong>
          </template>
        </Column>
        <Column
          field="aciklama"
          :header="t('common.description')"
        />
      </DataTable>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { raporAPI } from '../api/index.js'
import { formatCurrency, formatTarih as formatDate } from '../utils/format.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'

const { t } = useI18n()
const toastBildirim = useToastBildirim()

const nakitGun = ref(30)
const nakitData = ref(null)
const nakitLoading = ref(false)

const getNakitAkisi = async () => {
  nakitLoading.value = true
  try {
    const r = await raporAPI.nakitAkisiProjeksiyonu(nakitGun.value)
    nakitData.value = r.data
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('raporlar.nakitAkisiHata'))
  }
  nakitLoading.value = false
}

onMounted(getNakitAkisi)
defineExpose({ getNakitAkisi })
</script>
