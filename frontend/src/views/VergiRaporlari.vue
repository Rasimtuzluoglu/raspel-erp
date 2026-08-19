<template>
  <div class="vergi-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        Vergi Raporları
      </h1>
    </div>

    <IlkZiyaretIpuclari
      anahtar="vergi-raporlari"
      baslik="KDV Beyanname & BA/BS"
      metin="Dönem seçin: KDV beyannameye hazırlık (hesaplanan/indirilecek KDV) ve BA/BS bildirim formları için eşik üstü fatura listeleri üretilir."
    />

    <div class="donem-secim">
      <label>Dönem (Ay)</label>
      <InputText
        v-model="donem"
        placeholder="YYYY-MM"
        class="donem-input"
        @keyup.enter="yukle"
      />
      <Button
        icon="pi pi-search"
        label="Getir"
        @click="yukle"
      />
    </div>

    <div
      v-if="kdvBeyanname"
      class="vergi-seksiyon"
    >
      <h2 class="seksiyon-baslik">
        <i class="pi pi-file-edit" /> KDV Beyannamesi — {{ kdvBeyanname.donem }}
      </h2>
      <div class="kdv-ozet">
        <div class="ozet-kutu">
          <span>Hesaplanan KDV (Satış)</span><strong>{{ formatCurrency(kdvBeyanname.toplamHesaplananKdv) }}</strong>
        </div>
        <div class="ozet-kutu">
          <span>İndirilecek KDV (Alış)</span><strong>{{ formatCurrency(kdvBeyanname.toplamIndirilecekKdv) }}</strong>
        </div>
        <div
          class="ozet-kutu"
          :class="kdvBeyanname.odenecekKdv > 0 ? 'odenecek' : 'devreden'"
        >
          <span>{{ kdvBeyanname.odenecekKdv > 0 ? 'Ödenecek KDV' : 'Devreden KDV' }}</span>
          <strong>{{
            formatCurrency(kdvBeyanname.odenecekKdv > 0 ? kdvBeyanname.odenecekKdv : kdvBeyanname.devredenKdv)
          }}</strong>
        </div>
      </div>

      <div class="kdv-tablolar">
        <div class="kdv-tablo">
          <h3>1-2 no.lu Tablo (Hesaplanan KDV)</h3>
          <DataTable
            state-storage="session"
            state-key="vergiraporlari-table-state"
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
              header="Matrah"
            >
              <template #body="{ data }">
                {{ formatCurrency(data.matrah) }}
              </template>
            </Column>
            <Column
              field="kdv"
              header="KDV"
            >
              <template #body="{ data }">
                {{ formatCurrency(data.kdv) }}
              </template>
            </Column>
          </DataTable>
        </div>
        <div class="kdv-tablo">
          <h3>19-20 no.lu Tablo (İndirilecek KDV)</h3>
          <DataTable
            state-storage="session"
            state-key="vergiraporlari-table-state"
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
              header="Matrah"
            >
              <template #body="{ data }">
                {{ formatCurrency(data.matrah) }}
              </template>
            </Column>
            <Column
              field="kdv"
              header="KDV"
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
        <i class="pi pi-chart-bar" /> BA/BS Bildirim Formu (eşik: {{ formatCurrency(bsRapor?.esik || baRapor?.esik) }})
      </h2>
      <div class="ba-bs-secim">
        <SelectButton
          v-model="aktifBs"
          :options="[
            { label: 'BS (Satış)', value: true },
            { label: 'BA (Alış)', value: false }
          ]"
          option-label="label"
          option-value="value"
        />
      </div>
      <DataTable
        state-storage="session"
        state-key="vergiraporlari-table-state"
        :value="aktifBs ? bsRapor?.kayitlar || [] : baRapor?.kayitlar || []"
        striped-rows
      >
        <Column
          field="faturaNo"
          header="Fatura No"
        />
        <Column
          field="tarih"
          header="Tarih"
        >
          <template #body="{ data }">
            {{ formatDate(data.tarih) }}
          </template>
        </Column>
        <Column
          field="cariAd"
          header="Cari"
        />
        <Column
          field="cariVkn"
          header="VKN/TCKN"
        />
        <Column
          field="matrah"
          header="Matrah"
        >
          <template #body="{ data }">
            {{ formatCurrency(data.matrah) }}
          </template>
        </Column>
        <Column
          field="kdv"
          header="KDV"
        >
          <template #body="{ data }">
            {{ formatCurrency(data.kdv) }}
          </template>
        </Column>
        <Column
          field="tutar"
          header="Tutar"
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
        Toplam Tutar: <strong>{{ formatCurrency(aktifBs ? bsRapor?.toplamTutar : baRapor?.toplamTutar) }}</strong>
      </div>
      <EmptyState
        v-if="!(aktifBs ? bsRapor?.kayitlar?.length : baRapor?.kayitlar?.length)"
        message="Eşik üstü kayıt yok"
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

const toastBildirim = useToastBildirim()

const donem = ref(new Date().toISOString().slice(0, 7))
const kdvBeyanname = ref(null)
const bsRapor = ref(null)
const baRapor = ref(null)
const aktifBs = ref(true)

const formatCurrency = (v) =>
  v == null ? '0,00 ₺' : new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(v)
const formatDate = (d) =>
  d ? new Intl.DateTimeFormat('tr-TR', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(d)) : '-'

onMounted(yukle)

const yukle = async () => {
  if (!/^\d{4}-\d{2}$/.test(donem.value)) {
    toastBildirim.uyari('Dönemi YYYY-MM formatında girin')
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
    toastBildirim.hata(err?.response?.data?.message || 'Rapor alınamadı')
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
  min-width: 180px;
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
  grid-template-columns: 1fr 1fr;
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
