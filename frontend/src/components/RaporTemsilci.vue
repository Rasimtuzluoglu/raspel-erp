<template>
  <div>
    <div class="rapor-filtre">
      <div class="form-group">
        <label>{{ t('raporlar.baslangic') }}</label>
        <DatePicker
          v-model="tpBas"
          date-format="dd.mm.yy"
          show-icon
        />
      </div>
      <div class="form-group">
        <label>{{ t('raporlar.bitis') }}</label>
        <DatePicker
          v-model="tpBit"
          date-format="dd.mm.yy"
          show-icon
        />
      </div>
      <Button
        :label="t('raporlar.raporGetir')"
        icon="pi pi-search"
        :loading="tpLoading"
        @click="getTemsilciPerformans"
      />
    </div>
    <div
      v-if="tpData"
      class="rapor-sonuc"
    >
      <div class="yas-kova-ozet">
        <div class="yas-kova-kart toplam">
          <span class="yas-kova-ad">{{ t('raporlar.toplamSatis') }}</span>
          <span class="yas-kova-tutar">{{ formatCurrency(tpData.toplamSatis ?? 0) }}</span>
        </div>
        <div class="yas-kova-kart">
          <span class="yas-kova-ad">{{ t('raporlar.toplamFatura') }}</span>
          <span class="yas-kova-tutar">{{ tpData.toplamFatura ?? 0 }}</span>
        </div>
        <div class="yas-kova-kart">
          <span class="yas-kova-ad">{{ t('raporlar.temsilciAd') }}</span>
          <span class="yas-kova-tutar">{{ tpData.satirlar?.length ?? 0 }}</span>
        </div>
      </div>
      <DataTable
        :value="tpData.satirlar || []"
        striped-rows
        :rows="10"
        :paginator="true"
        paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport"
      >
        <template #empty>
          <EmptyState />
        </template>
        <Column
          field="temsilciAd"
          :header="t('raporlar.temsilciAd')"
        >
          <template #body="s">
            <!-- Backend temsilci atanmamis satırda `temsilciAd` null doner;
                 etiket burada yerellestirilir. -->
            <span
              v-if="s.data.atanmamisMi || !s.data.temsilciAd"
              class="vade-badge risk-yok"
            >
              {{ t('raporlar.temsilciAtanmamis') }}
            </span>
            <span v-else>{{ s.data.temsilciAd }}</span>
          </template>
        </Column>
        <Column
          field="faturaSayisi"
          :header="t('raporlar.faturaSayisi')"
          style="width: 120px"
        />
        <Column
          field="toplamSatis"
          :header="t('raporlar.toplamSatis')"
          style="width: 160px"
        >
          <template #body="s">
            <span class="positive">{{ formatCurrency(s.data.toplamSatis) }}</span>
          </template>
        </Column>
        <Column
          field="ortalamaFatura"
          :header="t('raporlar.ortalamaFatura')"
          style="width: 150px"
        >
          <template #body="s">
            {{ formatCurrency(s.data.ortalamaFatura) }}
          </template>
        </Column>
      </DataTable>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { raporAPI } from '../api/index.js'
import { formatCurrency, getLocalDateString } from '../utils/format.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'

const props = defineProps({
  tarihAraligi: { type: Array, default: () => [] }
})

const { t } = useI18n()
const toastBildirim = useToastBildirim()

const tpBas = ref(new Date(new Date().getFullYear(), 0, 1))
const tpBit = ref(new Date())
const tpData = ref(null)
const tpLoading = ref(false)

const tarihUygula = () => {
  const v = props.tarihAraligi
  if (v && v.length === 2 && v[0]) {
    tpBas.value = v[0]
    tpBit.value = v[1]
  }
}

const getTemsilciPerformans = async () => {
  tpLoading.value = true
  try {
    const r = await raporAPI.temsilciPerformans({
      baslangic: getLocalDateString(tpBas.value),
      bitis: getLocalDateString(tpBit.value)
    })
    tpData.value = r.data
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('raporlar.temsilciPerformansHata'))
    tpData.value = null
  } finally {
    tpLoading.value = false
  }
}

onMounted(() => {
  tarihUygula()
  getTemsilciPerformans()
})
watch(() => props.tarihAraligi, () => {
  tarihUygula()
  getTemsilciPerformans()
})

defineExpose({ getTemsilciPerformans })
</script>
