<template>
  <div class="depo-transfer-paneli">
    <div class="transfer-arac">
      <SelectButton
        v-model="durum"
        :options="durumSecenekleri"
        option-label="label"
        option-value="value"
        :allow-empty="false"
        @update:model-value="filtreDegisti"
      />
      <Dropdown
        v-model="depoId"
        :options="depolar"
        option-label="ad"
        option-value="id"
        show-clear
        :placeholder="t('depolar.depo')"
        @update:model-value="filtreDegisti"
      />
      <Button
        :label="t('depolar.csvIndir')"
        icon="pi pi-download"
        text
        size="small"
        @click="csvIndir"
      />
      <Button
        :label="t('common.retry')"
        icon="pi pi-refresh"
        text
        size="small"
        @click="yukle"
      />
    </div>

    <DataTable
      :value="liste"
      striped-rows
      size="small"
      :loading="yukleniyor"
      data-test="depo-transfer-tablo"
    >
      <template #empty>
        <EmptyState
          icon="pi pi-send"
          :message="t('depolar.transferYok')"
        />
      </template>
      <Column
        :header="t('depolar.guzergah')"
      >
        <template #body="{ data }">
          <span>{{ data.kaynakDepoAd || data.kaynakDepoId }}</span>
          <i class="pi pi-arrow-right" />
          <span>{{ data.hedefDepoAd || data.hedefDepoId }}</span>
        </template>
      </Column>
      <Column
        field="stokAd"
        :header="t('depolar.urun')"
      />
      <Column
        field="miktar"
        :header="t('depolar.miktar')"
      />
      <Column
        field="olusturanKullaniciAd"
        :header="t('depolar.talepEden')"
      />
      <Column
        field="yasSaat"
        :header="t('depolar.yas')"
      >
        <template #body="{ data }">
          {{ data.yasSaat ?? 0 }} sa
        </template>
      </Column>
      <Column
        field="slaDurumu"
        :header="t('depolar.sla')"
      >
        <template #body="{ data }">
          <Tag
            :value="t('depolar.sla' + (data.slaDurumu || 'NORMAL'))"
            :severity="slaSeverity(data.slaDurumu)"
          />
        </template>
      </Column>
      <Column
        field="durum"
        :header="t('common.status')"
      >
        <template #body="{ data }">
          <Tag
            :value="data.durum"
            :severity="data.durum === 'ONAYLANDI' ? 'success' : (data.durum === 'REDDEDILDI' ? 'danger' : 'warning')"
          />
          <small
            v-if="data.redSebebi"
            class="red-sebep"
          >{{ data.redSebebi }}</small>
        </template>
      </Column>
      <Column
        v-if="adminMi"
        :header="t('common.actions')"
        style="width: 120px"
      >
        <template #body="{ data }">
          <template v-if="data.durum === 'BEKLIYOR'">
            <Button
              icon="pi pi-check"
              severity="success"
              text
              rounded
              size="small"
              :aria-label="t('depolar.onayla')"
              @click="onayla(data)"
            />
            <Button
              icon="pi pi-times"
              severity="danger"
              text
              rounded
              size="small"
              :aria-label="t('depolar.reddet')"
              @click="redAc(data)"
            />
          </template>
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

    <Dialog
      v-model:visible="redDialog"
      :header="t('depolar.reddet')"
      modal
      :style="{ width: '92%', maxWidth: '400px' }"
    >
      <div class="field">
        <label>{{ t('depolar.redSebebi') }}</label>
        <Textarea
          v-model="redSebep"
          rows="3"
          maxlength="500"
          class="w-full"
        />
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          text
          @click="redDialog = false"
        />
        <Button
          :label="t('depolar.reddet')"
          severity="danger"
          :disabled="redSebep.trim().length < 3"
          :loading="islemde"
          @click="reddet"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { depoTransferAPI } from '../../api/index.js'
import { useToastBildirim } from '../../composables/useToastBildirim.js'

const props = defineProps({
  depolar: { type: Array, default: () => [] },
  adminMi: { type: Boolean, default: false }
})
const emit = defineEmits(['yenile'])

const { t } = useI18n()
const toast = useToastBildirim()

const durum = ref('BEKLIYOR')
const depoId = ref(null)
const sayfa = ref(0)
const boyut = ref(20)
const liste = ref([])
const toplam = ref(0)
const yukleniyor = ref(false)
const islemde = ref(false)
const redDialog = ref(false)
const redSebep = ref('')
const secili = ref(null)

const durumSecenekleri = computed(() => [
  { label: t('depolar.bekleyen'), value: 'BEKLIYOR' },
  { label: t('depolar.onaylanan'), value: 'ONAYLANDI' },
  { label: t('depolar.reddedilen'), value: 'REDDEDILDI' },
  { label: t('depolar.tumu'), value: 'TUMU' }
])

const slaSeverity = (s) => (s === 'GECIKTI' ? 'danger' : (s === 'YAKLASIYOR' ? 'warning' : 'success'))

const yukle = async () => {
  yukleniyor.value = true
  try {
    const r = await depoTransferAPI.sayfali({
      durum: durum.value === 'TUMU' ? undefined : durum.value,
      depoId: depoId.value || undefined,
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

const onayla = async (data) => {
  islemde.value = true
  try {
    await depoTransferAPI.onayla(data.id)
    toast.basarili(t('depolar.transferOnaylandi'))
    await yukle()
    emit('yenile')
  } catch (e) {
    toast.hata(e?.response?.data?.message || t('depolar.islemBasarisiz'))
  } finally {
    islemde.value = false
  }
}

const redAc = (data) => {
  secili.value = data
  redSebep.value = ''
  redDialog.value = true
}

const reddet = async () => {
  if (redSebep.value.trim().length < 3) return
  islemde.value = true
  try {
    await depoTransferAPI.reddet(secili.value.id, redSebep.value.trim())
    toast.basarili(t('depolar.transferReddedildi'))
    redDialog.value = false
    await yukle()
    emit('yenile')
  } catch (e) {
    toast.hata(e?.response?.data?.message || t('depolar.islemBasarisiz'))
  } finally {
    islemde.value = false
  }
}

const csvIndir = async () => {
  const params = {
    tur: 'transfer',
    durum: durum.value === 'TUMU' ? undefined : durum.value,
    depoId: depoId.value || undefined
  }
  const { depoAPI } = await import('../../api/index.js')
  const dosya = await depoAPI.export(params)
  const url = URL.createObjectURL(new Blob([dosya.data], { type: 'text/csv;charset=utf-8' }))
  const a = document.createElement('a')
  a.href = url
  a.download = 'depo-transferler.csv'
  a.click()
  URL.revokeObjectURL(url)
}

watch(() => props.depolar, () => { if (!depoId.value) yukle() })
watch([durum, depoId], () => yukle(), { immediate: true })
</script>

<style scoped>
.depo-transfer-paneli { display: flex; flex-direction: column; gap: 10px; }
.transfer-arac { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
.red-sebep { display: block; color: var(--text-muted); font-size: 11.5px; }
.field { display: flex; flex-direction: column; gap: 4px; }
.w-full { width: 100%; }

/* Dokunmatik: onay/red hedefleri en az 44px. */
@media (pointer: coarse) {
  .depo-transfer-paneli :deep(.p-button) { min-height: 44px; min-width: 44px; }
}
</style>
