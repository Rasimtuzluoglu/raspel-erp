<template>
  <div
    v-if="depo"
    class="depo-stok-paneli"
  >
    <div class="depo-basligi">
      <i class="pi pi-warehouse" />
      <strong>{{ depo.ad }}</strong>
      <Tag
        v-if="depo.subeAdi"
        :value="depo.subeAdi"
        severity="secondary"
      />
    </div>

    <div
      v-if="islemYapabilir"
      class="stok-islem-form"
    >
      <h3>{{ t('depolar.stokEkleCikar') }}</h3>
      <div class="form-row">
        <Dropdown
          v-model="form.stokId"
          :options="stokSecenekleri"
          option-label="ad"
          option-value="id"
          filter
          :placeholder="t('depolar.urunSec')"
          class="w-full"
        />
        <InputNumber
          v-model="form.miktar"
          :min="0"
          :placeholder="t('depolar.miktar')"
          @keyup="miktarTazele"
        />
        <Button
          :label="t('depolar.ekle')"
          icon="pi pi-plus"
          severity="success"
          :loading="kaydediliyor"
          :disabled="!gecerli"
          @click="islemYap('ekle')"
        />
        <Button
          :label="t('depolar.cikar')"
          icon="pi pi-minus"
          severity="warning"
          :loading="kaydediliyor"
          :disabled="!gecerli"
          @click="islemYap('cikar')"
        />
      </div>
      <small
        v-if="seciliMevcut"
        class="mevcut-stok-bilgi"
      >
        <i class="pi pi-info-circle" />
        {{ t('depolar.mevcutStok') }}: <strong>{{ formatMiktar(seciliMevcut) }}</strong>
      </small>
    </div>

    <div class="stok-tablo-ust">
      <span
        class="stok-ozet"
        aria-live="polite"
      >
        {{ toplam }} {{ t('depolar.kalem') }}
      </span>
      <span class="arama-kutu">
        <i class="pi pi-search" />
        <InputText
          v-model="q"
          :placeholder="t('depolar.stokAra')"
          class="arama-input"
          @update:model-value="aramaDegisti"
        />
      </span>
      <label class="filtre">
        <Checkbox
          v-model="kritik"
          binary
          @update:model-value="filtreDegisti"
        />
        {{ t('depolar.kritik') }}
      </label>
      <label class="filtre">
        <Checkbox
          v-model="sifir"
          binary
          @update:model-value="filtreDegisti"
        />
        {{ t('depolar.kpiSifirStok') }}
      </label>
      <Button
        :label="t('depolar.csvIndir')"
        icon="pi pi-download"
        text
        size="small"
        @click="csvIndir"
      />
    </div>

    <DataTable
      :value="liste"
      striped-rows
      size="small"
      :loading="yukleniyor"
      data-test="depo-stok-tablo"
    >
      <template #empty>
        <EmptyState
          icon="pi pi-box"
          :message="q ? t('common.noData') : t('depolar.stokYok')"
        />
      </template>
      <Column
        field="stokKodu"
        :header="t('depolar.stokKodu')"
        sortable
      />
      <Column
        field="stokAd"
        :header="t('depolar.urunAdi')"
        sortable
      />
      <Column
        field="birim"
        :header="t('depolar.birim')"
      />
      <Column
        field="miktar"
        :header="t('depolar.miktar')"
        sortable
      >
        <template #body="{ data }">
          <span :class="{ 'text-danger': Number(data.miktar) <= 0 }">{{ formatMiktar(data.miktar) }}</span>
        </template>
      </Column>
      <Column
        field="minMiktar"
        :header="t('depolar.minStok')"
      >
        <template #body="{ data }">
          {{ data.minMiktar != null ? formatMiktar(data.minMiktar) : '—' }}
        </template>
      </Column>
      <Column
        field="kritikMi"
        :header="t('depolar.kritik')"
      >
        <template #body="{ data }">
          <Tag
            v-if="data.kritikMi"
            :value="t('depolar.kritik')"
            severity="danger"
          />
          <span v-else>—</span>
        </template>
      </Column>
      <Column
        field="deger"
        :header="t('depolar.stokDegeri')"
      >
        <template #body="{ data }">
          {{ formatCurrency(data.deger || 0) }}
        </template>
      </Column>
      <Column
        :header="t('depolar.hareketler')"
        style="width: 80px"
      >
        <template #body="{ data }">
          <Button
            icon="pi pi-history"
            text
            rounded
            size="small"
            :aria-label="t('depolar.hareketler')"
            @click="$emit('hareket-goster', { stokId: data.stokId })"
          />
        </template>
      </Column>
    </DataTable>

    <Paginator
      v-if="toplam > boyut"
      :rows="boyut"
      :total-records="toplam"
      :first="sayfa * boyut"
      :rows-per-page-options="[20, 50, 100]"
      @page="sayfaDegisti"
    />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { depoAPI, stokAPI } from '../../api/index.js'
import { unwrapList } from '../../api/utils/unwrap.js'
import { useToastBildirim } from '../../composables/useToastBildirim.js'
import { formatCurrency } from '../../utils/format.js'

const props = defineProps({
  depo: { type: Object, default: null },
  islemYapabilir: { type: Boolean, default: false }
})
const emit = defineEmits(['hareket-goster', 'ozet-yenile'])

const { t } = useI18n()
const toast = useToastBildirim()

const q = ref('')
const kritik = ref(false)
const sifir = ref(false)
const sayfa = ref(0)
const boyut = ref(20)
const liste = ref([])
const toplam = ref(0)
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const stoklar = ref([])
const form = ref({ stokId: null, miktar: 0 })
let aramaZamanlayici = null

const stokSecenekleri = computed(() => stoklar.value)
const gecerli = computed(() => form.value.stokId && Number(form.value.miktar) > 0)
const seciliMevcut = computed(() => {
  if (!form.value.stokId) return null
  const satir = liste.value.find((s) => s.stokId === form.value.stokId)
  return satir ? satir.miktar : 0
})

const formatMiktar = (v) => Number(v || 0).toLocaleString('tr-TR', { maximumFractionDigits: 4 })

/**
 * InputNumber modelini yalnizca blur'da yazar; kullanici miktari yazip
 * dogrudan "Ekle"ye bastiginda dugme hala kapali gorunuyordu. Her tus
 * vurusunda ham metni sayiya cevirip modeli tazeliyoruz (negatif/sifir
 * degerler `gecerli` uzerinden kapali kalir).
 */
const miktarTazele = (e) => {
  const ham = String(e?.target?.value ?? '').trim()
  if (!ham) {
    form.value.miktar = null
    return
  }
  const normal = ham.includes(',') ? ham.replace(/\./g, '').replace(',', '.') : ham
  const sayi = Number(normal)
  form.value.miktar = Number.isFinite(sayi) ? sayi : null
}

const yukle = async () => {
  if (!props.depo) return
  yukleniyor.value = true
  try {
    const r = await depoAPI.stokSayfali(props.depo.id, {
      q: q.value || undefined,
      kritik: kritik.value || undefined,
      sifir: sifir.value || undefined,
      page: sayfa.value,
      size: boyut.value
    })
    liste.value = r.data?.content || []
    toplam.value = Number(r.data?.totalElements || 0)
  } catch {
    liste.value = []
    toplam.value = 0
  } finally {
    yukleniyor.value = false
  }
}

const stoklariYukle = async () => {
  try {
    const r = await stokAPI.getAll({ size: 500 })
    stoklar.value = unwrapList(r)
  } catch {
    stoklar.value = []
  }
}

const aramaDegisti = () => {
  if (aramaZamanlayici) clearTimeout(aramaZamanlayici)
  aramaZamanlayici = setTimeout(() => {
    sayfa.value = 0
    yukle()
  }, 300)
}

const filtreDegisti = () => {
  sayfa.value = 0
  yukle()
}

const sayfaDegisti = (e) => {
  if (e.rows !== boyut.value) {
    boyut.value = e.rows
    sayfa.value = 0
  } else {
    sayfa.value = Math.floor(e.first / e.rows)
  }
  yukle()
}

const islemYap = async (tur) => {
  if (!gecerli.value) return
  kaydediliyor.value = true
  try {
    const veri = { stokId: form.value.stokId, miktar: Number(form.value.miktar) }
    if (tur === 'ekle') await depoAPI.stokEkle(props.depo.id, veri)
    else await depoAPI.stokCikar(props.depo.id, veri)
    toast.basarili(t(tur === 'ekle' ? 'depolar.stokEklendi' : 'depolar.stokCikarildi'))
    form.value = { stokId: null, miktar: 0 }
    await yukle()
    emit('ozet-yenile')
  } catch (e) {
    toast.hata(e?.response?.data?.message || t('depolar.islemBasarisiz'))
  } finally {
    kaydediliyor.value = false
  }
}

const csvIndir = async () => {
  const r = await depoAPI.export({
    tur: 'stok',
    depoId: props.depo.id,
    q: q.value || undefined,
    kritik: kritik.value || undefined,
    sifir: sifir.value || undefined
  })
  const url = URL.createObjectURL(new Blob([r.data], { type: 'text/csv;charset=utf-8' }))
  const a = document.createElement('a')
  a.href = url
  a.download = `depo-${props.depo.id}-stok.csv`
  a.click()
  URL.revokeObjectURL(url)
}

watch(() => props.depo?.id, () => {
  sayfa.value = 0
  q.value = ''
  kritik.value = false
  sifir.value = false
  yukle()
}, { immediate: true })

watch(() => props.islemYapabilir, (v) => { if (v) stoklariYukle() }, { immediate: true })
</script>

<style scoped>
.depo-stok-paneli { display: flex; flex-direction: column; gap: 12px; }
.depo-basligi { display: flex; align-items: center; gap: 8px; }
.stok-islem-form { border: 1px solid var(--border); border-radius: 12px; padding: 12px; }
.form-row { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
.form-row :deep(.p-dropdown) { min-width: 240px; }
.w-full { width: 100%; }
.stok-tablo-ust { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; }
.arama-kutu { display: inline-flex; align-items: center; gap: 6px; }
.filtre { display: inline-flex; align-items: center; gap: 4px; font-size: 13px; }
.mevcut-stok-bilgi { display: inline-flex; align-items: center; gap: 4px; color: var(--text-muted); }
.text-danger { color: var(--danger); }

/* Dokunmatik: islem hedefleri en az 44px. */
@media (pointer: coarse) {
  .form-row :deep(.p-button),
  .stok-tablo-ust :deep(.p-button) { min-height: 44px; }
  .form-row :deep(.p-select) { min-height: 44px; }
}
</style>
