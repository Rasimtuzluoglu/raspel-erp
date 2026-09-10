<template>
  <div class="vergi-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        {{ t('vergiRaporlari.title') }}
      </h1>
    </div>

    <IlkZiyaretIpuclari
      anahtar="vergi-raporlari"
      :baslik="t('vergiRaporlari.ipucuBaslik')"
      :metin="t('vergiRaporlari.ipucuMetin')"
    />

    <div class="donem-secim">
      <label>{{ t('vergiRaporlari.donemAy') }}</label>
      <InputText
        v-model="donem"
        placeholder="YYYY-MM"
        class="donem-input"
        @keyup.enter="yukle"
      />
      <Button
        icon="pi pi-search"
        :label="t('vergiRaporlari.getir')"
        @click="yukle"
      />
    </div>

    <div
      v-if="kdvBeyanname"
      class="vergi-seksiyon"
    >
      <h2 class="seksiyon-baslik">
        <i class="pi pi-file-edit" /> {{ t('vergiRaporlari.kdvBeyannamesi') }} — {{ kdvBeyanname.donem }}
      </h2>
      <div class="kdv-ozet">
        <div class="ozet-kutu">
          <span>{{ t('vergiRaporlari.hesaplananKdv') }}</span><strong>{{ formatCurrency(kdvBeyanname.toplamHesaplananKdv) }}</strong>
        </div>
        <div class="ozet-kutu">
          <span>{{ t('vergiRaporlari.indirilecekKdv') }}</span><strong>{{ formatCurrency(kdvBeyanname.toplamIndirilecekKdv) }}</strong>
        </div>
        <div
          class="ozet-kutu"
          :class="kdvBeyanname.odenecekKdv > 0 ? 'odenecek' : 'devreden'"
        >
          <span>{{ kdvBeyanname.odenecekKdv > 0 ? t('vergiRaporlari.odenecekKdv') : t('vergiRaporlari.devredenKdv') }}</span>
          <strong>{{
            formatCurrency(kdvBeyanname.odenecekKdv > 0 ? kdvBeyanname.odenecekKdv : kdvBeyanname.devredenKdv)
          }}</strong>
        </div>
      </div>

      <div class="kdv-tablolar">
        <div class="kdv-tablo">
          <h3>{{ t('vergiRaporlari.tablo12') }}</h3>
          <DataTable
            :value="kdvBeyanname.satislar"
            striped-rows
          >
            <Column
              field="kdvOrani"
              header="KDV Oranı"
            >
              <template #body="{ data }">
                %{{ data.kdvOrani }}
              </template>
            </Column>
            <Column
              field="matrah"
              :header="t('vergiRaporlari.matrah')"
            >
              <template #body="{ data }">
                {{ formatCurrency(data.matrah) }}
              </template>
            </Column>
            <Column
              field="kdv"
              :header="t('vergiRaporlari.kdv')"
            >
              <template #body="{ data }">
                {{ formatCurrency(data.kdv) }}
              </template>
            </Column>
          </DataTable>
        </div>
        <div class="kdv-tablo">
          <h3>{{ t('vergiRaporlari.tablo1920') }}</h3>
          <DataTable
            :value="kdvBeyanname.alislar"
            striped-rows
          >
            <Column
              field="kdvOrani"
              header="KDV Oranı"
            >
              <template #body="{ data }">
                %{{ data.kdvOrani }}
              </template>
            </Column>
            <Column
              field="matrah"
              :header="t('vergiRaporlari.matrah')"
            >
              <template #body="{ data }">
                {{ formatCurrency(data.matrah) }}
              </template>
            </Column>
            <Column
              field="kdv"
              :header="t('vergiRaporlari.kdv')"
            >
              <template #body="{ data }">
                {{ formatCurrency(data.kdv) }}
              </template>
            </Column>
          </DataTable>
        </div>
      </div>
    </div>

    <div
      v-if="bsRapor || baRapor"
      class="vergi-seksiyon"
    >
      <h2 class="seksiyon-baslik">
        <i class="pi pi-chart-bar" /> {{ t('vergiRaporlari.baBsForm') }} ({{ t('vergiRaporlari.esik') }}: {{ formatCurrency(bsRapor?.esik || baRapor?.esik) }})
      </h2>
      <div class="ba-bs-secim">
        <SelectButton
          v-model="aktifBs"
          :options="[
            { label: t('vergiRaporlari.bsSatis'), value: true },
            { label: t('vergiRaporlari.baAlis'), value: false }
          ]"
          option-label="label"
          option-value="value"
        />
      </div>
      <DataTable
        :value="aktifBs ? bsRapor?.kayitlar || [] : baRapor?.kayitlar || []"
        striped-rows
      >
        <Column
          field="faturaNo"
          :header="t('vergiRaporlari.faturaNo')"
        />
        <Column
          field="tarih"
          :header="t('common.date')"
        >
          <template #body="{ data }">
            {{ formatDate(data.tarih) }}
          </template>
        </Column>
        <Column
          field="cariAd"
          :header="t('vergiRaporlari.cari')"
        />
        <Column
          field="cariVkn"
          :header="t('vergiRaporlari.vknTckn')"
        />
        <Column
          field="matrah"
          :header="t('vergiRaporlari.matrah')"
        >
          <template #body="{ data }">
            {{ formatCurrency(data.matrah) }}
          </template>
        </Column>
        <Column
          field="kdv"
          :header="t('vergiRaporlari.kdv')"
        >
          <template #body="{ data }">
            {{ formatCurrency(data.kdv) }}
          </template>
        </Column>
        <Column
          field="tutar"
          :header="t('common.amount')"
        >
          <template #body="{ data }">
            <strong>{{ formatCurrency(data.tutar) }}</strong>
          </template>
        </Column>
      </DataTable>
      <div
        v-if="aktifBs ? bsRapor?.kayitlar?.length : baRapor?.kayitlar?.length"
        class="ba-bs-toplam"
      >
        {{ t('vergiRaporlari.toplamTutar') }} <strong>{{ formatCurrency(aktifBs ? bsRapor?.toplamTutar : baRapor?.toplamTutar) }}</strong>
      </div>
      <EmptyState
        v-if="!(aktifBs ? bsRapor?.kayitlar?.length : baRapor?.kayitlar?.length)"
        :message="t('vergiRaporlari.esikUstuKayitYok')"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { raporAPI } from '../api/index.js'
import IlkZiyaretIpuclari from '../components/IlkZiyaretIpuclari.vue'
import SelectButton from 'primevue/selectbutton'
import { formatCurrency } from '../utils/format.js'
import { useI18n } from 'vue-i18n'

const toastBildirim = useToastBildirim()
const { t } = useI18n()

const donem = ref(new Date().toISOString().slice(0, 7))
const kdvBeyanname = ref(null)
const bsRapor = ref(null)
const baRapor = ref(null)
const aktifBs = ref(true)

import { formatTarih as formatDate } from '../utils/format.js'

onMounted(yukle)

const yukle = async () => {
  if (!/^\d{4}-\d{2}$/.test(donem.value)) {
    toastBildirim.uyari(t('vergiRaporlari.donemFormatUyari'))
    return
  }
  try {
    const [kdv, bs, ba] = await Promise.all([
      raporAPI.kdvBeyanname(donem.value),
      raporAPI.baBs({ donem: donem.value, tur: 'BS' }),
      raporAPI.baBs({ donem: donem.value, tur: 'BA' })
    ])
    kdvBeyanname.value = kdv.data
    bsRapor.value = bs.data
    baRapor.value = ba.data
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('vergiRaporlari.hataRapor'))
  }
}
</script>

<style scoped>
.vergi-container {
  padding: 0;
}
.sayfa-baslik {
  margin-bottom: 20px;
}
.donem-secim {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}
.donem-secim label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 6px;
  display: block;
}
.donem-input {
  width: 140px;
}
.vergi-seksiyon {
  margin-bottom: 28px;
}
.seksiyon-baslik {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  margin-bottom: 16px;
  color: var(--text-primary);
}
.seksiyon-baslik i {
  color: #3b82f6;
}
.kdv-ozet {
  display: flex;
  gap: 14px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}
.ozet-kutu {
  flex: 1;
  min-width: min(180px, 100%);
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.ozet-kutu span {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 600;
  text-transform: uppercase;
}
.ozet-kutu strong {
  font-size: 20px;
}
.ozet-kutu.odenecek strong {
  color: #ef4444;
}
.ozet-kutu.devreden strong {
  color: #10b981;
}
.kdv-tablolar {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}
.kdv-tablo h3 {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0 0 10px;
}
.ba-bs-secim {
  margin-bottom: 12px;
}
.ba-bs-toplam {
  margin-top: 12px;
  padding: 10px 14px;
  background: rgba(59, 130, 246, 0.08);
  border-radius: 8px;
  font-size: 13px;
}
@media (max-width: 900px) {
  .kdv-tablolar {
    grid-template-columns: 1fr;
  }
}
</style>
