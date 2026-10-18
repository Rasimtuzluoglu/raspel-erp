<template>
  <Dialog
    :visible="visible"
    :header="t('depolar.depoDetay')"
    modal
    :style="{ width: '90%', maxWidth: '720px' }"
    :breakpoints="{ '575px': '95vw' }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div
      v-if="depo"
      class="depo-360"
    >
      <div class="depo-360-ust">
        <div class="depo-360-baslik">
          <span class="depo-360-ikon"><i class="pi pi-warehouse" /></span>
          <div class="depo-360-ad">
            <h3>{{ depo.ad }}</h3>
            <span class="depo-360-alt">{{ depo.subeAdi || '-' }}</span>
          </div>
        </div>
        <Tag
          :value="depo.aktif ? t('status.active') : t('status.passive')"
          :severity="depo.aktif ? 'success' : 'danger'"
        />
      </div>

      <div class="depo-360-bilgi">
        <div class="bilgi-satir">
          <span>{{ t('depolar.sorumlu') }}</span>
          <strong>{{ depo.yetkili || '-' }}</strong>
        </div>
        <div class="bilgi-satir">
          <span>{{ t('depolar.bagliSube') }}</span>
          <strong>{{ depo.subeAdi || '-' }}</strong>
        </div>
        <div class="bilgi-satir">
          <span>{{ t('depolar.adres') }}</span>
          <strong>{{ depo.adres || '-' }}</strong>
        </div>
      </div>

      <div class="depo-360-ozet">
        <div class="ozet-kutu">
          <span class="ozet-deger">{{ stoklar.length }}</span>
          <span class="ozet-etiket">{{ t('depolar.kpiStokKalemi') }}</span>
        </div>
        <div class="ozet-kutu">
          <span class="ozet-deger">{{ formatMiktar(toplamMiktar) }}</span>
          <span class="ozet-etiket">{{ t('depolar.toplamMiktar') }}</span>
        </div>
        <div class="ozet-kutu">
          <span class="ozet-deger">{{ sifirSayisi }}</span>
          <span class="ozet-etiket">{{ t('depolar.kpiSifirStok') }}</span>
        </div>
      </div>

      <DataTable
        :value="stoklar"
        :loading="yukleniyor"
        size="small"
        striped-rows
        scrollable
        scroll-height="260px"
      >
        <template #empty>
          <EmptyState
            icon="pi pi-box"
            :message="t('depolar.stokYok')"
          />
        </template>
        <Column
          field="stokKodu"
          :header="t('depolar.stokKodu')"
          style="width: 130px"
        />
        <Column
          field="stokAd"
          :header="t('depolar.urunAdi')"
        />
        <Column
          field="birim"
          :header="t('depolar.birim')"
          style="width: 80px"
        />
        <Column
          field="miktar"
          :header="t('depolar.miktar')"
          style="width: 110px"
        >
          <template #body="{ data }">
            <span :class="{ sifir: Number(data.miktar) <= 0 }">{{ formatMiktar(data.miktar) }}</span>
          </template>
        </Column>
      </DataTable>
    </div>

    <template #footer>
      <Button
        :label="t('depolar.transfer')"
        icon="pi pi-sync"
        class="p-button-text"
        @click="$emit('transfer', depo)"
      />
      <Button
        :label="t('depolar.stoklariYonet')"
        icon="pi pi-box"
        class="p-button-outlined"
        @click="$emit('stoklari-yonet', depo)"
      />
      <Button
        :label="t('common.edit')"
        icon="pi pi-pencil"
        @click="$emit('duzenle', depo)"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { depoAPI } from '../api/index.js'
import EmptyState from './EmptyState.vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  depo: { type: Object, default: null }
})

defineEmits(['update:visible', 'duzenle', 'stoklari-yonet', 'transfer'])

const { t } = useI18n()
const stoklar = ref([])
const yukleniyor = ref(false)

const toplamMiktar = computed(() =>
  stoklar.value.reduce((acc, s) => acc + (Number(s?.miktar) || 0), 0)
)
const sifirSayisi = computed(() => stoklar.value.filter((s) => (Number(s?.miktar) || 0) <= 0).length)

const formatMiktar = (v) => {
  if (v === null || v === undefined || isNaN(v)) return '0'
  return new Intl.NumberFormat('tr-TR', { maximumFractionDigits: 2 }).format(v)
}

const stoklariYukle = async () => {
  if (!props.depo?.id) {
    stoklar.value = []
    return
  }
  yukleniyor.value = true
  try {
    const r = await depoAPI.getStoklar(props.depo.id)
    stoklar.value = r.data || []
  } catch {
    stoklar.value = []
  } finally {
    yukleniyor.value = false
  }
}

watch(
  () => [props.visible, props.depo?.id],
  ([acik]) => {
    if (acik) stoklariYukle()
  }
)
</script>

<style scoped>
.depo-360 {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.depo-360-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.depo-360-baslik {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}
.depo-360-ikon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 1.2rem;
  flex-shrink: 0;
}
.depo-360-ad h3 {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--text-primary);
}
.depo-360-alt {
  font-size: 0.8rem;
  color: var(--text-secondary);
}
.depo-360-bilgi {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(200px, 100%), 1fr));
  gap: 10px;
}
.bilgi-satir {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-secondary);
  min-width: 0;
}
.bilgi-satir span {
  font-size: 0.72rem;
  color: var(--text-muted);
  font-weight: 600;
}
.bilgi-satir strong {
  font-size: 0.88rem;
  color: var(--text-primary);
  word-break: break-word;
}
.depo-360-ozet {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(140px, 100%), 1fr));
  gap: 10px;
}
.ozet-kutu {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-card);
}
.ozet-deger {
  font-size: 1.35rem;
  font-weight: 800;
  color: var(--primary-color, var(--accent));
}
.ozet-etiket {
  font-size: 0.76rem;
  color: var(--text-secondary);
}
.sifir {
  color: var(--danger, #ef4444);
  font-weight: 700;
}
</style>
