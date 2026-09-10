<template>
  <div class="mutabakat-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        {{ t('bankaMutabakat.title') }}
      </h1>
    </div>

    <IlkZiyaretIpuclari
      anahtar="banka-mutabakat"
      :baslik="t('bankaMutabakat.ipucuBaslik')"
      :metin="t('bankaMutabakat.ipucuMetin')"
    />

    <div class="mutabakat-ust">
      <div class="banka-secim">
        <label>{{ t('bankaMutabakat.banka') }}</label>
        <Select
          v-model="seciliBanka"
          :options="bankalar"
          option-label="ad"
          option-value="id"
          :placeholder="t('bankaMutabakat.bankaSecin')"
          class="w-full"
          filter
          show-clear
          @change="bankaDegisti"
        />
      </div>
      <div class="ust-butonlar">
        <input
          ref="dosyaInput"
          type="file"
          accept=".csv,.xlsx,.txt,.ofx,.qfx"
          hidden
          @change="dosyaSecildi"
        >
        <Button
          :label="t('bankaMutabakat.hesapOzetiYukle')"
          icon="pi pi-upload"
          :disabled="!seciliBanka"
          :loading="yukleniyor"
          @click="dosyaInput.click()"
        />
        <Button
          :label="t('bankaMutabakat.otomatikEslestir')"
          icon="pi pi-link"
          severity="secondary"
          outlined
          :disabled="!seciliBanka"
          @click="otomatikEslestir"
        />
      </div>
    </div>

    <div
      v-if="eslesenSayisi !== null"
      class="mutabakat-ozet"
    >
      <div class="ozet-kutu">
        <span>{{ t('bankaMutabakat.toplamHareket') }}</span><strong>{{ hareketler ? hareketler.length : 0 }}</strong>
      </div>
      <div class="ozet-kutu">
        <span>{{ t('bankaMutabakat.eslesen') }}</span><strong class="pozitif">{{ eslesenSayisi }}</strong>
      </div>
      <div class="ozet-kutu">
        <span>{{ t('bankaMutabakat.eslesmeyen') }}</span><strong class="negatif">{{ eslesmeyenSayisi }}</strong>
      </div>
      <div class="ozet-kutu">
        <span>{{ t('bankaMutabakat.eslesmeOrani') }}</span><strong>{{ eslesmeOrani }}</strong>
      </div>
    </div>

    <AppDataTable
      :value="hareketler"
      :loading="yukleniyor"
      arama-aktif
      :arama-placeholder="t('bankaMutabakat.aramaPlaceholder')"
      gorunum-anahtari="banka_mutabakat"
      :empty-message="t('bankaMutabakat.empty')"
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
        field="aciklama"
        :header="t('common.description')"
      />
      <Column
        field="borc"
        :header="t('bankaMutabakat.borc')"
      >
        <template #body="{ data }">
          <span class="negatif">{{ formatCurrency(data.borc) }}</span>
        </template>
      </Column>
      <Column
        field="alacak"
        :header="t('bankaMutabakat.alacak')"
      >
        <template #body="{ data }">
          <span class="pozitif">{{ formatCurrency(data.alacak) }}</span>
        </template>
      </Column>
      <Column
        field="bakiye"
        :header="t('bankaMutabakat.bakiye')"
      >
        <template #body="{ data }">
          {{ data.bakiye != null ? formatCurrency(data.bakiye) : '-' }}
        </template>
      </Column>
      <Column
        field="eslestirildi"
        :header="t('bankaMutabakat.eslesme')"
        sortable
      >
        <template #body="{ data }">
          <Tag
            :value="data.eslestirildi ? t('bankaMutabakat.eslesti') : t('bankaMutabakat.eslesmedi')"
            :severity="data.eslestirildi ? 'success' : 'danger'"
          />
          <div
            v-if="data.eslesenFaturaNo"
            class="eslesen-fatura"
          >
            #{{ data.eslesenFaturaNo }}
          </div>
          <div
            v-else-if="data.onerilenFaturaNo"
            class="oneri-fatura"
          >
            <i class="pi pi-lightbulb" />
            <span>{{ t('bankaMutabakat.oneri') }} #{{ data.onerilenFaturaNo }}</span>
            <span
              class="guven-skoru"
              :class="skorSinifi(data.guvenSkoru)"
            >%{{ data.guvenSkoru }}</span>
          </div>
        </template>
      </Column>
      <Column
        :header="t('bankaMutabakat.islem')"
        style="width: 200px"
      >
        <template #body="{ data }">
          <div
            v-if="!data.eslestirildi"
            class="eylem-btns"
          >
            <Button
              v-if="data.onerilenFaturaId"
              :label="t('bankaMutabakat.onayla')"
              icon="pi pi-check"
              class="p-button-sm p-button-success p-button-outlined"
              @click="oneriOnayla(data)"
            />
            <Select
              v-model="data.eslesenFaturaId"
              :options="faturalar"
              option-label="etiket"
              option-value="id"
              :placeholder="t('bankaMutabakat.faturaBagla')"
              filter
              class="fatura-bagla"
              @change="manuelEslestir(data)"
            />
          </div>
          <Button
            v-else
            icon="pi pi-times"
            class="p-button-rounded p-button-text"
            :title="t('bankaMutabakat.eslestirmeyiKaldir')"
            @click="eslestirmeyiKaldir(data)"
          />
        </template>
      </Column>
    </AppDataTable>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { bankaAPI, bankaMutabakatAPI, faturaAPI } from '../api/index.js'
import IlkZiyaretIpuclari from '../components/IlkZiyaretIpuclari.vue'
import { useI18n } from 'vue-i18n'

const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { t } = useI18n()

const bankalar = ref([])
const faturalar = ref([])
const seciliBanka = ref(null)
const hareketler = ref([])
const yukleniyor = ref(false)
const dosyaInput = ref(null)

const eslesenSayisi = computed(() => hareketler.value.filter((h) => h.eslestirildi).length)
const eslesmeyenSayisi = computed(() => hareketler.value.length - eslesenSayisi.value)
const eslesmeOrani = computed(() => {
  if (!hareketler.value.length) return '—'
  return Math.round((eslesenSayisi.value / hareketler.value.length) * 100) + '%'
})

const formatCurrency = (v) =>
  v == null || Number(v) === 0
    ? '0,00 ₺'
    : new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(v)
import { formatTarih as formatDate } from '../utils/format.js'

onMounted(async () => {
  try {
    const r = await bankaAPI.getAll()
    bankalar.value = r.data || []
  } catch {
    /* empty */
  }
  try {
    const rf = await faturaAPI.getAll()
    const data = rf.data?.content || rf.data || []
    faturalar.value = data.map((f) => ({ ...f, etiket: `${f.faturaNumarasi} (${formatCurrency(f.genelToplam)})` }))
  } catch {
    /* empty */
  }
})

const bankaDegisti = () => {
  hareketler.value = []
  if (seciliBanka.value) yukle()
}

const yukle = async () => {
  yukleniyor.value = true
  try {
    const r = await bankaMutabakatAPI.listele(seciliBanka.value)
    hareketler.value = r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('bankaMutabakat.hataYukleme'))
  }
  yukleniyor.value = false
}

const dosyaSecildi = async (e) => {
  const file = e.target.files[0]
  if (!file) return
  yukleniyor.value = true
  try {
    await bankaMutabakatAPI.yukle(seciliBanka.value, file)
    toast.add({ severity: 'success', summary: t('bankaMutabakat.yuklendi'), detail: t('bankaMutabakat.yuklendiDetay'), life: 3000 })
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('bankaMutabakat.yuklemeBasarisiz'))
  }
  yukleniyor.value = false
  e.target.value = ''
}

const otomatikEslestir = async () => {
  yukleniyor.value = true
  try {
    await bankaMutabakatAPI.otomatikEslestir(seciliBanka.value)
    toast.add({ severity: 'success', summary: t('bankaMutabakat.eslestirildi'), detail: t('bankaMutabakat.eslestirildiDetay'), life: 3000 })
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('bankaMutabakat.eslestirmeBasarisiz'))
  }
  yukleniyor.value = false
}

const manuelEslestir = async (hareket) => {
  if (!hareket.eslesenFaturaId) return
  try {
    await bankaMutabakatAPI.eslestir(seciliBanka.value, hareket.id, hareket.eslesenFaturaId)
    toast.add({ severity: 'success', summary: t('bankaMutabakat.eslesti'), detail: t('bankaMutabakat.eslestiDetay'), life: 3000 })
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('bankaMutabakat.eslestirmeBasarisiz'))
  }
}

const skorSinifi = (skor) => {
  if (skor == null) return ''
  if (skor >= 90) return 'yuksek'
  if (skor >= 70) return 'orta'
  return 'dusuk'
}

const oneriOnayla = async (hareket) => {
  if (!hareket.onerilenFaturaId) return
  hareket.eslesenFaturaId = hareket.onerilenFaturaId
  await manuelEslestir(hareket)
}

const eslestirmeyiKaldir = (hareket) => {
  confirm.require({
    message: t('bankaMutabakat.kaldirOnayMesaj'),
    header: t('kasa.onay'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('bankaMutabakat.evet'),
    rejectLabel: t('common.cancel'),
    accept: async () => {
      try {
        await bankaMutabakatAPI.eslestirmeyiKaldir(seciliBanka.value, hareket.id)
        toast.add({ severity: 'success', summary: t('bankaMutabakat.kaldirildi'), detail: t('bankaMutabakat.kaldirildiDetay'), life: 3000 })
        await yukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('bankaMutabakat.islemBasarisiz'))
      }
    }
  })
}
</script>

<style scoped>
.mutabakat-container {
  padding: 0;
}
.sayfa-baslik {
  margin-bottom: 20px;
}
.mutabakat-ust {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
.banka-secim {
  min-width: min(280px, 100%);
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.banka-secim label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
}
.ust-butonlar {
  display: flex;
  gap: 8px;
}
.mutabakat-ozet {
  display: flex;
  gap: 14px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}
.ozet-kutu {
  flex: 1;
  min-width: min(140px, 100%);
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.ozet-kutu span {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 600;
  text-transform: uppercase;
}
.ozet-kutu strong {
  font-size: 18px;
}
.pozitif {
  color: #10b981;
  font-weight: 600;
}
.negatif {
  color: #ef4444;
  font-weight: 600;
}
.eslesen-fatura {
  font-size: 11px;
  color: #10b981;
  margin-top: 4px;
  font-weight: 600;
}
.oneri-fatura {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: var(--text-secondary);
  margin-top: 4px;
  flex-wrap: wrap;
}
.oneri-fatura i {
  color: #f59e0b;
}
.guven-skoru {
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 10px;
  font-size: 10px;
}
.guven-skoru.yuksek {
  background: rgba(16, 185, 129, 0.15);
  color: #10b981;
}
.guven-skoru.orta {
  background: rgba(245, 158, 11, 0.15);
  color: #f59e0b;
}
.guven-skoru.dusuk {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
}
.fatura-bagla {
  min-width: 160px;
}
.eylem-btns {
  display: flex;
  align-items: center;
}
.w-full {
  width: 100%;
}
</style>
