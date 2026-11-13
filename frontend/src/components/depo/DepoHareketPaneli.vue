<template>
  <div
    v-if="depo"
    class="depo-hareket-paneli"
  >
    <div class="hareket-arac">
      <Dropdown
        v-model="tip"
        :options="tipSecenekleri"
        option-label="label"
        option-value="value"
        show-clear
        :placeholder="t('depolar.hareketTipi')"
        @update:model-value="filtreDegisti"
      />
      <span class="toplam">{{ toplam }} {{ t('depolar.hareket') }}</span>
    </div>

    <DataTable
      :value="liste"
      striped-rows
      size="small"
      :loading="yukleniyor"
      data-test="depo-hareket-tablo"
    >
      <template #empty>
        <EmptyState
          icon="pi pi-history"
          :message="t('depolar.hareketYok')"
        />
      </template>
      <Column
        field="tarih"
        :header="t('depolar.tarih')"
      >
        <template #body="{ data }">
          {{ formatZaman(data.tarih || data.hareketTarihi) }}
        </template>
      </Column>
      <Column
        field="tur"
        :header="t('depolar.hareketTipi')"
      />
      <Column
        field="stokAd"
        :header="t('depolar.urun')"
      />
      <Column
        field="miktar"
        :header="t('depolar.miktar')"
      />
      <Column
        field="kaynakTip"
        :header="t('depolar.kaynak')"
      >
        <template #body="{ data }">
          {{ data.kaynakTip || '—' }}
        </template>
      </Column>
      <Column
        field="kullaniciAd"
        :header="t('depolar.kullanici')"
      >
        <template #body="{ data }">
          {{ data.kullaniciAd || '—' }}
        </template>
      </Column>
      <Column
        field="aciklama"
        :header="t('common.description')"
      />
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
import { depoAPI } from '../../api/index.js'

const props = defineProps({
  depo: { type: Object, default: null },
  stokId: { type: [Number, String], default: null }
})

const { t } = useI18n()
const tip = ref(null)
const sayfa = ref(0)
const boyut = ref(20)
const liste = ref([])
const toplam = ref(0)
const yukleniyor = ref(false)

const tipSecenekleri = computed(() => [
  { label: t('depolar.giris'), value: 'GIRIS' },
  { label: t('depolar.cikis'), value: 'CIKIS' },
  { label: t('depolar.duzeltme'), value: 'DUZELTME' }
])

const formatZaman = (v) => {
  if (!v) return '—'
  const d = new Date(v)
  return isNaN(d.getTime()) ? '—' : d.toLocaleString('tr-TR')
}

const yukle = async () => {
  if (!props.depo) return
  yukleniyor.value = true
  try {
    const r = await depoAPI.hareketler(props.depo.id, {
      tip: tip.value || undefined,
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

watch(() => props.depo?.id, () => {
  sayfa.value = 0
  tip.value = null
  yukle()
}, { immediate: true })
</script>

<style scoped>
.depo-hareket-paneli { display: flex; flex-direction: column; gap: 10px; }
.hareket-arac { display: flex; align-items: center; gap: 12px; }
.toplam { color: var(--text-muted); font-size: 13px; }
</style>
