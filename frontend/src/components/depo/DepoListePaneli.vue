<template>
  <div class="depo-liste-paneli">
    <div class="depo-arac-cubugu">
      <span class="arama-kutu">
        <i class="pi pi-search" />
        <InputText
          :model-value="store.filtre.q"
          :placeholder="t('depolar.depoAra')"
          class="arama-input"
          @update:model-value="aramaDegistir"
        />
      </span>
      <Dropdown
        :model-value="store.filtre.subeId"
        :options="subeler"
        option-label="ad"
        option-value="id"
        :placeholder="t('depolar.sube')"
        show-clear
        @update:model-value="(v) => store.filtreDegistir({ subeId: v })"
      />
      <Dropdown
        :model-value="store.filtre.aktif"
        :options="durumSecenekleri"
        option-label="label"
        option-value="value"
        :placeholder="t('common.status')"
        show-clear
        @update:model-value="(v) => store.filtreDegistir({ aktif: v })"
      />
      <label class="kritik-filtre">
        <Checkbox
          :model-value="store.filtre.kritik"
          binary
          @update:model-value="(v) => store.filtreDegistir({ kritik: !!v })"
        />
        <span>{{ t('depolar.sadeceKritik') }}</span>
      </label>
      <div class="arac-sag">
        <Button
          :icon="gosterim === 'tablo' ? 'pi pi-th-large' : 'pi pi-list'"
          text
          rounded
          :aria-label="gosterim === 'tablo' ? t('depolar.kartGorunumu') : t('depolar.tabloGorunumu')"
          @click="$emit('gosterim-degistir')"
        />
        <Button
          :label="t('depolar.csvIndir')"
          icon="pi pi-download"
          text
          @click="csvIndir"
        />
        <Button
          v-if="adminMi"
          :label="t('depolar.yeniDepo')"
          icon="pi pi-plus"
          @click="$emit('yeni')"
        />
      </div>
    </div>

    <div
      v-if="store.hata"
      class="depo-hata"
      role="alert"
      aria-live="assertive"
    >
      <i class="pi pi-exclamation-triangle" /> {{ store.hata }}
      <Button
        :label="t('common.retry')"
        text
        size="small"
        @click="store.yukle()"
      />
    </div>

    <DataTable
      v-if="gosterim === 'tablo'"
      :value="store.liste"
      striped-rows
      :loading="store.yukleniyor"
      data-test="depo-tablo"
    >
      <template #empty>
        <EmptyState
          :message="t('depolar.empty')"
          :sub-message="t('depolar.emptyHint')"
          icon="pi pi-warehouse"
          :action-label="t('depolar.yeniDepo')"
          action-icon="pi pi-plus"
          @action="$emit('yeni')"
        />
      </template>
      <Column
        field="ad"
        :header="t('depolar.depoAdi')"
        sortable
      />
      <Column
        field="subeAdi"
        :header="t('depolar.bagliSube')"
      />
      <Column
        field="sorumluKullaniciAd"
        :header="t('depolar.sorumluKullanici')"
      >
        <template #body="{ data }">
          {{ data.sorumluKullaniciAd || data.yetkili || '—' }}
        </template>
      </Column>
      <Column
        field="kalemSayisi"
        :header="t('depolar.kalem')"
      />
      <Column
        field="kritikStokSayisi"
        :header="t('depolar.kritik')"
      >
        <template #body="{ data }">
          <Tag
            v-if="Number(data.kritikStokSayisi) > 0"
            :value="data.kritikStokSayisi"
            severity="danger"
          />
          <span v-else>0</span>
        </template>
      </Column>
      <Column
        field="stokDegeri"
        :header="t('depolar.stokDegeri')"
      >
        <template #body="{ data }">
          {{ formatCurrency(data.stokDegeri || 0) }}
        </template>
      </Column>
      <Column
        field="dolulukYuzde"
        :header="t('depolar.doluluk')"
      >
        <template #body="{ data }">
          <span v-if="data.dolulukYuzde != null">{{ data.dolulukYuzde }}%</span>
          <span v-else>—</span>
        </template>
      </Column>
      <Column
        field="aktif"
        :header="t('common.status')"
      >
        <template #body="{ data }">
          <Tag
            :value="data.aktif ? t('status.active') : t('status.passive')"
            :severity="data.aktif ? 'success' : 'danger'"
          />
        </template>
      </Column>
      <Column
        :header="t('common.actions')"
        style="width: 70px"
      >
        <template #body="{ data }">
          <SatirEylemleri
            :gorunur="{ duzenle: adminMi, sil: adminMi }"
            :baslik="data.ad"
            :items="menuItems(data)"
            @duzenle="$emit('duzenle', data)"
            @sil="$emit('sil', data)"
          />
        </template>
      </Column>
    </DataTable>

    <div
      v-else
      class="depo-kartlar"
    >
      <div
        v-for="d in store.liste"
        :key="d.id"
        class="depo-kart"
      >
        <div class="depo-kart-ust">
          <strong>{{ d.ad }}</strong>
          <Tag
            :value="d.aktif ? t('status.active') : t('status.passive')"
            :severity="d.aktif ? 'success' : 'danger'"
          />
        </div>
        <div class="depo-kart-satir">
          <i class="pi pi-building" /> {{ d.subeAdi || '—' }}
        </div>
        <div class="depo-kart-satir">
          <i class="pi pi-user" /> {{ d.sorumluKullaniciAd || d.yetkili || '—' }}
        </div>
        <div class="depo-kart-satir depo-kart-metrik">
          <span>{{ d.kalemSayisi || 0 }} {{ t('depolar.kalem') }}</span>
          <span :class="{ 'text-danger': Number(d.kritikStokSayisi) > 0 }">
            {{ d.kritikStokSayisi || 0 }} {{ t('depolar.kritik') }}
          </span>
          <span>{{ formatCurrency(d.stokDegeri || 0) }}</span>
        </div>
        <div
          v-if="d.dolulukYuzde != null"
          class="doluluk-cubuk"
          :title="`${t('depolar.doluluk')}: ${d.dolulukYuzde}%`"
        >
          <span :style="{ width: Math.min(100, d.dolulukYuzde) + '%' }" />
        </div>
        <div class="depo-kart-alt">
          <Button
            :label="t('depolar.stoklariGor')"
            icon="pi pi-box"
            size="small"
            text
            @click="$emit('stoklar', d)"
          />
          <SatirEylemleri
            :gorunur="{ duzenle: adminMi, sil: adminMi }"
            :baslik="d.ad"
            :items="menuItems(d)"
            @duzenle="$emit('duzenle', d)"
            @sil="$emit('sil', d)"
          />
        </div>
      </div>
    </div>

    <Paginator
      v-if="store.toplam > store.boyut"
      :rows="store.boyut"
      :total-records="store.toplam"
      :first="store.sayfa * store.boyut"
      :rows-per-page-options="[20, 50, 100]"
      @page="sayfaDegisti"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDepoStore } from '../../stores/depoStore.js'
import { depoAPI } from '../../api/index.js'
import { formatCurrency } from '../../utils/format.js'

defineProps({
  subeler: { type: Array, default: () => [] },
  adminMi: { type: Boolean, default: false },
  gosterim: { type: String, default: 'tablo' }
})
const emit = defineEmits(['gosterim-degistir', 'yeni', 'duzenle', 'sil', 'detay', 'stoklar', 'transfer'])

const { t } = useI18n()
const store = useDepoStore()

const durumSecenekleri = computed(() => [
  { label: t('status.active'), value: true },
  { label: t('status.passive'), value: false }
])

const aramaDegistir = (v) => store.filtreDegistir({ q: v }, true)

const sayfaDegisti = (e) => {
  if (e.rows !== store.boyut) store.boyutDegistir(e.rows)
  else store.sayfaDegistir(Math.floor(e.first / e.rows))
}

const menuItems = (d) => [
  { etiket: t('depolar.detay'), ikon: 'pi pi-eye', islem: () => emit('detay', d) },
  { etiket: t('depolar.stoklariGor'), ikon: 'pi pi-box', islem: () => emit('stoklar', d) },
  { etiket: t('depolar.transferTalebiOlustur'), ikon: 'pi pi-send', islem: () => emit('transfer', d) }
]

const csvIndir = async () => {
  const r = await depoAPI.export({
    tur: 'liste',
    q: store.filtre.q || undefined,
    subeId: store.filtre.subeId || undefined,
    aktif: store.filtre.aktif ?? undefined,
    kritik: store.filtre.kritik || undefined
  })
  const url = URL.createObjectURL(new Blob([r.data], { type: 'text/csv;charset=utf-8' }))
  const a = document.createElement('a')
  a.href = url
  a.download = 'depolar.csv'
  a.click()
  URL.revokeObjectURL(url)
}

defineExpose({ store })
</script>

<style scoped>
.depo-liste-paneli { display: flex; flex-direction: column; gap: 10px; }
.depo-arac-cubugu { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
.arama-kutu { display: inline-flex; align-items: center; gap: 6px; }
.arama-input { min-width: 220px; }
.kritik-filtre { display: inline-flex; align-items: center; gap: 6px; font-size: 13px; }
.arac-sag { margin-left: auto; display: flex; gap: 4px; }
.depo-kartlar { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 12px; }
.depo-kart { border: 1px solid var(--border); border-radius: 12px; padding: 12px; background: var(--bg-card); display: flex; flex-direction: column; gap: 8px; }
.depo-kart-ust, .depo-kart-metrik { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.depo-kart-alt { display: flex; align-items: center; justify-content: space-between; gap: 8px; position: sticky; bottom: 0; background: var(--bg-card); }
.depo-kart-satir { display: flex; align-items: center; gap: 6px; font-size: 13px; color: var(--text-secondary); }
.depo-kart-metrik { font-size: 12px; color: var(--text-muted); }
.doluluk-cubuk { height: 6px; border-radius: 999px; background: var(--bg-secondary); overflow: hidden; }
.doluluk-cubuk span { display: block; height: 100%; background: var(--accent); }
.depo-hata { display: flex; align-items: center; gap: 8px; color: var(--danger); }

/* Dokunmatik: hedef alanlar en az 44px (WCAG 2.5.8 / plan Task 11). */
@media (pointer: coarse) {
  .depo-arac-cubugu :deep(.p-button),
  .depo-kart-alt :deep(.p-button),
  .depo-kartlar :deep(.p-button) { min-height: 44px; }
  .depo-arac-cubugu :deep(.p-select) { min-height: 44px; }
}
</style>
